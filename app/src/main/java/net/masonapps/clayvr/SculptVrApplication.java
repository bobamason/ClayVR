package net.masonapps.clayvr;

import android.app.Application;
import android.support.annotation.Nullable;

import com.badlogic.gdx.math.Matrix4;

import net.masonapps.clayvr.mesh.SculptMeshData;

/**
 * Created by Bob Mason on 10/9/2017.
 */

public class SculptVrApplication extends Application {

    private final Matrix4 transform = new Matrix4();
    @Nullable
    private SculptMeshData meshData = null;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onTerminate() {
        meshData = null;
        super.onTerminate();
    }

    @Nullable
    public SculptMeshData getMeshData() {
        return meshData;
    }

    public void setMeshData(@Nullable SculptMeshData meshData, Matrix4 transform) {
        this.meshData = meshData;
        this.transform.set(transform);
    }

    public Matrix4 getTransform() {
        return transform;
    }
}
