package tw.tib.financisto.worker;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Date;

import tw.tib.financisto.backup.DatabaseExport;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.export.Export;
import tw.tib.financisto.utils.MyPreferences;

public class AutoBackupWorker extends Worker {
    public static final String WORK_NAME = "AutoBackuo";

    private String TAG;

    public AutoBackupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        TAG = getClass().getSimpleName();
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        DatabaseAdapter db = new DatabaseAdapter(context);
        db.open();

        try {
            long t0 = System.currentTimeMillis();
            Log.e(TAG, "Auto-backup started at " + new Date());
            DatabaseExport export = new DatabaseExport(context, db.db(), true);
            Uri backupFileUri = export.export();
            boolean successful = true;
            if (MyPreferences.isDropboxUploadAutoBackups(context)) {
                try {
                    Export.uploadBackupFileToDropbox(context, backupFileUri);
                } catch (Exception e) {
                    Log.e(TAG, "Unable to upload auto-backup to Dropbox", e);
                    MyPreferences.notifyAutobackupFailed(context, e);
                    successful = false;
                }
            }
            if (MyPreferences.isGoogleDriveUploadAutoBackups(context)) {
                try {
                    Export.uploadBackupFileToGoogleDrive(context, backupFileUri);
                } catch (Exception e) {
                    Log.e(TAG, "Unable to upload auto-backup to Google Drive", e);
                    MyPreferences.notifyAutobackupFailed(context, e);
                    successful = false;
                }
            }
            Log.e(TAG, "Auto-backup completed in " + (System.currentTimeMillis() - t0) + "ms");
            if (successful) {
                MyPreferences.notifyAutobackupSucceeded(context);
            }

        } catch (Exception e) {
            Log.e(TAG, "Auto-backup unsuccessful", e);
            MyPreferences.notifyAutobackupFailed(context, e);

            return Result.failure();
        }

        return Result.success();
    }
}
