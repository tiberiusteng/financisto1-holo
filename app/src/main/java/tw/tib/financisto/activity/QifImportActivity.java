/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package tw.tib.financisto.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.widget.*;
import tw.tib.financisto.R;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.model.MultiChoiceItem;

import java.util.List;

public class QifImportActivity extends AbstractImportActivity implements ActivityLayoutListener {

    public static final String QIF_IMPORT_DATE_FORMAT = "QIF_IMPORT_DATE_FORMAT";
    public static final String QIF_IMPORT_URI = "QIF_IMPORT_URI";
    public static final String QIF_IMPORT_CURRENCY = "QIF_IMPORT_CURRENCY";

    private DatabaseAdapter db;

    public QifImportActivity() {
        super(R.layout.qif_import);
    }

    @Override
    protected void internalOnCreate() {
        db = new DatabaseAdapter(this);
        db.open();

        Spinner currencySpinner = (Spinner)findViewById(R.id.spinnerCurrency);
        Cursor currencyCursor = db.getAllCurrencies("name");
        startManagingCursor(currencyCursor);
        SimpleCursorAdapter currencyAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item, currencyCursor,
                new String[]{"e_name"}, new int[]{android.R.id.text1});
        currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        currencySpinner.setAdapter(currencyAdapter);

        Button bOk = (Button) findViewById(R.id.bOK);
        bOk.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (edFilename.getText().toString().equals("")) {
                    Toast.makeText(QifImportActivity.this, R.string.select_filename, Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent data = new Intent();
                updateResultIntentFromUi(data);
                setResult(RESULT_OK, data);
                finish();
            }
        });

        Button bCancel = (Button) findViewById(R.id.bCancel);
        bCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }

    @Override
    public void onSelected(int id, List<? extends MultiChoiceItem> items) {
    }

    @Override
    public void onSelectedPos(int id, int selectedPos) {
    }

    @Override
    public void onSelectedId(int id, long selectedId) {
    }

    @Override
    public void onClick(View view) {
    }

    @Override
    protected void updateResultIntentFromUi(Intent data) {
        Spinner currencySpinner = (Spinner)findViewById(R.id.spinnerCurrency);
        Spinner dateFormats = (Spinner)findViewById(R.id.spinnerDateFormats);
        data.putExtra(QIF_IMPORT_DATE_FORMAT, dateFormats.getSelectedItemPosition());
        data.putExtra(QIF_IMPORT_URI, importFileUri.toString());
        data.putExtra(QIF_IMPORT_CURRENCY, currencySpinner.getSelectedItemId());
    }

    @Override
    protected void savePreferences() {
        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
        Spinner dateFormats = (Spinner) findViewById(R.id.spinnerDateFormats);
        Spinner currencySpinner = (Spinner)findViewById(R.id.spinnerCurrency);
        editor.putInt(QIF_IMPORT_DATE_FORMAT, dateFormats.getSelectedItemPosition());
        editor.putString(QIF_IMPORT_URI, importFileUri.toString());
        editor.putLong(QIF_IMPORT_CURRENCY, currencySpinner.getSelectedItemId());
        editor.apply();
    }

    @Override
    protected void restorePreferences() {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        Spinner dateFormats = findViewById(R.id.spinnerDateFormats);
        dateFormats.setSelection(preferences.getInt(QIF_IMPORT_DATE_FORMAT, 0));

        edFilename = findViewById(R.id.edFilename);
        importFileUri = Uri.parse(preferences.getString(QIF_IMPORT_URI, ""));
        String filePath = importFileUri.getPath();
        if (filePath != null) {
            edFilename.setText(filePath.substring(filePath.lastIndexOf("/") + 1));
        }

        long currencyId = preferences.getLong(QIF_IMPORT_CURRENCY, 0);
        Spinner currencySpinner = findViewById(R.id.spinnerCurrency);
        int count = currencySpinner.getCount();
        for (int i=0; i<count; i++) {
            if (currencyId == currencySpinner.getItemIdAtPosition(i)) {
                currencySpinner.setSelection(i);
                break;
            }
        }
    }

}
