package net.masonapps.clayvr.bvh;

import com.badlogic.gdx.math.collision.BoundingBox;

import net.masonapps.clayvr.mesh.SculptMeshData;
import net.masonapps.clayvr.mesh.Triangle;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.Stack;

/**
 * Created by Bob Mason on 10/30/2017.
 */
public class BVHBuilder {
    private static final BoundingBox b1 = new BoundingBox();
    private static final BoundingBox b2 = new BoundingBox();
    private static final BoundingBox tempBounds = new BoundingBox();
    private static final Comparator<Triangle> cmpX = (o1, o2) -> {
        b1.inf();
        o1.extendBounds(b1);
        b2.inf();
        o2.extendBounds(b2);
        float c1 = b1.min.x + (b1.max.x - b1.min.x) / 2;
        float c2 = b2.min.x + (b2.max.x - b2.min.x) / 2;
        return Float.compare(c1, c2);
    };
    private static final Selector selectX = (bounds, split) -> {
        double centroid = bounds.min.x + (bounds.max.x - bounds.min.x) / 2;
        return centroid < split;
    };
    private static final Comparator<Triangle> cmpY = (o1, o2) -> {
        b1.inf();
        o1.extendBounds(b1);
        b2.inf();
        o2.extendBounds(b2);
        float c1 = b1.min.y + (b1.max.y - b1.min.y) / 2;
        float c2 = b2.min.y + (b2.max.y - b2.min.y) / 2;
        return Float.compare(c1, c2);
    };
    private static final Selector selectY = (bounds, split) -> {
        double centroid = bounds.min.y + (bounds.max.y - bounds.min.y) / 2;
        return centroid < split;
    };
    private static final Comparator<Triangle> cmpZ = (o1, o2) -> {
        b1.inf();
        o1.extendBounds(b1);
        b2.inf();
        o2.extendBounds(b2);
        float c1 = b1.min.z + (b1.max.z - b1.min.z) / 2;
        float c2 = b2.min.z + (b2.max.z - b2.min.z) / 2;
        return Float.compare(c1, c2);
    };
    private static final Selector selectZ = (bounds, split) -> {
        double centroid = bounds.min.z + (bounds.max.z - bounds.min.z) / 2;
        return centroid < split;
    };

    private Method method = Method.SAH;
    private int splitLimit = 4;

    public BVHBuilder() {
    }

    public BVHBuilder(Method method, int splitLimit) {
        this.method = method;
        this.splitLimit = splitLimit;
    }

    private static BoundingBox bb(Triangle[] triangles) {

        final BoundingBox boundingBox = new BoundingBox();
        boundingBox.inf();

        for (Triangle triangle : triangles) {
            triangle.extendBounds(boundingBox);
        }
        return boundingBox;
    }

    private static float surfaceArea(BoundingBox bb) {
        float x = bb.max.x - bb.min.x;
        float y = bb.max.y - bb.min.y;
        float z = bb.max.z - bb.min.z;
        return 2f * (y * z + x * z * x * y);
    }

    /**
     * Split a chunk on the major axis.
     */
    private static void splitMidpointMajorAxis(Triangle[] chunk, Stack<Action> actions, Stack<Triangle[]> chunks) {
        BoundingBox bb = bb(chunk);
        float xl = bb.max.x - bb.min.x;
        float yl = bb.max.y - bb.min.y;
        float zl = bb.max.z - bb.min.z;
        float splitPos;
        Selector selector;
        if (xl >= yl && xl >= zl) {
            splitPos = bb.min.x + (bb.max.x - bb.min.x) / 2;
            selector = selectX;
            Arrays.sort(chunk, cmpX);
        } else if (yl >= xl && yl >= zl) {
            splitPos = bb.min.y + (bb.max.y - bb.min.y) / 2;
            selector = selectY;
            Arrays.sort(chunk, cmpY);
        } else {
            splitPos = bb.min.z + (bb.max.z - bb.min.z) / 2;
            selector = selectZ;
            Arrays.sort(chunk, cmpZ);
        }

        int split;
        int end = chunk.length;
        for (split = 1; split < end; ++split) {
            tempBounds.inf();
            if (!selector.select(chunk[split].extendBounds(tempBounds), splitPos)) {
                break;
            }
        }

        actions.push(Action.MERGE);
        Triangle[] cons = new Triangle[split];
        System.arraycopy(chunk, 0, cons, 0, split);
        chunks.push(cons);
        actions.push(Action.PUSH);

        cons = new Triangle[end - split];
        System.arraycopy(chunk, split, cons, 0, end - split);
        chunks.push(cons);
        actions.push(Action.PUSH);
    }

    /**
     * Split a chunk based on Surface Area Heuristic of all possible splits
     */
    private static void splitSAH(Triangle[] chunk, Stack<Action> actions, Stack<Triangle[]> chunks) {
        BoundingBox bounds = new BoundingBox();
        bounds.inf();
        float cmin = Float.POSITIVE_INFINITY;
        int split = 0;
        int end = chunk.length;

        float[] sl = new float[end];
        float[] sr = new float[end];

        Comparator<Triangle> cmp = cmpX;
        Arrays.sort(chunk, cmpX);
        for (int i = 0; i < end - 1; ++i) {
            chunk[i].extendBounds(bounds);
            sl[i] = surfaceArea(bounds);
        }
        bounds.inf();
        for (int i = end - 1; i > 0; --i) {
            chunk[i].extendBounds(bounds);
            sr[i - 1] = surfaceArea(bounds);
        }
        for (int i = 0; i < end - 1; ++i) {
            float c = sl[i] * (i + 1) + sr[i] * (end - i - 1);
            if (c < cmin) {
                cmin = c;
                split = i;
            }
        }

        Arrays.sort(chunk, cmpY);
        for (int i = 0; i < end - 1; ++i) {
            chunk[i].extendBounds(bounds);
            sl[i] = surfaceArea(bounds);
        }
        bounds.inf();
        for (int i = end - 1; i > 0; --i) {
            chunk[i].extendBounds(bounds);
            sr[i - 1] = surfaceArea(bounds);
        }
        for (int i = 0; i < end - 1; ++i) {
            float c = sl[i] * (i + 1) + sr[i] * (end - i - 1);
            if (c < cmin) {
                cmin = c;
                split = i;
                cmp = cmpY;
            }
        }

        Arrays.sort(chunk, cmpZ);
        for (int i = 0; i < end - 1; ++i) {
            chunk[i].extendBounds(bounds);
            sl[i] = surfaceArea(bounds);
        }
        bounds.inf();
        for (int i = end - 1; i > 0; --i) {
            chunk[i].extendBounds(bounds);
            sr[i - 1] = surfaceArea(bounds);
        }
        for (int i = 0; i < end - 1; ++i) {
            float c = sl[i] * (i + 1) + sr[i] * (end - i - 1);
            if (c < cmin) {
                cmin = c;
                split = i;
                cmp = cmpZ;
            }
        }

        if (cmp != cmpZ) {
            Arrays.sort(chunk, cmp);
        }

        split += 1;

        actions.push(Action.MERGE);
        Triangle[] cons = new Triangle[split];
        System.arraycopy(chunk, 0, cons, 0, split);
        chunks.push(cons);
        actions.push(Action.PUSH);

        cons = new Triangle[end - split];
        System.arraycopy(chunk, split, cons, 0, end - split);
        chunks.push(cons);
        actions.push(Action.PUSH);
    }

    /**
     * Split a chunk based on Surface Area Heuristic of all possible splits.
     */
    private static void splitSAH_MA(Triangle[] chunk, Stack<Action> actions, Stack<Triangle[]> chunks) {
        BoundingBox bb = bb(chunk);
        float xl = bb.max.x - bb.min.x;
        float yl = bb.max.y - bb.min.y;
        float zl = bb.max.z - bb.min.z;
        Comparator<Triangle> cmp;
        if (xl >= yl && xl >= zl) {
            cmp = cmpX;
            Arrays.sort(chunk, cmpX);
        } else if (yl >= xl && yl >= zl) {
            cmp = cmpY;
        } else {
            cmp = cmpZ;
        }

        BoundingBox bounds = new BoundingBox();
        bounds.inf();
        double cmin = Double.POSITIVE_INFINITY;
        int split = 0;
        int end = chunk.length;

        double[] sl = new double[end];
        double[] sr = new double[end];

        Arrays.sort(chunk, cmp);
        for (int i = 0; i < end - 1; ++i) {
            chunk[i].extendBounds(bounds);
            sl[i] = surfaceArea(bounds);
        }
        bounds.inf();
        for (int i = end - 1; i > 0; --i) {
            chunk[i].extendBounds(bounds);
            sr[i - 1] = surfaceArea(bounds);
        }
        for (int i = 0; i < end - 1; ++i) {
            double c = sl[i] * (i + 1) + sr[i] * (end - i - 1);
            if (c < cmin) {
                cmin = c;
                split = i;
            }
        }

        split += 1;

        actions.push(Action.MERGE);
        Triangle[] cons = new Triangle[split];
        System.arraycopy(chunk, 0, cons, 0, split);
        chunks.push(cons);
        actions.push(Action.PUSH);

        cons = new Triangle[end - split];
        System.arraycopy(chunk, split, cons, 0, end - split);
        chunks.push(cons);
        actions.push(Action.PUSH);
    }

    public BVH.Node build(SculptMeshData meshData) {
        final Triangle[] triangleArray = new Triangle[meshData.getTriangleCount()];
        System.arraycopy(meshData.getTriangles(), 0, triangleArray, 0, triangleArray.length);

        final Random random = new Random(420);
        for (int i = triangleArray.length; i > 1; i--) {
            final int j = random.nextInt(i);
            Triangle tmp = triangleArray[i - 1];
            triangleArray[i - 1] = triangleArray[j];
            triangleArray[j] = tmp;

        }

        BVH.Node root = null;
        switch (method) {
            case MIDPOINT:
                root = constructMidpointSplit(triangleArray);
                break;
            case SAH:
                root = constructSAH(triangleArray);
                break;
            case SAH_MA:
                root = constructSAH_MA(triangleArray);
                break;
        }

        return root;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public void setSplitLimit(int splitLimit) {
        this.splitLimit = splitLimit;
    }

    /**
     * Simple BVH construction using splitting by major axis.
     *
     * @return root node of constructed BVH
     */
    private BVH.Node constructMidpointSplit(Triangle[] triangles) {
        Stack<BVH.Node> nodes = new Stack<>();
        Stack<Action> actions = new Stack<>();
        Stack<Triangle[]> chunks = new Stack<>();
        chunks.push(triangles);
        actions.push(Action.PUSH);
        while (!actions.isEmpty()) {
            Action action = actions.pop();
            if (action == Action.MERGE) {
                nodes.push(new BVH.Group(nodes.pop(), nodes.pop()));
            } else {
                Triangle[] chunk = chunks.pop();
                if (chunk.length <= splitLimit) {
                    nodes.push(new BVH.Leaf(chunk));
                } else {
                    splitMidpointMajorAxis(chunk, actions, chunks);
                }
            }
        }
        return nodes.pop();
    }

    /**
     * Construct a BVH using Surface Area Heuristic (SAH).
     *
     * @return root node of constructed BVH
     */
    private BVH.Node constructSAH(Triangle[] triangles) {
        Stack<BVH.Node> nodes = new Stack<>();
        Stack<Action> actions = new Stack<>();
        Stack<Triangle[]> chunks = new Stack<>();
        chunks.push(triangles);
        actions.push(Action.PUSH);
        while (!actions.isEmpty()) {
            Action action = actions.pop();
            if (action == Action.MERGE) {
                nodes.push(new BVH.Group(nodes.pop(), nodes.pop()));
            } else {
                Triangle[] chunk = chunks.pop();
                if (chunk.length <= splitLimit) {
                    nodes.push(new BVH.Leaf(chunk));
                } else {
                    splitSAH(chunk, actions, chunks);
                }
            }
        }
        return nodes.pop();
    }

    /**
     * Construct a BVH using Surface Area Heuristic (SAH)
     *
     * @return root node of constructed BVH
     */
    private BVH.Node constructSAH_MA(Triangle[] triangles) {
        Stack<BVH.Node> nodes = new Stack<>();
        Stack<Action> actions = new Stack<>();
        Stack<Triangle[]> chunks = new Stack<>();
        chunks.push(triangles);
        actions.push(Action.PUSH);
        while (!actions.isEmpty()) {
            Action action = actions.pop();
            if (action == Action.MERGE) {
                nodes.push(new BVH.Group(nodes.pop(), nodes.pop()));
            } else {
                Triangle[] chunk = chunks.pop();
                if (chunk.length <= splitLimit) {
                    nodes.push(new BVH.Leaf(chunk));
                } else {
                    splitSAH_MA(chunk, actions, chunks);
                }
            }
        }
        return nodes.pop();
    }

    public enum Method {
        MIDPOINT,
        SAH,
        SAH_MA,
    }

    enum Action {
        PUSH,
        MERGE,
    }

    interface Selector {
        boolean select(BoundingBox bounds, double split);
    }
}