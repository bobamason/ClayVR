package net.masonapps.clayvr.sculpt;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;

import net.masonapps.clayvr.Style;
import net.masonapps.clayvr.bvh.BVH;
import net.masonapps.clayvr.math.Segment;
import net.masonapps.clayvr.mesh.Vertex;

import org.masonapps.libgdxgooglevr.GdxVr;
import org.masonapps.libgdxgooglevr.gfx.Entity;
import org.masonapps.libgdxgooglevr.utils.ElapsedTimer;
import org.masonapps.libgdxgooglevr.utils.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Created by Bob Mason on 4/5/2018.
 */
public class SculptHandler {

    private final ExecutorService executor;
    private final BVH bvh;
    private final Brush brush;
    private final SculptMesh sculptMesh;
    private final UndoRedoCache undoRedoCache;
    private final Entity sculptEntity;
    private final Ray tmpRay = new Ray();
    private final Matrix4 tmpMat = new Matrix4();
    private final DropperListener dropperListener;
    private Vector3 startHitPoint = new Vector3();
    private Vector3 lastHitPoint = new Vector3();
    private Vector3 rawHitPoint = new Vector3();
    private Vector3 transformedHitPoint = new Vector3();
    private Vector3 hitPoint = new Vector3();
    private float rayLength;
    private Segment segment = new Segment();
    private BVH.IntersectionInfo intersection = new BVH.IntersectionInfo();
    private boolean shouldDoDropper = false;
    private boolean busySculpting = false;
    private List<Vertex> vertices = Collections.synchronizedList(new ArrayList<Vertex>());
    private Ray ray = new Ray();
    private Quaternion startRotation = new Quaternion();
    private Quaternion currentRotation = new Quaternion();
    private Quaternion grabRotation = new Quaternion();

    public SculptHandler(BVH bvh, Brush brush, SculptMesh sculptMesh, DropperListener dropperListener) {
        this.bvh = bvh;
        this.brush = brush;
        this.sculptMesh = sculptMesh;
        this.dropperListener = dropperListener;
        executor = Executors.newSingleThreadExecutor();
        undoRedoCache = new UndoRedoCache();
        undoRedoCache.save(sculptMesh.getVertexArray());
        final ModelBuilder builder = new ModelBuilder();
        sculptEntity = SculptMesh.createSculptEntity(builder, sculptMesh, bvh.root.bb, Style.createSculptMaterial());
    }

    public Brush getBrush() {
        return brush;
    }

    private void saveVertexPositions() {
        vertices.forEach(vertex -> {
            if (!vertex.isPositionSaved()) {
                vertex.savePosition();
                if (brush.useSymmetry() && vertex.symmetricPair != null)
                    vertex.symmetricPair.savePosition();
            }
        });
    }

    public void setShouldDoDropper(boolean shouldDoDropper) {
        this.shouldDoDropper = shouldDoDropper;
    }

    public boolean shouldDoDropper() {
        return shouldDoDropper;
    }

    public void shutdown() {
        undoRedoCache.clear();
        executor.shutdownNow();
    }

    public void sculpt() {
        Logger.d("sculpt()");
        ray.set(GdxVr.input.getInputRay()).mul(sculptEntity.getInverseTransform());
        if (shouldDoDropper
                && testBVHIntersection(ray, false)
                && intersection.triangle != null) {
            final Vertex v1 = intersection.triangle.v1;
            final Vertex v2 = intersection.triangle.v2;
            final Vertex v3 = intersection.triangle.v3;
            Vertex closest;
            if (v1.position.dst2(hitPoint) < v2.position.dst2(hitPoint)) {
                if (v1.position.dst2(hitPoint) < v3.position.dst2(hitPoint))
                    closest = v1;
                else
                    closest = v3;
            } else {
                if (v2.position.dst2(hitPoint) < v3.position.dst2(hitPoint))
                    closest = v2;
                else
                    closest = v3;
            }

            dropperListener.onDropperColorChanged(closest.color);
            return;
        }
        if (isBrushGrab()) {
            updateHitPointUsingRayLength();
            updateBrush(ray);
            grabRotation.set(startRotation).conjugate().mulLeft(currentRotation);
            brush.setGrabRotation(grabRotation);
            updateVertices();
            lastHitPoint.set(hitPoint);
        } else {
            if (!testBVHIntersection(ray, true)) {
                updateHitPointUsingRayLength();
            }
            segment.set(lastHitPoint, hitPoint);
            saveVertexPositions();
            updateBrush(ray);
            updateVertices();
            lastHitPoint.set(hitPoint);
        }
    }

    private boolean testBVHIntersection(Ray ray, boolean limitMovement) {
        boolean hasIntersection;
        hasIntersection = bvh.closestIntersection(ray, intersection);
        if (hasIntersection) {
            if (limitMovement) {
                rawHitPoint.set(intersection.hitPoint);
                hitPoint.set(rawHitPoint).sub(lastHitPoint).limit(brush.getRadius() * 0.75f).add(lastHitPoint);
            } else
                hitPoint.set(intersection.hitPoint);
            transformedHitPoint.set(hitPoint).mul(sculptEntity.getTransform());
            rayLength = intersection.t;
        } else {
            updateHitPointUsingRayLength();
            Logger.d("no hit");
        }
        Logger.d("transformedHitPoint = " + transformedHitPoint);
        return hasIntersection;
    }

    private void updateHitPointUsingRayLength() {
        hitPoint.set(ray.direction).scl(rayLength).add(ray.origin);
        transformedHitPoint.set(hitPoint).mul(sculptEntity.getTransform(tmpMat));
    }

    public void onControllerUpdate() {
        ray.set(GdxVr.input.getInputRay()).mul(sculptEntity.getInverseTransform());
        testBVHIntersection(ray, false);
        currentRotation.set(GdxVr.input.getControllerOrientation());
    }
    
    public void onTouchPadButtonDown() {
        Logger.d("onTouchPadButtonDown()");
        startHitPoint.set(hitPoint);
        lastHitPoint.set(hitPoint);
        startRotation.set(currentRotation);
        if (isBrushGrab()) {
            bvh.sphereSearch(vertices, hitPoint, brush.getRadius());
            saveVertexPositions();
        }
    }

    public void onTouchPadButtonUp() {
        Logger.d("onTouchPadButtonUp()");
        shouldDoDropper = false;
        Arrays.stream(sculptMesh.getVertexArray()).forEach(vertex -> {
            vertex.clearFlagSkipSphereTest();
            vertex.clearSavedFlag();
        });
        undoRedoCache.save(sculptMesh.getVertexArray());
        vertices.clear();
    }

    private void updateBrush(Ray ray) {
        brush.update(ray, startHitPoint, hitPoint, segment);
    }

    private boolean isBrushGrab() {
        return brush.getType() == Brush.Type.GRAB;
    }

    private void updateVertices() {
        if (busySculpting) return;
        busySculpting = true;
        final SculptAction sculptAction;
        if (isBrushGrab())
            sculptAction = new SculptAction(vertices, brush);
        else
            sculptAction = new SculptAction(bvh, hitPoint, brush);
        CompletableFuture.supplyAsync(() -> {
            ElapsedTimer.getInstance().start("sculpt");
            sculptAction.apply();

            bvh.refit();

            Arrays.stream(sculptMesh.getVertexArray())
                    .filter(Vertex::needsUpdate)
                    .forEach(vertex -> {
                        if (brush.getType() != Brush.Type.VERTEX_PAINT)
                            vertex.recalculateNormal();
                        vertex.clearUpdateFlag();
                    });
            ElapsedTimer.getInstance().print("sculpt");
            return sculptAction.getVertices().stream().map(Vertex::new).collect(Collectors.toList());
        }, executor).thenAccept(list -> runOnGLThread(() -> {
            updateSculptMesh(list);
            busySculpting = false;
        }));
    }

    private void runOnGLThread(Runnable runnable) {
        GdxVr.app.postRunnable(runnable);
    }

    private void updateSculptMesh(List<Vertex> vertices) {
        synchronized (bvh.root.bb) {
            sculptEntity.getBounds().set(bvh.root.bb);
        }
        sculptEntity.updateDimensions();
        if (brush.useSymmetry())
            vertices.forEach(vertex -> {
                sculptMesh.setVertex(vertex);
                if (vertex.symmetricPair != null)
                    sculptMesh.setVertex(vertex.symmetricPair);
            });
        else
            vertices.forEach(sculptMesh::setVertex);
        sculptMesh.update();
    }

    public SculptMesh getSculptMesh() {
        return sculptMesh;
    }

    public Ray getTransformedRay() {
        return ray;
    }

    public void undo() {
        UndoRedoCache.applySaveData(sculptMesh, undoRedoCache.undo(), bvh);
    }

    public void redo() {
        UndoRedoCache.applySaveData(sculptMesh, undoRedoCache.redo(), bvh);
    }

    public BVH getBVH() {
        return bvh;
    }

    public Entity getSculptEntity() {
        return sculptEntity;
    }

    public Vector3 getTransformedHitPoint() {
        return transformedHitPoint;
    }

    public interface DropperListener {
        void onDropperColorChanged(Color color);
    }
}
