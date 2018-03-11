package net.masonapps.clayvr.ui;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import net.masonapps.clayvr.R;
import net.masonapps.clayvr.Style;
import net.masonapps.clayvr.math.Side;

import org.masonapps.libgdxgooglevr.ui.WindowTableVR;

public class ViewControls extends WindowTableVR {


    private ViewControlListener listener;

    public ViewControls(Batch batch, Skin skin, WindowVrStyle windowStyle) {
        super(batch, skin, 200, 100, Style.getStringResource(R.string.view, "View"), windowStyle);
        initViewSideButtons(skin);
        resizeToFitTable();
    }

    public void setListener(ViewControlListener listener) {
        this.listener = listener;
    }

    protected void initViewSideButtons(Skin skin) {
        final float pad = 8f;

        final TextButton frontBtn = new TextButton("front", skin);
        frontBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (listener != null)
                    listener.onViewSelected(Side.FRONT);
            }
        });
        table.add(frontBtn).padTop(pad).padLeft(pad).padRight(pad);

        final TextButton leftBtn = new TextButton("left", skin);
        leftBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (listener != null)
                    listener.onViewSelected(Side.LEFT);
            }
        });
        table.add(leftBtn).padTop(pad).padRight(pad);

        final TextButton topBtn = new TextButton("top", skin);
        topBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (listener != null)
                    listener.onViewSelected(Side.TOP);
            }
        });
        table.add(topBtn).padTop(pad).padRight(pad).row();

        final TextButton backBtn = new TextButton("back", skin);
        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (listener != null)
                    listener.onViewSelected(Side.BACK);
            }
        });
        table.add(backBtn).padTop(pad).padLeft(pad).padRight(pad);

        final TextButton rightBtn = new TextButton("right", skin);
        rightBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (listener != null)
                    listener.onViewSelected(Side.RIGHT);
            }
        });
        table.add(rightBtn).padTop(pad).padRight(pad);

        final TextButton bottomBtn = new TextButton("bottom", skin);
        bottomBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (listener != null)
                    listener.onViewSelected(Side.BOTTOM);
            }
        });
        table.add(bottomBtn).padTop(pad).padRight(pad).row();
    }

    public interface ViewControlListener {
        void onViewSelected(Side side);
    }
}