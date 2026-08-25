package tw.tib.financisto.ai;

import java.io.Serializable;

public class ParsedTransactionResult implements Serializable {
    public long amount = 0; // Amount in subunit (e.g. 100 TWD -> 10000 with 2 decimals)
    public boolean isExpense = true;
    public String note = "";
    public String payeeName = "";
    public String accountName = "";
    public String categoryName = "";
    public long dateTime = System.currentTimeMillis();
    public long matchedAccountId = -1;
    public long matchedCategoryId = -1;
    public long matchedPayeeId = -1;
    public float confidence = 1.0f;
    public String rawInput = "";

    @Override
    public String toString() {
        return "ParsedTransactionResult{" +
                "amount=" + amount +
                ", isExpense=" + isExpense +
                ", note='" + note + '\'' +
                ", payeeName='" + payeeName + '\'' +
                ", accountName='" + accountName + '\'' +
                ", categoryName='" + categoryName + '\'' +
                ", matchedAccountId=" + matchedAccountId +
                ", matchedCategoryId=" + matchedCategoryId +
                ", matchedPayeeId=" + matchedPayeeId +
                '}';
    }
}
