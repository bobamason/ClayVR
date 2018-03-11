package net.masonapps.clayvr;

import android.os.Bundle;

import com.badlogic.gdx.graphics.Color;
import com.google.vr.ndk.base.AndroidCompat;
import com.google.vr.ndk.base.GvrLayout;

import org.masonapps.libgdxgooglevr.gfx.VrGame;
import org.masonapps.libgdxgooglevr.gfx.VrWorldScreen;
import org.masonapps.libgdxgooglevr.vr.VrActivity;

/**
 * Created by Bob on 3/9/2018.
 */

public class TestActivity extends VrActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final VrGame game = new VrGame();
        initialize(game);
        final VrWorldScreen screen = new VrWorldScreen(game);
        screen.setBackgroundColor(Color.GOLDENROD);
        game.setScreen(screen);
    }

    @Override
    protected void initGvrLayout(GvrLayout layout) {
        super.initGvrLayout(layout);
        if(layout.enableAsyncReprojectionProtected())
            AndroidCompat.setSustainedPerformanceMode(this, true);
    }
}
