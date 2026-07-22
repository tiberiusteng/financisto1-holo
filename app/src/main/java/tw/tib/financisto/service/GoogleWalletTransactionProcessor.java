package tw.tib.financisto.service;

import static java.lang.String.format;

import android.content.Context;
import android.util.Log;

import java.math.BigDecimal;
import java.util.List;

import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.model.Account;
import tw.tib.financisto.model.Payee;
import tw.tib.financisto.model.Transaction;
import tw.tib.financisto.model.TransactionStatus;
import tw.tib.financisto.service.GoogleWalletNotificationParser.ParsedPayment;

/**
 * Creates an expense transaction from a parsed Google Wallet payment notification.
 * The account is resolved by the card last 4 digits against the account card
 * number field (same matching as the SMS template <:A:> placeholder).
 */
public class GoogleWalletTransactionProcessor {
    private static final String TAG = "GoogleWalletProcessor";
    private static final BigDecimal HUNDRED = new BigDecimal(100);

    private final DatabaseAdapter db;

    public GoogleWalletTransactionProcessor(DatabaseAdapter db) {
        this.db = db;
    }

    public Transaction createTransaction(Context context, ParsedPayment payment,
            String notificationText, TransactionStatus status, boolean saveNotificationToNote)
    {
        long accountId = findAccount(payment);
        if (accountId <= 0) {
            // No matching account: skip silently. Do NOT create a log transaction
            // here — db.log() would insert a zero-amount entry into the first
            // account, which for Wallet (where every payment notification is
            // processed) would spam junk transactions.
            Log.w(TAG, format("No account matches Wallet card `%s` (last4=%s), skipping",
                    payment.cardLabel, payment.cardLast4));
            return null;
        }

        Transaction t = new Transaction();
        t.isTemplate = 0;
        t.fromAccountId = accountId;

        if (payment.merchant != null) {
            Payee payee = db.findOrInsertEntityByTitle(Payee.class, payment.merchant);
            t.payeeId = payee.id;
            t.categoryId = payee.lastCategoryId;
        }

        long fromAmount = -Math.abs(payment.amount.multiply(HUNDRED).longValue());
        if (payment.currency != null) {
            long currencyId = db.findCurrencyByName(payment.currency);
            if (currencyId != 0) {
                Account a = db.getAccount(accountId);
                if (a.currency.id != currencyId) {
                    t.originalCurrencyId = currencyId;
                    t.originalFromAmount = fromAmount;
                }
            }
        }
        if (t.originalCurrencyId == 0) {
            t.fromAmount = fromAmount;
        }

        if (saveNotificationToNote) {
            t.note = notificationText;
        } else {
            t.note = payment.merchant != null ? payment.merchant : "";
        }
        t.status = status;
        t.id = db.insertOrUpdate(t);

        Log.i(TAG, format("Google Wallet transaction `%s` was added with id=%s", t, t.id));
        return t;
    }

    /**
     * Resolves the Financisto account for a Wallet card. Primary matching is by
     * account name (the user names the account like the Wallet card label), with
     * fallbacks to the card last-4 digits and a substring match on the account
     * card-number field.
     */
    private long findAccount(ParsedPayment payment) {
        String label = payment.cardLabel != null ? payment.cardLabel.trim() : "";

        // 1. last 4 digits (e.g. "Visa PrivatBank ••6810") against the card-number field
        if (payment.cardLast4 != null && !payment.cardLast4.isEmpty()) {
            long id = firstOrNone(db.findAccountsByNumber(payment.cardLast4),
                    "card ending " + payment.cardLast4);
            if (id > 0) {
                return id;
            }
        }

        if (!label.isEmpty()) {
            // 2. account title equal to the card label (exact, then case-insensitive)
            long id = db.getEntityIdByTitle(Account.class, label);
            if (id > 0) {
                return id;
            }
            List<Account> accounts = db.getAllAccountsList();
            for (Account a : accounts) {
                if (a.title != null && a.title.trim().equalsIgnoreCase(label)) {
                    return a.id;
                }
            }

            // 3. card-number field contains the label
            id = firstOrNone(db.findAccountsByNumber(label), "card label " + label);
            if (id > 0) {
                return id;
            }

            // 4. account title contained in the label, e.g. account "PrivatBank"
            //    matches "Visa PrivatBank ••6810" (guarded to avoid trivial matches)
            String labelLc = label.toLowerCase();
            for (Account a : accounts) {
                String title = a.title != null ? a.title.trim() : "";
                if (title.length() >= 3 && labelLc.contains(title.toLowerCase())) {
                    return a.id;
                }
            }
        }
        return -1;
    }

    private long firstOrNone(List<Long> accountIds, String what) {
        if (accountIds == null || accountIds.isEmpty()) {
            return -1;
        }
        if (accountIds.size() > 1) {
            Log.e(TAG, format("More than one account matches %s", what));
        }
        return accountIds.get(0);
    }
}
