package tw.tib.financisto.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import tw.tib.financisto.ai.GeminiNanoManager;
import tw.tib.financisto.ai.ParsedTransactionResult;
import tw.tib.financisto.db.DatabaseAdapter;

/**
 * Handles incoming intents from:
 * 1. System text share (ACTION_SEND) from Gemini App, Chrome, etc.
 * 2. Deep links (ACTION_VIEW) e.g., financisto://quick-add?q=...
 * 3. Google Assistant / Gemini App Actions
 */
public class QuickAddActionActivity extends Activity {
    private DatabaseAdapter db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = new DatabaseAdapter(this);
        db.open();

        try {
            handleIntent(getIntent());
        } catch (Exception e) {
            Toast.makeText(this, "AI 記帳解析錯誤: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null) {
            db.close();
        }
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            finish();
            return;
        }

        String action = intent.getAction();
        String textToParse = null;

        // 1. Handle ACTION_SEND (Share from Gemini App or any app)
        if (Intent.ACTION_SEND.equals(action) && "text/plain".equals(intent.getType())) {
            textToParse = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (textToParse == null) {
                CharSequence textSeq = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
                if (textSeq != null) {
                    textToParse = textSeq.toString();
                }
            }
        }
        // 2. Handle ACTION_VIEW (Deep Link: financisto://quick-add?q=... or ?text=...)
        else if (Intent.ACTION_VIEW.equals(action)) {
            Uri data = intent.getData();
            if (data != null) {
                if (data.getQueryParameter("q") != null) {
                    textToParse = data.getQueryParameter("q");
                } else if (data.getQueryParameter("text") != null) {
                    textToParse = data.getQueryParameter("text");
                } else if (data.getQueryParameter("note") != null) {
                    textToParse = data.getQueryParameter("note");
                }
            }
        }

        // 3. Parse and forward to TransactionActivity
        if (textToParse != null && !textToParse.trim().isEmpty()) {
            ParsedTransactionResult result = GeminiNanoManager.getInstance().parseNaturalLanguageTransaction(textToParse, db);
            Intent txIntent = GeminiNanoManager.getInstance().createTransactionIntent(this, result);
            txIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(txIntent);
            Toast.makeText(this, "✨ Gemini Nano 已自動解析並預填欄位", Toast.LENGTH_SHORT).show();
        } else {
            // If no text, open standard natural language dialog or transaction activity
            Intent mainIntent = new Intent(this, MainActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(mainIntent);
        }

        finish();
    }
}
