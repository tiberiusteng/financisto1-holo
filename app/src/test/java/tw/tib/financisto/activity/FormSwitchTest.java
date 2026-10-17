package tw.tib.financisto.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import tw.tib.financisto.activity.FormSwitch.Block;
import tw.tib.financisto.activity.FormSwitch.Kind;
import tw.tib.financisto.activity.FormSwitch.Origin;
import tw.tib.financisto.model.Category;
import tw.tib.financisto.model.Transaction;
import tw.tib.financisto.model.TransactionStatus;

/**
 * Switching between the three forms: switching without editing must always return to where it
 * started; a field edited on the way is kept, and everything else stays as it was.
 *
 * <p>{@link Form} stands in for a form: it loads what {@link FormSwitch#project} returns and reads
 * back the values as they are once loaded. Like the real forms, the update balance form reads back
 * the difference (new balance shown - current balance), and the transfer form cannot show splits or
 * an original currency.
 */
public class FormSwitchTest {

	private static final long BALANCE = 100_000;   // current balance of the main account (update balance form)

	// ---------------------------------------------------------------- starting states

	private static Transaction common(Transaction t) {
		t.categoryId = 5;
		t.projectId = 6;
		t.payeeId = 7;
		t.locationId = 8;
		t.note = "lunch";
		t.tags = "\ntrip\n";
		t.dateTime = 1_790_000_000_000L;
		t.status = TransactionStatus.PN;
		t.attachedPicture = "pic.jpg";
		t.isCCardPayment = 1;
		HashMap<Long, String> attrs = new HashMap<>();
		attrs.put(3L, "x");
		t.categoryAttributes = attrs;
		return t;
	}

	private static Transaction expense() {
		Transaction t = common(new Transaction());
		t.fromAccountId = 1;
		t.fromAmount = -15_000;
		return t;
	}

	private static Transaction income() {
		Transaction t = common(new Transaction());
		t.fromAccountId = 1;
		t.fromAmount = 20_000;
		return t;
	}

	private static Transaction foreignCurrency() {
		Transaction t = common(new Transaction());
		t.fromAccountId = 1;
		t.fromAmount = -3_200;
		t.originalCurrencyId = 9;
		t.originalFromAmount = -1_000;
		return t;
	}

	private static Transaction split(long id, long from, long to, long amount, long category, String note) {
		Transaction s = new Transaction();
		s.id = id;
		s.fromAccountId = from;
		s.toAccountId = to;
		s.fromAmount = amount;
		s.toAmount = to > 0 ? -amount : 0;
		s.categoryId = category;
		s.note = note;
		return s;
	}

	private static Transaction withSplits() {
		Transaction t = expense();
		t.categoryId = Category.SPLIT_CATEGORY_ID;
		t.splits = new ArrayList<>(Arrays.asList(
				split(-1, 1, 0, -10_000, 2, "groceries"),
				split(-2, 1, 4, -5_000, 0, "paid for a friend")));
		return t;
	}

	private static Transaction transfer(long from, long to, long fromAmount, long toAmount) {
		Transaction t = common(new Transaction());
		t.fromAccountId = from;
		t.toAccountId = to;
		t.fromAmount = fromAmount;
		t.toAmount = toAmount;
		return t;
	}

	// ---------------------------------------------------------------- a simulated form

	/** An open form: the values as loaded (base) and as they are now (now). */
	private static final class Form {
		final Kind kind;
		final Origin origin;       // null = not opened by a switch
		final Transaction base;
		Transaction now;
		boolean incomeSelected;
		boolean baseIncome;
		final Boolean transferInflow;

		Form(Kind kind, Transaction loaded, boolean incomeSelected, Origin origin) {
			this.kind = kind;
			this.origin = origin;
			this.base = loaded;
			this.now = copy(loaded);
			this.incomeSelected = incomeSelected;
			this.transferInflow = origin != null ? Boolean.TRUE.equals(origin.inflow) : null;
		}

		/** The form the user opened first (not by a switch). */
		static Form open(Kind kind, Transaction t, boolean incomeSelected) {
			return new Form(kind, copy(t), incomeSelected, null);
		}

		Origin currentOrigin() {
			Origin now = FormSwitch.canonical(kind, this.now, incomeSelected, transferInflow);
			if (origin == null) return now;
			Origin base = FormSwitch.canonical(kind, this.base, baseIncome, transferInflow);
			return FormSwitch.merge(origin, base, now);
		}

		Form switchTo(Kind target) {
			return load(currentOrigin(), target);
		}

		/** Opens a target form with an origin. */
		static Form load(Origin o, Kind target) {
			Transaction p = FormSwitch.project(o, target, BALANCE);
			Transaction loaded = copy(p);
			if (target == Kind.BALANCE) {
				loaded.fromAmount = p.fromAmount - BALANCE;   // the form reads back the difference
			}
			// transaction form: with a zero amount the origin sets the direction (the real form sets
			// the income/expense button after loading)
			boolean income = target == Kind.TRANSACTION
					? (loaded.fromAmount != 0 ? loaded.fromAmount > 0 : Boolean.TRUE.equals(o.inflow))
					: false;
			Form f = new Form(target, loaded, income, o);
			f.baseIncome = income;
			return f;
		}
	}

	private static Transaction copy(Transaction t) {
		Transaction c = t.clone();
		c.categoryAttributes = t.categoryAttributes != null ? new HashMap<>(t.categoryAttributes) : null;
		c.splits = FormSwitch.copySplits(t.splits);
		return c;
	}

	private static Form walk(Form start, Kind... path) {
		Form f = start;
		for (Kind k : path) f = f.switchTo(k);
		return f;
	}

	private static void assertSameAsStart(Form start, Form end) {
		assertEquals(start.kind, end.kind);
		Transaction a = start.now;
		Transaction b = end.now;
		assertEquals("from", a.fromAccountId, b.fromAccountId);
		assertEquals("to", a.toAccountId, b.toAccountId);
		assertEquals("fromAmount", a.fromAmount, b.fromAmount);
		if (start.kind == Kind.TRANSFER) assertEquals("toAmount", a.toAmount, b.toAmount);
		assertEquals("originalCurrency", a.originalCurrencyId, b.originalCurrencyId);
		assertEquals("originalAmount", a.originalFromAmount, b.originalFromAmount);
		assertEquals("category", a.categoryId, b.categoryId);
		assertEquals("project", a.projectId, b.projectId);
		assertEquals("payee", a.payeeId, b.payeeId);
		assertEquals("location", a.locationId, b.locationId);
		assertEquals("note", a.note, b.note);
		assertEquals("tags", a.tags, b.tags);
		assertEquals("dateTime", a.dateTime, b.dateTime);
		assertEquals("status", a.status, b.status);
		assertEquals("picture", a.attachedPicture, b.attachedPicture);
		assertEquals("ccard", a.isCCardPayment, b.isCCardPayment);
		assertEquals("attributes", a.categoryAttributes, b.categoryAttributes);
		assertTrue("splits", FormSwitch.splitsEqual(a.splits, b.splits));
		if (start.kind == Kind.TRANSACTION) assertEquals("income toggle", start.incomeSelected, end.incomeSelected);
	}

	/** Every switch-only path from start back to start ends up identical to start. */
	private static void assertAllRoundTrips(Form start, boolean hasSplits) {
		List<Kind> others = new ArrayList<>(Arrays.asList(Kind.values()));
		others.remove(start.kind);
		List<List<Kind>> paths = new ArrayList<>();
		for (Kind a : others) {
			paths.add(Arrays.asList(a, start.kind));
			for (Kind b : Kind.values()) {
				if (b == a) continue;
				paths.add(b == start.kind ? Arrays.asList(a, b) : Arrays.asList(a, b, start.kind));
			}
		}
		paths.add(Arrays.asList(others.get(0), others.get(1), others.get(0), start.kind));
		for (List<Kind> path : paths) {
			if (hasSplits && path.contains(Kind.TRANSFER)) continue;     // a split cannot become a transfer
			Form end = walk(start, path.toArray(new Kind[0]));
			try {
				assertSameAsStart(start, end);
			} catch (AssertionError e) {
				throw new AssertionError(start.kind + " -> " + path + ": " + e.getMessage(), e);
			}
		}
	}

	// ---------------------------------------------------------------- switching only: back to the start

	@Test
	public void expenseRoundTrips() {
		assertAllRoundTrips(Form.open(Kind.TRANSACTION, expense(), false), false);
	}

	@Test
	public void incomeRoundTrips() {
		assertAllRoundTrips(Form.open(Kind.TRANSACTION, income(), true), false);
	}

	@Test
	public void zeroAmountIncomeKeepsItsDirection() {
		Transaction t = income();
		t.fromAmount = 0;
		assertAllRoundTrips(Form.open(Kind.TRANSACTION, t, true), false);
	}

	@Test
	public void foreignCurrencyRoundTrips() {
		assertAllRoundTrips(Form.open(Kind.TRANSACTION, foreignCurrency(), false), false);
	}

	@Test
	public void splitTransactionRoundTripsThroughBalance() {
		assertAllRoundTrips(Form.open(Kind.TRANSACTION, withSplits(), false), true);
	}

	@Test
	public void sameCurrencyTransferRoundTrips() {
		assertAllRoundTrips(Form.open(Kind.TRANSFER, transfer(1, 2, -5_000, 5_000), false), false);
	}

	@Test
	public void crossCurrencyTransferRoundTrips() {
		assertAllRoundTrips(Form.open(Kind.TRANSFER, transfer(1, 3, -3_000, 100), false), false);
	}

	@Test
	public void transferWithoutDestinationRoundTrips() {
		assertAllRoundTrips(Form.open(Kind.TRANSFER, transfer(1, 0, -700, 700), false), false);
	}

	@Test
	public void transferWithOnlyDestinationRoundTrips() {
		assertAllRoundTrips(Form.open(Kind.TRANSFER, transfer(0, 2, -800, 800), false), false);
	}

	@Test
	public void balanceWithSplitsRoundTrips() {
		assertAllRoundTrips(Form.open(Kind.BALANCE, withSplits(), false), true);
	}

	@Test
	public void zeroDifferenceBalanceRoundTrips() {
		Transaction t = expense();
		t.fromAmount = 0;
		assertAllRoundTrips(Form.open(Kind.BALANCE, t, false), false);
	}

	@Test
	public void noAccountRoundTrips() {
		Transaction t = expense();
		t.fromAccountId = 0;
		assertAllRoundTrips(Form.open(Kind.TRANSACTION, t, false), false);
	}

	// ---------------------------------------------------------------- mapping between the forms

	@Test
	public void expenseBecomesTransferOutOfTheAccount() {
		Form x = Form.open(Kind.TRANSACTION, expense(), false).switchTo(Kind.TRANSFER);
		assertEquals(1, x.now.fromAccountId);
		assertEquals(0, x.now.toAccountId);
		assertEquals(-15_000, x.now.fromAmount);
		assertEquals(15_000, x.now.toAmount);
	}

	@Test
	public void incomeBecomesTransferIntoTheAccount() {
		Form x = Form.open(Kind.TRANSACTION, income(), true).switchTo(Kind.TRANSFER);
		assertEquals(0, x.now.fromAccountId);
		assertEquals(1, x.now.toAccountId);
		assertEquals(-20_000, x.now.fromAmount);
		assertEquals(20_000, x.now.toAmount);
	}

	@Test
	public void transactionAmountBecomesBalanceDifference() {
		Transaction p = FormSwitch.project(
				FormSwitch.canonical(Kind.TRANSACTION, expense(), false, null), Kind.BALANCE, BALANCE);
		assertEquals(BALANCE - 15_000, p.fromAmount);   // the new balance the form shows
	}

	@Test
	public void balanceDifferenceBecomesTransactionAmount() {
		Transaction b = expense();
		b.fromAmount = 2_500;                            // difference +2500
		Transaction p = FormSwitch.project(
				FormSwitch.canonical(Kind.BALANCE, b, false, null), Kind.TRANSACTION, 0);
		assertEquals(2_500, p.fromAmount);
	}

	@Test
	public void transferIntoAccountBecomesIncome() {
		Form t = Form.open(Kind.TRANSFER, transfer(0, 2, -800, 800), false).switchTo(Kind.TRANSACTION);
		assertEquals(2, t.now.fromAccountId);
		assertEquals(800, t.now.fromAmount);
	}

	@Test
	public void splitCategoryWithoutSplitsShowsAsNoCategoryOnTransfer() {
		Transaction t = expense();
		t.categoryId = Category.SPLIT_CATEGORY_ID;
		Form x = Form.open(Kind.TRANSACTION, t, false).switchTo(Kind.TRANSFER);
		assertEquals(Category.NO_CATEGORY_ID, x.now.categoryId);
		assertEquals(Category.SPLIT_CATEGORY_ID, x.switchTo(Kind.TRANSACTION).now.categoryId);
	}

	// ---------------------------------------------------------------- edited on the way: kept, the rest unchanged

	@Test
	public void editedAmountOnTransactionKeepsTransferDestination() {
		Form t = Form.open(Kind.TRANSFER, transfer(1, 2, -5_000, 5_000), false).switchTo(Kind.TRANSACTION);
		t.now.fromAmount = -6_000;
		Form x = t.switchTo(Kind.TRANSFER);
		assertEquals(1, x.now.fromAccountId);
		assertEquals(2, x.now.toAccountId);
		assertEquals(-6_000, x.now.fromAmount);
		assertEquals(6_000, x.now.toAmount);
	}

	@Test
	public void editedNoteKeepsCrossCurrencyAmounts() {
		Form t = Form.open(Kind.TRANSFER, transfer(1, 3, -3_000, 100), false).switchTo(Kind.TRANSACTION);
		t.now.note = "edited";
		Form x = t.switchTo(Kind.TRANSFER);
		assertEquals(-3_000, x.now.fromAmount);
		assertEquals(100, x.now.toAmount);
		assertEquals("edited", x.now.note);
	}

	@Test
	public void editedNewBalanceChangesTheAmount() {
		Form b = Form.open(Kind.TRANSACTION, expense(), false).switchTo(Kind.BALANCE);
		b.now.fromAmount = -20_000;                     // new balance 80000 -> difference -20000
		Form t = b.switchTo(Kind.TRANSACTION);
		assertEquals(-20_000, t.now.fromAmount);
		assertEquals("lunch", t.now.note);
	}

	@Test
	public void formAutoAdjustingDirectionIsNotAnEdit() {
		// a zero-amount income: after loading, the transaction form flips the button to expense by
		// category (the form did it, not the user)
		Transaction t = income();
		t.fromAmount = 0;
		Form x = Form.open(Kind.TRANSACTION, t, true).switchTo(Kind.TRANSFER);
		Form back = x.switchTo(Kind.TRANSACTION);
		back.baseIncome = false;
		back.incomeSelected = false;
		Form again = back.switchTo(Kind.TRANSFER).switchTo(Kind.TRANSACTION);
		assertTrue(again.incomeSelected);
	}

	@Test
	public void userFlippingDirectionIsAnEdit() {
		Form t = Form.open(Kind.TRANSACTION, expense(), false).switchTo(Kind.BALANCE).switchTo(Kind.TRANSACTION);
		t.now.fromAmount = 15_000;
		t.incomeSelected = true;
		Form x = t.switchTo(Kind.TRANSFER);
		assertEquals(1, x.now.toAccountId);
	}

	@Test
	public void changingOnlyASplitStatusIsAnEdit() {
		// split parts have their own status: changing only one part's status survives a switch
		Form b = Form.open(Kind.TRANSACTION, withSplits(), false).switchTo(Kind.BALANCE);
		b.now.splits.get(0).status = TransactionStatus.CL;
		Form t = b.switchTo(Kind.TRANSACTION);
		assertEquals(TransactionStatus.CL, t.now.splits.get(0).status);
	}

	@Test
	public void editedSplitsReplaceTheCarriedOnes() {
		Form b = Form.open(Kind.TRANSACTION, withSplits(), false).switchTo(Kind.BALANCE);
		b.now.splits = new ArrayList<>(Collections.singletonList(split(-9, 1, 0, -15_000, 2, "all groceries")));
		Form t = b.switchTo(Kind.TRANSACTION);
		assertEquals(1, t.now.splits.size());
		assertEquals("all groceries", t.now.splits.get(0).note);
	}

	// ---------------------------------------------------------------- blocked switches

	@Test
	public void blocks() {
		assertEquals(Block.SPLIT_TO_TRANSFER, FormSwitch.check(Kind.TRANSFER, true));
		assertNull(FormSwitch.check(Kind.BALANCE, true));
		assertNull(FormSwitch.check(Kind.TRANSFER, false));
		assertNull(FormSwitch.check(Kind.TRANSACTION, true));
	}

	// ---------------------------------------------------------------- existing transaction to update balance: excluding itself

	@Test
	public void newTransactionUsesTheAccountTotal() {
		assertEquals(700, FormSwitch.balanceExcluding(700, 1, 0, 0, 0, 0));
	}

	@Test
	public void existingExpenseIsTakenOutOfTheBalance() {
		// wallet total 700 including a -150 lunch: current balance 850; the form shows 850 - 150 = 700 (as it is)
		long base = FormSwitch.balanceExcluding(700, 1, 1, 0, -150, 0);
		assertEquals(850, base);
		Origin o = FormSwitch.canonical(Kind.TRANSACTION, lunch(-150), false, null);
		assertEquals(700, FormSwitch.project(o, Kind.BALANCE, base).fromAmount);
		// counted 680 -> this transaction = 680 - 850 = -170 (using the total would give +20)
		assertEquals(-170, 680 - base);
	}

	@Test
	public void existingTransferUsesTheSideOfThatAccount() {
		// transfer A -> B 500: on B (the "to" side) the "to" amount is taken out; an unrelated account keeps its total
		assertEquals(1_000 - 500, FormSwitch.balanceExcluding(1_000, 2, 1, 2, -500, 500));
		assertEquals(300 + 500, FormSwitch.balanceExcluding(300, 1, 1, 2, -500, 500));
		assertEquals(42, FormSwitch.balanceExcluding(42, 3, 1, 2, -500, 500));
	}

	private static Transaction lunch(long amount) {
		Transaction t = new Transaction();
		t.fromAccountId = 1;
		t.fromAmount = amount;
		return t;
	}
}
