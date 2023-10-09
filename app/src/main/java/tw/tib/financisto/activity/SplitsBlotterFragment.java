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

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ListAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.BuildCompat;

import tw.tib.financisto.adapter.TransactionsListAdapter;
import tw.tib.financisto.blotter.BlotterTotalCalculationTask;
import tw.tib.financisto.blotter.TotalCalculationTask;

public class SplitsBlotterFragment extends BlotterFragment {

    @Override
    @BuildCompat.PrereleaseSdkCheck
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bFilter.setVisibility(View.GONE);
    }

    @Override
    protected Cursor createCursor() {
        return db.getBlotterForAccountWithSplits(blotterFilter);
    }

    @Override
    protected ListAdapter createAdapter(Cursor cursor) {
        return new TransactionsListAdapter(getContext(), db, cursor);
    }

    @Override
    protected TotalCalculationTask createTotalCalculationTask() {
        return new BlotterTotalCalculationTask(getContext(), db, blotterFilter, totalText);
    }

}
