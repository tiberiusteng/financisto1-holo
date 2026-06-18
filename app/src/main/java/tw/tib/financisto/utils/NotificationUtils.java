package tw.tib.financisto.utils;

import static android.app.PendingIntent.FLAG_CANCEL_CURRENT;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;

import tw.tib.financisto.R;
import tw.tib.financisto.activity.AbstractTransactionActivity;
import tw.tib.financisto.activity.MainActivity;
import tw.tib.financisto.model.TransactionInfo;
import tw.tib.financisto.recur.NotificationOptions;
import tw.tib.financisto.service.NotificationChannelService;

public class NotificationUtils {
    public static Notification generateNotification(Context context, TransactionInfo t, String tickerText, String contentTitle, String text) {
        var builder = new NotificationCompat.Builder(context, NotificationChannelService.TRANSACTIONS_CHANNEL)
                .setContentIntent(getTransactionPendingIntent(context, t))
                .setSmallIcon(R.mipmap.a_icon_notify)
                .setWhen(System.currentTimeMillis())
                .setTicker(tickerText)
                .setContentText(text)
                .setContentTitle(contentTitle)
                .setAutoCancel(true);

        applyNotificationOptions(builder, t.notificationOptions);

        return builder.build();
    }

    @Nullable
    private static PendingIntent getTransactionPendingIntent(Context context, TransactionInfo t) {
        Intent mainScreenIntent = new Intent(context, MainActivity.class);
        mainScreenIntent.putExtra(MainActivity.GO_TO_SCREEN, MyPreferences.StartupScreen.BLOTTER.ordinal());

        Intent transactionIntent = new Intent(context, t.getActivity());
        transactionIntent.putExtra(AbstractTransactionActivity.TRAN_ID_EXTRA, t.id);

        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context);
        taskStackBuilder.addNextIntent(mainScreenIntent);
        taskStackBuilder.addNextIntent(transactionIntent);

        /* https://stackoverflow.com/a/3730394/365675 */
        return taskStackBuilder.getPendingIntent((int) t.id,
                FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void applyNotificationOptions(NotificationCompat.Builder builder, String notificationOptions) {
        if (notificationOptions != null) {
            NotificationOptions options = NotificationOptions.parse(notificationOptions);
            options.apply(builder);
        }
    }

    public static void notifyUser(Context context, Notification notification, int id) {
        NotificationChannelService.initialize(context);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationManager.notify(id, notification);
    }
}
