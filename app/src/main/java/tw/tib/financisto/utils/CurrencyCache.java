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
package tw.tib.financisto.utils;

import android.database.Cursor;
import android.os.Build;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import tw.tib.financisto.Application;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.model.Currency;
import tw.tib.orb.EntityManager;
import tw.tib.orb.Query;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Format;
import java.util.Collection;

public class CurrencyCache {
	public static final String DEFAULT_FORMAT = "#,##0.00";

    //@ProtectedBy("this")
	private static final Long2ObjectOpenHashMap<Currency> CURRENCIES = new Long2ObjectOpenHashMap<>();
	private static Currency homeCurrency = Currency.EMPTY;
	private static boolean loaded = false;

	public static synchronized Currency getHomeCurrency() {
		if (!loaded) initialize(new DatabaseAdapter(Application.getInstance()));
		return homeCurrency;
	}

	public static synchronized Currency getCurrency(long currencyId) {
		if (!loaded) initialize(new DatabaseAdapter(Application.getInstance()));
		Currency c = CURRENCIES.get(currencyId);
		return c != null ? c : Currency.EMPTY;
	}

	public static synchronized void initialize(EntityManager em) {
		var currencies = new Long2ObjectOpenHashMap<Currency>();
		Query<Currency> q = em.createQuery(Currency.class);
		Cursor c = q.execute();
		homeCurrency = Currency.EMPTY;
		try {
			while (c.moveToNext()) {
				Currency currency = EntityManager.loadFromCursor(c, Currency.class);
				currencies.put(currency.id, currency);
				if (currency.isDefault) homeCurrency = currency;
			}
		} finally {
			c.close();
		}
		CURRENCIES.putAll(currencies);
		loaded = true;
	}
	
	public static Format createCurrencyFormat(Currency c) {
		String numberFormat;
		if (!Utils.isEmpty(c.numberFormat)) {
			numberFormat = c.numberFormat;
		}
		else {
			numberFormat = DEFAULT_FORMAT;
		}

		// android.icu.text.DecimalFormat in API level >= 24 support two grouping intervals,
		// like that in "#,##,##0.00" for Indian decimal formatting
		if (Build.VERSION.SDK_INT >= 24) {
			var dfs = new android.icu.text.DecimalFormatSymbols();
			dfs.setDecimalSeparator(charOrEmpty(c.decimalSeparator, dfs.getDecimalSeparator()));
			dfs.setGroupingSeparator(charOrEmpty(c.groupSeparator, dfs.getGroupingSeparator()));
			dfs.setMonetaryDecimalSeparator(dfs.getDecimalSeparator());
			dfs.setCurrencySymbol(c.symbol);

			android.icu.text.DecimalFormat df;
			try {
				df = new android.icu.text.DecimalFormat(numberFormat, dfs);
			} catch (Exception e) {
				df = new android.icu.text.DecimalFormat(DEFAULT_FORMAT, dfs);
			}
			df.setGroupingUsed(dfs.getGroupingSeparator() > 0);
			df.setMinimumFractionDigits(c.decimals);
			df.setMaximumFractionDigits(c.decimals);
			df.setDecimalSeparatorAlwaysShown(false);
			return df;
		}
		else {
			DecimalFormatSymbols dfs = new DecimalFormatSymbols();
			dfs.setDecimalSeparator(charOrEmpty(c.decimalSeparator, dfs.getDecimalSeparator()));
			dfs.setGroupingSeparator(charOrEmpty(c.groupSeparator, dfs.getGroupingSeparator()));
			dfs.setMonetaryDecimalSeparator(dfs.getDecimalSeparator());
			dfs.setCurrencySymbol(c.symbol);

			DecimalFormat df;
			try {
				df = new DecimalFormat(numberFormat, dfs);
			} catch (Exception e) {
				df = new DecimalFormat(DEFAULT_FORMAT, dfs);
			}
			df.setGroupingUsed(dfs.getGroupingSeparator() > 0);
			df.setMinimumFractionDigits(c.decimals);
			df.setMaximumFractionDigits(c.decimals);
			df.setDecimalSeparatorAlwaysShown(false);
			return df;
		}
	}

	private static char charOrEmpty(String s, char c) {
		return s != null ? (s.length() > 2 ? s.charAt(1) : 0): c;
	}

	public static synchronized Collection<Currency> getAllCurrencies() {
		if (!loaded) initialize(new DatabaseAdapter(Application.getInstance()));
		return CURRENCIES.values();
	}


}
