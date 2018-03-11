package net.masonapps.clayvr.mesh;

import android.support.annotation.Nullable;

import java.util.Collection;
import java.util.HashSet;

/**
 * Created by Bob on 7/24/2017.
 */

public class SculptMeshData {
    public final Vertex[] vertices;
    public final Triangle[] triangles;
    public final Collection<Edge> edges;
    @Nullable
    private String originalAssetName = null;
    private boolean symmetryEnabled = true;

    public SculptMeshData(Vertex[] vertices, Triangle[] triangles) {
        this.vertices = vertices;
        this.triangles = triangles;
        edges = new HashSet<>(vertices.length + 1);
//        Arrays.stream(triangles)
//                .forEach(triangle -> {
//                    if (!edges.contains(triangle.e1))
//                        edges.add(triangle.e1);
//                    if (!edges.contains(triangle.e2))
//                        edges.add(triangle.e2);
//                    if (!edges.contains(triangle.e3))
//                        edges.add(triangle.e3);
//                });
    }

    public SculptMeshData(SculptMeshData other) {
        this(other.vertices, other.triangles);
        this.originalAssetName = other.originalAssetName;
        this.symmetryEnabled = other.symmetryEnabled;
    }

    public Triangle[] getTriangles() {
        return triangles;
    }

    public Vertex getVertex(int i) {
        return vertices[i];
    }

    public Vertex[] getVertices() {
        return vertices;
    }

    @Nullable
    public String getOriginalAssetName() {
        return originalAssetName;
    }

    public void setOriginalAssetName(@Nullable String originalAssetName) {
        this.originalAssetName = originalAssetName;
    }

    public int getVertexCount() {
        return vertices.length;
    }

    public int getTriangleCount() {
        return triangles.length;
    }

    public boolean isSymmetryEnabled() {
        return symmetryEnabled;
    }

    public void setSymmetryEnabled(boolean symmetryEnabled) {
        this.symmetryEnabled = symmetryEnabled;
    }

    public short[] getSymmetryArray() {
        final short[] sym = new short[vertices.length];
        for (int i = 0; i < sym.length; i++) {
            final Vertex symmetricPair = vertices[i].symmetricPair;
            if (symmetricPair != null)
                sym[i] = (short) symmetricPair.index;
            else
                sym[i] = (short) -1;
        }
        return sym;
    }

    public SculptMeshData copy() {
        return new SculptMeshData(this);
    }

    public Triangle getTriangle(int index) {
        return triangles[index];
    }
}
