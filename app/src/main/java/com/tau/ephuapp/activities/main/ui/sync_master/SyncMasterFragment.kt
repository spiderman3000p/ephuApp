package com.tau.ephuapp.activities.main.ui.sync_master

import android.database.sqlite.SQLiteAccessPermException
import android.database.sqlite.SQLiteCantOpenDatabaseException
import android.database.sqlite.SQLiteDatabaseLockedException
import android.os.Bundle
import android.os.Environment
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.tau.ephuapp.R
import com.tau.ephuapp.activities.main.ui.tasks.TasksViewModel
import com.tau.ephuapp.classes.Utilities
import com.tau.ephuapp.database.AppDatabase
import com.tau.ephuapp.databinding.FragmentSyncBinding
import com.tau.ephuapp.databinding.FragmentSyncMasterBinding
import com.tau.ephuapp.models.HistoryType
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.joda.time.DateTime
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.DateFormat

class SyncMasterFragment : Fragment() {
    private val TAG = "SYNC_MASTER_FRAGMENT"
    private lateinit var viewModel: TasksViewModel
    private var _binding: FragmentSyncBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase
    private var isSyncingItems = false
    private var isSyncingTasks = false
    private var isSyncingAll = false
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSyncBinding.inflate(inflater, container, false)
        val _viewModel: TasksViewModel by activityViewModels()
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        refreshUi()
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
        binding.bdExportBtn.setOnClickListener{
            exportDB()
        }
        binding.syncAllBtn.setOnClickListener {
            isSyncingAll = true
            binding.syncAllBtn.isEnabled = false
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

    private fun syncTask(){
        isSyncingTasks = true
        binding.syncTasksBtn.isEnabled = false
        binding.progressBarSync.visibility = View.VISIBLE
        viewModel.repository.fetchTasksList(requireContext(), true)
    }

    private fun refreshUi(){
        doAsync {
            val totalItems = db.itemDao().countAll()
            val itemsLastSync = db.fetchedHistoryDao().getByTag(HistoryType.ITEMS.toString())
            val totalTasks = db.tasksDao().countAllByDevice(Utilities.getAndroidId(requireContext()))
            val tasksLastSync = db.fetchedHistoryDao().getByTag(HistoryType.TASKS.toString())
            val bdLastExported = activity?.defaultSharedPreferences?.getString("bdLastExported", null) ?: getString(R.string.never)
            uiThread {
                if(isSyncingAll && (!isSyncingItems && !isSyncingTasks)){
                    isSyncingAll = false
                    binding.syncAllBtn.isEnabled = true
                }
                if(!isSyncingAll && !isSyncingTasks && !isSyncingItems) {
                    binding.progressBarSync.visibility = View.INVISIBLE
                }
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
            }
        }
    }

    private fun exportDB(){
        var path: String? = null
        try {
            path =
                if (File("/data/data/com.tau.ephuapp/databases/ephuapp_database.db3").exists()) {
                    "/data/data/com.tau.ephuapp/databases/ephuapp_database.db3"
                } else if (File(
                        activity?.getDatabasePath("ephuapp_database")?.getAbsolutePath() ?: ""
                    ).exists()
                ) {
                    activity?.getDatabasePath("ephuapp_database")?.getAbsolutePath()
                } else {
                    null
                }
            if (path == null) {
                Log.e(TAG, "No se encontro un path valido para la BD de la app")
                Toast.makeText(
                    requireContext(),
                    "No se encontro la ubicacion de la BD",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception){
            e.printStackTrace()
            Log.e(TAG, "Error al obtener el path de la BD de la app")
            Toast.makeText(
                requireContext(),
                "Error al intentar obtener la ubicacion de la BD de la app",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val exportPath = Environment.getExternalStorageDirectory()
        val file = File(exportPath, "ephuapp_database_dump.db")
//        file.createNewFile()
        Log.i(TAG, "Se copiara la BD desde: $path")
        Log.i(TAG, "Se creara la copia de la BD en: $exportPath")
        val f = File(path!!)
        var fis: FileInputStream? = null
        var fos: FileOutputStream? = null
        try {
            fis = FileInputStream(f)
            fos = FileOutputStream(file)
            while (true) {
                val i: Int = fis.read()
                if (i != -1) {
                    fos.write(i)
                } else {
                    break
                }
            }
            fos.flush()
            activity?.defaultSharedPreferences?.edit {
                putString("bdLastExported", DateTime.now().toLocalDateTime().toString())
            }
            Toast.makeText(requireContext(), "DB dump OK", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "DB dump ERROR", Toast.LENGTH_LONG).show()
        } finally {
            try {
                fos?.close()
                fis?.close()
            } catch (ioe: IOException) {
                ioe.printStackTrace()
                Toast.makeText(requireContext(), "ERROR IO closing file stream", Toast.LENGTH_LONG).show()
            }
        }
    }
}