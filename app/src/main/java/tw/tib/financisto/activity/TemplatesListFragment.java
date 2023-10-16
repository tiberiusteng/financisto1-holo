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

import android.widget.ListAdapter;

import tw.tib.financisto.R;
import tw.tib.financisto.adapter.BlotterListAdapter;
import tw.tib.financisto.blotter.BlotterFilter;
import tw.tib.financisto.filter.WhereFilter;
import tw.tib.financisto.utils.MyPreferences;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.BuildCompat;

public class TemplatesListFragment extends BlotterFragment {

    public TemplatesListFragment() {
    }

    public TemplatesListFragment(int layoutId) {
        super(layoutId);
    }

    @Override
    protected void calculateTotals() {
        // do nothing
    }

    @Override
    protected Cursor createCursor() {
        String sortOrder = BlotterFilter.SORT_NEWER_TO_OLDER;

        switch (MyPreferences.getTemplatessSortOrder(getContext())) {
            case NAME:
                sortOrder = BlotterFilter.SORT_BY_TEMPLATE_NAME;
                break;

            case ACCOUNT:
                sortOrder = BlotterFilter.SORY_BY_ACCOUNT_NAME;
                break;
        }

        return db.getAllTemplates(blotterFilter, sortOrder);
    }

    @Override
    protected ListAdapter createAdapter(Cursor cursor) {
        return new BlotterListAdapter(getContext(), db, cursor) {
            @Override
            protected boolean isShowRunningBalance() {
                return false;
            }
        };
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // remove filter button and totals
        if (bFilter != null) {
            bFilter.setVisibility(View.GONE);
        }
        if (showAllBlotterButtons && bTemplate != null) {
            bTemplate.setVisibility(View.GONE);
        }
        View total = view.findViewById(R.id.total);
        if (total != null) {
            total.setVisibility(View.GONE);
        }
        internalOnCreateTemplates();
    }

    @Override
    protected boolean addTemplateToAddButton() {
        return false;
    }

    protected void internalOnCreateTemplates() {
        // change empty list message
        ((TextView) getView().findViewById(android.R.id.empty)).setText(R.string.no_templates);
        // fix filter
        blotterFilter = new WhereFilter("templates");
        blotterFilter.eq(BlotterFilter.IS_TEMPLATE, String.valueOf(1));
        blotterFilter.eq(BlotterFilter.PARENT_ID, String.valueOf(0));
    }

}
