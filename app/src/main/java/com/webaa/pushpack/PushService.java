package com.webaa.pushpack;

import android.content.Intent;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Map;

public class PushService extends FirebaseMessagingService {
    @Override
    public void onNewToken(String token) {
        getSharedPreferences("webaa_push", MODE_PRIVATE).edit().putString("token", token).apply();
        sendBroadcast(new Intent("webaa.push.TOKEN").setPackage(getPackageName()).putExtra("token", token));
    }

    @Override
    public void onMessageReceived(RemoteMessage m) {
        String title = null, body = null;
        if (m.getNotification() != null) {
            title = m.getNotification().getTitle();
            body = m.getNotification().getBody();
        }
        Map<String, String> d = m.getData();
        if (title == null) title = d.get("title");
        if (body == null) body = d.get("body");
        PushBoot.show(this, title, body, d.get("url"));
    }
}
