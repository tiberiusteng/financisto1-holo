package tw.tib.financisto.activity;

import android.database.Cursor;
import android.util.Log;
import android.widget.EditText;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import tw.tib.financisto.R;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.db.DatabaseHelper;
import tw.tib.financisto.model.Tag;

public class TagActivity extends MyEntityActivity<Tag> {
    private static final String TAG = "TagActivity";

    public TagActivity() {
        super(Tag.class, false, true);
    }

    @Override
    protected String getColor(Tag entity) {
        return entity.color;
    }

    @Override
    protected void preUpdateEntity(Tag updated) {
        var db = new DatabaseAdapter(this);
        Tag old = db.get(Tag.class, updated.id);

        Log.d(TAG, "preUpdateEntity old=" + (old != null ? old.title : "null") + ", updated=" + updated.title);
        if (old != null && !old.title.equals(updated.title)) {
            // update tags in transactions
            var txTags = new ObjectOpenHashSet<String>();
            var sqlitedb = db.db();
            sqlitedb.beginTransaction();
            try {
                Cursor tx = sqlitedb.query(DatabaseHelper.TRANSACTION_TABLE, new String[]{"_id", "tags"},
                        "tags LIKE ?", new String[]{"%\n" + old.title + "\n%"}, null, null, null);
                while (tx.moveToNext()) {
                    var tid = tx.getLong(0);

                    txTags.clear();
                    for (var txTag : tx.getString(1).split("\n")) {
                        if (!txTag.isBlank()) {
                            if (!txTag.equals(old.title)) {
                                txTags.add(txTag);
                            }
                            else {
                                txTags.add(updated.title);
                            }
                        }
                    }
                    sqlitedb.execSQL("UPDATE transactions SET tags = ? WHERE _id = ?",
                            new String[]{"\n" + String.join("\n", txTags) + "\n",
                                    String.valueOf(tid)});
                }
                tx.close();
                sqlitedb.setTransactionSuccessful();
            }
            finally {
                sqlitedb.endTransaction();
            }
        }

        updated.color = ((EditText) findViewById(R.id.color)).getText().toString().trim();
    }
}
