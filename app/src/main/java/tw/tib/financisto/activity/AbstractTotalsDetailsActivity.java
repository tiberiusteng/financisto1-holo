/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package tw.tib.financisto.activity;

import static java.lang.String.format;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import tw.tib.financisto.R;
import tw.tib.financisto.model.Currency;
import tw.tib.financisto.model.Total;
import tw.tib.financisto.rates.ExchangeRate;
import tw.tib.financisto.rates.ExchangeRateProvider;
import tw.tib.financisto.utils.Utils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 3/15/12 16:40 PM
 */
public abstract class AbstractTotalsDetailsActivity extends AbstractActivity {

    private static final String TAG = "AbstractTotalsDetails";

    private LinearLayout layout;
    private View calculatingNode;
    private Utils u;
    protected boolean shouldShowHomeCurrencyTotal = true;

    private final int titleNodeResId;

    protected AbstractTotalsDetailsActivity(int titleNodeResId) {
        this.titleNodeResId = titleNodeResId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.totals_details);

        u = new Utils(this);
        layout = (LinearLayout)findViewById(R.id.list);
        calculatingNode = x.addTitleNodeNoDivider(layout, R.string.calculating);

        Button bOk = (Button)findViewById(R.id.bOK);
        bOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        internalOnCreate();
        calculateTotals();
    }

    protected void internalOnCreate() {}

    private void calculateTotals() {
        CalculateAccountsTotalsTask task = new CalculateAccountsTotalsTask();
        task.execute();
    }

    @Override
    protected void onClick(View v, int id) {
    }
    
    private class CalculateAccountsTotalsTask extends AsyncTask<Void, Void, TotalsInfo> {
        Currency homeCurrency;
        DecimalFormat nf = new DecimalFormat("0.00000");

        @Override
        protected TotalsInfo doInBackground(Void... voids) {
            prepareInBackground();
            Total[] totals = getTotals();
            Total totalInHomeCurrency = getTotalInHomeCurrency();
            homeCurrency = totalInHomeCurrency.currency;
            ExchangeRateProvider rates = db.getLatestRates();
            List<TotalInfo> result = new ArrayList<TotalInfo>();
            for (Total total : totals) {
                ExchangeRate rate = rates.getRate(total.currency, homeCurrency);
                TotalInfo info = new TotalInfo(total, rate);
                result.add(info);
            }
            Collections.sort(result, new Comparator<TotalInfo>() {
                @Override
                public int compare(TotalInfo thisTotalInfo, TotalInfo thatTotalInfo) {
                    String thisName = thisTotalInfo.total.currency.name;
                    String thatName = thatTotalInfo.total.currency.name;
                    return thisName.compareTo(thatName);
                }
            });
            return new TotalsInfo(result, totalInHomeCurrency);
        }

        @Override
        protected void onPostExecute(TotalsInfo totals) {
            calculatingNode.setVisibility(View.GONE);
            for (TotalInfo total : totals.totals) {
                String title = getString(titleNodeResId, total.total.currency.name);
                if (total.total.currency.id != homeCurrency.id) {
                    addForeignAmountNode(total, title);
                }
                else{
                    addAmountNode(total.total, title);
                }
            }
            if (shouldShowHomeCurrencyTotal) {
                addAmountNode(totals.totalInHomeCurrency, getString(R.string.home_currency_total));
            }
        }

        private void addForeignAmountNode(TotalInfo totalInfo, String title) {
            x.addTitleNodeNoDivider(layout, title);
            if (totalInfo.total.isError()) {
                addAmountAndErrorNode(totalInfo.total);
            }
            else {
                addSingleForeignAmountNode(totalInfo);
            }
        }

        private void addSingleForeignAmountNode(TotalInfo totalInfo) {
            var v = x.addInfoNodeForeignTotal(layout, -1, "");
            u.setAmountText(v.left, totalInfo.total);
            if (totalInfo.rate == ExchangeRate.NA) {
                v.equal.setVisibility(View.INVISIBLE);
                v.right.setVisibility(View.INVISIBLE);
                v.rate.setText(getString(R.string.rate_not_available_error, totalInfo.total.currency.name, homeCurrency.name));
            }
            else {
                v.equal.setVisibility(View.VISIBLE);
                v.right.setVisibility(View.VISIBLE);

                var sb = new StringBuilder();
                sb.append(getString(R.string.rate_as_of,
                        DateUtils.formatDateTime(AbstractTotalsDetailsActivity.this, totalInfo.rate.date,
                                DateUtils.FORMAT_SHOW_DATE|DateUtils.FORMAT_SHOW_YEAR|DateUtils.FORMAT_NUMERIC_DATE)));
                sb.append(" ");
                sb.append(getString(R.string.rate_info, totalInfo.total.currency.name, nf.format(Math.abs(totalInfo.rate.rate)), homeCurrency.name));
                v.rate.setText(sb);

                Log.d(TAG, format("totalInfo.rate.rate: %f, totalInfo.total.balance: %d", totalInfo.rate.rate, totalInfo.total.balance));

                u.setAmountText(v.right, homeCurrency, (long) Math.floor(totalInfo.rate.rate * totalInfo.total.balance), false);
            }
        }

        private void addAmountNode(Total total, String title) {
            x.addTitleNodeNoDivider(layout, title);
            if (total.isError()) {
                addAmountAndErrorNode(total);
            } else {
                addSingleAmountNode(total);
            }
        }

        private void addAmountAndErrorNode(Total total) {
            TextView data = x.addInfoNode(layout, -1, R.string.not_available, "");
            Drawable dr = getResources().getDrawable(R.drawable.total_error);
            dr.setBounds(0, 0, dr.getIntrinsicWidth(), dr.getIntrinsicHeight());
            if (total.currency == Currency.EMPTY) {
                data.setText(R.string.currency_make_default_warning);
            } else {
                data.setText(total.getError(AbstractTotalsDetailsActivity.this));
            }
            data.setError("Error!", dr);
        }

        private void addSingleAmountNode(Total total) {
            TextView label = x.addInfoNodeSingle(layout, -1, "");
            label.setGravity(Gravity.RIGHT);
            u.setAmountText(label, total);
        }

    }

    protected abstract Total getTotalInHomeCurrency();

    protected abstract Total[] getTotals();

    protected void prepareInBackground() { }

    private static class TotalInfo {

        public final Total total;
        public final ExchangeRate rate;

        public TotalInfo(Total total, ExchangeRate rate) {
            this.total = total;
            this.rate = rate;
        }
    }
    
    private static class TotalsInfo {
        
        public final List<TotalInfo> totals;
        public final Total totalInHomeCurrency;

        public TotalsInfo(List<TotalInfo> totals, Total totalInHomeCurrency) {
            this.totals = totals;
            this.totalInHomeCurrency = totalInHomeCurrency;
        }

    }
    

}
