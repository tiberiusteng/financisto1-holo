package tw.tib.financisto.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.Locale;

import tw.tib.financisto.R;
import tw.tib.financisto.ai.ExpenseInsightGenerator;
import tw.tib.financisto.ai.ExpenseInsightReport;
import tw.tib.financisto.db.DatabaseAdapter;

public class AIExpenseInsightsActivity extends AppCompatActivity {
    private DatabaseAdapter db;

    private TextView tvPeriodLabel;
    private TextView tvTotalExpense;
    private TextView tvTotalIncome;
    private TextView tvNetBalance;
    private TextView tvAiSummary;
    private TextView tvAiTips;
    private LinearLayout llCategoriesContainer;

    private Button btnWeek;
    private Button btnMonth;
    private Button btnLast30;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_expense_insights);

        db = new DatabaseAdapter(this);
        db.open();

        tvPeriodLabel = findViewById(R.id.tv_period_label);
        tvTotalExpense = findViewById(R.id.tv_total_expense);
        tvTotalIncome = findViewById(R.id.tv_total_income);
        tvNetBalance = findViewById(R.id.tv_net_balance);
        tvAiSummary = findViewById(R.id.tv_ai_summary);
        tvAiTips = findViewById(R.id.tv_ai_tips);
        llCategoriesContainer = findViewById(R.id.ll_categories_container);

        btnWeek = findViewById(R.id.btn_period_week);
        btnMonth = findViewById(R.id.btn_period_month);
        btnLast30 = findViewById(R.id.btn_period_last30);

        btnWeek.setOnClickListener(v -> loadThisWeek());
        btnMonth.setOnClickListener(v -> loadThisMonth());
        btnLast30.setOnClickListener(v -> loadLast30Days());

        // Default to this month
        loadThisMonth();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null) {
            db.close();
        }
    }

    private void loadThisWeek() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        long end = cal.getTimeInMillis();

        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();

        loadReport("本週", start, end);
    }

    private void loadThisMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        long end = cal.getTimeInMillis();

        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();

        loadReport("本月", start, end);
    }

    private void loadLast30Days() {
        long end = System.currentTimeMillis();
        long start = end - (30L * 24L * 60L * 60L * 1000L);
        loadReport("近 30 天", start, end);
    }

    private void loadReport(String periodTitle, long start, long end) {
        ExpenseInsightReport report = ExpenseInsightGenerator.generateReport(db, periodTitle, start, end);

        tvPeriodLabel.setText(String.format(Locale.TAIWAN, "%s收支概況", periodTitle));
        tvTotalExpense.setText(String.format(Locale.TAIWAN, "$%,.0f", report.totalExpense / 100.0));
        tvTotalIncome.setText(String.format(Locale.TAIWAN, "$%,.0f", report.totalIncome / 100.0));

        double net = report.netBalance / 100.0;
        tvNetBalance.setText(String.format(Locale.TAIWAN, "%s$%,.0f", (net >= 0 ? "+" : ""), net));

        tvAiSummary.setText(report.summaryText);
        tvAiTips.setText(report.tipsText);

        // Populate categories
        llCategoriesContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        if (report.topCategories.isEmpty()) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText("此期間尚無支出紀錄");
            emptyTv.setTextColor(getResources().getColor(android.R.color.secondary_text_dark));
            llCategoriesContainer.addView(emptyTv);
        } else {
            for (ExpenseInsightReport.CategoryStat stat : report.topCategories) {
                View row = inflater.inflate(R.layout.item_category_insight, llCategoriesContainer, false);
                TextView tvName = row.findViewById(R.id.tv_category_name);
                TextView tvAmount = row.findViewById(R.id.tv_category_amount);
                ProgressBar pb = row.findViewById(R.id.pb_category_progress);

                tvName.setText(stat.categoryName);
                tvAmount.setText(String.format(Locale.TAIWAN, "$%,.0f (%.1f%%)", stat.amount / 100.0, stat.percentage));
                pb.setProgress((int) Math.round(stat.percentage));

                llCategoriesContainer.addView(row);
            }
        }
    }
}
