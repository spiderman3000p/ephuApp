package com.tau.ephuapp.activities

import android.annotation.SuppressLint
import android.database.sqlite.SQLiteAccessPermException
import android.database.sqlite.SQLiteCantOpenDatabaseException
import android.database.sqlite.SQLiteDatabaseLockedException
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.work.WorkInfo
import com.tau.ephuapp.R
import com.tau.ephuapp.database.AppDatabase
import com.tau.ephuapp.databinding.ActivityCertificateBinding
import com.tau.ephuapp.models.Certification
import com.tau.ephuapp.models.CertificationTaskItem
import com.tau.ephuapp.models.Task
import org.jetbrains.anko.doAsync

@SuppressLint("LongLogTag")
class CertificateActivity: AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private var task: Task? = null
    private var updatingListData: Boolean = false
    private lateinit var binding: ActivityCertificateBinding
    private lateinit var activityViewModel: CertificateActivityViewModel
    private lateinit var db: AppDatabase
    var totalToCertificate: Int = 0
    var totalCertified: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCertificateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val _Activity_viewModel: CertificateActivityViewModel by viewModels()
        activityViewModel = _Activity_viewModel
        try {
            db = AppDatabase.getDatabase(this)
        } catch (ex: SQLiteDatabaseLockedException) {
            Log.e(TAG, "Database error found", ex)
        } catch (ex: SQLiteAccessPermException) {
            Log.e(TAG, "Database error found", ex)
        } catch (ex: SQLiteCantOpenDatabaseException) {
            Log.e(TAG, "Database error found", ex)
        }
        binding.toolbar.setNavigationOnClickListener {
            Log.i(TAG, "setNavigationOnClickListener")
            finish()
        }
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        val navController = findNavController(R.id.nav_host_fragment2)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
                setOf(
                        R.id.navigation_scan,
                        R.id.navigation_scanned,
                        R.id.navigation_pending,
                        R.id.navigation_resume
                )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            Log.i(TAG, "menu item selected ${destination.label}, id: ${destination.id}")
            Log.i(TAG, "addOnDestinationChangedListener")
            invalidateOptionsMenu()
        }
        binding.navView4.setupWithNavController(navController)
        activityViewModel.certifiedItems.observe(this, { certifiedItems ->
            val pendingItems = arrayListOf<CertificationTaskItem>()
            activityViewModel.currentCertificationTaskItems.value?.forEach {
                val existentItem = pendingItems.find { it2 ->
                    it2.itemId == it.itemId
                }
                if (existentItem == null) {
                    it.totalQuantity = certifiedItems?.sumBy { it3 ->
                        if (it3.itemId == it.itemId && it3.quantity > 0) {
                            it3.quantity
                        } else {
                            0
                        }
                    } ?: 0
                    if (it.totalQuantity < it.totalUnits) {
                        pendingItems.add(it)
                    }
                }
            }
            activityViewModel.pendingCertificationTaskItems.setValue(pendingItems)
            updateProgressBar()
        })
        activityViewModel.savingCertificationWorkProgress.observe(this, {
            Log.i(TAG, "progreso de subida de certificacion observado...")
            it.forEach { workInfo ->
                if (WorkInfo.State.RUNNING == workInfo.state) {
                    binding.progressBar.isIndeterminate = true
                    //Utilities.showToast(requireContext(), getString(R.string.uploading_counts))
                } else {
                    binding.progressBar.isIndeterminate = false
                    updateProgressBar()
                }
                if (WorkInfo.State.SUCCEEDED == workInfo.state) {
                    var msg = getString(R.string.counts_uploaded_successfully)
                    if (workInfo.outputData.hasKeyWithValueOfType("exception", String::class.java)) {
                        msg = workInfo.outputData.getString("exception").toString()
                    } else if (workInfo.outputData.hasKeyWithValueOfType("error", String::class.java)) {
                        msg = workInfo.outputData.getString("error").toString()
                    } else {
                        Log.i(TAG, "WORK exitoso!")
                        // si todo salio bien, actualizar id's remotos
                        if (!updatingListData) {
                            updateListData()
                        }
                    }
                    //Utilities.showToast(requireContext(), msg)
                }
                if (WorkInfo.State.FAILED == workInfo.state) {
                    Log.e(TAG, "Work finalizado con error")
                    var msg = getString(R.string.error_uploading_certifications)
                    if (workInfo.outputData.hasKeyWithValueOfType("exception", String::class.java)) {
                        msg = workInfo.outputData.getString("exception").toString()
                    } else if (workInfo.outputData.hasKeyWithValueOfType("error", String::class.java)) {
                        msg = workInfo.outputData.getString("error").toString()
                    }
                    Log.e(TAG, "Error: $msg")
                }
                if (WorkInfo.State.CANCELLED == workInfo.state) {
                    Log.e(TAG, "Work cancelado")
                    //Utilities.showToast(requireContext(), getString(R.string.counts_uploading_cancelled))
                }
            }
        })
        intent.extras?.also{
            if (it.containsKey("task")) {
                task = it.getSerializable("task") as Task
                initTaskData()
                Log.i(TAG, "tarea recibida en extras ${task?.id}")
            } else {
                Log.i(TAG, "no se recibio ninguna planificacion. enviando a planificaciones")
                finish()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        Log.i(TAG, "onSupportNavigateUp")
        onBackPressed()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.i(TAG, "onOptionsItemSelected")
        /*if (item.itemId == android.R.id.home) {
            finish()
        }*/
        return super.onOptionsItemSelected(item)
    }

    fun updateProgressBar(){
        runOnUiThread {
            totalCertified = (activityViewModel.certifiedItems.value?.sumBy {
                it.quantity
            } ?: 0)
            Log.i(TAG, "total certified $totalCertified")
            totalToCertificate = (activityViewModel.currentCertificationTaskItems.value?.sumBy {
                it.totalUnits
            } ?: 0)
            Log.i(TAG, "certification progress $totalToCertificate")
            if (totalCertified > 0 && totalToCertificate > 0) {
                val percentage = (totalCertified * 100) / totalToCertificate
                Log.i(TAG, "certification progress $percentage")
                binding.progressBar.progress = percentage
            }
        }
    }

    fun initTaskData(){
        if (task != null) {
            activityViewModel.task.setValue(task)
            Log.i(TAG, "Cargando certification items de la tarea...")
            updateListData()
        }
    }

    fun updateListData(){
        doAsync {
            updatingListData = true
            val items = db.certificationTaskItemsDao().getAllByTask(task?.id!!)
            activityViewModel.currentCertificationTaskItems.postValue(ArrayList(items))
            // Obtener certificaciones hechas
            val certifiedItems: List<Certification> = db.certificationsDao().getAllByTask(task?.id!!)
            activityViewModel.certifiedItems.postValue(ArrayList(certifiedItems))
            updatingListData = false
        }
    }

    companion object {
        private const val TAG = "TASK_CERTIFICATE_ACTIVITY"
    }
}