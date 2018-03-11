package net.masonapps.clayvr.service;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.badlogic.gdx.math.Matrix4;

import net.masonapps.clayvr.Constants;
import net.masonapps.clayvr.R;
import net.masonapps.clayvr.SculptVrApplication;
import net.masonapps.clayvr.io.FileUtils;
import net.masonapps.clayvr.io.OBJWriter;
import net.masonapps.clayvr.io.PLYWriter;
import net.masonapps.clayvr.io.STLWriter;
import net.masonapps.clayvr.io.SculptMeshWriter;
import net.masonapps.clayvr.mesh.SculptMeshData;
import net.masonapps.clayvr.mesh.Triangle;
import net.masonapps.clayvr.mesh.Vertex;

import org.masonapps.libgdxgooglevr.utils.Logger;

import java.io.File;
import java.io.IOException;

/**
 * Created by Bob on 8/17/2017.
 */

public class ExportService extends IntentService {

    public static final String ACTION_EXPORT_COMPLETE = "masonapps.exportservice.action.ACTION_COMPLETE";
    public static final String TAG = ExportService.class.getName();
    private static final int NOTIFICATION_ID = 1;

    public ExportService() {
        super(TAG);
        Log.d(TAG, "service created");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressLint({"SetWorldReadable", "SetWorldWritable"})
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        Logger.d("service started");

        final SculptMeshData meshData = ((SculptVrApplication) getApplication()).getMeshData();
        final Matrix4 transform = new Matrix4(((SculptVrApplication) getApplication()).getTransform());
        Logger.d("export transform:\n" + transform);
        if (meshData == null)
            return;

        if (intent == null) return;

        final String filePath = intent.getStringExtra(Constants.KEY_FILE_PATH);
        final String fileType = intent.getStringExtra(Constants.KEY_FILE_TYPE);
        final Boolean isExternal = intent.getBooleanExtra(Constants.KEY_EXTERNAL, true);
        if (filePath == null || fileType == null)
            return;

        String channelId = getPackageName();
        final NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final NotificationChannel channel = new NotificationChannel(channelId, getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(getString(R.string.notification_channel_desc));
            channel.setShowBadge(false);
            if (notificationManager != null)
                notificationManager.createNotificationChannel(channel);
        }
        final NotificationCompat.Builder nb = new NotificationCompat.Builder(getApplicationContext(), channelId);
        final boolean isSavingProject = fileType.equals(Constants.FILE_TYPE_SCULPT) || fileType.equals(Constants.FILE_TYPE_SAVE_DATA);
        nb.setContentTitle(getString(isSavingProject ? R.string.notification_title_saving : R.string.notification_title_exporting))
                .setContentText(isSavingProject ? "saving..." : "creating " + fileType.toUpperCase() + " file...")
                .setSmallIcon(android.R.drawable.stat_notify_sdcard)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setProgress(10, 0, true);
        startForeground(NOTIFICATION_ID, nb.build());
        
        final File file = new File(filePath);
        if (isExternal) {
            file.setReadable(true, false);
            file.setWritable(true, false);
            file.setExecutable(true, false);
        }

        Logger.d("export started " + file.getAbsolutePath());
        try {
            final int vertexCount = meshData.getVertexCount();
            final float[] vertices = new float[vertexCount * 9];

            for (int i = 0; i < vertexCount; i++) {
                final Vertex vertex = meshData.vertices[i];
                vertices[i] = vertex.position.x;
                vertices[i + 1] = vertex.position.y;
                vertices[i + 2] = vertex.position.z;

                vertices[i + 3] = vertex.normal.x;
                vertices[i + 4] = vertex.normal.y;
                vertices[i + 5] = vertex.normal.z;

                vertices[i + 6] = vertex.uv.x;
                vertices[i + 7] = vertex.uv.y;

                vertices[i + 8] = vertex.color.toFloatBits();
            }

            final int triangleCount = meshData.getTriangleCount();
            final short[] indices = new short[triangleCount * 3];

            for (int i = 0; i < triangleCount; i++) {
                final Triangle triangle = meshData.triangles[i];
                indices[i] = (short) triangle.v1.index;
                indices[i + 1] = (short) triangle.v2.index;
                indices[i + 2] = (short) triangle.v3.index;
            }
            
            switch (fileType) {
                case Constants.FILE_TYPE_OBJ:
                    final String name = FileUtils.nameWithoutExtension(file);
                    final File folder = new File(file.getParentFile(), name);
                    folder.mkdirs();
                    final File objFile = new File(folder, name + ".obj");
                    final File mtlFile = new File(folder, name + ".mtl");
                    final File textureFile = new File(folder, name + ".png");
                    OBJWriter.writeToFiles(objFile, mtlFile, textureFile, vertices, indices, 9, false, transform);
                    break;
                case Constants.FILE_TYPE_PLY:
                    PLYWriter.writeToFile(file, vertices, indices, 9, transform);
                    break;
                case Constants.FILE_TYPE_STL:
                    STLWriter.writeToFile(file, meshData, transform);
                    break;
                case Constants.FILE_TYPE_SCULPT:
                    SculptMeshWriter.writeToFile(file, meshData, transform);
                    break;
            }

            if (isExternal) {
                final Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                scanIntent.setData(Uri.fromFile(file));
                sendBroadcast(scanIntent);

                sendBroadcast(new Intent(ACTION_EXPORT_COMPLETE));
            }
            Logger.d("export to " + filePath + " successful");
            nb.setContentText(isSavingProject ? "project saved" : ("export to " + file.getName() + " successful"));
            nb.setProgress(0, 0, false);
            notificationManager.notify(NOTIFICATION_ID, nb.build());

        } catch (IOException e) {
            Log.e(TAG, "export to " + filePath + " failed: " + e.getLocalizedMessage());
            e.printStackTrace();
            nb.setContentText((isSavingProject ? "saving" : "exporting") + " to " + file.getName() + " failed");
            nb.setProgress(0, 0, false);
            notificationManager.notify(NOTIFICATION_ID, nb.build());
        } finally {
            stopForeground(false);
        }
    }
}
