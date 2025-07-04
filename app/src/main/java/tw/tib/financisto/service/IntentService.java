package tw.tib.financisto.service;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import tw.tib.financisto.R;
import tw.tib.financisto.activity.AccountWidget;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.model.Transaction;
import tw.tib.financisto.model.TransactionInfo;
import tw.tib.financisto.utils.NotificationUtils;

public class IntentService extends Service {
    public static final String ACTION_NEW_TRANSACTION = "tw.tib.financisto.NEW_TRANSACTION";

    private DatabaseAdapter db;
    private IntentTransactionProcessor intentTransactionProcessor;

    @Override
    public void onCreate() {
        super.onCreate();
        db = new DatabaseAdapter(this);
        db.open();
        intentTransactionProcessor = new IntentTransactionProcessor(db);
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ACTION_NEW_TRANSACTION -> createTransactionFromIntent(intent);
                }
            }
        }
        return START_STICKY;
    }

    private void createTransactionFromIntent(Intent intent) {
        Transaction t = intentTransactionProcessor.createTransactionFromIntent(intent);

        if (t != null) {
            TransactionInfo transactionInfo = db.getTransactionInfo(t.id);
            Notification notification = createIntentTransactionNotification(transactionInfo);
            NotificationUtils.notifyUser(this, notification, (int) t.id);
            AccountWidget.updateWidgets(this);
        }
    }

    private Notification createIntentTransactionNotification(TransactionInfo t) {
        String tickerText = getString(R.string.new_intent_transaction_text);
        String contentTitle = getString(R.string.new_intent_transaction_title);
        String text = t.getNotificationContentText(this);

        return NotificationUtils.generateNotification(this, t, tickerText, contentTitle, text);
    }
}
