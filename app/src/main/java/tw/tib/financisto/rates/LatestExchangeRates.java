/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package tw.tib.financisto.rates;

import android.content.Context;
import android.util.Log;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.model.Currency;
import tw.tib.financisto.utils.CurrencyCache;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 1/25/12 11:49 PM
 */
public class LatestExchangeRates implements ExchangeRateProvider, ExchangeRatesCollection {
    private static final String TAG = "LatestRates";

    protected Context context;
    protected Currency homeCurrency;
    protected DatabaseAdapter db;

    public LatestExchangeRates(Context context) {
        this.context = context;
        this.db = new DatabaseAdapter(context);
    }

    private final Map<Long, Map<Long, ExchangeRate>> rates = new Long2ObjectOpenHashMap<>();

    @Override
    public ExchangeRate getRate(Currency fromCurrency, Currency toCurrency) {
        if (fromCurrency.id == toCurrency.id) {
            return ExchangeRate.ONE;
        }
        Map<Long, ExchangeRate> rateMap = getMapFor(fromCurrency.id);
        ExchangeRate rate = rateMap.get(toCurrency.id);
        if (rate != null) {
            Log.d(TAG, "getRate direct " + rate);
            return rate;
        }
        // estimate from inverse exchange
        Map<Long, ExchangeRate> rateMapInverse = getMapFor(toCurrency.id);
        rate = rateMapInverse.get(fromCurrency.id);
        if (rate != null) {
            ExchangeRate inverse = rate.flip();
            rateMap.put(toCurrency.id, inverse);
            return inverse;
        }
        // estimate from exchange via home currency
        if (homeCurrency == null) {
            homeCurrency = CurrencyCache.getHomeCurrency();
        }
        if (!homeCurrency.equals(Currency.EMPTY) &&
            !fromCurrency.equals(homeCurrency) &&
            !toCurrency.equals(homeCurrency))
        {
            Log.d(TAG, "getRate trying to estimate with home currency " + homeCurrency);
            ExchangeRate e1 = getRate(fromCurrency, homeCurrency);
            if (e1 != ExchangeRate.NA) {
                ExchangeRate e2 = getRate(homeCurrency, toCurrency);
                if (e2 != ExchangeRate.NA) {
                    return combineRate(rateMap, fromCurrency, toCurrency, e1, e2);
                }
            }
        }
        // through trading currency
        if (fromCurrency.tradingCurrencyId != 0) {
            Currency tradingCurrency = CurrencyCache.getCurrency(fromCurrency.tradingCurrencyId);
            Log.d(TAG, "getRate via trading currency " + tradingCurrency);
            ExchangeRate t1 = getRate(fromCurrency, tradingCurrency);
            if (t1 != ExchangeRate.NA) {
                ExchangeRate t2 = getRate(tradingCurrency, toCurrency);
                if (t2 != ExchangeRate.NA) {
                    return combineRate(rateMap, fromCurrency, toCurrency, t1, t2);
                }
            }
        }
        // negative cache
        rate = ExchangeRate.NA;
        rateMap.put(toCurrency.id, rate);
        return rate;
    }

    private ExchangeRate combineRate(
            Map<Long, ExchangeRate> rateMap, Currency fromCurrency, Currency toCurrency,
            ExchangeRate e1, ExchangeRate e2
    ) {
        Log.d(TAG, "combineRate e1=" + e1 + ", e2=" + e2);
        var rate = new ExchangeRate();
        rate.fromCurrencyId = fromCurrency.id;
        rate.toCurrencyId = toCurrency.id;
        rate.rate = e1.rate * e2.rate;
        rate.derivedFrom = Arrays.asList(e1, e2);
        rateMap.put(toCurrency.id, rate);
        Log.d(TAG, "    result " + rate);
        return rate;
    }

    @Override
    public ExchangeRate getRate(Currency fromCurrency, Currency toCurrency, long atTime) {
        return getRate(fromCurrency, toCurrency);
    }

    @Override
    public List<ExchangeRate> getRates(Currency homeCurrency, List<Currency> currencies) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addRate(ExchangeRate r) {
        Map<Long, ExchangeRate> rateMap = getMapFor(r.fromCurrencyId);
        rateMap.put(r.toCurrencyId, r);
    }

    private Map<Long, ExchangeRate> getMapFor(long fromCurrencyId) {
        Map<Long, ExchangeRate> m = rates.get(fromCurrencyId);
        if (m == null) {
            m = new Long2ObjectOpenHashMap<>();
            rates.put(fromCurrencyId, m);
        }
        return m;
    }

}
