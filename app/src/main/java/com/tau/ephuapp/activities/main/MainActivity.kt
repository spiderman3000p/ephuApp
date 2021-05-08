package com.tau.ephuapp.activities.main

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.TextView
import androidx.activity.viewModels
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.ui.setupWithNavController
import com.tau.ephuapp.R
import com.tau.ephuapp.activities.IntroActivity
import com.tau.ephuapp.activities.main.ui.tasks.TasksViewModel
import com.tau.ephuapp.classes.Utilities
import com.tau.ephuapp.database.AppDatabase
import com.tau.ephuapp.databinding.ActivityMainBinding
import com.tau.ephuapp.models.Device
import com.tau.ephuapp.services.MySettings
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val TAG = "MAIN_ACTIVITY"
    private lateinit var appBarConfiguration: AppBarConfiguration
    private val viewModel: MainActivityViewModel by viewModels()
    private val viewModelTasks: TasksViewModel by viewModels()
    private var device: Device? = null
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        MySettings.getInstance(this) // settings initialization
        val toolbar = binding.innerContentAppbar.toolbar
        setSupportActionBar(toolbar)
        val header = binding.navView.getHeaderView(0)
        val deviceIdTv: TextView = header.findViewById(R.id.deviceIdTv)
        deviceIdTv.text = Utilities.getAndroidId(this)
        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(setOf(
            R.id.nav_tasks, R.id.nav_cert_tasks, R.id.nav_sync_master
        ), binding.drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)
        val sharedPref = getSharedPreferences(packageName, Context.MODE_PRIVATE)
        if (!sharedPref.contains("firstRun")) {
            val intent = Intent(this, IntroActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        viewModel.repository.fetchOwnerData(this)
        viewModel.device.observe(this, Observer{
            if (it != null){
                device = it
                val usernameTv: TextView = header.findViewById(R.id.userNameTv)
                usernameTv.text = device?.ownerName
                if (device?.ownerId != null) {
                    viewModelTasks.repository.fetchItems(this, device?.ownerId!!)
                } else {
                    Utilities.showAlert(this, getString(R.string.error), getString(R.string.empty_owner_error_msg))
                }
            } else {
                Utilities.showAlert(this,getString(R.string.error),getString(R.string.device_data_empty_error_msg))
            }
        })
        viewModelTasks.tasksList.observe(this, {
            Log.i(TAG, "tareas observadas en main activity: $it")
            updateSyncDataInHeader()
        })
        viewModelTasks.repository.getItemsLoaded().observe(this, {
            Log.i(TAG, "items cargados: $it")
            updateSyncDataInHeader()
        })
    }

    private fun updateSyncDataInHeader(){
        Log.i(TAG, "actualizando datos de sincronizacion en el header...")
        doAsync {
            val db: AppDatabase = AppDatabase.getDatabase(this@MainActivity)
            val itemCount = db.itemDao().countAll()
            val header = binding.navView.getHeaderView(0)
            val syncedDataLabelTv: TextView = header.findViewById(R.id.syncedDataLabelTv)
            val lastSyncLabelTv: TextView = header.findViewById(R.id.lastSyncLabelTv)
            val datetime = SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
            ).format(System.currentTimeMillis())
            val taskCount = viewModelTasks.tasksList.value?.size ?: 0
            uiThread {
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