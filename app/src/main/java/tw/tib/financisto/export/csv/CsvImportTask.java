/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package tw.tib.financisto.export.csv;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import tw.tib.financisto.R;
import tw.tib.financisto.export.ImportExportAsyncTask;
import tw.tib.financisto.export.ImportExportException;
import tw.tib.financisto.export.ProgressListener;
import tw.tib.financisto.db.DatabaseAdapter;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 11/7/11 10:45 PM
 */
public class CsvImportTask extends ImportExportAsyncTask {

    private final CsvImportOptions options;

    public CsvImportTask(final Activity activity, ProgressDialog dialog, CsvImportOptions options) {
        super(activity, dialog);
        this.options = options;
    }

    @Override
    protected Object work(Context context, DatabaseAdapter db, Uri... params) throws Exception {
        try {
            CsvImport csvimport = new CsvImport(context, db, options);
            csvimport.setProgressListener(new ProgressListener() {
                @Override
                public void onProgress(int percentage) {
                    publishProgress(String.valueOf(percentage));
                }
            });
            return csvimport.doImport();
        } catch (Exception e) {
            Log.e("Financisto", "Csv import error", e);
            if (e instanceof ImportExportException)
                throw e;
            String message = e.getMessage();
            if (message == null)
                throw new ImportExportException(R.string.csv_import_error, e);
            else if (message.equals("Import file not found"))
                throw new ImportExportException(R.string.import_file_not_found);
            else if (message.equals("Unknown category in import line"))
                throw new ImportExportException(R.string.import_unknown_category);
            else if (message.equals("Unknown project in import line"))
                throw new ImportExportException(R.string.import_unknown_project);
            else if (message.equals("Wrong currency in import line"))
                throw new ImportExportException(R.string.import_wrong_currency);
            else if (message.equals("IllegalArgumentException"))
                throw new ImportExportException(R.string.import_illegal_argument_exception);
            else if (message.equals("ParseException"))
                throw new ImportExportException(R.string.import_parse_error, e.getCause());
            else if (e instanceof SecurityException)
                throw new ImportExportException(R.string.file_import_permission);
            else
                throw new ImportExportException(R.string.csv_import_error, e);
        }

    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        dialog.setMessage(context.getString(R.string.csv_import_inprogress_update, values[0]));
    }

}
