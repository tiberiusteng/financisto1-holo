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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.DecimalFormatSymbols;
import java.util.List;

import tw.tib.financisto.R;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.model.Currency;
import tw.tib.financisto.model.MultiChoiceItem;
import tw.tib.financisto.model.MyEntity;
import tw.tib.financisto.model.SymbolFormat;
import tw.tib.financisto.utils.CurrencyCache;
import tw.tib.financisto.utils.MyPreferences;
import tw.tib.financisto.utils.PinProtection;
import tw.tib.financisto.utils.TransactionUtils;
import tw.tib.financisto.view.NodeInflater;

import static tw.tib.financisto.utils.Utils.amountToString;
import static tw.tib.financisto.utils.Utils.checkEditText;
import static tw.tib.financisto.utils.Utils.text;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class CurrencyActivity extends Activity implements ActivityLayoutListener {
	private static final String TAG = "CurrencyActivity";

	public static final String CURRENCY_ID_EXTRA = "currencyId";
	private static final DecimalFormatSymbols s = new DecimalFormatSymbols();

	private ActivityLayout x;

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
	private TextView decimalsWarning;
	private TextView maxValue;
	private TextView minValue;
	private Spinner groupSeparators;
	private Spinner symbolFormat;
	private EditText numberFormat;
	private TextView tradingCurrency;
	private long selectedTradingCurrency = 0;

	private final Currency currencyNone = new Currency();

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
		decimalsWarning = findViewById(R.id.decimals_warning);
		maxValue = findViewById(R.id.max_value);
		minValue = findViewById(R.id.min_value);
		groupSeparators = findViewById(R.id.spinnerGroupSeparators);
		groupSeparators.setSelection(1);
		symbolFormat = findViewById(R.id.spinnerSymbolFormat);
		symbolFormat.setSelection(0);
		numberFormat = findViewById(R.id.number_format);

		LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		NodeInflater nodeInflater = new NodeInflater(layoutInflater);
		x = new ActivityLayout(nodeInflater, this);
		LinearLayout layout = findViewById(R.id.list);
		tradingCurrency = x.addListNode(layout, R.id.trading_currency, R.string.trading_currency, R.string.trading_currency_none);
		currencyNone.id = 0;
		currencyNone.name = getString(R.string.trading_currency_none);

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
				currency.tradingCurrencyId = selectedTradingCurrency;
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

		decimals.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if ((maxDecimals - position) > 2) {
					decimalsWarning.setVisibility(View.VISIBLE);
				}
				else {
					decimalsWarning.setVisibility(View.INVISIBLE);
				}

				Currency temp = new Currency();
				temp.symbol = text(symbol);
				temp.symbolFormat = symbolFormats[symbolFormat.getSelectedItemPosition()];
				temp.decimals = maxDecimals - decimals.getSelectedItemPosition();
				temp.decimalSeparator = decimalSeparators.getSelectedItem().toString();
				temp.groupSeparator = groupSeparators.getSelectedItem().toString();
				temp.numberFormat = text(numberFormat);

				maxValue.setText(getString(R.string.currency_max_value, amountToString(temp, Long.MAX_VALUE)));
				minValue.setText(getString(R.string.currency_min_value, amountToString(temp, Long.MIN_VALUE)));
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
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
		selectTradingCurrency(currency.tradingCurrencyId);
	}

	private void selectTradingCurrency(long id) {
		Log.d(TAG, "selectTradingCurrency id=" + id);
		selectedTradingCurrency = id;
		if (id == 0) {
			tradingCurrency.setText(getString(R.string.trading_currency_none));
		}
		else {
			Currency currency = CurrencyCache.getCurrency(db, id);
			tradingCurrency.setText(currency.name);
		}
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

	@Override
	public void onSelectedPos(int id, int selectedPos) {
	}

	@Override
	public void onSelectedId(int id, long selectedId) {
		if (id == R.id.trading_currency) {
			selectTradingCurrency(selectedId);
		}
	}

	@Override
	public void onSelected(int id, List<? extends MultiChoiceItem> items) {
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		if (id == R.id.trading_currency) {
			List<Currency> currencies = db.getAllCurrenciesList();
			// trading currency can't be self
			for (int i=0; i<currencies.size(); i++) {
				if (currencies.get(i).id == currency.id) {
					currencies.remove(i);
					break;
				}
			}
			currencies.add(0, currencyNone);
			ListAdapter adapter = TransactionUtils.createCurrencyAdapter(this, currencies);
			int selectedPos = MyEntity.indexOf(currencies, selectedTradingCurrency);
			x.selectItemId(this, R.id.trading_currency, R.string.trading_currency, adapter, selectedPos);
		}
	}
}
