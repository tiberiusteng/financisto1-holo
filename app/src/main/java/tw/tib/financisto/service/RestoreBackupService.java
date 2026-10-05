package tw.tib.financisto.service;

import static java.lang.String.format;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.documentfile.provider.DocumentFile;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import tw.tib.financisto.R;
import tw.tib.financisto.activity.AccountWidget;
import tw.tib.financisto.backup.DatabaseImport;
import tw.tib.financisto.bus.GreenRobotBus_;
import tw.tib.financisto.bus.RefreshCurrentTab;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.export.Export;

/**
 * Restores the database from a backup file in the configured backup folder without any UI.
 * Replaces all data, so the service is protected by the DUMP permission: adb shell holds it,
 * regular apps cannot get it. Runs in the foreground: a background service gets frozen
 * by the system before a large restore finishes.
 *
 * adb shell am start-foreground-service -n tw.tib.financisto/.service.RestoreBackupService \
 *     -a tw.tib.financisto.RESTORE_BACKUP --es FILE_NAME 20261005_110007_338.backup
 */
public class RestoreBackupService extends Service {
    private static final String TAG = "RestoreBackupService";
    private static final int NOTIFICATION_ID = 0x5e570;

    public static final String ACTION_RESTORE_BACKUP = "tw.tib.financisto.RESTORE_BACKUP";
    public static final String FILE_NAME = "FILE_NAME";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground();
        executor.execute(() -> {
            try {
                restore(intent);
            } finally {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
                stopSelf(startId);
            }
        });
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startForeground() {
        NotificationChannelService.initialize(this);
        Notification notification = new NotificationCompat.Builder(this, NotificationChannelService.TRANSACTIONS_CHANNEL)
                .setSmallIcon(R.mipmap.a_icon_notify)
                .setContentTitle(getString(R.string.restore_database_inprogress))
                .setOngoing(true)
                .build();
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC : 0;
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type);
    }

    private void restore(Intent intent) {
        if (intent == null || !ACTION_RESTORE_BACKUP.equals(intent.getAction())) {
            return;
        }
        String fileName = intent.getStringExtra(FILE_NAME);
        if (fileName == null || fileName.trim().isEmpty()) {
            Log.e(TAG, "Missing FILE_NAME — restore skipped");
            return;
        }

        DocumentFile backupFolder = DocumentFile.fromTreeUri(this, Uri.parse(Export.getBackupFolder(this)));
        DocumentFile backupFile = backupFolder != null ? backupFolder.findFile(fileName.trim()) : null;
        if (backupFile == null || !backupFile.isFile()) {
            Log.e(TAG, format("Backup file not found in backup folder: %s — restore skipped", fileName));
            return;
        }

        DatabaseAdapter db = new DatabaseAdapter(this);
        db.open();
        try {
            DatabaseImport.createFromFileBackup(this, db, backupFile.getUri()).importDatabase();
            Log.i(TAG, format("Restored database from %s", backupFile.getUri()));
        } catch (Exception e) {
            Log.e(TAG, format("Unable to restore database from %s", backupFile.getUri()), e);
            return;
        } finally {
            db.close();
        }

        GreenRobotBus_.getInstance_(this).post(new RefreshCurrentTab());
        AccountWidget.updateWidgets(this);
    }
}
