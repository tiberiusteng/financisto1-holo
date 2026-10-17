package tw.tib.financisto.db;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.database.Cursor;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import tw.tib.financisto.model.Transaction;

/**
 * Running balance when editing a transfer moves an account to the other side.
 *
 * <p>A running_balance row is keyed by {@code (account_id, transaction_id)}, so a transfer owns
 * one row per account. Editing a transfer so that its receiving account becomes the sending one —
 * swapping "A to B" into "B to A", or turning "A to B" into "B to C" — means the same key is both
 * an old to-side row to remove and a new from-side row to add. If the new row is written before
 * the old one is removed, the removal takes the new row with it: the account total is still right, but the transaction
 * has no running balance row any more. The list then shows 0 for that account on that row, and
 * when it is the account's latest transaction the "running balance seems to be inaccurate"
 * warning appears.
 *
 * <p>Each test edits the transfer through {@link DatabaseAdapter#insertOrUpdate}, the same path
 * as saving the transfer screen. The tests create and remove their own accounts, using a high id
 * range to stay clear of real data.
 */
@RunWith(AndroidJUnit4.class)
public class TransferSideChangeDbTest {

    private static final long BASE = 900300000L;
    private static final long A = BASE + 1;
    private static final long B = BASE + 2;
    private static final long C = BASE + 3;
    /** Fixed timestamp rather than "now": a test must not behave differently depending on when it runs. */
    private static final long T = 1700000000000L;

    private DatabaseAdapter db;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = new DatabaseAdapter(context);
        db.open();
        cleanUp();
        insertAccount(A, "TransferSideChangeDbTest A");
        insertAccount(B, "TransferSideChangeDbTest B");
        insertAccount(C, "TransferSideChangeDbTest C");
        // an earlier transaction on every account, so the transfer is never the first row
        expense(A, -10000, T - 60000);
        expense(B, -20000, T - 60000);
        expense(C, -30000, T - 60000);
    }

    @After
    public void tearDown() {
        cleanUp();
    }

    private void insertAccount(long id, String title) {
        db.db().execSQL("INSERT INTO account (_id, title, type, currency_id, total_amount,"
                        + " is_active, is_include_into_totals, sort_order, creation_date, last_transaction_date)"
                        + " VALUES (?,?,?,?,?,?,?,?,?,?)",
                new Object[]{id, title, "CASH", currencyId(), 0L, 1L, 1L, 0L, 0L, 0L});
    }

    private void cleanUp() {
        for (long id : new long[]{A, B, C}) {
            db.db().execSQL("DELETE FROM running_balance WHERE account_id = " + id);
            db.db().execSQL("DELETE FROM transactions WHERE from_account_id = " + id
                    + " OR to_account_id = " + id);
            db.db().execSQL("DELETE FROM account WHERE _id = " + id);
        }
    }

    private long currencyId() {
        try (Cursor c = db.db().rawQuery("SELECT _id FROM currency LIMIT 1", null)) {
            return c.moveToFirst() ? c.getLong(0) : 0L;
        }
    }

    private long expense(long account, long amount, long dateTime) {
        Transaction t = new Transaction();
        t.fromAccountId = account;
        t.fromAmount = amount;
        t.dateTime = dateTime;
        return db.insertOrUpdate(t);
    }

    private long transfer(long from, long to, long amount, long dateTime) {
        Transaction t = new Transaction();
        t.fromAccountId = from;
        t.toAccountId = to;
        t.fromAmount = -amount;
        t.toAmount = amount;
        t.dateTime = dateTime;
        return db.insertOrUpdate(t);
    }

    /** What saving the edited transfer screen does: load the transaction, change its accounts, save it. */
    private void editAccounts(long id, long from, long to) {
        Transaction t = db.getTransaction(id);
        t.fromAccountId = from;
        t.toAccountId = to;
        assertEquals(id, db.insertOrUpdate(t));
    }

    private long accountTotal(long account) {
        try (Cursor c = db.db().rawQuery("SELECT total_amount FROM account WHERE _id=?",
                new String[]{String.valueOf(account)})) {
            return c.moveToFirst() ? c.getLong(0) : Long.MIN_VALUE;
        }
    }

    /**
     * The two books of one account have to agree: every running balance row equals the running
     * sum up to that transaction in {@code (datetime, transaction_id)} order, every transaction
     * has exactly one row, and the sum equals the account total.
     */
    private void assertBooksAgree(long account, String when) {
        long expected = 0;
        int transactions = 0;
        try (Cursor c = db.db().rawQuery(
                "SELECT t._id, CASE WHEN t.from_account_id = ? THEN t.from_amount ELSE t.to_amount END,"
                        + " rb.balance IS NULL, rb.balance FROM transactions t"
                        + " LEFT JOIN running_balance rb ON rb.transaction_id = t._id AND rb.account_id = ?"
                        + " WHERE (t.from_account_id = ? OR t.to_account_id = ?) AND t.parent_id = 0"
                        + " ORDER BY t.datetime, t._id",
                new String[]{String.valueOf(account), String.valueOf(account),
                        String.valueOf(account), String.valueOf(account)})) {
            while (c.moveToNext()) {
                expected += c.getLong(1);
                transactions++;
                assertEquals(when + ": account " + account + ", transaction " + c.getLong(0)
                        + " should have a running balance row", 0, c.getInt(2));
                assertEquals(when + ": running balance of account " + account + ", transaction " + c.getLong(0),
                        expected, c.getLong(3));
            }
        }
        assertEquals(when + ": total of account " + account, expected, accountTotal(account));
        try (Cursor c = db.db().rawQuery("SELECT COUNT(*) FROM running_balance WHERE account_id=?",
                new String[]{String.valueOf(account)})) {
            c.moveToFirst();
            assertEquals(when + ": running balance rows of account " + account, transactions, c.getInt(0));
        }
    }

    private void assertAllBooksAgree(String when) {
        assertBooksAgree(A, when);
        assertBooksAgree(B, when);
        assertBooksAgree(C, when);
    }

    /** "A to B" corrected into "B to A": both accounts change sides. */
    @Test
    public void swappingTheTwoAccountsOfATransfer() {
        long id = transfer(A, B, 500, T);

        editAccounts(id, B, A);

        assertAllBooksAgree("A to B edited into B to A");
        assertEquals(-10000 + 500, accountTotal(A));
        assertEquals(-20000 - 500, accountTotal(B));
    }

    /** "A to B" edited into "B to C": the receiving account becomes the sending one. */
    @Test
    public void receivingAccountBecomesTheSendingAccount() {
        long id = transfer(A, B, 500, T);

        editAccounts(id, B, C);

        assertAllBooksAgree("A to B edited into B to C");
        assertEquals(-10000, accountTotal(A));
        assertEquals(-20000 - 500, accountTotal(B));
        assertEquals(-30000 + 500, accountTotal(C));
    }

    /**
     * "A to B" edited into "C to A": the sending account becomes the receiving one. The old
     * from-side row is removed before the new to-side row is added, so this direction was always
     * fine; pinned so a fix cannot break it.
     */
    @Test
    public void sendingAccountBecomesTheReceivingAccount() {
        long id = transfer(A, B, 500, T);

        editAccounts(id, C, A);

        assertAllBooksAgree("A to B edited into C to A");
        assertEquals(-10000 + 500, accountTotal(A));
        assertEquals(-20000, accountTotal(B));
        assertEquals(-30000 - 500, accountTotal(C));
    }

    /**
     * The transfer is not the latest row: the total and the last row still agree, so no warning
     * appears, but the edited transfer itself must keep its row and the rows after it must be right.
     */
    @Test
    public void swappingAccountsOfATransferWithLaterTransactions() {
        long id = transfer(A, B, 500, T);
        expense(A, -100, T + 60000);
        expense(B, -200, T + 60000);

        editAccounts(id, B, A);

        assertAllBooksAgree("A to B edited into B to A, with later transactions");
    }

    /** Control: no account changes sides. This was always fine and must stay so. */
    @Test
    public void changingOnlyTheReceivingAccount() {
        long id = transfer(A, B, 500, T);

        editAccounts(id, A, C);

        assertAllBooksAgree("A to B edited into A to C");
        assertEquals(-10000 - 500, accountTotal(A));
        assertEquals(-20000, accountTotal(B));
        assertEquals(-30000 + 500, accountTotal(C));
    }
}
