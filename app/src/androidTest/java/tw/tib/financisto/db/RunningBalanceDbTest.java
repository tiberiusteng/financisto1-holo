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
 * Running balance invariants when several transactions on one account share a timestamp.
 *
 * <p>The account total and the running balance are two separately maintained books; when they
 * disagree the app shows "running balance seems to be inaccurate". The statements that maintain
 * running_balance used to compare {@code datetime} alone to decide which rows to shift, while the
 * table is ordered by {@code (datetime, transaction_id)} — rows sharing the timestamp were skipped
 * and the two books drifted apart.
 *
 * <p>Sharing an exact timestamp is not exotic: scheduled transactions have their seconds and
 * milliseconds zeroed (AbstractTransactionActivity, via DateUtils.zeroSeconds), so two schedules
 * firing at the same date and time on one account collide every period; CSV import never carries
 * milliseconds (CsvTransaction.combineToMillis), so a file with date-only or minute-precision
 * timestamps produces whole batches of identical values. Once such rows exist, editing any one of
 * them goes down the same code path, because updating a transaction is delete + insert.
 *
 * <p>These run on a device because they need the real SQLite and the real DatabaseAdapter flow.
 * None of this is visible in the UI while it happens — only the warning afterwards. The test
 * creates and removes its own account, using a high id range to stay clear of real data.
 */
@RunWith(AndroidJUnit4.class)
public class RunningBalanceDbTest {

    private static final long BASE = 900100000L;
    private static final long ACCOUNT_ID = BASE + 1;
    /** Fixed timestamp rather than "now": a test must not behave differently depending on when it runs. */
    private static final long T = 1700000000000L;

    private DatabaseAdapter db;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = new DatabaseAdapter(context);
        db.open();
        cleanUp();
        db.db().execSQL("INSERT INTO account (_id, title, type, currency_id, total_amount,"
                + " is_active, is_include_into_totals, sort_order, creation_date, last_transaction_date)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?)",
                new Object[]{ACCOUNT_ID, "RunningBalanceDbTest", "CASH", currencyId(), 0L, 1L, 1L, 0L, 0L, 0L});
    }

    @After
    public void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        db.db().execSQL("DELETE FROM running_balance WHERE account_id = " + ACCOUNT_ID);
        db.db().execSQL("DELETE FROM transactions WHERE from_account_id = " + ACCOUNT_ID
                + " OR to_account_id = " + ACCOUNT_ID);
        db.db().execSQL("DELETE FROM account WHERE _id = " + ACCOUNT_ID);
    }

    private long currencyId() {
        try (Cursor c = db.db().rawQuery("SELECT _id FROM currency LIMIT 1", null)) {
            return c.moveToFirst() ? c.getLong(0) : 0L;
        }
    }

    private long insertTx(long amount, long dateTime) {
        Transaction t = new Transaction();
        t.fromAccountId = ACCOUNT_ID;
        t.fromAmount = amount;
        t.dateTime = dateTime;
        return db.insertOrUpdate(t);
    }

    private long accountTotal() {
        try (Cursor c = db.db().rawQuery("SELECT total_amount FROM account WHERE _id=?",
                new String[]{String.valueOf(ACCOUNT_ID)})) {
            return c.moveToFirst() ? c.getLong(0) : Long.MIN_VALUE;
        }
    }

    private long lastRunningBalance() {
        try (Cursor c = db.db().rawQuery("SELECT balance FROM running_balance WHERE account_id=?"
                        + " ORDER BY datetime DESC, transaction_id DESC LIMIT 1",
                new String[]{String.valueOf(ACCOUNT_ID)})) {
            return c.moveToFirst() ? c.getLong(0) : Long.MIN_VALUE;
        }
    }

    /**
     * The two books have to agree, at two levels.
     *
     * <p>(1) The last running balance row equals the account total — that is what the app's
     * "seems to be inaccurate" check looks at. (2) <em>Every</em> row equals the running sum up to
     * that transaction in {@code (datetime, transaction_id)} order. Checking only the last row
     * would miss a wrong value in the middle that happens to cancel out by the end, and those
     * middle rows are exactly the numbers the user reads in the transaction list.
     */
    private void assertBooksAgree(String when) {
        assertEquals(when + ": last running balance row should equal the account total",
                accountTotal(), lastRunningBalance());
        long expected = 0;
        try (Cursor c = db.db().rawQuery(
                "SELECT t._id, t.from_amount, rb.balance FROM transactions t"
                        + " LEFT JOIN running_balance rb"
                        + " ON rb.transaction_id = t._id AND rb.account_id = t.from_account_id"
                        + " WHERE t.from_account_id = ? ORDER BY t.datetime, t._id",
                new String[]{String.valueOf(ACCOUNT_ID)})) {
            while (c.moveToNext()) {
                expected += c.getLong(1);
                assertEquals(when + ": running balance of transaction " + c.getLong(0),
                        expected, c.getLong(2));
            }
        }
    }

    @Test
    public void twoTransactionsAtTheSameTimeStayInSync() {
        insertTx(-50000, T);
        insertTx(-30000, T);
        assertBooksAgree("two transactions at the same time");
        assertEquals(-80000L, accountTotal());
    }

    /**
     * Minimal reproduction of the reported drift: two transactions at the same time, delete the
     * one that sorts <em>first</em>. Before the fix the later row was not shifted, so the account
     * total and the running balance stayed apart by the deleted amount from then on.
     */
    @Test
    public void deletingTheEarlierOfTwoAtTheSameTimeKeepsBooksInSync() {
        long first = insertTx(-50000, T);
        insertTx(-30000, T);

        db.deleteTransaction(first);

        assertBooksAgree("deleted the earlier of two at the same time");
        assertEquals(-30000L, accountTotal());
    }

    /** Deleting the later one was always fine (nothing sorts after it); pinned so a fix cannot cover only one side. */
    @Test
    public void deletingTheLaterOfTwoAtTheSameTimeKeepsBooksInSync() {
        insertTx(-50000, T);
        long second = insertTx(-30000, T);

        db.deleteTransaction(second);

        assertBooksAgree("deleted the later of two at the same time");
        assertEquals(-50000L, accountTotal());
    }

    /** Control: distinct timestamps were always correct — this pins that the fix does not break them. */
    @Test
    public void transactionsAtDifferentTimesStayInSync() {
        long first = insertTx(-50000, T);
        insertTx(-30000, T + 60000);

        db.deleteTransaction(first);

        assertBooksAgree("deleted the earlier of two at different times");
        assertEquals(-30000L, accountTotal());
    }

    /** Inserting next to an existing row at the same time: the new row must not take its base from a neighbour that sorts after it. */
    @Test
    public void insertingAtTheSameTimeAsAnExistingOneKeepsBooksInSync() {
        insertTx(-50000, T);
        insertTx(-30000, T);
        insertTx(-20000, T);

        assertBooksAgree("third transaction at the same time");
        assertEquals(-100000L, accountTotal());
    }

    /**
     * Move an <em>older</em> transaction onto the timestamp of a <em>newer</em> one.
     *
     * <p>This covers the other half of the fix: computing the balance of the moved row means
     * taking the row that sorts immediately before it, which also needs the two-column
     * comparison. With {@code datetime} alone the same-timestamp neighbour that sorts after it
     * would be picked as the previous row — and having the smaller id is exactly the shape that
     * triggers it.
     */
    @Test
    public void movingAnOlderTransactionOntoALaterOnesTimeKeepsBooksInSync() {
        long older = insertTx(-50000, T);
        insertTx(-30000, T + 60000);

        Transaction moved = db.getTransaction(older);
        moved.dateTime = T + 60000;
        db.insertOrUpdate(moved);

        assertBooksAgree("moved the older transaction onto the newer one's timestamp");
        assertEquals(-80000L, accountTotal());
    }
}
