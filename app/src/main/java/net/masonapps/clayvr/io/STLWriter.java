package net.masonapps.clayvr.io;

import android.annotation.SuppressLint;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;

import net.masonapps.clayvr.mesh.SculptMeshData;
import net.masonapps.clayvr.mesh.Triangle;
import net.masonapps.clayvr.mesh.Vertex;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Locale;

/**
 * Created by Bob on 8/1/2017.
 */

public class STLWriter {

    public static void writeToFile(File file, SculptMeshData meshData, Matrix4 transform) throws IOException {
        writeToOutputStream(new FileOutputStream(file), meshData, transform);
    }

    public static void writeToFile(File file, float[] vertices, short[] indices, int vertexSize, Matrix4 transform) throws IOException {
        writeToOutputStream(new FileOutputStream(file), vertices, indices, vertexSize, transform);
    }

    @SuppressLint("DefaultLocale")
    public static void writeToOutputStream(OutputStream outputStream, SculptMeshData meshData, Matrix4 transform) throws IOException {
        final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
        try {
            writer.write("solid");
            writer.newLine();

            final Vector3 pos = new Vector3();
            final Vector3 nor = new Vector3();

            for (int i = 0; i < meshData.triangles.length; i++) {
                final Triangle t = meshData.triangles[i];
                nor.set(t.plane.normal).rot(transform).nor();
                writer.write(String.format(Locale.US, "facet normal %f %f %f",
                        nor.x,
                        nor.y,
                        nor.z));
                writer.newLine();

                writer.write("\touter loop");
                writer.newLine();

                final Vertex v1 = meshData.vertices[t.v1.index];
                pos.set(v1.position).mul(transform);
                writer.write(String.format(Locale.US, "\t\tvertex %f %f %f",
                        pos.x,
                        pos.y,
                        pos.z));
                writer.newLine();

                final Vertex v2 = meshData.vertices[t.v2.index];
                pos.set(v2.position).mul(transform);
                writer.write(String.format(Locale.US, "\t\tvertex %f %f %f",
                        pos.x,
                        pos.y,
                        pos.z));
                writer.newLine();

                final Vertex v3 = meshData.vertices[t.v3.index];
                pos.set(v3.position).mul(transform);
                writer.write(String.format(Locale.US, "\t\tvertex %f %f %f",
                        pos.x,
                        pos.y,
                        pos.z));
                writer.newLine();

                writer.write("\tendloop");
                writer.newLine();

                writer.write("endfacet");
                writer.newLine();
            }

            writer.write("endsolid");
            writer.newLine();
        } finally {
            writer.flush();
            writer.close();
        }
    }

    @SuppressLint("DefaultLocale")
    public static void writeToOutputStream(OutputStream outputStream, float[] vertices, short[] indices, int vertexSize, Matrix4 transform) throws IOException {
        final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
        try {
            writer.write("solid");
            writer.newLine();

            Vector3 va = new Vector3();
            Vector3 vb = new Vector3();
            Vector3 vc = new Vector3();
            Plane plane = new Plane();

            for (int i = 0; i < indices.length; i += 3) {
                int ia = indices[i];
                int ib = indices[i + 1];
                int ic = indices[i + 2];
                va.set(vertices[ia * vertexSize], vertices[ia * vertexSize + 1], vertices[ia * vertexSize + 2]).mul(transform);
                vb.set(vertices[ib * vertexSize], vertices[ib * vertexSize + 1], vertices[ib * vertexSize + 2]).mul(transform);
                vc.set(vertices[ic * vertexSize], vertices[ic * vertexSize + 1], vertices[ic * vertexSize + 2]).mul(transform);
                plane.set(va, vb, vc);
                writer.write(String.format(Locale.US, "facet normal %f %f %f",
                        plane.normal.x,
                        plane.normal.y,
                        plane.normal.z));
                writer.newLine();

                writer.write("\touter loop");
                writer.newLine();

                writer.write(String.format(Locale.US, "\t\tvertex %f %f %f",
                        va.x,
                        va.y,
                        va.z));
                writer.newLine();

                writer.write(String.format(Locale.US, "\t\tvertex %f %f %f",
                        vb.x,
                        vb.y,
                        vb.z));
                writer.newLine();

                writer.write(String.format(Locale.US, "\t\tvertex %f %f %f",
                        vc.x,
                        vc.y,
                        vc.z));
                writer.newLine();

                writer.write("\tendloop");
                writer.newLine();

                writer.write("endfacet");
                writer.newLine();
            }

            writer.write("endsolid");
            writer.newLine();
        } finally {
            writer.flush();
            writer.close();
        }
    }
}
