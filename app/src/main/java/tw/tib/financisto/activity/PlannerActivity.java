/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package tw.tib.financisto.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.TextView;
import tw.tib.financisto.R;
import tw.tib.financisto.datetime.Period;
import tw.tib.financisto.datetime.PeriodType;
import tw.tib.financisto.adapter.ScheduledListAdapter;
import tw.tib.financisto.db.DatabaseHelper;
import tw.tib.financisto.filter.Criteria;
import tw.tib.financisto.filter.WhereFilter;
import tw.tib.financisto.filter.DateTimeCriteria;
import tw.tib.financisto.model.Total;
import tw.tib.financisto.utils.FuturePlanner;
import tw.tib.financisto.utils.TransactionList;
import tw.tib.financisto.utils.Utils;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 10/20/12 2:22 PM
 */
public class PlannerActivity extends AbstractListActivity<TransactionList> {

    private TextView totalText;
    private TextView filterText;

    private WhereFilter filter = WhereFilter.empty();

    public PlannerActivity() {
        super(R.layout.planner);
    }

    @Override
    protected void internalOnCreate(Bundle savedInstanceState) {
        totalText = (TextView)findViewById(R.id.total);
        filterText = (TextView)findViewById(R.id.period);
        ImageButton bFilter = (ImageButton) findViewById(R.id.bFilter);
        bFilter.setImageResource(R.drawable.ic_menu_filter_on);
        bFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFilter();
            }
        });

        loadFilter();
        setupFilter();
    }

    private void loadFilter() {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        filter = WhereFilter.fromSharedPreferences(preferences);
        applyDateTimeCriteria(filter.getDateTime());
    }

    private void setupFilter() {
        if (filter.isEmpty()) {
            applyDateTimeCriteria(null);
        }
    }

    private void applyDateTimeCriteria(DateTimeCriteria criteria) {
        if (criteria == null) {
            Calendar date = Calendar.getInstance();
            date.add(Calendar.MONTH, 1);
            criteria = new DateTimeCriteria(PeriodType.THIS_MONTH);
        }
        long now = System.currentTimeMillis();
        if (now > criteria.getLongValue1()) {
            Period period = criteria.getPeriod();
            period.start = now;
            criteria = new DateTimeCriteria(period);
        }
        filter.put(criteria);
    }

    private void showFilter() {
        Intent intent = new Intent(this, BlotterFilterActivity.class);
        filter.toIntent(intent);
        intent.putExtra(BlotterFilterActivity.IS_PLANNER_FILTER, true);
        startActivityForResult(intent, 1);
    }

    private void saveFilter() {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        filter.toSharedPreferences(preferences);
        SharedPreferences.Editor editor = preferences.edit();
        editor.commit();
    }

    @Override
    protected TransactionList loadInBackground() {
        FuturePlanner planner = new FuturePlanner(this, db, filter, new Date());
        return planner.getPlannedTransactionsWithTotals();
    }

    @Override
    protected ListAdapter createAdapter(TransactionList data) {
        ScheduledListAdapter adapter = new ScheduledListAdapter(PlannerActivity.this, data.transactions);
        setTotals(data.totals);
        updateFilterText(filter);
        return adapter;
    }

    @Override
    protected void deleteItem(View v, int position, long id) {
    }

    @Override
    protected void editItem(View v, int position, long id) {
    }

    @Override
    protected void viewItem(View v, int position, long id) {
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            filter = WhereFilter.fromIntent(data);
            DateTimeCriteria c = filter.getDateTime();
            applyDateTimeCriteria(c);
            saveFilter();
            recreateCursor();
        }
    }

    private void updateFilterText(WhereFilter filter) {
        Criteria c = filter.get(DatabaseHelper.ReportColumns.DATETIME);
        if (c != null) {
            filterText.setText(DateUtils.formatDateRange(this, c.getLongValue1(), c.getLongValue2(),
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_MONTH));
        } else {
            filterText.setText(R.string.no_filter);
        }
    }

    private void setTotals(Total[] totals) {
        Utils u = new Utils(this);
        u.setTotal(totalText, totals[0]);
    }

}
