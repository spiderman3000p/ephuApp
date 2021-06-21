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

class UploadEditSingleCountWorker
    (val appContext: Context,
    val workerParams: WorkerParameters
) : Worker(appContext, workerParams) {
    private val TAG = "UPLOAD_EDIT_SINGLE_COUNT_WORKER"
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
            if(pendingToUploadCount.uploaded == false){
                return Result.failure(workDataOf(
                        "error" to appContext.getString(R.string.error_editing_not_uploaded_count)
                ))
            }
            val dataService: MyDataService = MyClient.getInstance(appContext).create(MyDataService::class.java)
            Log.i(TAG, "guardando edicion de conteo $pendingToUploadCount")
            try {
                val url = "inventories/${pendingToUploadCount.taskId}/countDetail/${pendingToUploadCount.id}"
                val call = dataService.editCount(url, pendingToUploadCount).execute()
                if (call.code() == 500 || call.code() == 400 || call.code() == 404 || call.code() == 403 || call.code() == 401) {
                    Log.e(TAG, "upload  edit count error response: ${call.errorBody()}")
                    return if (failedRequestsCounter < MAX_REINTENT) {
                        Log.i(TAG, "reintendando envio de conteo...")
                        failedRequestsCounter++
                        Result.retry()
                    } else {
                        Log.i(TAG, "edit count no pudo ser guardado en la BD remota")
                        Result.failure(workDataOf(
                                "error" to appContext.getString(R.string.error_saving_edit_count)
                        ))
                    }
                } else if (call.code() == 200 || call.code() == 201 || call.code() == 202) {
                    val uploadedCount = call.body()
                    val dataMap = mutableMapOf<String, Int?>()
                    Log.i(TAG, "upload edit count response: $uploadedCount")
                    if(uploadedCount != null && pendingToUploadCount.id != uploadedCount.id && uploadedCount.hasError == false) {
                        dataMap.put(uploadedCount.localId, uploadedCount.id)
                        db?.itemCountDao()?.delete(pendingToUploadCount)
                        db?.itemCountDao()?.insert(uploadedCount)
                    } else if(uploadedCount?.hasError == false){
                        db?.itemCountDao()?.updateUpdated(uploadedCount.localId)
                    } else if(uploadedCount?.hasError == true){
                        db?.itemCountDao()?.updateUpdatedWithError(uploadedCount.errorMessage ?: applicationContext.getString(R.string.unknown_error), uploadedCount.localId)
                    }
                    val dataToReturn = Data.Builder().putAll(dataMap.toMap()).build()
                    return Result.success(dataToReturn)
                }
            } catch(toe: SocketTimeoutException) {
                Log.e(TAG, "Network error when uploading edit count $pendingToUploadCount", toe)
                return if (failedRequestsCounter < MAX_REINTENT) {
                    Log.i(TAG, "reintentando enviar edicion de conteo...")
                    failedRequestsCounter++
                    Result.retry()
                } else {
                    Log.e(TAG, "fallo el envio de edicion de conteo!")
                    Result.failure(workDataOf(
                            "error" to appContext.getString(R.string.error_saving_edit_count)
                    ))
                }
            } catch (ioEx: IOException) {
                Log.e(TAG,
                    "Network error when uploading edit count $pendingToUploadCount",
                    ioEx)
                return if (failedRequestsCounter < MAX_REINTENT) {
                    Log.i(TAG, "reintentando enviar edicion de conteo...")
                    failedRequestsCounter++
                    Result.retry()
                } else {
                    Log.e(TAG, "fallo el envio de edicion de conteo!")
                    Result.failure(workDataOf(
                            "error" to appContext.getString(R.string.error_saving_edit_count)
                    ))
                }
            }
        }
        Log.e(TAG, "No se recibio el conteo a editar")
        return Result.failure(workDataOf(
                "error" to appContext.getString(R.string.no_data_received)
        ))
    }
}