package net.masonapps.clayvr.sculpt;

import android.content.SharedPreferences;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.ImageTextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Pools;

import net.masonapps.clayvr.R;
import net.masonapps.clayvr.Style;
import net.masonapps.clayvr.math.Side;
import net.masonapps.clayvr.ui.ColorPickerSimple;
import net.masonapps.clayvr.ui.ConfirmDialog;
import net.masonapps.clayvr.ui.DialogVR;
import net.masonapps.clayvr.ui.ExportDialog;
import net.masonapps.clayvr.ui.VerticalImageTextButton;
import net.masonapps.clayvr.ui.ViewControls;

import org.masonapps.libgdxgooglevr.math.CylindricalCoordinate;
import org.masonapps.libgdxgooglevr.ui.CylindricalWindowUiContainer;
import org.masonapps.libgdxgooglevr.ui.WindowTableVR;
import org.masonapps.libgdxgooglevr.ui.WindowVR;

import java.util.function.Consumer;

/**
 * Created by Bob on 7/24/2017.
 */

public class SculptingInterface extends CylindricalWindowUiContainer {

    public static final String WINDOW_BUTTON_BAR = "winBtnBar";
    public static final String WINDOW_COLOR_PICKER = "winColorPicker";
    public static final String WINDOW_BRUSH_SETTINGS = "winBrushSettings";
    public static final String WINDOW_VIEW_CONTROLS = "winViewControls";
    private static final float PADDING = 8f;
    private final Brush brush;
    private final Skin skin;
    private final SculptUiEventListener eventListener;
    private final DialogVR brushTypeTable;
    private final WindowTableVR brushSettingsTable;
    private final WindowTableVR buttonBar;
    private final ColorPickerSimple colorPicker;
    private final ConfirmDialog confirmDialog;
    private final ViewControls viewControls;
    private final ExportDialog exportDialog;
    private ImageButton brushButton;
    private CheckBox flipCheckBox;
    private CheckBox symmetryCheckBox;

    public SculptingInterface(Brush brush, Batch spriteBatch, Skin skin, SculptUiEventListener listener) {
        super(2f, 4f);
        this.brush = brush;
        this.skin = skin;
        this.eventListener = listener;
        final WindowVR.WindowVrStyle windowStyleWithClose = Style.createWindowVrStyle(skin);
        windowStyleWithClose.closeDrawable = skin.newDrawable(Style.Drawables.ic_close);
        buttonBar = new WindowTableVR(spriteBatch, skin, 560, 112, Style.createWindowVrStyle(skin));
        colorPicker = new ColorPickerSimple(spriteBatch, skin, 448, 448, Style.getStringResource(R.string.title_color_picker, "Color"), windowStyleWithClose);
        brushTypeTable = new DialogVR(spriteBatch, skin, 0, 0);
        confirmDialog = new ConfirmDialog(spriteBatch, skin);
        brushSettingsTable = new WindowTableVR(spriteBatch, skin, 448, 480, Style.getStringResource(R.string.title_brush_settings, "Brush Settings"), windowStyleWithClose);
        viewControls = new ViewControls(spriteBatch, skin, windowStyleWithClose);
        exportDialog = new ExportDialog(spriteBatch, skin, eventListener::onExportClicked);
        initBrushSettingsStage();
        initButtonBar();
        initColorTable();
        initConfirmDialog();
        initExportDialog();
        initBrushTypeTable();
        initViewControls();
    }

    private void initBrushSettingsStage() {
        final Table table = brushSettingsTable.getTable();

        brushButton = new ImageButton(Style.createImageButtonStyle(skin, Style.Drawables.ic_button_draw));
        brushButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showBrushMenu();
            }
        });
        table.add(brushButton).padTop(PADDING).padBottom(PADDING).padLeft(PADDING).padRight(PADDING).row();

        table.add(Style.getStringResource(R.string.brush_radius, "Brush Radius")).left().pad(PADDING).row();
        final Slider radiusSlider = new Slider(0f, 100f, 1f, false, skin);
        radiusSlider.setValue((brush.getRadius() - Brush.MIN_RADIUS) / (Brush.MAX_RADIUS - Brush.MIN_RADIUS) * 100f);
        table.add(radiusSlider).expandX().fillX().pad(PADDING).row();
        radiusSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                brush.setRadius(radiusSlider.getValue() * 0.01f * (Brush.MAX_RADIUS - Brush.MIN_RADIUS) + Brush.MIN_RADIUS);
            }
        });

        table.add(Style.getStringResource(R.string.brush_strength, "Brush Strength")).left().pad(PADDING).row();
        final Slider strengthSlider = new Slider(0f, 100f, 1f, false, skin);
        strengthSlider.setValue(brush.getStrength() * 100f);
        table.add(strengthSlider).expandX().fillX().pad(PADDING).row();
        strengthSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                brush.setStrength(strengthSlider.getValue() * 0.01f);
            }
        });

        flipCheckBox = new CheckBox(Style.getStringResource(R.string.flip, " flip"), skin);
        flipCheckBox.setChecked(brush.isFlipEnabled());
        flipCheckBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                brush.setFlipEnabled(flipCheckBox.isChecked());
            }
        });
        table.add(flipCheckBox).expandX().center().row();
        flipCheckBox.setVisible(brush.canFlip());

        symmetryCheckBox = new CheckBox(Style.getStringResource(R.string.symmetry, " symmetry"), skin);
        symmetryCheckBox.setChecked(brush.useSymmetry());
        symmetryCheckBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                final String msg = "Are you sure?";
                showConfirmDialog(msg, confirmed -> {
                    if (!confirmed) return;
                    eventListener.onSymmetryChanged(symmetryCheckBox.isChecked());
                });
            }
        });
        table.add(symmetryCheckBox).padTop(PADDING).expandX().center();

        final CylindricalCoordinate coordinate = new CylindricalCoordinate(getRadius(), 130f, 0.45f, CylindricalCoordinate.AngleMode.degrees);
        brushSettingsTable.setPosition(coordinate.toCartesian());
        brushSettingsTable.lookAt(new Vector3(0, coordinate.vertical, 0), Vector3.Y);
        brushSettingsTable.setActivationMovement(0);
        addProcessor(brushSettingsTable);
    }

    private void initButtonBar() {
        final Table buttonBarTable = buttonBar.getTable();

        final VerticalImageTextButton undoBtn = new VerticalImageTextButton(Style.getStringResource(R.string.undo, "undo"), Style.createImageTextButtonStyle(skin, Style.Drawables.ic_undo));
        undoBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                eventListener.onUndoClicked();
            }
        });
        buttonBarTable.add(undoBtn).padTop(PADDING).padBottom(PADDING).padLeft(PADDING).padRight(PADDING);

        final VerticalImageTextButton redoBtn = new VerticalImageTextButton(Style.getStringResource(R.string.redo, "redo"), Style.createImageTextButtonStyle(skin, Style.Drawables.ic_redo));
        redoBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                eventListener.onRedoClicked();
            }
        });
        buttonBarTable.add(redoBtn).padTop(PADDING).padBottom(PADDING).padRight(PADDING);

        final VerticalImageTextButton brushBtn = new VerticalImageTextButton(Style.getStringResource(R.string.brush, "brush"), Style.createImageTextButtonStyle(skin, Style.Drawables.ic_button_brush));
        brushBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                brushSettingsTable.setVisible(!brushSettingsTable.isVisible());
                colorPicker.setVisible(brush.getType() == Brush.Type.VERTEX_PAINT && brushSettingsTable.isVisible());
            }
        });
        buttonBarTable.add(brushBtn).padTop(PADDING).padBottom(PADDING).padRight(PADDING);

        final VerticalImageTextButton viewBtn = new VerticalImageTextButton(Style.getStringResource(R.string.view, "view"), Style.createImageTextButtonStyle(skin, Style.Drawables.ic_rotate));
        viewBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                viewControls.setVisible(!viewControls.isVisible());
            }
        });
        buttonBarTable.add(viewBtn).padTop(PADDING).padBottom(PADDING).padRight(PADDING);

        final VerticalImageTextButton exportBtn = new VerticalImageTextButton(Style.getStringResource(R.string.export, "export"), Style.createImageTextButtonStyle(skin, Style.Drawables.ic_export));
        exportBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (exportDialog.isVisible())
                    exportDialog.hide();
                else
                    exportDialog.show();
            }
        });
        buttonBarTable.add(exportBtn).padTop(PADDING).padBottom(PADDING).padRight(PADDING);

        addProcessor(buttonBar);
        buttonBar.resizeToFitTable();
        final float y = brushSettingsTable.getPosition().y - brushSettingsTable.getHeightWorld() / 2f - buttonBar.getHeightWorld() / 2f - 0.175f;
        buttonBar.setPosition(new CylindricalCoordinate(getRadius(), 135f, y, CylindricalCoordinate.AngleMode.degrees).toCartesian());
        buttonBar.lookAt(new Vector3(0, buttonBar.getPosition().y, 0), Vector3.Y);
    }

    private void initConfirmDialog() {
        confirmDialog.setVisible(false);
        confirmDialog.setBackground(skin.newDrawable(Style.Drawables.window, Style.COLOR_WINDOW));
        confirmDialog.setPosition(new CylindricalCoordinate(getRadius(), 90f, 0f, CylindricalCoordinate.AngleMode.degrees).toCartesian());
        addProcessor(confirmDialog);
    }

    private void initExportDialog() {
        exportDialog.dismiss();
        exportDialog.setBackground(skin.newDrawable(Style.Drawables.window, Style.COLOR_WINDOW));
        exportDialog.setPosition(new CylindricalCoordinate(getRadius(), 90f, 0f, CylindricalCoordinate.AngleMode.degrees).toCartesian());
        addProcessor(exportDialog);
    }

    private void showConfirmDialog(String msg, Consumer<Boolean> consumer) {
        confirmDialog.setMessage(msg);
        confirmDialog.setListener(consumer);
        confirmDialog.show();
    }

    private void initColorTable() {
        final float y = brushSettingsTable.getPosition().y + brushSettingsTable.getHeightWorld() / 2f - colorPicker.getHeightWorld() / 2f;
        final CylindricalCoordinate coordinate = new CylindricalCoordinate(getRadius(), 30f, y, CylindricalCoordinate.AngleMode.degrees);
        colorPicker.setPosition(coordinate.toCartesian());
        colorPicker.lookAt(new Vector3(0, coordinate.vertical, 0), Vector3.Y);
        colorPicker.setColorListener(brush::setColor);
        colorPicker.setVisible(brush.getType() == Brush.Type.VERTEX_PAINT);
        colorPicker.setDropperListener(eventListener::onDropperButtonClicked);
        addProcessor(colorPicker);
    }

    private void initBrushTypeTable() {
        final ImageButton closeBtn = new ImageButton(Style.createImageButtonStyle(skin, Style.Drawables.ic_close));
        closeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                brushTypeTable.setVisible(false);
            }
        });
        brushTypeTable.getTable().add(closeBtn).colspan(3).width(28).height(28).expandX().right().pad(4).row();
        addDrawButton();
        addPinchButton();
        addFlattenButton();
        addGrabButton();
        addSmoothButton();
        addPaintButton();
        brushTypeTable.dismiss();
        brushTypeTable.setBackground(skin.newDrawable(Style.Drawables.window, Style.COLOR_WINDOW));
        brushTypeTable.resizeToFitTable();
        brushTypeTable.setPosition(new CylindricalCoordinate(getRadius(), 90f, 0f, CylindricalCoordinate.AngleMode.degrees).toCartesian());
        addProcessor(brushTypeTable);
    }

    @Override
    public void act() {
        super.act();
    }

    private void addDrawButton() {
        final String name = Style.getStringResource(R.string.draw, "draw");
        final String region = Style.Drawables.ic_button_draw;
        final Brush.Type type = Brush.Type.DRAW;
        addTypeButton(name, region, type, false);
    }

    private void addPinchButton() {
        final String name = Style.getStringResource(R.string.pinch, "pinch");
        final String region = Style.Drawables.ic_button_pinch;
        final Brush.Type type = Brush.Type.PINCH;
        addTypeButton(name, region, type, false);
    }

    private void addFlattenButton() {
        final String name = Style.getStringResource(R.string.flatten, "flatten");
        final String region = Style.Drawables.ic_button_flatten;
        final Brush.Type type = Brush.Type.FLATTEN;
        addTypeButton(name, region, type, true);
    }

    private void addGrabButton() {
        final String name = Style.getStringResource(R.string.grab, "grab");
        final String region = Style.Drawables.ic_button_grab;
        final Brush.Type type = Brush.Type.GRAB;
        addTypeButton(name, region, type, false);
    }

    private void addSmoothButton() {
        final String name = Style.getStringResource(R.string.smooth, "smooth");
        final String region = Style.Drawables.ic_button_smooth;
        final Brush.Type type = Brush.Type.SMOOTH;
        addTypeButton(name, region, type, false);
    }

    private void addPaintButton() {
        final String name = Style.getStringResource(R.string.paint, "paint");
        final String region = Style.Drawables.ic_button_paint;
        final Brush.Type type = Brush.Type.VERTEX_PAINT;
        addTypeButton(name, region, type, true);
    }

    private void addTypeButton(String name, String region, Brush.Type type, boolean row) {
        final ImageTextButton button = new ImageTextButton(name, Style.createImageTextButtonStyle(skin, region));
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                brush.setType(type);
                flipCheckBox.setVisible(brush.canFlip());
                brushButton.getStyle().imageUp = button.getStyle().imageUp;
                hideBrushMenu();
                colorPicker.setVisible(brush.getType() == Brush.Type.VERTEX_PAINT);
            }
        });
        final Cell<ImageTextButton> cell = brushTypeTable.getTable().add(button).fillX().pad(PADDING);
        if (row) cell.row();
    }

    private void initViewControls() {
        final float y = colorPicker.getPosition().y - colorPicker.getHeightWorld() / 2f - viewControls.getHeightWorld() / 2f - 0.175f;
        final CylindricalCoordinate coordinate = new CylindricalCoordinate(getRadius(), 35f, y, CylindricalCoordinate.AngleMode.degrees);
        viewControls.setPosition(coordinate.toCartesian());
        viewControls.lookAt(Vector3.Zero, Vector3.Y);
        viewControls.setListener(eventListener::onViewSelected);
        viewControls.setVisible(false);
        addProcessor(viewControls);
    }

    private void showBrushMenu() {
        brushTypeTable.show();
    }

    private void hideBrushMenu() {
        brushTypeTable.dismiss();
    }

    public void loadWindowPositions(SharedPreferences sharedPreferences) {
        final Vector3 tmp = Pools.obtain(Vector3.class);

        tmp.fromString(sharedPreferences.getString(WINDOW_BUTTON_BAR, buttonBar.getPosition().toString()));
        buttonBar.setPosition(tmp);
        snapDragTableToCylinder(buttonBar);

        tmp.fromString(sharedPreferences.getString(WINDOW_COLOR_PICKER, colorPicker.getPosition().toString()));
        colorPicker.setPosition(tmp);
        snapDragTableToCylinder(colorPicker);

        tmp.fromString(sharedPreferences.getString(WINDOW_BRUSH_SETTINGS, brushSettingsTable.getPosition().toString()));
        brushSettingsTable.setPosition(tmp);
        snapDragTableToCylinder(brushSettingsTable);

        tmp.fromString(sharedPreferences.getString(WINDOW_VIEW_CONTROLS, viewControls.getPosition().toString()));
        viewControls.setPosition(tmp);
        snapDragTableToCylinder(viewControls);

        Pools.free(tmp);
    }

    public void saveWindowPositions(SharedPreferences.Editor editor) {
        editor.putString(WINDOW_BUTTON_BAR, buttonBar.getPosition().toString());
        editor.putString(WINDOW_COLOR_PICKER, colorPicker.getPosition().toString());
        editor.putString(WINDOW_BRUSH_SETTINGS, brushSettingsTable.getPosition().toString());
        editor.putString(WINDOW_VIEW_CONTROLS, viewControls.getPosition().toString());
    }

    public boolean onControllerBackButtonClicked() {
        if (brushTypeTable.isVisible()) {
            brushTypeTable.dismiss();
            return true;
        }
        if (confirmDialog.isVisible()) {
            confirmDialog.dismiss();
            return true;
        }
        if (exportDialog.isVisible()) {
            exportDialog.dismiss();
            return true;
        }
        return false;
    }

    public void setDropperColor(Color color) {
        colorPicker.setDropperColor(color);
    }

    public interface SculptUiEventListener {
        void onDropperButtonClicked();

        void onUndoClicked();

        void onRedoClicked();

        void onExportClicked(ExportDialog.ExportOptions options);

        void onSymmetryChanged(boolean enabled);

        void onViewSelected(Side side);
    }
}
