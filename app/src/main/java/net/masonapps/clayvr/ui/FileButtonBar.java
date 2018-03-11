package net.masonapps.clayvr.ui;

import android.support.annotation.Nullable;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import net.masonapps.clayvr.R;
import net.masonapps.clayvr.Style;

import org.masonapps.libgdxgooglevr.ui.TableVR;

public class FileButtonBar<T> extends TableVR {

    @Nullable
    private T t = null;
    private OnFileButtonClicked<T> listener;

    public FileButtonBar(Batch batch, Skin skin, int tableWidth, int tableHeight, OnFileButtonClicked<T> listener) {
        super(batch, skin, tableWidth, tableHeight);
        this.listener = listener;
        final Table buttonBarTable = getTable();
        setBackground(skin.newDrawable(Style.Drawables.window, Style.COLOR_WINDOW));

        final VerticalImageTextButton openBtn = new VerticalImageTextButton(Style.getStringResource(R.string.open, "open"), Style.createImageTextButtonStyle(skin, Style.Drawables.ic_open));
        openBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (t != null)
                    listener.onOpenClicked(t);
            }
        });
        final float padding = 8f;
        buttonBarTable.add(openBtn).padTop(padding).padBottom(padding).padLeft(padding).padRight(padding);

        final VerticalImageTextButton copyBtn = new VerticalImageTextButton(Style.getStringResource(R.string.copy, "copy"), Style.createImageTextButtonStyle(skin, Style.Drawables.ic_copy));
        copyBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (t != null)
                    listener.onCopyClicked(t);
            }
        });
        buttonBarTable.add(copyBtn).padTop(padding).padBottom(padding).padRight(padding);

        final VerticalImageTextButton deleteBtn = new VerticalImageTextButton(Style.getStringResource(R.string.delete, "export"), Style.createImageTextButtonStyle(skin, Style.Drawables.ic_delete));
        deleteBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (t != null)
                    FileButtonBar.this.listener.onDeleteClicked(t);
            }
        });
        buttonBarTable.add(deleteBtn).padTop(padding).padBottom(padding).padRight(padding);

        resizeToFitTable();
    }

    public void setT(@Nullable T t) {
        this.t = t;
    }

    public interface OnFileButtonClicked<T> {

        void onOpenClicked(final T t);

        void onCopyClicked(final T t);

        void onDeleteClicked(final T t);
    }
}