package tw.tib.financisto.worker;

import static android.app.PendingIntent.FLAG_CANCEL_CURRENT;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import tw.tib.financisto.activity.AbstractTransactionActivity;
import tw.tib.financisto.activity.AccountWidget;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.model.TransactionInfo;
import tw.tib.financisto.recur.NotificationOptions;
import tw.tib.financisto.service.RecurrenceScheduler;

public class ScheduleTxWorker extends Worker {
    private static final String TAG = "ScheduleTxWorker";
    public static final String WORK_TAG = "ScheduleTx";
    public static final String WORK_NAME_PREFIX = "ScheduleTx-";
    public static final String TX_ID = "txId";
    public static final String TX_TIME = "txTime";

    public ScheduleTxWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        Data args = getInputData();
        DatabaseAdapter db = new DatabaseAdapter(context);
        db.open();

        RecurrenceScheduler scheduler = new RecurrenceScheduler(db);

        long scheduledTransactionId = args.getLong(TX_ID, -1);
        long scheduledTimestamp = args.getLong(TX_TIME, System.currentTimeMillis());

        Log.i(TAG, "doWork txId=" + scheduledTransactionId + ", ts=" + scheduledTimestamp);

        if (scheduledTransactionId > 0) {
            TransactionInfo transaction = scheduler.scheduleOne(context, scheduledTransactionId, scheduledTimestamp);
            if (transaction != null) {
                notifyUser(transaction);
                AccountWidget.updateWidgets(context);
            }
        }

        return Result.success();
    }

    private void notifyUser(TransactionInfo transaction) {
        Notification notification = createScheduledNotification(transaction);
        notifyUser(notification, (int) transaction.id);
    }

    private void notifyUser(Notification notification, int id) {
        NotificationManager notificationManager =
                (NotificationManager) getApplicationContext()
                        .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, notification);
    }

    private Notification createScheduledNotification(TransactionInfo t) {
        var context = getApplicationContext();
        String tickerText = t.getNotificationTickerText(context);
        String contentTitle = t.getNotificationContentTitle(context);
        String text = t.getNotificationContentText(context);

        return generateNotification(t, tickerText, contentTitle, text);
    }

    private Notification generateNotification(TransactionInfo t, String tickerText, String contentTitle, String text) {
        Context context = getApplicationContext();
        Intent notificationIntent = new Intent(context, t.getActivity());
        notificationIntent.putExtra(AbstractTransactionActivity.TRAN_ID_EXTRA, t.id);
        PendingIntent contentIntent = PendingIntent.getActivity(context, (int) t.id, notificationIntent, FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE); /* https://stackoverflow.com/a/3730394/365675 */

        Notification notification = new NotificationCompat.Builder(context, "transactions")
                .setContentIntent(contentIntent)
                .setSmallIcon(t.getNotificationIcon())
                .setWhen(System.currentTimeMillis())
                .setTicker(tickerText)
                .setContentText(text)
                .setContentTitle(contentTitle)
                .setAutoCancel(true)
                .build();

        applyNotificationOptions(notification, t.notificationOptions);

        return notification;
    }

    private void applyNotificationOptions(Notification notification, String notificationOptions) {
        if (notificationOptions == null) {
            notification.defaults = Notification.DEFAULT_ALL;
        } else {
            NotificationOptions options = NotificationOptions.parse(notificationOptions);
            options.apply(notification);
        }
    }
}
