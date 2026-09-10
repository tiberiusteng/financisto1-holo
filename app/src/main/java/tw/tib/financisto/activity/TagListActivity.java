package tw.tib.financisto.activity;

import android.view.View;
import tw.tib.financisto.R;
import tw.tib.financisto.blotter.BlotterFilter;
import tw.tib.financisto.filter.Criterion;
import tw.tib.financisto.model.Tag;

public class TagListActivity extends MyEntityListActivity<Tag> {

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
        db.deleteTag(id);
        recreateCursor();
    }
}
