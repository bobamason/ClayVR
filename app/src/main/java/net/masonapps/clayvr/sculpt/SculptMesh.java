package net.masonapps.clayvr.sculpt;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.IndexBufferObject;
import com.badlogic.gdx.graphics.glutils.IndexData;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.VertexBufferObject;
import com.badlogic.gdx.graphics.glutils.VertexData;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;

import net.masonapps.clayvr.mesh.Edge;
import net.masonapps.clayvr.mesh.SculptMeshData;
import net.masonapps.clayvr.mesh.Triangle;
import net.masonapps.clayvr.mesh.Vertex;

import org.masonapps.libgdxgooglevr.gfx.Entity;
import org.masonapps.libgdxgooglevr.utils.Logger;

/**
 * Created by Bob on 7/10/2017.
 */

public class SculptMesh extends Mesh {


    private final float[] tempVertices;
    private final int vertexSize;
    private final SculptMeshData meshData;
    private final ShaderProgram lineShaderProgram;
    private final IndexBufferObject linesIBO;
    //    public Vector3 clipCenter = new Vector3();
//    public float clipY = -1000f;
//    public float clipRadius = 0f;
    private VertexBufferObject vbo;
    private Color lineColor = Color.GRAY.cpy();

    private SculptMesh(SculptMeshData meshData, VertexData vertices, IndexData indices) {
        super(vertices, indices, false);
        this.meshData = meshData;
        final FloatArray verticesArray = new FloatArray();
        final ShortArray indicesArray = new ShortArray(meshData.triangles.length * 3);
        for (Triangle triangle : meshData.triangles) {
            indicesArray.add(triangle.v1.index);
            indicesArray.add(triangle.v2.index);
            indicesArray.add(triangle.v3.index);
        }
        verticesArray.ensureCapacity(meshData.vertices.length * (getVertexSize() / 4));
        for (int i = 0; i < meshData.vertices.length; i++) {
            final Vertex vertex = meshData.vertices[i];
            if (vertex == null) continue;
            verticesArray.add(vertex.position.x);
            verticesArray.add(vertex.position.y);
            verticesArray.add(vertex.position.z);
            verticesArray.add(vertex.normal.x);
            verticesArray.add(vertex.normal.y);
            verticesArray.add(vertex.normal.z);
            verticesArray.add(vertex.uv.x);
            verticesArray.add(vertex.uv.y);
            verticesArray.add(vertex.color.toFloatBits());
        }
        vertexSize = (getVertexSize() / 4);
        tempVertices = verticesArray.toArray();
        setVertices(tempVertices);
        setIndices(indicesArray.toArray());
        linesIBO = createLinesIBO(meshData);
        lineShaderProgram = new ShaderProgram(getLineVertexShader(), getLineFragmentShader());
    }

    public static SculptMesh newInstance(SculptMeshData meshData) {
        final VertexBufferObject vbo = createVBO(meshData);
        final SculptMesh sculptMesh = new SculptMesh(meshData, vbo, createIBO(meshData));
        sculptMesh.vbo = vbo;
        return sculptMesh;
    }

    private static String getLineVertexShader() {
        return "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" +
                "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" +
                "attribute vec3 " + ShaderProgram.NORMAL_ATTRIBUTE + ";\n" +
                "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" +
                "uniform mat4 u_projTrans;\n" +
                "uniform mat4 u_worldTrans;\n" +
                "varying vec4 v_color;\n" +
                "varying vec3 v_pos;\n" +
                "\n" +
                "void main() {\n" +
                "    vec4 pos = u_worldTrans * a_position;\n" +
                "    v_pos = pos.xyz;\n" +
                "    v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" +
                "    gl_Position = u_projTrans * pos;\n" +
                "}";
    }

    private static String getLineFragmentShader() {
        return "#ifdef GL_ES \n" +
                "#define LOWP lowp\n" +
                "#define MED mediump\n" +
                "#define HIGH highp\n" +
                "precision highp float;\n" +
                "#else\n" +
                "#define MED\n" +
                "#define LOWP\n" +
                "#define HIGH\n" +
                "#endif\n" +
                "\n" +
                "uniform vec4 u_color;\n" +
//                "uniform float u_clip;\n" +
//                "uniform float u_radius;\n" +
//                "uniform vec3 u_center;\n" +
                "varying vec4 v_color;\n" +
                "varying vec3 v_pos;\n" +
                "\n" +
                "void main(){\n" +
//                "    float dst = length(v_pos - u_center);\n" +
//                "    if(v_pos.y < u_clip || dst < u_radius){\n" +
                "        gl_FragColor = u_color * v_color;\n" +
//                "    } else {\n" +
//                "        discard;\n" +
//                "    }\n" +
                "}";
    }

    protected static VertexBufferObject createVBO(SculptMeshData meshData) {
        return new VertexBufferObject(false, meshData.vertices.length, new VertexAttributes(VertexAttribute.Position(), VertexAttribute.Normal(), VertexAttribute.TexCoords(0), VertexAttribute.ColorPacked()));
    }

    protected static IndexBufferObject createIBO(SculptMeshData meshData) {
        return new IndexBufferObject(true, meshData.triangles.length * 3);
    }

    protected static IndexBufferObject createLinesIBO(SculptMeshData meshData) {
        final int n = meshData.edges.size() * 2;
        Logger.d("num edges: " + meshData.edges.size());
        final IndexBufferObject ibo = new IndexBufferObject(true, n);
        final short[] indices = new short[n];
        int i = 0;
        for (Edge edge : meshData.edges) {
            indices[i++] = (short) edge.v1.index;
            indices[i++] = (short) edge.v2.index;
        }
        ibo.setIndices(indices, 0, indices.length);
        return ibo;
    }

    public static Entity createSculptEntity(ModelBuilder builder, SculptMesh sculptMesh) {
//        final Material material = new Material(ColorAttribute.createDiffuse(Color.WHITE), ColorAttribute.createAmbient(Color.WHITE), ColorAttribute.createSpecular(color.r * 0.5f, color.g * 0.5f, color.b * 0.5f, 1), FloatAttribute.createShininess(50));
        final Material material = new Material(ColorAttribute.createDiffuse(Color.WHITE), ColorAttribute.createAmbient(Color.WHITE));
        builder.begin();
        builder.part("", sculptMesh, GL20.GL_TRIANGLES, material);
        final ModelInstance modelInstance = new ModelInstance(builder.end());
        return new Entity(modelInstance, new BoundingBox());
    }

    public static Entity createSculptEntity(ModelBuilder builder, SculptMesh sculptMesh, BoundingBox bounds, Material material) {
        builder.begin();
        builder.part("", sculptMesh, GL20.GL_TRIANGLES, material);
        final ModelInstance modelInstance = new ModelInstance(builder.end());
        return new Entity(modelInstance, bounds);
    }

    public void renderEdges(Camera camera, Matrix4 transform) {
        vbo.bind(lineShaderProgram, null);
        linesIBO.bind();

        lineShaderProgram.begin();

        lineShaderProgram.setUniformMatrix("u_worldTrans", transform);
        lineShaderProgram.setUniformMatrix("u_projTrans", camera.combined);
        lineShaderProgram.setUniformf("u_color", lineColor);
        Gdx.gl20.glDrawElements(GL20.GL_LINES, getNumIndices(), GL20.GL_UNSIGNED_SHORT, 0);

        lineShaderProgram.end();

        vbo.unbind(lineShaderProgram);
        linesIBO.unbind();
    }

    public void setLineColor(Color lineColor) {
        this.lineColor.set(lineColor);
    }

    public void setVertex(Vertex vertex) {
        if (vertex.index < 0) return;
//        vertices.get(vertex.index).set(vertex);
        int i = vertex.index * vertexSize;

        tempVertices[i] = vertex.position.x;
        tempVertices[i + 1] = vertex.position.y;
        tempVertices[i + 2] = vertex.position.z;

        tempVertices[i + 3] = vertex.normal.x;
        tempVertices[i + 4] = vertex.normal.y;
        tempVertices[i + 5] = vertex.normal.z;

        tempVertices[i + 6] = vertex.uv.x;
        tempVertices[i + 7] = vertex.uv.y;

        tempVertices[i + 8] = vertex.color.toFloatBits();
    }

    public void update() {
        super.setVertices(tempVertices);
    }

    public float[] getTempVertices() {
        return tempVertices;
    }

    public Triangle[] getTriangles() {
        return meshData.triangles;
    }

    public Vertex[] getVertexArray() {
        return meshData.vertices;
    }

    public synchronized Vertex getVertex(int i) {
        return meshData.vertices[i];
    }

    public SculptMeshData getMeshData() {
        return meshData;
    }

    @Override
    public Mesh copy(boolean isStatic, boolean removeDuplicates, int[] usage) {
        return SculptMesh.newInstance(meshData);
    }

    public SculptMesh copy() {
        return (SculptMesh) copy(false);
    }
}
