package tw.tib.financisto.service;

import static tw.tib.financisto.service.FinancistoService.ACTION_NEW_TRANSACTION_SMS;
import static tw.tib.financisto.service.FinancistoService.SMS_TRANSACTION_BODY;
import static tw.tib.financisto.service.FinancistoService.SMS_TRANSACTION_NUMBER;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.SpannableString;
import android.util.Log;

import java.util.Set;

import tw.tib.financisto.db.DatabaseAdapter;

public class NotificationListener extends NotificationListenerService {
    private static final String TAG = "NotificationListener";
    private String packageName;
    private NotificationCache notificationCache;

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.d(TAG, "onListenerConnected");

        packageName = getApplicationContext().getPackageName();
        notificationCache = NotificationCache.getInstance();

        for (StatusBarNotification sbn : getActiveNotifications()) {
            processNotification(sbn, false);
        }
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        Log.d(TAG, "onListenerDisconnected");
        notificationCache.cache.clear();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        Log.d(TAG, "onNotificationPosted");
        processNotification(sbn, true);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        String key = sbn.getKey();
        Log.d(TAG, "onNotificationRemoved key=" + key);
        notificationCache.cache.remove(key);
    }

    private void processNotification(StatusBarNotification sbn, boolean processTemplate) {
        String packageName = sbn.getPackageName();
        // don't try to process our own notification to enter an infinite recursion
        if (packageName.equals(this.packageName)) {
            return;
        }
        Log.d(TAG, "package=" + packageName);
        Log.d(TAG, "key=" + sbn.getKey());

        ParsedNotification notification = extractNotification(sbn);

        if (notification != null) {
            ParsedNotification existing = null;
            if (notification.title != null && notification.body != null) {
                existing = notificationCache.cache.put(notification.key, notification);
            }

            Context context = getApplicationContext();
            String title = notification.title;
            String body = notification.body;

            Log.d(TAG, "title=\"" + title + "\", body=\"" + body + "\"");
            Log.d(TAG, sbn.getNotification().extras.toString());

            if (processTemplate && (existing == null || !body.equals(existing.body))) {
                final DatabaseAdapter db = new DatabaseAdapter(context);
                Set<String> smsNumbers = db.findAllSmsTemplateNumbers();

                if (smsNumbers.contains(title)) {
                    Intent serviceIntent = new Intent(ACTION_NEW_TRANSACTION_SMS, null, context, FinancistoService.class);
                    serviceIntent.putExtra(SMS_TRANSACTION_NUMBER, title);
                    serviceIntent.putExtra(SMS_TRANSACTION_BODY, body);
                    FinancistoService.enqueueWork(context, serviceIntent);
                }
            }
        }
    }

    public static ParsedNotification extractNotification(StatusBarNotification sbn) {
        ParsedNotification result = null;
        Bundle extras = sbn.getNotification().extras;
        if (extras != null) {
            StringBuilder sb = new StringBuilder();
            result = new ParsedNotification();
            result.key = sbn.getKey();
            result.title = getString(extras.getCharSequence(Notification.EXTRA_TITLE));
            String text = getString(extras.getCharSequence(Notification.EXTRA_TEXT));
            if (text != null) {
                sb.append(text);
            }
            String bigText = getString(extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
            if (bigText != null && !bigText.equals(text)) {
                if (text != null) {
                    sb.append(" ");
                }
                sb.append(bigText);
            }
            result.body = sb.toString();
        }
        return result;
    }

    public static class ParsedNotification {
        public String key;
        public String title;
        public String body;
    }

    private static String getString(Object s) {
        if (s instanceof SpannableString) {
            return ((SpannableString) s).subSequence(0, ((SpannableString) s).length()).toString();
        }
        return (String) s;
    }
}
