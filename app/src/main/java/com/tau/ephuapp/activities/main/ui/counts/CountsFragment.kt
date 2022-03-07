package com.tau.ephuapp.activities.main.ui.counts

import android.database.sqlite.SQLiteAccessPermException
import android.database.sqlite.SQLiteCantOpenDatabaseException
import android.database.sqlite.SQLiteDatabaseLockedException
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.WorkInfo
import com.tau.ephuapp.R
import com.tau.ephuapp.activities.main.MainActivityViewModel
import com.tau.ephuapp.adapters.CountExtendedAdapter
import com.tau.ephuapp.database.AppDatabase
import com.tau.ephuapp.databinding.FragmentCountsBinding
import com.tau.ephuapp.models.ItemCount
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class CountsFragment : Fragment() {
    private var taskFilterStr: String? = null
    private var filtered: Boolean = false
    private lateinit var viewModel: MainActivityViewModel
    private var mAdapter: CountExtendedAdapter? = null
    private var filteredData = arrayListOf<ItemCount>()
    private var _binding: FragmentCountsBinding? = null
    private val binding get() = _binding
    private lateinit var db: AppDatabase
    //private lateinit var taskType: TaskType
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCountsBinding.inflate(inflater, container, false)
        binding?.titleTv?.text = getString(R.string.task_list_title, DateUtils.formatDateTime(context, System.currentTimeMillis(), 0), 0)
        activity?.actionBar?.title = getString(R.string.saved_counts)
        val _viewModel: MainActivityViewModel by activityViewModels()
        viewModel = _viewModel
        try {
            db = AppDatabase.getDatabase(requireContext())
        } catch (ex: SQLiteDatabaseLockedException) {
            Log.e(TAG, "Database error found", ex)
        } catch (ex: SQLiteAccessPermException) {
            Log.e(TAG, "Database error found", ex)
        } catch (ex: SQLiteCantOpenDatabaseException) {
            Log.e(TAG, "Database error found", ex)
        }
        initAdapter()
        fetchCounts()
        viewModel.savingCountsWorkProgress.observe(viewLifecycleOwner, {
            it.forEach { workInfo ->
                if (WorkInfo.State.ENQUEUED == workInfo.state) {
                    Log.i(TAG, "progreso de subida de conteos observado...trabajo encolado")
                    binding?.progressBar3?.visibility = View.VISIBLE
                    //Utilities.showToast(requireContext(), getString(R.string.uploading_counts))
                } else {
                    binding?.progressBar3?.visibility = View.INVISIBLE
                }
                if (WorkInfo.State.SUCCEEDED == workInfo.state) {
                    Log.i(TAG, "progreso de subida de conteos finalizado con exito")
                    var msg = getString(R.string.counts_uploaded_successfully)
                    if(workInfo.outputData.hasKeyWithValueOfType("exception", String::class.java)){
                        msg = workInfo.outputData.getString("exception").toString()
                    } else if (workInfo.outputData.hasKeyWithValueOfType("error", String::class.java)) {
                        msg = workInfo.outputData.getString("error").toString()
                    }
                    //Utilities.showToast(requireContext(), msg)
                    //fetchCounts()
                }
                if (WorkInfo.State.FAILED == workInfo.state) {
                    Log.i(TAG, "progreso de subida de conteos finalizado con error")
                    var msg = getString(R.string.error_uploading_counts)
                    if(workInfo.outputData.hasKeyWithValueOfType("exception", String::class.java)){
                        msg = workInfo.outputData.getString("exception").toString()
                    } else if (workInfo.outputData.hasKeyWithValueOfType("error", String::class.java)) {
                        msg = workInfo.outputData.getString("error").toString()
                    }
                    //Utilities.showToast(requireContext(), msg)
                }
                if (WorkInfo.State.CANCELLED == workInfo.state) {
                    Log.i(TAG, "progreso de subida de conteos cancelado")
                    //Utilities.showToast(requireContext(), getString(R.string.counts_uploading_cancelled))
                }
            }
        })
        viewModel.filterCountsInput.observe(viewLifecycleOwner, {
            Log.i(TAG, "filter input received: $it")
            filterCounts(it)
        })
        viewModel.filterCountsInput.setValue(null)
        return binding?.root
    }

    private fun fetchCounts() {
        doAsync {
            val counts = db.itemCountDao().getAll()
            Log.i(TAG, "counts locales: ${counts.size}")
            uiThread {
                presentCounts(counts)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    private fun initAdapter(){
        mAdapter = CountExtendedAdapter(filteredData, requireContext())
        binding?.countsRv?.layoutManager = LinearLayoutManager(requireContext())
        binding?.countsRv?.adapter = mAdapter
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun filterCounts(query: String?) {
        Log.i(TAG, "filtering counts")
        taskFilterStr = if(query?.startsWith("task ") == true || query?.startsWith("tarea ") == true){
            query.split(" ").get(1)
        } else {
            null
        }
        filtered = query?.isNotEmpty() == true
        doAsync {
            val filteredCounts = db.itemCountDao().getAll().filter {
                (!taskFilterStr.isNullOrBlank() && taskFilterStr == it.taskId.toString()) ||
                (query.isNullOrBlank() || it.sku?.toLowerCase()?.contains(query) == true ||
                it.description?.toLowerCase()?.contains(query) == true)
            }
            uiThread {
                presentCounts(filteredCounts)
            }
        }
    }

    fun presentCounts(counts: List<ItemCount>){
        Log.i(TAG, "presentando conteos: $counts")
        binding?.titleTv?.text = getString(
            if(filtered){
                if(!taskFilterStr.isNullOrBlank()) {
                    R.string.task_filtred_counts_list_title
                } else {
                    R.string.filtred_counts_list_title
                }
            } else {R.string.counts_list_title},
            DateUtils.formatDateTime(context, System.currentTimeMillis(), 0),
            counts.size,
            taskFilterStr
        )
        binding?.errorTv?.text = counts.count {
            it.hasError == true
        }.toString()
        binding?.uploadedTv?.text = counts.count {
            it.uploaded && !it.dirty
        }.toString()
        binding?.pendingTv?.text = counts.count {
            !it.uploaded || (it.uploaded && it.dirty)
        }.toString()
        filteredData.clear()
        filteredData.addAll(counts)
        mAdapter?.notifyDataSetChanged()
    }

    companion object{
        private const val TAG = "COUNTS_FRAGMENT"
    }
}