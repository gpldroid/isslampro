package com.gpldroid.isslampro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MediaCommandReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String action = intent == null ? "" : intent.getAction();
        if ("com.gpldroid.isslampro.PLAY_PAUSE".equals(action)) MainActivity.sendToPage("toggle");
        else if ("com.gpldroid.isslampro.NEXT".equals(action)) MainActivity.sendToPage("next");
        else if ("com.gpldroid.isslampro.PREV".equals(action)) MainActivity.sendToPage("prev");
        else if ("com.gpldroid.isslampro.STOP".equals(action)) MainActivity.sendToPage("stop");
    }
}
