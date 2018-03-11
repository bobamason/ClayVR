package net.masonapps.clayvr.io;

import android.support.annotation.Nullable;
import android.util.Log;

import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g3d.model.data.ModelData;
import com.badlogic.gdx.graphics.g3d.model.data.ModelMaterial;
import com.badlogic.gdx.graphics.g3d.model.data.ModelMesh;
import com.badlogic.gdx.graphics.g3d.model.data.ModelMeshPart;
import com.badlogic.gdx.graphics.g3d.model.data.ModelNode;
import com.badlogic.gdx.graphics.g3d.model.data.ModelNodePart;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import net.masonapps.clayvr.Constants;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Bob on 8/17/2017.
 */

public class SculptLoader extends ModelLoader<ModelLoader.ModelParameters> {


    public SculptLoader() {
        this(null);
    }

    public SculptLoader(FileHandleResolver resolver) {
        super(resolver);
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

    @Nullable
    public static ModelData getModelData(File file) throws FileNotFoundException {
        final String fileName = file.getName();
        if (fileName.endsWith("." + Constants.FILE_TYPE_SAVE_DATA))
//            return getModelFromSavedData(new FileInputStream(file));
            return null;
        else if (fileName.endsWith("." + Constants.FILE_TYPE_SCULPT))
            return getModelData(new FileInputStream(file));
        else return null;
    }
    
    public static ModelData getModelData(InputStream inputStream) {
        final DataInputStream stream = new DataInputStream(new BufferedInputStream(inputStream));
        final ModelData data = new ModelData();

        try {
            Array<VertexAttribute> attributes = new Array<>();
            attributes.add(VertexAttribute.Position());
            attributes.add(VertexAttribute.Normal());
            attributes.add(VertexAttribute.TexCoords(0));
            attributes.add(VertexAttribute.ColorPacked());

            final String header = readHeader(stream);

            final int vn = stream.readInt() * 9;
            final int in = stream.readInt() * 3;

            final float[] vertices = new float[vn];
            for (int i = 0; i < vn; i++) {
                vertices[i] = stream.readFloat();
            }

            final short[] indices = new short[in];
            for (int i = 0; i < in; i++) {
                indices[i] = stream.readShort();
            }

            ModelNode node = new ModelNode();
            final String nodeId = "node";
            final String meshId = "mesh";
            final String partId = "part";
            final String materialName = "mat0";
            node.id = nodeId;
            node.meshId = meshId;
            node.scale = new Vector3(1, 1, 1);
            node.translation = new Vector3();
            node.rotation = new Quaternion();
            ModelNodePart pm = new ModelNodePart();
            pm.meshPartId = partId;
            pm.materialId = materialName;
            node.parts = new ModelNodePart[]{pm};
            ModelMeshPart part = new ModelMeshPart();
            part.id = partId;
            part.indices = indices;
            part.primitiveType = GL20.GL_TRIANGLES;
            ModelMesh mesh = new ModelMesh();
            mesh.id = meshId;
            mesh.attributes = attributes.toArray(VertexAttribute.class);
            mesh.vertices = vertices;
            mesh.parts = new ModelMeshPart[]{part};
            data.nodes.add(node);
            data.meshes.add(mesh);
            ModelMaterial mat = new ModelMaterial();
            mat.id = "mat0";
            mat.ambient = new Color(Color.WHITE);
            mat.diffuse = new Color(Color.WHITE);
//            mat.specular = new Color(Color.DARK_GRAY);
            mat.opacity = 1f;
//            mat.shininess = 50f;
            data.materials.add(mat);
        } catch (IOException e) {
            Log.e(SculptLoader.class.getSimpleName(), "load failed: " + e.getLocalizedMessage());
            e.printStackTrace();
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return data;
    }

    @Override
    public ModelData loadModelData(FileHandle fileHandle, ModelParameters parameters) {

        return getModelData(fileHandle.read());
    }
}
