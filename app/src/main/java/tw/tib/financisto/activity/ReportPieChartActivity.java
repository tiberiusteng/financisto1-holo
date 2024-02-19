package tw.tib.financisto.activity;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;

import tw.tib.financisto.R;

public class ReportPieChartActivity extends Activity {
    public static final String TAG = "ReportPieChartActivity";

    public static final String PIE_CHART_DATA = "pie_chart_data";

    private PieChart chart;
    private ArrayList<PieEntry> entries = new ArrayList<>();
    private ArrayList<Integer> colors = new ArrayList<>();
    private PieDataSet dataset = new PieDataSet(entries, "");
    private PieData data = new PieData(dataset);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.report_piechart);

        chart = findViewById(R.id.chart);
        chart.setHighlightPerTapEnabled(true);
        chart.setUsePercentValues(true);
        chart.getDescription().setEnabled(false);

        chart.setExtraOffsets(0.f, 0.f, 0.f, 20.f);

        chart.setHoleColor(Color.BLACK);
        chart.setHoleRadius(30f);
        chart.setTransparentCircleRadius(35f);

        chart.setTransparentCircleColor(Color.BLACK);
        chart.setTransparentCircleAlpha(110);
        chart.setRotationEnabled(true);
        chart.setHighlightPerTapEnabled(true);

        Legend l = chart.getLegend();
        l.setTextSize(12.0f);
        l.setTextColor(Color.WHITE);
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        l.setOrientation(Legend.LegendOrientation.VERTICAL);
        l.setDrawInside(true);
        l.setEnabled(true);

        for (int c : ColorTemplate.COLORFUL_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.LIBERTY_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.VORDIPLOM_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.JOYFUL_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.PASTEL_COLORS)
            colors.add(c);

        dataset.setSliceSpace(1f);
        dataset.setSelectionShift(10f);
        dataset.setColors(colors);

        data.setValueFormatter(new PercentFormatter(chart));
        data.setValueTextSize(12f);
        data.setValueTextColor(Color.WHITE);

        chart.setData(data);

        Bundle args = getIntent().getExtras();
        if (args != null) {
            String json = args.getString(PIE_CHART_DATA);
            Log.d(TAG, json);
            if (json != null) {
                entries = new Gson().fromJson(json, new TypeToken<ArrayList<PieEntry>>(){}.getType());
                dataset.setValues(entries);
                data.notifyDataChanged();
                chart.highlightValues(null);
                chart.invalidate();
            }
        }
    }
}