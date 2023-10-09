/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package tw.tib.financisto.export.csv;

import android.content.Intent;

import tw.tib.financisto.activity.CsvExportActivity;
import tw.tib.financisto.filter.WhereFilter;
import tw.tib.financisto.model.Currency;
import tw.tib.financisto.utils.CurrencyCache;
import tw.tib.financisto.utils.CurrencyExportPreferences;

import java.text.NumberFormat;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 7/10/11 7:29 PM
 */
public class CsvExportOptions {

    public final NumberFormat amountFormat;
    public final char fieldSeparator;
    public final boolean includeHeader;
    public final boolean exportSplits;
    public final boolean uploadToDropbox;
    public final boolean uploadToGDrive;
    public final WhereFilter filter;
    public final boolean writeUtfBom;

    public CsvExportOptions(Currency currency, char fieldSeparator, boolean includeHeader,
                            boolean exportSplits, boolean uploadToDropbox, boolean uploadToGDrive,
                            WhereFilter filter, boolean writeUtfBom) {
        this.filter = filter;
        this.amountFormat = CurrencyCache.createCurrencyFormat(currency);
        this.fieldSeparator = fieldSeparator;
        this.includeHeader = includeHeader;
        this.exportSplits = exportSplits;
        this.uploadToDropbox = uploadToDropbox;
        this.uploadToGDrive = uploadToGDrive;
        this.writeUtfBom = writeUtfBom;
    }

    public static CsvExportOptions fromIntent(Intent data) {
        WhereFilter filter = WhereFilter.fromIntent(data);
        Currency currency = CurrencyExportPreferences.fromIntent(data, "csv");
        char fieldSeparator = data.getCharExtra(CsvExportActivity.CSV_EXPORT_FIELD_SEPARATOR, ',');
        boolean includeHeader = data.getBooleanExtra(CsvExportActivity.CSV_EXPORT_INCLUDE_HEADER, true);
        boolean exportSplits = data.getBooleanExtra(CsvExportActivity.CSV_EXPORT_SPLITS, false);
        boolean uploadToDropbox = data.getBooleanExtra(CsvExportActivity.CSV_EXPORT_UPLOAD_TO_DROPBOX, false);
        boolean uploadToGDrive = data.getBooleanExtra(CsvExportActivity.CSV_EXPORT_UPLOAD_TO_GDRIVE, false);
        return new CsvExportOptions(currency, fieldSeparator, includeHeader, exportSplits, uploadToDropbox, uploadToGDrive, filter, true);
    }

}
