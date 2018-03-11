package net.masonapps.clayvr.environment;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

import org.masonapps.libgdxgooglevr.ui.VirtualStage;

/**
 * Created by Bob Mason on 10/16/2017.
 */

public class UnitGrid extends VirtualStage {

    private final Skin skin;
    private Texture gridTexture;
    private Actor grid;

    public UnitGrid(Batch batch, Skin skin) {
        super(batch, 1000, 1000);
        this.skin = skin;
        createGrid();
        createLabels();
    }

    private void createGrid() {
        final Pixmap pixmap = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.CLEAR);
        pixmap.fill();
        pixmap.setColor(Color.WHITE);
        pixmap.drawRectangle(0, 0, 32, 32);
        pixmap.drawRectangle(1, 1, 31, 31);
        gridTexture = new Texture(pixmap);
        gridTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        grid = new Actor() {
            @Override
            public void draw(Batch batch, float parentAlpha) {
                batch.draw(gridTexture, 0, 0, 1000, 1000, 0, 0, gridTexture.getWidth() * 10, gridTexture.getWidth() * 10, false, false);
            }
        };
    }

    private void createLabels() {

    }

    @Override
    public void dispose() {
        super.dispose();
        gridTexture.dispose();
    }
}
