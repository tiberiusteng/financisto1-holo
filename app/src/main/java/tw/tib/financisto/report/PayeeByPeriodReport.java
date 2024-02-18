package tw.tib.financisto.report;

import android.content.Context;
import tw.tib.financisto.R;
import tw.tib.financisto.db.DatabaseHelper.TransactionColumns;
import tw.tib.financisto.db.MyEntityManager;
import tw.tib.financisto.graph.Report2DChart;
import tw.tib.financisto.model.Currency;
import tw.tib.financisto.model.Payee;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 2D Chart Report to display monthly results by Payees.
 * @author Denis Solonenko
 */
public class PayeeByPeriodReport extends Report2DChart {

	public PayeeByPeriodReport(Context context, MyEntityManager em, Calendar startPeriod, int periodLength, Currency currency) {
		super(context, em, startPeriod, periodLength, currency);
	}

	@Override
	public int getFilterItemTypeName() {
		return R.string.payee;
	}

	@Override
	public String getFilterName() {
		if (filterTitles.size()>0) {
			return filterTitles.get(currentFilterOrder);
		} else {
			// no payee
			return context.getString(R.string.no_payee);
		}
	}

	@Override
	public List<Report2DChart> getChildrenCharts() {
		return null;
	}

	@Override
	protected void createFilter() {
		columnFilter = TransactionColumns.payee_id.name();
		filterIds = new ArrayList<>();
		filterTitles = new ArrayList<>();
		currentFilterOrder = 0;
		List<Payee> payees = em.getAllPayeeList();
		for (Payee p : payees) {
			filterIds.add(p.id);
			filterTitles.add(p.title);
		}
	}

	@Override
	public String getNoFilterMessage(Context context) {
		return context.getString(R.string.report_no_payee);
	}
}
