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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import tw.tib.financisto.R;
import tw.tib.financisto.backup.DatabaseExport;
import tw.tib.financisto.db.DatabaseAdapter;

/**
 * Creates a backup file in the configured backup folder without any UI.
 * Runs in the foreground, so it can be started while the app is in the background
 * or the phone is locked: Android refuses to start the plain IntentService
 * (CREATE_BACKUP) for a background app.
 *
 * adb shell am start-foreground-service -n tw.tib.financisto/.service.CreateBackupService \
 *     -a tw.tib.financisto.CREATE_BACKUP
 */
public class CreateBackupService extends Service {
    private static final String TAG = "CreateBackupService";
    private static final int NOTIFICATION_ID = 0x5e571;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground();
        executor.execute(() -> {
            try {
                createBackup(intent);
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
        Notification notification = new NotificationCompat.Builder(this, NotificationChannelService.DATA_SYNC_CHANNEL)
                .setSmallIcon(R.mipmap.a_icon_notify)
                .setContentTitle(getString(R.string.backup_database_inprogress))
                .setOngoing(true)
                .setProgress(0, 0, true)
                .build();
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC : 0;
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type);
    }

    private void createBackup(Intent intent) {
        if (intent == null || !IntentService.ACTION_CREATE_BACKUP.equals(intent.getAction())) {
            return;
        }

        DatabaseAdapter db = new DatabaseAdapter(this);
        db.open();
        try {
            Uri backupFileUri = new DatabaseExport(this, db.db(), true).export();
            Log.i(TAG, format("Created backup %s", backupFileUri));
        } catch (Exception e) {
            Log.e(TAG, "Unable to create backup", e);
        } finally {
            db.close();
        }
    }
}
