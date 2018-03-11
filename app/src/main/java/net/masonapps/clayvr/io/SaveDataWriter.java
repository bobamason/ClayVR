package net.masonapps.clayvr.io;

import android.util.Log;

import net.masonapps.clayvr.Constants;
import net.masonapps.clayvr.mesh.SculptMeshData;
import net.masonapps.clayvr.sculpt.SaveData;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Created by Bob on 9/19/2017.
 */

public class SaveDataWriter {

    public static final int HEADER_LENGTH = 80;
    public static final String VERSION = "version";
    public static final String ASSET = "asset";
    public static final String VERSION_1 = "1";
    public static final String SYMMETRY = "symmetry";

    public static void writeToFile(File file, SculptMeshData meshData) throws IOException {
        final SaveData.SaveDataHolder saveDataHolder = SaveData.fromSculptMeshData(meshData);
        writeToOutputStream(new FileOutputStream(file), saveDataHolder);
    }

    public static void writeToOutputStream(OutputStream outputStream, SaveData.SaveDataHolder saveDataHolder) throws IOException {
        final DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(outputStream));
        writeHeader(stream, saveDataHolder);
        final SaveData[] saveData = saveDataHolder.saveData;
        stream.writeInt(saveData.length);

        for (int i = 0; i < saveData.length; i++) {
            final SaveData s = saveData[i];
            stream.writeFloat(s.position.x);
            stream.writeFloat(s.position.y);
            stream.writeFloat(s.position.z);
            stream.writeFloat(s.normal.x);
            stream.writeFloat(s.normal.y);
            stream.writeFloat(s.normal.z);
            stream.writeFloat(s.color.toFloatBits());
        }

        Log.d(SaveDataWriter.class.getSimpleName(), saveData.length + " vertices written to file");
        stream.flush();
        stream.close();
    }

    private static void writeHeader(DataOutputStream stream, SaveData.SaveDataHolder saveDataHolder) throws IOException {
        final char[] chars = new char[HEADER_LENGTH];
        Arrays.fill(chars, '\0');
        final String s = "created by " + Constants.APP_NAME + "\n" +
                VERSION + " " + VERSION_1 + "\n" +
                ASSET + " " + saveDataHolder.originalAssetName + "\n" +
                SYMMETRY + " " + Boolean.toString(saveDataHolder.symmetryEnabled) + "\n";
        s.getChars(0, s.length(), chars, 0);
        for (int i = 0; i < chars.length; i++) {
            stream.writeChar(chars[i]);
        }
    }
}
