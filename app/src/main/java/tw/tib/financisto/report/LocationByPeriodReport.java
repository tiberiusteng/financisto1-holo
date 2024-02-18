package tw.tib.financisto.report;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import tw.tib.financisto.R;
import tw.tib.financisto.db.MyEntityManager;
import tw.tib.financisto.db.DatabaseHelper.TransactionColumns;
import tw.tib.financisto.graph.Report2DChart;
import tw.tib.financisto.model.Currency;
import tw.tib.financisto.model.MyLocation;
import tw.tib.financisto.utils.MyPreferences;
import android.content.Context;

/**
 * 2D Chart Report to display monthly results by Locations.
 * @author Abdsandryk
 */
public class LocationByPeriodReport extends Report2DChart {

	public LocationByPeriodReport(Context context, MyEntityManager em, Calendar startPeriod, int periodLength, Currency currency) {
		super(context, em, startPeriod, periodLength, currency);
	}

	/* (non-Javadoc)
	 * @see tw.tib.financisto.graph.ReportGraphic2D#getChildrenGraphics()
	 */
	@Override
	public List<Report2DChart> getChildrenCharts() {
		return null;
	}

	@Override
	public int getFilterItemTypeName() {
		return R.string.location;
	}

	/* (non-Javadoc)
	 * @see tw.tib.financisto.graph.ReportGraphic2D#getFilterName()
	 */
	@Override
	public String getFilterName() {
		if (filterTitles.size()>0) {
			return filterTitles.get(currentFilterOrder);
		} else {
			// no location
			return context.getString(R.string.current_location);
		}
	}

	@Override
	protected void createFilter() {
		columnFilter = TransactionColumns.location_id.name();
		boolean includeNoLocation = MyPreferences.includeNoFilterInReport(context);
		filterIds = new ArrayList<>();
		filterTitles = new ArrayList<>();
		List<MyLocation> locations = em.getAllLocationsList(includeNoLocation);
		for (MyLocation l : locations) {
			filterIds.add(l.id);
			filterTitles.add(l.title);
		}
	}

	@Override
	public String getNoFilterMessage(Context context) {
		return context.getString(R.string.report_no_location);
	}

}
