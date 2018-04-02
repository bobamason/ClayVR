package net.masonapps.clayvr.mesh;

import android.support.annotation.Nullable;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import net.masonapps.clayvr.sculpt.SaveData;

import java.util.Arrays;

/**
 * Created by Bob on 5/11/2017.
 */

public class Vertex {
    public static final int FLAG_UPDATE = 2;
    public static final int FLAG_POSITION_SAVED = 4;
    public static final int FLAG_SKIP_SPHERE_TEST = 8;
    public final Vector3 position = new Vector3();
    public final Vector3 normal = new Vector3();
    public final Vector2 uv = new Vector2();
    public final SaveData savedState;
    public Color color = Color.GRAY.cpy();
    public int index = -1;
    public float tmpVal = 0f;
    public Triangle[] triangles = new Triangle[0];
    public Vertex[] adjacentVertices = new Vertex[0];
    @Nullable
    public Vertex symmetricPair = null;
    private Edge[] edges = new Edge[0];
    private boolean isChanged = true;
    private boolean isPositionSaved = false;
    private boolean needsUpdate = false;
    private boolean shouldSkipSphereTest = false;

    public Vertex() {
        savedState = new SaveData(this);
    }

    private static boolean isDuplicateVertex(Vertex[] vertices, Vertex vertex) {
        for (int i = 0; i < vertices.length; i++) {
            if (vertices[i].index == vertex.index)
                return true;
        }
        return false;
    }

    public void addTriangle(Triangle triangle) {
        final int length = triangles.length;
        triangles = Arrays.copyOf(triangles, length + 1);
        triangles[length] = triangle;
    }

    public Vertex set(Vertex vertex) {
        position.set(vertex.position);
        normal.set(vertex.normal);
        uv.set(vertex.uv);
        color.set(vertex.color);
        index = vertex.index;
        symmetricPair = vertex.symmetricPair;
        triangles = Arrays.copyOf(vertex.triangles, vertex.triangles.length);
        edges = Arrays.copyOf(vertex.edges, vertex.edges.length);
        return this;
    }

    public Vertex lerp(Vertex vertex, float t) {
        position.lerp(vertex.position, t);
        normal.lerp(vertex.normal, t);
        uv.lerp(vertex.uv, t);
        color.lerp(vertex.color, t);
        return this;
    }

    public void recalculateNormal() {
        normal.set(0, 0, 0);
        for (Triangle triangle : triangles) {
            normal.add(triangle.plane.normal).scl(triangle.getWeight(this));
        }
        normal.nor();
    }

    public void addEdge(Edge edge) {
        final int length = edges.length;
        edges = Arrays.copyOf(edges, length + 1);
        edges[length] = edge;
        updateAdjacentVertices();
    }

    public Vertex[] getAdjacentVertices() {
        return adjacentVertices;
    }

    private void updateAdjacentVertices() {
        adjacentVertices = new Vertex[0];
        for (Edge edge : edges) {
            final Vertex v1 = edge.v1;
            final Vertex v2 = edge.v2;
            if (v1 != this && !isDuplicateVertex(adjacentVertices, v1)) {
                final int length = adjacentVertices.length;
                adjacentVertices = Arrays.copyOf(adjacentVertices, length + 1);
                adjacentVertices[length] = v1;
            }
            if (v2 != this && !isDuplicateVertex(adjacentVertices, v2)) {
                final int length = adjacentVertices.length;
                adjacentVertices = Arrays.copyOf(adjacentVertices, length + 1);
                adjacentVertices[length] = v2;
            }
        }
    }

    public void savePosition() {
        savedState.set(this);
        isPositionSaved = true;
    }

    public void clearSavedFlag() {
        isPositionSaved = false;
    }

    public void clearUpdateFlag() {
        needsUpdate = false;
    }

    public boolean needsUpdate() {
        return needsUpdate;
    }

    public boolean isChanged() {
        return isChanged;
    }

    public void clearChangedFlag() {
        isChanged = false;
    }

    public boolean shouldSkipSphereTest() {
        return shouldSkipSphereTest;
    }

    public boolean isPositionSaved() {
        return isPositionSaved;
    }

    public void clearFlagSkipSphereTest() {
        shouldSkipSphereTest = false;
    }

    public void flagNeedsUpdate() {
        needsUpdate = true;
        isChanged = true;
    }

    public void flagSkipSphereTest() {
        shouldSkipSphereTest = true;
    }
}
