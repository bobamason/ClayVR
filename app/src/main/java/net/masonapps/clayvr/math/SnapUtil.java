package net.masonapps.clayvr.math;

import com.badlogic.gdx.math.Vector3;

/**
 * Created by Bob Mason on 1/17/2018.
 */

public class SnapUtil {

    public static float snap(float value, float stepSize) {
        return Math.round(value / stepSize) * stepSize;
    }

    public static Vector3 snap(Vector3 v, float stepSize) {
        v.x = snap(v.x, stepSize);
        v.y = snap(v.y, stepSize);
        v.z = snap(v.z, stepSize);
        return v;
    }
}
