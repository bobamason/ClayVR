package net.masonapps.clayvr.ui;

import android.support.annotation.Nullable;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;

import net.masonapps.clayvr.Style;

import java.util.function.Consumer;

/**
 * Created by Bob Mason on 10/12/2017.
 */

public class ConfirmDialog extends DialogVR {

    private final Label label;
    @Nullable
    private Consumer<Boolean> listener = null;

    public ConfirmDialog(Batch batch, Skin skin) {
        super(batch, skin, 0, 0);

        label = new Label("", skin);
        label.setAlignment(Align.center);
        final float paddingLarge = 24f;
        final float padding = 10f;
        getTable().add(label)
                .padTop(paddingLarge)
                .padBottom(padding)
                .padLeft(paddingLarge)
                .padRight(paddingLarge)
                .colspan(2)
                .center()
                .growX()
                .row();

        final TextButton cancelBtn = new TextButton(Style.getStringResource(android.R.string.cancel, "cancel"), skin);
        cancelBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (listener != null)
                    listener.accept(false);
                dismiss();
            }
        });
        getTable().add(cancelBtn)
                .padTop(padding)
                .padBottom(paddingLarge)
                .padLeft(paddingLarge)
                .padRight(padding)
                .center();

        final TextButton okBtn = new TextButton(Style.getStringResource(android.R.string.ok, "ok"), skin);
        okBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (listener != null)
                    listener.accept(true);
                dismiss();
            }
        });
        getTable().add(okBtn)
                .padTop(padding)
                .padBottom(paddingLarge)
                .padLeft(padding)
                .padRight(paddingLarge)
                .center();

        resizeToFitTable();
    }

    public void setListener(@Nullable Consumer<Boolean> listener) {
        this.listener = listener;
    }

    public void setMessage(String message) {
        label.setText(message);
    }

    @Override
    public void dismiss() {
        super.dismiss();
        listener = null;
    }
}
