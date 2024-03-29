/*
 * Copyright (c) 2013 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package tw.tib.financisto.rates;

import android.content.Context;
import android.content.SharedPreferences;

import okhttp3.OkHttpClient;
import tw.tib.financisto.http.HttpClientWrapper;

/**
 * Created with IntelliJ IDEA.
 * User: dsolonenko
 * Date: 2/19/13
 * Time: 12:06 AM
 */
public enum ExchangeRateProviderFactory {

    openexchangerates(){
        @Override
        public ExchangeRateProvider createProvider(SharedPreferences sharedPreferences, Context context) {
            String appId = sharedPreferences.getString("openexchangerates_app_id", "");
            return new OpenExchangeRatesDownloader(createDefaultWrapper(), appId, context);
        }
    },
    freeCurrency(){
        @Override
        public ExchangeRateProvider createProvider(SharedPreferences sharedPreferences, Context context) {
            return new FreeCurrencyRateDownloader(createDefaultWrapper(), System.currentTimeMillis(), context);
        }
    };

    public abstract ExchangeRateProvider createProvider(SharedPreferences sharedPreferences, Context context);

    private static HttpClientWrapper createDefaultWrapper() {
        return new HttpClientWrapper(new OkHttpClient());
    }

}