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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ListAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.BuildCompat;

import java.util.Map;

import tw.tib.financisto.adapter.TransactionsListAdapter;
import tw.tib.financisto.blotter.TotalCalculationTask;
import tw.tib.financisto.filter.WhereFilter;
import tw.tib.financisto.model.Budget;
import tw.tib.financisto.model.Category;
import tw.tib.financisto.model.MyEntity;
import tw.tib.financisto.model.Project;
import tw.tib.financisto.model.Total;

public class BudgetBlotterFragment extends BlotterFragment {

    private Map<Long, Category> categories;
    private Map<Long, Project> projects;

    public BudgetBlotterFragment() {
        super();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        categories = MyEntity.asMap(db.getCategoriesList(true));
        projects = MyEntity.asMap(db.getAllProjectsList(true));

        super.onViewCreated(view, savedInstanceState);

        bFilter.setVisibility(View.GONE);
    }

    @Override
    protected Cursor loadInBackground() {
        long budgetId = blotterFilter.getBudgetId();
        new Handler(Looper.getMainLooper()).post(()-> calculateTotals(blotterFilter));
        return getBlotterForBudget(budgetId);
    }

    @Override
    protected ListAdapter createAdapter(Cursor cursor) {
        return new TransactionsListAdapter(getContext(), db, cursor);
    }

    private Cursor getBlotterForBudget(long budgetId) {
        Budget b = db.load(Budget.class, budgetId);
        WhereFilter filter = Budget.createWhereFilter(b, categories, projects);
        return db.getBlotterForAccountWithSplits(filter);
    }

    @Override
    protected void showTotals() {
        Budget b = db.load(Budget.class, blotterFilter.getBudgetId());
        Intent intent = new Intent(getContext(), BlotterTotalsDetailsActivity.class);
        WhereFilter budgetFilter = Budget.createWhereFilter(b, categories, projects);
        budgetFilter.eq(WhereFilter.TAG_AS_IS, "1");
        budgetFilter.toIntent(intent);
        startActivityForResult(intent, -1);
    }

    @Override
    @SuppressLint("StaticFieldLeak")
    protected TotalCalculationTask createTotalCalculationTask(WhereFilter filter) {
        return new TotalCalculationTask(getContext(), db, totalText) {

            @Override
            public Total getTotalInHomeCurrency() {
                long t0 = System.currentTimeMillis();
                try {
                    try {
                        long budgetId = filter.getBudgetId();
                        Budget b = db.load(Budget.class, budgetId);
                        Total total = new Total(b.getBudgetCurrency());
                        total.balance = db.fetchBudgetBalance(categories, projects, b);
                        return total;
                    } finally {
                        long t1 = System.currentTimeMillis();
                        Log.d("BUDGET TOTALS", (t1-t0)+"ms");
                    }
                } catch (Exception ex) {
                    Log.e("BudgetTotals", "Unexpected error", ex);
                    return Total.ZERO;
                }
            }

            @Override
            public Total[] getTotals() {
                return new Total[0];
            }
        };
    }

}
