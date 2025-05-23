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

import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.CheckBox;
import android.widget.Spinner;
import tw.tib.financisto.R;
import tw.tib.financisto.utils.CurrencyExportPreferences;

public class CsvExportActivity extends AbstractExportActivity {
	
	public static final String CSV_EXPORT_FIELD_SEPARATOR = "CSV_EXPORT_FIELD_SEPARATOR";
	public static final String CSV_EXPORT_INCLUDE_HEADER = "CSV_EXPORT_INCLUDE_HEADER";
    public static final String CSV_EXPORT_INCLUDE_TX_STATUS = "CSV_EXPORT_INCLUDE_TX_STATUS";
    public static final String CSV_EXPORT_SPLITS = "CSV_EXPORT_SPLITS";
    public static final String CSV_EXPORT_SPLIT_PARENTS = "CSV_EXPORT_SPLIT_PARENTS";
    public static final String CSV_EXPORT_TX_IDS = "CSV_EXPORT_TX_IDS";
    public static final String CSV_EXPORT_ATTRIBUTES = "CSV_EXPORT_ATTRIBUTES";
    public static final String CSV_EXPORT_RUNNING_BALANCE = "CSV_EXPORT_RUNNING_BALANCE";
    public static final String CSV_EXPORT_UPLOAD_TO_DROPBOX = "CSV_EXPORT_UPLOAD_TO_DROPBOX";
    public static final String CSV_EXPORT_UPLOAD_TO_GDRIVE = "CSV_EXPORT_UPLOAD_TO_GDRIVE";

    private final CurrencyExportPreferences currencyPreferences = new CurrencyExportPreferences("csv");

    private Spinner fieldSeparators;
    private CheckBox includeHeader;
    private CheckBox exportSplits;
    private CheckBox exportSplitParents;
    private CheckBox exportTxIDs;
    private CheckBox exportAttributes;
    private CheckBox exportRunningBalance;
    private CheckBox includeTxStatus;
    private CheckBox uploadToDropbox;
    private CheckBox uploadToGDrive;

    public CsvExportActivity() {
        super(R.layout.csv_export);
    }

    @Override
    protected void internalOnCreate() {
        fieldSeparators = (Spinner)findViewById(R.id.spinnerFieldSeparator);
        includeHeader = (CheckBox)findViewById(R.id.checkboxIncludeHeader);
        exportSplits = (CheckBox)findViewById(R.id.checkboxExportSplits);
        exportSplitParents = (CheckBox)findViewById(R.id.checkboxExportSplitParents);
        exportTxIDs = (CheckBox)findViewById(R.id.checkboxExportTxIDs);
        exportAttributes = (CheckBox)findViewById(R.id.checkboxExportAttributes);
        exportRunningBalance = (CheckBox)findViewById(R.id.checkboxExportRunningBalance);
        includeHeader = (CheckBox)findViewById(R.id.checkboxIncludeHeader);
        includeTxStatus = (CheckBox)findViewById(R.id.checkboxIncludeTxStatus);
        uploadToDropbox = (CheckBox)findViewById(R.id.checkboxUploadToDropbox);
        uploadToGDrive = (CheckBox)findViewById(R.id.checkboxUploadToGDrive);
    }

    @Override
    protected void updateResultIntentFromUi(Intent data) {
        currencyPreferences.updateIntentFromUI(this, data);
        data.putExtra(CSV_EXPORT_FIELD_SEPARATOR, fieldSeparators.getSelectedItem().toString().charAt(1));
        data.putExtra(CSV_EXPORT_INCLUDE_HEADER, includeHeader.isChecked());
        data.putExtra(CSV_EXPORT_INCLUDE_TX_STATUS, includeTxStatus.isChecked());
        data.putExtra(CSV_EXPORT_SPLITS, exportSplits.isChecked());
        data.putExtra(CSV_EXPORT_SPLIT_PARENTS, exportSplitParents.isChecked());
        data.putExtra(CSV_EXPORT_TX_IDS, exportTxIDs.isChecked());
        data.putExtra(CSV_EXPORT_ATTRIBUTES, exportAttributes.isChecked());
        data.putExtra(CSV_EXPORT_RUNNING_BALANCE, exportRunningBalance.isChecked());
        data.putExtra(CSV_EXPORT_UPLOAD_TO_DROPBOX, uploadToDropbox.isChecked());
        data.putExtra(CSV_EXPORT_UPLOAD_TO_GDRIVE, uploadToGDrive.isChecked());
    }

	protected void savePreferences() {
		SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
        currencyPreferences.savePreferences(this, editor);
		editor.putInt(CSV_EXPORT_FIELD_SEPARATOR, fieldSeparators.getSelectedItemPosition());
		editor.putBoolean(CSV_EXPORT_INCLUDE_HEADER, includeHeader.isChecked());
        editor.putBoolean(CSV_EXPORT_INCLUDE_TX_STATUS, includeTxStatus.isChecked());
        editor.putBoolean(CSV_EXPORT_SPLITS, exportSplits.isChecked());
        editor.putBoolean(CSV_EXPORT_SPLIT_PARENTS, exportSplitParents.isChecked());
        editor.putBoolean(CSV_EXPORT_TX_IDS, exportTxIDs.isChecked());
        editor.putBoolean(CSV_EXPORT_ATTRIBUTES, exportAttributes.isChecked());
        editor.putBoolean(CSV_EXPORT_RUNNING_BALANCE, exportRunningBalance.isChecked());
        editor.putBoolean(CSV_EXPORT_UPLOAD_TO_DROPBOX, uploadToDropbox.isChecked());
        editor.putBoolean(CSV_EXPORT_UPLOAD_TO_GDRIVE, uploadToGDrive.isChecked());
		editor.apply();
	}

    protected void restorePreferences() {
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        currencyPreferences.restorePreferences(this, prefs);
        fieldSeparators.setSelection(prefs.getInt(CSV_EXPORT_FIELD_SEPARATOR, 0));
		includeHeader.setChecked(prefs.getBoolean(CSV_EXPORT_INCLUDE_HEADER, true));
        includeTxStatus.setChecked(prefs.getBoolean(CSV_EXPORT_INCLUDE_TX_STATUS, false));
        exportSplits.setChecked(prefs.getBoolean(CSV_EXPORT_SPLITS, false));
        exportSplitParents.setChecked(prefs.getBoolean(CSV_EXPORT_SPLIT_PARENTS, false));
        exportTxIDs.setChecked(prefs.getBoolean(CSV_EXPORT_TX_IDS, false));
        exportAttributes.setChecked(prefs.getBoolean(CSV_EXPORT_ATTRIBUTES, false));
        exportRunningBalance.setChecked(prefs.getBoolean(CSV_EXPORT_RUNNING_BALANCE, false));
        uploadToDropbox.setChecked(prefs.getBoolean(CSV_EXPORT_UPLOAD_TO_DROPBOX, false));
        uploadToGDrive.setChecked(prefs.getBoolean(CSV_EXPORT_UPLOAD_TO_GDRIVE, false));
	}

}
