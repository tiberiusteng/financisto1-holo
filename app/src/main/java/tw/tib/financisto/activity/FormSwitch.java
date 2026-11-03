package tw.tib.financisto.activity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import tw.tib.financisto.model.Category;
import tw.tib.financisto.model.Transaction;
import tw.tib.financisto.model.TransactionStatus;

/**
 * Switching between the transaction, transfer and update balance forms.
 *
 * <p>The three forms are three views of the same entry, but each holds different fields: the
 * transaction form has no "to" account, the transfer form has no splits or original currency, and
 * the update balance form holds the new balance rather than the change. Filling the next form
 * straight from the previous one would drop whatever it cannot hold, and switching back would not
 * return to where it started.
 *
 * <p>So a switch carries a form-independent copy of the entry, the {@link Origin}:
 * <ul>
 *   <li>on the first switch, the values read from the form are normalised into an origin;</li>
 *   <li>each form shows only the part of the origin it can hold ({@link #project}); the origin
 *       keeps the rest;</li>
 *   <li>on the next switch, only the fields actually changed on this form are written back
 *       ({@link #merge}). "Changed" is measured against what the form showed once it finished
 *       loading, not against the origin, so adjustments a form makes on its own while loading
 *       (flipping income/expense by category when the amount is 0, setting the direction from the
 *       sign of the balance) are not taken as edits.</li>
 * </ul>
 * Switching without editing never writes to the origin, so going back to the starting form
 * rebuilds it from the origin, however many forms were visited.
 *
 * <p>Amounts are kept as an absolute value on the "main" account plus a direction: for a
 * transaction or an update balance the main account is its account; for a transfer it is the
 * "from" or the "to" side, depending on the direction. The change on the update balance form
 * (new balance - current balance) and the transaction amount convert into each other with integer
 * arithmetic only.
 */
public final class FormSwitch {

	public enum Kind { TRANSACTION, TRANSFER, BALANCE }

	/** Why a switch is not possible; {@code null} means it is. */
	public enum Block {
		/** The entry is split: a transfer cannot be split. */
		SPLIT_TO_TRANSFER
	}

	private FormSwitch() {
	}

	/** The form-independent shape of an entry. Fields only; serializable to travel in an Intent. */
	public static final class Origin implements Serializable {
		private static final long serialVersionUID = 1L;

		/** Main account (0 = none selected). */
		public long account;
		/** The other account of a transfer (0 = none). */
		public long other;
		/** Whether money flows into the main account; {@code null} = the form cannot tell (a zero difference on the update balance form). */
		public Boolean inflow;
		/** Amount on the main account, absolute, in the main account's minor units. */
		public long amount;
		/** Amount on the other account, absolute; equal to amount unless a transfer crosses currencies. */
		public long otherAmount;
		/** Original currency of a transaction (0 = the account's own) and the original amount (absolute). */
		public long originalCurrencyId;
		public long originalAmount;

		public long categoryId;
		public long projectId;
		public long payeeId;
		public long locationId;
		public String note;
		public String tags;
		public long dateTime;
		public TransactionStatus status = TransactionStatus.UR;
		public String attachedPicture;
		public String blobKey;
		public int isCCardPayment;
		public HashMap<Long, String> attributes;
		/** Split children as the form holds them (signed amounts, relative to the main account); {@code null} = not split. */
		public ArrayList<Transaction> splits;

		public Origin copy() {
			Origin o = new Origin();
			o.account = account;
			o.other = other;
			o.inflow = inflow;
			o.amount = amount;
			o.otherAmount = otherAmount;
			o.originalCurrencyId = originalCurrencyId;
			o.originalAmount = originalAmount;
			o.categoryId = categoryId;
			o.projectId = projectId;
			o.payeeId = payeeId;
			o.locationId = locationId;
			o.note = note;
			o.tags = tags;
			o.dateTime = dateTime;
			o.status = status;
			o.attachedPicture = attachedPicture;
			o.blobKey = blobKey;
			o.isCCardPayment = isCCardPayment;
			o.attributes = attributes != null ? new HashMap<>(attributes) : null;
			o.splits = copySplits(splits);
			return o;
		}

		public boolean hasSplits() {
			return splits != null && !splits.isEmpty();
		}

		/** Signed amount: positive when money flows into the main account. */
		public long signedAmount() {
			return Boolean.TRUE.equals(inflow) ? amount : -amount;
		}
	}

	/** Whether switching to {@code to} is possible. */
	public static Block check(Kind to, boolean hasSplits) {
		if (to == Kind.TRANSFER && hasSplits) return Block.SPLIT_TO_TRANSFER;
		return null;
	}

	/**
	 * The "current balance" for the update balance form: the account total minus what this
	 * transaction itself contributes to that account.
	 *
	 * <p>Saving an update balance stores new balance - current balance. A new transaction is not
	 * in the account total yet, so the current balance is the total. An existing one already is, and
	 * using the total would count it twice: a wallet at 700 including a -150 lunch, counted at 680,
	 * should turn the lunch into -170, not +20. For a transfer it depends on the side the account is
	 * on; an account the transaction does not touch keeps its total. Pass 0 for both accounts for a
	 * new transaction.
	 *
	 * @param fromAccount/toAccount/fromAmount/toAmount the transaction as stored, before saving
	 */
	public static long balanceExcluding(long accountTotal, long account,
										long fromAccount, long toAccount, long fromAmount, long toAmount) {
		if (account <= 0) return accountTotal;
		if (fromAccount == account) return accountTotal - fromAmount;
		if (toAccount == account) return accountTotal - toAmount;
		return accountTotal;
	}

	/**
	 * Values read from a form, normalised into an origin.
	 *
	 * @param t the form as it would be saved: a transaction has from/fromAmount (plus the original
	 *          currency, if any); a transfer from/to/fromAmount/toAmount; an update balance from and
	 *          fromAmount = the difference (new balance - current balance)
	 * @param incomeSelected the income/expense button of the transaction form, which carries the
	 *                       direction while the amount is 0
	 * @param transferInflow which side of a transfer is the main account: {@code true} = the "to"
	 *                       side. {@code null} infers it from the form (only a "to" account filled
	 *                       in = the "to" side). Two reads of the same form must use the same value.
	 */
	public static Origin canonical(Kind kind, Transaction t, boolean incomeSelected, Boolean transferInflow) {
		Origin o = new Origin();
		o.categoryId = t.categoryId;
		o.projectId = t.projectId;
		o.payeeId = t.payeeId;
		o.locationId = t.locationId;
		o.note = t.note;
		o.tags = t.tags;
		o.dateTime = t.dateTime;
		o.status = t.status;
		o.attachedPicture = t.attachedPicture;
		o.blobKey = t.blobKey;
		o.isCCardPayment = t.isCCardPayment;
		o.attributes = t.categoryAttributes != null ? new HashMap<>(t.categoryAttributes) : null;
		switch (kind) {
			case TRANSFER: {
				boolean in = transferInflow != null ? transferInflow
						: (t.fromAccountId <= 0 && t.toAccountId > 0);
				if (in) {
					o.account = positive(t.toAccountId);
					o.other = positive(t.fromAccountId);
					o.amount = Math.abs(t.toAmount);
					o.otherAmount = Math.abs(t.fromAmount);
				} else {
					o.account = positive(t.fromAccountId);
					o.other = positive(t.toAccountId);
					o.amount = Math.abs(t.fromAmount);
					o.otherAmount = Math.abs(t.toAmount);
				}
				o.inflow = in;
				break;
			}
			case TRANSACTION:
			case BALANCE:
			default: {
				o.account = positive(t.fromAccountId);
				o.amount = Math.abs(t.fromAmount);
				if (t.fromAmount > 0) {
					o.inflow = Boolean.TRUE;
				} else if (t.fromAmount < 0) {
					o.inflow = Boolean.FALSE;
				} else {
					o.inflow = kind == Kind.TRANSACTION ? Boolean.valueOf(incomeSelected) : null;
				}
				if (kind == Kind.TRANSACTION && t.originalCurrencyId > 0) {
					o.originalCurrencyId = t.originalCurrencyId;
					o.originalAmount = Math.abs(t.originalFromAmount);
				}
				o.splits = t.splits != null && !t.splits.isEmpty() ? copySplits(t.splits) : null;
				break;
			}
		}
		return o;
	}

	/**
	 * Writes the fields changed on this form back into the origin. {@code base} is what the form
	 * showed once loaded and {@code now} what it shows now, both normalised by {@link #canonical}
	 * with the same direction. Fields equal in both keep the origin's value.
	 */
	public static Origin merge(Origin origin, Origin base, Origin now) {
		Origin r = origin.copy();
		if (base.account != now.account) r.account = now.account;
		if (base.other != now.other) r.other = now.other;
		if (!Objects.equals(base.inflow, now.inflow) && now.inflow != null) r.inflow = now.inflow;
		if (base.amount != now.amount) {
			// in one currency the other side is the same number and follows the main amount;
			// across currencies the two are independent and the other one is kept unless edited
			if (origin.otherAmount == origin.amount) r.otherAmount = now.amount;
			r.amount = now.amount;
		}
		if (base.otherAmount != now.otherAmount) r.otherAmount = now.otherAmount;
		if (base.originalCurrencyId != now.originalCurrencyId) r.originalCurrencyId = now.originalCurrencyId;
		if (base.originalAmount != now.originalAmount) r.originalAmount = now.originalAmount;
		if (base.categoryId != now.categoryId) r.categoryId = now.categoryId;
		if (base.projectId != now.projectId) r.projectId = now.projectId;
		if (base.payeeId != now.payeeId) r.payeeId = now.payeeId;
		if (base.locationId != now.locationId) r.locationId = now.locationId;
		if (!Objects.equals(base.note, now.note)) r.note = now.note;
		if (!Objects.equals(base.tags, now.tags)) r.tags = now.tags;
		if (base.dateTime != now.dateTime) r.dateTime = now.dateTime;
		if (base.status != now.status) r.status = now.status;
		if (!Objects.equals(base.attachedPicture, now.attachedPicture)) {
			r.attachedPicture = now.attachedPicture;
			r.blobKey = now.blobKey;
		}
		if (base.isCCardPayment != now.isCCardPayment) r.isCCardPayment = now.isCCardPayment;
		if (!Objects.equals(base.attributes, now.attributes)) {
			r.attributes = now.attributes != null ? new HashMap<>(now.attributes) : null;
		}
		if (!splitsEqual(base.splits, now.splits)) r.splits = copySplits(now.splits);
		return r;
	}

	/**
	 * Origin to the transaction the target form loads (same shape as the input of
	 * {@link #canonical}; the id is always -1).
	 *
	 * @param currentBalance for the update balance form, the main account's current balance; the
	 *                       amount field shows current balance + change
	 */
	public static Transaction project(Origin o, Kind kind, long currentBalance) {
		Transaction t = new Transaction();
		t.id = -1;
		t.categoryId = o.categoryId;
		t.projectId = o.projectId;
		t.payeeId = o.payeeId;
		t.locationId = o.locationId;
		t.note = o.note;
		t.tags = o.tags;
		t.dateTime = o.dateTime;
		t.status = o.status;
		t.attachedPicture = o.attachedPicture;
		t.blobKey = o.blobKey;
		t.isCCardPayment = o.isCCardPayment;
		t.categoryAttributes = o.attributes != null ? new HashMap<>(o.attributes) : null;
		switch (kind) {
			case TRANSFER: {
				boolean in = Boolean.TRUE.equals(o.inflow);
				long main = o.amount;
				long other = o.otherAmount > 0 ? o.otherAmount : o.amount;
				if (in) {
					t.fromAccountId = o.other;
					t.toAccountId = o.account;
					t.fromAmount = -other;
					t.toAmount = main;
				} else {
					t.fromAccountId = o.account;
					t.toAccountId = o.other;
					t.fromAmount = -main;
					t.toAmount = other;
				}
				// "Split" can be selected without any parts yet. The transfer form's category list has
				// no split entry, so it shows no category; untouched, the origin still says split and
				// switching back restores it.
				if (t.categoryId == Category.SPLIT_CATEGORY_ID) t.categoryId = Category.NO_CATEGORY_ID;
				break;
			}
			case BALANCE:
				t.fromAccountId = o.account;
				t.fromAmount = currentBalance + o.signedAmount();
				t.splits = copySplits(o.splits);
				break;
			case TRANSACTION:
			default:
				t.fromAccountId = o.account;
				t.fromAmount = o.signedAmount();
				if (o.originalCurrencyId > 0) {
					t.originalCurrencyId = o.originalCurrencyId;
					t.originalFromAmount = Boolean.TRUE.equals(o.inflow) ? o.originalAmount : -o.originalAmount;
				}
				t.splits = copySplits(o.splits);
				break;
		}
		return t;
	}

	static ArrayList<Transaction> copySplits(List<Transaction> splits) {
		if (splits == null) return null;
		ArrayList<Transaction> copy = new ArrayList<>(splits.size());
		for (Transaction s : splits) {
			Transaction c = s.clone();
			c.categoryAttributes = s.categoryAttributes != null ? new HashMap<>(s.categoryAttributes) : null;
			copy.add(c);
		}
		return copy;
	}

	/** Same split parts, ignoring ids (the form renumbers its temporary ids on every load). */
	static boolean splitsEqual(List<Transaction> a, List<Transaction> b) {
		boolean aEmpty = a == null || a.isEmpty();
		boolean bEmpty = b == null || b.isEmpty();
		if (aEmpty || bEmpty) return aEmpty == bEmpty;
		if (a.size() != b.size()) return false;
		for (int i = 0; i < a.size(); i++) {
			if (!splitSignature(a.get(i)).equals(splitSignature(b.get(i)))) return false;
		}
		return true;
	}

	private static String splitSignature(Transaction s) {
		Map<Long, String> attrs = s.categoryAttributes;
		return s.fromAccountId + "|" + s.toAccountId + "|" + s.fromAmount + "|" + s.toAmount
				+ "|" + s.categoryId + "|" + s.projectId + "|" + s.payeeId + "|" + s.locationId
				+ "|" + s.originalCurrencyId + "|" + s.originalFromAmount
				+ "|" + s.note + "|" + s.tags + "|" + s.status
				+ "|" + (attrs != null && !attrs.isEmpty() ? attrs : "");
	}

	private static long positive(long id) {
		return id > 0 ? id : 0;
	}
}
