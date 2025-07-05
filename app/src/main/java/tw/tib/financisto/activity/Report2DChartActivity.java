package tw.tib.financisto.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;

import tw.tib.financisto.R;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.graph.Report2DChart;
import tw.tib.financisto.graph.Report2DPoint;
import tw.tib.financisto.model.Currency;
import tw.tib.financisto.model.PeriodValue;
import tw.tib.financisto.model.ReportDataByPeriod;
import tw.tib.financisto.report.AccountByPeriodReport;
import tw.tib.financisto.report.CategoryByPeriodReport;
import tw.tib.financisto.report.LocationByPeriodReport;
import tw.tib.financisto.report.PayeeByPeriodReport;
import tw.tib.financisto.report.ProjectByPeriodReport;
import tw.tib.financisto.report.ReportType;
import tw.tib.financisto.utils.CurrencyCache;
import tw.tib.financisto.utils.MyPreferences;
import tw.tib.financisto.utils.PinProtection;
import tw.tib.financisto.utils.Utils;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Activity to display 2D Reports.
 *
 * @author Abdsandryk
 */
public class Report2DChartActivity extends Activity implements OnChartValueSelectedListener {
    private static final String TAG = "Report2DChartActivity";

    // activity result identifier to get results back
    public static final int REPORT_PREFERENCES = 1;

    // Data to display
    private Report2DChart reportData;
    private DatabaseAdapter db;

    private int[] periods;
    private int selectedPeriod;
    private Currency currency;
    private Calendar startPeriod;
    private ReportType reportType;

    private LineChart chart;
    private List<Entry> vals;
    private LineDataSet ds;
    private ArrayList<ILineDataSet> dss;

    private TextView pointDate;
    private TextView pointAmount;

    private int positive;
    private int negative;

    public static final int meanColor = 0xFF206DED;

    // array of string report preferences to identify changes
    String[] initialPrefs;

    // boolean to check if preferred currency is set
    private boolean prefCurNotSet = false;
    // boolean to check if preferred period is set
    private boolean prefPerNotSet = false;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(MyPreferences.switchLocale(base));
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // initialize activity
        super.onCreate(savedInstanceState);
        setContentView(R.layout.report_2d);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.report_base), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.statusBars()
                    | WindowInsetsCompat.Type.captionBar());
            var lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            lp.topMargin = insets.top;
            lp.bottomMargin = insets.bottom;
            v.setLayoutParams(lp);
            return WindowInsetsCompat.CONSUMED;
        });

        // get report type
        Intent intent = getIntent();
        if (intent != null) {
            reportType = ReportType.valueOf(intent.getStringExtra(Report2DChart.REPORT_TYPE));
        }

        String[] r = getResources().getStringArray(R.array.report_reference_period_values);
        periods = new int[r.length];
        for (int i=0; i<r.length; ++i) {
            periods[i] = Integer.parseInt(r[i]);
        }

        init();
    }

    private int selectPeriodFromLength(int months) {
        for (int i=0; i<periods.length; ++i) {
            if (periods[i] == months) return i;
        }
        return 0;
    }

    /**
     * Initialize activity.
     */
    private void init() {
        // database adapter to query data
        db = new DatabaseAdapter(this);
        db.open();

        // get report preferences to display chart
        // Reference Currency
        currency = getReferenceCurrency();
        // Period of Reference
        int periodLength = getPeriodOfReference();
        selectedPeriod = selectPeriodFromLength(periodLength);

        // check report preferences for reference month different of current month
        setStartPeriod(periodLength);

        boolean built = false;
        switch (reportType) {
            case BY_ACCOUNT_BY_PERIOD:
                reportData = new AccountByPeriodReport(this, db, startPeriod, periodLength, currency);
                break;
            case BY_CATEGORY_BY_PERIOD:
                reportData = new CategoryByPeriodReport(this, db, startPeriod, periodLength, currency);
                break;
            case BY_PAYEE_BY_PERIOD:
                reportData = new PayeeByPeriodReport(this, db, startPeriod, periodLength, currency);
                break;
            case BY_LOCATION_BY_PERIOD:
                reportData = new LocationByPeriodReport(this, db, startPeriod, periodLength, currency);
                break;
            case BY_PROJECT_BY_PERIOD:
                reportData = new ProjectByPeriodReport(this, db, startPeriod, periodLength, currency);
                break;
        }

        initChart();

        if (reportData.hasFilter()) {
            refreshView();
            built = true;
        } else {
            //  There is no <location, project or category> available for filtering data.
            alertNoFilter(reportData.getNoFilterMessage(this));
            adjustLabels();
        }

        if (built && (prefCurNotSet || prefPerNotSet)) {
            alertPreferencesNotSet(prefCurNotSet, prefPerNotSet);
        }

        // previous filter button
        ImageButton bPrevious = (ImageButton) findViewById(R.id.bt_filter_previous);
        bPrevious.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (reportData.previousFilter()) {
                    refreshView();
                }
            }
        });

        // next filter button
        ImageButton bNext = (ImageButton) findViewById(R.id.bt_filter_next);
        bNext.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (reportData.nextFilter()) {
                    refreshView();
                }
            }
        });

        TextView reportFilterName = findViewById(R.id.report_filter_name);
        reportFilterName.setOnClickListener((v) -> {
            new AlertDialog.Builder(this)
                    .setSingleChoiceItems(new ArrayAdapter<>(this,
                                    android.R.layout.simple_list_item_activated_1,
                                    android.R.id.text1,
                                    reportData.getFilterItemTitles()),
                            reportData.getSelectedFilter(),
                            (dialog, which) -> {
                                dialog.cancel();
                                if (reportData.selectFilter(which)) {
                                    refreshView();
                                }
                            })
                    .setTitle(reportData.getFilterItemTypeName())
                    .show();
        });

        // prefs
        ImageButton bPrefs = (ImageButton) findViewById(R.id.bt_preferences);
        bPrefs.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                showPreferences();
            }
        });

        // period length
        findViewById(R.id.report_period).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                // pop up options to choose the period
                changePeriodLength(selectedPeriod);
            }
        });
        findViewById(R.id.report_period).setFocusable(true);

        pointDate = findViewById(R.id.point_date);
        pointAmount = findViewById(R.id.point_amount);

        positive = getResources().getColor(R.color.positive_amount);
        negative = getResources().getColor(R.color.negative_amount);
    }

    private void initChart() {
        chart = findViewById(R.id.report_2d_chart);

        chart.setBackgroundColor(Color.parseColor("#ff111111"));
        chart.setTouchEnabled(true);
        chart.setOnChartValueSelectedListener(this);

        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);

        YAxis yAxis = chart.getAxisLeft();
        yAxis.setTextColor(Color.WHITE);
        yAxis.setTextSize(12f);
        yAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
        yAxis.enableGridDashedLine(10.0f, 10.0f, 0.0f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return DateUtils.formatDateTime(Report2DChartActivity.this, (long) value,
                        DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_NO_MONTH_DAY);
            }
        });
        xAxis.setTextColor(Color.WHITE);
        xAxis.setTextSize(12f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.enableGridDashedLine(10.0f, 10.0f, 0.0f);

        chart.getAxisRight().setEnabled(false);

        chart.getLegend().setEnabled(false);

        vals = new ArrayList<>();

        ds = new LineDataSet(vals, "");

        ds.setColor(Color.YELLOW);
        ds.setCircleColor(Color.YELLOW);
        ds.setLineWidth(1f);
        ds.setCircleRadius(2f);
        ds.setDrawValues(false);

        dss = new ArrayList<>();
        dss.add(ds);

        chart.setData(new LineData(dss));
    }

    /**
     * Display a message when preferences not set to alert the use of default values.
     *
     * @param isCurrency Inform if currency is not set on report preferences.
     * @param isPeriod   Inform if period is not set on report preferences.
     */
    private void alertPreferencesNotSet(boolean isCurrency, boolean isPeriod) {
        // display message: preferences not set
        String message = "";
        if (isCurrency) {
            if (isPeriod) {
                // neither currency neither period is set
                message = getResources().getString(R.string.report_preferences_not_set);
            } else {
                // only currency not set
                message = getResources().getString(R.string.currency_not_set);
            }
        } else {
            if (isPeriod) {
                // only period not set
                message = getResources().getString(R.string.period_not_set);
            }
        }
        AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
        dlgAlert.setMessage(message);
        dlgAlert.setTitle(R.string.reports);
        dlgAlert.setPositiveButton(R.string.ok, null);
        dlgAlert.setCancelable(true);
        dlgAlert.create().show();
    }

    /**
     * Alert message to warn that there is no filter available (no category, no project, no account or no location)
     *
     * @param message Message warning the lack of filters by report type.
     */
    private void alertNoFilter(String message) {
        AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
        dlgAlert.setMessage(message);
        dlgAlert.setTitle(R.string.reports);
        dlgAlert.setPositiveButton(R.string.ok, null);
        dlgAlert.setCancelable(true);
        dlgAlert.create().show();
    }

    /**
     * Display a list of period length options to redefine period, rebuild data and refresh view.
     *
     * @param previousPeriod The previous selected period to check if data changed, rebuild data and refresh view.
     */
    private void changePeriodLength(final int previousPeriod) {
        final Context context = this;
        new AlertDialog.Builder(this)
                .setTitle(R.string.report_reference_period)
                .setSingleChoiceItems(
                        new ArrayAdapter<>(this, android.R.layout.simple_list_item_activated_1,
                                android.R.id.text1,
                                getResources().getStringArray(R.array.report_reference_period_entities)),
                        selectedPeriod,
                        (dialog, which) -> {
                            dialog.cancel();
                            selectedPeriod = which;
                            processPeriodLengthChange(previousPeriod, true);
                        })
                .show();
    }

    /**
     * Process the period length change
     *
     * @param previousPeriod The selected period length before changing
     * @param refresh        True if requires refresh, false if refresh will be processed later
     */
    private void processPeriodLengthChange(int previousPeriod, boolean refresh) {
        if (previousPeriod != selectedPeriod) {
            reportData.changePeriodLength(periods[selectedPeriod]);
            setStartPeriod(periods[selectedPeriod]);
            reportData.changeStartPeriod(startPeriod);
            if (refresh) refreshView();
        }
    }

    /**
     * Update the view reflecting data changes
     */
    private void refreshView() {
        // set data to plot
        if (reportData.hasDataToPlot()) {
            findViewById(R.id.report_empty).setVisibility(View.GONE);

            chart.setVisibility(View.VISIBLE);

            vals.clear();
            for (Report2DPoint p : reportData.getPoints()) {
                PeriodValue v = p.getPointData();
                // x value is 32-bit floating point, on recent timestamps the step size is 131.072 seconds
                // so sometimes it will become 1~2 minutes earlier in the previous month when converting to float
                // we are only using the month part, so add 86400*1000*14 ms to shift it to middle of month
                vals.add(new Entry(v.getMonthTimeInMillis() + 1209600000f, (float) v.getValue() / 100.0f));
            }

            ds.notifyDataSetChanged();
            chart.getData().notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.invalidate();

            if (chart.valuesToHighlight()) {
                Highlight[] highlights = chart.getHighlighted();
                Entry entry = chart.getData().getEntryForHighlight(highlights[0]);
                onValueSelected(entry, highlights[0]);
            }
        } else {
            findViewById(R.id.report_empty).setVisibility(View.VISIBLE);
            findViewById(R.id.report_2d_chart).setVisibility(View.GONE);
            onNothingSelected();
        }
        // adjust report 2D user interface elements
        adjustLabels();
        fillStatistics();
    }

    /**
     * Adjust labels after changing report parameters
     */
    private void adjustLabels() {
        // Filter name
        ((TextView) findViewById(R.id.report_filter_name)).setText(reportData.getFilterName());
        // Period
        ((TextView) findViewById(R.id.report_period)).setText(reportData.getPeriodLengthString(this));
    }

    /**
     * Fill statistics panel based on report data
     */
    private void fillStatistics() {
        boolean considerNull = MyPreferences.considerNullResultsInReport(this);
        Double max;
        Double min;
        Double mean;
        Double meanWithSign;
        Double sum = reportData.getDataBuilder().getSum();
        if (considerNull) {
            max = reportData.getDataBuilder().getMaxValue();
            min = reportData.getDataBuilder().getMinValue();
            mean = meanWithSign = reportData.getDataBuilder().getMean();
            if ((min * max >= 0)) {
                // absolute calculation (all points over the x axis)
                max = reportData.getDataBuilder().getAbsoluteMaxValue();
                min = reportData.getDataBuilder().getAbsoluteMinValue();
                mean = Math.abs(mean);
                sum = Math.abs(sum);
            }
        } else {
            // exclude impact of null values in statistics
            max = reportData.getDataBuilder().getMaxExcludingNulls();
            min = reportData.getDataBuilder().getMinExcludingNulls();
            mean = meanWithSign = reportData.getDataBuilder().getMeanExcludingNulls();
            if ((min * max >= 0)) {
                // absolute calculation (all points over the x axis)
                max = reportData.getDataBuilder().getAbsoluteMaxExcludingNulls();
                min = reportData.getDataBuilder().getAbsoluteMinExcludingNulls();
                mean = Math.abs(mean);
                sum = Math.abs(sum);
            }
        }
        // chart limits
        ((TextView) findViewById(R.id.report_max_result)).setText(Utils.amountToString(reportData.getCurrency(), max.longValue()));
        ((TextView) findViewById(R.id.report_min_result)).setText(Utils.amountToString(reportData.getCurrency(), min.longValue()));
        // sum and mean
        ((TextView) findViewById(R.id.report_mean_result)).setText(Utils.amountToString(reportData.getCurrency(), mean.longValue()));
        ((TextView) findViewById(R.id.report_mean_result)).setTextColor(meanColor);
        ((TextView) findViewById(R.id.report_sum_result)).setText(Utils.amountToString(reportData.getCurrency(), sum.longValue()));

        // mean line
        LimitLine ll = new LimitLine(meanWithSign.floatValue() / 100.0f, getString(R.string.mean_line_label));
        ll.setTextColor(meanColor);
        ll.setLineColor(meanColor);
        ll.setLineWidth(1.0f);
        ll.setTextSize(12f);
        ll.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);

        chart.getAxisLeft().removeAllLimitLines();
        chart.getAxisLeft().addLimitLine(ll);
    }

    /**
     * Gets the reference currency registered on preferences or, if not registered, gets the default currency.
     *
     * @return The currency registered as a reference to display chart reports or the default currency if not configured yet.
     */
    private Currency getReferenceCurrency() {
        Currency c = MyPreferences.getReferenceCurrency(this);
        if (c == null) {
            prefCurNotSet = true;
            Collection<Currency> currencies = CurrencyCache.getAllCurrencies();
            if (currencies != null && currencies.size() > 0) {
                for (Currency currency : currencies) {
                    if (currency.isDefault) {
                        c = currency;
                        break;
                    }
                }
                if (c == null) {
                    c = getNewDefaultCurrency();
                }
            } else {
                c = getNewDefaultCurrency();
            }
        }
        return c;
    }

    /**
     * Gets default currency when currency is not set in report preferences.
     *
     * @return Default currency
     */
    private Currency getNewDefaultCurrency() {
        return Currency.defaultCurrency();
    }

    private void showPreferences() {
        // save preferences status before call report preferences activity
        initialPrefs = MyPreferences.getReportPreferences(this);
        // call report preferences activity asking for result when closed
        Intent intent = new Intent(this, ReportPreferencesActivity.class);
        startActivityForResult(intent, REPORT_PREFERENCES);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // See which child activity is calling us back.
        if (initialPrefs != null) {
            boolean changed = preferencesChanged(initialPrefs, MyPreferences.getReportPreferences(this));
            if (changed) {
                // rebuild data
                reportData.rebuild(this, db, startPeriod, periods[selectedPeriod], currency);
                refreshView();
            }
        }
    }

    /**
     * Check if preferences changed
     *
     * @param initial Preferences status before call Report Preferences Activity.
     * @param actual  Current preferences status.
     * @return True if preferences changed, false otherwise.
     */
    private boolean preferencesChanged(String[] initial, String[] actual) {
        boolean changed = false;
        // general report preferences
        // 0 reference currency
        if (!initial[0].equals(actual[0])) {
            // set reference currency
            currency = getReferenceCurrency();
            changed = true;
        }
        // 1 period of reference
        if (!initial[1].equals(actual[1])) {
            // change period length to the one set in report preferences
            int refPeriodLength = getPeriodOfReference();
            int previousPeriod = selectedPeriod;
            selectedPeriod = selectPeriodFromLength(refPeriodLength);
            processPeriodLengthChange(previousPeriod, false);
            changed = true;
        }
        // 2 reference month
        if (!initial[2].equals(actual[2])) {
            setStartPeriod(periods[selectedPeriod]);
            changed = true;
        }
        // 3 consider nulls in statistics (affects statistics only > recalculate)
        if (!initial[3].equals(actual[3])) {
            // affects statistics only - recalculate
            changed = true;
        }
        // 4 include <no filter> (rebuild will regenerate the filter Ids list)
        if (!initial[4].equals(actual[4])) {
            // the change will be processed in rebuild
            changed = true;
        }

        if (reportType == ReportType.BY_CATEGORY_BY_PERIOD) {
            // include sub categories in list (rebuild will regenerate the filter Ids list)
            if (!initial[5].equals(actual[5])) {
                // the change will be processed in rebuild
                changed = true;
            }
            // add sub categories result to root categories result (affects statistics only > recalculate)
            if (!initial[6].equals(actual[6])) changed = true;
        }
        return changed;
    }

    /**
     * Set the start period based on given period length and reference month registered in report preferences.
     * Start period = Reference Month - periodLength months
     *
     * @param periodLength The number of months to be represented in the 2D report.
     */
    private void setStartPeriod(int periodLength) {
        int refMonth = MyPreferences.getReferenceMonth(this);
        Calendar now = Calendar.getInstance();
        startPeriod = new GregorianCalendar(now.get(Calendar.YEAR), now.get(Calendar.MONTH), 1);
        if (refMonth != 0) {
            startPeriod.add(Calendar.MONTH, refMonth);
        }
        // move to start period (reference month - <periodLength> months)
        startPeriod.add(Calendar.MONTH, (-1) * periodLength + 1);
    }

    /**
     * Get the period of reference set in report preferences.
     *
     * @return The number of months to be represented in the 2D report.
     */
    private int getPeriodOfReference() {
        int periodLength = MyPreferences.getPeriodOfReference(this);
        if (periodLength == 0) {
            periodLength = ReportDataByPeriod.DEFAULT_PERIOD;
            prefPerNotSet = true;
        }
        return periodLength;
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        PinProtection.lock(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PinProtection.unlock(this);
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        pointDate.setText(DateUtils.formatDateTime(this, (long) e.getX(), DateUtils.FORMAT_NO_MONTH_DAY));
        pointAmount.setText(Utils.amountToString(currency, (long) e.getY() * 100));
        pointAmount.setTextColor(e.getY() >= 0 ? positive : negative);
    }

    @Override
    public void onNothingSelected() {
        if (pointDate != null) pointDate.setText("");
        if (pointAmount != null) pointAmount.setText("");
    }
}
