package net.masonapps.clayvr.io;

import android.util.Log;

import com.badlogic.gdx.math.Vector3;

import net.masonapps.clayvr.mesh.SculptMeshData;
import net.masonapps.clayvr.mesh.Vertex;

import java.io.IOException;

/**
 * Created by Bob on 8/18/2017.
 */

public class SymmetryMapUtils {


    public static void createSymmetry(Vertex[] vertices, float tolerance) {
        int count = 0;
        boolean found;
        final Vector3 p = new Vector3();
        final Vector3 p2 = new Vector3();
        for (int i = 0; i < vertices.length; i++) {
            p.set(vertices[i].position);
            found = false;
            for (int j = 0; j < vertices.length; j++) {
                p2.set(vertices[j].position).scl(-1, 1, 1);
                if (i != j && p.dst(p2) <= tolerance) {
                    vertices[i].symmetricPair = vertices[j];
                    vertices[j].symmetricPair = vertices[i];
                    found = true;
                    break;
                }
            }
            if (found || Math.abs(p.x) <= tolerance)
                count++;
        }
        if (count != vertices.length)
            Log.e(SymmetryMapUtils.class.getSimpleName(), "mesh was not symmetrical, " + (vertices.length - count) + " vertices do not have symmetrical pairs");
        else
            Log.d(SymmetryMapUtils.class.getSimpleName(), "mesh is symmetrical");
    }

    public static short[] extractSymmetryMap(SculptMeshData meshData) throws IOException {
        final int n = meshData.getVertexCount();
        final short[] indexPairs = new short[n];
        for (int i = 0; i < n; i++) {
            final Vertex symmetricVertex = meshData.getVertices()[i].symmetricPair;
            if (symmetricVertex != null)
                indexPairs[i] = (short) symmetricVertex.index;
            else
                indexPairs[i] = -1;
        }
        return indexPairs;
    }
}
