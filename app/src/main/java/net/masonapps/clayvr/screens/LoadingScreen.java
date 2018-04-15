package net.masonapps.clayvr.screens;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Align;

import net.masonapps.clayvr.Assets;
import net.masonapps.clayvr.Style;

import org.masonapps.libgdxgooglevr.GdxVr;
import org.masonapps.libgdxgooglevr.gfx.VrGame;
import org.masonapps.libgdxgooglevr.gfx.VrWorldScreen;
import org.masonapps.libgdxgooglevr.ui.VirtualStage;

/**
 * Created by Bob on 12/28/2016.
 */
public class LoadingScreen extends VrWorldScreen {

    private static final float SPEED = -360f;
    private static final float Z = -2f;
    private final Image loadingSpinner;
    private final VirtualStage virtualStage;

    public LoadingScreen(VrGame game) {
        super(game);
        getWorld().add(Style.newGradientBackground(getVrCamera().far - 2f));
        setBackgroundColor(Color.BLUE);

        final SpriteBatch spriteBatch = new SpriteBatch();
        final Texture texture = new Texture(Assets.LOADING_SPINNER_ASSET);
        manageDisposable(spriteBatch, texture);
        loadingSpinner = new Image(texture);
        loadingSpinner.setOrigin(Align.center);
        virtualStage = new VirtualStage(spriteBatch, (int) loadingSpinner.getWidth(), (int) loadingSpinner.getHeight());
        virtualStage.setTouchable(false);
        virtualStage.setPosition(0, 0, Z);
        virtualStage.addActor(loadingSpinner);
        loadingSpinner.setPosition(loadingSpinner.getWidth() / 2f, loadingSpinner.getHeight() / 2f);
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
        virtualStage.act();
        loadingSpinner.rotateBy(GdxVr.graphics.getDeltaTime() * SPEED);
    }

    @Override
    public void render(Camera camera, int whichEye) {
        super.render(camera, whichEye);
        virtualStage.draw(camera);
    }
}
