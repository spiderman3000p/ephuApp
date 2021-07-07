package com.tau.ephuapp.classes

import android.content.Context
import android.database.sqlite.SQLiteAccessPermException
import android.database.sqlite.SQLiteCantOpenDatabaseException
import android.database.sqlite.SQLiteDatabaseLockedException
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.tau.ephuapp.database.AppDatabase
import com.tau.ephuapp.models.ItemCount
import com.tau.ephuapp.services.MyClient
import com.tau.ephuapp.services.MyDataService
import com.tau.ephuapp.services.MyWorkerManagerService
import org.jetbrains.anko.doAsync

class UploadFailedCountsWorker
    (val appContext: Context,
    val workerParams: WorkerParameters
) : Worker(appContext, workerParams) {
    private val TAG = "UPLOAD_FAILED_COUNTS_WORKER"
    var db: AppDatabase? = null
    private var pendingToUploadCountsAndRecounts: List<ItemCount>? = null
    private var pendingToUpdateCountsAndRecounts: List<ItemCount>? = null
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
        pendingToUploadCountsAndRecounts = db?.itemCountDao()?.getAllPendingCountsAndRecountsToUpload()
        pendingToUpdateCountsAndRecounts = db?.itemCountDao()?.getAllPendingCountsAndRecountsToUpdate()
        if (!pendingToUploadCountsAndRecounts.isNullOrEmpty()) {
            val tasksIds = pendingToUploadCountsAndRecounts?.map{
                it.taskId
            }
            for(taskId in tasksIds!!) {
                val countsToSave = pendingToUploadCountsAndRecounts?.filter {
                    it.taskId == taskId
                }
                MyWorkerManagerService.enqueCountToUploadArrayWork(appContext, countsToSave!!, taskId!!, Constants.SAVING_COUNTS_PROGRESS)
            }
        }

        if(!pendingToUpdateCountsAndRecounts.isNullOrEmpty()){
            for(itemCount in pendingToUpdateCountsAndRecounts!!) {
                MyWorkerManagerService.enqueEditCountToUploadWork(appContext, itemCount, Constants.SAVING_EDIT_COUNT_PROGRESS)
                Thread.sleep(2000)
            }
        }
        return Result.success()
    }
}