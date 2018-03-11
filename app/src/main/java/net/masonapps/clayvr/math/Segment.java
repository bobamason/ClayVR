package net.masonapps.clayvr.math;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Pool;

/**
 * Created by Bob on 7/20/2017.
 */

public class Segment implements Pool.Poolable {

    private final Vector3 n = new Vector3();
    private final Vector3 v = new Vector3();
    public Vector3 p1;
    public Vector3 p2;

    public Segment() {
        this.p1 = new Vector3();
        this.p2 = new Vector3();
    }

    public Segment(Vector3 p1, Vector3 p2) {
        this.p1 = p1.cpy();
        this.p2 = p2.cpy();
    }

    public float length() {
        return Vector3.dst(p1.x, p1.y, p1.z, p2.x, p2.y, p2.z);
    }

    public float length2() {
        return Vector3.dst2(p1.x, p1.y, p1.z, p2.x, p2.y, p2.z);
    }

    public float distancePointToSegment(Vector3 p) {
        n.set(p2).sub(p1);
        v.set(p).sub(p1);

        float c1 = v.dot(n);
        if (c1 <= 0)
            return Vector3.dst(p.x, p.y, p.z, p1.x, p1.y, p1.z);
        float c2 = n.dot(n);
        if (c2 <= c1)
            return Vector3.dst(p.x, p.y, p.z, p2.x, p2.y, p2.z);

        float b = c1 / c2;
        return Vector3.dst(p.x, p.y, p.z, p1.x + n.x * b, p1.y + n.y * b, p1.z + n.z * b);
    }

    public float sqDistancePointToSegment(Vector3 p) {
        n.set(p2).sub(p1);
        v.set(p).sub(p1);

        float c1 = v.dot(n);
        if (c1 <= 0)
            return Vector3.dst2(p.x, p.y, p.z, p1.x, p1.y, p1.z);
        float c2 = n.dot(n);
        if (c2 <= c1)
            return Vector3.dst2(p.x, p.y, p.z, p2.x, p2.y, p2.z);

        float b = c1 / c2;
        return Vector3.dst2(p.x, p.y, p.z, p1.x + n.x * b, p1.y + n.y * b, p1.z + n.z * b);
    }

    public Vector3 projectToSegment(Vector3 p) {
        n.set(p2).sub(p1);
        v.set(p).sub(p1);

        float c1 = v.dot(n);
        if (c1 <= 0)
            return p1.cpy();
        float c2 = n.dot(n);
        if (c2 <= c1)
            return p2.cpy();

        float b = c1 / c2;
        return new Vector3(p1.x + n.x * b, p1.y + n.y * b, p1.z + n.z * b);
    }

    public void set(Segment segment) {
        set(segment.p1, segment.p2);
    }

    public void set(Vector3 p0, Vector3 p1) {
        this.p1.set(p0);
        this.p2.set(p1);
    }

    @Override
    public void reset() {
        this.p1.set(0, 0, 0);
        this.p2.set(0, 0, 0);
    }
}
