package tw.tib.financisto.export.csv;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;

import tw.tib.financisto.export.ImportExportAsyncTask;
import tw.tib.financisto.db.DatabaseAdapter;

public class CsvExportTask extends ImportExportAsyncTask {

	private final CsvExportOptions options;

	public CsvExportTask(Activity context, ProgressDialog dialog, CsvExportOptions options) {
		super(context, dialog);
		this.options = options;
	}

	@Override
	protected Object work(Context context, DatabaseAdapter db, Uri...params) throws Exception {
		CsvExport export = new CsvExport(context, db, options);
		Uri backupFileUri = export.export();
		if (options.uploadToDropbox) {
			doUploadToDropbox(context, backupFileUri);
		}
		if (options.uploadToGDrive) {
			doForceUploadToGoogleDrive(context, backupFileUri);
		}
		return backupFileUri;
	}

	@Override
	protected String getSuccessMessage(Object result) {
		return String.valueOf(result);
	}

}
