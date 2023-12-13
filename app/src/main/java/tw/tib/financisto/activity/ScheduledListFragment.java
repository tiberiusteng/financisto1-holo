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

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import tw.tib.financisto.R;
import tw.tib.financisto.adapter.ScheduledListAdapter;
import tw.tib.financisto.blotter.BlotterFilter;
import tw.tib.financisto.filter.WhereFilter;
import tw.tib.financisto.model.TransactionInfo;
import tw.tib.financisto.service.RecurrenceScheduler;

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
        blotterFilter = new WhereFilter(getString(R.string.scheduled_transactions));
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
        scheduler.cancelPendingWorkForSchedule(getContext(), id);
    }

    @Override
    public void integrityCheck() {
        new InstalledOnSdCardCheckTask(getActivity()).execute();
    }

}
