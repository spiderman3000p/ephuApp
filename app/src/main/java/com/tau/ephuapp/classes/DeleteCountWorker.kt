package com.tau.ephuapp.classes

import android.content.Context
import android.database.sqlite.SQLiteAccessPermException
import android.database.sqlite.SQLiteCantOpenDatabaseException
import android.database.sqlite.SQLiteDatabaseLockedException
import android.util.Log
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.gson.Gson
import com.tau.ephuapp.R
import com.tau.ephuapp.database.AppDatabase
import com.tau.ephuapp.models.ItemCount
import com.tau.ephuapp.services.MyDataService
import com.tau.ephuapp.services.MyClient
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.*

class DeleteCountWorker
    (val appContext: Context,
    val workerParams: WorkerParameters
) : Worker(appContext, workerParams) {
    private var failedRequestsCounter = 0

    override fun doWork(): Result {
        if (inputData.hasKeyWithValueOfType("countJSON", String::class.java)) {
            Log.i(TAG, "conteo recibido con exito")
            val pendingToDeleteCount = Gson().fromJson(inputData.getString("countJSON"), ItemCount::class.java)
            Log.i(TAG, "conteo: $pendingToDeleteCount")
            if(!pendingToDeleteCount.uploaded){
                return Result.failure(workDataOf(
                    "error" to appContext.getString(R.string.error_deleting_not_uploaded_count)
                ))
            }
            val dataService: MyDataService = MyClient.getInstance(appContext).create(MyDataService::class.java)
            Log.i(TAG, "solicitando eliminacion del conteo $pendingToDeleteCount")
            try {
                val url = "inventories/${pendingToDeleteCount.taskId}/countDetail/${pendingToDeleteCount.id}"
                val call = dataService.deleteCount(url).execute()
                if (call.code() == 500 || call.code() == 400 || call.code() == 404 || call.code() == 403 || call.code() == 401) {
                    Log.e(TAG, "delete count error response: ${call.errorBody()}")
                    return if (MAX_REINTENT ==-1 || failedRequestsCounter < MAX_REINTENT) {
                        Log.i(TAG, "reintendando eliminacion de conteo...")
                        failedRequestsCounter++
                        Result.retry()
                    } else {
                        Log.i(TAG, "eliminacion de conteo no pudo ser realizado en la BD remota")
                        Result.failure(workDataOf(
                                "error" to appContext.getString(R.string.error_deleting_count)
                        ))
                    }
                } else if (call.code() == 200 || call.code() == 201 || call.code() == 202) {
                    Log.i(TAG, "respuesta exitosa al eliminar conteo")
                    return Result.success()
                }
            } catch(toe: SocketTimeoutException) {
                Log.e(TAG, "Network error when deleting count $pendingToDeleteCount", toe)
                return if (MAX_REINTENT ==-1 || failedRequestsCounter < MAX_REINTENT) {
                    Log.i(TAG, "reintentando eliminacion de conteo...")
                    failedRequestsCounter++
                    Result.retry()
                } else {
                    Log.e(TAG, "fallo la eliminacion de conteo!")
                    Result.failure(workDataOf(
                            "error" to appContext.getString(R.string.error_deleting_count)
                    ))
                }
            } catch (ioEx: IOException) {
                Log.e(TAG,
                    "Network error when deleting count $pendingToDeleteCount",
                    ioEx)
                return if (MAX_REINTENT ==-1 || failedRequestsCounter < MAX_REINTENT) {
                    Log.i(TAG, "reintentando eliminacion del conteo...")
                    failedRequestsCounter++
                    Result.retry()
                } else {
                    Log.e(TAG, "fallo la elimiinacion del conteo!")
                    Result.failure(workDataOf(
                            "error" to appContext.getString(R.string.error_deleting_count)
                    ))
                }
            }
        }
        Log.e(TAG, "No se recibio el conteo a eliminar")
        return Result.failure(workDataOf(
                "error" to appContext.getString(R.string.no_data_received)
        ))
    }

    companion object{
        private const val TAG = "DELETE_COUNT_WORKER"
        private const val MAX_REINTENT = 3
    }
}