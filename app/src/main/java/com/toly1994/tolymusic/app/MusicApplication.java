package com.toly1994.tolymusic.app;

import android.app.Application;
import com.toly1994.tolymusic.four.activity.HomeActivity;

/**
 * launcher
 */
public class MusicApplication extends Application {
    public static Class<?> launchActivity = HomeActivity.class;
}
