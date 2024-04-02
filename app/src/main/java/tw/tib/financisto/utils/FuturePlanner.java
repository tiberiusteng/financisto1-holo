/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package tw.tib.financisto.utils;

import android.content.Context;
import android.database.Cursor;

import tw.tib.financisto.blotter.BlotterFilter;
import tw.tib.financisto.db.DatabaseHelper;
import tw.tib.financisto.filter.Criteria;
import tw.tib.financisto.filter.WhereFilter;
import tw.tib.financisto.db.TransactionsTotalCalculator;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.model.Total;
import tw.tib.financisto.model.TransactionInfo;

import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 8/25/11 11:00 PM
 */
public class FuturePlanner extends AbstractPlanner {
    public FuturePlanner(Context context, DatabaseAdapter db, WhereFilter filter, Date now) {
        super(context, db, filter, now);
    }

    @Override
    protected Cursor getRegularTransactions() {
        WhereFilter blotterFilter = WhereFilter.copyOf(filter);
        return db.getBlotter(blotterFilter);
    }

    @Override
    protected List<TransactionInfo> getScheduledTransactions() {
        WhereFilter blotterFilter = WhereFilter.copyOf(filter);
        blotterFilter.remove(BlotterFilter.DATETIME);
        blotterFilter.put(Criteria.eq("is_template", "2"));
        blotterFilter.put(Criteria.eq("parent_id", "0"));

        return asTransactionList(db.db().query(
                DatabaseHelper.V_ALL_TRANSACTIONS,
                DatabaseHelper.BlotterColumns.NORMAL_PROJECTION,
                blotterFilter.getSelection(), blotterFilter.getSelectionArgs(),
                "", "", ""));
    }

    @Override
    protected TransactionInfo prepareScheduledTransaction(TransactionInfo scheduledTransaction) {
        return scheduledTransaction;
    }

    @Override
    protected boolean includeScheduledTransaction(TransactionInfo transaction) {
        return true;
    }

    @Override
    protected boolean includeScheduledSplitTransaction(TransactionInfo split) {
        return false;
    }

    @Override
    protected Total[] calculateTotals(List<TransactionInfo> transactions) {
        Total[] totals = new Total[1];
        totals[0] = TransactionsTotalCalculator.calculateTotalFromListInHomeCurrency(db, transactions);
        return totals;
    }

    /*@Override
    protected Comparator<TransactionInfo> createSortComparator() {
        return new Comparator<TransactionInfo>() {
            @Override
            public int compare(TransactionInfo t1, TransactionInfo t2) {
                return t1.dateTime > t2.dateTime ? -1 : (t1.dateTime < t2.dateTime ? 1 : 0);
            }
        };
    } */

//    private WhereFilter createMonthlyViewFilter() {
//        return WhereFilter.empty()
//                .btw("datetime", String.valueOf(startDate.getTime()), String.valueOf(endDate.getTime()))
//                .desc("datetime");
//    }
//
//
}
