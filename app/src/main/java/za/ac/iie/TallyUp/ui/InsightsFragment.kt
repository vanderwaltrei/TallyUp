@file:Suppress("PackageName")

package za.ac.iie.TallyUp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import za.ac.iie.TallyUp.databinding.FragmentInsightsBinding

class InsightsFragment : Fragment() {

    private var _binding: FragmentInsightsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInsightsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTabs()
        loadData()
    }

    private fun setupTabs() {
        binding.analysisTabs.addOnTabSelectedListener(object :
            com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showTrendChart()
                    1 -> showDailyChart()
                    2 -> showCategoriesChart()
                }
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })

        // Default tab
        showTrendChart()
    }

    private fun loadData() {
        val hasTransactions = true // dummy
        binding.noDataCard.visibility = if (hasTransactions) View.GONE else View.VISIBLE
    }

    // === Trend (Line Chart) ===
    private fun showTrendChart() {
        binding.chartContainer.removeAllViews()
        val chart = LineChart(requireContext())
        val entries = listOf(
            Entry(1f, 120f), Entry(2f, 110f), Entry(3f, 80f),
            Entry(4f, 160f), Entry(5f, 140f), Entry(6f, 150f), Entry(7f, 200f)
        )
        val dataSet = LineDataSet(entries, "Trend").apply {
            color = resources.getColor(android.R.color.holo_blue_light, null)
            valueTextColor = resources.getColor(android.R.color.white, null)
            lineWidth = 2f
            setCircleColor(resources.getColor(android.R.color.holo_blue_dark, null))
        }
        chart.data = LineData(dataSet)
        chart.description = Description().apply { text = "" }
        binding.chartContainer.addView(chart)
    }

    // === Daily (Stacked Bar Chart) ===
    private fun showDailyChart() {
        binding.chartContainer.removeAllViews()
        val chart = BarChart(requireContext())
        val entries = listOf(
            BarEntry(1f, floatArrayOf(40f, 20f, 30f, 10f)),
            BarEntry(2f, floatArrayOf(30f, 50f, 20f, 40f)),
            BarEntry(3f, floatArrayOf(20f, 60f, 10f, 30f)),
            BarEntry(4f, floatArrayOf(50f, 40f, 60f, 30f)),
            BarEntry(5f, floatArrayOf(40f, 20f, 30f, 50f))
        )
        val dataSet = BarDataSet(entries, "Daily Breakdown").apply {
            setColors(*ColorTemplate.MATERIAL_COLORS)
            stackLabels = arrayOf("Food", "Transport", "Bills", "Other")
            valueTextColor = resources.getColor(android.R.color.white, null)
        }
        chart.data = BarData(dataSet)
        chart.description = Description().apply { text = "" }
        binding.chartContainer.addView(chart)
    }

    // === Categories (Pie Chart) ===
    private fun showCategoriesChart() {
        binding.chartContainer.removeAllViews()
        val chart = PieChart(requireContext())
        val entries = listOf(
            PieEntry(40f, "Food"),
            PieEntry(25f, "Transport"),
            PieEntry(20f, "Bills"),
            PieEntry(15f, "Other")
        )
        val dataSet = PieDataSet(entries, "Categories").apply {
            setColors(*ColorTemplate.COLORFUL_COLORS)
            valueTextColor = resources.getColor(android.R.color.white, null)
            valueTextSize = 14f
        }
        chart.data = PieData(dataSet)
        chart.description = Description().apply { text = "" }
        chart.setUsePercentValues(true)
        chart.setEntryLabelColor(resources.getColor(android.R.color.white, null))
        binding.chartContainer.addView(chart)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
