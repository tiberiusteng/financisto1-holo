package tw.tib.financisto.rates;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import tw.tib.financisto.R;
import tw.tib.financisto.http.HttpClientWrapper;
import tw.tib.financisto.model.Currency;

/**
 * Daily ECB reference rates, latest and dated, from any API in the Frankfurter
 * format. No account or key is needed. The ECB table has about 30 currencies.
 */
public class AllRatesTodayDownloader implements ExchangeRateProvider {

    public static final String DEFAULT_BASE_URL = "https://allratestoday.com/api/compat/frankfurter";

    private static final String LATEST = "latest";
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private final HttpClientWrapper httpClient;
    private final String baseUrl;
    private final Context context;

    public AllRatesTodayDownloader(HttpClientWrapper httpClient, String baseUrl, Context context) {
        this.httpClient = httpClient;
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.context = context;
    }

    private static String normalizeBaseUrl(String baseUrl) {
        String url = baseUrl == null ? "" : baseUrl.trim();
        if (url.isEmpty()) {
            return DEFAULT_BASE_URL;
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    @Override
    public ExchangeRate getRate(Currency fromCurrency, Currency toCurrency) {
        return downloadRate(fromCurrency, toCurrency, LATEST);
    }

    @Override
    public ExchangeRate getRate(Currency fromCurrency, Currency toCurrency, long atTime) {
        String date = new SimpleDateFormat(DATE_FORMAT, Locale.US).format(new Date(atTime));
        return downloadRate(fromCurrency, toCurrency, date);
    }

    @Override
    public List<ExchangeRate> getRates(Currency homeCurrency, List<Currency> currencies) {
        List<ExchangeRate> rates = new ArrayList<>();

        try {
            JSONObject json = httpClient.getAsJson(baseUrl + "/" + LATEST + "?base=" + homeCurrency.name);
            JSONObject jsonRates = getRates(json);
            long date = getDate(json);

            for (Currency c : currencies) {
                if (c.isDefault || !c.updateExchangeRate) {
                    continue;
                }
                if (jsonRates.has(c.name)) {
                    ExchangeRate rate = createRate(homeCurrency, c);
                    rate.rate = jsonRates.getDouble(c.name);
                    rate.date = date;
                    rates.add(rate);
                }
            }
        } catch (Exception e) {
            showError(e);
            return null;
        }

        return rates;
    }

    private ExchangeRate downloadRate(Currency fromCurrency, Currency toCurrency, String path) {
        ExchangeRate rate = createRate(fromCurrency, toCurrency);
        try {
            JSONObject json = httpClient.getAsJson(
                    baseUrl + "/" + path + "?base=" + fromCurrency.name + "&symbols=" + toCurrency.name);
            rate.rate = getRates(json).getDouble(toCurrency.name);
            rate.date = getDate(json);
        } catch (Exception e) {
            rate.error = e.getMessage();
        }
        return rate;
    }

    private ExchangeRate createRate(Currency fromCurrency, Currency toCurrency) {
        ExchangeRate r = new ExchangeRate();
        r.fromCurrencyId = fromCurrency.id;
        r.toCurrencyId = toCurrency.id;
        return r;
    }

    private JSONObject getRates(JSONObject json) {
        JSONObject rates = json.optJSONObject("rates");
        if (rates == null) {
            // unknown currency or a date before the first table
            throw new RuntimeException(json.optString("message", json.toString()));
        }
        return rates;
    }

    private long getDate(JSONObject json) {
        try {
            return new SimpleDateFormat(DATE_FORMAT, Locale.US).parse(json.optString("date")).getTime();
        } catch (ParseException e) {
            return System.currentTimeMillis();
        }
    }

    protected void showError(Exception e) {
        new Handler(Looper.getMainLooper()).post(() -> new AlertDialog.Builder(context)
                .setMessage(context.getString(R.string.exchange_rate_provider_error, e.getMessage()))
                .show());
    }

}
