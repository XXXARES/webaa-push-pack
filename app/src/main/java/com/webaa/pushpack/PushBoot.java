package com.webaa.pushpack;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import com.google.firebase.messaging.FirebaseMessaging;

/** Uygulama sureci acilirken calisir: bildirim kanalini kurar ve "all" konusuna abone eder. */
public class PushBoot extends ContentProvider {
    static final String CHANNEL = "webaa_default";

    @Override
    public boolean onCreate() {
        final Context c = getContext();
        if (c == null) return true;
        channel(c);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    FirebaseMessaging.getInstance().subscribeToTopic("all");
                } catch (Throwable ignored) {
                    // google-services degerleri yoksa Firebase baslatilmamistir
                }
            }
        });
        return true;
    }

    static void channel(Context c) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Bildirimler", NotificationManager.IMPORTANCE_DEFAULT));
    }

    static void show(Context c, String title, String body, String url) {
        channel(c);
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        Intent open = c.getPackageManager().getLaunchIntentForPackage(c.getPackageName());
        if (open == null) open = new Intent();
        if (url != null) open.putExtra("url", url);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        int id = (int) (System.currentTimeMillis() % 100000);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, CHANNEL) : new Notification.Builder(c);
        b.setSmallIcon(c.getApplicationInfo().icon)
                .setContentTitle(title != null ? title : "")
                .setContentText(body != null ? body : "")
                .setStyle(new Notification.BigTextStyle().bigText(body != null ? body : ""))
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(c, id, open, flags));
        nm.notify(id, b.build());
    }

    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) { return null; }
    @Override public String getType(Uri u) { return null; }
    @Override public Uri insert(Uri u, ContentValues v) { return null; }
    @Override public int delete(Uri u, String s, String[] a) { return 0; }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { return 0; }
}
