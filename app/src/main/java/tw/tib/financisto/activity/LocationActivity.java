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
import tw.tib.financisto.model.MyLocation;

public class LocationActivity extends MyEntityActivity<MyLocation> {
    public static final String TAG = "LocationActivity";

    public LocationActivity() {
        super(MyLocation.class, true);
    }

    @Override
    protected String getAliases(MyLocation entity) {
        Log.d(TAG, "getAliases " + entity.title + " " + entity.aliases);
        return entity.aliases;
    }

    @Override
    protected void updateEntity(MyLocation entity) {
        var db = new DatabaseAdapter(this);
        var sqlitedb = db.db();
        EditText aliases = findViewById(R.id.aliases);

        sqlitedb.beginTransaction();
        try {
            db.rebuildAliases(MyLocation.class, sqlitedb, entity.id, aliases.getText().toString());
            sqlitedb.setTransactionSuccessful();
        }
        finally {
            sqlitedb.endTransaction();
        }
    }
}
