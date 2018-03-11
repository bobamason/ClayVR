package net.masonapps.clayvr.math;

import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;

/**
 * Created by Bob on 7/26/2017.
 */

public class RotationUtil {

    public static void rotateToViewSide(Quaternion rotation, Side side) {
        switch (side) {
            case FRONT:
                rotation.idt();
                break;
            case BACK:
                rotation.set(Vector3.Y, 180);
                break;
            case LEFT:
                rotation.set(Vector3.Y, -90);
                break;
            case RIGHT:
                rotation.set(Vector3.Y, 90);
                break;
            case TOP:
                rotation.set(Vector3.X, 90);
                break;
            case BOTTOM:
                rotation.set(Vector3.X, -90);
                break;
        }
    }
}
