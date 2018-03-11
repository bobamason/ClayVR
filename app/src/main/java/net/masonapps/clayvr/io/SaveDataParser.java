package net.masonapps.clayvr.io;

import android.util.Log;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.NumberUtils;

import net.masonapps.clayvr.sculpt.SaveData;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Bob on 9/19/2017.
 */

public class SaveDataParser {

    private static final Vector3 position = new Vector3();
    private static final Vector3 normal = new Vector3();
    private static final Color color = new Color();

    public static SaveData.SaveDataHolder parse(File file) throws IOException {
        return parse(new FileInputStream(file));
    }

    public static SaveData.SaveDataHolder parse(InputStream inputStream) throws IOException {
        final DataInputStream stream = new DataInputStream(new BufferedInputStream(inputStream));
        String assetName = "none";
        String version = SculptMeshWriter.VERSION_1;
        boolean symmetryEnabled = true;
        SaveData[] saveData = new SaveData[0];
        try {
            final String header = readHeader(stream);
            Log.d(SaveDataParser.class.getSimpleName(), "file header: " + header);
            final String[] lines = header.split("\n");
            for (String line : lines) {
                if (line.startsWith(SculptMeshWriter.VERSION)) {
                    version = parseVersion(line);
                } else if (line.startsWith(SculptMeshWriter.ASSET)) {
                    assetName = parseAssetName(line);
                } else if (line.startsWith(SculptMeshWriter.SYMMETRY)) {
                    symmetryEnabled = parseSymmetry(line);
                }
            }
            Log.d(SaveDataParser.class.getSimpleName(), "version: " + version);
            Log.d(SaveDataParser.class.getSimpleName(), "original asset: " + assetName);

            final int vertexCount = stream.readInt();
            saveData = new SaveData[vertexCount];

            for (int i = 0; i < vertexCount; i++) {
                saveData[i] = parseSaveData(stream, i);
            }
        } finally {
            stream.close();
        }
        return new SaveData.SaveDataHolder(saveData, assetName, symmetryEnabled);
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

    private static SaveData parseSaveData(DataInputStream stream, int index) throws IOException {
        position.set(stream.readFloat(), stream.readFloat(), stream.readFloat());
        normal.set(stream.readFloat(), stream.readFloat(), stream.readFloat());
        Color.argb8888ToColor(color, NumberUtils.floatToIntColor(stream.readFloat()));
        return new SaveData(position, normal, color, index);
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
