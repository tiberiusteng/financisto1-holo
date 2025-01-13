/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package tw.tib.financisto.export.csv;

import androidx.annotation.NonNull;

import tw.tib.financisto.R;
import tw.tib.financisto.export.ImportExportException;
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

    public Long id;
    public Date date;
    public Date time;
    public String status;
    public String account;
    public Account defaultAccount;
    public Long fromAmount;
    public Long originalAmount;
    public String originalCurrency;
    public String payee;
    public String category;
    public String categoryParent;
    public String note;
    public String project;
    public String currency;
    public long delta;

    void updateTransaction(
            Transaction t,
            Map<String, Account> accountsByName,
            Map<Long, Account> accountsById,
            Map<String, Currency> currencies,
            Map<String, Category> categories,
            Map<String, Project> projects,
            Map<String, Payee> payees)
            throws ImportExportException
    {
        if (date != null && time != null) {
            t.dateTime = combineToMillis(date, time, delta);
        }
        if (status != null) {
            t.status = TransactionStatus.valueOf(status);
        }
        if (account != null) {
            Account entity = accountsByName.get(account);
            if (entity == null) {
                throw new ImportExportException(R.string.csv_account_not_found, null, account);
            }
            t.fromAccountId = entity.id;
        }
        if (currency != null) {
            Account entity = accountsById.get(t.fromAccountId);
            if (!currency.equals(entity.currency.name)) {
                throw new ImportExportException(R.string.import_wrong_currency_2, null, currency,
                        entity.currency.name);
            }
        }
        if (fromAmount != null) {
            t.fromAmount = fromAmount;
        }
        if (category != null) {
            t.categoryId = getEntityIdOrZero(categories, category);
        }
        if (payee != null) {
            t.payeeId = getEntityIdOrZero(payees, payee);
        }
        if (project != null) {
            t.projectId = getEntityIdOrZero(projects, project);
        }
        if (originalCurrency != null) {
            Currency currency = currencies.get(originalCurrency);
            if (currency == null) {
                throw new ImportExportException(R.string.csv_currency_not_found, null, originalCurrency);
            }
            t.originalCurrencyId = currency.id;
        }
        if (originalAmount != null) {
            t.originalFromAmount = originalAmount;
        }
        if (note != null) {
            t.note = note;
        }
    }

    Transaction createTransaction(
            Map<String, Account> accounts,
            Map<String, Currency> currencies,
            Map<String, Category> categories,
            Map<String, Project> projects,
            Map<String, Payee> payees)
            throws ImportExportException
    {
        Transaction t = new Transaction();
        t.dateTime = combineToMillis(date, time, delta);
        if (status != null) {
            t.status = TransactionStatus.valueOf(status);
        }
        if (account != null) {
            Account entity = accounts.get(account);
            if (entity == null) {
                throw new ImportExportException(R.string.csv_account_not_found, null, account);
            }
            t.fromAccountId = entity.id;

            if (currency != null && !currency.equals(entity.currency.name)) {
                throw new ImportExportException(R.string.import_wrong_currency_2, null, currency,
                        entity.currency.name);
            }
        }
        else {
            t.fromAccountId = defaultAccount.id;
        }
        if (fromAmount == null) {
            throw new ImportExportException(R.string.csv_amount_required);
        }
        t.fromAmount = fromAmount;
        t.categoryId = getEntityIdOrZero(categories, category);
        t.payeeId = getEntityIdOrZero(payees, payee);
        t.projectId = getEntityIdOrZero(projects, project);
        if (originalAmount != null && originalAmount != 0) {
            Currency currency = currencies.get(originalCurrency);
            if (currency == null) {
                throw new ImportExportException(R.string.csv_currency_not_found, null, originalCurrency);
            }
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
