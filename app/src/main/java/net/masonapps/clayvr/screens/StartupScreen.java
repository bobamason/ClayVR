package net.masonapps.clayvr.screens;

import android.support.annotation.NonNull;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.environment.BaseLight;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;

import net.masonapps.clayvr.Assets;
import net.masonapps.clayvr.R;
import net.masonapps.clayvr.SculptingVrGame;
import net.masonapps.clayvr.Style;
import net.masonapps.clayvr.ui.VerticalImageTextButton;

import org.masonapps.libgdxgooglevr.GdxVr;
import org.masonapps.libgdxgooglevr.math.CylindricalCoordinate;
import org.masonapps.libgdxgooglevr.ui.VirtualStage;
import org.masonapps.libgdxgooglevr.ui.VrUiContainer;

/**
 * Created by Bob on 8/10/2017.
 */

public class StartupScreen extends RoomScreen {


    private final StartupInterface ui;

    public StartupScreen(SculptingVrGame sculptingVrGame, StartupScreenListener listener) {
        super(sculptingVrGame);
        ui = new StartupInterface(new SpriteBatch(), ((SculptingVrGame) game).getSkin(), listener);
//        getWorld().add(Style.newGradientBackground(getVrCamera().far - 1f));
//        getWorld().add(Grid.newInstance(20f, 0.5f, 0.02f, Color.WHITE, Color.DARK_GRAY)).setPosition(0, -1.3f, 0);
    }

    @Override
    protected void addLights(Array<BaseLight> lights) {
        final DirectionalLight light = new DirectionalLight();
        light.setColor(Color.WHITE);
        light.setDirection(new Vector3(1, -1, -1).nor());
        lights.add(light);
    }

    @Override
    public void update() {
        super.update();
        ui.act();
    }

    @Override
    public void render(Camera camera, int whichEye) {
        super.render(camera, whichEye);
        ui.draw(camera);
    }

    @Override
    public void show() {
        super.show();
        GdxVr.input.setInputProcessor(ui);
    }

    @Override
    public void hide() {
        super.hide();
        GdxVr.input.setInputProcessor(null);
    }

    @Override
    public void onControllerBackButtonClicked() {
        ui.onControllerBackButtonClicked();
    }

    public interface StartupScreenListener {
        void onCreateNewProjectClicked(String asset);

        void onOpenProjectClicked();
    }

    private static class StartupInterface extends VrUiContainer {

        private static final float PADDING = 10f;
        private final Batch spriteBatch;
        private final Skin skin;
        private StartupScreenListener listener;
        private final CylindricalCoordinate cylCoord = new CylindricalCoordinate();
        private VrUiContainer mainContainer;
        private VrUiContainer templateSelectionContainer;

        public StartupInterface(Batch spriteBatch, Skin skin, StartupScreenListener listener) {
            super();
            this.spriteBatch = spriteBatch;
            this.skin = skin;
            this.listener = listener;
            mainContainer = new VrUiContainer();
            templateSelectionContainer = new VrUiContainer();
            templateSelectionContainer.setVisible(false);
            addProcessor(mainContainer);
            addProcessor(templateSelectionContainer);
            initMainLayout();
            initTemplateSelectionLayout();
        }

        private void initMainLayout() {
            // create new project
            final VirtualStage newBtnStage = new VirtualStage(spriteBatch, 128, 128);
            newBtnStage.setActivationMovement(0.125f);
            final VerticalImageTextButton newBtn = new VerticalImageTextButton(Style.getStringResource(R.string.new_project, "New Project"), Style.createImageTextButtonStyle(skin, Style.Drawables.new_project));
            newBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (!templateSelectionContainer.isVisible()) {
                        templateSelectionContainer.setVisible(true);
                        mainContainer.setVisible(false);
                    }
                }
            });
            newBtnStage.setSize((int) newBtn.getWidth(), (int) newBtn.getHeight());
            newBtnStage.addActor(newBtn);
            cylCoord.set(2f, -25f, 0f);
            newBtnStage.setPosition(cylCoord.toCartesian());
            newBtnStage.lookAt(Vector3.Zero, Vector3.Y);
            mainContainer.addProcessor(newBtnStage);

            // open project
            final VirtualStage openBtnStage = new VirtualStage(spriteBatch, 128, 128);
            openBtnStage.setActivationMovement(0.125f);
            final VerticalImageTextButton openBtn = new VerticalImageTextButton(Style.getStringResource(R.string.open_project, "Open Project"), Style.createImageTextButtonStyle(skin, Style.Drawables.open_project));
            openBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    listener.onOpenProjectClicked();
                }
            });
            openBtnStage.setSize((int) openBtn.getWidth(), (int) openBtn.getHeight());
            openBtnStage.addActor(openBtn);
            cylCoord.set(2f, 25f, 0f);
            openBtnStage.setPosition(cylCoord.toCartesian());
            openBtnStage.lookAt(Vector3.Zero, Vector3.Y);
            mainContainer.addProcessor(openBtnStage);
        }

        private void initTemplateSelectionLayout() {
            final VirtualStage sphereBtn = createTemplateButton(R.string.sphere, "Sphere", Style.Drawables.new_project, Assets.ICOSPHERE_MESH_MED);
            cylCoord.set(2f, -20f, 0f);
            sphereBtn.setPosition(cylCoord.toCartesian());
            sphereBtn.lookAt(Vector3.Zero, Vector3.Y);
            templateSelectionContainer.addProcessor(sphereBtn);

            final VirtualStage humanBtn = createTemplateButton(R.string.human, "Human", Style.Drawables.new_project, Assets.HUMAN_TEMPLATE);
            cylCoord.set(2f, 20f, 0f);
            humanBtn.setPosition(cylCoord.toCartesian());
            humanBtn.lookAt(Vector3.Zero, Vector3.Y);
            templateSelectionContainer.addProcessor(humanBtn);
        }

        @NonNull
        private VirtualStage createTemplateButton(int strRes, String strDefault, String drawableName, final String asset) {
            final VirtualStage stage = new VirtualStage(spriteBatch, 128, 128);
            stage.setActivationMovement(0.125f);
            final VerticalImageTextButton button = new VerticalImageTextButton(Style.getStringResource(strRes, strDefault), Style.createImageTextButtonStyle(skin, drawableName));
            button.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    listener.onCreateNewProjectClicked(asset);
                }
            });
            stage.setSize((int) button.getWidth(), (int) button.getHeight());
            stage.addActor(button);
            return stage;
        }

        @Override
        public void act() {
            super.act();
        }

        public void onControllerBackButtonClicked() {
            if (templateSelectionContainer.isVisible()) {
                templateSelectionContainer.setVisible(false);
                mainContainer.setVisible(true);
            }
        }
    }
}
