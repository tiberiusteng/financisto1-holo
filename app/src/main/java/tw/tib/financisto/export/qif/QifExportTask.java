package tw.tib.financisto.export.qif;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;

import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.export.ImportExportAsyncTask;

public class QifExportTask extends ImportExportAsyncTask {

	private final QifExportOptions options;

	public QifExportTask(Activity context, ProgressDialog dialog, QifExportOptions options) {
		super(context, dialog);
		this.options = options;
	}

	@Override
	protected Object work(Context context, DatabaseAdapter db, Uri...params) throws Exception {
		QifExport qifExport = new QifExport(context, db, options);
		Uri backupFileUri = qifExport.export();
		if (options.uploadToDropbox) {
			doForceUploadToDropbox(context, backupFileUri);
		}
		if (options.uploadToGDrive) {
			doForceUploadToGoogleDrive(context, backupFileUri);
		}
		return backupFileUri;
	}

}
