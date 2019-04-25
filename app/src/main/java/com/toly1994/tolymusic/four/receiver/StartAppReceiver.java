package com.toly1994.tolymusic.four.receiver;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.toly1994.tolymusic.app.MusicApplication;

public class StartAppReceiver extends BroadcastReceiver {
    public static final String START_APP = "com.toly1994.tolymusic.START_APP";

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent send = new Intent(context, MusicApplication.launchActivity);
        send.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(send);
    }

}
