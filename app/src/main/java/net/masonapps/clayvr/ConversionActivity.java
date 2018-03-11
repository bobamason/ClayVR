package net.masonapps.clayvr;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.badlogic.gdx.math.Matrix4;

import net.masonapps.clayvr.bvh.BVH;
import net.masonapps.clayvr.bvh.BVHBuilder;
import net.masonapps.clayvr.bvh.BVHFileIO;
import net.masonapps.clayvr.io.PLYConverter;
import net.masonapps.clayvr.io.SculptMeshParser;
import net.masonapps.clayvr.io.SculptMeshWriter;
import net.masonapps.clayvr.mesh.SculptMeshData;
import net.masonapps.clayvr.mesh.Vertex;

import org.masonapps.libgdxgooglevr.utils.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * Created by Bob on 8/1/2017.
 */

public class ConversionActivity extends Activity {

    private static final int REQUEST_CODE_STORAGE = 901;
    private ProgressBar progressBar;
    private volatile int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final FrameLayout frameLayout = new FrameLayout(this);
        progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);
        frameLayout.addView(progressBar, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        setContentView(frameLayout);
//        final File[] files = getFilesDir().listFiles();
//        for (File file : files) {
//            file.delete();
//        }
        if (ContextCompat.checkSelfPermission(ConversionActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            convertFiles();
        } else {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            convertFiles();
        } else {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void convertFiles() {
        final File dir = new File(Environment.getExternalStorageDirectory(), "Sculpt VR");
        if (!dir.exists())
            dir.mkdir();
//        convertFile("models/icosphere.ply", dir, "icosphere_med");
//        convertFile("models/icosphere2.ply", dir, "icosphere_hi");
//        convertFile("models/human1.ply", dir, "human1");
//        convertFile("models/human2.ply", dir, "human2");
        exportBVH(Assets.ICOSPHERE_MESH_MED, dir, "icosphere_med");
//        exportBVH(Assets.ICOSPHERE_MESH_HI, dir, "icosphere_hi");
    }

    private void exportBVH(String asset, File directory, final String outFilename) {
        progressBar.setVisibility(View.VISIBLE);
        count++;
        new Thread(() -> {
            try {
                Long t0;
                
                Logger.d("starting bvh export");
                final SculptMeshData meshData = SculptMeshParser.parse(getAssets().open(asset));

                t0 = System.currentTimeMillis();
                final BVH bvh = new BVH(meshData, BVHBuilder.Method.SAH, 2);
                Logger.d("bvh construction took " + (System.currentTimeMillis() - t0) + "ms");

                final File file = new File(directory, outFilename + ".bvh");
                file.setReadable(true);
                file.setWritable(true);
                file.setExecutable(true);

                BVHFileIO.serialize(bvh, file);

                final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                BVHFileIO.serialize(bvh, byteArrayOutputStream);
                final String out = byteArrayOutputStream.toString();
                Logger.d("OUT ->\n" + out);

                t0 = System.currentTimeMillis();
                final BVH bvhIn = BVHFileIO.deserialize(meshData, file);
                Logger.d("bvh load from file took " + (System.currentTimeMillis() - t0) + "ms");

                final ByteArrayOutputStream byteArrayOutputStream2 = new ByteArrayOutputStream();
                BVHFileIO.serialize(bvhIn, byteArrayOutputStream2);
                final String in = byteArrayOutputStream2.toString();
                Logger.d("IN ->\n" + in);
                Logger.d("IN equals OUT: " + in.equals(out));

                final Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                intent.setData(Uri.fromFile(file));
                sendBroadcast(intent);
                runOnUiThread(() -> {
                    Toast.makeText(ConversionActivity.this, outFilename + " successfully saved", Toast.LENGTH_SHORT).show();
                    count--;
                    if (count == 0)
                        progressBar.setVisibility(View.GONE);
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(ConversionActivity.this, outFilename + " failed to save", Toast.LENGTH_SHORT).show();
                    count--;
                    if (count == 0)
                        progressBar.setVisibility(View.GONE);
                });
                Logger.e("export failed", e);
            }
        }).start();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressLint({"SetWorldReadable", "SetWorldWritable"})
    private void convertFile(final String inAsset, File directory, final String outFilename) {
        progressBar.setVisibility(View.VISIBLE);
        count++;
        new Thread(() -> {
            try {
                final SculptMeshData meshData = PLYConverter.createMeshData(getAssets().open(inAsset), outFilename);
//                Log.d(SculptingVrGame.class.getSimpleName(), "mesh symmetry: \n" + Arrays.toString(SymmetryMapUtils.extractSymmetryMap(meshData)));
                meshData.setOriginalAssetName(outFilename);
                for (Vertex vertex : meshData.vertices) {
                    vertex.flagNeedsUpdate();
                }
                for (Vertex vertex : meshData.vertices) {
                    if ((vertex.flag & Vertex.FLAG_UPDATE) == Vertex.FLAG_UPDATE) {
                        vertex.recalculateNormal();
                        vertex.clearUpdateFlag();
                    }
                }

                final File sculptFile = new File(directory, outFilename + ".sculpt");
                sculptFile.setReadable(true);
                sculptFile.setWritable(true);
                sculptFile.setExecutable(true);

                SculptMeshWriter.writeToFile(sculptFile, meshData, new Matrix4());
                Log.d(ConversionActivity.class.getSimpleName(), "sculpt file written successfully: " + sculptFile.getAbsolutePath());

                final Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                intent.setData(Uri.fromFile(sculptFile));
                sendBroadcast(intent);

            } catch (IOException e) {
                e.printStackTrace();

                final String msg = e.getMessage();
                runOnUiThread(() -> {
                    Toast.makeText(ConversionActivity.this, msg, Toast.LENGTH_SHORT).show();
                    count--;
                    if (count == 0)
                        progressBar.setVisibility(View.GONE);
                });
            }
            runOnUiThread(() -> {
                Toast.makeText(ConversionActivity.this, outFilename + " successfully saved", Toast.LENGTH_SHORT).show();
                count--;
                if (count == 0)
                    progressBar.setVisibility(View.GONE);
            });
        }).start();
    }
}
