package net.masonapps.clayvr.sculpt;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;

import net.masonapps.clayvr.math.SculptUtils;
import net.masonapps.clayvr.math.Segment;
import net.masonapps.clayvr.mesh.Triangle;
import net.masonapps.clayvr.mesh.Vertex;

import java.util.List;

import static net.masonapps.clayvr.sculpt.Brush.DropOffFunction.NONE;

/**
 * Created by Bob on 7/12/2017.
 */

public class Brush {

    public static final float SYMMETRY_TOLERANCE = 1e-4f;
    public static final float MIN_RADIUS = 0.075f;
    public static final float MAX_RADIUS = 0.75f;
    private float strength = 0.5f;
    private float radius = 0.2f;
    private Type type = Type.DRAW;
    private float flip = 1;
    private DropOffFunction dropOffFunction = DropOffFunction.SMOOTH;
    private Segment segment = new Segment();
    private Plane sculptPlane = new Plane();
    private Ray ray = new Ray();
    private Vector3 hitPoint = new Vector3();
    private Vector3 startHitPoint = new Vector3();
    private Color color = new Color();
    private boolean useSymmetry = true;
    private Stroke stroke;
    private final Vector3 tmp = new Vector3();
    private final Segment tmpSegment = new Segment();
    private Quaternion grabRotation = new Quaternion();

    public Brush() {

    }

    public Brush(Brush brush) {
        color.set(brush.color);
        sculptPlane.set(brush.sculptPlane);
        strength = brush.strength;
        type = brush.type;
        flip = brush.flip;
        radius = brush.radius;
        useSymmetry = brush.useSymmetry;
        dropOffFunction = brush.dropOffFunction;
        grabRotation.set(brush.grabRotation);
        update(brush.ray, brush.startHitPoint, brush.hitPoint, brush.segment);
    }

    public void applyDrawBrush(Vertex vertex, Segment segment, Vector3 direction) {
        Vector3 tmp = new Vector3();
        tmp.set(direction).scl(flip * radius * 0.25f * strength * calculateStrength(segment.distancePointToSegment(vertex.position)));
        vertex.position.add(tmp);
//        final float s = radius * 0.25f * strength * calculateStrength(segment.distancePointToSegment(vertex.position));
//        if (s > vertex.strength) {
//            vertex.strength = s;
//            tmp.set(direction).scl(flip * vertex.strength);
//            vertex.position.add(tmp);
//        }
    }

    public void applyDrawBrush(Vertex vertex, Stroke stroke, Vector3 direction) {
        Vector3 tmp = new Vector3();
        tmp.set(direction).scl(flip * radius * strength * calculateStrength(vertex.position, stroke));
        vertex.position.add(tmp);
    }

    public void applyPinchBrush(Vertex vertex, Segment segment) {
        Vector3 tmp = segment.projectToSegment(vertex.position);
        tmp.sub(vertex.position).scl(strength * radius * 0.5f * calculateStrength(segment.distancePointToSegment(vertex.position)));
        vertex.position.add(tmp);
    }

    private void applyInflateBrush(Vertex vertex, Segment segment) {
        Vector3 tmp = new Vector3();
        tmp.set(vertex.normal).scl(flip * radius * 0.25f * strength * calculateStrength(segment.distancePointToSegment(vertex.position)));
        vertex.position.add(tmp);
    }

    private void applyFlattenBrush(Vertex vertex, Segment segment) {
        Vector3 tmp = new Vector3();
        float t = -sculptPlane.distance(vertex.position);
        tmp.set(sculptPlane.normal).scl(t).scl(strength * radius * 0.5f * calculateStrength(segment.distancePointToSegment(vertex.position)));
//        tmp.set(sculptPlane.normal).scl(t).scl(strength * radius * 0.5f);
        vertex.position.add(tmp);
    }

    public void applySmoothBrush(Vertex vertex, Segment segment, Vertex[] adjVerts, int smoothPasses, DropOffFunction dropOffFunction) {
        final Array<Vector3> positions = new Array<>();
        for (Vertex adjVert : adjVerts) {
            positions.add(adjVert.position);
        }
        if (positions.size == 0) return;
        for (int i = 0; i < smoothPasses; i++) {
            final Vector3 average = new Vector3(vertex.position);
            for (Vector3 v : positions) {
                average.add(v);
            }
            average.scl(1f / (positions.size + 1));
            tmp.set(average).sub(vertex.position).scl(strength * radius * calculateStrength(segment.distancePointToSegment(vertex.position)));
            vertex.position.add(tmp);
        }
    }

    public void applySmoothBrush(Vertex vertex, Segment segment, Vertex[] adjVerts, int smoothPasses) {
        final Array<Vector3> positions = new Array<>();
        for (Vertex adjVert : adjVerts) {
            positions.add(adjVert.position);
        }
        if (positions.size == 0) return;
        for (int i = 0; i < smoothPasses; i++) {
            final Vector3 average = new Vector3(vertex.position);
            for (Vector3 v : positions) {
                average.add(v);
            }
            average.scl(1f / (positions.size + 1));
            tmp.set(average).sub(vertex.position).scl(strength * radius * calculateStrength(segment.distancePointToSegment(vertex.position)));
            vertex.position.add(tmp);
        }
    }

    public void applyVertexPaintBrush(Vertex vertex, Segment segment, Color color) {
        vertex.color.lerp(color, strength * calculateStrength(segment.distancePointToSegment(vertex.position)));
    }

    public void doGrabBrush(Vertex vertex, Vector3 startHitPoint, Vector3 hitPoint, Quaternion rotation) {
        tmp.set(vertex.savedState.position).sub(startHitPoint).mul(rotation).add(hitPoint);
        vertex.position.set(vertex.savedState.position).lerp(tmp, calculateStrength(vertex.savedState.position.dst(startHitPoint)));
    }

    public float getStrength() {
        return strength;
    }

    public void setStrength(float strength) {
        this.strength = strength;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = Math.max(radius, 0.01f);
    }

    public DropOffFunction getDropOffFunction() {
        return dropOffFunction;
    }

    public void setDropOffFunction(DropOffFunction dropOffFunction) {
        this.dropOffFunction = dropOffFunction;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setGrabRotation(Quaternion rotation) {
        this.grabRotation.set(rotation);
    }

    public float calculateStrength(float d) {
        return calculateStrength(d, dropOffFunction);
    }

    public float calculateStrength(float d, DropOffFunction dropOffFunction) {
        if (dropOffFunction == NONE) return 1f;
        final float t = MathUtils.clamp(d / radius, 0f, 1f);
        switch (dropOffFunction) {
            case LINEAR:
                return MathUtils.lerp(0f, 1f, 1f - t);
            case QUADRATIC:
                return MathUtils.lerp(0f, 1f, 1f - t * t);
            case CUBIC:
                return MathUtils.lerp(0f, 1f, 1f - t * t * t);
            case SMOOTH:
                return MathUtils.lerp(0f, 1f, 1f - (t * t * (3f - 2f * t)));
            case SMOOTHER:
                return MathUtils.lerp(0f, 1f, 1f - (t * t * t * (t * (6f * t - 15f) + 10f)));
            case FLAT:
            default:
                if (t < 1f)
                    return 1f;
                else
                    return 0f;
        }
    }

    public float calculateStrength(Vector3 p, Stroke stroke) {
        float s = 0f;
        final int n = stroke.getPointCount();
        for (int i = 1; i < n; i++) {
            tmpSegment.set(stroke.getPoint(i - 1), stroke.getPoint(i));
            final float strength = calculateStrength(Stroke.distancePointToSegment(p, tmpSegment, i == 1, i == n - 1));
            if (strength > 0f)
                s = Math.max(s, strength);
//                s += strength;
        }
        return s;
    }

    public void update(Ray ray, Vector3 startHitPoint, Vector3 hitPoint, Segment segment) {
        this.ray.set(ray);
        this.startHitPoint.set(startHitPoint);
        this.hitPoint.set(hitPoint);
        this.segment.set(segment);
    }

    public void applyBrushToVertex(Vertex vertex) {

        if (useSymmetry) {
            if (hitPoint.x > 0f && vertex.position.x < -SYMMETRY_TOLERANCE)
                return;
            if (hitPoint.x < 0f && vertex.position.x > SYMMETRY_TOLERANCE)
                return;
        }
        
        switch (type) {
            case DRAW:
                applyDrawBrush(vertex, segment, sculptPlane.normal);
                applySmoothBrush(vertex, segment, vertex.getAdjacentVertices(), 1, DropOffFunction.NONE);
                break;
            case PINCH:
                applyPinchBrush(vertex, segment);
                break;
            case INFLATE:
                applyInflateBrush(vertex, segment);
                applySmoothBrush(vertex, segment, vertex.getAdjacentVertices(), 1, DropOffFunction.NONE);
                break;
            case FLATTEN:
                applyFlattenBrush(vertex, segment);
                break;
            case SMOOTH:
                applySmoothBrush(vertex, segment, vertex.getAdjacentVertices(), 4);
                break;
            case GRAB:
                doGrabBrush(vertex, startHitPoint, hitPoint, grabRotation);
                break;
            case VERTEX_PAINT:
                applyVertexPaintBrush(vertex, segment, color);
                break;
        }

        if (useSymmetry) {

            if (vertex.symmetricPair != null) {
                if (hitPoint.x > 0f)
                    vertex.position.x = Math.max(vertex.position.x, 0f);
                else
                    vertex.position.x = Math.min(vertex.position.x, 0f);
                
                vertex.symmetricPair.position.set(-vertex.position.x, vertex.position.y, vertex.position.z);
                vertex.symmetricPair.color.set(vertex.color);
                vertex.symmetricPair.flagNeedsUpdate();
                for (Triangle triangle : vertex.symmetricPair.triangles) {
                    triangle.flagNeedsUpdate();
                }
            } else {
                vertex.position.x = 0f;
            }
        }

        vertex.flagNeedsUpdate();
        if (type != Type.VERTEX_PAINT) {
            for (Triangle triangle : vertex.triangles) {
                triangle.flagNeedsUpdate();
            }
        }
    }

    public void applyBrushToVertexUsingStroke(Vertex vertex) {

        if (useSymmetry) {
            if (hitPoint.x > 0 && vertex.position.x < -SYMMETRY_TOLERANCE)
                return;
            if (hitPoint.x < 0 && vertex.position.x > SYMMETRY_TOLERANCE)
                return;
        }

        switch (type) {
            case DRAW:
                applyDrawBrush(vertex, stroke, sculptPlane.normal);
                break;
        }

        if (useSymmetry) {
            if (hitPoint.x > 0)
                vertex.position.x = Math.max(vertex.position.x, 0);
            else
                vertex.position.x = Math.min(vertex.position.x, 0);

            if (vertex.symmetricPair != null) {
                vertex.symmetricPair.position.set(-vertex.position.x, vertex.position.y, vertex.position.z);
                vertex.symmetricPair.color.set(vertex.color);
                vertex.symmetricPair.flagNeedsUpdate();
                if (type != Type.VERTEX_PAINT) {
                    for (Triangle triangle : vertex.symmetricPair.triangles) {
                        triangle.flagNeedsUpdate();
                    }
                }
            }
        }

        vertex.flagNeedsUpdate();
        if (type != Type.VERTEX_PAINT) {
            for (Triangle triangle : vertex.triangles) {
                triangle.flagNeedsUpdate();
            }
        }
    }

    public void updateSculptPlane(List<Vertex> vertexList) {
        if (vertexList.isEmpty()) return;
        if (type != Type.DRAW && type != Type.FLATTEN) return;
        SculptUtils.setPlaneFromVertices(vertexList, sculptPlane, ray);
    }

    public void setColor(Color color) {
        this.color.set(color);
    }

    public boolean isFlipEnabled() {
        return flip < 0;
    }

    public void setFlipEnabled(boolean flip) {
        this.flip = flip ? -1 : 1;
    }

    public boolean canFlip() {
        return type == Type.DRAW || type == Type.INFLATE;
    }

    public void setUseSymmetry(boolean useSymmetry) {
        this.useSymmetry = useSymmetry;
    }

    public boolean useSymmetry() {
        return useSymmetry;
    }

    public void update(Stroke stroke) {
        this.stroke = stroke;
    }

    public void updateSculptPlane(Plane plane) {
        sculptPlane.set(plane);
    }

    public enum Type {
        DRAW, PINCH, FLATTEN, SMOOTH, VERTEX_PAINT, INFLATE, GRAB
    }

    public enum DropOffFunction {
        LINEAR, QUADRATIC, CUBIC, SMOOTH, SMOOTHER, FLAT, NONE
    }
}
