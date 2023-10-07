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
package ru.orangesoftware.financisto.activity;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.BuildCompat;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.ScheduledListAdapter;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.model.TransactionInfo;
import ru.orangesoftware.financisto.service.RecurrenceScheduler;

import java.util.ArrayList;

public class ScheduledListFragment extends BlotterFragment {

    private RecurrenceScheduler scheduler;

    public ScheduledListFragment() {}

    public ScheduledListFragment(int layoutId) {
        super(layoutId);
    }

    @Override
    protected void calculateTotals() {
        // do nothing
    }

    @Override
    protected Cursor createCursor() {
        return null;
    }

    @Override
    protected ListAdapter createAdapter(Cursor cursor) {
        ArrayList<TransactionInfo> transactions = scheduler.getSortedSchedules(System.currentTimeMillis());
        return new ScheduledListAdapter(getContext(), transactions);
    }

    @Override
    public void recreateCursor() {
        long now = System.currentTimeMillis();
        ArrayList<TransactionInfo> transactions = scheduler.scheduleAll(getContext(), now);
        updateAdapter(transactions);
    }

    private void updateAdapter(ArrayList<TransactionInfo> transactions) {
        ((ScheduledListAdapter)adapter).setTransactions(transactions);
    }

    @Override
    @BuildCompat.PrereleaseSdkCheck
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        scheduler = new RecurrenceScheduler(db);
        // remove filter button and totals
        bFilter.setVisibility(View.GONE);
        view.findViewById(R.id.total).setVisibility(View.GONE);
        internalOnCreateTemplates();
    }

    protected void internalOnCreateTemplates() {
        // change empty list message
        ((TextView) getView().findViewById(android.R.id.empty)).setText(R.string.no_scheduled_transactions);
        // fix filter
        blotterFilter = new WhereFilter("schedules");
        blotterFilter.eq(BlotterFilter.IS_TEMPLATE, String.valueOf(2));
        blotterFilter.eq(BlotterFilter.PARENT_ID, String.valueOf(0));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            recreateCursor();
        }
    }

    @Override
    public void afterDeletingTransaction(long id) {
        super.afterDeletingTransaction(id);
        scheduler.cancelPendingIntentForSchedule(getContext(), id);
    }

    @Override
    public void integrityCheck() {
        new InstalledOnSdCardCheckTask(getActivity()).execute();
    }

}
