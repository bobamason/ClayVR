package net.masonapps.clayvr.io;

import android.annotation.SuppressLint;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.NumberUtils;

import net.masonapps.clayvr.Constants;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Locale;

/**
 * Created by Bob on 7/11/2017.
 */

public class PLYWriter {

    public static void writeToFile(File file, float[] vertices, short[] indices, int vertexSize, Matrix4 transform) throws IOException {
        writeToOutputStream(new FileOutputStream(file), vertices, indices, vertexSize, transform);
    }

    @SuppressLint("DefaultLocale")
    public static void writeToOutputStream(OutputStream outputStream, float[] vertices, short[] indices, int vertexSize, Matrix4 transform) throws IOException {
        final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
        try {
            writer.write("ply");
            writer.newLine();
            writer.write("format ascii 1.0");
            writer.newLine();
            writer.write("comment Created by " + Constants.APP_NAME);
            writer.newLine();

            final String vs = "element vertex " + vertices.length;
            vs.replace(",", "");
            writer.write(vs);
            writer.newLine();

            writer.write("property float x");
            writer.newLine();
            writer.write("property float y");
            writer.newLine();
            writer.write("property float z");
            writer.newLine();

            if (vertexSize > 3) {
                writer.write("property float nx");
                writer.newLine();
                writer.write("property float ny");
                writer.newLine();
                writer.write("property float nz");
                writer.newLine();
            }

            if (vertexSize > 8) {
                writer.write("property uchar red");
                writer.newLine();
                writer.write("property uchar green");
                writer.newLine();
                writer.write("property uchar blue");
                writer.newLine();
            }

            writer.write("element face " + (indices.length / 3));
            writer.newLine();
            writer.write("property list uchar uint vertex_indices");
            writer.newLine();
            writer.write("end_header");
            writer.newLine();
            final Vector3 pos = new Vector3();
            final Vector3 nor = new Vector3();

            for (int i = 0; i < vertices.length; i += vertexSize) {
                pos.set(vertices[i], vertices[i + 1], vertices[i + 2]).mul(transform);
                writer.write(String.format(Locale.US, "%f %f %f",
                        pos.x,
                        pos.y,
                        pos.z));
                if (vertexSize > 3) {
                    nor.set(vertices[i + 3], vertices[i + 4], vertices[i + 5]).rot(transform).nor();
                    writer.write(String.format(Locale.US, " %f %f %f",
                            nor.x,
                            nor.y,
                            nor.z));
                }
                if (vertexSize > 8) {
                    int c = NumberUtils.floatToIntColor(vertices[i + 8]);
                    writer.write(String.format(Locale.US, " %d %d %d",
                            (c & 0x000000ff),
                            ((c & 0x0000ff00) >>> 8),
                            ((c & 0x00ff0000) >>> 16)));
                }
                writer.newLine();
            }

            for (int i = 0; i < indices.length; i += 3) {
                writer.write(String.format(Locale.US, "%d %d %d %d", 3, indices[i], indices[i + 1], indices[i + 2]));
                writer.newLine();
            }
        } finally {
            writer.flush();
            writer.close();
        }
    }
}
