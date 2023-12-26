package tw.tib.financisto.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import tw.tib.financisto.R;

public class NotificationChannelService {
    private static boolean initialized = false;

    public static final String TRANSACTIONS_CHANNEL = "transactions";

    private NotificationChannelService(Context context) {
    }

    public static void initialize(Context context) {
        if (initialized) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Context c = context.getApplicationContext();

            CharSequence name = c.getString(R.string.notification_channel_name);
            NotificationChannel channel = new NotificationChannel(TRANSACTIONS_CHANNEL, name, NotificationManager.IMPORTANCE_DEFAULT);

            NotificationManager notificationManager = c.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
