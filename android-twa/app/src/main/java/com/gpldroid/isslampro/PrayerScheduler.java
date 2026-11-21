package com.gpldroid.isslampro;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;

final class PrayerScheduler {
    private static final String PREFS = "isslampro_prayer_schedule";
    private static final String KEY = "schedule_json";

    static void schedule(Context context, String json) {
        try {
            JSONObject root = new JSONObject(json);
            Context app = context.getApplicationContext();
            android.content.SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

            // Remove the previous plan before replacing it. Cancelling only the new
            // timestamps leaves stale alarms behind after a city/time/muezzin change.
            String previous = prefs.getString(KEY, null);
            if (previous != null) {
                try { cancelAlarms(app, new JSONObject(previous).optJSONArray("items")); }
                catch (Exception ignored) { }
            }

            JSONArray items = root.optJSONArray("items");
            if (items == null) items = new JSONArray();
            cancelAlarms(app, items);
            prefs.edit().putString(KEY, root.toString()).apply();

            AlarmManager alarms = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
            if (alarms == null) return;
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item == null) continue;
                long at = item.optLong("t", 0L);
                if (at <= System.currentTimeMillis()) continue;
                Intent intent = new Intent(app, PrayerAlarmReceiver.class)
                    .putExtra("title", "أذان " + item.optString("name", item.optString("k", "الصلاة")))
                    .putExtra("url", item.optString("url", ""))
                    .putExtra("city", root.optString("city", ""));
                PendingIntent pi = PendingIntent.getBroadcast(app, requestCode(at, item.optString("k")),
                    intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                if (Build.VERSION.SDK_INT >= 31 && alarms.canScheduleExactAlarms()) {
                    alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                } else {
                    alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                }
            }
        } catch (Exception ignored) { }
    }

    static void test(Context context, String title, String url, int delaySeconds) {
        long at = System.currentTimeMillis() + Math.max(1, delaySeconds) * 1000L;
        Intent intent = new Intent(context, PrayerAlarmReceiver.class)
            .putExtra("title", title).putExtra("url", url).putExtra("test", true);
        PendingIntent pi = PendingIntent.getBroadcast(context, 783410,
            intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;
        if (Build.VERSION.SDK_INT >= 31 && alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        }
    }

    static void clear(Context context) {
        android.content.SharedPreferences prefs = context.getApplicationContext()
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY, null);
        if (json != null) {
            try { cancelAlarms(context, new JSONObject(json).optJSONArray("items")); }
            catch (Exception ignored) { }
        }
        prefs.edit().remove(KEY).apply();
    }

    static void restore(Context context) {
        String json = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null);
        if (json != null) schedule(context, json);
    }

    private static void cancelAlarms(Context context, JSONArray items) {
        if (items == null) return;
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) continue;
            long at = item.optLong("t", 0L);
            Intent intent = new Intent(context, PrayerAlarmReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(context, requestCode(at, item.optString("k")),
                intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pi != null) {
                alarms.cancel(pi);
                pi.cancel();
            }
        }
    }

    private static int requestCode(long at, String prayer) {
        long minute = at / 60000L;
        return (int) (minute ^ (minute >>> 32) ^ prayer.hashCode());
    }
}
