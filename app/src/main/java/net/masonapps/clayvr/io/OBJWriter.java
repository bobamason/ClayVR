package net.masonapps.clayvr.io;

import android.annotation.SuppressLint;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

import net.masonapps.clayvr.mesh.SculptMeshData;
import net.masonapps.clayvr.mesh.Triangle;
import net.masonapps.clayvr.mesh.Vertex;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by Bob on 9/1/2017.
 */

public class OBJWriter {

    private static String DEFAULT_MATERIAL = "material0";

    public static void writeToZip(File zipFile, SculptMeshData meshData, boolean flipTexCoordV, Matrix4 transform) throws IOException {
        final int index = zipFile.getName().lastIndexOf('.');
        final String name = zipFile.getName().substring(0, index > 0 ? index : zipFile.getName().length());

        final ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile));

        zipOutputStream.putNextEntry(new ZipEntry(name + ".obj"));
        final ByteArrayOutputStream objOutputStream = new ByteArrayOutputStream();
        writeObjToOutputStream(objOutputStream, meshData, name.substring(0, index > 0 ? index : name.length()), transform);
        zipOutputStream.write(objOutputStream.toByteArray());

        zipOutputStream.putNextEntry(new ZipEntry(name + ".mtl"));
        final ByteArrayOutputStream mtlOutputStream = new ByteArrayOutputStream();
        final String textureFilename = name + ".jpg";
        writeMtlToOutputStream(mtlOutputStream, DEFAULT_MATERIAL, textureFilename);
        zipOutputStream.write(mtlOutputStream.toByteArray());
//        if (bitmap != null) {
//            zipOutputStream.putNextEntry(new ZipEntry(textureFilename));
//            final ByteArrayOutputStream textureOutputStream = new ByteArrayOutputStream(bitmap.getByteCount());
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, textureOutputStream);
//            zipOutputStream.write(textureOutputStream.toByteArray());
//        }
    }

    public static void writeToFiles(File objFile, File mtlFile, File textureFile, SculptMeshData meshData, boolean flipTexCoordV, Matrix4 transform) throws IOException {

        final String name = mtlFile.getName();
        final int index = name.lastIndexOf('.');
        writeObjToOutputStream(new FileOutputStream(objFile), meshData, name.substring(0, index > 0 ? index : name.length()), transform);

        writeMtlToOutputStream(new FileOutputStream(mtlFile), DEFAULT_MATERIAL, textureFile.getName());

//            if(bitmap != null) {
//                final boolean useJPG = textureFile.getName().endsWith(".jpg");
//                bitmap.compress(useJPG ? Bitmap.CompressFormat.JPEG : Bitmap.CompressFormat.PNG, 70, new FileOutputStream(textureFile));
//            }
    }

    @SuppressLint("DefaultLocale")
    private static void writeObjToOutputStream(OutputStream outputStream, SculptMeshData meshData, String mtlfilename, Matrix4 transform) throws IOException {
        final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
        try {
            writer.write("mtllib ./" + mtlfilename);
            writer.newLine();
            writer.write("g SculptMesh");
            writer.newLine();
            writer.write("usemtl " + DEFAULT_MATERIAL);
            writer.newLine();
            final Vector3 pos = new Vector3();
            final Vector3 nor = new Vector3();

            for (int i = 0; i < meshData.vertices.length; i++) {
                final Vertex vertex = meshData.vertices[i];
                pos.set(vertex.position).mul(transform);
//                writePosition(writer, pos.x, pos.y, pos.z);
                writePositionAndColor(writer, pos.x, pos.y, pos.z, vertex.color);
            }

            for (int i = 0; i < meshData.vertices.length; i++) {
                final Vertex vertex = meshData.vertices[i];
                nor.set(vertex.normal).rot(transform).nor();
                writeNormal(writer, nor.x, nor.y, nor.z);
            }

            for (int i = 0; i < meshData.vertices.length; i++) {
                final Vertex vertex = meshData.vertices[i];
                writeTextureCoordinate(writer, vertex.uv.x, vertex.uv.y);
            }

            for (int i = 0; i < meshData.triangles.length; i++) {
                final Triangle triangle = meshData.triangles[i];
                writer.write(String.format(Locale.US, "f %d %d %d", triangle.v1.index, triangle.v2.index, triangle.v3.index));
                if (i != meshData.triangles.length - 1)
                    writer.newLine();
            }
        } finally {
            writer.flush();
            writer.close();
        }
    }

    private static void writePosition(BufferedWriter writer, float x, float y, float z) throws IOException {
        writer.write(String.format(Locale.US, "v %f %f %f", x, y, z));
        writer.newLine();
    }

    private static void writePositionAndColor(BufferedWriter writer, float x, float y, float z, Color vc) throws IOException {
        writer.write(String.format(Locale.US, "v %f %f %f %f %f %f", x, y, z, vc.r, vc.g, vc.b));
        writer.newLine();
    }

    private static void writeNormal(BufferedWriter writer, float x, float y, float z) throws IOException {
        writer.write(String.format(Locale.US, "vn %f %f %f", x, y, z));
        writer.newLine();
    }

    private static void writeTextureCoordinate(BufferedWriter writer, float u, float v) throws IOException {
        writer.write(String.format(Locale.US, "vt %f %f", u, v));
        writer.newLine();
    }

    private static void writeMtlToOutputStream(OutputStream outputStream, String materialName, String textureFilename) throws IOException {
        final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));

        try {
            writer.write("newmtl " + materialName);
            writer.newLine();
            writer.write(String.format(Locale.US, "Ka %f %f %f", 0.5f, 0.5f, 0.5f));
            writer.newLine();
            writer.write(String.format(Locale.US, "Kd %f %f %f", 1f, 1f, 1f));
            writer.newLine();
            writer.write(String.format(Locale.US, "Ks %f %f %f", 0.5f, 0.5f, 0.5f));
            writer.newLine();
            writer.write("illum 2");
            writer.newLine();
//            writer.write("map_Ka " + textureFilename);
//            writer.newLine();
//            writer.write("map_Kd " + textureFilename);
//            writer.newLine();
        } finally {
            writer.flush();
            writer.close();
        }
    }

//    @SuppressWarnings("NumericOverflow")
//    public static void drawVertexColorsToCanvas(Canvas canvas, SculptMeshData meshData, boolean flipV) {
//        float w = canvas.getWidth();
//        float h = canvas.getHeight();
//        final Vector2 tc = new Vector2();
//        final Color c = new Color();
//        final float[] vertices2D = new float[meshData.getVertexCount() * 2];
//        final int[] colors = new int[meshData.getVertexCount() * 2];
//
//        for (int i = 0; i < vertices2D.length; i += 2) {
//            final int iv = i / 2;
//            final Vertex vertex = meshData.getVertex(iv);
//            tc.set(vertex.uv.x, vertex.uv.y);
//            vertices2D[i] = tc.x * w;
//            vertices2D[i] = (flipV ? 1f - tc.y : tc.y) * h;
//            c.set(vertex.color);
//            final int red = Math.round(c.r * 255f);
//            final int green = Math.round(c.g * 255f);
//            final int blue = Math.round(c.b * 255f);
//            colors[i / 2] = android.graphics.Color.rgb(red, green, blue);
//        }
//        
//        final ShortArray shortArray = new ShortArray();
//        for (Triangle triangle : meshData.triangles) {
//            shortArray.add(triangle.v1.index);
//            shortArray.add(triangle.v2.index);
//            shortArray.add(triangle.v3.index);
//        }
//        
//        final short[] indices = shortArray.toArray();
//
//        for (int i = 0; i < indices.length; i += 3) {
//            canvas.drawVertices(Canvas.VertexMode.TRIANGLES, vertices2D.length, vertices2D, 0, null, 0, colors, 0, indices, 0, indices.length, new Paint(Paint.ANTI_ALIAS_FLAG));
//        }
//    }
}
