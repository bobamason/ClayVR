package net.masonapps.clayvr.io;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.NumberUtils;

import org.masonapps.libgdxgooglevr.utils.Logger;

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

    public static void writeToZip(File zipFile, float[] vertices, short[] indices, int vertexSize, boolean flipTexCoordV, Matrix4 transform) throws IOException {
        final int index = zipFile.getName().lastIndexOf('.');
        final String name = zipFile.getName().substring(0, index > 0 ? index : zipFile.getName().length());

        final ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile));

        zipOutputStream.putNextEntry(new ZipEntry(name + ".obj"));
        final ByteArrayOutputStream objOutputStream = new ByteArrayOutputStream();
        writeObjToOutputStream(objOutputStream, vertices, indices, vertexSize, name.substring(0, index > 0 ? index : name.length()), transform);
        zipOutputStream.write(objOutputStream.toByteArray());

        zipOutputStream.putNextEntry(new ZipEntry(name + ".mtl"));
        final ByteArrayOutputStream mtlOutputStream = new ByteArrayOutputStream();
        final String textureFilename = name + ".jpg";
        writeMtlToOutputStream(mtlOutputStream, DEFAULT_MATERIAL, textureFilename);
        zipOutputStream.write(mtlOutputStream.toByteArray());


        final Bitmap bitmap = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888);
        try {
            drawVertexColorsToCanvas(new Canvas(bitmap), vertices, indices, vertexSize, flipTexCoordV);
        } catch (Exception e) {
            Logger.e("failed to draw colors to canvas", e);
        }

        zipOutputStream.putNextEntry(new ZipEntry(textureFilename));
        final ByteArrayOutputStream textureOutputStream = new ByteArrayOutputStream(bitmap.getByteCount());
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, textureOutputStream);
        zipOutputStream.write(textureOutputStream.toByteArray());
    }

    public static void writeToFiles(File objFile, File mtlFile, File textureFile, float[] vertices, short[] indices, int vertexSize, boolean flipTexCoordV, Matrix4 transform) throws IOException {

        final String name = mtlFile.getName();
        final int index = name.lastIndexOf('.');
        writeObjToOutputStream(new FileOutputStream(objFile), vertices, indices, vertexSize, name.substring(0, index > 0 ? index : name.length()), transform);

        writeMtlToOutputStream(new FileOutputStream(mtlFile), DEFAULT_MATERIAL, textureFile.getName());

        try {
            final Bitmap bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888);
            drawVertexColorsToCanvas(new Canvas(bitmap), vertices, indices, vertexSize, flipTexCoordV);

            final boolean useJPG = textureFile.getName().endsWith(".jpg");
            bitmap.compress(useJPG ? Bitmap.CompressFormat.JPEG : Bitmap.CompressFormat.PNG, 70, new FileOutputStream(textureFile));
        } catch (Exception e) {
            Logger.e("failed to draw colors to canvas", e);
        }
    }

    @SuppressLint("DefaultLocale")
    private static void writeObjToOutputStream(OutputStream outputStream, float[] vertices, short[] indices, int vertexSize, String mtlfilename, Matrix4 transform) throws IOException {
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

            for (int i = 0; i < vertices.length; i += vertexSize) {
                pos.set(vertices[i], vertices[i + 1], vertices[i + 2]).mul(transform);
                writePosition(writer, pos.x, pos.y, pos.z);
            }

            for (int i = 0; i < vertices.length; i += vertexSize) {
                nor.set(vertices[i + 3], vertices[i + 4], vertices[i + 5]).rot(transform).nor();
                writeNormal(writer, nor.x, nor.y, nor.z);
            }

            for (int i = 0; i < vertices.length; i += vertexSize) {
                writeTextureCoordinate(writer, vertices[i + 6], vertices[i + 7]);
            }

            for (int i = 0; i < indices.length; i += 3) {
                writeFace(writer, indices[i], indices[i + 1], indices[i + 2]);
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

    private static void writeNormal(BufferedWriter writer, float x, float y, float z) throws IOException {
        writer.write(String.format(Locale.US, "vn %f %f %f", x, y, z));
        writer.newLine();
    }

    private static void writeTextureCoordinate(BufferedWriter writer, float u, float v) throws IOException {
        writer.write(String.format(Locale.US, "vt %f %f", u, v));
        writer.newLine();
    }

    private static void writeFace(BufferedWriter writer, int a, int b, int c) throws IOException {
        writer.write(String.format(Locale.US, "f %d %d %d", a, b, c));
        writer.newLine();
    }

    private static void writeMtlToOutputStream(OutputStream outputStream, String materialName, String textureFilename) throws IOException {
        final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));

        try {
            writer.write("newmtl " + materialName);
            writer.newLine();
            writer.write(String.format(Locale.US, "Ka %f %f %f", 1f, 1f, 1f));
            writer.newLine();
            writer.write(String.format(Locale.US, "Kd %f %f %f", 1f, 1f, 1f));
            writer.newLine();
            writer.write(String.format(Locale.US, "Ks %f %f %f", 0f, 0f, 0f));
            writer.newLine();
            writer.write("illum 2");
            writer.newLine();
            writer.write("map_Ka " + textureFilename);
            writer.newLine();
            writer.write("map_Kd " + textureFilename);
            writer.newLine();
        } finally {
            writer.flush();
            writer.close();
        }
    }

    @SuppressWarnings("NumericOverflow")
    private static void drawVertexColorsToCanvas(Canvas canvas, float[] vertices, short[] indices, int vertexSize, boolean flipV) {
        if (vertexSize != 9 || vertices.length % 9 != 0)
            throw new IllegalArgumentException("vertexSize must be 9, [px, py, pz, nx, ny, nz, u, v, color]");
        float w = canvas.getWidth();
        float h = canvas.getHeight();
        final Vector2 tc = new Vector2();
        final Color c = new Color();
        final float[] vertices2D = new float[vertices.length / vertexSize * 2];
        final int[] colors = new int[vertices.length / vertexSize];

        for (int i = 0; i < vertices2D.length; i += 2) {
            final int iv = i / 2 * vertexSize;
            tc.set(vertices[iv + 6], vertices[iv + 7]);
            vertices2D[i] = tc.x * w;
            vertices2D[i] = (flipV ? 1f - tc.y : tc.y) * h;
            Color.argb8888ToColor(c, NumberUtils.floatToIntColor(vertices[iv + 8]));
            final int red = Math.round(c.r * 255f);
            final int green = Math.round(c.g * 255f);
            final int blue = Math.round(c.b * 255f);
            colors[i / 2] = android.graphics.Color.rgb(red, green, blue);
        }

        for (int i = 0; i < indices.length; i += 3) {
            canvas.drawVertices(Canvas.VertexMode.TRIANGLES, vertices2D.length, vertices2D, 0, null, 0, colors, 0, indices, 0, indices.length, new Paint(Paint.ANTI_ALIAS_FLAG));
        }
    }
}
