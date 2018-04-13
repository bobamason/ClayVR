package net.masonapps.clayvr.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;

import net.masonapps.clayvr.SculptingVrGame;
import net.masonapps.clayvr.Style;
import net.masonapps.clayvr.environment.ModelUtils;

import org.masonapps.libgdxgooglevr.gfx.Entity;
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
        final float environmentOffset = -1.2f;
        getWorld().add(Style.newGradientBackground(getVrCamera().far - 2f)).setPosition(0, environmentOffset, 0);
        final Material floorMat = new Material(TextureAttribute.createDiffuse(getSculptingVrGame().getSkin().getRegion(Style.Drawables.grid)), new BlendingAttribute(true, 1f), FloatAttribute.createAlphaTest(0.1f));
        getWorld().add(new Entity(new ModelInstance(ModelUtils.createFloorRect(new ModelBuilder(), 4f, floorMat)))).setPosition(0, environmentOffset, 0);
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

    @Override
    public void show() {
        super.show();
        getSculptingVrGame().setCursorVisible(true);
        getSculptingVrGame().setControllerVisible(true);
        getVrCamera().position.set(0, 0, 0);
        getVrCamera().direction.set(0, 0, -1);
        getVrCamera().up.set(0, 1, 0);
    }

    public SculptingVrGame getSculptingVrGame() {
        return sculptingVrGame;
    }

    public abstract void onControllerBackButtonClicked();
}
