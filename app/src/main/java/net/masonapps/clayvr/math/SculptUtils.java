package net.masonapps.clayvr.math;

import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;

import net.masonapps.clayvr.mesh.Vertex;

import java.util.List;

/**
 * Created by Bob on 8/23/2017.
 */

public class SculptUtils {
    private static final Vector3 point = new Vector3();
    private static final Vector3 normal = new Vector3();

    public static void setPlaneFromVertices(List<Vertex> vertexList, Plane plane, Ray ray) {
        normal.set(0, 0, 0);
        int n = 0;
        for (Vertex vertex : vertexList) {
            if (ray.direction.dot(vertex.normal) < 0) {
                point.add(vertex.position);
                normal.add(vertex.normal);
                n++;
            }
        }
        point.scl(1f / vertexList.size());
        if (n > 0)
            normal.scl(1f / n);
        plane.set(point, normal);
    }

    public static void setPlaneFromVertices(List<Vertex> vertexList, Plane plane, Vector3 hitPoint, Ray ray) {
        normal.set(0, 0, 0);
        int n = 0;
        for (Vertex v : vertexList) {
            if (ray.direction.dot(v.normal) < 0) {
                normal.add(v.normal);
                n++;
            }
        }
        point.scl(1f / vertexList.size());
        if (n > 0)
            normal.scl(1f / n);
        plane.set(hitPoint, normal);
    }
}
