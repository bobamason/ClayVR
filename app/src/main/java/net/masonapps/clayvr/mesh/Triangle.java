package net.masonapps.clayvr.mesh;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;

import net.masonapps.clayvr.bvh.BVH;

public class Triangle {

    public final Plane plane = new Plane();
    public final Vertex v1;
    public final Vertex v2;
    public final Vertex v3;
    public final Edge e1;
    public final Edge e2;
    public final Edge e3;
    private final Vector3 tmp = new Vector3();
    private final Vector3 tmp2 = new Vector3();
    public float a1 = 0;
    public float a2 = 0;
    public float a3 = 0;
    public BVH.Node node = null;
    public int index;
    private boolean needsUpdate = false;

    public Triangle(Vertex v1, Vertex v2, Vertex v3) {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        v1.addTriangle(this);
        v2.addTriangle(this);
        v3.addTriangle(this);
        e1 = new Edge(v1, v2);
        e2 = new Edge(v2, v3);
        e3 = new Edge(v3, v1);
        update();
    }

    public boolean intersect(Ray ray) {
        return intersect(ray, null);
    }

    public boolean intersect(Ray ray, Vector3 hitPoint) {
        return Intersector.intersectRayTriangle(ray, v3.position, v2.position, v1.position, hitPoint);
    }


    public BoundingBox extendBounds(BoundingBox bounds) {
        bounds.ext(v1.position);
        bounds.ext(v2.position);
        bounds.ext(v3.position);
        return bounds;
    }

    public void update() {
        a1 = 1f;
        a2 = 1f;
        a3 = 1f;

        plane.set(v1.position, v2.position, v3.position);
        clearUpdateFlag();
    }

    public void updateSlow() {
        tmp.set(v2.position).sub(v1.position).nor();
        tmp2.set(v3.position).sub(v1.position).nor();
        a1 = (float) Math.acos(tmp.dot(tmp2));

        tmp.set(v3.position).sub(v2.position).nor();
        tmp2.set(v1.position).sub(v2.position).nor();
        a2 = (float) Math.acos(tmp.dot(tmp2));

        tmp.set(v1.position).sub(v3.position).nor();
        tmp2.set(v2.position).sub(v3.position).nor();
        a3 = (float) Math.acos(tmp.dot(tmp2));

        plane.set(v1.position, v2.position, v3.position);

        clearUpdateFlag();
    }
    public float getWeight(Vertex vertex) {
        if (vertex.index == v1.index)
            return a1;
        if (vertex.index == v2.index)
            return a2;
        if (vertex.index == v3.index)
            return a3;
        return 0;
    }

    public void flagNeedsUpdate() {
        needsUpdate = true;
        if (node != null)
            node.flagNeedsRefit();
    }

    public void clearUpdateFlag() {
        needsUpdate = false;
    }

    public boolean needsUpdate() {
        return needsUpdate;
    }
}