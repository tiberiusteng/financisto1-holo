/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package tw.tib.financisto.export.csv;

import androidx.annotation.NonNull;

import tw.tib.financisto.model.*;
import tw.tib.financisto.model.Category;
import tw.tib.financisto.model.Currency;
import tw.tib.financisto.model.MyEntity;
import tw.tib.financisto.model.Payee;
import tw.tib.financisto.model.Project;
import tw.tib.financisto.model.Transaction;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

public class CsvTransaction {

    public Date date;
    public Date time;
    public String status;
    public long fromAccountId;
    public long fromAmount;
    public long originalAmount;
    public String originalCurrency;
    public String payee;
    public String category;
    public String categoryParent;
    public String note;
    public String project;
    public String currency;
    public long delta;

    Transaction createTransaction(Map<String, Currency> currencies, Map<String, Category> categories, Map<String, Project> projects, Map<String, Payee> payees) {
        Transaction t = new Transaction();
        t.dateTime = combineToMillis(date, time, delta);
        if (status != null) {
            t.status = TransactionStatus.valueOf(status);
        }
        t.fromAccountId = fromAccountId;
        t.fromAmount = fromAmount;
        t.categoryId = getEntityIdOrZero(categories, category);
        t.payeeId = getEntityIdOrZero(payees, payee);
        t.projectId = getEntityIdOrZero(projects, project);
        if (originalAmount != 0) {
            Currency currency = currencies.get(originalCurrency);
            t.originalFromAmount = originalAmount;
            t.originalCurrencyId = currency.id;
        }
        t.note = note;
        return t;
    }

    private long combineToMillis(Date date, Date time, long delta) {
        Calendar dateC = emptyCalendar(date);
        Calendar dateT = emptyCalendar(time);
        Calendar c = Calendar.getInstance();
        copy(Calendar.YEAR, dateC, c);
        copy(Calendar.MONTH, dateC, c);
        copy(Calendar.DAY_OF_MONTH, dateC, c);
        copy(Calendar.HOUR_OF_DAY, dateT, c);
        copy(Calendar.MINUTE, dateT, c);
        copy(Calendar.SECOND, dateT, c);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis() + delta;
    }

    @NonNull
    private Calendar emptyCalendar(Date date) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.setTimeInMillis(date.getTime());
        return c;
    }

    private void copy(int field, Calendar fromC, Calendar toC) {
        toC.set(field, fromC.get(field));
    }

    private static <T extends MyEntity> long getEntityIdOrZero(Map<String, T> map, String value) {
        T e = map.get(value);
        return e != null ? e.id : 0;
    }

}
