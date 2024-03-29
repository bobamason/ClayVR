package net.masonapps.clayvr.sculpt;

import android.support.annotation.Nullable;

import net.masonapps.clayvr.bvh.BVH;
import net.masonapps.clayvr.mesh.SculptMeshData;
import net.masonapps.clayvr.mesh.Triangle;
import net.masonapps.clayvr.mesh.Vertex;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * Created by Bob on 7/21/2017.
 */

public class UndoRedoCache {
    private static final int MAX_UNDO_STACK_COUNT = 10;
    private Stack<SaveData[]> undoStack = new Stack<>();
    private Stack<SaveData[]> redoStack = new Stack<>();
    @Nullable
    private SaveData[] current = null;

    public static List<Vertex> applySaveData(SculptMeshData meshData, @Nullable SaveData[] saveData, BVH bvh) {
        if (saveData == null) return null;
        if (meshData.getVertices().length != saveData.length)
            throw new IllegalArgumentException("save data array must be same length as mesh vertices array");
        for (int i = 0; i < saveData.length; i++) {
            final Vertex vertex = meshData.getVertex(i);
            vertex.position.set(saveData[i].position);
            vertex.normal.set(saveData[i].normal);
            vertex.color.set(saveData[i].color);
            vertex.flagNeedsUpdate();
        }

        Arrays.stream(meshData.getTriangles())
                .forEach(Triangle::flagNeedsUpdate);

        bvh.refit();

        Arrays.stream(meshData.getVertices())
                .forEach(Vertex::clearUpdateFlag);
        return Arrays.stream(meshData.getVertices()).collect(Collectors.toList());
    }

    @Nullable
    public SaveData[] undo() {
        if (undoStack.empty()) return null;
        if (current != null)
            redoStack.push(current);
        current = undoStack.pop();
//        log();
        return current;
    }

    public int getUndoCount() {
        return undoStack.size();
    }

    @Nullable
    public SaveData[] redo() {
        if (redoStack.empty()) return null;
        if (current != null)
            undoStack.push(current);
        current = redoStack.pop();
//        log();
        return current;
    }

    public int getRedoCount() {
        return redoStack.size();
    }

    public void save(Vertex[] vertices) {
        if (undoStack.size() >= MAX_UNDO_STACK_COUNT)
            undoStack.remove(0);
        redoStack.clear();
        if (current != null)
            undoStack.push(current);
        current = new SaveData[vertices.length];
        for (int i = 0; i < vertices.length; i++) {
            final Vertex vertex = vertices[i];
            current[i] = new SaveData(vertex);
        }
//        log();
    }

//    protected void log() {
//        Logger.d("undo size: " + undoStack.size() + ", redo size: " + redoStack.size() + ", has current: " + (current != null));
//    }

    public void clear() {
        current = null;
        undoStack.clear();
        redoStack.clear();
    }

}
