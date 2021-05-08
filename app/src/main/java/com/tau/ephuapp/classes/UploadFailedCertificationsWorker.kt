package com.tau.ephuapp.classes

import android.content.Context
import android.database.sqlite.SQLiteAccessPermException
import android.database.sqlite.SQLiteCantOpenDatabaseException
import android.database.sqlite.SQLiteDatabaseLockedException
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.tau.ephuapp.database.AppDatabase
import com.tau.ephuapp.models.PendingToUploadCertification
import com.tau.ephuapp.services.MyClient
import com.tau.ephuapp.services.MyDataService

class UploadFailedCertificationsWorker
    (val appContext: Context,
    val workerParams: WorkerParameters
) : Worker(appContext, workerParams) {
    private val TAG = "UPLOAD_CERTIFICATIONS_WORKER"
    var db: AppDatabase? = null
    private var pendingCertifications: List<PendingToUploadCertification>? = null

    fun initAll(){
        try {
            db = AppDatabase.getDatabase(appContext)
        } catch(ex: SQLiteDatabaseLockedException) {
            Log.e(TAG, "Database error found", ex)
        } catch (ex: SQLiteAccessPermException) {
            Log.e(TAG, "Database error found", ex)
        } catch (ex: SQLiteCantOpenDatabaseException) {
            Log.e(TAG, "Database error found", ex)
        }
    }

    override fun doWork(): Result {
        initAll()
        pendingCertifications = db?.pendingToUploadCertificationDao()?.getAll()
        if (!pendingCertifications.isNullOrEmpty()) {
            val dataService: MyDataService? = MyClient.getInstance(appContext)?.create(
                MyDataService::class.java)
            if (dataService != null) {
                for(pendingCertification in pendingCertifications!!) {
                    //MyWorkerManagerService.enqueUploadSingleCertificationWork(appContext, pendingCertification)
                }
            } else {
                Log.e(TAG, "El cliente http o la autenticacion de usuario son invalidos. No se puede realizar el proceso")
            }
        }
        return Result.success()
    }
}