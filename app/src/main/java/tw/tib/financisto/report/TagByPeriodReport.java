package tw.tib.financisto.report;

import android.content.Context;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import tw.tib.financisto.R;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.db.DatabaseHelper;
import tw.tib.financisto.filter.Criterion;
import tw.tib.financisto.graph.Report2DChart;
import tw.tib.financisto.model.Currency;
import tw.tib.financisto.model.ReportDataByPeriod;
import tw.tib.financisto.model.Tag;
import tw.tib.financisto.utils.MyPreferences;

public class TagByPeriodReport extends Report2DChart {
    public TagByPeriodReport(Context context, DatabaseAdapter em, Calendar startPeriod, int periodLength, Currency currency, MyPreferences.ReportAggregateUnit aggregateUnit) {
        super(context, em, startPeriod, periodLength, currency, aggregateUnit);
    }

    @Override
    public int getFilterItemTypeName() {
        return R.string.tag;
    }

    /* (non-Javadoc)
     * @see tw.tib.financisto.graph.ReportGraphic2D#getFilterName()
     */
    @Override
    public String getFilterName() {
        if (filterTitles.size()>0) {
            return filterTitles.get(currentFilterOrder);
        } else {
            // no tags
            return context.getString(R.string.no_tags);
        }
    }

    @Override
    public List<Report2DChart> getChildrenCharts() {
        return null;
    }

    @Override
    protected void createFilter() {
        columnFilter = DatabaseHelper.TransactionColumns.tags.name();
        boolean includeNoTags = MyPreferences.includeNoFilterInReport();
        filterIds = new ArrayList<>();
        filterTitles = new ArrayList<>();
        currentFilterOrder = 0;
        List<Tag> tags = em.getAllTagsList(includeNoTags);

        Tag noTag = new Tag(context.getString(R.string.no_tags));
        noTag.id = 0;
        tags.add(0, noTag);

        for (Tag t : tags) {
            filterIds.add(t.id);
            filterTitles.add(t.title);
        }
    }

    @Override
    public String getNoFilterMessage(Context context) {
        return context.getString(R.string.report_no_tag);
    }

    @Override
    public Criterion getCriteria() {
        String tag = filterTitles.get(currentFilterOrder);
        if (tag.equals(context.getString(R.string.no_tags))) {
            return Criterion.isNull(columnFilter);
        }
        else {
            return Criterion.like(columnFilter, "%\n" + tag + "\n%");
        }
    }

    @Override
    protected ReportDataByPeriod createDataBuilder() {
        return new ReportDataByPeriod(context, startPeriod, periodLength, currency, columnFilter, filterTitles.get(currentFilterOrder), em, aggregateUnit);
    }
}
