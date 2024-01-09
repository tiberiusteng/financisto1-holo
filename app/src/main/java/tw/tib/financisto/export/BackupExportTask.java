package tw.tib.financisto.export;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;

import tw.tib.financisto.backup.DatabaseExport;
import tw.tib.financisto.db.DatabaseAdapter;

public class BackupExportTask extends ImportExportAsyncTask {

    public final boolean uploadOnline;

    public volatile Uri backupFileUri;
	
	public BackupExportTask(Context context, ProgressDialog dialog, boolean uploadOnline) {
		super(context, dialog);
        this.uploadOnline = uploadOnline;
	}
	
	@Override
	protected Object work(Context context, DatabaseAdapter db, Uri...params) throws Exception {
		DatabaseExport export = new DatabaseExport(context, db.db(), true);
        backupFileUri = export.export();
        if (backupFileUri != null && uploadOnline) {
            doUploadToDropbox(context, backupFileUri);
			doUploadToGoogleDrive(context, backupFileUri);
        }
        return backupFileUri;
	}
}