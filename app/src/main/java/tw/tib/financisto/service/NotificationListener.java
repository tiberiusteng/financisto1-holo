package tw.tib.financisto.service;

import static tw.tib.financisto.service.FinancistoService.ACTION_NEW_TRANSACTION_SMS;
import static tw.tib.financisto.service.FinancistoService.ACTION_NEW_TRANSACTION_WALLET;
import static tw.tib.financisto.service.FinancistoService.SMS_TRANSACTION_BODY;
import static tw.tib.financisto.service.FinancistoService.SMS_TRANSACTION_NUMBER;
import static tw.tib.financisto.service.FinancistoService.WALLET_TRANSACTION_TEXT;
import static tw.tib.financisto.service.FinancistoService.WALLET_TRANSACTION_TITLE;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.SpannableString;
import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.model.SmsTemplate;
import tw.tib.financisto.utils.MyPreferences;

public class NotificationListener extends NotificationListenerService {
    private static final String TAG = "NotificationListener";

    private static final Set<String> GOOGLE_WALLET_PACKAGES = new HashSet<>(Arrays.asList(
            // Google Wallet
            "com.google.android.apps.walletnfcrel",
            // Google Pay (India)
            "com.google.android.apps.nbu.paisa.user"));

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
                if (GOOGLE_WALLET_PACKAGES.contains(packageName)
                        && MyPreferences.isGoogleWalletTransactionEnabled())
                {
                    Intent serviceIntent = new Intent(ACTION_NEW_TRANSACTION_WALLET, null, context, FinancistoService.class);
                    serviceIntent.putExtra(WALLET_TRANSACTION_TITLE, title);
                    serviceIntent.putExtra(WALLET_TRANSACTION_TEXT, notification.text);
                    FinancistoService.enqueueWork(context, serviceIntent);
                    return;
                }

                final DatabaseAdapter db = new DatabaseAdapter(context);
                List<SmsTemplate> templates = db.getSmsTemplatesByNumber(title);

                if (!templates.isEmpty()) {
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
            result.text = sb.toString();
            result.body = result.title + " " + sb;
        }
        return result;
    }

    public static class ParsedNotification {
        public String key;
        public String title;
        public String text;
        public String body;
    }

    private static String getString(Object s) {
        if (s instanceof SpannableString) {
            return ((SpannableString) s).subSequence(0, ((SpannableString) s).length()).toString();
        }
        if (s == null) {
            return "";
        }
        return (String) s;
    }
}
