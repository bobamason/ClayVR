package net.masonapps.clayvr.controller;

import com.badlogic.gdx.math.MathUtils;
import com.google.vr.sdk.controller.Controller;

import org.masonapps.libgdxgooglevr.input.DaydreamButtonEvent;
import org.masonapps.libgdxgooglevr.input.DaydreamControllerInputListener;
import org.masonapps.libgdxgooglevr.input.DaydreamTouchEvent;

/**
 * Created by Bob on 8/15/2017.
 */

public abstract class QuadButtonListener implements DaydreamControllerInputListener {

    public static final int NONE = -1;
    public static final int TOP = 0;
    public static final int BOTTOM = 1;
    public static final int LEFT = 2;
    public static final int RIGHT = 3;
    private static final float PI_OVER_4 = MathUtils.PI / 4f;
    private static final float THREE_PI_OVER_4 = 3f * MathUtils.PI / 4f;
    private float deadZone = 0.05f;
    private float deadZoneSq = deadZone * deadZone;
    private int focusedButton = NONE;
    private boolean locked = false;

    @Override
    public void onControllerButtonEvent(Controller controller, DaydreamButtonEvent event) {
        if (event.button == DaydreamButtonEvent.BUTTON_TOUCHPAD) {
            if (event.action == DaydreamButtonEvent.ACTION_DOWN) {
                onButtonDown(focusedButton);
                locked = true;
            }
            if (event.action == DaydreamButtonEvent.ACTION_UP) {
                onButtonUp();
                locked = false;
            }
        }
    }

    @Override
    public void onControllerTouchPadEvent(Controller controller, DaydreamTouchEvent event) {
        if (locked) return;
        final int newButton = getFocusedButton(event.x - 0.5f, event.y - 0.5f);
        switch (event.action) {
            case DaydreamTouchEvent.ACTION_DOWN:
            case DaydreamTouchEvent.ACTION_MOVE:
                if (focusedButton != newButton)
                    onFocusedButtonChanged(newButton);
                this.focusedButton = newButton;
                break;
            case DaydreamTouchEvent.ACTION_UP:
                onFocusedButtonChanged(NONE);
                this.focusedButton = NONE;
                break;
        }
    }

    private int getFocusedButton(float x, float y) {
        if ((x * x + y * y) > deadZoneSq) {
            final float a = MathUtils.atan2(y, x);
            if (a >= -THREE_PI_OVER_4 && a < -PI_OVER_4)
                return TOP;
            if (a >= PI_OVER_4 && a < THREE_PI_OVER_4)
                return BOTTOM;
            if (a >= -PI_OVER_4 && a < PI_OVER_4)
                return RIGHT;
            if (a >= THREE_PI_OVER_4 || a < -THREE_PI_OVER_4)
                return LEFT;
        }
        return NONE;
    }

    public float getDeadZone() {
        return deadZone;
    }

    public void setDeadZone(float deadZone) {
        this.deadZone = deadZone;
        deadZoneSq = deadZone * deadZone;
    }

    public abstract void onFocusedButtonChanged(int focusedButton);

    public abstract void onButtonDown(int focusedButton);

    public abstract void onButtonUp();
}
