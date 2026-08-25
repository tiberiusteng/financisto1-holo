package tw.tib.financisto.ai;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ExpenseInsightReport implements Serializable {
    public static class CategoryStat implements Serializable {
        public String categoryName;
        public long amount;
        public double percentage;

        public CategoryStat(String categoryName, long amount, double percentage) {
            this.categoryName = categoryName;
            this.amount = amount;
            this.percentage = percentage;
        }
    }

    public static class PayeeStat implements Serializable {
        public String payeeName;
        public long amount;
        public int count;

        public PayeeStat(String payeeName, long amount, int count) {
            this.payeeName = payeeName;
            this.amount = amount;
            this.count = count;
        }
    }

    public String periodTitle = "";
    public long startDate;
    public long endDate;
    public long totalIncome = 0;
    public long totalExpense = 0;
    public long netBalance = 0;
    public long previousPeriodExpense = 0;
    public double expenseChangePercentage = 0.0;
    public int transactionCount = 0;

    public List<CategoryStat> topCategories = new ArrayList<>();
    public List<PayeeStat> topPayees = new ArrayList<>();

    public String summaryText = "";
    public String tipsText = "";
}
