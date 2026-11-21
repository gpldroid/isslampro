package com.gpldroid.isslampro;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

public class AdhanPlaybackService extends Service {
    private static final String CHANNEL = "isslampro_adhan";
    private static final int NOTIFICATION_ID = 7201;
    private MediaPlayer player;

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String title = intent == null ? "حان وقت الصلاة" : intent.getStringExtra("title");
        String url = intent == null ? null : intent.getStringExtra("url");
        startForeground(NOTIFICATION_ID, notification(title));
        if (player != null) {
            try { player.stop(); player.release(); } catch (Exception ignored) { }
            player = null;
        }
        if (url == null || url.isEmpty()) { stopSelf(startId); return START_NOT_STICKY; }
        try {
            player = new MediaPlayer();
            player.setWakeMode(this, android.os.PowerManager.PARTIAL_WAKE_LOCK);
            player.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            player.setOnPreparedListener(MediaPlayer::start);
            player.setOnCompletionListener(mp -> stopSelf());
            player.setOnErrorListener((mp, what, extra) -> { stopSelf(); return true; });
            player.setDataSource(url);
            player.prepareAsync();
        } catch (Exception e) { stopSelf(startId); }
        return START_NOT_STICKY;
    }

    private Notification notification(String title) {
        return new NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title == null ? "إسلام برو" : title)
            .setContentText("إسلام برو · مواقيت الصلاة")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true).setAutoCancel(false).build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL, "الأذان",
                NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("تنبيهات وتشغيل الأذان عند دخول وقت الصلاة");
            channel.setSound(null, null);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @Override public void onDestroy() {
        if (player != null) { try { player.stop(); player.release(); } catch (Exception ignored) { } player = null; }
        stopForeground(true);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
