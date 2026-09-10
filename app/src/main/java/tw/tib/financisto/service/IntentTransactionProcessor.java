package tw.tib.financisto.service;

import static java.lang.String.format;

import android.content.Intent;
import android.util.Log;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.model.Account;
import tw.tib.financisto.model.Category;
import tw.tib.financisto.model.Payee;
import tw.tib.financisto.model.Project;
import tw.tib.financisto.model.Tag;
import tw.tib.financisto.model.Transaction;
import tw.tib.financisto.model.TransactionStatus;

public class IntentTransactionProcessor {
    private static final String TAG = "IntentTxProc";

    private final DatabaseAdapter db;

    public static final String ACCOUNT_NAME = "ACCOUNT_NAME";
    public static final String ACCOUNT_NUMBER_PARTIAL = "ACCOUNT_NUMBER_PARTIAL"; // "1234" matches account with number "25691234" etc.
    public static final String ACCOUNT_NAME_TRANSFER_TO = "ACCOUNT_NAME_TRANSFER_TO";
    public static final String CATEGORY_NAME = "CATEGORY_NAME";
    public static final String PAYEE_NAME = "PAYEE_NAME";
    public static final String PROJECT_NAME = "PROJECT_NAME";
    public static final String AMOUNT = "AMOUNT"; // String, "-16.25", "35", "4,500", "1.23"
    public static final String NOTE = "NOTE";
    public static final String IS_CREDIT_CARD_PAYMENT = "IS_CREDIT_CARD_PAYMENT";
    public static final String STATUS = "STATUS"; // "RS", "PN", "UR", "CL", "RC"; see TransactionStatus
    public static final String TIMESTAMP_MILLIS = "TIMESTAMP_MILLIS"; // Unix timestamp in milliseconds, long
    public static final String TIMESTAMP_ISO8601 = "TIMESTAMP_ISO8601"; // "2026-09-10T19:57:45+08:00"
    public static final String TAGS = "TAGS"; // String ("food" or "food, groceries"), String[], or ArrayList<String>

    private static BigDecimal HUNDRED = new BigDecimal(100);

    public IntentTransactionProcessor(DatabaseAdapter db) {
        this.db = db;
    }

    public Transaction createTransactionFromIntent(Intent intent) {
        Transaction tx = null;
        long accountId = 0;
        long transferToAccountId = 0;

        String accountName = intent.getStringExtra(ACCOUNT_NAME);
        if (accountName != null) {
            accountId = db.getEntityIdByTitle(Account.class, accountName);
        }

        String accountNumberPartial = intent.getStringExtra(ACCOUNT_NUMBER_PARTIAL);
        if (accountId == 0 && accountNumberPartial != null) {
            List<Long> accountIds = db.findAccountsByNumber(accountNumberPartial);
            if (!accountIds.isEmpty()) {
                accountId = accountIds.get(0);
                if (accountIds.size() > 1) {
                    Log.e(TAG, format("Accounts number with partial `%s` - more than one!", accountNumberPartial));
                }
            }
        }

        Log.d(TAG, format("accountName=%s accountNumberPartial=%s accountId=%s", accountName, accountNumberPartial, accountId));

        String accountNameTransferTo = intent.getStringExtra(ACCOUNT_NAME_TRANSFER_TO);
        if (accountNameTransferTo != null) {
            transferToAccountId = db.getEntityIdByTitle(Account.class, accountNameTransferTo);
        }

        Log.d(TAG, format("accountNameTransferTo=%s transferToAccountId=%s", accountNameTransferTo, transferToAccountId));

        Category category = null;
        String categoryName = intent.getStringExtra(CATEGORY_NAME);
        if (categoryName != null) {
            category = db.getCategory(categoryName);
        }

        Payee payee = null;
        Project project = null;
        String payeeName = intent.getStringExtra(PAYEE_NAME);
        if (payeeName != null) {
            payee = db.findOrInsertEntityByTitle(Payee.class, payeeName);
        }
        String projectName = intent.getStringExtra(PROJECT_NAME);
        if (projectName != null) {
            project = db.findOrInsertEntityByTitle(Project.class, projectName);
        }
        String amountString = intent.getStringExtra(AMOUNT);
        BigDecimal amount = SmsTransactionProcessor.toBigDecimal(amountString);

        Log.d(TAG, format("payee=%s project=%s amount=%s", payee, project, amount));

        TransactionStatus status;
        try {
            status = TransactionStatus.valueOf(intent.getStringExtra(STATUS));
        } catch (Exception e) {
            status = TransactionStatus.UR;
        }

        Log.d(TAG, format("status=%s", status));

        if (amount.compareTo(BigDecimal.ZERO) != 0 && accountId != 0) {
            tx = new Transaction();
            tx.isTemplate = 0;
            tx.fromAccountId = accountId;

            if (payee != null) {
                tx.payeeId = payee.id;
                tx.categoryId = payee.lastCategoryId;
            }

            if (category != null) {
                tx.categoryId = category.id;
            }

            if (project != null) {
                tx.projectId = project.id;
            }

            tx.fromAmount = amount.multiply(HUNDRED).longValue();
            if (transferToAccountId != 0) {
                tx.toAccountId = transferToAccountId;
                tx.toAmount = -1L * tx.fromAmount;
            }

            String note = intent.getStringExtra(NOTE);
            if (note != null) {
                tx.note = note;
            }

            tx.status = status;

            if (intent.getBooleanExtra(IS_CREDIT_CARD_PAYMENT, false)) {
                tx.isCCardPayment = 1;
            }

            List<String> tags = extractTagsFromIntent(intent);
            if (!tags.isEmpty()) {
                for (String t : tags) {
                    db.findOrInsertEntityByTitle(Tag.class, t);
                }
                tx.tags = String.join("\n", tags);
            }

            long timestampMillis = intent.getLongExtra(TIMESTAMP_MILLIS, 0);
            if (timestampMillis != 0) {
                tx.dateTime = timestampMillis;
            }

            String timeIso8601 = intent.getStringExtra(TIMESTAMP_ISO8601);
            if (timeIso8601 != null) {
                DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_DATE_TIME;
                try {
                    tx.dateTime = Instant.from(timeFormatter.parse(timeIso8601)).toEpochMilli();
                } catch (Exception ignored) {

                }
            }

            tx.id = db.insertOrUpdate(tx);
            return tx;
        }

        return null;
    }

    public static List<String> extractTagsFromIntent(Intent intent) {
        if (intent == null) return Collections.emptyList();
        List<String> result = new ArrayList<>();

        // 1. Check String arrays / ArrayLists first
        for (String key : new String[]{"TAGS", "tags"}) {
            String[] arr = intent.getStringArrayExtra(key);
            if (arr != null && arr.length > 0) {
                for (String s : arr) {
                    addTagToList(result, s);
                }
                return result;
            }
            ArrayList<String> list = intent.getStringArrayListExtra(key);
            if (list != null && !list.isEmpty()) {
                for (String s : list) {
                    addTagToList(result, s);
                }
                return result;
            }
        }

        // 2. Check String extra (single tag e.g. "Coffee" or newline/comma/semicolon-separated tags e.g. "Coffee, Food")
        for (String key : new String[]{"TAGS", "tags"}) {
            String val = intent.getStringExtra(key);
            if (val != null && !val.trim().isEmpty()) {
                for (String s : val.split("[\n,;]")) {
                    addTagToList(result, s);
                }
                if (!result.isEmpty()) {
                    return result;
                }
            }
        }

        return result;
    }

    private static void addTagToList(List<String> list, String s) {
        if (s != null) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty() && !list.contains(trimmed)) {
                list.add(trimmed);
            }
        }
    }
}
