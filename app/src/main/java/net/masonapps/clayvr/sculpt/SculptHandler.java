package net.masonapps.clayvr.sculpt;

import android.support.annotation.Nullable;

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
    private final DropperListener dropperListener;
    private final Vector3 startHitPoint = new Vector3();
    private final Vector3 lastHitPoint = new Vector3();
    private final Vector3 rawHitPoint = new Vector3();
    private final Vector3 transformedHitPoint = new Vector3();
    private final Vector3 hitPoint = new Vector3();
    private final Segment segment = new Segment();
    private final BVH.IntersectionInfo intersection = new BVH.IntersectionInfo();
    private final List<Vertex> vertices = Collections.synchronizedList(new ArrayList<Vertex>());
    private final Ray ray = new Ray();
    private final Quaternion startRotation = new Quaternion();
    private final Quaternion currentRotation = new Quaternion();
    private final Quaternion grabRotation = new Quaternion();
    private final Matrix4 sculptEntityTransform = new Matrix4();
    private float rayLength = 3f;
    private volatile boolean shouldDoDropper = false;
    private volatile boolean busySculpting = false;
    private volatile boolean isSculpting = false;
    private volatile boolean isCursorOver = false;

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
        if (busySculpting)
            return;
        busySculpting = true;
        CompletableFuture.supplyAsync(() -> {
            final boolean bvhIntersection;
            if (isBrushGrab()) {
                updateHitPointUsingRayLength();
                bvhIntersection = true;
            } else {
                bvhIntersection = testBVHIntersection(ray, !shouldDoDropper);
            }
            if (shouldDoDropper()
                    && bvhIntersection
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
                return null;
            }
            if (isBrushGrab()) {
                updateBrush(ray);
                grabRotation.set(startRotation).conjugate().mul(currentRotation);
                brush.setGrabRotation(grabRotation);
            } else {
                segment.set(lastHitPoint, hitPoint);
                saveVertexPositions();
                updateBrush(ray);
            }
            lastHitPoint.set(hitPoint);
            return updateVertices();
        }, executor).thenAccept(list -> runOnGLThread(() -> {
            updateSculptMesh(list, brush.useSymmetry());
            busySculpting = false;
        }));
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
            synchronized (transformedHitPoint) {
                transformedHitPoint.set(hitPoint).mul(sculptEntityTransform);
            }
            rayLength = intersection.t;
        } else {
            updateHitPointUsingRayLength();
        }
        return hasIntersection;
    }

    private void updateHitPointUsingRayLength() {
        synchronized (transformedHitPoint) {
            hitPoint.set(ray.direction).scl(rayLength).add(ray.origin);
            transformedHitPoint.set(hitPoint).mul(sculptEntityTransform);
        }
    }

    public void onControllerUpdate() {
        synchronized (ray) {
            ray.set(GdxVr.input.getInputRay()).mul(sculptEntity.getInverseTransform());
            ray.direction.nor();
        }
        synchronized (sculptEntityTransform) {
            sculptEntityTransform.set(sculptEntity.getTransform());
        }
        synchronized (currentRotation) {
            currentRotation.set(GdxVr.input.getControllerOrientation());
        }
        if (isSculpting)
            sculpt();
        else
            CompletableFuture.runAsync(() -> isCursorOver = testBVHIntersection(ray, false), executor);
    }

    public void onTouchPadButtonDown() {
        CompletableFuture.runAsync(() -> {
            isSculpting = true;
            synchronized (hitPoint) {
                startHitPoint.set(hitPoint);
                lastHitPoint.set(hitPoint);
            }
            if (isBrushGrab()) {
                startRotation.set(currentRotation);
                bvh.sphereSearch(vertices, hitPoint, brush.getRadius());
                saveVertexPositions();
            }
            }, executor);
    }

    public void onTouchPadButtonUp() {
        CompletableFuture.runAsync(() -> {
            isSculpting = false;
            shouldDoDropper = false;
            Arrays.stream(sculptMesh.getVertexArray()).forEach(vertex -> {
                vertex.clearFlagSkipSphereTest();
                vertex.clearSavedFlag();
            });
            undoRedoCache.save(sculptMesh.getVertexArray());
            vertices.clear();
        }, executor);
    }

    private void updateBrush(Ray ray) {
        brush.update(ray, startHitPoint, hitPoint, segment);
    }

    private synchronized boolean isBrushGrab() {
        return brush.getType() == Brush.Type.GRAB;
    }

    private List<Vertex> updateVertices() {
        final SculptAction sculptAction;
        if (isBrushGrab())
            sculptAction = new SculptAction(vertices, brush);
        else
            sculptAction = new SculptAction(bvh, hitPoint, brush);
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
    }

    private void runOnGLThread(Runnable runnable) {
        GdxVr.app.postRunnable(runnable);
    }

    private void updateSculptMesh(@Nullable List<Vertex> vertices, boolean applySymmetry) {
        if (vertices == null) return;
        synchronized (bvh.root.bb) {
            sculptEntity.getBounds().set(bvh.root.bb);
        }
        sculptEntity.updateDimensions();
        if (applySymmetry)
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

    public void undo() {
        applySaveData(undoRedoCache.undo());
    }

    public void redo() {
        applySaveData(undoRedoCache.redo());
    }

    private void applySaveData(SaveData[] redo) {
        if (busySculpting) return;
        busySculpting = true;
        CompletableFuture.supplyAsync(() -> UndoRedoCache.applySaveData(sculptMesh.getMeshData(), redo, bvh), executor)
                .thenAccept(list -> runOnGLThread(() -> {
                    updateSculptMesh(list, false);
                    busySculpting = false;
                }));
    }

    public BVH getBVH() {
        return bvh;
    }

    public Entity getSculptEntity() {
        return sculptEntity;
    }

    public synchronized Vector3 getTransformedHitPoint() {
        return transformedHitPoint;
    }

    public boolean isCursorOver() {
        return isCursorOver;
    }

    public interface DropperListener {
        void onDropperColorChanged(Color color);
    }
}
