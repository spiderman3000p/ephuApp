package com.tau.ephuapp.activities.main

import android.content.Intent
import android.content.SharedPreferences
import android.database.sqlite.SQLiteAccessPermException
import android.database.sqlite.SQLiteCantOpenDatabaseException
import android.database.sqlite.SQLiteDatabaseLockedException
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.SearchView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.common.util.concurrent.ListenableFuture
import com.tau.ephuapp.R
import com.tau.ephuapp.activities.IntroActivity
import com.tau.ephuapp.classes.Constants
import com.tau.ephuapp.classes.Utilities
import com.tau.ephuapp.database.AppDatabase
import com.tau.ephuapp.databinding.ActivityMainBinding
import com.tau.ephuapp.models.Device
import com.tau.ephuapp.models.HistoryType
import com.tau.ephuapp.services.MySettings
import com.tau.ephuapp.services.MyWorkerManagerService
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutionException


class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val TAG = "MAIN_ACTIVITY"
    private lateinit var appBarConfiguration: AppBarConfiguration
    private val viewModel: MainActivityViewModel by viewModels()
    private var device: Device? = null
    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        try {
            db = AppDatabase.getDatabase(this)
        } catch (ex: SQLiteDatabaseLockedException) {
            Log.e(TAG, "Database error found", ex)
        } catch (ex: SQLiteAccessPermException) {
            Log.e(TAG, "Database error found", ex)
        } catch (ex: SQLiteCantOpenDatabaseException) {
            Log.e(TAG, "Database error found", ex)
        }
        MySettings.getInstance(this) // settings initialization
        val toolbar = binding.innerContentAppbar.toolbar
        setSupportActionBar(toolbar)
        val header = binding.navView.getHeaderView(0)
        val deviceIdTv: TextView = header.findViewById(R.id.deviceIdTv)
        deviceIdTv.text = Utilities.getAndroidId(this)
        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_inventory_tasks,
                R.id.nav_cert_tasks,
                R.id.nav_counts,
                R.id.nav_sync_master,
                R.id.nav_settings
            ), binding.drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            Log.i(TAG, "menu item selected ${destination.label}, id: ${destination.id}")
            invalidateOptionsMenu()
        }
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        if (!sharedPref.contains("firstRun")) {
            val intent = Intent(this, IntroActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        if (!sharedPref.contains("password")) {
            Log.i(TAG, "estableciendo password por defecto...")
            with(sharedPref.edit()) {
                putString("password", "12345")
                commit()
            }
        } else {
            Log.i(TAG, "ya existe una clave definida")
        }
        viewModel.repository.fetchOwnerData(this)
        viewModel.repository.getDevice().observe(this, Observer {
            Log.i(TAG, "dispositivo observado: $it")
            if (it != null) {
                device = it
                val usernameTv: TextView = header.findViewById(R.id.userNameTv)
                usernameTv.text = device?.ownerName
                if (device?.ownerId != null) {
                    viewModel.repository.fetchItems(this, device?.ownerId!!)
                } else {
                    Utilities.showAlert(
                        this,
                        getString(R.string.error),
                        getString(R.string.empty_owner_error_msg)
                    )
                }
            } else {
                Utilities.showAlert(
                    this,
                    getString(R.string.error),
                    getString(R.string.device_data_empty_error_msg)
                )
            }
        })
        viewModel.tasksList.observe(this, {
            Log.i(TAG, "tareas observadas en main activity: $it")
            updateSyncDataInHeader()
        })
        viewModel.repository.getItemsLoaded().observe(this, {
            Log.i(TAG, "items cargados: $it")
            updateSyncDataInHeader()
            doAsync {
                val tasksExist = db.fetchedHistoryDao().getByTag(HistoryType.TASKS.toString()) != null
                Log.i(TAG, "hay tareas cargadas de hoy?: $tasksExist")
                Log.i(TAG, "hay tareas cargadas en view model?: ${!viewModel.tasksList.value.isNullOrEmpty()}")
                if (!tasksExist || viewModel.tasksList.value.isNullOrEmpty()) {
                    viewModel.repository.fetchTasksList(this@MainActivity, tasksExist)
                }
            }
        })
        // provisional TODO: quitar esto de aqui al salir a produccion
        //WorkManager.getInstance().cancelAllWork()
        MyWorkerManagerService.uploadPendingCounts(this)
    }

    private fun isWorkScheduled(tag: String): Boolean {
        val instance = WorkManager.getInstance()
        val statuses: ListenableFuture<List<WorkInfo>> = instance.getWorkInfosByTag(tag)
        return try {
            var running = false
            val workInfoList: List<WorkInfo> = statuses.get()
            for (workInfo in workInfoList) {
                val state = workInfo.state
                running = state == WorkInfo.State.RUNNING || state == WorkInfo.State.ENQUEUED
            }
            running
        } catch (e: ExecutionException) {
            e.printStackTrace()
            false
        } catch (e: InterruptedException) {
            e.printStackTrace()
            false
        }
    }

    private fun updateSyncDataInHeader(){
        Log.i(TAG, "actualizando datos de sincronizacion en el header...")
        doAsync {
            val db: AppDatabase = AppDatabase.getDatabase(this@MainActivity)
            val itemCount = db.itemDao().countAll()
            val header = binding.navView.getHeaderView(0)
            val syncedDataLabelTv: TextView = header.findViewById(R.id.syncedDataLabelTv)
            val lastSyncLabelTv: TextView = header.findViewById(R.id.lastSyncLabelTv)
            val appVersionTv: TextView = header.findViewById(R.id.appVersionTv)
            val datetime = SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
            ).format(System.currentTimeMillis())
            val taskCount = viewModel.tasksList.value?.size ?: 0
            uiThread {
                appVersionTv.text = getString(R.string.app_version, Utilities.getVersionName())
                syncedDataLabelTv.text =
                    getString(R.string.synced_data_label, taskCount, itemCount)
                lastSyncLabelTv.text = getString(R.string.last_sync_date, datetime)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        //menuInflater.inflate(R.menu.main, menu)
        return true
    }

    private fun filterCounts(query: String?) {
        Log.i(TAG, "filtering planifications")

    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        if(navController.currentDestination?.label == getString(R.string.menu_counts)) {
            Log.i(TAG, "mostrando el menu de busqueda y filtrado")
            menuInflater.inflate(R.menu.search_filter, menu)
            val searchMenuItem = menu?.findItem(R.id.search)
            searchMenuItem?.isVisible = true
            val searchView = searchMenuItem?.actionView as androidx.appcompat.widget.SearchView
            //searchView.setQuery(searchStr, false)
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
                androidx.appcompat.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    // Toast like print
                    Log.i(TAG, "query submit $query")
                    /*if( ! searchView.isIconified()) {
                        searchView.setIconified(true)
                    }
                    searchMenuItem.collapseActionView()*/
                    return true
                }

                override fun onQueryTextChange(s: String): Boolean {
                    Log.i(TAG, "text change query $s")
                    viewModel.filterCountsInput.postValue(s.toLowerCase(Locale.getDefault()))
                    return true
                }
            })
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.i(TAG, "opcion seleccionada del menu: ${item.title}")
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        Log.i(TAG, "la configuracion ha cambiado...")
        val value = sharedPreferences?.getString(key, "")
        Log.i(TAG, "nuevo valor para $key: $value")
        MySettings.resetValues(this)
    }
}