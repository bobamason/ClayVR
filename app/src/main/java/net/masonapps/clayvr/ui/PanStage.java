package net.masonapps.clayvr.ui;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;

import net.masonapps.clayvr.Style;

import org.masonapps.libgdxgooglevr.ui.VirtualStage;
import org.masonapps.libgdxgooglevr.utils.Logger;

/**
 * Created by Bob Mason on 4/12/2018.
 */
public class PanStage extends VirtualStage {

    private final OnPanListener listener;
    private float startX;
    private float startY;
    private boolean isDragging = false;

    public PanStage(Batch batch, Skin skin, OnPanListener listener) {
        super(batch, 1000, 1000);
        this.listener = listener;
        setTouchable(true);
        final Image image = new Image(skin.newDrawable(Style.Drawables.pan_arrows, Style.COLOR_PRIMARY_LIGHT));
        image.setPosition(getWidth() / 2, getHeight() / 2, Align.center);
        addActor(image);
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (isCursorOver && getHitPoint3D() != null) {
            startX = getHitPoint3D().x;
            startY = getHitPoint3D().y;
            isDragging = true;
        }
        Logger.d("pan touchDown isDragging = " + isDragging + " startX = " + startX + " startY = " + startY);
        return isDragging;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (isDragging && getHitPoint3D() != null) {
            final float dx = getHitPoint3D().x - startX;
            final float dy = getHitPoint3D().y - startY;
            listener.onPan(dx, dy);
            Logger.d("pan dragged dx = " + dx + " dy = " + dy);
        }
        return isDragging;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        isDragging = false;
        return true;
    }

    public interface OnPanListener {
        void onPan(float dx, float dy);
    }
}
