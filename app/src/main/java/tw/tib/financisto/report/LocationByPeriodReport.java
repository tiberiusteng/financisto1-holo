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
	
	/**
	 * Default constructor.
	 * @param dbAdapter
	 * @param context
	 * @param periodLength
	 * @param currency
	 */
	public LocationByPeriodReport(Context context, MyEntityManager em, int periodLength, Currency currency) {
		super(context, em, periodLength, currency);
	}
	
	/**
	 * Default constructor.
	 * @param context
	 * @param dbAdapter
	 * @param startPeriod
	 * @param periodLength
	 * @param currency
	 */
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

	/* (non-Javadoc)
	 * @see tw.tib.financisto.graph.ReportGraphic2D#getFilterName()
	 */
	@Override
	public String getFilterName() {
		if (filterIds.size()>0) {
			long locationId = filterIds.get(currentFilterOrder);
			MyLocation location = em.get(MyLocation.class, locationId);
			if (location != null) {
				return location.name;
			} else {
				return context.getString(R.string.current_location);
			}
		} else {
			// no location
			return context.getString(R.string.current_location);
		}
	}

	/* (non-Javadoc)
	 * @see tw.tib.financisto.graph.ReportGraphic2D#setFilterIds()
	 */
	@Override
	public void setFilterIds() {
		boolean includeNoLocation = MyPreferences.includeNoFilterInReport(context);
		filterIds = new ArrayList<Long>();
		currentFilterOrder = 0;
		List<MyLocation> locations = em.getAllLocationsList(includeNoLocation);
		if (locations.size()>0) {
			MyLocation l;
			for (int i=0; i<locations.size(); i++) {
				l = locations.get(i);
				filterIds.add(l.id);
			}
		}
	}

	@Override
	protected void setColumnFilter() {
		columnFilter = TransactionColumns.location_id.name();
	}
	
	@Override
	public String getNoFilterMessage(Context context) {
		return context.getString(R.string.report_no_location);
	}

}
