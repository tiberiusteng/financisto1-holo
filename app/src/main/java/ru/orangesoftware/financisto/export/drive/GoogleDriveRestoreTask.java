package ru.orangesoftware.financisto.export.drive;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.backup.DatabaseImport;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.export.ImportExportAsyncTask;

public class GoogleDriveRestoreTask extends ImportExportAsyncTask {
    private final GoogleDriveFileInfo backupFile;

    public GoogleDriveRestoreTask(final Activity activity, ProgressDialog dialog, GoogleDriveFileInfo backupFile) {
        super(activity, dialog);
        this.backupFile = backupFile;
    }

    @Override
    protected Object work(Context context, DatabaseAdapter db, Uri... params) throws Exception {
        GoogleDriveRESTClient googleDriveRESTClient = new GoogleDriveRESTClient(context);
        DatabaseImport.createFromGoogleDriveBackup(context, db, googleDriveRESTClient, backupFile).importDatabase();
        return true;
    }

    @Override
    protected String getSuccessMessage(Object result) {
        return context.getString(R.string.restore_database_success);
    }

}
