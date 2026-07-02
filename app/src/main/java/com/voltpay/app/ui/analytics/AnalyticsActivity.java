package com.voltpay.app.ui.analytics;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import androidx.core.widget.NestedScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.voltpay.app.R;
import com.voltpay.app.data.db.TransactionDao;
import com.voltpay.app.data.model.Transaction;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AnalyticsActivity extends AppCompatActivity {

    private ProgressBar pbLoading;
    private NestedScrollView svContent;
    private TextView tvWeekSpent, tvWeekReceived, tvWeekTxCount;
    private TextView tvMonthSpent, tvMonthReceived, tvMonthLargest, tvMonthAverage;
    
    private BarChart barChart;
    private PieChart pieChart;
    private LineChart lineChart;
    
    private TextView tvNoBarData, tvNoPieData, tvNoLineData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        pbLoading = findViewById(R.id.pbLoading);
        svContent = findViewById(R.id.svContent);

        tvWeekSpent = findViewById(R.id.tvWeekSpent);
        tvWeekReceived = findViewById(R.id.tvWeekReceived);
        tvWeekTxCount = findViewById(R.id.tvWeekTxCount);

        tvMonthSpent = findViewById(R.id.tvMonthSpent);
        tvMonthReceived = findViewById(R.id.tvMonthReceived);
        tvMonthLargest = findViewById(R.id.tvMonthLargest);
        tvMonthAverage = findViewById(R.id.tvMonthAverage);

        barChart = findViewById(R.id.barChart);
        pieChart = findViewById(R.id.pieChart);
        lineChart = findViewById(R.id.lineChart);
        
        tvNoBarData = findViewById(R.id.tvNoBarData);
        tvNoPieData = findViewById(R.id.tvNoPieData);
        tvNoLineData = findViewById(R.id.tvNoLineData);

        loadAnalyticsData();
    }

    private void loadAnalyticsData() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        
        pbLoading.setVisibility(View.VISIBLE);
        svContent.setVisibility(View.GONE);

        executor.execute(() -> {
            try {
                TransactionDao dao = new TransactionDao(this);
                
                // Week bounds
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);
                long endWeek = cal.getTimeInMillis();
                cal.add(Calendar.DAY_OF_YEAR, -6);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                long startWeek = cal.getTimeInMillis();
                
                // Month bounds
                Calendar mCal = Calendar.getInstance();
                mCal.set(Calendar.DAY_OF_MONTH, mCal.getActualMaximum(Calendar.DAY_OF_MONTH));
                mCal.set(Calendar.HOUR_OF_DAY, 23);
                mCal.set(Calendar.MINUTE, 59);
                mCal.set(Calendar.SECOND, 59);
                long endMonth = mCal.getTimeInMillis();
                mCal.set(Calendar.DAY_OF_MONTH, 1);
                mCal.set(Calendar.HOUR_OF_DAY, 0);
                mCal.set(Calendar.MINUTE, 0);
                mCal.set(Calendar.SECOND, 0);
                long startMonth = mCal.getTimeInMillis();

                // Fetch Week Stats
                List<Transaction> weekTxs = dao.getByDateRange(startWeek, endWeek);
                double weekSpent = dao.getTotalAmountByType("DEBIT", startWeek, endWeek);
                double weekReceived = dao.getTotalAmountByType("CREDIT", startWeek, endWeek);
                int weekCount = weekTxs.size();

                // Fetch Month Stats
                List<Transaction> monthTxs = dao.getByDateRange(startMonth, endMonth);
                double monthSpent = dao.getTotalAmountByType("DEBIT", startMonth, endMonth);
                double monthReceived = dao.getTotalAmountByType("CREDIT", startMonth, endMonth);
                
                double largestTx = 0;
                int debitCount = 0;
                for (Transaction tx : monthTxs) {
                    if ("DEBIT".equals(tx.getType())) {
                        debitCount++;
                        if (tx.getAmount() > largestTx) largestTx = tx.getAmount();
                    }
                }
                double averageTx = debitCount > 0 ? (monthSpent / debitCount) : 0;
                
                final double finalLargestTx = largestTx;
                final double finalAverageTx = averageTx;

                // Fetch Chart Data
                LinkedHashMap<String, Double> dailyTotals = dao.getDailyTotals(startWeek, endWeek);
                LinkedHashMap<String, Double> monthlyTotals = dao.getMonthlyTotals(6);

                handler.post(() -> {
                    // Update UI
                    tvWeekSpent.setText(String.format(Locale.getDefault(), "₹%.2f", weekSpent));
                    tvWeekReceived.setText(String.format(Locale.getDefault(), "₹%.2f", weekReceived));
                    tvWeekTxCount.setText(weekCount + " transactions this week");

                    tvMonthSpent.setText(String.format(Locale.getDefault(), "₹%.2f", monthSpent));
                    tvMonthReceived.setText(String.format(Locale.getDefault(), "₹%.2f", monthReceived));
                    tvMonthLargest.setText(String.format(Locale.getDefault(), "Largest: ₹%.2f", finalLargestTx));
                    tvMonthAverage.setText(String.format(Locale.getDefault(), "Average: ₹%.2f", finalAverageTx));

                    setupBarChart(dailyTotals, weekSpent);
                    setupPieChart(monthSpent, monthReceived);
                    setupLineChart(monthlyTotals);

                    pbLoading.setVisibility(View.GONE);
                    svContent.setVisibility(View.VISIBLE);
                });
            } catch (Throwable t) {
                t.printStackTrace();
                handler.post(() -> {
                    pbLoading.setVisibility(View.GONE);
                    svContent.setVisibility(View.VISIBLE);
                    tvWeekSpent.setText("Error");
                    tvNoBarData.setText(t.toString());
                    tvNoBarData.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private void setupBarChart(LinkedHashMap<String, Double> data, double weekSpent) {
        barChart.getDescription().setEnabled(false);
        barChart.setTouchEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.getAxisLeft().setDrawGridLines(false);
        barChart.getAxisLeft().setTextColor(Color.WHITE);
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setDrawGridLines(false);
        barChart.getXAxis().setTextColor(Color.WHITE);

        if (weekSpent == 0) {
            barChart.setVisibility(View.GONE);
            tvNoBarData.setVisibility(View.VISIBLE);
            return;
        }
        barChart.setVisibility(View.VISIBLE);
        tvNoBarData.setVisibility(View.GONE);

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        
        int i = 0;
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            entries.add(new BarEntry(i, entry.getValue().floatValue()));
            labels.add(entry.getKey());
            i++;
        }

        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChart.getXAxis().setGranularity(1f);

        BarDataSet dataSet = new BarDataSet(entries, "Daily Spend");
        dataSet.setColor(Color.parseColor("#2ED573"));
        
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);
        barData.setDrawValues(false);
        
        barChart.setData(barData);
        barChart.invalidate();
    }

    private void setupPieChart(double spent, double received) {
        pieChart.getDescription().setEnabled(false);
        pieChart.setTouchEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.parseColor("#1A1A1A"));
        pieChart.setHoleRadius(40f);
        pieChart.getLegend().setEnabled(true);
        pieChart.getLegend().setTextColor(Color.WHITE);
        pieChart.getLegend().setVerticalAlignment(com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM);
        pieChart.getLegend().setHorizontalAlignment(com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER);
        pieChart.setUsePercentValues(true);

        if (spent == 0 && received == 0) {
            pieChart.setVisibility(View.GONE);
            tvNoPieData.setVisibility(View.VISIBLE);
            return;
        }
        pieChart.setVisibility(View.VISIBLE);
        tvNoPieData.setVisibility(View.GONE);

        ArrayList<PieEntry> entries = new ArrayList<>();
        if (spent > 0) entries.add(new PieEntry((float) spent, "Sent"));
        if (received > 0) entries.add(new PieEntry((float) received, "Received"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        ArrayList<Integer> colors = new ArrayList<>();
        if (spent > 0) colors.add(Color.parseColor("#FF4444")); // Red for spent
        if (received > 0) colors.add(Color.parseColor("#4ADE80")); // Green for received
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new PercentFormatter(pieChart));
        pieChart.setData(pieData);
        pieChart.invalidate();
    }

    private void setupLineChart(LinkedHashMap<String, Double> data) {
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getAxisLeft().setTextColor(Color.WHITE);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getXAxis().setDrawGridLines(false);
        lineChart.getXAxis().setTextColor(Color.WHITE);

        int nonZeroCount = 0;
        for (Double val : data.values()) {
            if (val > 0) nonZeroCount++;
        }

        if (nonZeroCount < 2) {
            lineChart.setVisibility(View.GONE);
            tvNoLineData.setVisibility(View.VISIBLE);
            return;
        }
        lineChart.setVisibility(View.VISIBLE);
        tvNoLineData.setVisibility(View.GONE);

        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        
        int i = 0;
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            entries.add(new Entry(i, entry.getValue().floatValue()));
            labels.add(entry.getKey());
            i++;
        }

        lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        lineChart.getXAxis().setGranularity(1f);

        LineDataSet dataSet = new LineDataSet(entries, "Monthly Spend");
        dataSet.setColor(Color.parseColor("#2ED573"));
        dataSet.setCircleColor(Color.parseColor("#2ED573"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#2ED573"));
        dataSet.setFillAlpha(100);
        dataSet.setDrawValues(false);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.invalidate();
    }
}
