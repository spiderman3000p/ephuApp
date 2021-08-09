package com.tau.ephuapp.classes

import android.content.Context
import android.database.sqlite.SQLiteAccessPermException
import android.database.sqlite.SQLiteCantOpenDatabaseException
import android.database.sqlite.SQLiteDatabaseLockedException
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.hasKeyWithValueOfType
import com.google.gson.Gson
import com.tau.ephuapp.database.AppDatabase
import com.tau.ephuapp.models.ItemCount
import com.tau.ephuapp.models.TaskState
import com.tau.ephuapp.services.MyDataService
import com.tau.ephuapp.services.MyClient
import java.io.IOException
import java.net.SocketTimeoutException

class ChangeLocationIsEmptyWorker
    (val appContext: Context,
    val workerParams: WorkerParameters
) : Worker(appContext, workerParams) {
    private val TAG = "CHANGE_LOCATION_IS_EMPTY_WORKER"
    private val MAX_REINTENT = -1
    private var failedRequestsCounter = 0
    var db: AppDatabase? = null

    override fun doWork(): Result {
        try {
            db = AppDatabase.getDatabase(appContext)
        } catch(ex: SQLiteDatabaseLockedException) {
            Log.e(TAG, "Database error found", ex)
        } catch (ex: SQLiteAccessPermException) {
            Log.e(TAG, "Database error found", ex)
        } catch (ex: SQLiteCantOpenDatabaseException) {
            Log.e(TAG, "Database error found", ex)
        }
        Log.i(TAG, "input data: $inputData")
        if (inputData.hasKeyWithValueOfType<Int>("taskId") &&
            inputData.hasKeyWithValueOfType<Int>("locationId") &&
            inputData.hasKeyWithValueOfType<Boolean>("isEmpty") &&
            inputData.hasKeyWithValueOfType<String>("countsJSON")) {
            val taskId = inputData.getInt("taskId", 0)
            val locationId = inputData.getInt("locationId", 0)
            val countsJSON = inputData.getString("countsJSON")
            val counts = Gson().fromJson(countsJSON, Array<ItemCount>::class.java).toList()
            val isEmpty = inputData.getBoolean("isEmpty", false)
            Log.i(TAG, "tarea de cambio de isEmpty a $isEmpty de la ubicacion$locationId de la tarea $taskId recibida con exito")
            Log.i(TAG, "countsJSON: $countsJSON")
            val dataService: MyDataService = MyClient.getInstance(appContext).create(MyDataService::class.java)
            try {
                val call = dataService.saveCounts(counts).execute()
                if (call.code() == 500 || call.code() == 400 || call.code() == 404 || call.code() == 403 || call.code() == 401) {
                    Log.e(TAG, "change isEmpty error response: ${call.errorBody()}")
                    return if (MAX_REINTENT ==-1 || failedRequestsCounter < MAX_REINTENT) {
                        Log.i(TAG, "reintendando cambio de isEmpty a la ubicacion $locationId de la tarea $taskId a $isEmpty...")
                        failedRequestsCounter++
                        Result.retry()
                    } else {
                        Log.i(TAG, "el isEmpty de la ubicacion $locationId de la tarea $taskId no pudo ser cambiado a $isEmpty")
                        Result.failure()
                    }
                } else if (call.code() == 200 || call.code() == 201 || call.code() == 202) {
                    return Result.success()
                }
            } catch(toe: SocketTimeoutException) {
                Log.e(TAG, "Network error when changing isEmpty de la ubicacion $locationId de la tarea $taskId to $isEmpty", toe)
                return if (MAX_REINTENT ==-1 || failedRequestsCounter < MAX_REINTENT) {
                    Log.i(TAG, "reintendando cambio de isEmpty a la ubicacion $locationId de la tarea $taskId a $isEmpty...")
                    failedRequestsCounter++
                    Result.retry()
                } else {
                    Log.e(TAG, "fallo al cambiar el isEmpty de la ubicacion $locationId de la tarea $taskId a $isEmpty!")
                    Result.failure()
                }
            } catch (ioEx: IOException) {
                Log.e(TAG, "Network error when changing isEmpty de la ubicacion $locationId de la tarea $taskId to $isEmpty", ioEx)
                return if (MAX_REINTENT ==-1 || failedRequestsCounter < MAX_REINTENT) {
                    Log.i(TAG, "reintendando cambio de isEmpty a la ubicacion $locationId de la tarea $taskId a $isEmpty...")
                    failedRequestsCounter++
                    Result.retry()
                } else {
                    Log.e(TAG, "fallo al cambiar el isEmpty de la ubicacion $locationId de la tarea $taskId a $isEmpty!")
                    Result.failure()
                }
            }
        }
        Log.e(TAG, "No se recibieron los datos necesarios")
        return Result.failure()
    }
}