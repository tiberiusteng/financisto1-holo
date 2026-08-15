package tw.tib.financisto.service;

import android.app.Notification;
import android.content.Intent;

import tw.tib.financisto.R;
import tw.tib.financisto.activity.AccountWidget;
import tw.tib.financisto.backup.DatabaseExport;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.model.Transaction;
import tw.tib.financisto.model.TransactionInfo;
import tw.tib.financisto.utils.NotificationUtils;

public class IntentService extends android.app.IntentService {
    public static final String ACTION_NEW_TRANSACTION = "tw.tib.financisto.NEW_TRANSACTION";
    public static final String ACTION_CREATE_BACKUP = "tw.tib.financisto.CREATE_BACKUP";

    private DatabaseAdapter db;
    private IntentTransactionProcessor intentTransactionProcessor;

    public IntentService() {
        super("IntentService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        db = new DatabaseAdapter(this);
        db.open();
        intentTransactionProcessor = new IntentTransactionProcessor(db);
    }

    @Override
    public void onHandleIntent(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ACTION_NEW_TRANSACTION -> createTransactionFromIntent(intent);
                    case ACTION_CREATE_BACKUP -> createBackup();
                }
            }
        }
    }

    private void createBackup() {
        try {
            new DatabaseExport(this, db.db(), true).export();
        } catch (Exception ignore) {
        }
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
