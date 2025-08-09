package tw.tib.financisto.report;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import tw.tib.financisto.R;
import tw.tib.financisto.db.MyEntityManager;
import tw.tib.financisto.db.DatabaseHelper.TransactionColumns;
import tw.tib.financisto.graph.Report2DChart;
import tw.tib.financisto.model.Account;
import tw.tib.financisto.model.Currency;
import tw.tib.financisto.model.ReportDataByPeriod;

import android.content.Context;

/**
 * 2D Chart Report to display monthly account results.
 * @author Abdsandryk
 */
public class AccountByPeriodReport extends Report2DChart {

	public AccountByPeriodReport(Context context, MyEntityManager em, Calendar startPeriod, int periodLength, Currency currency) {
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
		return R.string.account;
	}

	/* (non-Javadoc)
	 * @see tw.tib.financisto.graph.ReportGraphic2D#getFilterName()
	 */
	@Override
	public String getFilterName() {
		if (filterTitles.size()>0) {
			return filterTitles.get(currentFilterOrder);
		} else {
			return context.getString(R.string.no_account);
		}
	}

	@Override
	protected void createFilter() {
		columnFilter = TransactionColumns.from_account_id.name();

		filterIds = new ArrayList<>();
		filterTitles = new ArrayList<>();
		currentFilterOrder = 0;
		List<Account> accounts = em.getAllAccountsList();
		for (Account a: accounts) {
			filterIds.add(a.id);
			filterTitles.add(a.title);
		}
	}

	@Override
	public String getNoFilterMessage(Context context) {
		return context.getString(R.string.report_no_account);
	}

	@Override
	public Currency getCurrency() {
		return em.getAccount(filterIds.get(currentFilterOrder)).currency;
	}

	@Override
	protected ReportDataByPeriod createDataBuilder() {
		return new ReportDataByPeriod(context, startPeriod, periodLength, currency, columnFilter,
				filterIds.get(currentFilterOrder), em, ReportDataByPeriod.ValueAggregation.SUM,
				true, false);
	}
}
