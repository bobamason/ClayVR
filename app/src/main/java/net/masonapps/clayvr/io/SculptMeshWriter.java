package net.masonapps.clayvr.io;

import android.util.Log;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

import net.masonapps.clayvr.Constants;
import net.masonapps.clayvr.mesh.SculptMeshData;
import net.masonapps.clayvr.mesh.Triangle;
import net.masonapps.clayvr.mesh.Vertex;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Created by Bob on 7/27/2017.
 */

public class SculptMeshWriter {

    public static final int HEADER_LENGTH = 80;
    public static final String VERSION = "version";
    public static final String ASSET = "asset";
    public static final String SYMMETRY = "symmetry";
    public static final String VERSION_1 = "1";

    public static void writeToFile(File file, SculptMeshData meshData, Matrix4 transform) throws IOException {
        writeToOutputStream(new FileOutputStream(file), meshData, transform);
    }

    public static void writeToOutputStream(OutputStream outputStream, SculptMeshData meshData, Matrix4 transform) throws IOException {
        final DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(outputStream));
        try {
            writeHeader(stream, meshData);
            stream.writeInt(meshData.vertices.length);
            stream.writeInt(meshData.triangles.length);

            final Vector3 pos = new Vector3();
            final Vector3 nor = new Vector3();

            for (int i = 0; i < meshData.vertices.length; i++) {
                final Vertex v = meshData.vertices[i];

                pos.set(v.position).mul(transform);
                nor.set(v.normal).rot(transform).nor();

                stream.writeFloat(pos.x);
                stream.writeFloat(pos.y);
                stream.writeFloat(pos.z);
                stream.writeFloat(nor.x);
                stream.writeFloat(nor.y);
                stream.writeFloat(nor.z);
                stream.writeFloat(v.uv.x);
                stream.writeFloat(v.uv.y);
                stream.writeFloat(v.color.toFloatBits());
            }

            Log.d(SculptMeshWriter.class.getSimpleName(), meshData.vertices.length + " vertices written to file");

            for (int i = 0; i < meshData.triangles.length; i++) {
                final Triangle t = meshData.triangles[i];
                stream.writeShort(t.v1.index);
                stream.writeShort(t.v2.index);
                stream.writeShort(t.v3.index);
            }


            for (int i = 0; i < meshData.vertices.length; i++) {
                final Vertex symmetricVertex = meshData.vertices[i].symmetricPair;
                stream.writeShort(symmetricVertex != null ? symmetricVertex.index : -1);
            }

            Log.d(SculptMeshWriter.class.getSimpleName(), (meshData.triangles.length * 3) + " indices written to file");
        } finally {
            stream.flush();
            stream.close();
        }
    }

    private static void writeHeader(DataOutputStream stream, SculptMeshData meshData) throws IOException {
        final char[] chars = new char[HEADER_LENGTH];
        Arrays.fill(chars, '\0');
        final String s = "created by " + Constants.APP_NAME + "\n" +
                VERSION + " " + VERSION_1 + "\n" +
                ASSET + " " + meshData.getOriginalAssetName() + "\n" +
                SYMMETRY + " " + Boolean.toString(meshData.isSymmetryEnabled()) + "\n";
        s.getChars(0, s.length(), chars, 0);
        for (int i = 0; i < chars.length; i++) {
            stream.writeChar(chars[i]);
        }
    }
}
