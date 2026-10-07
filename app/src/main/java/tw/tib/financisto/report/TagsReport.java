package tw.tib.financisto.report;

import static tw.tib.financisto.db.DatabaseHelper.V_REPORT_TAGS;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

import tw.tib.financisto.activity.SplitsBlotterActivity;
import tw.tib.financisto.blotter.BlotterFilter;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.filter.Criterion;
import tw.tib.financisto.filter.WhereFilter;
import tw.tib.financisto.model.Currency;
import tw.tib.financisto.model.Tag;

public class TagsReport extends Report {
    public TagsReport(Context context, Currency currency) {
        super(ReportType.BY_TAG, context, currency);
    }

    @Override
    public ReportData getReport(DatabaseAdapter db, WhereFilter filter) {
        cleanupFilter(filter);
        return queryReport(db, V_REPORT_TAGS, filter);
    }

    @Override
    public Criterion getCriteriaForId(DatabaseAdapter db, long id) {
        Tag tag = db.get(Tag.class, id);
        return Criterion.like(BlotterFilter.TAGS, "%\n" + tag.title + "\n%");
    }

    @Override
    protected Class<? extends AppCompatActivity> getBlotterActivityClass() {
        return SplitsBlotterActivity.class;
    }
}
