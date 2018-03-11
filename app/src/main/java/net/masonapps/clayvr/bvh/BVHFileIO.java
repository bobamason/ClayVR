package net.masonapps.clayvr.bvh;

import android.support.annotation.Nullable;

import net.masonapps.clayvr.mesh.SculptMeshData;
import net.masonapps.clayvr.mesh.Triangle;

import org.masonapps.libgdxgooglevr.utils.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.Stack;

/**
 * Created by Bob Mason on 10/30/2017.
 */
public class BVHFileIO {

    public static final char GROUP = 'g';
    public static final char NULL = '/';
    public static final char LEAF = 'l';
    public static final char START = '(';
    public static final char END = ')';
    public static final String COMMA = ",";
    public static final String EXTENSION = "bvh";

    public static void serialize(BVH bvh, File file) throws IOException {
        if (!file.getParentFile().exists())
            file.getParentFile().mkdirs();
        if (!file.exists())
            file.createNewFile();
        serialize(bvh, new FileOutputStream(file));
    }

    public static void serialize(BVH bvh, OutputStream outputStream) throws IOException {
        final BufferedWriter stream = new BufferedWriter(new OutputStreamWriter(outputStream));
        try {
            writeNode(bvh.root, stream);
            stream.flush();
        } finally {
            stream.close();
        }
    }

    private static void writeNode(@Nullable BVH.Node node, BufferedWriter writer) throws IOException {
        if (node == null) {
            writer.write(NULL);
        } else if (node instanceof BVH.Group) {
            if (node.triangles != null && node.triangles.length > 0)
                Logger.e("group contains " + node.triangles.length + " triangles");
            writer.write(GROUP);
            final BVH.Node child1 = ((BVH.Group) node).child1;
            final BVH.Node child2 = ((BVH.Group) node).child2;
                writeNode(child1, writer);
                writeNode(child2, writer);
        } else if (node instanceof BVH.Leaf) {
            writer.write(LEAF);
            writer.write(START);
            for (int i = 0; i < node.triangles.length; i++) {
                writer.write(Integer.toString(node.triangles[i].index));
                if (i != node.triangles.length - 1)
                    writer.write(COMMA);
            }
            writer.write(END);
        }
    }

    public static BVH deserialize(SculptMeshData meshData, File file) throws IOException {
        return deserialize(meshData, new FileInputStream(file));
    }

    public static BVH deserialize(SculptMeshData meshData, InputStream outputStream) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(outputStream));
        try {
            final BVH.Node root = deserializeNode(meshData, reader);
            final BVH bvh = new BVH(meshData, root);
            bvh.refit();
            return bvh;
        } finally {
            reader.close();
        }
    }
    private static BVH.Node deserializeNode(SculptMeshData meshData, Reader reader) throws IOException {
        BVH.Node node = null;
        try {
            final char nextChar = (char) reader.read();
            switch (nextChar) {
                case NULL:
                    node = null;
                    break;
                case GROUP:
                    node = new BVH.Group();

                    ((BVH.Group) node).child1 = deserializeNode(meshData, reader);
                    if (((BVH.Group) node).child1 != null)
                        ((BVH.Group) node).child1.parent = node;

                    ((BVH.Group) node).child2 = deserializeNode(meshData, reader);
                    if (((BVH.Group) node).child2 != null)
                        ((BVH.Group) node).child2.parent = node;
                    break;
                case LEAF:
                    StringBuilder sb = new StringBuilder();
                    if (((char) reader.read()) == START) {
                        char c;
                        while ((c = (char) reader.read()) != END) {
                            sb.append(c);
                        }
                        node = parseLeaf(sb.toString(), meshData);
                        sb.delete(0, sb.length());
                    }
                    break;
            }
        } catch (EOFException e) {
            node = null;
        } catch (StackOverflowError soe) {
            node = null;
            Logger.e("bvh read failed", soe);
        }
        if (node != null)
            node.flagNeedsRefit();
        return node;
    }

    private static BVH.Node deserializeNodeNonRecursive(SculptMeshData meshData, Reader reader) throws IOException {
        BVH.Node root = null;
        Stack<BVH.Group> stack = new Stack<>();
        try {
            final char startChar = (char) reader.read();
            switch (startChar) {
                case GROUP:
                    root = new BVH.Group();
                    stack.push((BVH.Group) root);
                    break;
                case LEAF:
                    StringBuilder sb = new StringBuilder();
                    if (((char) reader.read()) == START) {
                        char c;
                        while ((c = (char) reader.read()) != END) {
                            sb.append(c);
                        }
                        root = parseLeaf(sb.toString(), meshData);
                        sb.delete(0, sb.length());
                    }
                    return root;
                case NULL:
                    return null;
            }

            while (!stack.isEmpty()) {
                BVH.Group node = stack.pop();
                switch ((char) reader.read()) {
                    case NULL:
                        if (node.child1 == null)
                            node.child1 = new BVH.Leaf(new Triangle[0]);
                        else if (node.child2 == null)
                            node.child2 = new BVH.Leaf(new Triangle[0]);
                        break;
                    case GROUP:

                        final BVH.Group group = new BVH.Group();
                        if (node.child1 == null)
                            node.child1 = group;
                        else if (node.child2 == null)
                            node.child2 = group;
                        node.child1 = group;
                        group.parent = node;
                        stack.push(group);
                        break;
                    case LEAF:
                        StringBuilder sb = new StringBuilder();
                        if (((char) reader.read()) == START) {
                            char c;
                            while ((c = (char) reader.read()) != END) {
                                sb.append(c);
                            }

                            final BVH.Leaf leaf = parseLeaf(sb.toString(), meshData);
                            if (node.child1 == null)
                                node.child1 = leaf;
                            else if (node.child2 == null)
                                node.child2 = leaf;
                            leaf.parent = node;

                            sb.delete(0, sb.length());
                        }
                        break;
                }
            }
        } catch (EOFException ignored) {
        }
        if (root != null)
            root.flagNeedsRefit();
        return root;
    }

    private static BVH.Leaf parseLeaf(String s, SculptMeshData meshData) {
        final String[] split = s.split(COMMA);
        final int triCount = split.length;
        final Triangle[] triangles = new Triangle[triCount];
        for (int i = 0; i < triCount; i++) {
            final int index = Integer.parseInt(split[i]);
            triangles[i] = meshData.getTriangle(index);
        }
        return new BVH.Leaf(triangles);
    }
}