/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package tw.tib.financisto.service;

import android.content.Context;
import android.util.Log;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Operation;
import androidx.work.WorkManager;

import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.model.RestoredTransaction;
import tw.tib.financisto.model.SystemAttribute;
import tw.tib.financisto.model.TransactionAttributeInfo;
import tw.tib.financisto.model.TransactionInfo;
import tw.tib.financisto.recur.DateRecurrenceIterator;
import tw.tib.financisto.recur.Recurrence;
import tw.tib.financisto.utils.MyPreferences;
import tw.tib.financisto.worker.ScheduleTxWorker;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class RecurrenceScheduler {

    private static final String TAG = "RecurrenceScheduler";
    private static final Date NULL_DATE = new Date(0);
    private static final int MAX_RESTORED = 1000;

    private final DatabaseAdapter db;

    public RecurrenceScheduler(DatabaseAdapter db) {
        this.db = db;
    }

    public int scheduleAll(Context context) {
        long now = System.currentTimeMillis();
        int restoredTransactionsCount = 0;
        if (MyPreferences.isRestoreMissedScheduledTransactions(context)) {
            restoredTransactionsCount = restoreMissedSchedules(now);
            // all transactions up to and including now has already been restored
            now += 1000;
        }
        scheduleAll(context, now);
        return restoredTransactionsCount;
    }

    public TransactionInfo scheduleOne(Context context, long scheduledTransactionId, long timestamp) {
        Log.i(TAG, "scheduleOne called with txId=" + scheduledTransactionId + ", timestamp=" + timestamp);
        TransactionInfo transaction = db.getTransactionInfo(scheduledTransactionId);
        if (transaction != null) {
            long transactionId = duplicateTransactionFromTemplate(transaction, timestamp);
            boolean hasBeenRescheduled = rescheduleTransaction(context, transaction, timestamp);
            if (!hasBeenRescheduled) {
                deleteTransactionIfNeeded(transaction);
                Log.i(TAG, "Expired transaction "+transaction.id+" has been deleted");
            }
            transaction.id = transactionId;
            return transaction;
        }
        return null;
    }

    private void deleteTransactionIfNeeded(TransactionInfo transaction) {
        TransactionAttributeInfo a = db.getSystemAttributeForTransaction(SystemAttribute.DELETE_AFTER_EXPIRED, transaction.id);
        if (a != null && Boolean.valueOf(a.value)) {
            db.deleteTransaction(transaction.id);
        }
    }

    /**
     * Restores missed scheduled transactions on backup and on phone restart
     * @param now current time
     * @return restored transactions count
     */
    private int restoreMissedSchedules(long now) {
        try {
            List<RestoredTransaction> restored = getMissedSchedules(now);
            if (restored.size() > 0) {
                db.storeMissedSchedules(restored, now);
                Log.i(TAG, "["+restored.size()+"] scheduled transactions have been restored:");
                for (int i=0; i<10 && i<restored.size(); i++) {
                    RestoredTransaction rt = restored.get(i);
                    Log.i(TAG, rt.transactionId+" at "+rt.dateTime);
                }
                return restored.size();
            }
        } catch (Exception ex) {
            // eat all exceptions
            Log.e(TAG, "Unexpected error while restoring schedules", ex);
        }
        return 0;
    }

    private long duplicateTransactionFromTemplate(TransactionInfo transaction, long timestamp) {
        return db.duplicateTransactionWithTimestamp(transaction.id, timestamp);
    }

    public List<RestoredTransaction> getMissedSchedules(long now) {
        long t0 = System.currentTimeMillis();
        try {
            Date endDate = new Date(now);
            List<RestoredTransaction> restored = new ArrayList<RestoredTransaction>();
            ArrayList<TransactionInfo> list = db.getAllScheduledTransactions();
            for (TransactionInfo t : list) {
                if (t.recurrence != null) {
                    long lastRecurrence = t.lastRecurrence;
                    if (lastRecurrence > 0) {
                        // move lastRecurrence time by 1 sec into future to not trigger the same time again
                        DateRecurrenceIterator ri = createIterator(t.recurrence, lastRecurrence+1000);
                        while (ri.hasNext()) {
                            Date nextDate = ri.next();
                            if (nextDate.after(endDate)) {
                                break;
                            }
                            addRestoredTransaction(restored, t, nextDate);
                        }
                    }
                } else {
                    Date nextDate = new Date(t.dateTime);
                    if (nextDate.before(endDate)) {
                        addRestoredTransaction(restored, t, nextDate);
                    }
                }
            }
            if (restored.size() > MAX_RESTORED) {
                Collections.sort(restored, new Comparator<RestoredTransaction>(){
                    @Override
                    public int compare(RestoredTransaction t0, RestoredTransaction t1) {
                        return t1.dateTime.compareTo(t0.dateTime);
                    }
                });
                restored = restored.subList(0, MAX_RESTORED);
            }
            return restored;
        } finally {
            Log.i(TAG, "getSortedSchedules="+(System.currentTimeMillis()-t0)+"ms");
        }
    }

    private void addRestoredTransaction(List<RestoredTransaction> restored,
                                        TransactionInfo t, Date nextDate) {
        RestoredTransaction rt = new RestoredTransaction(t.id, nextDate);
        restored.add(rt);
    }

    public ArrayList<TransactionInfo> getSortedSchedules(long now) {
        long t0 = System.currentTimeMillis();
        try {
            ArrayList<TransactionInfo> list = db.getAllScheduledTransactions();
            Log.i(TAG, "Got "+list.size()+" scheduled transactions");
            calculateNextScheduleDateForAllTransactions(list, now);
            sortTransactionsByScheduleDate(list, now);
            return list;
        } finally {
            Log.i(TAG, "getSortedSchedules="+(System.currentTimeMillis()-t0)+"ms");
        }
    }

    public ArrayList<TransactionInfo> scheduleAll(Context context, long now) {
        cancelAll(context);

        ArrayList<TransactionInfo> scheduled = getSortedSchedules(now);
        for (TransactionInfo transaction : scheduled) {
            scheduleWork(context, transaction, now);
        }
        return scheduled;
    }

    public Operation cancelAll(Context context) {
        return WorkManager.getInstance(context).cancelAllWorkByTag(ScheduleTxWorker.WORK_TAG);
    }

    public boolean scheduleWork(Context context, TransactionInfo transaction, long now) {
        if (shouldSchedule(transaction, now)) {
            Date scheduleTime = transaction.nextDateTime;

            long initialDelay = scheduleTime.getTime() - System.currentTimeMillis();

            var workRequest = new OneTimeWorkRequest.Builder(ScheduleTxWorker.class)
                    .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                    .addTag(ScheduleTxWorker.WORK_TAG)
                    .addTag(ScheduleTxWorker.WORK_NAME_PREFIX + transaction.id)
                    .setInputData(new Data.Builder()
                            .putLong(ScheduleTxWorker.TX_ID, transaction.id)
                            .putLong(ScheduleTxWorker.TX_TIME, scheduleTime.getTime())
                            .build())
                    .build();

            WorkManager.getInstance(context).enqueue(workRequest);

            Log.i(TAG, "Scheduling work for "+transaction.id+" at "+scheduleTime+" initial delay "+initialDelay);
            return true;
        }
        Log.i(TAG, "Transactions "+transaction.id+" with next date/time "+transaction.nextDateTime+" is not selected for schedule");
        return false;
    }

    public boolean rescheduleTransaction(Context context, TransactionInfo transaction, long timestamp) {
        if (transaction.recurrence != null) {
            long now = timestamp+1000;
            calculateAndSetNextDateTimeOnTransaction(transaction, now);
            return scheduleWork(context, transaction, now);
        }
        return false;
    }

    private boolean shouldSchedule(TransactionInfo transaction, long now) {
        return transaction.nextDateTime != null && now < transaction.nextDateTime.getTime();
    }

    public void cancelPendingWorkForSchedule(Context context, long transactionId) {
        Log.i(TAG, "Cancelling pending work for "+transactionId);
        WorkManager.getInstance(context).cancelAllWorkByTag(ScheduleTxWorker.WORK_NAME_PREFIX + transactionId);
    }

    /**
     * Correct order by nextDateTime:
     * 2010-12-01
     * 2010-12-02
     * 2010-11-23 <- today
     * 2010-11-11
     * 2010-10-08
     * NULL
     */
    public static class RecurrenceComparator implements Comparator<TransactionInfo> {

        private final Date today;

        public RecurrenceComparator(long now) {
            this.today = new Date(now);
        }

        @Override
        public int compare(TransactionInfo o1, TransactionInfo o2) {
            Date d1 = o1 != null ? (o1.nextDateTime != null ? o1.nextDateTime : NULL_DATE) : NULL_DATE;
            Date d2 = o2 != null ? (o2.nextDateTime != null ? o2.nextDateTime : NULL_DATE) : NULL_DATE;
            if (d1.after(today)) {
                if (d2.after(today)) {
                    return d1.compareTo(d2);
                } else {
                    return -1;
                }
            } else {
                if (d2.after(today)) {
                    return 1;
                } else {
                    return -d1.compareTo(d2);
                }
            }
        }
    }

    private void sortTransactionsByScheduleDate(ArrayList<TransactionInfo> list, long now) {
        Collections.sort(list, new RecurrenceComparator(now));
    }

    private long calculateNextScheduleDateForAllTransactions(ArrayList<TransactionInfo> list, long now) {
        for (TransactionInfo t : list) {
            calculateAndSetNextDateTimeOnTransaction(t, now);
        }
        return now;
    }

    private void calculateAndSetNextDateTimeOnTransaction(TransactionInfo t, long now) {
        if (t.recurrence != null) {
            t.nextDateTime = calculateNextDate(t.recurrence, now);
        } else {
            t.nextDateTime = new Date(t.dateTime);
        }
        Log.i(TAG, "Calculated schedule time for "+t.id+" is "+t.nextDateTime);
    }

    public Date calculateNextDate(String recurrence, long now) {
        try {
            DateRecurrenceIterator ri = createIterator(recurrence, now);
            if (ri.hasNext()) {
                return ri.next();
            }
        } catch (Exception ex) {
            Log.e(TAG, "Unable to calculate next date for "+recurrence+" at "+now);
        }
        return null;
    }

    private DateRecurrenceIterator createIterator(String recurrence, long now) {
        Recurrence r = Recurrence.parse(recurrence);
        Date advanceDate = new Date(now);
        return r.createIterator(advanceDate);
    }

}
