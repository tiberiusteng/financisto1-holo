/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package tw.tib.financisto.activity;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.*;
import tw.tib.financisto.R;
import tw.tib.financisto.datetime.DateUtils;
import tw.tib.financisto.datetime.Period;
import tw.tib.financisto.blotter.BlotterFilter;
import tw.tib.financisto.filter.Criteria;
import tw.tib.financisto.filter.DateTimeCriteria;
import tw.tib.financisto.filter.WhereFilter;
import tw.tib.financisto.model.Account;
import tw.tib.financisto.model.Currency;
import tw.tib.financisto.model.MultiChoiceItem;
import tw.tib.financisto.model.TransactionStatus;
import tw.tib.financisto.utils.EnumUtils;
import tw.tib.financisto.utils.TransactionUtils;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import static tw.tib.financisto.blotter.BlotterFilter.FROM_ACCOUNT_ID;

public class BlotterFilterActivity extends FilterAbstractActivity {

	public static final String IS_ACCOUNT_FILTER = "IS_ACCOUNT_FILTER";
	public static final String IS_PLANNER_FILTER = "IS_PLANNER_FILTER";
	private static final TransactionStatus[] statuses = TransactionStatus.values();

	private static final int REQUEST_DATE_FILTER = 1;
	private static final int REQUEST_NOTE_FILTER = 2;

	private TextView period;
	private TextView account;
	private TextView currency;
	private TextView note;
	private TextView sortOrder;
	private TextView status;

	private DateFormat df;
	private String[] sortBlotterEntries;

	private long accountId;
	private boolean isAccountFilter;
	private boolean isPlannerFilter;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.blotter_filter);

		Intent intent = getIntent();
		if (intent != null) {
			isPlannerFilter = intent.getBooleanExtra(IS_PLANNER_FILTER, false);
		}

		df = DateUtils.getShortDateFormat(this);
		sortBlotterEntries = getResources().getStringArray(R.array.sort_blotter_entries);
		noFilterValue = getString(R.string.no_filter);

		LinearLayout layout = findViewById(R.id.layout);
		period = x.addFilterNodeMinus(layout, R.id.period, R.id.period_clear, R.string.period, R.string.no_filter);
		account = x.addFilterNodeMinus(layout, R.id.account, R.id.account_clear, R.string.account, R.string.no_filter);
		currency = x.addFilterNodeMinus(layout, R.id.currency, R.id.currency_clear, R.string.currency, R.string.no_filter);
		initCategorySelector(layout);
		initPayeeSelector(layout);
		initProjectSelector(layout);
		initLocationSelector(layout);
		note = x.addFilterNodeMinus(layout, R.id.note, R.id.note_clear, R.string.note, R.string.no_filter);
		status = x.addFilterNodeMinus(layout, R.id.status, R.id.status_clear, R.string.transaction_status, R.string.no_filter);
		if (!isPlannerFilter) {
			sortOrder = x.addFilterNodeMinus(layout, R.id.sort_order, R.id.sort_order_clear, R.string.sort_order, 0, sortBlotterEntries[0]);
		}

		Button bOk = findViewById(R.id.bOK);
		bOk.setOnClickListener(v -> {
			Intent data = new Intent();
			filter.toIntent(data);
			setResult(RESULT_OK, data);
			finish();
		});

		Button bCancel = findViewById(R.id.bCancel);
		bCancel.setOnClickListener(v -> {
			setResult(RESULT_CANCELED);
			finish();
		});

		ImageButton bNoFilter = findViewById(R.id.bNoFilter);
		bNoFilter.setOnClickListener(v -> {
			if (isAccountFilter()) {
				Intent data = new Intent();
				Criteria.eq(FROM_ACCOUNT_ID, String.valueOf(accountId)).toIntent(filter.getTitle(), data);
				setResult(RESULT_OK, data);
				finish();
			} else {
				setResult(RESULT_FIRST_USER);
				finish();
			}
		});

		if (intent != null) {
			filter = WhereFilter.fromIntent(intent);
			getAccountIdFromFilter(intent);
			updatePeriodFromFilter();
			updateAccountFromFilter();
			updateCurrencyFromFilter();
			updateCategoryFromFilter();
			updateProjectFromFilter();
			updatePayeeFromFilter();
			updateNoteFromFilter();
			updateLocationFromFilter();
			updateSortOrderFromFilter();
			updateStatusFromFilter();
			disableAccountResetButtonIfNeeded();
		}
	}

	private boolean isAccountFilter() {
		return isAccountFilter && accountId > 0;
	}

	private void getAccountIdFromFilter(Intent intent) {
		isAccountFilter = intent.getBooleanExtra(IS_ACCOUNT_FILTER, false);
		accountId = filter.getAccountId();
	}

	private void disableAccountResetButtonIfNeeded() {
		if (isAccountFilter()) {
			hideMinusButton(account);
		}
	}

	private void updateSortOrderFromFilter() {
		if (sortOrder == null) return;
		String s = filter.getSortOrder();
		if (BlotterFilter.SORT_OLDER_TO_NEWER.equals(s)) {
			sortOrder.setText(sortBlotterEntries[1]);
		} else {
			sortOrder.setText(sortBlotterEntries[0]);
		}
	}

	private void updatePeriodFromFilter() {
		DateTimeCriteria c = (DateTimeCriteria)filter.get(BlotterFilter.DATETIME);
		if (c != null) {
			Period p = c.getPeriod();
			if (p.isCustom()) {
				long periodFrom = c.getLongValue1();
				long periodTo = c.getLongValue2();
				period.setText(df.format(new Date(periodFrom))+"-"+df.format(new Date(periodTo)));
			} else {
				period.setText(p.type.titleId);
			}
			if (!isPlannerFilter) {
				showMinusButton(period);
			}
		} else {
			if (!isPlannerFilter) {
				clear(BlotterFilter.DATETIME, period);
			}
		}
	}

	private void updateAccountFromFilter() {
		updateEntityFromFilter(FROM_ACCOUNT_ID, Account.class, account);
	}

	private void updateCurrencyFromFilter() {
		updateEntityFromFilter(BlotterFilter.FROM_ACCOUNT_CURRENCY_ID, Currency.class, currency);
	}

	private void updateNoteFromFilter() {
		Criteria c = filter.get(BlotterFilter.NOTE);
		if (c != null) {
			String v = c.getStringValue();
			note.setText(String.format(getString(R.string.note_text_containing_value),
					v.substring(1, v.length() - 1).replace("%", " ")));
			showMinusButton(note);
		} else {
			note.setText(R.string.no_filter);
			hideMinusButton(note);
		}
	}

	private void updateStatusFromFilter() {
		Criteria c = filter.get(BlotterFilter.STATUS);
		if (c != null) {
			var statusTitle = new ArrayList<String>();
			for (String state : c.getValues()) {
				statusTitle.add(getString(TransactionStatus.valueOf(state).titleId));
			}
			status.setText(String.join(", ", statusTitle));
			showMinusButton(status);
		} else {
			status.setText(R.string.no_filter);
			hideMinusButton(status);
		}
	}

	@Override
	protected void onClick(View v, int id) {
		super.onClick(v, id);
		Intent intent;
		if (id == R.id.period) {
			intent = new Intent(this, DateFilterActivity.class);
			filter.toIntent(intent);
			if (isPlannerFilter) {
				intent.putExtra(DateFilterActivity.EXTRA_FILTER_DONT_SHOW_NO_FILTER, true);
				intent.putExtra(DateFilterActivity.EXTRA_FILTER_SHOW_PLANNER, true);
			}
			startActivityForResult(intent, REQUEST_DATE_FILTER);
		} else if (id == R.id.period_clear) {
			clear(BlotterFilter.DATETIME, period);
		} else if (id == R.id.account) {
			if (isAccountFilter()) {
				return;
			}
			Cursor cursor = db.getAllAccounts();
			startManagingCursor(cursor);
			ListAdapter adapter = TransactionUtils.createAccountAdapter(this, cursor);
			Criteria c = filter.get(FROM_ACCOUNT_ID);
			long selectedId = c != null ? c.getLongValue1() : -1;
			x.select(this, R.id.account, R.string.account, cursor, adapter, "_id", selectedId);
		} else if (id == R.id.account_clear) {
			if (isAccountFilter()) {
				return;
			}
			clear(FROM_ACCOUNT_ID, account);
		} else if (id == R.id.currency) {
			Cursor cursor = db.getAllCurrencies("name");
			startManagingCursor(cursor);
			ListAdapter adapter = TransactionUtils.createCurrencyAdapter(this, cursor);
			Criteria c = filter.get(BlotterFilter.FROM_ACCOUNT_CURRENCY_ID);
			long selectedId = c != null ? c.getLongValue1() : -1;
			x.select(this, R.id.currency, R.string.currency, cursor, adapter, "_id", selectedId);
		} else if (id == R.id.currency_clear) {
			clear(BlotterFilter.FROM_ACCOUNT_CURRENCY_ID, currency);
		} else if (id ==  R.id.note) {
			intent = new Intent(this, NoteFilterActivity.class);
			filter.toIntent(intent);
			startActivityForResult(intent, REQUEST_NOTE_FILTER);
		} else if (id == R.id.note_clear) {
			clear(BlotterFilter.NOTE, note);
		} else if (id == R.id.sort_order) {
			ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sortBlotterEntries);
			int selectedId = BlotterFilter.SORT_OLDER_TO_NEWER.equals(filter.getSortOrder()) ? 1 : 0;
			x.selectPosition(this, R.id.sort_order, R.string.sort_order, adapter, selectedId);
		} else if (id == R.id.sort_order_clear) {
			filter.resetSort();
			filter.desc(BlotterFilter.DATETIME);
			updateSortOrderFromFilter();
		} else if (id == R.id.status) {
			var items = new ArrayList<TransactionStatusMultiChoiceItem>();
			var selected = new HashSet<String>();
			Criteria c = filter.get(BlotterFilter.STATUS);
			if (c != null) {
				selected.addAll(Arrays.asList(c.getValues()));
			}
			for (var status : statuses) {
				var item = new TransactionStatusMultiChoiceItem(status, getString(status.getTitleId()));
				if (selected.contains(status.name())) {
					item.setChecked(true);
				}
				items.add(item);
			}
			x.selectMultiChoice(this, R.id.status, R.string.transaction_status, items);
		} else if (id == R.id.status_clear) {
			clear(BlotterFilter.STATUS, status);
		}
	}

	@Override
	public void onSelectedId(final int id, final long selectedId) {
		super.onSelectedId(id, selectedId);
		if (id == R.id.account) {
			filter.put(Criteria.eq(FROM_ACCOUNT_ID, String.valueOf(selectedId)));
			updateAccountFromFilter();
		} else if (id == R.id.currency) {
			filter.put(Criteria.or(
					Criteria.eq(BlotterFilter.FROM_ACCOUNT_CURRENCY_ID, String.valueOf(selectedId)),
					Criteria.eq(BlotterFilter.ORIGINAL_CURRENCY_ID, String.valueOf(selectedId))
			));
			updateCurrencyFromFilter();
		}
	}

	@Override
	public void onSelectedPos(int id, int selectedPos) {
		super.onSelectedPos(id, selectedPos);
		if (id == R.id.sort_order) {
			filter.resetSort();
			if (selectedPos == 1) {
				filter.asc(BlotterFilter.DATETIME);
			} else {
				filter.desc(BlotterFilter.DATETIME);
			}
			updateSortOrderFromFilter();
		}
	}

	@Override
	public void onSelected(int id, List<? extends MultiChoiceItem> items) {
		super.onSelected(id, items);
		if (id == R.id.status) {
			var statuses = new ArrayList<String>();
			for (var item : items) {
				if (item.isChecked()) {
					statuses.add(((TransactionStatusMultiChoiceItem) item).status.name());
				}
			}
			if (!statuses.isEmpty()) {
				filter.put(Criteria.in(BlotterFilter.STATUS, statuses.toArray(new String[0])));
			}
			else {
				clear(BlotterFilter.STATUS, status);
			}
			updateStatusFromFilter();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			case REQUEST_DATE_FILTER:
				if (resultCode == RESULT_FIRST_USER) {
					onClick(period, R.id.period_clear);
				} else if (resultCode == RESULT_OK) {
					DateTimeCriteria c = WhereFilter.dateTimeFromIntent(this, data);
					filter.put(c);
					updatePeriodFromFilter();
				}
				break;

			case REQUEST_NOTE_FILTER:
				if (resultCode == RESULT_FIRST_USER) {
					onClick(note, R.id.note_clear);
				} else if (resultCode == RESULT_OK) {
					filter.put(new Criteria(BlotterFilter.NOTE, WhereFilter.Operation.LIKE,
							data.getStringExtra(NoteFilterActivity.NOTE_CONTAINING)));
					updateNoteFromFilter();
				}
				break;
		}
	}

	static class TransactionStatusMultiChoiceItem implements MultiChoiceItem {
		public TransactionStatus status;
		private String title;
		private boolean isChecked;

		public TransactionStatusMultiChoiceItem(TransactionStatus status, String title) {
			this.status = status;
			this.title = title;
		}

		@Override
		public long getId() {
			return status.ordinal();
		}

		@Override
		public String getTitle() {
			return this.title;
		}

		@Override
		public boolean isChecked() {
			return isChecked;
		}

		@Override
		public void setChecked(boolean checked) {
			isChecked = checked;
		}
	}
}
