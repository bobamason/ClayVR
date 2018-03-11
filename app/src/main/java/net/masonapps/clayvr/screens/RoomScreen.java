package net.masonapps.clayvr.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;

import net.masonapps.clayvr.SculptingVrGame;

import org.masonapps.libgdxgooglevr.gfx.VrGame;
import org.masonapps.libgdxgooglevr.gfx.VrWorldScreen;
import org.masonapps.libgdxgooglevr.gfx.World;

/**
 * Created by Bob on 12/28/2016.
 */
public abstract class RoomScreen extends VrWorldScreen {
    private final SculptingVrGame sculptingVrGame;

    public RoomScreen(VrGame game) {
        super(game);
        setBackgroundColor(Color.SKY);
        if (!(game instanceof SculptingVrGame))
            throw new IllegalArgumentException("game must be SculptingVrGame");
        sculptingVrGame = (SculptingVrGame) game;
    }

    @Override
    protected Environment createEnvironment() {
        final Environment environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, Color.DARK_GRAY));
        return environment;
    }

    @Override
    protected World createWorld() {
        return new World() {
            @Override
            public void render(ModelBatch batch, Environment lights) {
                final ModelInstance roomInstance = sculptingVrGame.getRoomInstance();
                if (roomInstance != null)
                    batch.render(roomInstance, getEnvironment());
                super.render(batch, lights);
            }
        };
    }

    public SculptingVrGame getSculptingVrGame() {
        return sculptingVrGame;
    }

    public abstract void onControllerBackButtonClicked();
}
