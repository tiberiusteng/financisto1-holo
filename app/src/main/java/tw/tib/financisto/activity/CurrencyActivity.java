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
package tw.tib.financisto.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import java.text.DecimalFormatSymbols;

import tw.tib.financisto.R;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.model.Currency;
import tw.tib.financisto.model.SymbolFormat;
import tw.tib.financisto.utils.CurrencyCache;
import tw.tib.financisto.utils.MyPreferences;
import tw.tib.financisto.utils.PinProtection;

import static tw.tib.financisto.utils.Utils.checkEditText;
import static tw.tib.financisto.utils.Utils.text;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class CurrencyActivity extends Activity {

	public static final String CURRENCY_ID_EXTRA = "currencyId";
	private static final DecimalFormatSymbols s = new DecimalFormatSymbols();

	private DatabaseAdapter db;

	private String[] decimalSeparatorsItems;
	private String[] groupSeparatorsItems;
	private SymbolFormat[] symbolFormats;

	private EditText name;
	private EditText title;
	private EditText symbol;
	private CheckBox isDefault;
	private CheckBox updateExchangeRate;
	private Spinner decimals;
	private Spinner decimalSeparators;
	private Spinner groupSeparators;
	private Spinner symbolFormat;
	private EditText numberFormat;

	private int maxDecimals;

	private Currency currency = new Currency();

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(MyPreferences.switchLocale(base));
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.currency);

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.currency), (v, windowInsets) -> {
			Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
					| WindowInsetsCompat.Type.statusBars()
					| WindowInsetsCompat.Type.captionBar());
			var lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
			lp.topMargin = insets.top;
			lp.bottomMargin = insets.bottom;
			v.setLayoutParams(lp);
			return WindowInsetsCompat.CONSUMED;
		});

		db = new DatabaseAdapter(this);
		db.open();

		name = findViewById(R.id.name);
		title = findViewById(R.id.title);
		symbol = findViewById(R.id.symbol);
		isDefault = findViewById(R.id.is_default);
		updateExchangeRate = findViewById(R.id.update_exchange_rate);
		decimals = findViewById(R.id.spinnerDecimals);
		decimalSeparators = findViewById(R.id.spinnerDecimalSeparators);
		groupSeparators = findViewById(R.id.spinnerGroupSeparators);
		groupSeparators.setSelection(1);
		symbolFormat = findViewById(R.id.spinnerSymbolFormat);
		symbolFormat.setSelection(0);
		numberFormat = findViewById(R.id.number_format);

		maxDecimals = decimals.getCount() - 1;

		decimalSeparatorsItems = getResources().getStringArray(R.array.decimal_separators);
		groupSeparatorsItems = getResources().getStringArray(R.array.group_separators);
		symbolFormats = SymbolFormat.values();

		Button bOk = findViewById(R.id.bOK);
		bOk.setOnClickListener(view -> {
			if (checkEditText(title, "title", true, 100)
					&& checkEditText(name, "code", true, 100)
					&& checkEditText(symbol, "symbol", true, 3)) {
				currency.title = text(title);
				currency.name = text(name);
				currency.symbol = text(symbol);
				currency.isDefault = isDefault.isChecked();
				currency.updateExchangeRate = updateExchangeRate.isChecked();
				currency.decimals = maxDecimals - decimals.getSelectedItemPosition();
				currency.decimalSeparator = decimalSeparators.getSelectedItem().toString();
				currency.groupSeparator = groupSeparators.getSelectedItem().toString();
				currency.symbolFormat = symbolFormats[symbolFormat.getSelectedItemPosition()];
				currency.numberFormat = text(numberFormat);
				long id = db.saveOrUpdate(currency);
				CurrencyCache.initialize(db);
				Intent data = new Intent();
				data.putExtra(CURRENCY_ID_EXTRA, id);
				setResult(RESULT_OK, data);
				finish();
			}
		});

		Button bCancel = findViewById(R.id.bCancel);
		bCancel.setOnClickListener(view -> {
			setResult(RESULT_CANCELED, null);
			finish();
		});

		Intent intent = getIntent();
		if (intent != null) {
			long id = intent.getLongExtra(CURRENCY_ID_EXTRA, -1);
			if (id != -1) {
				currency = db.load(Currency.class, id);
				editCurrency();
			} else {
				makeDefaultIfNecessary();
			}
		}
	}

	private void makeDefaultIfNecessary() {
		isDefault.setChecked(db.getAllCurrenciesList().isEmpty());
		updateExchangeRate.setChecked(false);
	}

	private void editCurrency() {
		Currency currency = this.currency;
		name.setText(currency.name);
		title.setText(currency.title);
		symbol.setText(currency.symbol);
		isDefault.setChecked(currency.isDefault);
		updateExchangeRate.setChecked(currency.updateExchangeRate);
		decimals.setSelection(maxDecimals - currency.decimals);
		decimalSeparators.setSelection(indexOf(decimalSeparatorsItems, currency.decimalSeparator, s.getDecimalSeparator()));
		groupSeparators.setSelection(indexOf(groupSeparatorsItems, currency.groupSeparator, s.getGroupingSeparator()));
		symbolFormat.setSelection(currency.symbolFormat.ordinal());
		numberFormat.setText(currency.numberFormat);
	}

	private int indexOf(String[] a, String v, char c) {
		int count = a.length;
		int d = -1;
		for (int i = 0; i < count; i++) {
			String s = a[i];
			if (v != null && s.charAt(1) == v.charAt(1)) {
				return i;
			}
			if (s.charAt(1) == c) {
				d = i;
			}
		}
		return d;
	}

	@Override
	protected void onDestroy() {
		db.close();
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
		PinProtection.lock(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		PinProtection.unlock(this);
	}
}
