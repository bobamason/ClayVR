package net.masonapps.clayvr.screens;

import android.util.Log;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.model.data.ModelData;

import net.masonapps.clayvr.SculptingVrGame;
import net.masonapps.clayvr.io.FileUtils;
import net.masonapps.clayvr.io.SculptLoader;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Created by Bob on 8/30/2017.
 */

public class OpenProjectScreen extends ModelSelectionScreen<File> {

    private final OpenProjectListener listener;

    public OpenProjectScreen(SculptingVrGame game, List<File> list, OpenProjectListener listener) {
        super(game, game.getSkin(), list);
        this.listener = listener;
    }

    @Override
    public ModelData loadModelData(File file) throws IOException {
        return SculptLoader.getModelData(file);
    }

    @Override
    public void onLoadModelFailed(File file, Throwable e) {
        Log.e(OpenProjectScreen.class.getSimpleName(), file.getName() + " failed to load: " + e.getLocalizedMessage());
    }

    @Override
    public void show() {
        super.show();
    }

    @Override
    public void hide() {
        super.hide();
    }

    @Override
    public void onOpenClicked(final File file) {
        listener.onOpenProject(file);
    }

    @Override
    public void onCopyClicked(final File file) {
        CompletableFuture.runAsync(() -> {
            final File parent = file.getParentFile();
            final String fileName = file.getName();
            final int endIndex = fileName.lastIndexOf('.');
            if (endIndex == -1) return;

            String oldName = fileName.substring(0, endIndex);
            final String copyStr = "_copy";
            final int index = oldName.indexOf(copyStr);
            int i = 2;
            if (index != -1) {
                if (index + copyStr.length() < oldName.length()) {
                    try {
                        i = Integer.parseInt(oldName.substring(index + copyStr.length()));
                    } catch (NumberFormatException ignored) {
                        i = 2;
                    }
                }
                oldName = oldName.substring(0, index);
            }
            final String ext = fileName.substring(endIndex);

            File copy = new File(parent, oldName + copyStr + ext);
            while (copy.exists()) {
                copy = new File(parent, oldName + copyStr + i + ext);
                i++;
            }
            try {
                copy.createNewFile();
                FileUtils.copyFile(file, copy);
                ui.setList(((SculptingVrGame) game).getProjectFileList());
                Log.d(OpenProjectScreen.class.getSimpleName(), "file copied to " + copy.getName());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onDeleteClicked(final File file) {
        CompletableFuture.runAsync(() -> {
            final String fileName = file.getName();
            final boolean deleted = file.delete();
            if (deleted) {
                ui.setList(((SculptingVrGame) game).getProjectFileList());
                Log.d(OpenProjectScreen.class.getSimpleName(), "file " + fileName + " deleted");
            }
        });
    }

    @Override
    public void update() {
        super.update();
    }

    @Override
    public void render(Camera camera, int whichEye) {
        super.render(camera, whichEye);
    }

    public interface OpenProjectListener {
        void onOpenProject(File file);
    }
}
