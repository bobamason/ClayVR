package net.masonapps.clayvr.math;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;

/**
 * Created by Bob on 1/5/2018.
 */

public class Animator {

    private float value = 1f;
    private float duration = 0.5f;
    private Animator.AnimationListener listener;
    private Interpolation interpolation = Interpolation.linear;

    public Animator(Animator.AnimationListener listener) {
        this.listener = listener;
    }

    public void update(float dT) {
        if (value < 1f) {
            value += dT / duration;
            listener.apply(interpolation.apply(MathUtils.clamp(value, 0f, 1f)));
            if (value >= 1f) {
                listener.finished();
            }
        }
    }

    public void start() {
        value = 0f;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }

    public void setInterpolation(Interpolation interpolation) {
        this.interpolation = interpolation;
    }

    public interface AnimationListener {
        void apply(float value);

        void finished();
    }
}
