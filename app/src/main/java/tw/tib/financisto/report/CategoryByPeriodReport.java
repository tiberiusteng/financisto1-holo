/**
 * 
 */
package tw.tib.financisto.report;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import tw.tib.financisto.R;
import tw.tib.financisto.db.DatabaseHelper;
import tw.tib.financisto.db.MyEntityManager;
import tw.tib.financisto.db.DatabaseHelper.CategoryColumns;
import tw.tib.financisto.db.DatabaseHelper.TransactionColumns;
import tw.tib.financisto.graph.Report2DChart;
import tw.tib.financisto.graph.Report2DPoint;
import tw.tib.financisto.model.Category;
import tw.tib.financisto.model.Currency;
import tw.tib.financisto.model.PeriodValue;
import tw.tib.financisto.model.ReportDataByPeriod;
import tw.tib.financisto.utils.MyPreferences;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * 2D Chart Report to display monthly results by Categories.
 * @author Abdsandryk
 */
public class CategoryByPeriodReport extends Report2DChart {
	
	public CategoryByPeriodReport(Context context, MyEntityManager em, Calendar startPeriod, int periodLength, Currency currency) {
		super(context, em, startPeriod, periodLength, currency);
	}

	@Override
	public int getFilterItemTypeName() {
		return R.string.category;
	}

	@Override
	public String getFilterName() {
		if (filterTitles.size()>0) {
			return filterTitles.get(currentFilterOrder);
		} else {
			// no category
			return context.getString(R.string.no_category);
		}
	}

	@Override
	public List<Report2DChart> getChildrenCharts() {
		return null;
	}

	@Override
	public boolean isRoot() {
		return false;
	}

	@Override
	protected void createFilter() {
		columnFilter = TransactionColumns.category_id.name();
		boolean includeSubCategories = MyPreferences.includeSubCategoriesInReport(context);
		boolean includeNoCategory = MyPreferences.includeNoFilterInReport(context);
		filterIds = new ArrayList<>();
		filterTitles = new ArrayList<>();
		currentFilterOrder = 0;
		List<Category> categories = em.getAllCategoriesList(includeNoCategory);
		for (Category c : categories) {
			if (includeSubCategories) {
				filterIds.add(c.id);
				filterTitles.add(c.title);
			} else {
				// do not include sub categories
				if (c.level == 1) {
					// filter root categories only
					filterIds.add(c.id);
					filterTitles.add(c.title);
				}
			}
		}
	}

	/**
	 * Request data and fill data objects (list of points, max, min, etc.)
	 */
	@Override
	protected void build() {
		boolean addSubs = MyPreferences.addSubCategoriesToSum(context);
		if (addSubs) {
			SQLiteDatabase db = em.db();
			Cursor cursor = null;
			try {
				long categoryId = filterIds.get(currentFilterOrder);
				Category parent = em.getCategory(categoryId);
				String where = CategoryColumns.left+" BETWEEN ? AND ?";
				String[] pars = new String[]{String.valueOf(parent.left), String.valueOf(parent.right)};
				cursor = db.query(DatabaseHelper.CATEGORY_TABLE, new String[]{CategoryColumns._id.name()}, where, pars, null, null, null);
				long[] categories = new long[cursor.getCount()+1];
				int i=0;
				while (cursor.moveToNext()) {
					categories[i] = (int)cursor.getInt(0);
					i++;
				}
				categories[i] = filterIds.get(currentFilterOrder);
				data = new ReportDataByPeriod(context, startPeriod, periodLength, currency, columnFilter, categories, em);
			} finally {
				if (cursor!=null) cursor.close();
			}
		} else {
			// only root category
			data = new ReportDataByPeriod(context, startPeriod, periodLength, currency, columnFilter, filterIds.get(currentFilterOrder), em);
		}
		
		points = new ArrayList<Report2DPoint>();
		List<PeriodValue> pvs = data.getPeriodValues();

        for (PeriodValue pv : pvs) {
            points.add(new Report2DPoint(pv));
        }
	}

	@Override
	public String getNoFilterMessage(Context context) {
		return context.getString(R.string.report_no_category);
	}

}
