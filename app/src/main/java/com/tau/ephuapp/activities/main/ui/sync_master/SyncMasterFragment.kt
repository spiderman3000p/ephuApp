package com.tau.ephuapp.activities.main.ui.sync_master

import android.database.sqlite.SQLiteAccessPermException
import android.database.sqlite.SQLiteCantOpenDatabaseException
import android.database.sqlite.SQLiteDatabaseLockedException
import android.os.Bundle
import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.work.WorkManager
import com.kbj.androxlsxparser.mxlsxparser.StreamingReader
import com.tau.ephuapp.R
import com.tau.ephuapp.activities.main.MainActivityViewModel
import com.tau.ephuapp.classes.Constants
import com.tau.ephuapp.classes.Utilities
import com.tau.ephuapp.database.AppDatabase
import com.tau.ephuapp.databinding.FragmentSyncMasterBinding
import com.tau.ephuapp.models.HistoryType
import com.tau.ephuapp.services.MyWorkerManagerService
import org.apache.poi.ss.usermodel.Workbook
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.joda.time.DateTime
import java.io.*
import java.text.DateFormat

class SyncMasterFragment : Fragment() {
    private val TAG = "SYNC_MASTER_FRAGMENT"
    private lateinit var viewModel: MainActivityViewModel
    private var _binding: FragmentSyncMasterBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase
    private var isSyncingDevice = false
    private var isSyncingItems = false
    private var isSyncingTasks = false
    private var isCheckingForPendingCounts = false
    private var isSyncingAll = false
    private var deviceId: String = ""
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSyncMasterBinding.inflate(inflater, container, false)
        val _viewModel: MainActivityViewModel by activityViewModels()
        viewModel = _viewModel
        deviceId = Utilities.getAndroidId(requireContext())
        try {
            db = AppDatabase.getDatabase(requireContext())
        } catch (ex: SQLiteDatabaseLockedException) {
            Log.e(TAG, "Database error found", ex)
        } catch (ex: SQLiteAccessPermException) {
            Log.e(TAG, "Database error found", ex)
        } catch (ex: SQLiteCantOpenDatabaseException) {
            Log.e(TAG, "Database error found", ex)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.deviceAndroidIdTv.text = deviceId
        refreshUi()
        viewModel.device.observe(viewLifecycleOwner, {
            isSyncingDevice = false
            binding.syncDeviceBtn.isEnabled = true
            binding.deviceOwnerTv.text = it?.ownerName ?: getString(R.string.unknown)
            refreshUi()
        })
        viewModel.tasksList.observe(viewLifecycleOwner, { _ ->
            isSyncingTasks = false
            binding.syncTasksBtn.isEnabled = true
            refreshUi()
        })
        viewModel.repository.getItemsLoaded().observe(viewLifecycleOwner, {
            if (it != false) {
                isSyncingItems = false
                binding.syncItemsBtn.isEnabled = true
            }
            refreshUi()
        })
        binding.syncTasksBtn.setOnClickListener {
            syncTask()
        }
        binding.syncItemsBtn.setOnClickListener {
            syncItems()
        }
        binding.syncDeviceBtn.setOnClickListener {
            syncDevice()
        }
        binding.bdExportBtn.setOnClickListener{
            exportDatabase()
        }
        binding.pushPendingCountsTv.setOnClickListener {
            checkForPendingCounts()
        }
        binding.syncAllBtn.setOnClickListener {
            isSyncingAll = true
            binding.syncAllBtn.isEnabled = false
            syncDevice()
            syncItems()
            syncTask()
        }
    }

    private fun syncItems(){
        binding.progressBarSync.visibility = View.VISIBLE
        isSyncingItems = true
        binding.syncItemsBtn.isEnabled = false
        doAsync {
            val androidId = Utilities.getAndroidId(requireContext())
            db.deviceDao().getByDevice(androidId).let { device ->
                if (device != null) {
                    viewModel.repository.fetchItems(requireContext(), device.ownerId!!, true)
                } else {
                    Utilities.showAlert(
                        requireContext(),
                        getString(R.string.error),
                        getString(R.string.device_data_empty_error_msg)
                    )
                }
            }
        }
    }

    private fun checkForPendingCounts(){
        isCheckingForPendingCounts = true
        binding.forcePushCountsBtn.isEnabled = false
        //binding.progressBarSync.visibility = View.VISIBLE
        doAsync {
            WorkManager.getInstance().cancelAllWorkByTag(Constants.SAVING_COUNTS_PROGRESS)
            Thread.sleep(2000)
            MyWorkerManagerService.uploadPendingCounts(requireContext())
            Utilities.showToast(requireContext(), getString(R.string.checking_for_pending_counts))
        }
    }

    private fun syncTask(){
        isSyncingTasks = true
        binding.syncTasksBtn.isEnabled = false
        binding.progressBarSync.visibility = View.VISIBLE
        viewModel.repository.fetchTasksList(requireContext(), true)
    }

    private fun syncDevice(){
        isSyncingDevice = true
        binding.syncDeviceBtn.isEnabled = false
        binding.progressBarSync.visibility = View.VISIBLE
        viewModel.repository.fetchOwnerData(requireContext(), true)
    }

    private fun refreshUi(){
        doAsync {
            val device = db.deviceDao().getByDevice(deviceId)
            val deviceLastSync = db.fetchedHistoryDao().getByTag(HistoryType.DEVICES.toString())
            val totalItems = db.itemDao().countAll()
            val itemsLastSync = db.fetchedHistoryDao().getByTag(HistoryType.ITEMS.toString())
            val totalTasks = db.tasksDao().countAllByDevice(Utilities.getAndroidId(requireContext()))
            val tasksLastSync = db.fetchedHistoryDao().getByTag(HistoryType.TASKS.toString())
            val bdLastExported = activity?.defaultSharedPreferences?.getString("bdLastExported", null) ?: getString(R.string.never)
            val lastPendingCountsRevision = db.fetchedHistoryDao().getByTag(HistoryType.LAST_PENDING_REVISION.toString())
            val pendingCounts = db.itemCountDao().countAllPendingToUploadByDevice(deviceId)
            uiThread {
                binding.forcePushCountsBtn.isEnabled = pendingCounts > 0
                if(isSyncingAll && (!isSyncingItems && !isSyncingTasks && !isSyncingDevice)){
                    isSyncingAll = false
                    binding.syncAllBtn.isEnabled = true
                }
                if(!isSyncingAll && !isSyncingTasks && !isSyncingItems && !isSyncingDevice) {
                    binding.progressBarSync.visibility = View.INVISIBLE
                }
                binding.deviceOwnerTv.text = device?.ownerName ?: getString(R.string.unknown)
                binding.deviceLastSyncTv.text = getString(
                    R.string.last_synced_date, DateUtils.formatSameDayTime(
                        deviceLastSync?.lastUpdate ?: 0,
                        System.currentTimeMillis(),
                        DateFormat.SHORT,
                        DateFormat.SHORT
                    )
                )

                binding.totalItemsTv.text = getString(R.string.last_synced_items, totalItems)
                binding.itemLastSyncTv.text = getString(
                    R.string.last_synced_date, DateUtils.formatSameDayTime(
                        itemsLastSync?.lastUpdate ?: 0,
                        System.currentTimeMillis(),
                        DateFormat.SHORT,
                        DateFormat.SHORT
                    )
                )

                binding.totalTasksTv.text = getString(R.string.last_synced_tasks, totalTasks)
                binding.tasksLastSyncTv.text = getString(
                    R.string.last_synced_date, DateUtils.formatSameDayTime(
                        tasksLastSync?.lastUpdate ?: 0,
                        System.currentTimeMillis(),
                        DateFormat.SHORT,
                        DateFormat.SHORT
                    )
                )

                binding.bdLastExportTv.text = getString(R.string.last_exported, bdLastExported)
                binding.bdPathTv.text = activity?.getDatabasePath("ephuapp_database")?.getAbsolutePath() ?: getString(R.string.unknown)

                binding.pendingCountsTv.text = getString(R.string.pending_counts, pendingCounts)
                binding.lastPendingCountsRevisionTv.text = getString(
                    R.string.last_pending_counts_revision, DateUtils.formatSameDayTime(
                        lastPendingCountsRevision?.lastUpdate ?: 0,
                        System.currentTimeMillis(),
                        DateFormat.SHORT,
                        DateFormat.SHORT
                    )
                )
            }
        }
    }

    fun exportDatabase(){
        val sd = requireContext().getExternalFilesDir(DIRECTORY_DOWNLOADS)
        if (sd?.canWrite() == true) {
            val currentDBPath = AppDatabase.getDatabase(requireContext()).openHelper.writableDatabase.path
            val backupDBPath = "ephu_database_backup.sqlite"//you can modify the file type you need to export
            val currentDB = File(currentDBPath)
            val backupDB = File(sd, backupDBPath)
            if (currentDB.exists()) {
                try {
                    val src = FileInputStream(currentDB).channel
                    val dst = FileOutputStream(backupDB).channel
                    dst.transferFrom(src, 0, src.size())
                    src.close()
                    dst.close()
                    activity?.defaultSharedPreferences?.edit {
                        putString("bdLastExported", DateTime.now().toLocalDateTime().toString())
                        commit()
                    }
                    refreshUi()
                    Toast.makeText(requireContext(), getString(R.string.db_exported), Toast.LENGTH_LONG).show()
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), getString(R.string.error_exporting_db), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun runTest(){
        doAsync {
            val sd = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val inputStream: InputStream = FileInputStream(File(sd, "test.xlsx"));
            val workbook: Workbook = StreamingReader.builder()
                .rowCacheSize(100) // number of rows to keep in memory (defaults to 10)
                .bufferSize(4096) // buffer size to use when reading InputStream to file (defaults to 1024)
                .open(requireContext(), inputStream) // InputStream or File for XLSX file (required)

            for (i in 0..workbook.numberOfSheets) {
                val sheet = workbook.getSheetAt(i)
                uiThread {
                    Log.i(TAG, "Hoja $i:${sheet.sheetName}")
                }
                for (r in sheet) {
                    uiThread {
                        Log.i(TAG, "Fila ${r.rowNum}")
                    }
                    for (c in r) {
                        uiThread {
                            Log.i(TAG, "Celda ${c.columnIndex}:${c.stringCellValue}")
                        }
                    }
                }
            }
        }
    }
}