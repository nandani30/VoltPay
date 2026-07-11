package com.voltpay.app.ui.analytics

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.widget.ScrollView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.voltpay.app.R
import com.voltpay.app.data.db.TransactionDao
import java.util.ArrayList
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.Executors

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var pbLoading: ProgressBar
    private lateinit var svContent: ScrollView
    private lateinit var tvWeekSpent: TextView
    private lateinit var tvWeekReceived: TextView
    private lateinit var tvWeekTxCount: TextView
    private lateinit var tvMonthSpent: TextView
    private lateinit var tvMonthReceived: TextView
    private lateinit var tvMonthLargest: TextView
    private lateinit var tvMonthAverage: TextView
    
    private lateinit var barChart: BarChart
    private lateinit var pieChart: PieChart
    private lateinit var lineChart: LineChart
    
    private lateinit var tvNoBarData: TextView
    private lateinit var tvNoPieData: TextView
    private lateinit var tvNoLineData: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        pbLoading = findViewById(R.id.pbLoading)
        svContent = findViewById(R.id.svContent)

        tvWeekSpent = findViewById(R.id.tvWeekSpent)
        tvWeekReceived = findViewById(R.id.tvWeekReceived)
        tvWeekTxCount = findViewById(R.id.tvWeekTxCount)

        tvMonthSpent = findViewById(R.id.tvMonthSpent)
        tvMonthReceived = findViewById(R.id.tvMonthReceived)
        tvMonthLargest = findViewById(R.id.tvMonthLargest)
        tvMonthAverage = findViewById(R.id.tvMonthAverage)

        barChart = findViewById(R.id.barChart)
        pieChart = findViewById(R.id.pieChart)
        lineChart = findViewById(R.id.lineChart)
        
        tvNoBarData = findViewById(R.id.tvNoBarData)
        tvNoPieData = findViewById(R.id.tvNoPieData)
        tvNoLineData = findViewById(R.id.tvNoLineData)

        loadAnalyticsData()
    }

    private fun loadAnalyticsData() {
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        
        pbLoading.visibility = View.VISIBLE
        svContent.visibility = View.GONE

        executor.execute {
            try {
                val dao = TransactionDao(this)
                
                // Week bounds
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val endWeek = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, -6)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val startWeek = cal.timeInMillis
                
                // Month bounds
                val mCal = Calendar.getInstance()
                mCal.set(Calendar.DAY_OF_MONTH, mCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                mCal.set(Calendar.HOUR_OF_DAY, 23)
                mCal.set(Calendar.MINUTE, 59)
                mCal.set(Calendar.SECOND, 59)
                val endMonth = mCal.timeInMillis
                mCal.set(Calendar.DAY_OF_MONTH, 1)
                mCal.set(Calendar.HOUR_OF_DAY, 0)
                mCal.set(Calendar.MINUTE, 0)
                mCal.set(Calendar.SECOND, 0)
                val startMonth = mCal.timeInMillis

                // Fetch Week Stats
                val weekTxs = dao.getByDateRange(startWeek, endWeek)
                val weekSpent = dao.getTotalAmountByType("DEBIT", startWeek, endWeek)
                val weekReceived = dao.getTotalAmountByType("CREDIT", startWeek, endWeek)
                val weekCount = weekTxs.size

                // Fetch Month Stats
                val monthTxs = dao.getByDateRange(startMonth, endMonth)
                val monthSpent = dao.getTotalAmountByType("DEBIT", startMonth, endMonth)
                val monthReceived = dao.getTotalAmountByType("CREDIT", startMonth, endMonth)
                
                var largestTx = 0.0
                var debitCount = 0
                for (tx in monthTxs) {
                    if (tx.type == "DEBIT") {
                        debitCount++
                        if (tx.amount > largestTx) largestTx = tx.amount
                    }
                }
                val averageTx = if (debitCount > 0) (monthSpent / debitCount) else 0.0

                // Fetch Chart Data
                val dailyTotals = dao.getDailyTotals(startWeek, endWeek)
                val monthlyTotals = dao.getMonthlyTotals(6)

                handler.post {
                    // Update UI
                    tvWeekSpent.text = String.format(Locale.getDefault(), "₹%.2f", weekSpent)
                    tvWeekReceived.text = String.format(Locale.getDefault(), "₹%.2f", weekReceived)
                    tvWeekTxCount.text = "$weekCount transactions this week"

                    tvMonthSpent.text = String.format(Locale.getDefault(), "₹%.2f", monthSpent)
                    tvMonthReceived.text = String.format(Locale.getDefault(), "₹%.2f", monthReceived)
                    tvMonthLargest.text = String.format(Locale.getDefault(), "Largest: ₹%.2f", largestTx)
                    tvMonthAverage.text = String.format(Locale.getDefault(), "Average: ₹%.2f", averageTx)

                    setupBarChart(dailyTotals, weekSpent)
                    setupPieChart(monthSpent, monthReceived)
                    setupLineChart(monthlyTotals)

                    pbLoading.visibility = View.GONE
                    svContent.visibility = View.VISIBLE
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                handler.post {
                    pbLoading.visibility = View.GONE
                    svContent.visibility = View.VISIBLE
                    tvWeekSpent.text = "Error"
                    tvNoBarData.text = t.toString()
                    tvNoBarData.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupBarChart(data: LinkedHashMap<String, Double>, weekSpent: Double) {
        barChart.description.isEnabled = false
        barChart.setTouchEnabled(false)
        barChart.legend.isEnabled = false
        barChart.axisRight.isEnabled = false
        barChart.axisLeft.setDrawGridLines(false)
        barChart.axisLeft.textColor = Color.WHITE
        barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        barChart.xAxis.setDrawGridLines(false)
        barChart.xAxis.textColor = Color.WHITE

        if (weekSpent == 0.0) {
            barChart.visibility = View.GONE
            tvNoBarData.visibility = View.VISIBLE
            return
        }
        barChart.visibility = View.VISIBLE
        tvNoBarData.visibility = View.GONE

        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        
        var i = 0
        for ((key, value) in data) {
            entries.add(BarEntry(i.toFloat(), value.toFloat()))
            labels.add(key)
            i++
        }

        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChart.xAxis.granularity = 1f

        val dataSet = BarDataSet(entries, "Daily Spend")
        dataSet.color = Color.parseColor("#2ED573")
        
        val barData = BarData(dataSet)
        barData.barWidth = 0.5f
        barData.setDrawValues(false)
        
        barChart.data = barData
        barChart.invalidate()
    }

    private fun setupPieChart(spent: Double, received: Double) {
        pieChart.description.isEnabled = false
        pieChart.setTouchEnabled(false)
        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(Color.parseColor("#1A1A1A"))
        pieChart.holeRadius = 40f
        pieChart.legend.isEnabled = true
        pieChart.legend.textColor = Color.WHITE
        pieChart.legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        pieChart.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        pieChart.setUsePercentValues(true)

        if (spent == 0.0 && received == 0.0) {
            pieChart.visibility = View.GONE
            tvNoPieData.visibility = View.VISIBLE
            return
        }
        pieChart.visibility = View.VISIBLE
        tvNoPieData.visibility = View.GONE

        val entries = ArrayList<PieEntry>()
        if (spent > 0) entries.add(PieEntry(spent.toFloat(), "Sent"))
        if (received > 0) entries.add(PieEntry(received.toFloat(), "Received"))

        val dataSet = PieDataSet(entries, "")
        val colors = ArrayList<Int>()
        if (spent > 0) colors.add(Color.parseColor("#FF4444")) // Red for spent
        if (received > 0) colors.add(Color.parseColor("#4ADE80")) // Green for received
        dataSet.colors = colors
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 12f

        val pieData = PieData(dataSet)
        pieData.setValueFormatter(PercentFormatter(pieChart))
        pieChart.data = pieData
        pieChart.invalidate()
    }

    private fun setupLineChart(data: LinkedHashMap<String, Double>) {
        lineChart.description.isEnabled = false
        lineChart.setTouchEnabled(false)
        lineChart.legend.isEnabled = false
        lineChart.axisRight.isEnabled = false
        lineChart.axisLeft.textColor = Color.WHITE
        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChart.xAxis.setDrawGridLines(false)
        lineChart.xAxis.textColor = Color.WHITE

        var nonZeroCount = 0
        for (value in data.values) {
            if (value > 0) nonZeroCount++
        }

        if (nonZeroCount < 2) {
            lineChart.visibility = View.GONE
            tvNoLineData.visibility = View.VISIBLE
            return
        }
        lineChart.visibility = View.VISIBLE
        tvNoLineData.visibility = View.GONE

        val entries = ArrayList<Entry>()
        val labels = ArrayList<String>()
        
        var i = 0
        for ((key, value) in data) {
            entries.add(Entry(i.toFloat(), value.toFloat()))
            labels.add(key)
            i++
        }

        lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        lineChart.xAxis.granularity = 1f

        val dataSet = LineDataSet(entries, "Monthly Spend")
        dataSet.color = Color.parseColor("#2ED573")
        dataSet.setCircleColor(Color.parseColor("#2ED573"))
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 4f
        dataSet.setDrawCircleHole(false)
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.parseColor("#2ED573")
        dataSet.fillAlpha = 100
        dataSet.setDrawValues(false)

        val lineData = LineData(dataSet)
        lineChart.data = lineData
        lineChart.invalidate()
    }
}
