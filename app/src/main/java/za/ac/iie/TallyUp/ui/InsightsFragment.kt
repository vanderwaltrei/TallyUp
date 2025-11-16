@file:Suppress("PackageName")

package za.ac.iie.TallyUp.ui

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.*
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import za.ac.iie.TallyUp.adapters.TransactionAdapter
import za.ac.iie.TallyUp.data.AppDatabase
import za.ac.iie.TallyUp.data.Transaction
import za.ac.iie.TallyUp.databinding.FragmentInsightsBinding
import java.text.SimpleDateFormat
import java.util.*
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlin.math.roundToInt
import za.ac.iie.TallyUp.R

class InsightsFragment : Fragment() {

    private var _binding: FragmentInsightsBinding? = null
    private val binding get() = _binding!!

    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var appDatabase: AppDatabase
    private var allTransactions = listOf<Transaction>()
    private var currentTypeFilter = "All"
    private var currentTimeFilter = "All"
    private var currentCategoryFilter = "All"

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

        setupTabs()
        setupTransactionList()
        loadTransactions()

        // Open AddTransactionFragment when button is clicked
        binding.addFirstTransactionBtnAlt.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AddTransactionFragment())
                .addToBackStack("insights_to_add")
                .commit()
        }
    }

    private fun setupTabs() {
        binding.analysisTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showTrendChart()
                    1 -> showDailyChart()
                    2 -> showCategoriesChart()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        showTrendChart() // Default tab
    }

    private fun setupTransactionList() {
        transactionAdapter = TransactionAdapter()
        binding.transactionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
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
        }
    }

    private fun applyFilters() {
        var filtered = allTransactions

        if (currentTypeFilter != "All") {
            filtered = filtered.filter { it.type == currentTypeFilter }
        }

        filtered = when (currentTimeFilter) {
            "Today" -> filtered.filter { isToday(it.date) }
            "This Week" -> filtered.filter { isThisWeek(it.date) }
            "This Month" -> filtered.filter { isThisMonth(it.date) }
            else -> filtered
        }

        if (currentCategoryFilter != "All") {
            filtered = filtered.filter { it.category == currentCategoryFilter }
        }

        transactionAdapter.submitList(filtered)
        updateVisibility(filtered)
    }

    private fun updateVisibility(filtered: List<Transaction>) {
        val hasTransactions = filtered.isNotEmpty()

        val emptyStateVisibility = if (hasTransactions) View.GONE else View.VISIBLE
        binding.tvSpend1.visibility = emptyStateVisibility
        binding.tvSpend2.visibility = emptyStateVisibility
        binding.imgSpend.visibility = emptyStateVisibility
        binding.addFirstTransactionBtnAlt.visibility = emptyStateVisibility

        val dataVisibility = if (hasTransactions) View.VISIBLE else View.GONE
        binding.tvSpend3.visibility = dataVisibility
        binding.transactionsRecyclerView.visibility = dataVisibility
    }

    private fun filterTransactionsByTime(transactions: List<Transaction>): List<Transaction> {
        return when (currentTimeFilter) {
            "Today" -> transactions.filter { isToday(it.date) }
            "This Week" -> transactions.filter { isThisWeek(it.date) }
            "This Month" -> transactions.filter { isThisMonth(it.date) }
            else -> transactions
        }
    }

    // === Chart implementations ===

    private fun showTrendChart() {
        binding.chartContainer.removeAllViews()
        val chart = LineChart(requireContext())
        binding.chartContainer.addView(chart)

        chart.description.isEnabled = false  // remove "Description Label"
        chart.setTouchEnabled(true)
        chart.setPinchZoom(true)

        val filtered = filterTransactionsByTime(allTransactions).sortedBy { it.date }

        val entries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()
        var cumulative = 0f
        filtered.forEachIndexed { index, t ->
            cumulative += if (t.type == "Expense") t.amount.toFloat() else -t.amount.toFloat()
            entries.add(Entry(index.toFloat(), cumulative))
            labels.add(SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(t.date)))
        }

        val dataSet = LineDataSet(entries, "Trend")
        dataSet.color = Color.BLUE
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.CYAN
        val lineData = LineData(dataSet)
        chart.data = lineData

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)

        chart.invalidate()
    }

    private fun showDailyChart() {
        binding.chartContainer.removeAllViews()
        val chart = BarChart(requireContext())
        binding.chartContainer.addView(chart)

        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setPinchZoom(true)

        val filtered = filterTransactionsByTime(allTransactions).filter { it.type == "Expense" }

        val dailyTotals = filtered
            .groupBy { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.date)) }
            .map { (day, txs) -> day to txs.sumOf { it.amount } }

        val entries = dailyTotals.mapIndexed { index, pair ->
            BarEntry(index.toFloat(), pair.second.toFloat())
        }

        val dataSet = BarDataSet(entries, "Daily Spending")
        // Red bars with ~40% transparency
        dataSet.color = 0x66FF0000.toInt()

        val barData = BarData(dataSet)
        chart.data = barData

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = IndexAxisValueFormatter(dailyTotals.map { it.first })
        xAxis.granularity = 1f

        chart.invalidate()
    }

    private fun showCategoriesChart() {
        binding.chartContainer.removeAllViews()
        val chart = BarChart(requireContext())
        binding.chartContainer.addView(chart)

        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setPinchZoom(true)

        val filtered = filterTransactionsByTime(allTransactions).filter { it.type == "Expense" }

        // Group transactions by category
        val categoryTotals = filtered.groupBy { it.category }
            .map { (category, txs) -> category to txs.sumOf { it.amount } }

        // Entries for the chart
        val entries = categoryTotals.mapIndexed { index, pair ->
            BarEntry(index.toFloat(), pair.second.toFloat())
        }

        val dataSet = BarDataSet(entries, "Category Spending")
        val categoryColors = categoryTotals.map { (category, _) ->
            when (category) {
                "Transport" -> 0x660000FF.toInt() // Blue
                "Food" -> 0x66FFA500.toInt()      // Orange
                "Shopping" -> 0x66FF0000.toInt()  // Red
                "Other" -> 0x66999999.toInt()     // Grey
                "Fun" -> 0x66FFFF00.toInt()       // Yellow
                "Books" -> 0x6600FF00.toInt()     // Green
                else -> 0x66AAAAAA.toInt()        // fallback grey
            }
        }
        dataSet.colors = categoryColors

        val barData = BarData(dataSet)
        chart.data = barData

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = IndexAxisValueFormatter(categoryTotals.map { it.first })
        xAxis.granularity = 1f

        chart.invalidate()
    }

    // === Date helpers ===
    private fun isToday(date: Long): Boolean {
        val today = Calendar.getInstance()
        val transactionDate = Calendar.getInstance().apply { timeInMillis = date }
        return today.get(Calendar.YEAR) == transactionDate.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == transactionDate.get(Calendar.DAY_OF_YEAR)
    }

    private fun isThisWeek(date: Long): Boolean {
        val now = Calendar.getInstance()
        val transactionDate = Calendar.getInstance().apply { timeInMillis = date }
        return now.get(Calendar.YEAR) == transactionDate.get(Calendar.YEAR) &&
                now.get(Calendar.WEEK_OF_YEAR) == transactionDate.get(Calendar.WEEK_OF_YEAR)
    }

    private fun isThisMonth(date: Long): Boolean {
        val now = Calendar.getInstance()
        val transactionDate = Calendar.getInstance().apply { timeInMillis = date }
        return now.get(Calendar.YEAR) == transactionDate.get(Calendar.YEAR) &&
                now.get(Calendar.MONTH) == transactionDate.get(Calendar.MONTH)
    }

    private fun getCurrentUserId(): String {
        val prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
        return prefs.getString("userId", "") ?: "default"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}