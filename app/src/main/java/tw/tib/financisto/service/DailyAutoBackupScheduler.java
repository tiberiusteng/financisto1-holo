/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package tw.tib.financisto.service;

import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import tw.tib.financisto.utils.MyPreferences;
import tw.tib.financisto.worker.AutoBackupWorker;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 12/16/11 12:54 AM
 */
public class DailyAutoBackupScheduler {

    private final int hh;
    private final int mm;
    private final long now;

    private String TAG;

    public static void scheduleNextAutoBackup(Context context) {
        if (MyPreferences.isAutoBackupEnabled(context)) {
            int hhmm = MyPreferences.getAutoBackupTime(context);
            int hh = hhmm/100;
            int mm = hhmm - 100*hh;
            new DailyAutoBackupScheduler(hh, mm, System.currentTimeMillis()).scheduleBackup(context);
        }
        else {
            WorkManager.getInstance(context)
                    .cancelUniqueWork(AutoBackupWorker.WORK_NAME);
        }
    }

    DailyAutoBackupScheduler(int hh, int mm, long now) {
        this.hh = hh;
        this.mm = mm;
        this.now = now;

        TAG = getClass().getSimpleName();
    }

    private void scheduleBackup(Context context) {
        Date scheduledTime = getScheduledTime();

        long initialDelay = scheduledTime.getTime() - Calendar.getInstance().getTime().getTime();

        Log.i(TAG, "Initial delay: " + initialDelay + " ms");

        var builder = new PeriodicWorkRequest.Builder(AutoBackupWorker.class,
                initialDelay, TimeUnit.MILLISECONDS, 1, TimeUnit.HOURS);

        if (MyPreferences.isDropboxUploadAutoBackups(context)
                || MyPreferences.isGoogleDriveUploadAutoBackups(context))
        {
            builder.setConstraints(new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED).build());
        }

        PeriodicWorkRequest backupWorkRequest = builder.build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                AutoBackupWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                backupWorkRequest);

        Log.i(TAG, "Next auto-backup scheduled at " + scheduledTime);
    }

    Date getScheduledTime() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(now);
        c.set(Calendar.HOUR_OF_DAY, hh);
        c.set(Calendar.MINUTE, mm);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        if (c.getTimeInMillis() < (now + (2 * 3600 * 1000))) {
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        return c.getTime();
    }

}
