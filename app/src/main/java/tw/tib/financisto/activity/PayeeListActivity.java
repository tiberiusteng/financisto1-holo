/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * <p/>
 * Contributors:
 * Denis Solonenko - initial API and implementation
 ******************************************************************************/
package tw.tib.financisto.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.widget.Toast;

import tw.tib.financisto.R;
import tw.tib.financisto.blotter.BlotterFilter;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.db.DatabaseHelper;
import tw.tib.financisto.export.BackupExportTask;
import tw.tib.financisto.filter.Criteria;
import tw.tib.financisto.model.Payee;

public class PayeeListActivity extends MyEntityListActivity<Payee> {

    public PayeeListActivity() {
        super(Payee.class, R.string.no_payees, true);
    }

    @Override
    protected Class<? extends MyEntityActivity> getEditActivityClass() {
        return PayeeActivity.class;
    }

    @Override
    protected Criteria createBlotterCriteria(Payee p) {
        return Criteria.eq(BlotterFilter.PAYEE_ID, String.valueOf(p.id));
    }

    @Override
    protected void mergeSelectedEntities() {
        long[] checkedIds = listView.getCheckedItemIds();
        if (checkedIds.length < 2) return;

        String keepPayeeTitle = db.get(Payee.class, checkedIds[0]).title;
        StringBuilder sb = new StringBuilder();
        for (int i=1; i<checkedIds.length; i++) {
            if (i != 1) sb.append(", ");
            sb.append(db.get(Payee.class, checkedIds[i]).title);
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.merge_payee_confirm)
                .setMessage(getString(R.string.merge_payee_message, keepPayeeTitle) + "\n\n" + sb + "\n\n" + getString(R.string.merge_payee_message_continue))
                .setPositiveButton(R.string.proceed, (dialogInterface, i) -> {
                    doMergePayees(checkedIds);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    protected void doMergePayees(long[] checkedIds) {
        DatabaseAdapter adapter = new DatabaseAdapter(this);
        // backup database before proceed
        ProgressDialog d = ProgressDialog.show(this, null, getString(R.string.backup_database_inprogress), true);
        BackupExportTask t = new BackupExportTask(this, d, false);
        t.setShowResultMessage(false);
        t.setListener(result -> {
            SQLiteDatabase db = adapter.db();

            var placeholder = new StringBuilder();
            for (int i=0; i<checkedIds.length-1; i++) {
                if (i != 0) placeholder.append(",");
                placeholder.append("?");
            }

            String[] checkedIdsString = new String[checkedIds.length];
            for (int i=0; i<checkedIds.length; i++) {
                checkedIdsString[i] = String.valueOf(checkedIds[i]);
            }

            try {
                db.beginTransaction();
                // update transaction
                db.execSQL("UPDATE " + DatabaseHelper.TRANSACTION_TABLE + " SET " + DatabaseHelper.TransactionColumns.payee_id + " = ? " +
                        "WHERE " + DatabaseHelper.TransactionColumns.payee_id + " IN (" + placeholder + ")", checkedIdsString);
                // delete merged payees
                for (int i = 1; i < checkedIds.length; i++) {
                    db.delete(DatabaseHelper.PAYEE_TABLE, DatabaseHelper.EntityColumns.ID + " = ?", new String[]{checkedIdsString[i]});
                }
                db.setTransactionSuccessful();
            }
            finally {
                db.endTransaction();
            }

            Toast.makeText(this, getString(R.string.merge_payee_success), Toast.LENGTH_LONG).show();
            recreateCursor();
        });
        t.execute((Uri[]) null);
    }
}
