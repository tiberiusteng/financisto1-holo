/*
 * Copyright (c) 2013 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package tw.tib.financisto.rates;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import tw.tib.financisto.R;
import tw.tib.financisto.http.HttpClientWrapper;
import tw.tib.financisto.model.Currency;

/**
 * Created with IntelliJ IDEA.
 * User: dsolonenko
 * Date: 2/16/13
 * Time: 6:27 PM
 */
//@NotThreadSafe
public class OpenExchangeRatesDownloader implements ExchangeRateProvider {

    private static final String TAG = OpenExchangeRatesDownloader.class.getSimpleName();
    private static final String GET_LATEST = "https://openexchangerates.org/api/latest.json?app_id=";
    private static final String GET_HISTORICAL = "https://openexchangerates.org/api/historical/%s.json?app_id=%s";

    private final String appId;
    private final HttpClientWrapper httpClient;

    private JSONObject json;
    private Context context;
    private Handler handler;

    public OpenExchangeRatesDownloader(HttpClientWrapper httpClient, String appId, Context context) {
        this.httpClient = httpClient;
        this.appId = appId;
        this.context = context;
        this.handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public ExchangeRate getRate(Currency fromCurrency, Currency toCurrency) {
        ExchangeRate rate = createRate(fromCurrency, toCurrency);
        try {
            downloadLatestRates();
            if (hasError(json)) {
                rate.error = error(json);
            } else {
                updateRate(json, rate, fromCurrency, toCurrency);
            }
        } catch (Exception e) {
            rate.error = error(e);
        }
        return rate;
    }

    @Override
    public List<ExchangeRate> getRates(Currency homeCurrency, List<Currency> currencies) {
        List<ExchangeRate> rates = new ArrayList<>();

        try {
            downloadLatestRates();
            if (hasError(json)) {
                handler.post(() -> new AlertDialog.Builder(context)
                        .setMessage(error(json))
                        .show());
                return null;
            }

            JSONObject jsonRates = json.getJSONObject("rates");
            long timestamp = 1000 * json.optLong("timestamp", System.currentTimeMillis() / 1000);
            double homeToUsd;

            try {
                homeToUsd = 1.0d / jsonRates.getDouble(homeCurrency.name);
            } catch (Exception e) {
                handler.post(() -> new AlertDialog.Builder(context)
                        .setMessage(R.string.exchange_rate_default_currency_no_rate)
                        .show());
                return null;
            }

            for (Currency c : currencies) {
                if (c.isDefault || !c.updateExchangeRate) {
                    continue;
                }
                try {
                    double usdTo = jsonRates.getDouble(c.name);
                    ExchangeRate rate = new ExchangeRate();
                    rate.fromCurrencyId = homeCurrency.id;
                    rate.toCurrencyId = c.id;
                    rate.rate = homeToUsd * usdTo;
                    rate.date = timestamp;
                    rates.add(rate);

                } catch (Exception e) {
                    // skip single currency
                }
            }

        } catch (Exception e) {
            handler.post(() -> new AlertDialog.Builder(context)
                    .setMessage(error(e))
                    .show());
            return null;
        }

        return rates;
    }

    private ExchangeRate createRate(Currency fromCurrency, Currency toCurrency) {
        ExchangeRate r = new ExchangeRate();
        r.fromCurrencyId = fromCurrency.id;
        r.toCurrencyId = toCurrency.id;
        return r;
    }

    private void downloadLatestRates() throws Exception {
        if (json == null) {
            if (appIdIsNotSet()) {
                throw new RuntimeException("App ID is not set");
            }
            Log.i(TAG, "Downloading latest rates...");
            json = httpClient.getAsJson(getLatestUrl());
            Log.i(TAG, json.toString());
        }
    }

    private boolean appIdIsNotSet() {
        return TextUtils.getTrimmedLength(appId) == 0;
    }

    private String getLatestUrl() {
        return GET_LATEST+appId;
    }

    private boolean hasError(JSONObject json) throws JSONException {
        return json.optBoolean("error", false);
    }

    private String error(JSONObject json) {
        String status = json.optString("status");
        String message = json.optString("message");
        String description = json.optString("description");
        return status+" ("+message+"): "+description;
    }

    private String error(Exception e) {
        return context.getString(R.string.exchange_rate_provider_error, e.getMessage());
    }

    private void updateRate(JSONObject json, ExchangeRate exchangeRate, Currency fromCurrency, Currency toCurrency) throws JSONException {
        JSONObject rates = json.getJSONObject("rates");
        double usdFrom = rates.getDouble(fromCurrency.name);
        double usdTo = rates.getDouble(toCurrency.name);
        exchangeRate.rate = usdTo * (1 / usdFrom);
        exchangeRate.date = 1000*json.optLong("timestamp", System.currentTimeMillis()/1000);
    }

    @Override
    public ExchangeRate getRate(Currency fromCurrency, Currency toCurrency, long atTime) {
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date(atTime));
        Log.d(TAG, "getRate "+fromCurrency.name+"->"+toCurrency.name + " at " + date);

        ExchangeRate result = new ExchangeRate();
        try {
            String historicalUrl = String.format(GET_HISTORICAL, date, appId);
            JSONObject json = httpClient.getAsJson(historicalUrl);

            if (json.optBoolean("error", false)) {
                result.error = json.optString("description", "");
                return result;
            }

            JSONObject jsonRates = json.getJSONObject("rates");
            long timestamp = 1000 * json.optLong("timestamp", System.currentTimeMillis() / 1000);
            double fromToUsd;

            fromToUsd = 1.0d / jsonRates.getDouble(fromCurrency.name);

            double usdTo = jsonRates.getDouble(toCurrency.name);
            result.fromCurrencyId = fromCurrency.id;
            result.toCurrencyId = toCurrency.id;
            result.rate = fromToUsd * usdTo;
            result.date = timestamp;

        } catch (Exception e) {
            result.error = e.getMessage();
        }
        return result;
    }

}
