package net.masonapps.clayvr.sculpt;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.badlogic.gdx.math.Vector3;

import net.masonapps.clayvr.bvh.BVH;
import net.masonapps.clayvr.mesh.Vertex;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Bob Mason on 3/12/2018.
 */

public class SculptAction implements Comparable<SculptAction> {
    private final long timestamp;
    private final List<Vertex> vertices;
    private final Brush brush;
    @Nullable
    private final BVH bvh;
    private final Vector3 hitPoint = new Vector3();

    public SculptAction(BVH bvh, Vector3 hitPoint, Brush brush) {
        this.bvh = bvh;
        this.hitPoint.set(hitPoint);
        this.vertices = new ArrayList<>();
        this.brush = new Brush(brush);
        timestamp = System.nanoTime();
    }

    public SculptAction(List<Vertex> vertices, Brush brush) {
        this.bvh = null;
        this.vertices = new ArrayList<>(vertices.size());
        this.vertices.addAll(vertices);
        this.brush = new Brush(brush);
        timestamp = System.nanoTime();
    }

    public void apply() {
        if (bvh != null)
            bvh.sphereSearch(vertices, hitPoint, brush.getRadius() + 0.125f);
        if (brush.getType() != Brush.Type.VERTEX_PAINT && brush.getType() != Brush.Type.GRAB)
            brush.updateSculptPlane(vertices);
        vertices.forEach(brush::applyBrushToVertex);
    }

    @Override
    public int compareTo(@NonNull SculptAction o) {
        return Long.compare(this.timestamp, o.timestamp);
    }

    public List<Vertex> getVertices() {
        return vertices;
    }
}
