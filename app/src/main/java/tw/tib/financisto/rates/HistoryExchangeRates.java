/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package tw.tib.financisto.rates;

import android.content.Context;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.model.Currency;

import java.util.*;

/**
 * Not thread safe
 *
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 1/30/12 7:54 PM
 */
public class HistoryExchangeRates implements ExchangeRateProvider, ExchangeRatesCollection {
    protected Context context;
    protected Currency homeCurrency;

    public HistoryExchangeRates(Context context) {
        this.context = context;
    }

    private final TLongObjectMap<TLongObjectMap<SortedSet<ExchangeRate>>> rates = new TLongObjectHashMap<TLongObjectMap<SortedSet<ExchangeRate>>>();

    @Override
    public void addRate(ExchangeRate r) {
        SortedSet<ExchangeRate> s = getRates(r.fromCurrencyId, r.toCurrencyId);
        s.add(r);
    }

    @Override
    public ExchangeRate getRate(Currency fromCurrency, Currency toCurrency) {
        SortedSet<ExchangeRate> s = getRates(fromCurrency.id, toCurrency.id);
        if (!s.isEmpty()) {
            return s.first();
        }
        s = getRates(toCurrency.id, fromCurrency.id);
        if (!s.isEmpty()) {
            return s.first().flip();
        }
        return ExchangeRate.NA;
    }

    @Override
    public ExchangeRate getRate(Currency fromCurrency, Currency toCurrency, long atTime) {
        ExchangeRate r = new ExchangeRate();
        SortedSet<ExchangeRate> s = getRates(fromCurrency.id, toCurrency.id);
        r.date = atTime;
        // s.tailSet(r) still creates a new TreeSet object
        SortedSet<ExchangeRate> rates = s.tailSet(r);
        if (!rates.isEmpty()) {
            return rates.first();
        }
        // estimate from inverse exchange
        SortedSet<ExchangeRate> fs = getRates(toCurrency.id, fromCurrency.id);
        rates = fs.tailSet(r);
        if (!rates.isEmpty()) {
            ExchangeRate inverse = rates.first().flip();
            s.add(inverse);
            return inverse;
        }
        // estimate from exchange via home currency
        if (homeCurrency == null) {
            homeCurrency = new DatabaseAdapter(context).getHomeCurrency();
        }
        if (!homeCurrency.equals(Currency.EMPTY) &&
            !fromCurrency.equals(homeCurrency) &&
            !toCurrency.equals(homeCurrency))
        {
            ExchangeRate e1 = getRate(fromCurrency, homeCurrency, atTime);
            if (e1 != ExchangeRate.NA) {
                ExchangeRate e2 = getRate(homeCurrency, toCurrency, atTime);
                if (e2 != ExchangeRate.NA) {
                    r.fromCurrencyId = fromCurrency.id;
                    r.toCurrencyId = toCurrency.id;
                    r.rate = e1.rate * e2.rate;
                    s.add(r);
                    return r;
                }
            }
        }
        // negative cache
        ExchangeRate defaultRate = ExchangeRate.NA;
        s.add(defaultRate);
        return defaultRate;
    }

    @Override
    public List<ExchangeRate> getRates(Currency homeCurrency, List<Currency> currencies) {
        throw new UnsupportedOperationException();
    }

    private SortedSet<ExchangeRate> getRates(long fromCurrencyId, long toCurrencyId) {
        TLongObjectMap<SortedSet<ExchangeRate>> map = getMapFor(fromCurrencyId);
        return getSetFor(map, toCurrencyId);
    }

    private TLongObjectMap<SortedSet<ExchangeRate>> getMapFor(long fromCurrencyId) {
        TLongObjectMap<SortedSet<ExchangeRate>> m = rates.get(fromCurrencyId);
        if (m == null) {
            m = new TLongObjectHashMap<SortedSet<ExchangeRate>>();
            rates.put(fromCurrencyId, m);
        }
        return m;
    }
    
    private SortedSet<ExchangeRate> getSetFor(TLongObjectMap<SortedSet<ExchangeRate>> rates, long date) {
        SortedSet<ExchangeRate> s = rates.get(date);
        if (s == null) {
            s = new TreeSet<ExchangeRate>();
            rates.put(date, s);
        }
        return s;
    }

}
