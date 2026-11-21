package com.gpldroid.isslampro;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

public class NativeMediaNotificationService extends Service {
    private static final String CHANNEL = "isslampro_media";
    private static final int ID = 7202;
    private MediaSessionCompat session;
    private String title = "إسلام برو";
    private String artist = "القرآن الكريم";
    private boolean playing;

    static void update(Context context, String title, String artist, boolean playing, boolean visible, String url) {
        Intent intent = new Intent(context, NativeMediaNotificationService.class)
            .setAction("UPDATE").putExtra("title", title).putExtra("artist", artist)
            .putExtra("playing", playing).putExtra("visible", visible).putExtra("url", url);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent);
        else context.startService(intent);
    }

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        session = new MediaSessionCompat(this, "IsslamProMedia");
        session.setCallback(new MediaSessionCompat.Callback() {
            @Override public void onPlay() { MainActivity.sendToPage("toggle"); }
            @Override public void onPause() { MainActivity.sendToPage("toggle"); }
            @Override public void onSkipToNext() { MainActivity.sendToPage("next"); }
            @Override public void onSkipToPrevious() { MainActivity.sendToPage("prev"); }
            @Override public void onStop() { MainActivity.sendToPage("stop"); }
            @Override public void onSeekTo(long pos) { }
        });
        session.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
            MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || !"UPDATE".equals(intent.getAction())) { stopSelf(startId); return START_NOT_STICKY; }
        title = intent.getStringExtra("title");
        artist = intent.getStringExtra("artist");
        playing = intent.getBooleanExtra("playing", false);
        boolean visible = intent.getBooleanExtra("visible", false);
        if (!visible) {
            stopForeground(true);
            if (session != null) { session.setActive(false); session.release(); session = null; }
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        if (session != null) {
            session.setMetadata(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title == null ? "إسلام برو" : title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist == null ? "" : artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "إسلام برو").build());
            long actions = PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE |
                PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_STOP;
            session.setPlaybackState(new PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(playing ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED,
                    PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f).build());
            session.setActive(true);
        }
        Notification notification = buildNotification();
        if (playing) {
            if (Build.VERSION.SDK_INT >= 29) startForeground(ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            else startForeground(ID, notification);
        } else {
            stopForeground(false);
            getSystemService(NotificationManager.class).notify(ID, notification);
        }
        return START_NOT_STICKY;
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title == null ? "إسلام برو" : title)
            .setContentText(artist == null ? "القرآن الكريم" : artist)
            .setOnlyAlertOnce(true).setOngoing(playing)
            .addAction(android.R.drawable.ic_media_previous, "السابق", command("com.gpldroid.isslampro.PREV", 1))
            .addAction(playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                playing ? "إيقاف مؤقت" : "تشغيل", command("com.gpldroid.isslampro.PLAY_PAUSE", 2))
            .addAction(android.R.drawable.ic_media_next, "التالي", command("com.gpldroid.isslampro.NEXT", 3))
            .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(session.getSessionToken()).setShowActionsInCompactView(0, 1, 2))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build();
    }

    private PendingIntent command(String action, int request) {
        Intent i = new Intent(this, MediaCommandReceiver.class).setAction(action);
        return PendingIntent.getBroadcast(this, request, i,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL, "مشغل إسلام برو",
                NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("إشعار التحكم بتلاوات القرآن الكريم");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @Override public void onDestroy() {
        stopForeground(true);
        if (session != null) { session.setActive(false); session.release(); session = null; }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
