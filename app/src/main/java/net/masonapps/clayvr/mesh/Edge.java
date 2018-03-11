package net.masonapps.clayvr.mesh;

/**
 * Created by Bob on 7/12/2017.
 */

public class Edge {
    public final Vertex v1;
    public final Vertex v2;
    private final int hashCode;

    public Edge(Vertex v1, Vertex v2) {
        this.v1 = v1;
        this.v2 = v2;
        v1.addEdge(this);
        v2.addEdge(this);
        hashCode = createHash();
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    protected int createHash() {
        if (v1.index < v2.index)
            return v1.index << 16 | (v2.index & 0xFFFF);
        else
            return v2.index << 16 | (v1.index & 0xFFFF);
    }
}
