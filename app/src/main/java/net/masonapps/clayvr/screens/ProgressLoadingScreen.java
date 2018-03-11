package net.masonapps.clayvr.screens;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;

import net.masonapps.clayvr.Style;

import org.masonapps.libgdxgooglevr.GdxVr;
import org.masonapps.libgdxgooglevr.gfx.VrGame;
import org.masonapps.libgdxgooglevr.ui.VirtualStage;

/**
 * Created by Bob on 12/28/2016.
 */
public class ProgressLoadingScreen extends RoomScreen {

    private static final float SPEED = -360f;
    private static final float PADDING = 8f;
    private static final float Z = -2.5f;
    private final VirtualStage virtualStage;
    private final Label label;
    private final Image image;
    private String message = "";
    private boolean isAnimationEnabled = true;

    public ProgressLoadingScreen(VrGame game, Skin skin) {
        super(game);
        final SpriteBatch spriteBatch = new SpriteBatch();
        manageDisposable(spriteBatch);
//        getWorld().add(Style.newGradientBackground(getVrCamera().far - 1f));
        setBackgroundColor(Color.BLACK);
        virtualStage = new VirtualStage(spriteBatch, 400, 100);
        virtualStage.setPosition(0, -0.3f, Z);

        image = new Image(skin.newDrawable(Style.Drawables.loading_spinner, Style.COLOR_ACCENT));
        image.setOrigin(Align.center);
        image.setPosition(virtualStage.getWidth() / 2f, virtualStage.getHeight() / 2f - image.getHeight() / 2f - 10f, Align.center);
        virtualStage.addActor(image);


        label = new Label(message, skin);
        label.setAlignment(Align.center);
        label.setPosition(virtualStage.getWidth() / 2f, virtualStage.getHeight() / 2f - label.getHeight(), Align.center);
        virtualStage.addActor(label);
    }

    public void setMessage(String message) {
        this.message = message;
        label.setText(this.message);
    }

    @Override
    public void resume() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void show() {
        super.show();
        game.setCursorVisible(false);
    }

    @Override
    public void hide() {
        super.hide();
        game.setCursorVisible(true);
    }

    @Override
    public void update() {
        super.update();
        if (isAnimationEnabled)
            image.rotateBy(SPEED * GdxVr.graphics.getDeltaTime());
        virtualStage.act();
    }

    @Override
    public void onCardboardTrigger() {
        super.onCardboardTrigger();
    }


    @Override
    public void render(Camera camera, int whichEye) {
        super.render(camera, whichEye);
        virtualStage.draw(camera);
    }

    public void setAnimationEnabled(boolean animationEnabled) {
        isAnimationEnabled = animationEnabled;
    }

    @Override
    public void onControllerBackButtonClicked() {
        if (getSculptingVrGame().isLoadingFailed())
            getSculptingVrGame().switchToStartupScreen();
    }
}
