/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package tw.tib.financisto.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import tw.tib.financisto.R;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.model.Transaction;
import tw.tib.financisto.model.TransactionStatus;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 8/13/11 12:02 AM
 */
public class BlotterOperations {

    private static final int EDIT_TRANSACTION_REQUEST = 2;
	private static final int EDIT_TRANSFER_REQUEST = 4;

    private final Context context;
    private final BlotterOperationsCallback callback;
    private final DatabaseAdapter db;
    private final Transaction originalTransaction;
    private final Transaction targetTransaction;

    private boolean newFromTemplate = false;

    public BlotterOperations(Context context, BlotterOperationsCallback callback, DatabaseAdapter db, long transactionId) {
        this.context = context;
        this.callback = callback;
        this.db = db;
        this.originalTransaction = db.getTransaction(transactionId);
        if (this.originalTransaction.isSplitChild()) {
            this.targetTransaction = db.getTransaction(this.originalTransaction.parentId);
        } else {
            this.targetTransaction = this.originalTransaction;
        }
    }

    public BlotterOperations asNewFromTemplate() {
        newFromTemplate = true;
        return this;
    }

    public void editTransaction() {
        if (targetTransaction.isTransfer()) {
            startEditTransactionActivity(TransferActivity.class, EDIT_TRANSFER_REQUEST);
        } else {
            startEditTransactionActivity(TransactionActivity.class, EDIT_TRANSACTION_REQUEST);
        }
    }

    private void startEditTransactionActivity(Class<? extends Activity> activityClass, int requestCode) {
        Intent intent = new Intent(context, activityClass);
        intent.putExtra(AbstractTransactionActivity.TRAN_ID_EXTRA, targetTransaction.id);
        intent.putExtra(AbstractTransactionActivity.DUPLICATE_EXTRA, false);
        intent.putExtra(AbstractTransactionActivity.NEW_FROM_TEMPLATE_EXTRA, newFromTemplate);
        callback.startActivityForResult(intent, requestCode);
    }

    public void deleteTransaction() {
        int titleId = targetTransaction.isTemplate() ? R.string.delete_template_confirm
                : (originalTransaction.isSplitChild() ? R.string.delete_transaction_parent_confirm : R.string.delete_transaction_confirm);
        new AlertDialog.Builder(context)
                .setMessage(titleId)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        long transactionIdToDelete = targetTransaction.id;
                        db.deleteTransaction(transactionIdToDelete);
                        callback.afterDeletingTransaction(transactionIdToDelete);
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    public long duplicateTransaction(int multiplier) {
        long newId;
		if (multiplier > 1) {
			newId = db.duplicateTransactionWithMultiplier(targetTransaction.id, multiplier);
		} else {
			newId = db.duplicateTransaction(targetTransaction.id);
		}
        return newId;
    }

    public long duplicateTransactionKeepTime() {
        return db.duplicateTransactionKeepTimeInDay(targetTransaction.id);
    }

    public long duplicateTransactionKeepDateTime() {
        return db.duplicateTransactionKeepDateTime(targetTransaction.id);
    }

    public void duplicateAsTemplate() {
        db.duplicateTransactionAsTemplate(targetTransaction.id);
    }

    public void restoreTransaction() {
        db.updateTransactionStatus(targetTransaction.id, TransactionStatus.RS);
    }

    public void pendingTransaction() {
        db.updateTransactionStatus(targetTransaction.id, TransactionStatus.PN);
    }

    public void unreconcileTransaction() {
        db.updateTransactionStatus(targetTransaction.id, TransactionStatus.UR);
    }

    public void clearTransaction() {
        db.updateTransactionStatus(targetTransaction.id, TransactionStatus.CL);
    }

    public void reconcileTransaction() {
        db.updateTransactionStatus(targetTransaction.id, TransactionStatus.RC);
    }

    public interface BlotterOperationsCallback {
        void afterDeletingTransaction(long id);
        void startActivityForResult(Intent intent, int requestCode);
    }
}
