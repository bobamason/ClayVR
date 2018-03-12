package net.masonapps.clayvr.sculpt;

import android.support.annotation.NonNull;

/**
 * Created by Bob Mason on 3/12/2018.
 */

public class SculptAction implements Comparable<SculptAction> {
    private final long timestamp;

    public SculptAction() {
        timestamp = System.nanoTime();
    }

    @Override
    public int compareTo(@NonNull SculptAction o) {
        return Long.compare(this.timestamp, o.timestamp);
    }
}
