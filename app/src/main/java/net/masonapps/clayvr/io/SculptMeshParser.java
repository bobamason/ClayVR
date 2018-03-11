package net.masonapps.clayvr.io;

import net.masonapps.clayvr.mesh.SculptMeshData;
import net.masonapps.clayvr.mesh.Triangle;
import net.masonapps.clayvr.mesh.Vertex;

import org.masonapps.libgdxgooglevr.utils.Logger;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Bob on 7/27/2017.
 */

public class SculptMeshParser {

    public static SculptMeshData parse(File file) throws IOException {
        return parse(new FileInputStream(file));
    }

    public static SculptMeshData parse(InputStream inputStream) throws IOException {
        final DataInputStream stream = new DataInputStream(new BufferedInputStream(inputStream));
        String assetName = null;
        boolean symmetryEnabled = true;

        Vertex[] vertices = new Vertex[0];
        Triangle[] triangles = new Triangle[0];
        try {
            final String header = readHeader(stream);
            Logger.d("file header: " + header);
            final String[] lines = header.split("\n");
            String version = SculptMeshWriter.VERSION_1;
            for (String line : lines) {
                if (line.startsWith(SculptMeshWriter.VERSION)) {
                    version = parseVersion(line);
                } else if (line.startsWith(SculptMeshWriter.ASSET)) {
                    assetName = parseAssetName(line);
                } else if (line.startsWith(SculptMeshWriter.SYMMETRY)) {
                    symmetryEnabled = parseSymmetry(line);
                }
            }
            Logger.d("version: " + version);
            Logger.d("original asset: " + assetName);

            final int vertexCount = stream.readInt();
            final int triangleCount = stream.readInt();
            Logger.d("vertexCount: " + vertexCount);
            Logger.d("triangleCount: " + triangleCount);
            vertices = new Vertex[vertexCount];
            triangles = new Triangle[triangleCount];

            for (int i = 0; i < vertexCount; i++) {
                vertices[i] = parseVertex(stream, i);
            }
            Logger.d("parse vertices complete");

            for (int i = 0; i < triangleCount; i++) {
                final Triangle triangle = parseTriangle(stream, vertices);
                triangle.index = i;
                triangles[i] = triangle;
            }
            Logger.d("parse triangles complete");

            for (int i = 0; i < vertexCount; i++) {
                final short j = stream.readShort();
                if (j >= 0 && j < vertices.length)
                    vertices[i].symmetricPair = vertices[j];
            }
            Logger.d("parse symmetry complete");
        } finally {
            stream.close();
        }
        final SculptMeshData meshData = new SculptMeshData(vertices, triangles);
        meshData.setOriginalAssetName(assetName);
        meshData.setSymmetryEnabled(symmetryEnabled);
        return meshData;
    }

    protected static String parseVersion(String line) {
        final int i = line.lastIndexOf(' ');
        if (i != -1 && i + 1 < line.length())
            return line.substring(i + 1);
        else
            return SculptMeshWriter.VERSION_1;
    }

    protected static String parseAssetName(String line) {
        final int i = line.lastIndexOf(' ');
        if (i != -1 && i + 1 < line.length())
            return line.substring(i + 1);
        else
            return null;
    }

    protected static boolean parseSymmetry(String line) {
        final int i = line.lastIndexOf(' ');
        if (i != -1 && i + 1 < line.length())
            return Boolean.valueOf(line.substring(i + 1));
        else
            return true;
    }

    private static Vertex parseVertex(DataInputStream stream, int index) throws IOException {
        final Vertex vertex = new Vertex();
        vertex.index = index;
        vertex.position.set(stream.readFloat(), stream.readFloat(), stream.readFloat());
        vertex.normal.set(stream.readFloat(), stream.readFloat(), stream.readFloat());
        vertex.uv.set(stream.readFloat(), stream.readFloat());
        final int c = Float.floatToRawIntBits(stream.readFloat());
        vertex.color.a = ((c & 0xff000000) >>> 24) / 255f;
        vertex.color.b = ((c & 0x00ff0000) >>> 16) / 255f;
        vertex.color.g = ((c & 0x0000ff00) >>> 8) / 255f;
        vertex.color.r = ((c & 0x000000ff)) / 255f;
        return vertex;
    }

    private static Triangle parseTriangle(DataInputStream stream, Vertex[] vertexArray) throws IOException {
        final short ia = stream.readShort();
        final short ib = stream.readShort();
        final short ic = stream.readShort();
        return new Triangle(vertexArray[ia], vertexArray[ib], vertexArray[ic]);
    }

    private static String readHeader(DataInputStream stream) throws IOException {
        final StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < SculptMeshWriter.HEADER_LENGTH; i++) {
            final char c = stream.readChar();
            if (c != '\0')
                stringBuilder.append(c);
        }
        return stringBuilder.toString();
    }
}
