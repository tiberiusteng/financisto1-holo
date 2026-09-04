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
package tw.tib.financisto.activity;

import android.util.Log;
import android.widget.EditText;

import tw.tib.financisto.R;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.model.Payee;

public class PayeeActivity extends MyEntityActivity<Payee> {
    public static final String TAG = "PayeeActivity";

    public PayeeActivity() {
        super(Payee.class, true);
    }

    @Override
    protected String getAliases(Payee entity) {
        Log.d(TAG, "getAliases " + entity.title + " " + entity.aliases);
        return entity.aliases;
    }

    @Override
    protected void updateEntity(Payee entity) {
        var db = new DatabaseAdapter(this);
        var sqlitedb = db.db();
        EditText aliases = findViewById(R.id.aliases);

        sqlitedb.beginTransaction();
        try {
            db.rebuildAliases(Payee.class, sqlitedb, entity.id, aliases.getText().toString());
            sqlitedb.setTransactionSuccessful();
        }
        finally {
            sqlitedb.endTransaction();
        }
    }
}
