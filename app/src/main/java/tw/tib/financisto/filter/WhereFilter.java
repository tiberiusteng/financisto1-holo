/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package tw.tib.financisto.filter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import tw.tib.financisto.activity.DateFilterActivity;
import tw.tib.financisto.blotter.BlotterFilter;
import tw.tib.financisto.datetime.PeriodType;
import tw.tib.financisto.utils.ArrUtils;
import tw.tib.financisto.utils.StringUtil;
import tw.tib.orb.Expression;
import tw.tib.orb.Expressions;

import static tw.tib.orb.EntityManager.DEF_SORT_COL;

public class WhereFilter {
	private static final String TAG = "WhereFilter";

	public static final String TITLE_EXTRA = "title";
	public static final String FILTER_EXTRA = "filter";
	public static final String SORT_ORDER_EXTRA = DEF_SORT_COL;

	public static final String FILTER_TITLE_PREF = "filterTitle";
	public static final String FILTER_LENGTH_PREF = "filterLength";
	public static final String FILTER_CRITERIA_PREF = "filterCriteria";
	public static final String FILTER_SORT_ORDER_PREF = "filterSortOrder";

	public static final String TAG_AS_IS = "__tag_as_is"; // signal Total Details activity to not filter excluded transactions

	private final String title;
	private final LinkedList<Criteria> criterias = new LinkedList<>();
	private final LinkedList<String> sorts = new LinkedList<>();

	public WhereFilter(String title) {
		this.title = title;
	}

	public synchronized WhereFilter eq(Criteria c) {
		criterias.add(c);
		return this;
	}

	public synchronized WhereFilter eq(String column, String value) {
		criterias.add(Criteria.eq(column, value));
		return this;
	}

	public synchronized WhereFilter neq(String column, String value) {
		criterias.add(Criteria.neq(column, value));
		return this;
	}

	public synchronized WhereFilter btw(String column, String value1, String value2) {
		criterias.add(Criteria.btw(column, value1, value2));
		return this;
	}

	public synchronized WhereFilter gt(String column, String value) {
		criterias.add(Criteria.gt(column, value));
		return this;
	}

	public synchronized WhereFilter gte(String column, String value) {
		criterias.add(Criteria.gte(column, value));
		return this;
	}

	public synchronized WhereFilter lt(String column, String value) {
		criterias.add(Criteria.lt(column, value));
		return this;
	}

	public synchronized WhereFilter lte(String column, String value) {
		criterias.add(Criteria.lte(column, value));
		return this;
	}

	public synchronized WhereFilter isNull(String column) {
		criterias.add(Criteria.isNull(column));
		return this;
	}

	public synchronized WhereFilter asc(String column) {
		sorts.add(column+" asc");
		return this;
	}

	public synchronized WhereFilter desc(String column) {
		sorts.add(column+" desc");
		return this;
	}

	public synchronized WhereFilter contains(String column, String text){
		criterias.add(Criteria.like(column, String.format("%%%s%%", text)));
		return this;
	}

	private String getSelection(List<Criteria> criterias) {
		StringBuilder sb = new StringBuilder();
		Log.d(TAG, "getSelection:");
		for (Criteria c : criterias) {
			Log.d(TAG, " " + c.toStringExtra());
			Log.d(TAG, "     " + String.join(", ", c.getSelection()));
			if (sb.length() > 0) {
				sb.append(" AND ");
			}
			sb.append(c.getSelection());
		}
		return sb.toString().trim();
	}

	private String[] getSelectionArgs(List<Criteria> criterias) {
		String[] args = new String[0];
		Log.d(TAG, "getSelectionArgs:");
		for (Criteria c : criterias) {
			Log.d(TAG, " " + c.toStringExtra());
			Log.d(TAG, "     " + String.join(", ", c.getSelectionArgs()));
			args = ArrUtils.joinArrays(args, c.getSelectionArgs());
		}
		return args;
	}

	public synchronized Criteria get(String name) {
		for (Criteria c : criterias) {
			String column = c.columnName;
			if (name.equals(column)) {
				return c;
			}
		}
		return null;
	}

	public DateTimeCriteria getDateTime() {
		return (DateTimeCriteria)get(BlotterFilter.DATETIME);
	}

	public synchronized Criteria put(Criteria criteria) {
		for (int i=0; i<criterias.size(); i++) {
			Criteria c = criterias.get(i);
			if (criteria.columnName.equals(c.columnName)) {
				criterias.set(i, criteria);
				return c;
			}
		}
		criterias.add(criteria);
		return null;
	}

	public synchronized Criteria remove(String name) {
		for (Iterator<Criteria> i = criterias.iterator(); i.hasNext();) {
			Criteria c = i.next();
			if (name.equals(c.columnName)) {
				i.remove();
				return c;
			}
		}
		return null;
	}

	public synchronized void clear() {
		criterias.clear();
		sorts.clear();
	}

	public static WhereFilter copyOf(WhereFilter filter) {
		synchronized (filter) {
			WhereFilter f = new WhereFilter(filter.title);
			f.criterias.addAll(filter.criterias);
			f.sorts.addAll(filter.sorts);
			return f;
		}
	}

	public static WhereFilter empty() {
		return new WhereFilter("");
	}

	public synchronized Expression toWhereExpression() {
		int count = criterias.size();
		Expression[] ee = new Expression[count];
		for (int i=0; i<count; i++) {
			ee[i] = criterias.get(i).toWhereExpression();
		}
		return Expressions.and(ee);
	}

	public synchronized void toBundle(Bundle bundle) {
		String[] extras = new String[criterias.size()];
		for (int i=0; i<extras.length; i++) {
			extras[i] = criterias.get(i).toStringExtra();
			Log.d(TAG, "extras " + i + ": " + extras[i]);
		}
		bundle.putString(TITLE_EXTRA, title);
		bundle.putStringArray(FILTER_EXTRA, extras);
		bundle.putString(SORT_ORDER_EXTRA, getSortOrder());
	}

	public static WhereFilter fromBundle(Bundle bundle) {
		String title = bundle.getString(TITLE_EXTRA);
		WhereFilter filter = new WhereFilter(title);
		try {
			synchronized (filter) {
				String[] a = bundle.getStringArray(FILTER_EXTRA);
				if (a != null) {
					for (String s : a) {
						filter.put(Criteria.fromStringExtra(s));
					}
				}
				String sortOrder = bundle.getString(SORT_ORDER_EXTRA);
				if (sortOrder != null) {
					String[] orders = sortOrder.split(",");
					if (orders != null && orders.length > 0) {
						filter.sorts.addAll(Arrays.asList(orders));
					}
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "fromBundle", e);
			return WhereFilter.empty();
		}
		return filter;
	}

	public void toIntent(Intent intent) {
		Bundle bundle = intent.getExtras();
		if (bundle == null) bundle = new Bundle();
		toBundle(bundle);
		intent.replaceExtras(bundle);
	}

	public static WhereFilter fromIntent(Intent intent) {
		Bundle bundle = intent.getExtras();
		if (bundle == null) bundle = new Bundle();
		return fromBundle(bundle);
	}

	public synchronized String getSortOrder() {
		StringBuilder sb = new StringBuilder();
		for (String o : sorts) {
			if (sb.length() > 0) {
				sb.append(",");
			}
			sb.append(o);
		}
		return sb.toString();
	}

	public synchronized void resetSort() {
		sorts.clear();
	}

	public synchronized void toSharedPreferences(SharedPreferences preferences) {
		Editor e = preferences.edit();
		int count = criterias.size();
		e.putString(FILTER_TITLE_PREF, title);
		e.putInt(FILTER_LENGTH_PREF, count);
		for (int i=0; i<count; i++) {
			e.putString(FILTER_CRITERIA_PREF+i, criterias.get(i).toStringExtra());
		}
		e.putString(FILTER_SORT_ORDER_PREF, getSortOrder());
		e.commit();
	}

	public static WhereFilter fromSharedPreferences(SharedPreferences preferences) {
		String title = preferences.getString(FILTER_TITLE_PREF, "");
		WhereFilter filter = new WhereFilter(title);
		try {
			synchronized (filter) {
				int count = preferences.getInt(FILTER_LENGTH_PREF, 0);
				if (count > 0) {
					for (int i = 0; i < count; i++) {
						String criteria = preferences.getString(FILTER_CRITERIA_PREF + i, "");
						if (criteria.length() > 0) {
							filter.put(Criteria.fromStringExtra(criteria));
						}
					}
				}
				String sortOrder = preferences.getString(FILTER_SORT_ORDER_PREF, "");
				String[] orders = sortOrder.split(",");
				if (orders != null && orders.length > 0) {
					filter.sorts.addAll(Arrays.asList(orders));
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "fromSharedPreferences", e);
			return WhereFilter.empty();
		}
		return filter;
	}

	public synchronized String getSelection() {
		String ret = getSelection(criterias);
		Log.d("WhereFilter", "getSelection=" + ret);
		return ret;
	}

	public synchronized String[] getSelectionArgs() {
		String[] ret = getSelectionArgs(criterias);
		Log.d("WhereFilter", "getSelectionArgs=" + String.join(",", ret));
		return ret;
	}

	public long getAccountId() {
		Criteria c = get(BlotterFilter.FROM_ACCOUNT_ID);
		return c != null ? c.getLongValue1() : -1;
	}

	public long getBudgetId() {
		Criteria c = get(BlotterFilter.BUDGET_ID);
		return c != null ? c.getLongValue1() : -1;
	}

	public int getIsTemplate() {
		Criteria c = get(BlotterFilter.IS_TEMPLATE);
		return c != null ? c.getIntValue() : 0;
	}

	public boolean isTemplate() {
		Criteria c = get(BlotterFilter.IS_TEMPLATE);
		return c != null && c.getLongValue1() == 1;
	}

	public boolean isSchedule() {
		Criteria c = get(BlotterFilter.IS_TEMPLATE);
		return c != null && c.getLongValue1() == 2;
	}

	public String getTitle() {
		return title;
	}

	public synchronized boolean isEmpty() {
		return criterias.isEmpty();
	}

	public enum Operation {
		NOPE(""), EQ("=?"), NEQ("!=?"), GT(">?"), GTE(">=?"), LT("<?"), LTE("<=?"), BTW("BETWEEN ? AND ?", "OR", 2),
		IN("IN (?)") {
			@Override
			public String getOp(int operands) {
				return super.getOp(operands).replace("?", StringUtil.generateSeparated("?", ",", operands));
			}
		},
		ISNULL("is NULL"), LIKE("LIKE ?"), OR("OR"), AND("AND");

		private final String op;
		private final String groupOp;
		private final int valsPerGroup;

		Operation(String op) {
			this(op, null, 1);
		}

		Operation(String op, String groupOp, int valsPerGroup) {
			this.op = op;
			this.groupOp = groupOp;
			this.valsPerGroup = valsPerGroup;
		}

		public String getOp(int ignore) {
			return op;
		}

		public String getGroupOp() {
			return groupOp;
		}

		public int getValsPerGroup() {
			return valsPerGroup;
		}
	}

	public void clearDateTime() {
		remove(BlotterFilter.DATETIME);
	}

	public void recalculatePeriod(Context context) {
		DateTimeCriteria c = getDateTime();
		if (c != null) {
			PeriodType t = c.getPeriod().type;
			if (t != PeriodType.CUSTOM) {
				put(new DateTimeCriteria(context, t));
			}
		}
	}

	public static DateTimeCriteria dateTimeFromIntent(Context context, Intent data) {
		String periodType = data.getStringExtra(DateFilterActivity.EXTRA_FILTER_PERIOD_TYPE);
		PeriodType p = PeriodType.valueOf(periodType);
		if (PeriodType.CUSTOM == p) {
			long periodFrom = data.getLongExtra(DateFilterActivity.EXTRA_FILTER_PERIOD_FROM, 0);
			long periodTo = data.getLongExtra(DateFilterActivity.EXTRA_FILTER_PERIOD_TO, 0);
			return new DateTimeCriteria(periodFrom, periodTo);
		} else {
			return new DateTimeCriteria(context, p);
		}

	}

}
