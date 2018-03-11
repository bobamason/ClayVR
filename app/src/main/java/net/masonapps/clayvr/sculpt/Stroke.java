package net.masonapps.clayvr.sculpt;

import com.badlogic.gdx.math.Vector3;

import net.masonapps.clayvr.math.Segment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Bob on 7/20/2017.
 */

public class Stroke {

    private static final Vector3 n = new Vector3();
    private static final Vector3 v = new Vector3();

    private List<Vector3> points = new ArrayList<>();

    public Stroke() {
    }

    /**
     * used for distance to stroke path
     *
     * @param p
     * @param segment
     * @param includeStart if false it returns -1 if the point is before the start
     * @param includeEnd   if false it returns -1 if the point is passed the end
     * @return distance from point to line segment or -1 if it is passed the excluded ends of the segment
     */
    public static float distancePointToSegment(Vector3 p, Segment segment, boolean includeStart, boolean includeEnd) {
        final Vector3 p1 = segment.p1;
        final Vector3 p2 = segment.p2;
        n.set(p2).sub(p1);
        v.set(p).sub(p1);

        float c1 = v.dot(n);
        if (c1 <= 0)
            return includeStart ? Vector3.dst(p.x, p.y, p.z, p1.x, p1.y, p1.z) : -1;
        float c2 = n.dot(n);
        if (c2 <= c1)
            return includeEnd ? Vector3.dst(p.x, p.y, p.z, p2.x, p2.y, p2.z) : -1;

        float b = c1 / c2;
        return Vector3.dst(p.x, p.y, p.z, p1.x + n.x * b, p1.y + n.y * b, p1.z + n.z * b);
    }

    public void addPoint(Vector3 point) {
        points.add(point.cpy());
    }

    public boolean isEmpty() {
        return points.isEmpty();
    }

    public int getPointCount() {
        return points.size();
    }

    public void clear() {
        points.clear();
    }

    public Vector3 getPoint(int index) {
        return points.get(index);
    }

    public Vector3 getStartPoint() {
        return points.get(0);
    }

    public Vector3 getEndPoint() {
        return points.get(points.size() - 1);
    }

    public void simplifyByPerpendicularDistance(float minDist) {
        if (points.size() < 3) return;
        final Segment segment = new Segment();
        final float minSqDist = minDist * minDist;
        int i = 2;
        while (i < points.size() - 1) {
            segment.p1.set(points.get(i - 1));
            segment.p2.set(points.get(i + 1));
            if (segment.sqDistancePointToSegment(points.get(i)) < minSqDist) {
                points.remove(i);
            } else {
                i++;
            }
        }
    }

    public void simplifyBySegmentLength(float minDist) {
        if (points.size() < 2) return;
        final Segment segment = new Segment();
        final float minSqDist = minDist * minDist;
        int i = 1;
        while (i < points.size()) {
            segment.p1.set(points.get(i - 1));
            segment.p2.set(points.get(i));
            if (segment.length2() < minSqDist) {
                points.remove(i);
            } else {
                i++;
            }
        }
    }

    /**
     * used for distance to stroke path
     *
     * @param p
     * @param segment
     * @param includeStart if false it returns -1 if the point is before the start
     * @param includeEnd   if false it returns -1 if the point is passed the end
     * @return squared distance from point to line segment or -1 if it is passed the excluded ends of the segment
     */
    public float sqDistancePointToSegment(Vector3 p, Segment segment, boolean includeStart, boolean includeEnd) {
        final Vector3 p1 = segment.p1;
        final Vector3 p2 = segment.p2;
        n.set(p2).sub(p1);
        v.set(p).sub(p1);

        float c1 = v.dot(n);
        if (c1 <= 0)
            return includeStart ? Vector3.dst2(p.x, p.y, p.z, p1.x, p1.y, p1.z) : -1;
        float c2 = n.dot(n);
        if (c2 <= c1)
            return includeEnd ? Vector3.dst2(p.x, p.y, p.z, p2.x, p2.y, p2.z) : -1;

        float b = c1 / c2;
        return Vector3.dst2(p.x, p.y, p.z, p1.x + n.x * b, p1.y + n.y * b, p1.z + n.z * b);
    }
}
