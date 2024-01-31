/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package tw.tib.financisto.widget;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;

import tw.tib.financisto.R;
import tw.tib.financisto.activity.ActivityLayout;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.model.Currency;
import tw.tib.financisto.rates.ExchangeRate;
import tw.tib.financisto.rates.ExchangeRateProvider;
import tw.tib.financisto.utils.MyPreferences;
import tw.tib.financisto.utils.Utils;

public class RateNode {
    private static final String TAG = "RateNode";

    public static final int EDIT_RATE = 112;

    private final DecimalFormat nf = new DecimalFormat("0.00000");

    private final RateNodeOwner owner;
    private final ActivityLayout x;
    private final LinearLayout layout;
    private final Context context;

    View rateInfoNode;

    private TextView rateInfo;
    private EditText rate;
    private long rateTimestamp;

    private ImageButton bCalc;
    private ImageButton bDownload;
    private ImageButton bAssign;

    public RateNode(RateNodeOwner owner, Context context, ActivityLayout x, LinearLayout layout) {
        this.owner = owner;
        this.x = x;
        this.layout = layout;
        this.context = context;
        this.rateTimestamp = 0;
        createUI();
    }

    private void createUI() {
        rateInfoNode = x.addRateNode(layout);
        rate = rateInfoNode.findViewById(R.id.rate);
        rate.addTextChangedListener(rateWatcher);
        rate.setOnFocusChangeListener((view, b) -> {
            if (b) {
                rate.selectAll();
            }
        });
        rateInfo = rateInfoNode.findViewById(R.id.data);
        bCalc = rateInfoNode.findViewById(R.id.rateCalculator);
        bCalc.setOnClickListener(v -> {
            Activity activity = owner.getActivity();

            CalculatorInput input = CalculatorInput_.builder().amount(String.valueOf(getRate())).build();
            input.setListener(amount -> {
                try {
                    setRate(Float.parseFloat(amount));
                    owner.onRateChanged();
                } catch (NumberFormatException ignored) {
                }
            });
            input.show(activity.getFragmentManager(), "calculator");
        });
        bDownload = rateInfoNode.findViewById(R.id.rateDownload);
        bDownload.setOnClickListener(v -> new RateDownloadTask().execute());
        bAssign = rateInfoNode.findViewById(R.id.rateAssign);
        bAssign.setOnClickListener(v -> owner.onRequestAssign());
    }

    public void disableAll() {
        rate.setEnabled(false);
        bCalc.setEnabled(false);
        bDownload.setEnabled(false);
    }

    public void enableAll() {
        rate.setEnabled(true);
        bCalc.setEnabled(true);
        bDownload.setEnabled(true);
    }

    public float getRate() {
        try {
            String rateText = Utils.text(rate);
            if (rateText != null) {
                rateText = rateText.replace(',', '.');
                return Float.parseFloat(rateText);
            }
            return 0;
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    public void setRate(double r, boolean updateRateInfo) {
        Log.d(TAG, "setRate");
        markRateConsistent();
        rate.removeTextChangedListener(rateWatcher);
        rate.setText(nf.format(Math.abs(r)));
        rate.addTextChangedListener(rateWatcher);
        rateTimestamp = 0;
        if (updateRateInfo) {
            updateRateInfo();
        }
    }

    public void setRate(double r) {
        setRate(r, true);
    }

    public void setRate(ExchangeRate r){
        setRate(r.rate, false);
        rateTimestamp = r.date;
        updateRateInfo();
    }

    public String checkIfRateConsistent(double r) {
        String rateInfo = nf.format(Math.abs(r));
        if (rateInfo.equals(rate.getText().toString())) {
            markRateConsistent();
        }
        else {
            markRateInconsistent();
        }
        return rateInfo;
    }

    public void hideAssignButton() {
        bAssign.setVisibility(View.GONE);
    }

    public void updateRateInfo() {
        double r = getRate();
        StringBuilder sb = new StringBuilder();
        Currency currencyFrom = owner.getCurrencyFrom();
        Currency currencyTo = owner.getCurrencyTo();
        if (rateTimestamp != 0) {
            sb.append(context.getString(R.string.rate_as_of,
                    DateUtils.formatDateTime(context, rateTimestamp,
                            DateUtils.FORMAT_SHOW_DATE|DateUtils.FORMAT_SHOW_YEAR|DateUtils.FORMAT_NUMERIC_DATE)));
        }
        if (currencyFrom != null && currencyTo != null) {
            sb.append(context.getString(R.string.rate_info, currencyTo.name, nf.format(1.0 / r), currencyFrom.name));
        }
        rateInfo.setText(sb.toString());
    }

    public void markRateInconsistent() {
        rate.setTextColor(context.getResources().getColor(R.color.holo_red_light));
    }

    public void markRateConsistent() {
        rate.setTextColor(context.getResources().getColor(android.R.color.primary_text_dark));
    }

    private class RateDownloadTask extends AsyncTask<Void, Void, ExchangeRate> {

        private ProgressDialog progressDialog;

        @Override
        protected ExchangeRate doInBackground(Void... args) {
            Currency fromCurrency = owner.getCurrencyFrom();
            Currency toCurrency = owner.getCurrencyTo();
            if (fromCurrency != null && toCurrency != null) {
                return getProvider().getRate(fromCurrency, toCurrency);
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            showProgressDialog();
            owner.onBeforeRateDownload();
        }

        private void showProgressDialog() {
            Context context = owner.getActivity();
            String message = context.getString(R.string.downloading_rate, owner.getCurrencyFrom(), owner.getCurrencyTo());
            progressDialog = ProgressDialog.show(context, null, message, true, true, dialogInterface -> cancel(true));
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            owner.onAfterRateDownload();
        }

        @Override
        protected void onPostExecute(ExchangeRate result) {
            progressDialog.dismiss();
            owner.onAfterRateDownload();
            if (result != null) {
                if (result.isOk()) {
                    setRate(result);
                    new DatabaseAdapter(context).saveRate(result);
                    owner.onSuccessfulRateDownload();
                } else {
                    Toast.makeText(owner.getActivity(), result.getErrorMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }

        private ExchangeRateProvider getProvider() {
            return MyPreferences.createExchangeRatesProvider(owner.getActivity());
        }

    }

    private final TextWatcher rateWatcher = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            rateTimestamp = 0;
            owner.onRateChanged();
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    };

}
