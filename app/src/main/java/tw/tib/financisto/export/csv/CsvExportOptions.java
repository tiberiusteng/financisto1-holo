/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package tw.tib.financisto.export.csv;

import android.content.Intent;
import android.util.Log;

import gnu.trove.map.hash.TLongObjectHashMap;
import tw.tib.financisto.Application;
import tw.tib.financisto.activity.CsvExportActivity;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.filter.WhereFilter;
import tw.tib.financisto.model.Currency;
import tw.tib.financisto.utils.CurrencyCache;
import tw.tib.financisto.utils.CurrencyExportPreferences;

import java.text.Format;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 7/10/11 7:29 PM
 */
public class CsvExportOptions {
    private static final String TAG = "CsvExportOptions";

    public final Currency currency;
    public final Format amountFormat;
    public final boolean useCurrencySpecificDecimals;
    public final char fieldSeparator;
    public final boolean includeHeader;
    public final boolean includeTxStatus;
    public final boolean exportSplits;
    public final boolean exportSplitParents;
    public final boolean exportTxIDs;
    public final boolean exportAttributes;
    public final boolean exportRunningBalance;
    public final boolean exportTransferInSingleLine;
    public final boolean uploadToDropbox;
    public final boolean uploadToGDrive;
    public final WhereFilter filter;
    public final boolean writeUtfBom;
    public final TLongObjectHashMap<Format> currencyAmountFormat = new TLongObjectHashMap<>();

    public CsvExportOptions(Currency currency, char fieldSeparator, boolean includeHeader,
                            boolean includeTxStatus, boolean exportSplits, boolean exportSplitParents,
                            boolean exportTxIDs, boolean exportAttributes, boolean exportRunningBalance,
                            boolean exportTransferInSingleLine,
                            boolean uploadToDropbox, boolean uploadToGDrive,
                            WhereFilter filter, boolean writeUtfBom) {
        this.filter = filter;
        this.useCurrencySpecificDecimals = (currency.decimals == -1);
        this.currency = currency;
        this.amountFormat = CurrencyCache.createCurrencyFormat(currency);
        this.fieldSeparator = fieldSeparator;
        this.includeHeader = includeHeader;
        this.includeTxStatus = includeTxStatus;
        this.exportSplits = exportSplits;
        this.exportSplitParents = exportSplitParents;
        this.exportTxIDs = exportTxIDs;
        this.exportAttributes = exportAttributes;
        this.exportRunningBalance = exportRunningBalance;
        this.exportTransferInSingleLine = exportTransferInSingleLine;
        this.uploadToDropbox = uploadToDropbox;
        this.uploadToGDrive = uploadToGDrive;
        this.writeUtfBom = writeUtfBom;
    }

    public static CsvExportOptions fromIntent(Intent data) {
        WhereFilter filter = WhereFilter.fromIntent(data);
        Currency currency = CurrencyExportPreferences.fromIntent(data, "csv");
        Log.d(TAG, "currency decimals=" + currency.decimals);
        char fieldSeparator = data.getCharExtra(CsvExportActivity.CSV_EXPORT_FIELD_SEPARATOR, ',');
        boolean includeHeader = data.getBooleanExtra(CsvExportActivity.CSV_EXPORT_INCLUDE_HEADER, true);
        boolean includeTxStatus = data.getBooleanExtra(CsvExportActivity.CSV_EXPORT_INCLUDE_TX_STATUS, false);
        boolean exportSplits = data.getBooleanExtra(CsvExportActivity.CSV_EXPORT_SPLITS, false);
        boolean exportSplitParents = data.getBooleanExtra(CsvExportActivity.CSV_EXPORT_SPLIT_PARENTS, false);
        boolean exportTxIDs = data.getBooleanExtra(CsvExportActivity.CSV_EXPORT_TX_IDS, false);
        boolean exportAttributes = data.getBooleanExtra(CsvExportActivity.CSV_EXPORT_ATTRIBUTES, false);
        boolean exportRunningBalance = data.getBooleanExtra(CsvExportActivity.CSV_EXPORT_RUNNING_BALANCE, false);
        boolean exportTransferInSingleLine = data.getBooleanExtra(CsvExportActivity.CSV_EXPORT_TRANSFER_IN_SINGLE_LINE, false);
        boolean uploadToDropbox = data.getBooleanExtra(CsvExportActivity.CSV_EXPORT_UPLOAD_TO_DROPBOX, false);
        boolean uploadToGDrive = data.getBooleanExtra(CsvExportActivity.CSV_EXPORT_UPLOAD_TO_GDRIVE, false);
        return new CsvExportOptions(currency, fieldSeparator, includeHeader, includeTxStatus,
                exportSplits, exportSplitParents, exportTxIDs, exportAttributes, exportRunningBalance,
                exportTransferInSingleLine,
                uploadToDropbox, uploadToGDrive, filter, true);
    }

    public Format getCurrencyAmountFormat(long currencyId) {
        Format f = currencyAmountFormat.get(currencyId);
        if (f != null) {
            return f;
        }

        Currency c = CurrencyCache.getCurrency(currencyId);
        currency.decimals = c.decimals;

        f = CurrencyCache.createCurrencyFormat(currency);
        currencyAmountFormat.put(currencyId, f);
        return f;
    }
}
