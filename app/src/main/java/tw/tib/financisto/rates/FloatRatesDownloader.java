package tw.tib.financisto.rates;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import tw.tib.financisto.R;
import tw.tib.financisto.http.HttpClientWrapper;
import tw.tib.financisto.model.Currency;

public class FloatRatesDownloader implements ExchangeRateProvider {
    protected HttpClientWrapper httpClient;
    protected Context context;
    protected Handler handler;

    protected JSONObject json;
    protected long lastUpdate = 0;

    protected final String DAILY_FEED = "https://www.floatrates.com/daily/usd.json";

    public FloatRatesDownloader(HttpClientWrapper httpClient, Context context) {
        this.httpClient = httpClient;
        this.context = context;
        this.handler = new Handler(Looper.getMainLooper());
    }

    protected void refreshRate() {
        long now = System.currentTimeMillis();
        if ((now - lastUpdate) < 43200_000) return;

        try {
            json = httpClient.getAsJson(DAILY_FEED);
        } catch (Exception e) {
            return;
        }

        lastUpdate = now;
    }

    protected ExchangeRate createRate(Currency fromCurrency, Currency toCurrency) {
        ExchangeRate r = new ExchangeRate();
        r.fromCurrencyId = fromCurrency.id;
        r.toCurrencyId = toCurrency.id;
        return r;
    }

    protected void updateRate(ExchangeRate exchangeRate, Currency fromCurrency, Currency toCurrency) throws JSONException {
        double from2usd, usd2to;

        if (fromCurrency.name.equals("USD")) {
            from2usd = 1.0d;
        }
        else {
            from2usd = json.getJSONObject(fromCurrency.name.toLowerCase()).getDouble("inverseRate");
        }
        if (toCurrency.name.equals("USD")) {
            usd2to = 1.0d;
        }
        else {
            usd2to = json.getJSONObject(toCurrency.name.toLowerCase()).getDouble("rate");
        }

        exchangeRate.rate = from2usd * usd2to;
        exchangeRate.date = lastUpdate;
    }

    protected String error(Exception e) {
        return context.getString(R.string.exchange_rate_provider_error, e.getMessage());
    }

    @Override
    public ExchangeRate getRate(Currency fromCurrency, Currency toCurrency) {
        ExchangeRate rate = createRate(fromCurrency, toCurrency);
        try {
            refreshRate();
            updateRate(rate, fromCurrency, toCurrency);
        } catch (Exception e) {
            rate.error = error(e);
        }
        return rate;
    }

    @Override
    public ExchangeRate getRate(Currency fromCurrency, Currency toCurrency, long atTime) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<ExchangeRate> getRates(Currency homeCurrency, List<Currency> currencies) {
        List<ExchangeRate> rates = new ArrayList<>();

        try {
            refreshRate();
            if (json == null) {
                handler.post(() -> new AlertDialog.Builder(context)
                        .setMessage(error(new Exception()))
                        .show());
                return null;
            }

            double homeToUsd;

            if (homeCurrency.name.equals("USD")) {
                homeToUsd = 1.0d;
            } else {
                try {
                    homeToUsd = json.getJSONObject(homeCurrency.name.toLowerCase()).getDouble("inverseRate");
                } catch (Exception e) {
                    handler.post(() -> new AlertDialog.Builder(context)
                            .setMessage(R.string.exchange_rate_default_currency_no_rate)
                            .show());
                    return null;
                }
            }

            for (Currency c : currencies) {
                if (c.isDefault || !c.updateExchangeRate) {
                    continue;
                }
                try {
                    double usdTo;
                    if (c.name.equals("USD")) {
                        usdTo = 1.0d;
                    } else {
                        usdTo = json.getJSONObject(c.name.toLowerCase()).getDouble("rate");
                    }
                    ExchangeRate rate = new ExchangeRate();
                    rate.fromCurrencyId = homeCurrency.id;
                    rate.toCurrencyId = c.id;
                    rate.rate = homeToUsd * usdTo;
                    rate.date = lastUpdate;
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
}
