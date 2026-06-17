package tw.tib.financisto.utils;

import android.content.Context;

import java.util.Date;

import tw.tib.financisto.R;
import tw.tib.financisto.datetime.DateUtils;

public class IntegrityCheckAutobackup implements IntegrityCheck {

    private final Context context;
    private final long threshold;

    public IntegrityCheckAutobackup(Context context, long threshold) {
        this.context = context;
        this.threshold = threshold;
    }

    @Override
    public Result check() {
        if (MyPreferences.isAutoBackupEnabled()) {
            if (MyPreferences.isAutoBackupWarningEnabled()) {
                MyPreferences.AutobackupStatus status = MyPreferences.getAutobackupStatus();
                if (status.notify) {
                    MyPreferences.notifyAutobackupSucceeded();
                    return new Result(Level.ERROR,
                            context.getString(R.string.autobackup_failed_message,
                                    DateUtils.getTimeFormat(context).format(new Date(status.timestamp)),
                                    status.errorMessage));
                }
            }
        } else {
            if (MyPreferences.isAutoBackupReminderEnabled()) {
                long lastCheck = MyPreferences.getLastAutobackupCheck();
                if (lastCheck == 0) {
                    MyPreferences.updateLastAutobackupCheck();
                } else {
                    long delta = System.currentTimeMillis() - lastCheck;
                    if (delta > threshold) {
                        MyPreferences.updateLastAutobackupCheck();
                        return new Result(Level.INFO, context.getString(R.string.auto_backup_is_not_enabled));
                    }
                }
            }
        }
        return Result.OK;
    }

}
