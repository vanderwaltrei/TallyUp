@file:Suppress("PackageName")

package za.ac.iie.TallyUp.ui

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import za.ac.iie.TallyUp.R
import za.ac.iie.TallyUp.adapters.TransactionAdapter
import za.ac.iie.TallyUp.data.AppDatabase
import za.ac.iie.TallyUp.data.Transaction
import za.ac.iie.TallyUp.databinding.FragmentInsightsBinding
import za.ac.iie.TallyUp.data.AppRepository
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class InsightsFragment : Fragment() {

    private var _binding: FragmentInsightsBinding? = null
    private val binding get() = _binding!!

    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var appDatabase: AppDatabase
    private var allTransactions = listOf<Transaction>()
    private var currentTimeFilter = "Month"

    private val categoryColors = mapOf(
        "Food"      to R.color.color_food,
        "Transport" to R.color.color_transport,
        "Books"     to R.color.color_books,
        "Fun"       to R.color.color_fun,
        "Shopping"  to R.color.color_shopping,
        "Other"     to R.color.color_other,
        "Salary"    to R.color.color_salary,
        "Gift"      to R.color.color_gift,
        "Freelance" to R.color.color_freelance,
        "Allowance" to R.color.color_allowance
    )

    private val currencyFormatter = object : ValueFormatter() {
        override fun getFormattedValue(value: Float): String =
            if (value >= 1000f) "R${"%.1f".format(value / 1000f)}k"
            else "R${"%.0f".format(value)}"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInsightsBinding.inflate(inflater, container, false)
        appDatabase = AppDatabase.getDatabase(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTransactionList()
        setupTabs()
        setupTimeFilters()
        loadTransactions()

        binding.addFirstTransactionBtnAlt.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AddTransactionFragment())
                .addToBackStack("insights_to_add")
                .commit()
        }
    }

    private fun setupTransactionList() {
        transactionAdapter = TransactionAdapter()
        binding.transactionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
        }
    }

    private fun setupTabs() {
        binding.analysisTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                animateChartTransition()
                when (tab?.position) {
                    0 -> showTrendChart()
                    1 -> showDailyChart()
                    2 -> showCategoriesChart()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupTimeFilters() {
        val filterButtons = listOf(binding.filterToday, binding.filterWeek, binding.filterMonth)
        filterButtons.forEach { button ->
            button.setOnClickListener {
                filterButtons.forEach { btn ->
                    btn.background = null
                    btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.muted_foreground))
                }
                button.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.bg_tab_selected)
                button.setTextColor(ContextCompat.getColor(requireContext(), R.color.foreground))
                currentTimeFilter = button.text.toString()
                refreshData()
            }
        }
    }

    private fun refreshData() {
        val spent = calculateTotalSpentForPeriod()
        binding.totalSpent.text = "R${String.format("%.2f", spent)}"
        updateRemaining()
        updateQuickStats()

        binding.chartContainer.post {
            when (binding.analysisTabs.selectedTabPosition) {
                0    -> showTrendChart()
                1    -> showDailyChart()
                2    -> showCategoriesChart()
                else -> showTrendChart()
            }
        }
    }

    private fun loadTransactions() {
        val userId = getCurrentUserId()
        CoroutineScope(Dispatchers.Main).launch {
            val transactions = withContext(Dispatchers.IO) {
                appDatabase.transactionDao().getTransactionsForUser(userId)
            }
            allTransactions = transactions
            applyFilters()
            refreshData()
        }
    }

    private fun applyFilters() {
        val filtered = filterTransactionsByTime(allTransactions)
        transactionAdapter.submitList(filtered)
        updateVisibility(filtered)
    }

    private fun updateVisibility(filtered: List<Transaction>) {
        val hasTransactions = filtered.isNotEmpty()
        binding.emptyStateCard.visibility   = if (hasTransactions) View.GONE else View.VISIBLE
        binding.transactionsCard.visibility = if (hasTransactions) View.VISIBLE else View.GONE
        if (binding.analysisTabs.selectedTabPosition != 2) {
            binding.categoryBreakdownCard.visibility = View.GONE
        }
    }

    private fun filterTransactionsByTime(transactions: List<Transaction>): List<Transaction> =
        when (currentTimeFilter) {
            "Today" -> transactions.filter { isToday(it.date) }
            "Week"  -> transactions.filter { isThisWeek(it.date) }
            "Month" -> transactions.filter { isThisMonth(it.date) }
            else    -> transactions
        }

    private fun animateChartTransition() {
        binding.chartContainer.alpha = 0f
        binding.chartContainer.animate()
            .alpha(1f).setDuration(300)
            .setInterpolator(DecelerateInterpolator()).start()
    }

    // ════════════════════════════════════════════════════════════════════════
    //  TREND CHART (unchanged)
    // ════════════════════════════════════════════════════════════════════════

    private fun showTrendChart() {
        binding.chartContainer.removeAllViews()
        val chart = LineChart(requireContext())
        binding.chartContainer.addView(chart, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))

        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setPinchZoom(false)
        chart.setDrawGridBackground(false)
        chart.setDrawBorders(false)
        chart.legend.isEnabled = false
        chart.setExtraOffsets(16f, 20f, 16f, 16f)
        chart.setBackgroundColor(Color.TRANSPARENT)
        chart.setNoDataText("Add transactions to see your trend")
        chart.setNoDataTextColor(muted())

        val filtered = filterTransactionsByTime(allTransactions).sortedBy { it.date }
        val entries  = mutableListOf<Entry>()
        val labels   = mutableListOf<String>()
        var cumulative = 0f

        filtered.forEachIndexed { i, t ->
            cumulative += if (t.type == "Expense") t.amount.toFloat() else -t.amount.toFloat()
            entries.add(Entry(i.toFloat(), cumulative))
            labels.add(SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(t.date)))
        }
        if (entries.isEmpty()) { entries.add(Entry(0f, 0f)); labels.add("—") }

        val lineColor = accent()
        val fillGradient = android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Color.argb(165, Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor)),
                Color.TRANSPARENT
            )
        )

        val dataSet = LineDataSet(entries, "Trend").apply {
            color           = lineColor
            lineWidth       = 3.5f
            mode            = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity  = 0.15f
            setDrawCircles(true)
            setCircleColor(lineColor)
            circleRadius      = 8f
            circleHoleRadius  = 4f
            circleHoleColor   = Color.WHITE
            setDrawFilled(true)
            fillDrawable      = fillGradient
            setDrawValues(false)
            highLightColor    = lineColor

            enableDashedHighlightLine(14f, 8f, 0f)
        }

        chart.data = LineData(dataSet)
        styleXAxis(chart.xAxis, labels, rotateDeg = -25f)
        styleLeftAxis(chart.axisLeft)
        chart.axisRight.isEnabled = false
        chart.animateX(900, com.github.mikephil.charting.animation.Easing.EaseInOutCubic)
        chart.invalidate()
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DAILY CHART (unchanged)
    // ════════════════════════════════════════════════════════════════════════

    private fun showDailyChart() {
        binding.chartContainer.removeAllViews()
        val chart = BarChart(requireContext())
        binding.chartContainer.addView(chart, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))

        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setPinchZoom(false)
        chart.setDrawGridBackground(false)
        chart.setDrawBorders(false)
        chart.legend.isEnabled = false
        chart.setExtraOffsets(16f, 20f, 16f, 16f)
        chart.setFitBars(true)
        chart.setBackgroundColor(Color.TRANSPARENT)
        chart.setNoDataText("No expenses recorded yet")
        chart.setNoDataTextColor(muted())

        val filtered    = filterTransactionsByTime(allTransactions).filter { it.type == "Expense" }
        val dailyTotals = filtered
            .groupBy { SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(it.date)) }
            .map    { (day, txs) -> day to txs.sumOf { it.amount } }
            .sortedBy { it.first }

        val rawEntries = dailyTotals.mapIndexed { i, (_, amount) -> BarEntry(i.toFloat(), amount.toFloat()) }
        val entries = if (rawEntries.isEmpty()) listOf(BarEntry(0f, 0f)) else rawEntries

        val dataSet = BarDataSet(entries, "Daily Spending").apply {
            color          = accent()
            highLightColor = accent()
            highLightAlpha = 60
            setDrawValues(true)
            valueTextColor  = muted()
            valueTextSize   = 10f
            valueFormatter  = currencyFormatter
        }

        chart.data = BarData(dataSet).apply { barWidth = 0.6f }
        styleXAxis(chart.xAxis, dailyTotals.map { it.first }, rotateDeg = -25f)
        styleLeftAxis(chart.axisLeft)
        chart.axisRight.isEnabled = false
        chart.animateY(950, com.github.mikephil.charting.animation.Easing.EaseOutBounce)
        chart.invalidate()
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CATEGORIES — SOLID PIE CHART  ← FIXED
    // ════════════════════════════════════════════════════════════════════════

    private fun showCategoriesChart() {
        binding.chartContainer.removeAllViews()

        val filtered = filterTransactionsByTime(allTransactions).filter { it.type == "Expense" }
        val categoryTotals = filtered
            .groupBy { it.category }
            .map    { (cat, txs) -> cat to txs.sumOf { it.amount } }
            .sortedByDescending { it.second }

        val chart = PieChart(requireContext())
        binding.chartContainer.addView(chart, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))

        // ── SOLID pie — no hole, no centre text ─────────────────────────────
        chart.isDrawHoleEnabled    = false      // ← solid pie, not a donut
        chart.setDrawCenterText(false)

        chart.description.isEnabled  = false
        chart.setTouchEnabled(true)
        chart.isRotationEnabled      = true
        chart.isHighlightPerTapEnabled = true

        // Turn off labels drawn ON the slices — they get cramped and unreadable
        chart.setDrawEntryLabels(false)

        chart.setExtraOffsets(16f, 8f, 16f, 8f)
        chart.setBackgroundColor(Color.TRANSPARENT)
        chart.setNoDataText("No expense categories yet")
        chart.setNoDataTextColor(muted())

        // ── Legend below chart: colour dot + name + percentage ───────────────
        chart.legend.apply {
            isEnabled          = true
            isWordWrapEnabled  = true
            textColor          = fg()
            textSize           = 12f
            form               = Legend.LegendForm.CIRCLE
            formSize           = 12f
            xEntrySpace        = 16f
            yEntrySpace        = 6f
            horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            verticalAlignment   = Legend.LegendVerticalAlignment.BOTTOM
            orientation         = Legend.LegendOrientation.HORIZONTAL
        }

        if (categoryTotals.isEmpty()) {
            chart.data = null
            chart.invalidate()
            updateCategoryBreakdown(emptyList())
            return
        }

        val pieEntries = categoryTotals.map { (cat, amount) ->
            PieEntry(amount.toFloat(), cat)
        }

        val sliceColors = categoryTotals.map { (cat, _) ->
            ContextCompat.getColor(requireContext(), categoryColors[cat] ?: R.color.color_other)
        }

        val dataSet = PieDataSet(pieEntries, "").apply {
            colors        = sliceColors
            sliceSpace    = 3f              // clean white gap between slices
            selectionShift = 8f            // slice pops out on tap

            // ── Percentage labels OUTSIDE slices with leader lines ───────────
            setDrawValues(true)
            valueFormatter = PercentFormatter(chart)
            valueTextSize  = 12f
            valueTextColor = fg()           // dark text → readable on any bg

            // Place labels outside the pie with a short leader line
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            valueLinePart1OffsetPercentage = 90f
            valueLinePart1Length           = 0.5f
            valueLinePart2Length           = 0.4f
            valueLineColor                 = fg()
            valueLineWidth                 = 1f
            isUsingSliceColorAsValueLineColor = true  // leader line matches slice colour
        }

        chart.data = PieData(dataSet)
        chart.setUsePercentValues(true)

        chart.animateY(1000, com.github.mikephil.charting.animation.Easing.EaseInOutCubic)
        chart.invalidate()

        updateCategoryBreakdown(categoryTotals)
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SHARED AXIS STYLING
    // ════════════════════════════════════════════════════════════════════════

    private fun styleXAxis(
        xAxis: com.github.mikephil.charting.components.XAxis,
        labels: List<String>,
        rotateDeg: Float = -25f
    ) {
        xAxis.position           = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity        = 1f
        xAxis.valueFormatter     = IndexAxisValueFormatter(labels)
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(true)
        xAxis.axisLineColor      = border()
        xAxis.axisLineWidth      = 1f
        xAxis.textColor          = muted()
        xAxis.textSize           = 11f
        xAxis.labelRotationAngle = rotateDeg
        if (labels.size > 8) xAxis.setLabelCount(7, true)
    }

    private fun styleLeftAxis(axisLeft: YAxis) {
        axisLeft.setDrawGridLines(true)
        axisLeft.gridColor      = border()
        axisLeft.gridLineWidth  = 0.8f
        axisLeft.setDrawAxisLine(false)
        axisLeft.textColor      = muted()
        axisLeft.textSize       = 11f
        axisLeft.valueFormatter = currencyFormatter
        axisLeft.axisMinimum    = 0f
        axisLeft.spaceTop       = 15f
    }

    // ════════════════════════════════════════════════════════════════════════
    //  COLOUR HELPERS
    // ════════════════════════════════════════════════════════════════════════

    private fun accent() = ContextCompat.getColor(requireContext(), R.color.accent)
    private fun muted()  = ContextCompat.getColor(requireContext(), R.color.muted_foreground)
    private fun border() = ContextCompat.getColor(requireContext(), R.color.border)
    private fun fg()     = ContextCompat.getColor(requireContext(), R.color.foreground)

    // ════════════════════════════════════════════════════════════════════════
    //  CATEGORY BREAKDOWN ROWS (original, unchanged)
    // ════════════════════════════════════════════════════════════════════════

    private fun updateCategoryBreakdown(categoryTotals: List<Pair<String, Double>>) {
        val container = binding.categoryBarsContainer
        container.removeAllViews()

        if (categoryTotals.isEmpty()) {
            binding.categoryBreakdownCard.visibility = View.GONE
            return
        }

        binding.categoryBreakdownCard.visibility = View.VISIBLE
        val maxAmount = categoryTotals.maxOf { it.second }.toFloat()

        categoryTotals.take(5).forEach { (category, amount) ->
            val itemView = layoutInflater.inflate(
                R.layout.item_category_progress, container, false
            )
            itemView.findViewById<TextView>(R.id.categoryName).text   = category
            itemView.findViewById<TextView>(R.id.categoryAmount).text =
                "R${String.format("%.2f", amount)}"

            val color = ContextCompat.getColor(
                requireContext(), categoryColors[category] ?: R.color.color_other
            )
            val progressBar =
                itemView.findViewById<android.widget.ProgressBar>(R.id.categoryProgress)
            progressBar.progressDrawable?.setTint(color)
            progressBar.progress =
                if (maxAmount > 0) ((amount / maxAmount) * 100).toInt() else 0

            container.addView(itemView)
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DATE HELPERS (original, unchanged)
    // ════════════════════════════════════════════════════════════════════════

    private fun isToday(date: Long): Boolean {
        val a = Calendar.getInstance()
        val b = Calendar.getInstance().apply { timeInMillis = date }
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }

    private fun isThisWeek(date: Long): Boolean {
        val a = Calendar.getInstance()
        val b = Calendar.getInstance().apply { timeInMillis = date }
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.WEEK_OF_YEAR) == b.get(Calendar.WEEK_OF_YEAR)
    }

    private fun isThisMonth(date: Long): Boolean {
        val a = Calendar.getInstance()
        val b = Calendar.getInstance().apply { timeInMillis = date }
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.MONTH) == b.get(Calendar.MONTH)
    }

    private fun getCurrentUserId(): String =
        requireContext()
            .getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
            .getString("userId", "") ?: "default"

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ════════════════════════════════════════════════════════════════════════
    //  STATS HELPERS (original, unchanged)
    // ════════════════════════════════════════════════════════════════════════

    private fun calculateTotalSpentForPeriod(): Double =
        filterTransactionsByTime(allTransactions)
            .filter { it.type == "Expense" }
            .sumOf { it.amount }

    private fun getMonthlyBudget(): Double {
        val appState = AppRepository(requireContext()).loadAppState()
        return appState.budgetCategories.sumOf { it.budgeted }
    }

    private fun updateRemaining() {
        val remaining = getMonthlyBudget() - calculateTotalSpentForPeriod()
        binding.remainingAmount.text = "R${"%.2f".format(abs(remaining))}"
    }

    private fun updateQuickStats() {
        val filtered = filterTransactionsByTime(allTransactions).filter { it.type == "Expense" }

        val days = when (currentTimeFilter) {
            "Today" -> 1
            "Week"  -> 7
            "Month" -> Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            else    -> 30
        }
        val totalSpent = filtered.sumOf { it.amount }
        val avgDaily   = if (days > 0) totalSpent / days else 0.0
        binding.avgDailyText.text = "R${String.format("%.2f", avgDaily)}"

        binding.topCategoryText.text = filtered
            .groupBy { it.category }
            .maxByOrNull { it.value.sumOf { t -> t.amount } }
            ?.key ?: "None"

        binding.TranAmount.text =
            "${filtered.size} transactions this ${currentTimeFilter.lowercase()}"

        val categoryTotals = filtered
            .groupBy { it.category }
            .map { (cat, txs) -> cat to txs.sumOf { it.amount } }
            .sortedByDescending { it.second }

        if (categoryTotals.isNotEmpty() && binding.analysisTabs.selectedTabPosition == 2) {
            updateCategoryBreakdown(categoryTotals)
        }
    }
}