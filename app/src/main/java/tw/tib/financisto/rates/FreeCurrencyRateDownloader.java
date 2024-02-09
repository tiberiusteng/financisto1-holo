package tw.tib.financisto.rates;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tw.tib.financisto.R;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.http.HttpClientWrapper;
import tw.tib.financisto.model.Currency;

/**
 * Created by vteremasov on 11/8/17.
 */

public class FreeCurrencyRateDownloader implements ExchangeRateProvider {
    private static final String TAG = "FreeCurrencyRateDL";

    private final HttpClientWrapper client;
    private final long dateTime;
    private final Pattern unknownCurrencyPattern;
    private final Handler handler;
    private final Context context;

    public FreeCurrencyRateDownloader(HttpClientWrapper client, long dateTime, Context context) {
        this.client = client;
        this.dateTime = dateTime;
        this.unknownCurrencyPattern = Pattern.compile("Unknown currency: (\\w+)");
        this.handler = new Handler(Looper.getMainLooper());
        this.context = context;
    }

    @Override
    public ExchangeRate getRate(Currency fromCurrency, Currency toCurrency) {
        ExchangeRate rate = createRate(fromCurrency, toCurrency);
        try {
            String s = getResponse(fromCurrency, toCurrency);
            rate.rate = Double.parseDouble(s);
            return rate;
        } catch (Exception e) {
            rate.error = "Unable to get exchange rates: "+e.getMessage();
        }
        return rate;
    }

    @Override
    public ExchangeRate getRate(Currency fromCurrency, Currency toCurrency, long atTime) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public List<ExchangeRate> getRates(Currency homeCurrency, List<Currency> currencies) {
        StringBuilder toCurrenciesISO = new StringBuilder();
        HashSet<String> skip = new HashSet<>();
        JSONObject json = null;
        List<ExchangeRate> rates = new ArrayList<>();
        String result;
        DatabaseAdapter db = new DatabaseAdapter(context);
        boolean retry = true;

        while (retry) {
            toCurrenciesISO.setLength(0);
            for (Currency c : currencies) {
                if (!c.isDefault && !skip.contains(c.name) && c.updateExchangeRate) {
                    toCurrenciesISO.append(c.name);
                }
            }
            // get result as string
            try {
                result = client.getAsString(buildUrl(homeCurrency.name, toCurrenciesISO.toString()));
            }
            catch (Exception e) {
                Log.d(TAG, "getAsString", e);
                handler.post(() -> new AlertDialog.Builder(context).setMessage(e.toString()).show());
                return null;
            }
            // parse json
            try {
                json = new JSONObject(result);
                retry = false;
            } catch (Exception e) {
                // has Unknown currency, add the offending currency to skip list
                Matcher m = unknownCurrencyPattern.matcher(result);
                if (m.matches()) {
                    skip.add(m.group(1));
                }
                else {
                    Log.d(TAG, "new JSONObject", e);
                    String message = e + ", " + result;
                    handler.post(() -> new AlertDialog.Builder(context).setMessage(message).show());
                    return null;
                }
            }
        }
        for (Currency c : currencies) {
            // Stop getting update for Unknown currencies next time
            if (skip.contains(c.name)) {
                Log.d(TAG, "set updateExchangeRate=false for " + c.name);
                c.updateExchangeRate = false;
                db.saveOrUpdate(c);
                continue;
            }
            if (!c.updateExchangeRate || c.isDefault) {
                continue;
            }
            // Create rate entry
            try {
                double r = json.getDouble(c.name);
                ExchangeRate rate = new ExchangeRate();
                rate.fromCurrencyId = homeCurrency.id;
                rate.toCurrencyId = c.id;
                rate.date = dateTime;
                rate.rate = r;
                rates.add(rate);
            } catch (Exception e) {
                Log.d(TAG, "creating rate for " + c.name, e);
            }
        }
        return rates;
    }

    private String getResponse(Currency fromCurrency, Currency toCurrency) throws Exception {
        String url = buildUrl(fromCurrency, toCurrency);
        Log.i(TAG, url);
        JSONObject jsonObject = client.getAsJson(url);
        Log.i(TAG, jsonObject.getString(toCurrency.name));
        return jsonObject.getString(toCurrency.name);
    }

    private ExchangeRate createRate(Currency fromCurrency, Currency toCurrency) {
        ExchangeRate rate = new ExchangeRate();
        rate.fromCurrencyId = fromCurrency.id;
        rate.toCurrencyId = toCurrency.id;
        rate.date = dateTime;
        return rate;
    }

    private String buildUrl (Currency fromCurrency, Currency toCurrency) {
        return "https://freecurrencyrates.com/api/action.php?s=fcr&iso="+toCurrency.name+"&f="+fromCurrency.name+"&v=1&do=cvals";
    }

    private String buildUrl (String fromCurrency, String toCurrency) {
        return "https://freecurrencyrates.com/api/action.php?s=fcr&iso="+toCurrency+"&f="+fromCurrency+"&v=1&do=cvals";
    }
}