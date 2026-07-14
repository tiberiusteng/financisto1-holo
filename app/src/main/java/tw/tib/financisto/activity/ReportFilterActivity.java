/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
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
import tw.tib.financisto.db.DatabaseHelper.ReportColumns;
import tw.tib.financisto.filter.Criterion;
import tw.tib.financisto.filter.DateTimeCriterion;
import tw.tib.financisto.filter.WhereFilter;
import tw.tib.financisto.model.Account;
import tw.tib.financisto.model.Currency;
import tw.tib.financisto.model.TransactionStatus;
import tw.tib.financisto.utils.EnumUtils;
import tw.tib.financisto.utils.LocalizableEnum;
import tw.tib.financisto.utils.TransactionUtils;

import java.text.DateFormat;
import java.util.Date;

public class ReportFilterActivity extends FilterAbstractActivity {

    public static final String HIDE_PERIOD = "hide_period";
    public static final String NO_FILTER_SPLIT = "no_filter_split";

    private enum FilterTransfer implements LocalizableEnum {
        NO_FILTER(R.string.no_filter),
        EXCLUDE(R.string.filter_transfer_exclude);

        public final int titleId;

        FilterTransfer(int titleId) {
            this.titleId = titleId;
        }

        @Override
        public int getTitleId() {
            return titleId;
        }
    }

    private enum FilterSplit implements LocalizableEnum {
        NO_FILTER(R.string.no_filter),
        ONLY_SUMMARY(R.string.filter_report_split_only_summary),
        ONLY_CHILDREN(R.string.filter_report_split_only_children);

        public final int titleId;

        FilterSplit(int titleId) {
            this.titleId = titleId;
        }

        @Override
        public int getTitleId() {
            return titleId;
        }

    }

    private static final FilterTransfer[] filterTransfer = FilterTransfer.values();
    private static final TransactionStatus[] statuses = TransactionStatus.values();
    private static final FilterSplit[] filterSplit = FilterSplit.values();

    private TextView period;
    private TextView account;
    private TextView currency;
    private TextView status;
    private TextView transfer;
    private TextView split;

    private DateFormat df;
    private String filterValueNotFound;

    private boolean hidePeriod = false;
    private boolean noFilterSplit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.blotter_filter);

        df = DateUtils.getShortDateFormat(this);
        filterValueNotFound = getString(R.string.filter_value_not_found);

        Intent intent = getIntent();
        if (intent != null) {
            hidePeriod = intent.getBooleanExtra(HIDE_PERIOD, false);
            noFilterSplit = intent.getBooleanExtra(NO_FILTER_SPLIT, false);
        }

        LinearLayout layout = findViewById(R.id.layout);
        if (!hidePeriod) {
            period = x.addFilterNodeMinus(layout, R.id.period, R.id.period_clear, R.string.period, R.string.no_filter);
        }
        account = x.addFilterNodeMinus(layout, R.id.account, R.id.account_clear, R.string.account, R.string.no_filter);
        currency = x.addFilterNodeMinus(layout, R.id.currency, R.id.currency_clear, R.string.currency, R.string.no_filter);
        initCategorySelector(layout);
        initPayeeSelector(layout);
        initProjectSelector(layout);
        initLocationSelector(layout);
        status = x.addFilterNodeMinus(layout, R.id.status, R.id.status_clear, R.string.transaction_status, R.string.no_filter);
        transfer = x.addFilterNodeMinus(layout, R.id.transfer, R.id.transfer_clear, R.string.filter_transfer, R.string.no_filter);
        if (!noFilterSplit) {
            split = x.addFilterNodeMinus(layout, R.id.split, R.id.split_clear, R.string.filter_split, R.string.no_filter);
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
            setResult(RESULT_FIRST_USER);
            finish();
        });

        if (intent != null) {
            filter = WhereFilter.fromIntent(intent);
            if (!hidePeriod) {
                updatePeriodFromFilter();
            }
            updateAccountFromFilter();
            updateCurrencyFromFilter();
            updateCategoryFromFilter();
            updateProjectFromFilter();
            updatePayeeFromFilter();
            updateLocationFromFilter();
            updateStatusFromFilter();
            updateTransferFromFilter();
            if (!noFilterSplit) {
                updateSplitFromFilter();
            }
        }

    }

    private void updatePeriodFromFilter() {
        DateTimeCriterion c = (DateTimeCriterion)filter.get(BlotterFilter.DATETIME);
        if (c != null) {
            Period p = c.getPeriod();
            if (p.isCustom()) {
                long periodFrom = c.getLongValue1();
                long periodTo = c.getLongValue2();
                period.setText(df.format(new Date(periodFrom))+"-"+df.format(new Date(periodTo)));
            } else {
                period.setText(p.type.titleId);
            }
            showMinusButton(period);
        } else {
            clear(BlotterFilter.DATETIME, period);
        }
    }

    private void updateAccountFromFilter() {
        updateEntityFromFilter(BlotterFilter.FROM_ACCOUNT_ID, Account.class, account);
    }

    private void updateCurrencyFromFilter() {
        updateEntityFromFilter(BlotterFilter.FROM_ACCOUNT_CURRENCY_ID, Currency.class, currency);
    }

    private void updateStatusFromFilter() {
        Criterion c = filter.get(BlotterFilter.STATUS);
        if (c != null) {
            TransactionStatus s = TransactionStatus.valueOf(c.getStringValue());
            status.setText(getString(s.titleId));
            showMinusButton(status);
        } else {
            status.setText(R.string.no_filter);
            hideMinusButton(status);
        }
    }

    private void updateTransferFromFilter() {
        Criterion c = filter.get(ReportColumns.IS_TRANSFER);
        if (c != null) {
            transfer.setText(R.string.filter_transfer_exclude);
            showMinusButton(transfer);
        } else {
            transfer.setText(R.string.no_filter);
            hideMinusButton(transfer);
        }
    }

    private void updateSplitFromFilter() {
        Criterion c = filter.get(ReportColumns.CATEGORY_ID);
        Criterion p = filter.get(ReportColumns.PARENT_ID);
        if (c == null && p == null) {
            split.setText(R.string.no_filter);
        }
        else if (p != null) {
            split.setText(R.string.filter_report_split_only_summary);
        }
        else {
            split.setText(R.string.filter_report_split_only_children);
        }
    }

    @Override
    protected void onClick(View v, int id) {
        super.onClick(v, id);
        switch (id) {
            case R.id.period:
                Intent intent = new Intent(this, DateFilterActivity.class);
                filter.toIntent(intent);
                startActivityForResult(intent, 1);
                break;
            case R.id.period_clear:
                clear(BlotterFilter.DATETIME, period);
                break;
            case R.id.account: {
                Cursor cursor = db.getAllAccounts();
                startManagingCursor(cursor);
                ListAdapter adapter = TransactionUtils.createAccountAdapter(this, cursor);
                Criterion c = filter.get(BlotterFilter.FROM_ACCOUNT_ID);
                long selectedId = c != null ? c.getLongValue1() : -1;
                x.select(this, R.id.account, R.string.account, cursor, adapter, "_id", selectedId);
            } break;
            case R.id.account_clear:
                clear(BlotterFilter.FROM_ACCOUNT_ID, account);
                break;
            case R.id.currency: {
                Cursor cursor = db.getAllCurrencies("name");
                startManagingCursor(cursor);
                ListAdapter adapter = TransactionUtils.createCurrencyAdapter(this, cursor);
                Criterion c = filter.get(BlotterFilter.FROM_ACCOUNT_CURRENCY_ID);
                long selectedId = c != null ? c.getLongValue1() : -1;
                x.select(this, R.id.currency, R.string.currency, cursor, adapter, "_id", selectedId);
            } break;
            case R.id.currency_clear:
                clear(BlotterFilter.FROM_ACCOUNT_CURRENCY_ID, currency);
                break;
            case R.id.status: {
                ArrayAdapter<String> adapter = EnumUtils.createDropDownAdapter(this, statuses);
                Criterion c = filter.get(BlotterFilter.STATUS);
                int selectedPos = c != null ? TransactionStatus.valueOf(c.getStringValue()).ordinal() : -1;
                x.selectPosition(this, R.id.status, R.string.transaction_status, adapter, selectedPos);
            } break;
            case R.id.transfer: {
                ArrayAdapter<String> adapter = EnumUtils.createDropDownAdapter(this, filterTransfer);
                Criterion c = filter.get(ReportColumns.IS_TRANSFER);
                int selectedPos = c != null ? 1 : 0;
                x.selectPosition(this, R.id.transfer, R.string.filter_transfer, adapter, selectedPos);
            } break;
            case R.id.split: {
                ArrayAdapter<String> adapter = EnumUtils.createDropDownAdapter(this, filterSplit);
                Criterion c = filter.get(ReportColumns.CATEGORY_ID);
                Criterion p = filter.get(ReportColumns.PARENT_ID);
                int selectedPos = 0;
                if (p != null) {
                    selectedPos = 1;
                }
                else if (c != null) {
                    selectedPos = 2;
                }
                x.selectPosition(this, R.id.split, R.string.filter_split, adapter, selectedPos);
            } break;
            case R.id.status_clear:
                clear(BlotterFilter.STATUS, status);
                break;
            case R.id.transfer_clear:
                clear(ReportColumns.IS_TRANSFER, transfer);
                break;
            case R.id.split_clear:
                clear(ReportColumns.CATEGORY_ID, split);
                clear(ReportColumns.PARENT_ID, split);
                break;
        }
    }

    @Override
    public void onSelectedId(int id, long selectedId) {
        super.onSelectedId(id, selectedId);
        switch (id) {
            case R.id.account:
                filter.put(Criterion.eq(BlotterFilter.FROM_ACCOUNT_ID, String.valueOf(selectedId)));
                updateAccountFromFilter();
                break;
            case R.id.currency:
                filter.put(Criterion.eq(BlotterFilter.FROM_ACCOUNT_CURRENCY_ID, String.valueOf(selectedId)));
                updateCurrencyFromFilter();
                break;
        }
    }

    @Override
    public void onSelectedPos(int id, int selectedPos) {
        super.onSelectedPos(id, selectedPos);
        switch (id) {
            case R.id.status:
                filter.put(Criterion.eq(BlotterFilter.STATUS, statuses[selectedPos].name()));
                updateStatusFromFilter();
                break;
            case R.id.transfer:
                if (selectedPos == 0) {
                    filter.remove(ReportColumns.IS_TRANSFER);
                }
                else {
                    filter.put(Criterion.eq(ReportColumns.IS_TRANSFER, "0"));
                }
                updateTransferFromFilter();
                break;
            case R.id.split:
                if (selectedPos == 0) {
                    filter.remove(ReportColumns.CATEGORY_ID);
                    filter.remove(ReportColumns.PARENT_ID);
                }
                else if (selectedPos == 1) {
                    filter.remove(ReportColumns.CATEGORY_ID);
                    filter.put(Criterion.eq(ReportColumns.PARENT_ID, "0"));
                }
                else if (selectedPos == 2) {
                    filter.put(Criterion.neq(ReportColumns.CATEGORY_ID, "-1"));
                    filter.remove(ReportColumns.PARENT_ID);
                }
                updateSplitFromFilter();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_FIRST_USER) {
                onClick(period, R.id.period_clear);
            } else if (resultCode == RESULT_OK) {
                DateTimeCriterion c = WhereFilter.dateTimeFromIntent(data);
                filter.put(c);
                updatePeriodFromFilter();
            }
        }
    }

}
