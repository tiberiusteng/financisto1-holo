package tw.tib.financisto.worker;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Calendar;
import java.util.Date;

import tw.tib.financisto.backup.DatabaseExport;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.export.Export;
import tw.tib.financisto.service.DailyAutoBackupScheduler;
import tw.tib.financisto.utils.MyPreferences;

public class AutoBackupWorker extends Worker {
    public static final String WORK_NAME = "AutoBackuo";
    public static final String SCHEDULE_TIME = "scheduleTime";

    private String TAG = "AutoBackupWorker";

    public AutoBackupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        TAG = getClass().getSimpleName();
    }

    @NonNull
    @Override
    public Result doWork() {
        StringBuilder log = new StringBuilder();
        Context context = getApplicationContext();
        Data args = getInputData();
        long scheduledTime = args.getLong(SCHEDULE_TIME, System.currentTimeMillis());
        DatabaseAdapter db = new DatabaseAdapter(context);
        db.open();

        try {
            long t0 = System.currentTimeMillis();
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(scheduledTime);
            Log.e(TAG, "Auto-backup started at " + new Date());
            log.append(String.format("Auto-backup started at %s for %s (%s)\n", t0, scheduledTime, c.getTime()));
            DatabaseExport export = new DatabaseExport(context, db.db(), true);
            Uri backupFileUri = export.export();
            boolean successful = true;
            if (MyPreferences.isDropboxUploadAutoBackups(context)) {
                try {
                    Export.uploadBackupFileToDropbox(context, backupFileUri);
                } catch (Exception e) {
                    Log.e(TAG, "Unable to upload auto-backup to Dropbox", e);
                    log.append("Unable to upload auto-backup to Dropbox\n").append(e);
                    MyPreferences.notifyAutobackupFailed(context, e);
                    successful = false;
                }
            }
            if (MyPreferences.isGoogleDriveUploadAutoBackups(context)) {
                try {
                    Export.uploadBackupFileToGoogleDrive(context, backupFileUri);
                } catch (Exception e) {
                    Log.e(TAG, "Unable to upload auto-backup to Google Drive", e);
                    log.append("Unable to upload auto-backup to Google Drive\n").append(e);
                    MyPreferences.notifyAutobackupFailed(context, e);
                    successful = false;
                }
            }
            Log.e(TAG, "Auto-backup completed in " + (System.currentTimeMillis() - t0) + "ms");
            log.append("Auto-backup completed in ").append(System.currentTimeMillis() - t0).append("ms");
            if (successful) {
                MyPreferences.notifyAutobackupSucceeded(context);
            }

        } catch (Exception e) {
            Log.e(TAG, "Auto-backup unsuccessful", e);
            MyPreferences.notifyAutobackupFailed(context, e);

            return Result.failure();
        }

        DailyAutoBackupScheduler.scheduleNextAutoBackup(context);

        return Result.success();
    }
}
