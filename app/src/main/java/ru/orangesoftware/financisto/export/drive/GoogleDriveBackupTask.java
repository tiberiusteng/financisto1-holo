package ru.orangesoftware.financisto.export.drive;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;

import ru.orangesoftware.financisto.backup.DatabaseExport;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.export.ImportExportAsyncTask;

public class GoogleDriveBackupTask extends ImportExportAsyncTask {

    public GoogleDriveBackupTask(Activity mainActivity, ProgressDialog dialog) {
        super(mainActivity, dialog);
    }

    @Override
    protected Object work(Context context, DatabaseAdapter db, String... params) throws Exception {
        DatabaseExport export = new DatabaseExport(context, db.db(), true);
        String backupFileName = export.export();
        doForceUploadToGoogleDrive(context, backupFileName);
        return backupFileName;
    }

    @Override
    protected String getSuccessMessage(Object result) {
        return String.valueOf(result);
    }

}
