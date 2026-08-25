package tw.tib.financisto.ai;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import tw.tib.financisto.activity.AbstractTransactionActivity;
import tw.tib.financisto.activity.TransactionActivity;
import tw.tib.financisto.db.DatabaseAdapter;

public class GeminiNanoManager {
    private static final String TAG = "GeminiNanoManager";
    private static volatile GeminiNanoManager instance;

    private GeminiNanoManager() {
    }

    public static GeminiNanoManager getInstance() {
        if (instance == null) {
            synchronized (GeminiNanoManager.class) {
                if (instance == null) {
                    instance = new GeminiNanoManager();
                }
            }
        }
        return instance;
    }

    /**
     * Checks if on-device Gemini Nano / AICore features are supported.
     */
    public boolean isGeminiNanoSupported(Context context) {
        // AICore system service is available starting from Android 14 (API 34)
        return Build.VERSION.SDK_INT >= 34;
    }

    /**
     * Parses natural language transaction input and returns a structured result.
     */
    public ParsedTransactionResult parseNaturalLanguageTransaction(String text, DatabaseAdapter db) {
        return NLTransactionParser.parse(text, db);
    }

    /**
     * Creates an Intent to start TransactionActivity with parsed pre-filled values.
     */
    public Intent createTransactionIntent(Context context, ParsedTransactionResult result) {
        Intent intent = new Intent(context, TransactionActivity.class);
        if (result.matchedAccountId > 0) {
            intent.putExtra(AbstractTransactionActivity.ACCOUNT_ID_EXTRA, result.matchedAccountId);
        }
        if (result.amount > 0) {
            long signedAmount = result.isExpense ? -Math.abs(result.amount) : Math.abs(result.amount);
            intent.putExtra(TransactionActivity.AMOUNT_EXTRA, signedAmount);
        }
        if (result.matchedCategoryId > 0) {
            intent.putExtra(AbstractTransactionActivity.CATEGORY_ID_EXTRA, result.matchedCategoryId);
        }
        if (result.matchedPayeeId > 0) {
            intent.putExtra(AbstractTransactionActivity.PAYEE_ID_EXTRA, result.matchedPayeeId);
        }
        if (result.note != null && !result.note.isEmpty()) {
            intent.putExtra(AbstractTransactionActivity.NOTE_EXTRA, result.note);
        }
        if (result.dateTime > 0) {
            intent.putExtra(AbstractTransactionActivity.DATETIME_EXTRA, result.dateTime);
        }
        return intent;
    }

    /**
     * Generates on-device expense insights and summaries for a specific time range.
     */
    public ExpenseInsightReport generateInsights(DatabaseAdapter db, String periodTitle, long startDate, long endDate) {
        return ExpenseInsightGenerator.generateReport(db, periodTitle, startDate, endDate);
    }
}
