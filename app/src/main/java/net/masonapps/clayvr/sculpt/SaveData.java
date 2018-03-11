package net.masonapps.clayvr.sculpt;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;

import net.masonapps.clayvr.mesh.SculptMeshData;
import net.masonapps.clayvr.mesh.Vertex;

/**
 * Created by Bob on 9/19/2017.
 */
public class SaveData {
    public Vector3 position = new Vector3();
    public Vector3 normal = new Vector3();
    public Color color = Color.WHITE.cpy();
    public int index = -1;

    public SaveData(Vector3 position, Vector3 normal, Color color) {
        this.position.set(position);
        this.normal.set(normal);
        this.color.set(color);
    }

    public SaveData(Vector3 position, Vector3 normal, Color color, int index) {
        this.position.set(position);
        this.normal.set(normal);
        this.color.set(color);
        this.index = index;
    }

    public SaveData(Vertex vertex) {
        this.position.set(vertex.position);
        this.normal.set(vertex.normal);
        this.color.set(vertex.color);
        this.index = vertex.index;
    }

    public static SaveDataHolder fromSculptMeshData(SculptMeshData meshData) {
        final SaveData[] saveData = new SaveData[meshData.getVertexCount()];
        for (int i = 0; i < saveData.length; i++) {
            saveData[i] = new SaveData(meshData.getVertex(i));
        }
        return new SaveDataHolder(saveData, meshData.getOriginalAssetName(), meshData.isSymmetryEnabled());
    }

    public void set(Vertex vertex) {
        this.position.set(vertex.position);
        this.normal.set(vertex.normal);
        this.color.set(vertex.color);
        this.index = vertex.index;
    }

    public static class SaveDataHolder {
        public SaveData[] saveData;
        public String originalAssetName;
        public boolean symmetryEnabled;

        public SaveDataHolder(SaveData[] saveData, String originalAssetName, boolean symmetryEnabled) {
            this.saveData = saveData;
            this.originalAssetName = originalAssetName;
            this.symmetryEnabled = symmetryEnabled;
        }
    }
}
