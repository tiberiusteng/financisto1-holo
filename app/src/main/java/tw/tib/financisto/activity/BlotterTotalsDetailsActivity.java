/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package tw.tib.financisto.activity;

import android.content.Intent;
import android.util.Log;

import tw.tib.financisto.R;
import tw.tib.financisto.blotter.AccountTotalCalculationTask;
import tw.tib.financisto.blotter.BlotterFilter;
import tw.tib.financisto.blotter.BlotterTotalCalculationTask;
import tw.tib.financisto.blotter.TotalCalculationTask;
import tw.tib.financisto.filter.WhereFilter;
import tw.tib.financisto.model.Total;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 3/15/12 16:40 PM
 */
public class BlotterTotalsDetailsActivity extends AbstractTotalsDetailsActivity  {
    private static final String TAG = "BlotterTotalsDetails";

    private volatile TotalCalculationTask totalCalculationTask;

    public BlotterTotalsDetailsActivity() {
        super(R.string.blotter_total_in_currency);
    }

    @Override
    protected void internalOnCreate() {
        Intent intent = getIntent();
        if (intent != null) {
            WhereFilter blotterFilter = WhereFilter.fromIntent(intent);
            cleanupFilter(blotterFilter);
            totalCalculationTask = createTotalCalculationTask(blotterFilter);
        }
    }

    private void cleanupFilter(WhereFilter blotterFilter) {
        blotterFilter.remove(BlotterFilter.BUDGET_ID);
    }

    private TotalCalculationTask createTotalCalculationTask(WhereFilter blotterFilter) {
        WhereFilter filter = WhereFilter.copyOf(blotterFilter);
        Log.d(TAG, "blotterFilter; " + blotterFilter.getSelectionArgs());
        if (filter.getAccountId() > 0 && filter.get(WhereFilter.TAG_AS_IS) == null) {
            shouldShowHomeCurrencyTotal = false;
            return new AccountTotalCalculationTask(this, db, filter, null);
        } else {
            return new BlotterTotalCalculationTask(this, db, filter, null);
        }
    }

    protected Total[] getTotals() {
        return totalCalculationTask.getTotals();
    }

}
