package com.gpldroid.isslampro;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.Settings;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;

final class NativeBridge {
    private NativeBridge() {}

    static void handle(Context context, String raw) {
        try {
            JSONObject message = new JSONObject(raw);
            String method = message.optString("method");
            JSONArray args = message.optJSONArray("args");
            if (args == null) args = new JSONArray();
            switch (method) {
                case "playbackState": {
                    String title = args.optString(0, "");
                    String artist = args.optString(1, "");
                    boolean playing = args.optBoolean(2, false);
                    boolean visible = args.optBoolean(3, false);
                    String url = args.optString(4, "");
                    NativeMediaNotificationService.update(context, title, artist, playing, visible, url);
                    break;
                }
                case "schedulePrayers":
                    if (args.length() > 0) PrayerScheduler.schedule(context, args.optString(0));
                    break;
                case "clearPrayers":
                    PrayerScheduler.clear(context);
                    break;
                case "testAdhan":
                    PrayerScheduler.test(context, args.optString(0, "تجربة الأذان"),
                        args.optString(1, ""), args.optInt(2, 8));
                    break;
                case "openBatterySettings":
                    openBatterySettings(context);
                    break;
                case "openNotificationSettings":
                    openNotificationSettings(context);
                    break;
            }
        } catch (Exception ignored) { }
    }

    private static void openBatterySettings(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    private static void openNotificationSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
