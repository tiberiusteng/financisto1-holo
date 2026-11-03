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

import tw.tib.financisto.activity.FormSwitch;
import tw.tib.financisto.model.Transaction;

/**
 * Changing the kind of an existing transaction (transaction / transfer) updates it in place: in
 * the table the two differ only in to_account_id, the id stays, and updateTransaction recalculates
 * the account totals and running balances from the old and the new version.
 *
 * <p>These check that recalculation: after each change both accounts' totals and every running
 * balance row agree with the transactions, and changing back restores the original state. None of
 * it shows on screen except the "running balances look out of sync" warning afterwards. The tests
 * create and remove their own accounts, using a high id range to stay clear of real data.
 */
@RunWith(AndroidJUnit4.class)
public class TypeChangeInPlaceDbTest {

    private static final long BASE = 900200000L;
    private static final long A = BASE + 1;
    private static final long B = BASE + 2;
    /** Fixed timestamp rather than "now": a test must not behave differently depending on when it runs. */
    private static final long T = 1700000000000L;

    private DatabaseAdapter db;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = new DatabaseAdapter(context);
        db.open();
        cleanUp();
        insertAccount(A, "TypeChangeInPlaceDbTest A");
        insertAccount(B, "TypeChangeInPlaceDbTest B");
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
        for (long id : new long[]{A, B}) {
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

    private long total(long account) {
        try (Cursor c = db.db().rawQuery("SELECT total_amount FROM account WHERE _id=?",
                new String[]{String.valueOf(account)})) {
            return c.moveToFirst() ? c.getLong(0) : Long.MIN_VALUE;
        }
    }

    private int transactionCount() {
        try (Cursor c = db.db().rawQuery("SELECT COUNT(*) FROM transactions WHERE from_account_id IN (?,?)"
                        + " OR to_account_id IN (?,?)",
                new String[]{String.valueOf(A), String.valueOf(B), String.valueOf(A), String.valueOf(B)})) {
            return c.moveToFirst() ? c.getInt(0) : -1;
        }
    }

    /** Every running balance row of the account = the sum up to that transaction in (datetime, _id) order, and the last one = the total. */
    private void assertBooksAgree(long account, String when) {
        long expected = 0;
        int rows = 0;
        try (Cursor c = db.db().rawQuery(
                "SELECT t._id, CASE WHEN t.from_account_id = ? THEN t.from_amount ELSE t.to_amount END,"
                        + " rb.balance FROM transactions t"
                        + " LEFT JOIN running_balance rb ON rb.transaction_id = t._id AND rb.account_id = ?"
                        + " WHERE (t.from_account_id = ? OR t.to_account_id = ?) AND t.parent_id = 0"
                        + " ORDER BY t.datetime, t._id",
                new String[]{String.valueOf(account), String.valueOf(account),
                        String.valueOf(account), String.valueOf(account)})) {
            while (c.moveToNext()) {
                expected += c.getLong(1);
                rows++;
                assertEquals(when + ": running balance of account " + account + ", transaction " + c.getLong(0),
                        expected, c.getLong(2));
            }
        }
        assertEquals(when + ": total of account " + account, expected, total(account));
        try (Cursor c = db.db().rawQuery("SELECT COUNT(*) FROM running_balance WHERE account_id=?",
                new String[]{String.valueOf(account)})) {
            c.moveToFirst();
            assertEquals(when + ": running balance rows of account " + account, rows, c.getInt(0));
        }
    }

    /** What saving a switched form does: take the original id and insertOrUpdate it with the new kind's fields. */
    private void saveInPlace(long id, long from, long to, long fromAmount, long toAmount) {
        Transaction t = db.getTransaction(id);
        t.fromAccountId = from;
        t.toAccountId = to;
        t.fromAmount = fromAmount;
        t.toAmount = toAmount;
        assertEquals(id, db.insertOrUpdate(t));
    }

    @Test
    public void expenseBecomesTransferAndBackInPlace() {
        expense(A, -10000, T - 1000);
        long id = expense(A, -500, T);
        expense(B, -200, T + 1000);
        int count = transactionCount();

        saveInPlace(id, A, B, -500, 500);
        assertEquals("updated in place, no extra transaction", count, transactionCount());
        assertEquals(-10500, total(A));
        assertEquals(300, total(B));
        assertBooksAgree(A, "expense into transfer");
        assertBooksAgree(B, "expense into transfer");

        saveInPlace(id, A, 0, -500, 0);
        assertEquals(-10500, total(A));
        assertEquals(-200, total(B));
        assertBooksAgree(A, "back to expense");
        assertBooksAgree(B, "back to expense");
    }

    @Test
    public void incomeBecomesTransferIntoTheSameAccount() {
        long id = expense(A, 800, T);
        saveInPlace(id, B, A, -800, 800);       // income to transfer: the account is on the "to" side
        assertEquals(800, total(A));
        assertEquals(-800, total(B));
        assertBooksAgree(A, "income into transfer in");
        assertBooksAgree(B, "income into transfer in");
    }

    /**
     * An existing transaction switched to update balance and saved with a counted balance: the
     * current balance excludes the transaction itself (FormSwitch.balanceExcluding), so afterwards
     * the account total is exactly the counted balance.
     */
    @Test
    public void existingExpenseRestatedThroughUpdateBalance() {
        expense(A, -10000, T - 1000);
        long id = expense(A, -150, T);
        Transaction old = db.getTransaction(id);
        long base = FormSwitch.balanceExcluding(total(A), A,
                old.fromAccountId, old.toAccountId, old.fromAmount, old.toAmount);
        long counted = -10170;
        saveInPlace(id, A, 0, counted - base, 0);
        assertEquals(-170, db.getTransaction(id).fromAmount);
        assertEquals(counted, total(A));
        assertBooksAgree(A, "existing expense restated with a counted balance");
    }

    @Test
    public void existingTransferRestatedOnItsDestination() {
        long id = transfer(A, B, 500, T);
        Transaction old = db.getTransaction(id);
        long base = FormSwitch.balanceExcluding(total(B), B,
                old.fromAccountId, old.toAccountId, old.fromAmount, old.toAmount);
        saveInPlace(id, B, 0, 450 - base, 0);    // B counted 450: this becomes +450 on B, A gets its -500 back
        assertEquals(450, total(B));
        assertEquals(0, total(A));
        assertBooksAgree(A, "existing transfer restated on its \"to\" side");
        assertBooksAgree(B, "existing transfer restated on its \"to\" side");
    }

    /** The account moves from the "to" side to the "from" side: transfer B -> A into an income on A (see #170). */
    @Test
    public void transferIntoAnAccountBecomesItsIncome() {
        long id = transfer(B, A, 800, T);
        expense(A, -100, T + 1000);

        saveInPlace(id, A, 0, 800, 0);
        assertEquals(700, total(A));
        assertEquals(0, total(B));
        assertBooksAgree(A, "transfer in into income");
        assertBooksAgree(B, "transfer in into income");
    }

    @Test
    public void transferBecomesExpenseOnTheSourceAccount() {
        long id = transfer(A, B, 700, T);
        expense(B, -50, T);                      // another one at the same time: covers that path too

        saveInPlace(id, A, 0, -700, 0);
        assertEquals(-700, total(A));
        assertEquals(-50, total(B));
        assertBooksAgree(A, "transfer into expense");
        assertBooksAgree(B, "transfer into expense");
    }
}
