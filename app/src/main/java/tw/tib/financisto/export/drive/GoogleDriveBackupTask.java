package tw.tib.financisto.export.drive;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;

import tw.tib.financisto.backup.DatabaseExport;
import tw.tib.financisto.export.ImportExportAsyncTask;
import tw.tib.financisto.db.DatabaseAdapter;

public class GoogleDriveBackupTask extends ImportExportAsyncTask {

    public GoogleDriveBackupTask(Activity mainActivity, ProgressDialog dialog) {
        super(mainActivity, dialog);
    }

    @Override
    protected Object work(Context context, DatabaseAdapter db, Uri... params) throws Exception {
        DatabaseExport export = new DatabaseExport(context, db.db(), true);
        Uri backupFileUri = export.export();
        doForceUploadToGoogleDrive(context, backupFileUri);
        return backupFileUri;
    }

}
