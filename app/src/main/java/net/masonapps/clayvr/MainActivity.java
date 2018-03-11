package net.masonapps.clayvr;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.google.vr.ndk.base.AndroidCompat;
import com.google.vr.ndk.base.DaydreamApi;
import com.google.vr.ndk.base.GvrLayout;

import net.masonapps.clayvr.service.ExportService;

import org.masonapps.libgdxgooglevr.GdxVr;
import org.masonapps.libgdxgooglevr.utils.Logger;
import org.masonapps.libgdxgooglevr.vr.VrActivity;

import java.io.File;
import java.util.Arrays;

public class MainActivity extends VrActivity {
    private static final int RC_PERMISSIONS = 901;
    private static final String READ_EXTERNAL_STORAGE = Manifest.permission.READ_EXTERNAL_STORAGE;
    private static final String WRITE_EXTERNAL_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private StoragePermissionResultListener listener;
    @Nullable
    private RequestStoragePermissionAction action = null;
    @Nullable
    private DaydreamApi daydreamApi = null;
    private SculptingVrGame vrGame;
    private BroadcastReceiver exportCompleteReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        deleteAllProjects();
        
        vrGame = new SculptingVrGame();
        initialize(vrGame);
        exportCompleteReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (action != null && action.equals(ExportService.ACTION_EXPORT_COMPLETE))
                    GdxVr.app.postRunnable(() -> vrGame.onExportComplete());
            }
        };
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void deleteAllProjects() {
        final File[] files = getFilesDir().listFiles();
        Arrays.stream(files)
                .filter(file -> file.getName().endsWith(Constants.FILE_TYPE_SCULPT) || file.getName().endsWith(Constants.FILE_TYPE_SAVE_DATA))
                .forEach(File::delete);
        Toast.makeText(this, "all projects deleted", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void initGvrLayout(GvrLayout layout) {
        if (layout.enableAsyncReprojectionProtected()) {
            Logger.d("Async Reprojection Enabled");
            AndroidCompat.setSustainedPerformanceMode(this, true);
        }
    }

    @Override
    protected void onPause() {
        vrGame.saveCurrentProject();
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(exportCompleteReceiver, new IntentFilter(ExportService.ACTION_EXPORT_COMPLETE));
        daydreamApi = DaydreamApi.create(this);
    }

    @Override
    protected void onStop() {
        unregisterReceiver(exportCompleteReceiver);
        if (daydreamApi != null)
            daydreamApi.close();
        super.onStop();
    }

    public boolean isReadStoragePermissionGranted() {
        return ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isWriteStoragePermissionGranted() {
        return ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public void requestStoragePermissions(StoragePermissionResultListener listener) {

        this.listener = listener;
        action = new RequestStoragePermissionAction(true, true);
        exitVrAndRequestPermissions();
    }

    public void requestWriteStoragePermissions(StoragePermissionResultListener listener) {
        this.listener = listener;
        action = new RequestStoragePermissionAction(false, true);
        exitVrAndRequestPermissions();
    }

    public void requestReadStoragePermissions(StoragePermissionResultListener listener) {
        this.listener = listener;
        action = new RequestStoragePermissionAction(true, false);
        exitVrAndRequestPermissions();
    }

    private void exitVrAndRequestPermissions() {
        if (daydreamApi != null)
            daydreamApi.exitFromVr(this, RC_PERMISSIONS, null);
        else
            requestPermissions();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_PERMISSIONS && resultCode == RESULT_OK) {
            requestPermissions();
        }
    }

    private void requestPermissions() {
        if (action != null) {
            if (action.read && action.write)
                ActivityCompat.requestPermissions(this, new String[]{READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE}, RC_PERMISSIONS);
            else if (action.read)
                ActivityCompat.requestPermissions(this, new String[]{READ_EXTERNAL_STORAGE}, RC_PERMISSIONS);
            else if (action.write)
                ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE}, RC_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RC_PERMISSIONS) {
            if (listener != null) {
                listener.onResult(isReadStoragePermissionGranted(), isWriteStoragePermissionGranted());
            }
        }
    }

    public boolean areStoragePermissionsGranted() {
        return isWriteStoragePermissionGranted() && isReadStoragePermissionGranted();
    }

    public interface StoragePermissionResultListener {
        void onResult(boolean readGranted, boolean writeGranted);
    }

    private static class RequestStoragePermissionAction {
        boolean read, write;

        public RequestStoragePermissionAction(boolean read, boolean write) {
            this.read = read;
            this.write = write;
        }
    }
}
