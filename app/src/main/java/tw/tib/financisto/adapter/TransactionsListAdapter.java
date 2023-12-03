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
package tw.tib.financisto.adapter;

import static tw.tib.financisto.model.Project.NO_PROJECT_ID;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.text.format.DateUtils;
import android.view.View;

import java.util.Calendar;

import tw.tib.financisto.R;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.db.DatabaseHelper.BlotterColumns;
import tw.tib.financisto.model.Currency;
import tw.tib.financisto.utils.CurrencyCache;
import tw.tib.financisto.utils.MyPreferences;
import tw.tib.financisto.utils.Utils;

public class TransactionsListAdapter extends BlotterListAdapter {

    private int dateColor;
    private int dateWeekendColor;
    private int projectColor;
    private boolean showProject;

    public TransactionsListAdapter(Context context, DatabaseAdapter db, Cursor c) {
        super(context, db, c);
        Resources r = context.getResources();

        this.dateColor = r.getColor(R.color.transaction_date);
        this.dateWeekendColor = r.getColor(R.color.transaction_date_weekend);
        this.projectColor = r.getColor(R.color.project_color);
        this.showProject = MyPreferences.isShowProjectInBlotter(context);
    }

    @Override
    protected void bindView(BlotterViewHolder v, Context context, Cursor cursor) {
        long toAccountId = cursor.getLong(BlotterColumns.to_account_id.ordinal());
        String payee = cursor.getString(BlotterColumns.payee.ordinal());
        String note = cursor.getString(BlotterColumns.note.ordinal());
        long locationId = cursor.getLong(BlotterColumns.location_id.ordinal());
        String location = "";
        if (locationId > 0) {
            location = cursor.getString(BlotterColumns.location.ordinal());
        }
        String toAccount = cursor.getString(BlotterColumns.to_account_title.ordinal());
        long fromAmount = cursor.getLong(BlotterColumns.from_amount.ordinal());
        if (toAccountId > 0) {
            v.topView.setText(R.string.transfer);
            if (fromAmount > 0) {
                note = toAccount+" \u00BB";
            } else {
                note = "\u00AB "+toAccount;
            }
            u.setTransferTextColor(v.centerView);
        } else {
            String title = cursor.getString(BlotterColumns.from_account_title.ordinal());
            v.topView.setText(title);
            v.centerView.setTextColor(Color.WHITE);
        }

        long categoryId = cursor.getLong(BlotterColumns.category_id.ordinal());
        String category = "";
        if (categoryId != 0) {
            category = cursor.getString(BlotterColumns.category_title.ordinal());
        }
        CharSequence text = transactionTitleUtils.generateTransactionTitle(toAccountId > 0, payee, note, location, categoryId, category);
        v.centerView.setText(text);
        sb.setLength(0);

        long projectId = cursor.getLong(BlotterColumns.project_id.ordinal());

        if (projectId == NO_PROJECT_ID || showProject == false) {
            v.top2View.setVisibility(View.INVISIBLE);
        }
        else {
            v.top2View.setVisibility(View.VISIBLE);
            v.top2View.setTextColor(projectColor);
            v.top2View.setText(cursor.getString(BlotterColumns.project.ordinal()));
        }

        long currencyId = cursor.getLong(BlotterColumns.from_account_currency_id.ordinal());
        Currency c = CurrencyCache.getCurrency(db, currencyId);
        long originalCurrencyId = cursor.getLong(BlotterColumns.original_currency_id.ordinal());
        if (originalCurrencyId > 0) {
            Currency originalCurrency = CurrencyCache.getCurrency(db, originalCurrencyId);
            long originalAmount = cursor.getLong(BlotterColumns.original_from_amount.ordinal());
            u.setAmountText(sb, v.rightCenterView, originalCurrency, originalAmount, c, fromAmount, true);
        } else {
            u.setAmountText(v.rightCenterView, c, fromAmount, true);
        }
        if (fromAmount > 0) {
            v.iconView.setImageDrawable(icBlotterIncome);
            v.iconView.setColorFilter(u.positiveColor);
        } else if (fromAmount < 0) {
            v.iconView.setImageDrawable(icBlotterExpense);
            v.iconView.setColorFilter(u.negativeColor);
        }

        long date = cursor.getLong(BlotterColumns.datetime.ordinal());
        v.bottomView.setText(DateUtils.formatDateTime(context, date,
                DateUtils.FORMAT_SHOW_DATE|DateUtils.FORMAT_SHOW_WEEKDAY|DateUtils.FORMAT_ABBREV_WEEKDAY|DateUtils.FORMAT_SHOW_TIME|DateUtils.FORMAT_ABBREV_MONTH));
        if (date > System.currentTimeMillis()) {
            u.setFutureTextColor(v.bottomView);
        } else {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(date);
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            if (dayOfWeek == Calendar.SUNDAY || dayOfWeek == Calendar.SATURDAY) {
                v.bottomView.setTextColor(dateWeekendColor);
            } else {
                v.bottomView.setTextColor(dateColor);
            }
        }

        long balance = cursor.getLong(BlotterColumns.from_account_balance.ordinal());
        v.rightView.setText(Utils.amountToString(c, balance, false));
        removeRightViewIfNeeded(v);
        setIndicatorColor(v, cursor);
    }

}
