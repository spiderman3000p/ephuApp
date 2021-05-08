package com.tau.ephuapp.classes

import android.content.Context
import android.database.sqlite.SQLiteAccessPermException
import android.database.sqlite.SQLiteCantOpenDatabaseException
import android.database.sqlite.SQLiteDatabaseLockedException
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.tau.ephuapp.database.AppDatabase
import com.tau.ephuapp.models.ItemCount
import com.tau.ephuapp.services.MyDataService
import com.tau.ephuapp.services.MyClient
import java.io.IOException
import java.net.SocketTimeoutException

class UploadSingleCountWorker
    (val appContext: Context,
    val workerParams: WorkerParameters
) : Worker(appContext, workerParams) {
    private val TAG = "UPLOAD_SINGLE_COUNT_WORKER"
    private val MAX_REINTENT = 3
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
        if (inputData.hasKeyWithValueOfType("countJSON", String::class.java)) {
            Log.i(TAG, "conteo recibido con exito")
            val pendingToUploadCount = Gson().fromJson(inputData.getString("countJSON"), ItemCount::class.java)
            Log.i(TAG, "conteo: $pendingToUploadCount")
            val dataService: MyDataService = MyClient.getInstance(appContext).create(MyDataService::class.java)
            Log.i(TAG, "guardando conteo $pendingToUploadCount")
            try {
                val call = dataService.saveCount(pendingToUploadCount).execute()
                if (call.code() == 500 || call.code() == 400 || call.code() == 404 || call.code() == 403 || call.code() == 401) {
                    Log.e(TAG, "upload count error response: ${call.errorBody()}")
                    return if (failedRequestsCounter < MAX_REINTENT) {
                        Log.i(TAG, "reintendando envio de conteo...")
                        failedRequestsCounter++
                        Result.retry()
                    } else {
                        Log.i(TAG, "guardando count en la tabla de requests fallidas $pendingToUploadCount")
                        Log.i(TAG, "count no pudo ser guardado en la BD remota")
                        Result.failure()
                    }
                } else if (call.code() == 200 || call.code() == 201 || call.code() == 202) {
                    Log.i(TAG, "upload count response: ${call.body()}")
                    pendingToUploadCount.uploaded = true
                    pendingToUploadCount.sent = false
                    pendingToUploadCount.dirty = false
                    db?.itemCountDao()?.insert(pendingToUploadCount)
                    return Result.success()
                }
            } catch(toe: SocketTimeoutException) {
                Log.e(TAG, "Network error when uploading count $pendingToUploadCount", toe)
                return if (failedRequestsCounter < MAX_REINTENT) {
                    Log.i(TAG, "reintentando enviar conteo...")
                    failedRequestsCounter++
                    Result.retry()
                } else {
                    Log.e(TAG, "fallo el envio de conteo!")
                    Result.failure()
                }
            } catch (ioEx: IOException) {
                Log.e(TAG,
                    "Network error when uploading count $pendingToUploadCount",
                    ioEx)
                return if (failedRequestsCounter < MAX_REINTENT) {
                    Log.i(TAG, "reintentando enviar conteo...")
                    failedRequestsCounter++
                    Result.retry()
                } else {
                    Log.e(TAG, "fallo el envio de conteo!")
                    Result.failure()
                }
            }
        }
        Log.e(TAG, "No se recibio el conteo")
        return Result.failure()
    }
}