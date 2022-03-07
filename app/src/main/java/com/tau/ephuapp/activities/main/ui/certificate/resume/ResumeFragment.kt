package com.tau.ephuapp.activities.main.ui.certificate.resume

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.anychart.AnyChart
import com.anychart.AnyChartView
import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.chart.common.dataentry.ValueDataEntry
import com.anychart.charts.Cartesian
import com.anychart.core.cartesian.series.Column
import com.anychart.enums.*
import com.tau.ephuapp.R
import com.tau.ephuapp.activities.CertificateActivity
import com.tau.ephuapp.activities.CertificateActivityViewModel
import com.tau.ephuapp.databinding.FragmentResumeBinding
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class ResumeFragment : Fragment() {
    private val viewModel: CertificateActivityViewModel by activityViewModels()
    var _binding: FragmentResumeBinding? = null
    var totalCertifiedItems: Int = 0
    var totalItemsToCertify: Int = 0
    var totalPendingItemsToCertify: Int = 0
    var chartsInitialized = false
    var initializingCharts = false
    val binding get() = _binding
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentResumeBinding.inflate(inflater, container, false)
        viewModel.certifiedItems.observe(viewLifecycleOwner, Observer { certifiedDeliveryLines ->
            totalCertifiedItems = certifiedDeliveryLines?.sumBy {
                it.quantity
            } ?: 0
            totalItemsToCertify = viewModel.currentCertificationTaskItems.value?.sumBy {
                it.totalUnits
            } ?: 0
            totalPendingItemsToCertify = totalItemsToCertify - totalCertifiedItems
            if(!initializingCharts) {
                initCharts()
            }
        })
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as CertificateActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (activity as CertificateActivity).supportActionBar?.setDisplayShowHomeEnabled(true)
        if (!chartsInitialized && !initializingCharts) {
            initCharts()
        }
    }

    fun initCharts(){
        initializingCharts = true
        doAsync {
            /*val pieChartView: AnyChartView? = binding?.pieChartView
            pieChartView?.setProgressBar(binding?.progressBar)
            val pie = AnyChart.pie()
            pie.setOnClickListener(object :
                    ListenersInterface.OnClickListener(arrayOf("x", "value")) {
                override fun onClick(event: Event) {
                    Toast.makeText(
                            this@ResumeFragment.requireContext(),
                            event.getData().get("x").toString() + ":" + event.getData().get("value"),
                            Toast.LENGTH_SHORT
                    ).show()
                }
            })
            val pieData: MutableList<DataEntry> = ArrayList()
            pieData.add(ValueDataEntry(getString(R.string.certified), totalCertifiedItems))
            pieData.add(ValueDataEntry(getString(R.string.not_certified), totalPendingItemsToCertify))
            pie.data(pieData)
            pie.title(context?.getString(R.string.certified_items_for_task, viewModel.currentTask.value?.id))
            pie.labels().position("outside")
            pie.legend()
                .position("center-bottom")
                .itemsLayout(LegendLayout.HORIZONTAL)
                .align(Align.CENTER)*/
            val barChartView: AnyChartView? = binding?.barChartView
            barChartView?.setProgressBar(binding?.progressBar)
            val cartesian: Cartesian = AnyChart.column()
            val barData: MutableList<DataEntry> = ArrayList()
            barData.add(ValueDataEntry(getString(R.string.total_items), totalItemsToCertify))
            barData.add(ValueDataEntry(getString(R.string.pending), totalPendingItemsToCertify))
            barData.add(ValueDataEntry(getString(R.string.certifieds), totalCertifiedItems))
            val column: Column = cartesian.column(barData)
            column.tooltip()
                    .titleFormat("{%X}")
                    .position(Position.CENTER_BOTTOM)
                    .anchor(Anchor.CENTER_BOTTOM)
                    .offsetX(0.0)
                    .offsetY(5.0)
                    .format("{%Value}{groupsSeparator: }")
            cartesian.animation(true)
            cartesian.title(getString(R.string.bar_chart_title))
            cartesian.yScale().minimum(0.0)
            cartesian.yAxis(0).labels().format("{%Value}{groupsSeparator: }")
            cartesian.tooltip().positionMode(TooltipPositionMode.POINT)
            cartesian.interactivity().hoverMode(HoverMode.BY_X)
            cartesian.xAxis(0).title(getString(R.string.categories))
            cartesian.yAxis(0).title(getString(R.string.quantity))
            uiThread {
                barChartView?.setChart(cartesian)
                //pieChartView?.setChart(pie)
                chartsInitialized = true
                initializingCharts = false
            }
        }
    }

    companion object{
        private const val TAG = "RESUME_FRAGMENT"
    }
}