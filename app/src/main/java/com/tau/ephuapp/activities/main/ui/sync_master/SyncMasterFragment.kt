package com.tau.ephuapp.activities.main.ui.sync_master

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteAccessPermException
import android.database.sqlite.SQLiteCantOpenDatabaseException
import android.database.sqlite.SQLiteDatabaseLockedException
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.tau.ephuapp.R
import com.tau.ephuapp.activities.main.MainActivityViewModel
import com.tau.ephuapp.classes.Constants
import com.tau.ephuapp.classes.Utilities
import com.tau.ephuapp.database.AppDatabase
import com.tau.ephuapp.databinding.FragmentSyncMasterBinding
import com.tau.ephuapp.models.HistoryType
import com.tau.ephuapp.services.MyWorkerManagerService
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.joda.time.DateTime
import java.io.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


class SyncMasterFragment : Fragment() {
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
        binding.bdPathTv.text = ""
        binding.pendingToUploadCountsPathTv.text = ""
        viewModel.savingEditCountWorkProgress.observe(viewLifecycleOwner, {
            it.forEach { workInfo ->
                if (WorkInfo.State.ENQUEUED == workInfo.state) {
                    Log.i(TAG, "progreso de subida de conteos observado...trabajo encolado")
                    binding.progressBarSync.visibility = View.VISIBLE
                    //Utilities.showToast(requireContext(), getString(R.string.uploading_counts))
                } else {
                    binding.progressBarSync.visibility = View.INVISIBLE
                }
                if (WorkInfo.State.SUCCEEDED == workInfo.state) {
                    finishForceUploadPendingCounts()
                    Log.i(
                        TAG,
                        "progreso de subida de conteos observado...trabajo finalizado con exito"
                    )
                    var msg = getString(R.string.counts_uploaded_successfully)
                    if (workInfo.outputData.hasKeyWithValueOfType(
                            "exception",
                            String::class.java
                        )
                    ) {
                        msg = workInfo.outputData.getString("exception").toString()
                    } else if (workInfo.outputData.hasKeyWithValueOfType(
                            "error",
                            String::class.java
                        )
                    ) {
                        msg = workInfo.outputData.getString("error").toString()
                    }
                    //Utilities.showToast(requireContext(), msg)
                }
                if (WorkInfo.State.FAILED == workInfo.state) {
                    Log.i(
                        TAG,
                        "progreso de subida de conteos observado...trabajo finalizado con error"
                    )
                    finishForceUploadPendingCounts()
                    var msg = getString(R.string.error_uploading_counts)
                    if (workInfo.outputData.hasKeyWithValueOfType(
                            "exception",
                            String::class.java
                        )
                    ) {
                        msg = workInfo.outputData.getString("exception").toString()
                    } else if (workInfo.outputData.hasKeyWithValueOfType(
                            "error",
                            String::class.java
                        )
                    ) {
                        msg = workInfo.outputData.getString("error").toString()
                    }
                    //Utilities.showToast(requireContext(), msg)
                }
                if (WorkInfo.State.CANCELLED == workInfo.state) {
                    Log.i(TAG, "progreso de subida de conteos observado...trabajo cancelado")
                    //Utilities.showToast(requireContext(), getString(R.string.counts_uploading_cancelled))
                }
            }
        })
        viewModel.savingCountsWorkProgress.observe(viewLifecycleOwner, {
            it.forEach { workInfo ->
                if (WorkInfo.State.ENQUEUED == workInfo.state) {
                    Log.i(TAG, "progreso de subida de conteos observado...trabajo encolado")
                    binding.progressBarSync.visibility = View.VISIBLE
                    //Utilities.showToast(requireContext(), getString(R.string.uploading_counts))
                } else {
                    binding.progressBarSync.visibility = View.INVISIBLE
                }
                if (WorkInfo.State.SUCCEEDED == workInfo.state) {
                    finishForceUploadPendingCounts()
                    Log.i(
                        TAG,
                        "progreso de subida de conteos observado...trabajo finalizado con exito"
                    )
                    var msg = getString(R.string.counts_uploaded_successfully)
                    if (workInfo.outputData.hasKeyWithValueOfType(
                            "exception",
                            String::class.java
                        )
                    ) {
                        msg = workInfo.outputData.getString("exception").toString()
                    } else if (workInfo.outputData.hasKeyWithValueOfType(
                            "error",
                            String::class.java
                        )
                    ) {
                        msg = workInfo.outputData.getString("error").toString()
                    }
                    //Utilities.showToast(requireContext(), msg)
                }
                if (WorkInfo.State.FAILED == workInfo.state) {
                    Log.i(
                        TAG,
                        "progreso de subida de conteos observado...trabajo finalizado con error"
                    )
                    finishForceUploadPendingCounts()
                    var msg = getString(R.string.error_uploading_counts)
                    if (workInfo.outputData.hasKeyWithValueOfType(
                            "exception",
                            String::class.java
                        )
                    ) {
                        msg = workInfo.outputData.getString("exception").toString()
                    } else if (workInfo.outputData.hasKeyWithValueOfType(
                            "error",
                            String::class.java
                        )
                    ) {
                        msg = workInfo.outputData.getString("error").toString()
                    }
                    //Utilities.showToast(requireContext(), msg)
                }
                if (WorkInfo.State.CANCELLED == workInfo.state) {
                    Log.i(TAG, "progreso de subida de conteos observado...trabajo cancelado")
                    //Utilities.showToast(requireContext(), getString(R.string.counts_uploading_cancelled))
                }
            }
        })
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
            val datetime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val backupDBPath = "ephu_db_backup_$datetime.sqlite"
            createFile(backupDBPath, EXPORT_DATABASE)
        }
        binding.pendingToUploadCountsExportBtn.setOnClickListener{
            val datetime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val path = "ephu_counts_$datetime.txt"
            doAsync {
                val pendingCounts = db.itemCountDao().countAllNotUploadedByDevice(deviceId)
                val pendingCountsWithError = db.itemCountDao().countAllNotUploadedWithError(deviceId)
                if (pendingCounts > 0 || pendingCountsWithError > 0) {
                    createFile(path, EXPORT_COUNTS_JSON, "application/text")
                } else if (pendingCounts == 0 && pendingCountsWithError > 0) {
                    Utilities.showAlert(requireContext(),
                        getString(R.string.confirmation),
                        getString(
                            R.string.only_counts_w_err_msg
                        ),
                        {
                            createFile(path, EXPORT_COUNTS_JSON, "application/text")
                        })
                } else if (pendingCounts == 0 || pendingCountsWithError == 0) {
                    Utilities.showAlert(
                        requireContext(),
                        getString(R.string.error),
                        getString(R.string.no_data_to_export_error_msg)
                    )
                }
            }
        }
        binding.forcePushCountsBtn.setOnClickListener {
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

    private fun finishForceUploadPendingCounts() {
        isCheckingForPendingCounts = false
        binding.forcePushCountsBtn.isEnabled = true
        refreshUi()
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
        }
    }

    private fun syncTask(){
        doAsync {
            val pendingCounts = db.itemCountDao().countAllNotUploadedByDevice(deviceId)
            if (pendingCounts > 0) {//verificar si hay conteos pendientes (actualizaciones y creaciones)
                Utilities.showAlert(
                    requireContext(),
                    "Error",
                    "Hay conteos pendientes por subir. Por favor primero suba estos conteos y luego reintente la sincronizacion de tareas"
                )
                return@doAsync
            }
            uiThread {
                isSyncingTasks = true
                binding.syncTasksBtn.isEnabled = false
                binding.progressBarSync.visibility = View.VISIBLE
                viewModel.repository.fetchTasksList(requireContext(), true)
            }
        }
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
            val bdLastExported = activity?.defaultSharedPreferences?.getString(
                "bdLastExported",
                null
            ) ?: getString(R.string.never)
            val pendingToUplloadCountsLastExported = activity?.defaultSharedPreferences?.getString(
                "pendingToUploadCountsLastExported",
                null
            ) ?: getString(R.string.never)
            val lastPendingCountsRevision = db.fetchedHistoryDao().getByTag(HistoryType.LAST_PENDING_REVISION.toString())
            val pendingCounts = db.itemCountDao().countAllNotUploadedByDevice(deviceId)
            val pendingCountsWithError = db.itemCountDao().countAllNotUploadedWithError(deviceId)
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

                binding.pendingToUploadCountsLastExportTv.text = getString(
                    R.string.last_exported,
                    pendingToUplloadCountsLastExported
                )

                binding.pendingCountsTv.text = getString(
                    R.string.pending_counts,
                    pendingCounts,
                    pendingCountsWithError
                )
                binding.pendingCountsTv2.text = getString(
                    R.string.pending_counts,
                    pendingCounts,
                    pendingCountsWithError
                )
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

    fun exportDatabase(uri: Uri){
        try {
            requireContext().contentResolver.openFileDescriptor(uri, "w")?.use {
                FileOutputStream(it.fileDescriptor).use {
                    val currentDBPath = AppDatabase.getDatabase(requireContext()).openHelper.writableDatabase.path
                    val currentDB = File(currentDBPath)
                    val exportDBFn = {
                        try {
                            val src = FileInputStream(currentDB).channel
                            val dst = it.channel
                            dst.transferFrom(src, 0, src.size())
                            src.close()
                            dst.close()
                            activity?.defaultSharedPreferences?.edit {
                                putString("bdLastExported", DateTime.now().toLocalDateTime().toString())
                                commit()
                            }
                            activity?.runOnUiThread {
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.db_exported),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            binding.bdPathTv.text = uri.lastPathSegment
                            refreshUi()
                        } catch (e: IOException) {
                            e.printStackTrace()
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.error_exporting_db),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    if (currentDB.exists()) {
                        exportDBFn()
                    }
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            Toast.makeText(
                requireContext(),
                getString(R.string.error_exporting_db),
                Toast.LENGTH_LONG
            ).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(
                requireContext(),
                getString(R.string.error_exporting_db),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun exportPendingToUploadCounts(uri: Uri) {
        try {
            doAsync {
                requireContext().contentResolver.openFileDescriptor(uri, "w")?.use {
                    FileOutputStream(it.fileDescriptor).use { output ->
                        val gson = Gson()
                        val pendingToUploadCounts =
                            db.itemCountDao().getAllNotUploadedByDevice(deviceId)
                        val json = gson.toJson(pendingToUploadCounts)
                        output.write(json.toByteArray())
                        output.close()
                        activity?.defaultSharedPreferences?.edit {
                            putString("pendingToUploadCountsLastExported", DateTime.now().toLocalDateTime().toString())
                            commit()
                        }
                        uiThread {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.json_file_exported),
                                Toast.LENGTH_LONG
                            ).show()
                            binding.pendingToUploadCountsPathTv.text = uri.lastPathSegment
                            refreshUi()
                        }
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(
                requireContext(),
                getString(R.string.error_exporting_json_file),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun createFile(filename: String, code: Int, mimeType: String? = null, pickerInitialUri: Uri? = null) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType ?: "*/*"
            putExtra(Intent.EXTRA_TITLE, filename)
            // Optionally, specify a URI for the directory that should be opened in
            // the system file picker before your app creates the document.
            if (pickerInitialUri != null) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
            }
        }
        startActivityForResult(intent, code)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CREATE_FILE && resultCode == RESULT_OK && data?.data != null) {
            /*val jsonFile = File(data.data.toString())
            try {
                jsonFile.createNewFile()
                if (jsonFile.exists()) {
                    val gson = Gson()
                    val pendingCounts = db.itemCountDao().getAllNotUploadedByDevice(deviceId)
                    val json = gson.toJson(pendingCounts)
                    val output: OutputStream = FileOutputStream(jsonFile)
                    output.write(json.toByteArray())
                    output.close()
                    activity?.defaultSharedPreferences?.edit {
                        putString(
                            "pendingToUploadCountsLastExported",
                            DateTime.now().toLocalDateTime().toString()
                        )
                        commit()
                    }
                    activity?.runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.json_file_exported),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    refreshUi()
                    openDir(sd)
                } else {
                    Utilities.showAlert(
                        requireContext(), getString(R.string.error), getString(
                            R.string.json_file_error_msg
                        )
                    )
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_exporting_json_file),
                    Toast.LENGTH_LONG
                ).show()
            }*/
        }
        if (requestCode == EXPORT_DATABASE && resultCode == RESULT_OK && data?.data != null) {
            exportDatabase(data.data!!)
        }
        if (requestCode == EXPORT_COUNTS_JSON && resultCode == RESULT_OK && data?.data != null) {
            exportPendingToUploadCounts(data.data!!)
        }
    }

    companion object{
        private const val TAG = "SYNC_MASTER_FRAGMENT"
        const val CREATE_FILE = 1
        const val EXPORT_DATABASE = 2
        const val EXPORT_COUNTS_JSON = 3
    }
}