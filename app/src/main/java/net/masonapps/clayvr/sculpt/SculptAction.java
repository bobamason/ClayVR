package net.masonapps.clayvr.sculpt;

import android.support.annotation.NonNull;

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

    public SculptAction(List<Vertex> vertices, Brush brush) {
        this.vertices = new ArrayList<>(vertices.size());
        this.brush = new Brush(brush);
        this.vertices.addAll(vertices);
        timestamp = System.nanoTime();
    }

    public void apply() {
        vertices.forEach(brush::applyBrushToVertex);
    }

    @Override
    public int compareTo(@NonNull SculptAction o) {
        return Long.compare(this.timestamp, o.timestamp);
    }
}
