package net.masonapps.clayvr.controller;

import android.support.annotation.Nullable;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.google.vr.sdk.controller.Controller;

import net.masonapps.clayvr.Style;

import org.masonapps.libgdxgooglevr.GdxVr;
import org.masonapps.libgdxgooglevr.ui.VirtualStage;

import java.util.Arrays;
import java.util.Objects;

/**
 * Created by Bob on 8/15/2017.
 */

public class SculptControlsVirtualStage extends VirtualStage {

    public static final float IMAGE_OFFSET = 1.4f;
    private final float midRadius;
    private final SculptButtonListener sculptButtonListener;
    private final Image buttonDown;
    private final Image[] images;
    private final Image[] backgroundImages;
    private final float[] angles = new float[]{0f, 180f, 90f, 270f};
    private final String[] labelStrings;
    private float focusedScale = 1.4f;
    private float actionDuration = 0.25f;
    @Nullable
    private SculptControlsListener listener = null;
    private Vector3 offset = new Vector3(0f, 0.010f, -0.034f);
    private Interpolation interpolation;
    private Vector3 tmp = new Vector3();
    private Vector2 tmpV2 = new Vector2();
    private boolean isTouchDown = false;
    private Label label;

    public SculptControlsVirtualStage(Batch batch, Skin skin, float diameter, Drawable topDrawable, String topLabel, Drawable bottomDrawable, String bottomLabel, Drawable leftDrawable, String leftLabel, Drawable rightDrawable, String rightLabel) {
        super(batch, diameter, diameter, null);
        setActivationEnabled(false);
        setTouchable(false);
        sculptButtonListener = new SculptButtonListener();
        midRadius = getWidth() / 4f;
        interpolation = Interpolation.swing;

        label = new Label("", skin, Style.DEFAULT_FONT, Style.COLOR_ACCENT);
        label.setFontScale(0.5f);
        labelStrings = new String[]{topLabel, bottomLabel, leftLabel, rightLabel};

        images = new Image[4];
        backgroundImages = new Image[4];

        buttonDown = createBackgroundImage(skin.newDrawable(Style.Drawables.touch_pad_button_down, Style.COLOR_PRIMARY_LIGHT), 0f);
        buttonDown.setScale(focusedScale);
        buttonDown.setVisible(false);
        addActor(buttonDown);

        addImage(topDrawable, QuadButtonListener.TOP);

        addImage(bottomDrawable, QuadButtonListener.BOTTOM);

        addImage(leftDrawable, QuadButtonListener.LEFT);

        addImage(rightDrawable, QuadButtonListener.RIGHT);

        final Drawable backgroundDrawable = skin.newDrawable(Style.Drawables.touch_pad_background, Style.COLOR_PRIMARY);
        addBackgroundImage(backgroundDrawable, QuadButtonListener.TOP);

        addBackgroundImage(backgroundDrawable, QuadButtonListener.BOTTOM);

        addBackgroundImage(backgroundDrawable, QuadButtonListener.LEFT);

        addBackgroundImage(backgroundDrawable, QuadButtonListener.RIGHT);

        Arrays.stream(backgroundImages)
                .filter(Objects::nonNull)
                .forEach(this::addActor);
        Arrays.stream(images)
                .filter(Objects::nonNull)
                .forEach(this::addActor);

        addActor(label);
    }

    private void addBackgroundImage(Drawable backgroundDrawable, int i) {
        if (images[i] != null) {
            backgroundImages[i] = createBackgroundImage(backgroundDrawable, angles[i]);
        }
    }

    private void addImage(Drawable drawable, int i) {
        final Image image = createImage(drawable, angles[i]);
        images[i] = image;
    }

    @Nullable
    private Image createImage(Drawable drawable, float angle) {
        if (drawable == null) return null;
        final float imageWidth = getWidth() / 6;
        final float imageHeight = getHeight() / 6;
        final Image image = new Image(drawable);
        image.setAlign(Align.center);
        image.setSize(imageWidth, imageHeight);
//        image.setOrigin(imageWidth / 2f, imageHeight / 2f);
        tmpV2.set(0, midRadius * IMAGE_OFFSET).rotate(angle);
        image.setPosition(tmpV2.x, tmpV2.y, Align.center);
        return image;
    }

    private Image createBackgroundImage(Drawable drawable, float angle) {
        final Image image = new Image(drawable);
        float w = getWidth();
        float h = w / (image.getWidth() / image.getHeight());
        image.setAlign(Align.center);
        image.setSize(w, h);
        image.setOrigin(w / 2, h / 2);
        image.setRotation(angle);
        tmpV2.set(0, midRadius).rotate(angle);
        image.setPosition(tmpV2.x, tmpV2.y, Align.center);
        return image;
    }

    public void attachListener() {
        GdxVr.input.addDaydreamControllerListener(sculptButtonListener);
    }

    public void detachListener() {
        GdxVr.input.removeDaydreamControllerListener(sculptButtonListener);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (!visible && isTouchDown) {
            buttonDown.setVisible(false);
            Arrays.stream(backgroundImages)
                    .filter(Objects::nonNull)
                    .forEach(image -> image.setVisible(true));
            if (listener != null)
                listener.onButtonUp();
            isTouchDown = false;
        }
    }

    @Override
    public void recalculateTransform() {
        final float hw = getWidth() * pixelSizeWorld * scale.x / 2f;
        final float hh = getHeight() * pixelSizeWorld * scale.y / 2f;
        bounds.set(-hw, -hh, hw, hh);
        transform.idt().translate(position).rotate(rotation).scale(pixelSizeWorld * scale.x, pixelSizeWorld * scale.y, 1f);
        updated = true;
    }

    @Override
    public boolean performRayTest(Ray ray) {
        return false;
    }

    @Override
    public boolean isCursorOver() {
        return false;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        final Quaternion controllerOrientation = GdxVr.input.getControllerOrientation();
        setPosition(GdxVr.input.getControllerPosition());
        translate(tmp.set(offset).mul(controllerOrientation));
        setRotation(controllerOrientation);
        rotateX(-90);
    }

    private void scaleImages(int focusedButton) {
        for (int i = 0; i < backgroundImages.length; i++) {
            final Image backgroundImage = backgroundImages[i];
            if (backgroundImage == null) continue;
//            tmpV2.set(0, midRadius * IMAGE_OFFSET).rotate(angles[i]);
            if (i == focusedButton) {
//                tmpV2.set(0, midRadius * focusedScale).rotate(angles[i]);
//                images[i].addAction(Actions.moveToAligned(tmpV2.x, tmpV2.y, Align.center));
                backgroundImage.addAction(Actions.scaleTo(focusedScale, focusedScale, actionDuration, interpolation));
            } else {
//                images[i].addAction(Actions.moveToAligned(tmpV2.x, tmpV2.y, Align.center));
                backgroundImage.addAction(Actions.scaleTo(1f, 1f, actionDuration, interpolation));
            }
        }
    }

    private void hideImage(int focusedButton) {
        for (int i = 0; i < backgroundImages.length; i++) {
            final Image backgroundImage = backgroundImages[i];
            if (backgroundImage == null) continue;
            if (i == focusedButton)
                backgroundImage.setVisible(false);
            else
                backgroundImage.setVisible(true);
        }
    }

    public void setListener(@Nullable SculptControlsListener listener) {
        this.listener = listener;
    }

    public void setActionDuration(float actionDuration) {
        this.actionDuration = actionDuration;
    }

    public void setFocusedScale(float focusedScale) {
        this.focusedScale = focusedScale;
    }

    @Override
    public void setInterpolation(Interpolation interpolation) {
        this.interpolation = interpolation;
    }

    public interface SculptControlsListener {
        void onButtonDown(int focusedButton);

        void onButtonUp();
    }

    private class SculptButtonListener extends QuadButtonListener {

        @Override
        public void onFocusedButtonChanged(int focusedButton) {
            scaleImages(focusedButton);
            switch (focusedButton) {
                case QuadButtonListener.NONE:
                    label.setText("");
                    label.setVisible(false);
                    break;
                default:
                    label.setVisible(true);
                    final String str = labelStrings[focusedButton];
                    label.setText(str == null ? "" : str);
                    label.setPosition(0, 0, Align.center);
                    break;
            }
        }

        @Override
        public void onButtonDown(int focusedButton) {
            if (!isVisible()) return;
            if (focusedButton != NONE) {
                isTouchDown = true;
                hideImage(focusedButton);
                buttonDown.setRotation(angles[focusedButton]);
                tmpV2.set(0, midRadius).rotate(angles[focusedButton]);
                buttonDown.setPosition(tmpV2.x, tmpV2.y, Align.center);
                buttonDown.setVisible(true);
            }
            if (listener != null)
                listener.onButtonDown(focusedButton);
        }

        @Override
        public void onButtonUp() {
            if (!isVisible()) return;
            isTouchDown = false;
            buttonDown.setVisible(false);
            Arrays.stream(backgroundImages)
                    .filter(Objects::nonNull)
                    .forEach(image -> image.setVisible(true));
            if (listener != null)
                listener.onButtonUp();
        }

        @Override
        public void onDaydreamControllerUpdate(Controller controller, int connectionState) {

        }

        @Override
        public void onControllerConnectionStateChange(int connectionState) {
            setVisible(connectionState == Controller.ConnectionStates.CONNECTED);
        }
    }
}
