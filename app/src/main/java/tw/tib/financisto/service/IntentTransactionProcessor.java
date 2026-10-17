package tw.tib.financisto.service;

import static java.lang.String.format;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tw.tib.financisto.R;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.model.Account;
import tw.tib.financisto.model.Category;
import tw.tib.financisto.model.Currency;
import tw.tib.financisto.model.Payee;
import tw.tib.financisto.model.Project;
import tw.tib.financisto.model.Tag;
import tw.tib.financisto.model.Transaction;
import tw.tib.financisto.model.TransactionStatus;
import tw.tib.financisto.utils.NotificationUtils;

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
    // A payment in a foreign currency: the currency's code exactly as in the currency list ("GBP")
    // and the amount in that currency, in the same format as AMOUNT. Used only when both are given;
    // AMOUNT stays the amount in the account's currency, and its sign wins if the two signs differ.
    public static final String ORIGINAL_CURRENCY = "ORIGINAL_CURRENCY";
    public static final String ORIGINAL_AMOUNT = "ORIGINAL_AMOUNT";

    /**
     * The extras that pre-fill a new transaction in TransactionActivity. TAGS does not start a
     * pre-fill: the activity already reads it on its own. ACCOUNT_NAME_TRANSFER_TO is left out
     * because TransactionActivity does not create transfers.
     */
    private static final String[] PREFILL_EXTRAS = {
            ACCOUNT_NAME, ACCOUNT_NUMBER_PARTIAL, CATEGORY_NAME, PAYEE_NAME, PROJECT_NAME, AMOUNT,
            NOTE, IS_CREDIT_CARD_PAYMENT, STATUS, TIMESTAMP_MILLIS, TIMESTAMP_ISO8601,
            ORIGINAL_CURRENCY, ORIGINAL_AMOUNT
    };

    /** What became of ORIGINAL_CURRENCY and ORIGINAL_AMOUNT. */
    enum OriginalAmount { NOT_GIVEN, APPLIED, SAME_AS_ACCOUNT, INCOMPLETE, UNKNOWN_CURRENCY, INVALID_AMOUNT }

    public IntentTransactionProcessor(DatabaseAdapter db) {
        this.db = db;
    }

    public Transaction createTransactionFromIntent(Intent intent) {
        Transaction tx = null;
        long transferToAccountId = 0;

        String accountName = trimmedExtra(intent, ACCOUNT_NAME);
        String accountNumberPartial = trimmedExtra(intent, ACCOUNT_NUMBER_PARTIAL);
        long accountId = findAccountId(accountName, accountNumberPartial);

        Log.d(TAG, format("accountName=%s accountNumberPartial=%s accountId=%s", accountName, accountNumberPartial, accountId));

        // Stress-test finding: Fail gracefully if account cannot be resolved, avoiding silent drop of transaction
        if (accountId <= 0) {
            String identifier = accountName != null ? accountName : (accountNumberPartial != null ? accountNumberPartial : "");
            Log.w(TAG, format("Account not found: %s — transaction skipped", identifier));
            sendErrorNotification(format("Financisto: Unknown account '%s' — transaction not imported", identifier),
                    identifier.hashCode());
            return null;
        }

        String accountNameTransferTo = intent.getStringExtra(ACCOUNT_NAME_TRANSFER_TO);
        if (accountNameTransferTo != null && !accountNameTransferTo.trim().isEmpty()) {
            accountNameTransferTo = accountNameTransferTo.trim();
            transferToAccountId = db.getEntityIdByTitle(Account.class, accountNameTransferTo);
            if (transferToAccountId <= 0) {
                Log.w(TAG, format("Transfer account not found: %s — transaction skipped", accountNameTransferTo));
                sendErrorNotification(format("Financisto: Unknown transfer account '%s' — transaction not imported", accountNameTransferTo),
                        accountNameTransferTo.hashCode());
                return null;
            }
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
        if (amountString == null || amountString.trim().isEmpty()) {
            Log.e(TAG, "Empty or missing AMOUNT — transaction skipped");
            sendErrorNotification("Financisto: Missing or empty amount — transaction not imported", 2);
            return null;
        }
        BigDecimal amount = parseAmount(amountString);
        if (amount == null) {
            Log.e(TAG, format("Invalid or zero AMOUNT '%s' — transaction skipped", amountString));
            sendErrorNotification(format("Financisto: Invalid amount '%s' — transaction not imported", amountString),
                    amountString.hashCode());
            return null;
        }

        Log.d(TAG, format("payee=%s project=%s amount=%s", payee, project, amount));

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

            Account account = db.getAccount(accountId);
            tx.fromAmount = toMinorUnits(amount, scaleOf(account));
            OriginalAmount original = OriginalAmount.NOT_GIVEN;
            if (transferToAccountId != 0) {
                tx.toAccountId = transferToAccountId;
                tx.toAmount = -1L * tx.fromAmount;
            } else {
                original = applyOriginalCurrency(tx, intent, amount, account);
            }

            applyCommonFields(tx, intent);
            Log.d(TAG, format("status=%s", tx.status));

            List<String> tags = extractTagsFromIntent(intent);
            if (!tags.isEmpty()) {
                for (String t : tags) {
                    db.findOrInsertEntityByTitle(Tag.class, t);
                }
                tx.tags = "\n" + String.join("\n", tags) + "\n";
            }

            tx.id = db.insertOrUpdate(tx);
            notifyOriginalAmountIgnored(original, intent);
            return tx;
        }

        return null;
    }

    /**
     * A new transaction built from an intent for the user to review in TransactionActivity.
     * Building it writes nothing to the database: the payee and project names are passed on as
     * given, and the activity matches or creates them when the user saves.
     */
    public static class Prefill {
        public final Transaction transaction = new Transaction();
        /** True when the intent names an account, whether it was found or not. */
        public boolean accountGiven;
        /** What the intent gave for an account, category or currency that was not found. */
        public String unknownAccount;
        public String unknownCategory;
        public String unknownCurrency;
        /** An AMOUNT or ORIGINAL_AMOUNT that is not a valid number. */
        public String invalidAmount;
        public String invalidOriginalAmount;
        /** Only one of ORIGINAL_CURRENCY and ORIGINAL_AMOUNT was given. */
        public boolean originalIncomplete;
        /**
         * The payee and project names: the entry's own title when found (also by an alias),
         * else as the intent gives them. A payee or project that exists is also set in the
         * transaction, for a screen that does not show that field.
         */
        public String payeeName;
        public String projectName;
        /**
         * The found payee's last category. The activity applies it only with "remember last
         * category" on, as when the payee is picked by hand.
         */
        public long payeeLastCategoryId;
    }

    public static boolean hasPrefillExtras(Intent intent) {
        if (intent == null) return false;
        for (String key : PREFILL_EXTRAS) {
            if (intent.hasExtra(key)) return true;
        }
        return false;
    }

    /** Removes the pre-fill extras, and the tags, so that Save & New starts empty. */
    public static void removePrefillExtras(Intent intent) {
        for (String key : PREFILL_EXTRAS) {
            intent.removeExtra(key);
        }
        intent.removeExtra(TAGS);
        intent.removeExtra("tags");
    }

    /**
     * @param defaultAccountId the account to use when the intent names none (the screen's usual
     *                         default for a new transaction), or 0 for none
     */
    public Prefill createPrefillFromIntent(Intent intent, long defaultAccountId) {
        Prefill p = new Prefill();
        Transaction tx = p.transaction;
        tx.isTemplate = 0;

        String accountName = trimmedExtra(intent, ACCOUNT_NAME);
        String accountNumberPartial = trimmedExtra(intent, ACCOUNT_NUMBER_PARTIAL);
        p.accountGiven = accountName != null || accountNumberPartial != null;
        if (p.accountGiven) {
            tx.fromAccountId = Math.max(0, findAccountId(accountName, accountNumberPartial));
            if (tx.fromAccountId == 0) {
                p.unknownAccount = accountName != null ? accountName : accountNumberPartial;
            }
        } else if (defaultAccountId > 0 && db.getAccount(defaultAccountId) != null) {
            // set before the amounts below, which use the account's currency
            tx.fromAccountId = defaultAccountId;
        }

        p.payeeName = trimmedExtra(intent, PAYEE_NAME);
        if (p.payeeName != null) {
            Payee payee = db.findEntityByTitle(Payee.class, p.payeeName);
            if (payee != null) {
                tx.payeeId = payee.id;
                p.payeeName = payee.title;
                p.payeeLastCategoryId = payee.lastCategoryId;
            }
        }

        String categoryName = trimmedExtra(intent, CATEGORY_NAME);
        if (categoryName != null) {
            Category category = db.getCategory(categoryName);
            if (category != null) {
                tx.categoryId = category.id;
            } else {
                // left empty, not the payee's last category, so the user sees it must be picked
                p.unknownCategory = categoryName;
            }
        }

        p.projectName = trimmedExtra(intent, PROJECT_NAME);
        if (p.projectName != null) {
            Project project = db.findEntityByTitle(Project.class, p.projectName);
            if (project != null) {
                tx.projectId = project.id;
                p.projectName = project.title;
            }
        }

        Account account = tx.fromAccountId > 0 ? db.getAccount(tx.fromAccountId) : null;
        BigDecimal amount = null;
        String amountString = trimmedExtra(intent, AMOUNT);
        if (amountString != null) {
            amount = parseAmount(amountString);
            if (amount != null) {
                tx.fromAmount = toMinorUnits(amount, scaleOf(account));
            } else {
                p.invalidAmount = amountString;
            }
        }

        switch (applyOriginalCurrency(tx, intent, amount, account)) {
            case UNKNOWN_CURRENCY -> p.unknownCurrency = trimmedExtra(intent, ORIGINAL_CURRENCY);
            case INVALID_AMOUNT -> p.invalidOriginalAmount = trimmedExtra(intent, ORIGINAL_AMOUNT);
            case INCOMPLETE -> p.originalIncomplete = true;
            default -> { }
        }

        applyCommonFields(tx, intent);
        return p;
    }

    /** Account by exact name first, then by the end of its number; 0 when neither matches. */
    private long findAccountId(String accountName, String accountNumberPartial) {
        long accountId = 0;
        if (accountName != null) {
            accountId = db.getEntityIdByTitle(Account.class, accountName);
        }
        if (accountId <= 0 && accountNumberPartial != null) {
            List<Long> accountIds = db.findAccountsByNumber(accountNumberPartial);
            if (!accountIds.isEmpty()) {
                accountId = accountIds.get(0);
                if (accountIds.size() > 1) {
                    Log.e(TAG, format("Accounts number with partial `%s` - more than one!", accountNumberPartial));
                }
            }
        }
        return accountId;
    }

    /** Note, status, credit-card flag and date: the same for a saved and a pre-filled transaction. */
    private static void applyCommonFields(Transaction tx, Intent intent) {
        String note = intent.getStringExtra(NOTE);
        if (note != null) {
            tx.note = note;
        }

        TransactionStatus status;
        try {
            status = TransactionStatus.valueOf(intent.getStringExtra(STATUS));
        } catch (Exception e) {
            status = TransactionStatus.UR;
        }
        tx.status = status;

        if (intent.getBooleanExtra(IS_CREDIT_CARD_PAYMENT, false)) {
            tx.isCCardPayment = 1;
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
    }

    /**
     * Sets the original currency and amount from ORIGINAL_CURRENCY and ORIGINAL_AMOUNT, when both
     * are given and the currency is not the account's own.
     *
     * @param amount  the amount in the account's currency, or null if not known
     * @param account the transaction's account, or null if not known
     */
    private OriginalAmount applyOriginalCurrency(Transaction tx, Intent intent, BigDecimal amount, Account account) {
        String code = trimmedExtra(intent, ORIGINAL_CURRENCY);
        String originalAmountString = trimmedExtra(intent, ORIGINAL_AMOUNT);
        if (code == null && originalAmountString == null) {
            return OriginalAmount.NOT_GIVEN;
        }
        if (code == null || originalAmountString == null) {
            return OriginalAmount.INCOMPLETE;
        }
        long currencyId = db.findCurrencyByName(code);
        Currency currency = currencyId > 0 ? db.get(Currency.class, currencyId) : null;
        if (currency == null) {
            return OriginalAmount.UNKNOWN_CURRENCY;
        }
        BigDecimal originalAmount = parseAmount(originalAmountString);
        if (originalAmount == null) {
            return OriginalAmount.INVALID_AMOUNT;
        }
        if (account != null && account.currency != null && account.currency.id == currency.id) {
            return OriginalAmount.SAME_AS_ACCOUNT;
        }
        tx.originalCurrencyId = currency.id;
        tx.originalFromAmount = toMinorUnits(withSignOf(originalAmount, amount), currency.getScale());
        return OriginalAmount.APPLIED;
    }

    /** For the NEW_TRANSACTION service: the transaction is saved, but say what was left out. */
    private void notifyOriginalAmountIgnored(OriginalAmount result, Intent intent) {
        String reason;
        switch (result) {
            case INCOMPLETE -> reason = "ORIGINAL_CURRENCY and ORIGINAL_AMOUNT must be sent together";
            case UNKNOWN_CURRENCY -> reason = format("unknown currency '%s'", trimmedExtra(intent, ORIGINAL_CURRENCY));
            case INVALID_AMOUNT -> reason = format("invalid ORIGINAL_AMOUNT '%s'", trimmedExtra(intent, ORIGINAL_AMOUNT));
            default -> {
                return;
            }
        }
        Log.w(TAG, "Original amount ignored: " + reason);
        sendErrorNotification("Financisto: Transaction saved without the original amount — " + reason,
                reason.hashCode());
    }

    /** The scale this account's amounts are stored in (Currency.getScale()); 2 if not known. */
    private static int scaleOf(Account account) {
        return account != null && account.currency != null ? account.currency.getScale() : 2;
    }

    /** value with the sign of reference; value unchanged when reference is null or zero. */
    static BigDecimal withSignOf(BigDecimal value, BigDecimal reference) {
        if (reference == null || reference.signum() == 0 || value.signum() == 0) {
            return value;
        }
        return value.signum() == reference.signum() ? value : value.negate();
    }

    /** The amount as a number, or null if it is empty, not a number or zero. */
    static BigDecimal parseAmount(String amountString) {
        try {
            BigDecimal amount = SmsTransactionProcessor.toBigDecimal(amountString);
            return amount == null || amount.signum() == 0 ? null : amount;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** The amount as stored: in minor units of the given scale, e.g. 1.5 is 150 at 2, 1500 at 3. */
    static long toMinorUnits(BigDecimal amount, int scale) {
        return amount.movePointRight(scale).longValue();
    }

    private static String trimmedExtra(Intent intent, String key) {
        String value = intent.getStringExtra(key);
        if (value == null) return null;
        value = value.trim();
        return value.isEmpty() ? null : value;
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

    private void sendErrorNotification(String message, int notificationId) {
        Context context = db != null ? db.getContext() : null;
        if (context != null) {
            try {
                Notification notification = new NotificationCompat.Builder(context, NotificationChannelService.TRANSACTIONS_CHANNEL)
                        .setSmallIcon(R.mipmap.a_icon_notify)
                        .setWhen(System.currentTimeMillis())
                        .setContentTitle("Financisto")
                        .setContentText(message)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                        .setTicker(message)
                        .setAutoCancel(true)
                        .build();
                NotificationUtils.notifyUser(context, notification, notificationId);
            } catch (Exception e) {
                Log.e(TAG, "Failed to show notification: " + message, e);
            }
        }
    }
}
