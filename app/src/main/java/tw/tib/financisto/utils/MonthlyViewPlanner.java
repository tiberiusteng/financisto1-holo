/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package tw.tib.financisto.utils;

import android.content.Context;
import android.database.Cursor;

import tw.tib.financisto.filter.Criteria;
import tw.tib.financisto.filter.DateTimeCriteria;
import tw.tib.financisto.filter.WhereFilter;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.db.DatabaseHelper;
import tw.tib.financisto.model.Account;
import tw.tib.financisto.model.Total;
import tw.tib.financisto.model.TransactionInfo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 8/25/11 11:00 PM
 */
public class MonthlyViewPlanner extends AbstractPlanner {

    public static final TransactionInfo PAYMENTS_HEADER = new TransactionInfo();
    public static final TransactionInfo CREDITS_HEADER = new TransactionInfo();
    public static final TransactionInfo EXPENSES_HEADER = new TransactionInfo();

    static {
        PAYMENTS_HEADER.dateTime = 0;
        CREDITS_HEADER.dateTime = 0;
        EXPENSES_HEADER.dateTime = 0;
    }

    private final Account account;
    private final boolean isStatementPreview;
    private final boolean treatTransferToCCardAsPayment;

    public MonthlyViewPlanner(Context context, DatabaseAdapter db, Account account, boolean isStatementPreview, boolean treatTransferToCCardAsPayment, Date startDate, Date endDate, Date now) {
        super(context, db, createMonthlyViewFilter(startDate, endDate, account), now);
        this.account = account;
        this.isStatementPreview = isStatementPreview;
        this.treatTransferToCCardAsPayment = treatTransferToCCardAsPayment;
    }

    @Override
    protected Cursor getRegularTransactions() {
        WhereFilter blotterFilter = WhereFilter.copyOf(filter);
        return db.getBlotterForAccountWithSplits(blotterFilter);
    }

    private static WhereFilter createMonthlyViewFilter(Date startDate, Date endDate, Account account) {
        WhereFilter filter = WhereFilter.empty();
        filter.put(new DateTimeCriteria(startDate.getTime(), endDate.getTime()));
        filter.eq(DatabaseHelper.BlotterColumns.from_account_id.name(), String.valueOf(account.id));
        filter.eq(Criteria.raw("(" + DatabaseHelper.TransactionColumns.parent_id + "=0 OR " + DatabaseHelper.BlotterColumns.is_transfer + "=-1)"));
        filter.asc(DatabaseHelper.BlotterColumns.datetime.name());
        return filter;
    }

    @Override
    protected TransactionInfo prepareScheduledTransaction(TransactionInfo scheduledTransaction) {
        return inverseTransaction(scheduledTransaction);
    }

    @Override
    protected boolean includeScheduledTransaction(TransactionInfo transaction) {
        return transaction.fromAccount.id == account.id;
    }

    @Override
    protected boolean includeScheduledSplitTransaction(TransactionInfo split) {
        return split.isTransfer() && split.toAccount.id == account.id;
    }

    private TransactionInfo inverseTransaction(TransactionInfo transaction) {
        if (transaction.isTransfer() && transaction.toAccount.id == account.id) {
            TransactionInfo inverse = transaction.clone();
            inverse.fromAccount = transaction.toAccount;
            inverse.fromAmount = transaction.toAmount;
            inverse.toAccount = transaction.fromAccount;
            inverse.toAmount = transaction.fromAmount;
            return inverse;
        }
        return transaction;
    }


    public TransactionList getCreditCardStatement() {
        TransactionList withTotals = getPlannedTransactionsWithTotals();
        List<TransactionInfo> transactions = withTotals.transactions;
        List<TransactionInfo> statement = new ArrayList<TransactionInfo>(transactions.size()+3);
        // add payments
        statement.add(PAYMENTS_HEADER);
        for (TransactionInfo transaction : transactions) {
            if (((treatTransferToCCardAsPayment && transaction.isTransfer()) || transaction.isCreditCardPayment()) && transaction.fromAmount > 0) {
                statement.add(transaction);
            }
        }
        // add credits
        statement.add(CREDITS_HEADER);
        for (TransactionInfo transaction : transactions) {
            if (!((treatTransferToCCardAsPayment && transaction.isTransfer()) || transaction.isCreditCardPayment()) && transaction.fromAmount > 0) {
                statement.add(transaction);
            }
        }
        // add expenses
        statement.add(EXPENSES_HEADER);
        for (TransactionInfo transaction : transactions) {
            if (transaction.fromAmount < 0) {
                statement.add(transaction);
            }
        }
        return new TransactionList(statement, withTotals.totals);
    }

    @Override
    protected Total[] calculateTotals(List<TransactionInfo> transactions) {
        Total[] totals = new Total[1];
        totals[0] = new Total(account.currency);
        totals[0].balance = calculateTotal(transactions);
        return totals;
    }

    private long calculateTotal(List<TransactionInfo> transactions) {
        long total = 0;
        if (isStatementPreview) {
            // exclude payments
            for (TransactionInfo t : transactions) {
                if (!((treatTransferToCCardAsPayment && t.isTransfer() && t.fromAmount > 0) || t.isCreditCardPayment())) {
                    total += getAmount(t);
                }
            }
        } else {
            // consider all transactions
            for (TransactionInfo t : transactions) {
                total += getAmount(t);
            }
        }
        return total;
    }

    private long getAmount(TransactionInfo t) {
        if (t.fromAccount.id == account.id) {
            return t.fromAmount;
        } else if (t.isTransfer() && t.toAccount.id == account.id) {
            return t.toAmount;
        }
        return 0;
    }

}
