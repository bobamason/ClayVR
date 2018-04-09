package net.masonapps.clayvr;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.ImageTextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.google.vr.sdk.audio.GvrAudioEngine;
import com.google.vr.sdk.controller.Controller;

import net.masonapps.clayvr.bvh.BVH;
import net.masonapps.clayvr.bvh.BVHBuilder;
import net.masonapps.clayvr.bvh.BVHFileIO;
import net.masonapps.clayvr.environment.SkyDomeBuilder;
import net.masonapps.clayvr.io.SculptMeshParser;
import net.masonapps.clayvr.mesh.SculptMeshData;
import net.masonapps.clayvr.screens.LoadingScreen;
import net.masonapps.clayvr.screens.OpenProjectScreen;
import net.masonapps.clayvr.screens.ProgressLoadingScreen;
import net.masonapps.clayvr.screens.RoomScreen;
import net.masonapps.clayvr.screens.SculptingScreen;
import net.masonapps.clayvr.screens.StartupScreen;
import net.masonapps.clayvr.sculpt.SculptMesh;
import net.masonapps.clayvr.service.ExportService;

import org.masonapps.libgdxgooglevr.GdxVr;
import org.masonapps.libgdxgooglevr.gfx.VrGame;
import org.masonapps.libgdxgooglevr.gfx.VrScreen;
import org.masonapps.libgdxgooglevr.input.DaydreamButtonEvent;
import org.masonapps.libgdxgooglevr.input.VrInputProcessor;
import org.masonapps.libgdxgooglevr.utils.Logger;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

/**
 * Created by Bob on 5/26/2017.
 */

public class SculptingVrGame extends VrGame {

    private boolean isAtlasLoaded = false;
    private LoadingScreen loadingScreen;
    private ProgressLoadingScreen progressLoadingScreen;
    @Nullable
    private SculptingScreen sculptingScreen = null;
    private Skin skin;
    @Nullable
    private StartupScreen startupScreen = null;
    private int buttonSoundId = GvrAudioEngine.INVALID_ID;
    @Nullable
    private ModelInstance roomInstance = null;
    private boolean loadingFailed = false;
    private boolean appButtonDown = false;

    @SuppressLint("SimpleDateFormat")
    public static String generateNewProjectName() {
        return "sculpt_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
//        return "sculpt_" + MathUtils.random(5000);
    }

    private static Model createSkySphere(ModelBuilder builder, Texture texture, float radius) {
        builder.begin();
        final MeshPartBuilder part = builder.part("s", GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.TextureCoordinates, new Material(TextureAttribute.createDiffuse(texture), IntAttribute.createCullFace(GL20.GL_FRONT)));
        final float diameter = radius * 2f;
        SkyDomeBuilder.build(part, diameter * 0.5f, 64, 16);
        part.setVertexTransform(new Matrix4().setToTranslation(0, -2.0f, 0));

        final MeshPartBuilder g1 = builder.part("g1", GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal, new Material(ColorAttribute.createDiffuse(Color.DARK_GRAY)));
        final int hsize = 16;
        final float r2 = 12f * 12f;
        for (int z = -hsize; z < hsize; z++) {
            for (int x = -hsize; x < hsize; x++) {
                if (Math.abs(x) % 2 == Math.abs(z) % 2) continue;
                float y = -2.6f;
                if (x * x + z * z > r2)
                    y += MathUtils.random(4) * 0.25f + 0.25f;
                BoxShapeBuilder.build(g1, x + 0.5f, y, z - 0.5f, 1f, 1f, 1f);
            }
        }
        final MeshPartBuilder g2 = builder.part("g2", GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal, new Material(ColorAttribute.createDiffuse(Color.GRAY)));
        for (int z = -hsize; z < hsize; z++) {
            for (int x = -hsize; x < hsize; x++) {
                if (Math.abs(x) % 2 != Math.abs(z) % 2) continue;
                float y = -2.6f;
                if (x * x + z * z > r2)
                    y += MathUtils.random(4);
                BoxShapeBuilder.build(g2, x + 0.5f, y, z - 0.5f, 1f, 1f, 1f);
            }
        }
        return builder.end();
    }

    @Override
    public void create() {
        super.create();
        getVrCamera().near = 0.1f;
        getVrCamera().far = 50f;
        loadingScreen = new LoadingScreen(this);
        setScreen(loadingScreen);
        skin = new Skin();
        loadAsset(Style.ATLAS_FILE, TextureAtlas.class);
        final TextureLoader.TextureParameter textureParameter = new TextureLoader.TextureParameter();
        textureParameter.minFilter = Texture.TextureFilter.Linear;
        textureParameter.magFilter = Texture.TextureFilter.Linear;
        loadAsset(Assets.SKY_TEXTURE, Texture.class, textureParameter);
    }

    @Override
    public void preloadSoundFiles(GvrAudioEngine gvrAudioEngine) {
        if (!gvrAudioEngine.preloadSoundFile(Assets.BUTTON_SOUND_FILE))
            Logger.e("load sound " + Assets.BUTTON_SOUND_FILE + " failed");
    }

    @Override
    protected void doneLoading(AssetManager assets) {
        if (!isAtlasLoaded) {
//            final Texture skyTexture = assets.get(Assets.SKY_TEXTURE, Texture.class);
//            final Model skySphere = createSkySphere(new ModelBuilder(), skyTexture, getVrCamera().far - 1f);
//            roomInstance = new ModelInstance(skySphere, new Matrix4().rotate(Vector3.Y, 180));

            final TextureAtlas atlas = assets.get(Style.ATLAS_FILE, TextureAtlas.class);
            getSkin().addRegions(atlas);
            setupSkin();
            progressLoadingScreen = new ProgressLoadingScreen(this, getSkin());
            switchToStartupScreen();
            isAtlasLoaded = true;
        }
    }

    @Override
    public void update() {
        super.update();
    }

    @Override
    public void dispose() {
        super.dispose();
        if (loadingScreen != null)
            loadingScreen.dispose();
        if (progressLoadingScreen != null)
            progressLoadingScreen.dispose();
        if (sculptingScreen != null)
            sculptingScreen.dispose();
    }

    private void resetProgressLoadingScreen() {
        GdxVr.app.postRunnable(() -> {
            if (progressLoadingScreen != null) {
                progressLoadingScreen.setMessage(Style.getStringResource(R.string.loading, "loading..."));
                progressLoadingScreen.setAnimationEnabled(true);
            }
        });
    }

    private void setLoadingScreenAnimationEnabled(final boolean enabled) {
        GdxVr.app.postRunnable(() -> {
            if (progressLoadingScreen != null)
                progressLoadingScreen.setAnimationEnabled(enabled);
        });
    }

    private void setLoadingScreenMessage(final String msg) {
        GdxVr.app.postRunnable(() -> {
            if (progressLoadingScreen != null)
                progressLoadingScreen.setMessage(msg);
        });
    }

    public void switchToStartupScreen() {
        if (startupScreen == null) {
            startupScreen = new StartupScreen(this, new StartupScreen.StartupScreenListener() {
                @Override
                public void onCreateNewProjectClicked() {
//                switchToNewProjectScreen();
                    createNewProject(Assets.HUMAN_TEMPLATE);
                }

                @Override
                public void onOpenProjectClicked() {
                    switchToOpenProjectScreen();
                }
            });
        }
        setScreen(startupScreen);
        resetProgressLoadingScreen();
    }

    private void switchToOpenProjectScreen() {
        setScreen(new OpenProjectScreen(this, getProjectFileList(), this::openProject));
        resetProgressLoadingScreen();
    }

    private void createNewProject(final String asset) {
        setScreen(progressLoadingScreen);
        loadingFailed = false;
        CompletableFuture.supplyAsync(() -> {
            try {
                setLoadingScreenMessage("loading mesh data...");
                final SculptMeshData meshData = SculptMeshParser.parse(Gdx.files.internal(asset).read());
                setLoadingScreenMessage("creating sculpting mesh...");
                final String bvhAsset = asset.substring(0, asset.lastIndexOf('.')) + "." + BVHFileIO.EXTENSION;
                return BVHFileIO.deserialize(meshData, Gdx.files.internal(bvhAsset).read());
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }).exceptionally(e -> {
            Logger.e("unable to create a new project", e);
            setLoadingScreenMessage(e.getLocalizedMessage());
            return null;
        }).thenAccept(bvh -> {
            if (bvh != null) {
                final String projectName = generateNewProjectName();
                Logger.d("new project created: " + projectName);
                GdxVr.app.postRunnable(() -> setScreen(new SculptingScreen(SculptingVrGame.this, bvh, projectName)));
            } else {
                loadingFailed = true;
                showError("unable to create a new project");
            }
        });
    }

    private void openProject(final File file) {
        setScreen(progressLoadingScreen);
        final String fileName = file.getName();
        loadingFailed = false;
        CompletableFuture.supplyAsync(() -> {
            try {
                setLoadingScreenMessage("loading mesh data...");
                final SculptMeshData meshData = SculptMeshParser.parse(file);
                setLoadingScreenMessage("creating sculpting mesh...");
                String assetName = meshData.getOriginalAssetName();
                final Context context = GdxVr.app.getContext();
                if (context == null || assetName == null || assetName.equals("null")) {
                    return new BVH(meshData, BVHBuilder.Method.SAH, 2);
                } else {
                    final String bvhAsset = Assets.FOLDER_MODELS + assetName + "." + BVHFileIO.EXTENSION;
                    Logger.d("loading BVH asset \'" + bvhAsset + "\'");
                    return BVHFileIO.deserialize(meshData, context.getAssets().open(bvhAsset));
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new CompletionException(e);
            }
        }).exceptionally(e -> {
            Logger.e("unable to open project " + fileName, e);
            setLoadingScreenMessage(e.getLocalizedMessage());
            return null;
        }).thenAccept(bvh -> {
            if (bvh != null) {
                final String projectName = fileName.substring(0, fileName.lastIndexOf('.'));
                GdxVr.app.postRunnable(() -> setScreen(new SculptingScreen(SculptingVrGame.this, bvh, projectName)));
            } else {
                loadingFailed = true;
                showError("unable to open project");
            }
        });
    }

    private void showError(String message) {
        setLoadingScreenMessage(message);
        setLoadingScreenAnimationEnabled(false);
        Logger.e(message);
    }

    public void saveCurrentProject() {
        if (sculptingScreen != null) {
            saveCurrentProject(sculptingScreen.getSculptMesh().getMeshData(), sculptingScreen.getProjectName());
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void saveCurrentProject(final SculptMeshData meshData, final String projectName) {
        Log.d(Constants.APP_NAME, "saving project " + projectName + "...");
        Activity activity = GdxVr.app.getActivityWeakReference().get();
        if (activity != null) {
            ((SculptVrApplication) activity.getApplication()).setMeshData(meshData.copy(), new Matrix4());
            final File file = new File(activity.getFilesDir(), projectName + "." + Constants.FILE_TYPE_SCULPT);
            final Intent intent = new Intent(activity, ExportService.class);
            intent.putExtra(Constants.KEY_FILE_PATH, file.getAbsolutePath());
            intent.putExtra(Constants.KEY_FILE_TYPE, Constants.FILE_TYPE_SCULPT);
            intent.putExtra(Constants.KEY_ASSET_NAME, meshData.getOriginalAssetName());
            intent.putExtra(Constants.KEY_EXTERNAL, false);
            activity.startService(intent);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressLint({"SetWorldReadable", "SetWorldWritable"})
    public void exportFile(final SculptMesh sculptMesh, final String projectName, final String fileType, Matrix4 transform) {
        Activity activity = GdxVr.app.getActivityWeakReference().get();
        if (activity == null || sculptingScreen == null) return;
        if (!((MainActivity) activity).areStoragePermissionsGranted()) {
            ((MainActivity) activity).requestStoragePermissions((readGranted, writeGranted) -> {
                if (writeGranted) {
                    exportFile(sculptMesh, projectName, fileType, transform);
                }
            });
        } else {
            ((SculptVrApplication) activity.getApplication()).setMeshData(sculptMesh.getMeshData().copy(), transform);
            final File dir = new File(Environment.getExternalStorageDirectory(), Constants.EXPORT_FOLDER);
            dir.mkdirs();
            String extension;
            if (fileType.equals(Constants.FILE_TYPE_OBJ))
                extension = "";
            else
                extension = "." + fileType;

            File file = new File(dir, "sculpt" + projectName + extension);
            int i = 2;
            while (file.exists()) {
                file = new File(dir, "sculpt" + projectName + (i++) + extension);
            }
            final Intent intent = new Intent(activity, ExportService.class);
            intent.putExtra(Constants.KEY_FILE_PATH, file.getAbsolutePath());
            intent.putExtra(Constants.KEY_FILE_TYPE, fileType);
            intent.putExtra(Constants.KEY_ASSET_NAME, sculptMesh.getMeshData().getOriginalAssetName());
            intent.putExtra(Constants.KEY_EXTERNAL, true);
            activity.startService(intent);
        }
    }

    private void saveSculptFile(Context context, SculptMesh sculptMesh) {
        final Intent intent = new Intent(context, ExportService.class);
        final File file = new File(context.getFilesDir(), ((SculptingScreen) getScreen()).getProjectName());
        intent.putExtra(Constants.KEY_FILE_PATH, file.getAbsolutePath());
        intent.putExtra(Constants.KEY_FILE_TYPE, Constants.FILE_TYPE_SCULPT);
        intent.putExtra(Constants.KEY_ASSET_NAME, sculptMesh.getMeshData().getOriginalAssetName());
        intent.putExtra(Constants.KEY_EXTERNAL, false);
        intent.putExtra(Constants.KEY_VERTICES, sculptMesh.getTempVertices());
        final short[] indices = new short[sculptMesh.getNumIndices()];
        sculptMesh.getIndices(indices);
        intent.putExtra(Constants.KEY_INDICES, indices);
        intent.putExtra(Constants.KEY_SYMMETRY, sculptMesh.getMeshData().getSymmetryArray());
        context.startService(intent);
    }

    @Override
    public void setScreen(VrScreen screen) {
        super.setScreen(screen);
        if (screen instanceof SculptingScreen) {
            sculptingScreen = (SculptingScreen) screen;
        } else if (screen instanceof ProgressLoadingScreen) {
            resetProgressLoadingScreen();
        }
    }

    @Override
    public void onControllerButtonEvent(Controller controller, DaydreamButtonEvent event) {
        super.onControllerButtonEvent(controller, event);
        if (event.button == DaydreamButtonEvent.BUTTON_APP) {
            if (!appButtonDown && event.action == DaydreamButtonEvent.ACTION_DOWN) {
                appButtonDown = true;
                onControllerBackButtonClicked();
            }
            if (event.action == DaydreamButtonEvent.ACTION_UP) {
                appButtonDown = false;
            }
        } else if (event.button == DaydreamButtonEvent.BUTTON_TOUCHPAD) {
            if (event.action == DaydreamButtonEvent.ACTION_DOWN)
                playButtonSound();
        }
    }

    protected void playButtonSound() {
        final VrInputProcessor inputProcessor = GdxVr.input.getVrInputProcessor();
        if (inputProcessor != null && inputProcessor.isCursorOver() && inputProcessor.getHitPoint3D() != null) {
            final Vector3 hitPoint3D = inputProcessor.getHitPoint3D();
            final GvrAudioEngine gvrAudioEngine = getGvrAudioEngine();
            buttonSoundId = gvrAudioEngine.createSoundObject(Assets.BUTTON_SOUND_FILE);
            if (buttonSoundId != -1) {
                gvrAudioEngine.setSoundObjectPosition(buttonSoundId, hitPoint3D.x, hitPoint3D.y, hitPoint3D.z);
                gvrAudioEngine.playSound(buttonSoundId, false);
            } else {
                Logger.e("sound " + Assets.BUTTON_SOUND_FILE + " failed to play");
            }
        }
    }

    private void onControllerBackButtonClicked() {
        if (getScreen() instanceof RoomScreen)
            ((RoomScreen) getScreen()).onControllerBackButtonClicked();
    }

    public void closeSculptScreen() {
        sculptingScreen.hide();
        saveCurrentProject(sculptingScreen.getSculptMesh().getMeshData(), sculptingScreen.getProjectName());
        sculptingScreen.dispose();
        sculptingScreen = null;
    }

    public boolean isLoadingFailed() {
        return loadingFailed;
    }

    @Nullable
    public SculptingScreen getSculptingScreen() {
        return sculptingScreen;
    }

    private void setupSkin() {
        addFont();
        addSliderStyle();
        addProgressBarStyle();
        addButtonStyle();
        addCheckBoxStyle();
        addLabelStyle();
    }

    private void addFont() {
        getSkin().add(Style.DEFAULT_FONT, new BitmapFont(Gdx.files.internal(Style.FONT_FILE), getSkin().getRegion(Style.FONT_REGION)), BitmapFont.class);
    }

    private void addProgressBarStyle() {
        final ProgressBar.ProgressBarStyle progressBarStyle = new ProgressBar.ProgressBarStyle();
        progressBarStyle.background = getSkin().newDrawable(Style.Drawables.slider, Style.COLOR_UP_2);
        progressBarStyle.knobBefore = getSkin().newDrawable(Style.Drawables.slider, Style.COLOR_ACCENT);
        getSkin().add(Style.DEFAULT_HORIZONTAL, progressBarStyle, ProgressBar.ProgressBarStyle.class);
    }

    private void addSliderStyle() {
        final Slider.SliderStyle sliderStyle = new Slider.SliderStyle(getSkin().newDrawable(Style.Drawables.slider, Style.SLIDER_BG_COLOR), getSkin().newDrawable(Style.Drawables.slider_knob, Style.COLOR_PRIMARY_DARK));
        sliderStyle.knobBefore = getSkin().newDrawable(Style.Drawables.slider, Style.COLOR_PRIMARY_LIGHT);
        getSkin().add(Style.DEFAULT_HORIZONTAL, sliderStyle, Slider.SliderStyle.class);
    }

    private void addButtonStyle() {
        final ImageButton.ImageButtonStyle imageButtonStyle = new ImageButton.ImageButtonStyle();
        imageButtonStyle.up = skin.newDrawable(Style.Drawables.round_button, Style.COLOR_UP);
        imageButtonStyle.over = skin.newDrawable(Style.Drawables.round_button, Style.COLOR_OVER);
        imageButtonStyle.down = skin.newDrawable(Style.Drawables.round_button, Style.COLOR_DOWN);
        imageButtonStyle.disabled = skin.newDrawable(Style.Drawables.round_button, Style.COLOR_DISABLED);
        imageButtonStyle.checked = null;
        getSkin().add(Style.DEFAULT, imageButtonStyle, ImageButton.ImageButtonStyle.class);

        final ImageTextButton.ImageTextButtonStyle imageTextButtonStyle = new ImageTextButton.ImageTextButtonStyle();
        imageTextButtonStyle.font = skin.getFont(Style.DEFAULT_FONT);
        imageTextButtonStyle.up = skin.newDrawable(Style.Drawables.button, Style.COLOR_UP);
        imageTextButtonStyle.over = skin.newDrawable(Style.Drawables.button, Style.COLOR_OVER);
        imageTextButtonStyle.down = skin.newDrawable(Style.Drawables.button, Style.COLOR_DOWN);
        imageTextButtonStyle.checked = null;
        imageTextButtonStyle.fontColor = Style.FONT_COLOR;
        getSkin().add(Style.DEFAULT, imageTextButtonStyle, ImageTextButton.ImageTextButtonStyle.class);


        final TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.font = getSkin().getFont(Style.DEFAULT_FONT);
        textButtonStyle.up = getSkin().newDrawable(Style.Drawables.button, Style.COLOR_UP);
        textButtonStyle.over = getSkin().newDrawable(Style.Drawables.button, Style.COLOR_OVER);
        textButtonStyle.down = getSkin().newDrawable(Style.Drawables.button, Style.COLOR_DOWN);
        textButtonStyle.checked = null;
        textButtonStyle.fontColor = Style.FONT_COLOR;
        getSkin().add(Style.DEFAULT, textButtonStyle, TextButton.TextButtonStyle.class);

        final TextButton.TextButtonStyle toggleStyle = new TextButton.TextButtonStyle();
        toggleStyle.font = getSkin().getFont(Style.DEFAULT_FONT);
        toggleStyle.up = getSkin().newDrawable(Style.Drawables.button, Style.COLOR_UP);
        toggleStyle.over = getSkin().newDrawable(Style.Drawables.button, Style.COLOR_OVER);
        toggleStyle.down = getSkin().newDrawable(Style.Drawables.button, Style.COLOR_DOWN);
        toggleStyle.checked = getSkin().newDrawable(Style.Drawables.button, Style.COLOR_DOWN);
        toggleStyle.fontColor = Style.FONT_COLOR;
        getSkin().add(Style.TOGGLE, toggleStyle, TextButton.TextButtonStyle.class);

        final TextButton.TextButtonStyle listBtnStyle = new TextButton.TextButtonStyle();
        listBtnStyle.font = getSkin().getFont(Style.DEFAULT_FONT);
        listBtnStyle.up = getSkin().newDrawable(Style.Drawables.button, new Color(0, 0, 0, 0.84706f));
        listBtnStyle.over = getSkin().newDrawable(Style.Drawables.button, new Color(0.15f, 0.15f, 0.15f, 0.84706f));
        listBtnStyle.down = getSkin().newDrawable(Style.Drawables.button, Style.COLOR_DOWN);
        listBtnStyle.checked = null;
        listBtnStyle.fontColor = Style.FONT_COLOR;
        getSkin().add(Style.LIST_ITEM, listBtnStyle, TextButton.TextButtonStyle.class);
    }

    private void addCheckBoxStyle() {
        final CheckBox.CheckBoxStyle checkBoxStyle = new CheckBox.CheckBoxStyle();
        checkBoxStyle.checkboxOn = getSkin().newDrawable(Style.Drawables.checkbox_on, Style.COLOR_ACCENT);
        checkBoxStyle.checkboxOff = getSkin().newDrawable(Style.Drawables.checkbox_off, Style.COLOR_ACCENT);
        checkBoxStyle.font = getSkin().getFont(Style.DEFAULT_FONT);
        checkBoxStyle.fontColor = Style.FONT_COLOR;
        getSkin().add(Style.DEFAULT, checkBoxStyle, CheckBox.CheckBoxStyle.class);
    }

    private void addLabelStyle() {
        getSkin().add(Style.DEFAULT, new Label.LabelStyle(getSkin().getFont(Style.DEFAULT_FONT), Style.FONT_COLOR), Label.LabelStyle.class);
    }

    public Skin getSkin() {
        return skin;
    }

    public List<File> getProjectFileList() {
        final Context context = GdxVr.app.getActivityWeakReference().get();
        if (context != null)
            return Arrays.stream(context.getFilesDir().listFiles())
                    .filter(file -> file.getName().endsWith(Constants.FILE_TYPE_SCULPT) || file.getName().endsWith(Constants.FILE_TYPE_SAVE_DATA))
                    .sorted(((o1, o2) -> Long.compare(o2.lastModified(), o1.lastModified())))
                    .collect(Collectors.toList());
        else
            return new ArrayList<>();
    }

    @Nullable
    public ModelInstance getRoomInstance() {
        return roomInstance;
    }

    public void onExportComplete() {
        // TODO: 4/9/2018 notify user 
    }
}
