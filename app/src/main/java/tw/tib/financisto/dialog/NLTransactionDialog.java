package tw.tib.financisto.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import tw.tib.financisto.R;
import tw.tib.financisto.ai.GeminiNanoManager;
import tw.tib.financisto.ai.NLTransactionParser;
import tw.tib.financisto.ai.ParsedTransactionResult;
import tw.tib.financisto.db.DatabaseAdapter;

public class NLTransactionDialog {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());

    public static void show(Context context, DatabaseAdapter db) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.ai_nl_dialog_title);
        builder.setIcon(R.drawable.ic_ai_sparkle);

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_nl_transaction, null);

        EditText etInput = view.findViewById(R.id.et_nl_input);
        TextView tvPreview = view.findViewById(R.id.tv_preview_details);

        Button btnSample1 = view.findViewById(R.id.btn_sample_1);
        Button btnSample2 = view.findViewById(R.id.btn_sample_2);
        Button btnSample3 = view.findViewById(R.id.btn_sample_3);
        Button btnSample4 = view.findViewById(R.id.btn_sample_4);

        final ParsedTransactionResult[] currentResult = new ParsedTransactionResult[]{new ParsedTransactionResult()};

        Runnable updatePreview = () -> {
            String text = etInput.getText().toString();
            if (text.trim().isEmpty()) {
                tvPreview.setText(R.string.ai_nl_preview_waiting);
                currentResult[0] = new ParsedTransactionResult();
                return;
            }

            ParsedTransactionResult r = NLTransactionParser.parse(text, db);
            currentResult[0] = r;

            StringBuilder sb = new StringBuilder();
            sb.append("• ").append(context.getString(R.string.ai_nl_amount_label))
              .append(r.isExpense ? context.getString(R.string.ai_expense) : context.getString(R.string.ai_income))
              .append(" ");
            if (r.amount > 0) {
                sb.append(String.format(Locale.getDefault(), "$%,.2f\n", r.amount / 100.0));
            } else {
                sb.append(context.getString(R.string.ai_nl_unrecognized)).append("\n");
            }

            sb.append("• ").append(context.getString(R.string.ai_nl_account_label))
              .append(!r.accountName.isEmpty() ? r.accountName : context.getString(R.string.ai_nl_default_account)).append("\n");
            sb.append("• ").append(context.getString(R.string.ai_nl_category_label))
              .append(!r.categoryName.isEmpty() ? r.categoryName : context.getString(R.string.ai_nl_uncategorized)).append("\n");
            if (!r.payeeName.isEmpty()) {
                sb.append("• ").append(context.getString(R.string.ai_nl_payee_label)).append(r.payeeName).append("\n");
            }
            if (!r.note.isEmpty()) {
                sb.append("• ").append(context.getString(R.string.ai_nl_note_label)).append(r.note).append("\n");
            }
            sb.append("• ").append(context.getString(R.string.ai_nl_time_label)).append(DATE_FORMAT.format(new Date(r.dateTime)));

            tvPreview.setText(sb.toString());
        };

        etInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePreview.run();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        View.OnClickListener sampleClickListener = v -> {
            Button b = (Button) v;
            etInput.setText(b.getText());
            etInput.setSelection(etInput.getText().length());
        };

        btnSample1.setOnClickListener(sampleClickListener);
        btnSample2.setOnClickListener(sampleClickListener);
        btnSample3.setOnClickListener(sampleClickListener);
        btnSample4.setOnClickListener(sampleClickListener);

        builder.setView(view);
        builder.setPositiveButton(R.string.ai_nl_confirm_button, (dialog, which) -> {
            ParsedTransactionResult r = currentResult[0];
            if (r == null || (r.amount == 0 && r.note.isEmpty())) {
                Toast.makeText(context, R.string.ai_nl_input_empty_warning, Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = GeminiNanoManager.getInstance().createTransactionIntent(context, r);
            context.startActivity(intent);
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}

