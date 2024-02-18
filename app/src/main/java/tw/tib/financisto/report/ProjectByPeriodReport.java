package tw.tib.financisto.report;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import tw.tib.financisto.R;
import tw.tib.financisto.db.MyEntityManager;
import tw.tib.financisto.db.DatabaseHelper.TransactionColumns;
import tw.tib.financisto.graph.Report2DChart;
import tw.tib.financisto.model.Currency;
import tw.tib.financisto.model.Project;
import tw.tib.financisto.utils.MyPreferences;
import android.content.Context;

/**
 * 2D Chart Report to display monthly results by Projects.
 * @author Abdsandryk
 */
public class ProjectByPeriodReport extends Report2DChart {
	
	public ProjectByPeriodReport(Context context, MyEntityManager em, Calendar startPeriod, int periodLength, Currency currency) {
		super(context, em, startPeriod, periodLength, currency);
	}

	@Override
	public int getFilterItemTypeName() {
		return R.string.project;
	}

	/* (non-Javadoc)
	 * @see tw.tib.financisto.graph.ReportGraphic2D#getFilterName()
	 */
	@Override
	public String getFilterName() {
		if (filterTitles.size()>0) {
			return filterTitles.get(currentFilterOrder);
		} else {
			// no project
			return context.getString(R.string.no_project);
		}
	}

	@Override
	public List<Report2DChart> getChildrenCharts() {
		return null;
	}

	@Override
	protected void createFilter() {
		columnFilter = TransactionColumns.project_id.name();
		boolean includeNoProject = MyPreferences.includeNoFilterInReport(context);
		filterIds = new ArrayList<>();
		filterTitles = new ArrayList<>();
		currentFilterOrder = 0;
		List<Project> projects = em.getAllProjectsList(includeNoProject);
		for (Project p : projects) {
			filterIds.add(p.id);
			filterTitles.add(p.title);
		}
	}

	@Override
	public String getNoFilterMessage(Context context) {
		return context.getString(R.string.report_no_project);
	}
}
