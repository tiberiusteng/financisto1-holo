package tw.tib.financisto.report;

import static java.lang.String.format;
import static tw.tib.financisto.db.DatabaseHelper.V_BLOTTER_FOR_ACCOUNT_WITH_SPLITS;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import tw.tib.financisto.R;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.db.DatabaseHelper;
import tw.tib.financisto.graph.Report2DChart;
import tw.tib.financisto.model.Currency;
import tw.tib.financisto.model.ReportDataByPeriod;
import tw.tib.financisto.rates.ExchangeRate;
import tw.tib.financisto.utils.CurrencyCache;
import tw.tib.financisto.utils.MyPreferences;

public class TotalBalanceByPeriodReport extends Report2DChart {
    private static final String TAG = "TotalBalanceReport";

    public TotalBalanceByPeriodReport(Context context, DatabaseAdapter em, Calendar startPeriod, int periodLength, Currency currency, MyPreferences.ReportAggregateUnit aggregateUnit) {
        super(context, em, startPeriod, periodLength, currency, aggregateUnit);
    }

    @Override
    public String getNoFilterMessage(Context context) {
        return "";
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
        return context.getString(R.string.all_accounts);
    }

    @Override
    protected void createFilter() {
        filterIds = List.of(0L);
        filterTitles = List.of(context.getString(R.string.all_accounts));
    }

    @Override
    public Currency getCurrency() {
        return CurrencyCache.getHomeCurrency();
    }

    @Override
    protected ReportDataByPeriod createDataBuilder() {
        return new ReportDataByPeriod(context, startPeriod, periodLength, currency, columnFilter,
                filterIds.get(currentFilterOrder), em, ReportDataByPeriod.ValueAggregation.LAST, false, false, aggregateUnit) {

            private Calendar start;
            private Calendar end;

            @Override
            protected Cursor queryData(SQLiteDatabase db, String filterColumn, String where, String[] args) {
                Log.d(TAG, format("filterColumn:%s where:%s args:%s", filterColumn, where, Arrays.toString(args)));

                start = Calendar.getInstance();
                start.setTimeInMillis(Long.parseLong(args[1]));
                end = Calendar.getInstance();
                end.setTimeInMillis(Long.parseLong(args[2]));

                Cursor r = db.query(V_BLOTTER_FOR_ACCOUNT_WITH_SPLITS,
                        new String[]{
                                DatabaseHelper.BlotterColumns.from_account_currency_id.name(),
                                DatabaseHelper.BlotterColumns.from_amount.name(),
                                DatabaseHelper.BlotterColumns.datetime.name()
                        },
                        null, null, null, null,
                        DatabaseHelper.BlotterColumns.datetime.name());
                Log.d(TAG, "result count=" + r.getCount());
                return r;
            }

            @Override
            protected void extractDataInnerLoop(Cursor c) {
                var homeCurrency = CurrencyCache.getHomeCurrency();
                var rates = em.getLatestRates();
                boolean toasted = false;

                double result = 0;
                while (c.moveToNext()) {

                    // get month of reference
                    Calendar timeframe = getTransactionTimeframe(c);

                    boolean stepTimeframe = false;
                    // get result from transactions in the reference month
                    do {
                        Calendar transactionTimeframe = getTransactionTimeframe(c);
                        if(transactionTimeframe.compareTo(timeframe)!=0) {
                            stepTimeframe = true;
                            break;
                        }

                        long currencyId = c.getLong(0);
                        if (currencyId == homeCurrency.id) {
                            result += c.getLong(1);
                        }
                        else {
                            var currency = CurrencyCache.getCurrency(currencyId);
                            var rate = rates.getRate(currency, homeCurrency);
                            if (rate == ExchangeRate.NA) {
                                if (!toasted) {
                                    Toast.makeText(context, context.getString(R.string.rate_not_available_error, currency.name, homeCurrency.name), Toast.LENGTH_LONG);
                                    toasted = true;
                                }
                            }
                            else {
                                result += (new BigDecimal(c.getLong(1)).movePointLeft(currency.getScale()).movePointRight(2).longValue() * rate.rate);
                            }
                        }

                    } while(c.moveToNext());

                    // If step month, get back to transaction of the new month in cursor.
                    if (stepTimeframe)
                        c.moveToPrevious();

                    if (start.compareTo(timeframe) <= 0 && end.compareTo(timeframe) >= 0)
                        storePeriodResult(timeframe, result);
                }
            }
        };
    }
}
