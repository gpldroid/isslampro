package com.gpldroid.isslampro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class PrayerAlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context, AdhanPlaybackService.class)
            .putExtra("title", intent.getStringExtra("title"))
            .putExtra("url", intent.getStringExtra("url"))
            .putExtra("test", intent.getBooleanExtra("test", false));
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(service);
        else context.startService(service);
    }
}
