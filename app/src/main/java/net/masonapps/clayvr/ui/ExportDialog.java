package net.masonapps.clayvr.ui;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import net.masonapps.clayvr.Constants;
import net.masonapps.clayvr.R;
import net.masonapps.clayvr.SculptingVrGame;
import net.masonapps.clayvr.Style;

/**
 * Created by Bob Mason on 3/29/2018.
 */
public class ExportDialog extends DialogVR {

    private static final float PADDING = 8f;
    private final ExportListener listener;

    public ExportDialog(Batch batch, Skin skin, ExportListener listener) {
        super(batch, skin, 100, 100);
        this.listener = listener;

        final Table table = getTable();

        final TextButton stlBtn = new TextButton(Style.getStringResource(R.string.stl, "Stereolithography (.stl)"), skin);
        final TextButton plyBtn = new TextButton(Style.getStringResource(R.string.ply, "Polygon File Format (.ply)"), skin);
        final TextButton objBtn = new TextButton(Style.getStringResource(R.string.obj, "Wavefront OBJ (.obj)"), skin);

        stlBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                exportClicked(Constants.FILE_TYPE_STL);
            }
        });

        objBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                exportClicked(Constants.FILE_TYPE_OBJ);
            }
        });

        plyBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                exportClicked(Constants.FILE_TYPE_PLY);
            }
        });

        table.add(stlBtn)
                .growX()
                .center()
                .padTop(PADDING)
                .padBottom(PADDING)
                .padLeft(PADDING)
                .padRight(PADDING)
                .row();

        table.add(objBtn)
                .growX()
                .center()
                .padBottom(PADDING)
                .padLeft(PADDING)
                .padRight(PADDING)
                .row();

        table.add(plyBtn)
                .growX()
                .center()
                .padBottom(PADDING)
                .padLeft(PADDING)
                .padRight(PADDING)
                .row();

        resizeToFitTable();
    }

    private void exportClicked(String fileType) {
        listener.onExportFile(new ExportOptions(SculptingVrGame.generateNewProjectName(), fileType, new Matrix4()));
    }

    public interface ExportListener {
        void onExportFile(ExportOptions options);
    }

    public static class ExportOptions {
        public String projectName;
        public String fileType;
        public Matrix4 transform;

        public ExportOptions(String projectName, String fileType, Matrix4 transform) {
            this.projectName = projectName;
            this.fileType = fileType;
            this.transform = transform;
        }
    }
}
