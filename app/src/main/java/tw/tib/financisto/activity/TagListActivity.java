package tw.tib.financisto.activity;

import android.database.Cursor;
import android.view.View;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import tw.tib.financisto.R;
import tw.tib.financisto.blotter.BlotterFilter;
import tw.tib.financisto.db.DatabaseHelper;
import tw.tib.financisto.filter.Criterion;
import tw.tib.financisto.model.Tag;

public class TagListActivity extends MyEntityListActivity<Tag> {
    private static final String TAG = "TagListActivity";

    public TagListActivity() {
        super(Tag.class, R.string.no_tags);
    }

    @Override
    protected Class<? extends MyEntityActivity> getEditActivityClass() {
        return TagActivity.class;
    }

    @Override
    protected Criterion createBlotterCriteria(Tag t) {
        return Criterion.like(BlotterFilter.TAGS, "%" + t.title + "%");
    }

    @Override
    protected void deleteItem(View v, int position, long id) {
        Tag tag = db.get(Tag.class, id);

        // remove tag from transactions
        var txTags = new ObjectOpenHashSet<String>();
        var sqlitedb = db.db();
        sqlitedb.beginTransaction();
        try {
            Cursor tx = sqlitedb.query(DatabaseHelper.TRANSACTION_TABLE, new String[]{"_id", "tags"},
                    "tags LIKE ?", new String[]{"%\n" + tag.title + "\n%"}, null, null, null);
            while (tx.moveToNext()) {
                var tid = tx.getLong(0);

                txTags.clear();
                for (var txTag : tx.getString(1).split("\n")) {
                    if (!txTag.isBlank() && !txTag.equals(tag.title)) {
                        txTags.add(txTag);
                    }
                }
                if (!txTags.isEmpty()) {
                    sqlitedb.execSQL("UPDATE transactions SET tags = ? WHERE _id = ?",
                            new String[]{"\n" + String.join("\n", txTags) + "\n",
                                    String.valueOf(tid)});
                }
                else {
                    sqlitedb.execSQL("UPDATE transactions SET tags = NULL WHERE _id = ?",
                            new String[]{String.valueOf(tid)});
                }
            }
            tx.close();
            sqlitedb.setTransactionSuccessful();
        }
        finally {
            sqlitedb.endTransaction();
        }
        db.deleteTag(id);
        recreateCursor();
    }
}
