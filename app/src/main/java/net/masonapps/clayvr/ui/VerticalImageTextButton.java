package net.masonapps.clayvr.ui;

import com.badlogic.gdx.scenes.scene2d.ui.ImageTextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

/**
 * Created by Bob Mason on 10/12/2017.
 */

public class VerticalImageTextButton extends ImageTextButton {
    public VerticalImageTextButton(String text, Skin skin) {
        super(text, skin);
        fixTable();
    }

    public VerticalImageTextButton(String text, Skin skin, String styleName) {
        super(text, skin, styleName);
        fixTable();
    }

    public VerticalImageTextButton(String text, ImageTextButtonStyle style) {
        super(text, style);
        fixTable();
    }

    private void fixTable() {
        clearChildren();
        add(getImage()).row();
        add(getLabel());
        setStyle(getStyle());
        setSize(getPrefWidth(), getPrefHeight());
    }
}
