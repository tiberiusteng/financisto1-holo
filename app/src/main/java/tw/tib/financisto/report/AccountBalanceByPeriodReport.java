package tw.tib.financisto.report;

import static java.lang.String.format;
import static tw.tib.financisto.db.DatabaseHelper.V_BLOTTER_FOR_ACCOUNT_WITH_SPLITS;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import tw.tib.financisto.R;
import tw.tib.financisto.db.DatabaseHelper;
import tw.tib.financisto.db.MyEntityManager;
import tw.tib.financisto.graph.Report2DChart;
import tw.tib.financisto.model.Account;
import tw.tib.financisto.model.Currency;
import tw.tib.financisto.model.ReportDataByPeriod;

public class AccountBalanceByPeriodReport extends Report2DChart {
    private static final String TAG = "AcctBalanceReport";

    public AccountBalanceByPeriodReport(Context context, MyEntityManager em, Calendar startPeriod, int periodLength, Currency currency) {
        super(context, em, startPeriod, periodLength, currency);
    }

    @Override
    public String getNoFilterMessage(Context context) {
        return context.getString(R.string.report_no_account);
    }

    @Override
    public List<Report2DChart> getChildrenCharts() {
        return Collections.emptyList();
    }

    @Override
    public int getFilterItemTypeName() {
        return R.string.account;
    }

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
        columnFilter = DatabaseHelper.TransactionColumns.from_account_id.name();

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
    protected ReportDataByPeriod createDataBuilder() {
        return new ReportDataByPeriod(context, startPeriod, periodLength, currency, columnFilter,
                filterIds.get(currentFilterOrder), em, ReportDataByPeriod.ValueAggregation.LAST, false) {
            @Override
            protected Cursor queryData(SQLiteDatabase db, String filterColumn, String where, String[] args) {
                Log.d(TAG, format("filterColumn:%s where:%s args:%s", filterColumn, where, Arrays.toString(args)));

                Cursor r = db.query(V_BLOTTER_FOR_ACCOUNT_WITH_SPLITS,
                        new String[]{
                                filterColumn,
                                DatabaseHelper.BlotterColumns.from_account_balance.name(),
                                DatabaseHelper.BlotterColumns.datetime.name()
                        },
                        where, args, null, null,
                        DatabaseHelper.BlotterColumns.datetime.name());
                Log.d(TAG, "result count=" + r.getCount());
                return r;
            }
        };
    }
}
