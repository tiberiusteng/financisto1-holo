package tw.tib.financisto.export.drive;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;

import java.util.List;

import tw.tib.financisto.R;
import tw.tib.financisto.bus.GreenRobotBus_;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.export.ImportExportAsyncTask;
import tw.tib.financisto.export.ImportExportAsyncTaskListener;
import tw.tib.financisto.export.ImportExportException;

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
    protected Object work(Context context, DatabaseAdapter db, Uri... params) throws Exception {
        try {
            GoogleDriveRESTClient client = new GoogleDriveRESTClient(context);
            List<GoogleDriveFileInfo> files = client.listFiles();
            return files.toArray(new GoogleDriveFileInfo[files.size()]);
        } catch (Exception e) {
            throw new ImportExportException(R.string.google_drive_list_files_failed);
        }
    }

}
