package tw.tib.financisto.ai;

import android.database.Cursor;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.db.DatabaseHelper;

public class ExpenseInsightGenerator {
    private static final String TAG = "ExpenseInsightGen";

    public static ExpenseInsightReport generateReport(DatabaseAdapter db, String periodTitle, long startDate, long endDate) {
        ExpenseInsightReport report = new ExpenseInsightReport();
        report.periodTitle = periodTitle;
        report.startDate = startDate;
        report.endDate = endDate;

        if (db == null) {
            return report;
        }

        try {
            // 1. Query current period transactions
            String selection = "datetime >= ? AND datetime <= ? AND is_template = 0 AND parent_id = 0";
            String[] selectionArgs = new String[]{String.valueOf(startDate), String.valueOf(endDate)};

            Map<Long, Long> categoryAmounts = new HashMap<>();
            Map<Long, Long> payeeAmounts = new HashMap<>();
            Map<Long, Integer> payeeCounts = new HashMap<>();

            try (Cursor c = db.db().query(DatabaseHelper.TRANSACTION_TABLE,
                    new String[]{"from_amount", "to_amount", "category_id", "payee_id", "from_account_id", "to_account_id"},
                    selection, selectionArgs, null, null, null)) {

                if (c != null && c.moveToFirst()) {
                    do {
                        long fromAmount = c.getLong(0);
                        long toAccountId = c.getLong(5);
                        long categoryId = c.getLong(2);
                        long payeeId = c.getLong(3);

                        report.transactionCount++;

                        // Non-transfer transactions
                        if (toAccountId <= 0) {
                            if (fromAmount < 0) {
                                long expense = Math.abs(fromAmount);
                                report.totalExpense += expense;

                                if (categoryId > 0) {
                                    categoryAmounts.put(categoryId, categoryAmounts.getOrDefault(categoryId, 0L) + expense);
                                }
                                if (payeeId > 0) {
                                    payeeAmounts.put(payeeId, payeeAmounts.getOrDefault(payeeId, 0L) + expense);
                                    payeeCounts.put(payeeId, payeeCounts.getOrDefault(payeeId, 0) + 1);
                                }
                            } else if (fromAmount > 0) {
                                report.totalIncome += fromAmount;
                            }
                        }
                    } while (c.moveToNext());
                }
            }

            report.netBalance = report.totalIncome - report.totalExpense;

            // 2. Query category titles
            for (Map.Entry<Long, Long> entry : categoryAmounts.entrySet()) {
                String catTitle = "未分類";
                try (Cursor catCursor = db.db().query(DatabaseHelper.CATEGORY_TABLE, new String[]{"title"},
                        "_id = ?", new String[]{String.valueOf(entry.getKey())}, null, null, null)) {
                    if (catCursor != null && catCursor.moveToFirst()) {
                        catTitle = catCursor.getString(0);
                    }
                }
                double pct = report.totalExpense > 0 ? (entry.getValue() * 100.0 / report.totalExpense) : 0.0;
                report.topCategories.add(new ExpenseInsightReport.CategoryStat(catTitle, entry.getValue(), pct));
            }
            // Sort categories descending by amount
            Collections.sort(report.topCategories, (a, b) -> Long.compare(b.amount, a.amount));

            // 3. Query payee titles
            for (Map.Entry<Long, Long> entry : payeeAmounts.entrySet()) {
                String payeeTitle = "未知店家";
                try (Cursor payeeCursor = db.db().query(DatabaseHelper.PAYEE_TABLE, new String[]{"title"},
                        "_id = ?", new String[]{String.valueOf(entry.getKey())}, null, null, null)) {
                    if (payeeCursor != null && payeeCursor.moveToFirst()) {
                        payeeTitle = payeeCursor.getString(0);
                    }
                }
                report.topPayees.add(new ExpenseInsightReport.PayeeStat(payeeTitle, entry.getValue(), payeeCounts.getOrDefault(entry.getKey(), 1)));
            }
            // Sort payees descending by amount
            Collections.sort(report.topPayees, (a, b) -> Long.compare(b.amount, a.amount));

            // 4. Query previous period for comparison
            long duration = endDate - startDate;
            long prevStart = startDate - duration;
            long prevEnd = startDate - 1;
            String[] prevArgs = new String[]{String.valueOf(prevStart), String.valueOf(prevEnd)};

            try (Cursor cPrev = db.db().query(DatabaseHelper.TRANSACTION_TABLE,
                    new String[]{"from_amount", "to_account_id"},
                    selection, prevArgs, null, null, null)) {
                if (cPrev != null && cPrev.moveToFirst()) {
                    do {
                        long fromAmount = cPrev.getLong(0);
                        long toAccountId = cPrev.getLong(1);
                        if (toAccountId <= 0 && fromAmount < 0) {
                            report.previousPeriodExpense += Math.abs(fromAmount);
                        }
                    } while (cPrev.moveToNext());
                }
            }

            if (report.previousPeriodExpense > 0) {
                report.expenseChangePercentage = ((report.totalExpense - report.previousPeriodExpense) * 100.0) / report.previousPeriodExpense;
            }

            // 5. Generate AI insights and tips text
            generateInsightText(report);

        } catch (Exception e) {
            Log.e(TAG, "Error generating expense insight report", e);
        }

        return report;
    }

    private static void generateInsightText(ExpenseInsightReport report) {
        StringBuilder summary = new StringBuilder();
        StringBuilder tips = new StringBuilder();

        double expInDollars = report.totalExpense / 100.0;
        double incInDollars = report.totalIncome / 100.0;
        double netInDollars = report.netBalance / 100.0;

        summary.append(String.format(Locale.TAIWAN, "📌 **%s財務總覽**\n", report.periodTitle));
        summary.append(String.format(Locale.TAIWAN, "• **總支出**：$%,.0f 元 (共 %d 筆交易)\n", expInDollars, report.transactionCount));
        if (report.totalIncome > 0) {
            summary.append(String.format(Locale.TAIWAN, "• **總收入**：$%,.0f 元\n", incInDollars));
            summary.append(String.format(Locale.TAIWAN, "• **本期淨結餘**：%s$%,.0f 元\n", (netInDollars >= 0 ? "+" : ""), netInDollars));
        }

        if (report.previousPeriodExpense > 0) {
            if (report.expenseChangePercentage < 0) {
                summary.append(String.format(Locale.TAIWAN, "• **前期對比**：支出較上一週期 **減少了 %.1f%%** 👏（控制良好！）\n", Math.abs(report.expenseChangePercentage)));
            } else if (report.expenseChangePercentage > 0) {
                summary.append(String.format(Locale.TAIWAN, "• **前期對比**：支出較上一週期 **增加了 %.1f%%** ⚠️\n", report.expenseChangePercentage));
            } else {
                summary.append("• **前期對比**：支出與上一週期持平。\n");
            }
        }

        if (!report.topCategories.isEmpty()) {
            ExpenseInsightReport.CategoryStat top1 = report.topCategories.get(0);
            summary.append(String.format(Locale.TAIWAN, "• **最大支出類別**：【%s】佔比高達 **%.1f%%** ($%,.0f 元)\n",
                    top1.categoryName, top1.percentage, top1.amount / 100.0));
        }

        if (!report.topPayees.isEmpty()) {
            ExpenseInsightReport.PayeeStat topPayee = report.topPayees.get(0);
            summary.append(String.format(Locale.TAIWAN, "• **最常消費店家**：【%s】(共消費 %d 次，合計 $%,.0f 元)\n",
                    topPayee.payeeName, topPayee.count, topPayee.amount / 100.0));
        }

        // Actionable savings tips
        tips.append("💡 **AI 財務優化建議**\n");
        if (report.topCategories.size() >= 1) {
            ExpenseInsightReport.CategoryStat top1 = report.topCategories.get(0);
            if (top1.percentage >= 40.0) {
                tips.append(String.format(Locale.TAIWAN, "1. 【%s】支出佔比偏高（%.1f%%），建議設定單週/單月預算上限，降低非必要開銷。\n", top1.categoryName, top1.percentage));
            } else {
                tips.append(String.format(Locale.TAIWAN, "1. 本期各類別支出分配相對平均，請繼續維持穩健的記帳與預算習慣。\n"));
            }
        }

        if (report.netBalance < 0 && report.totalIncome > 0) {
            tips.append("2. ⚠️ 本期出現入不敷出（赤字）現象，建議優先檢視娛樂與大額購物消費。\n");
        } else if (netInDollars > 0) {
            tips.append(String.format(Locale.TAIWAN, "2. ✅ 本期保持正向儲蓄結餘 $%,.0f 元，可規劃轉入投資或緊急預備金帳戶。\n", netInDollars));
        } else {
            tips.append("2. 建議多利用帳戶分類管理生活費與固定扣款，掌握現金流動向。\n");
        }

        tips.append("3. 📱 所有資料皆在裝置端本機處理，完全保障您的財務隱私安全。");

        report.summaryText = summary.toString();
        report.tipsText = tips.toString();
    }
}
