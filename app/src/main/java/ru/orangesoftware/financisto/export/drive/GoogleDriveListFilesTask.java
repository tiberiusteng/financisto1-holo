package ru.orangesoftware.financisto.export.drive;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;

import java.util.List;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.bus.GreenRobotBus_;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.export.ImportExportAsyncTask;
import ru.orangesoftware.financisto.export.ImportExportAsyncTaskListener;
import ru.orangesoftware.financisto.export.ImportExportException;

public class GoogleDriveListFilesTask extends ImportExportAsyncTask {

    public GoogleDriveListFilesTask(final Activity context, ProgressDialog dialog) {
        super(context, dialog);
        setShowResultMessage(false);
        setListener(new ImportExportAsyncTaskListener() {
            @Override
            public void onCompleted(Object result) {
                GreenRobotBus_.getInstance_(context).post(new GoogleDriveFileList((GoogleDriveFileInfo[]) result));
            }
        });
    }

    @Override
    protected Object work(Context context, DatabaseAdapter db, String... params) throws Exception {
        try {
            GoogleDriveRESTClient client = new GoogleDriveRESTClient(context);
            List<GoogleDriveFileInfo> files = client.listFiles();
            return files.toArray(new GoogleDriveFileInfo[files.size()]);
        } catch (Exception e) {
            throw new ImportExportException(R.string.google_drive_list_files_failed);
        }
    }

    @Override
    protected String getSuccessMessage(Object result) {
        return null;
    }

}
