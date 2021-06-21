package com.tau.ephuapp.classes

import android.content.Context
import android.database.sqlite.SQLiteAccessPermException
import android.database.sqlite.SQLiteCantOpenDatabaseException
import android.database.sqlite.SQLiteDatabaseLockedException
import android.util.Log
import androidx.work.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.tau.ephuapp.R
import com.tau.ephuapp.database.AppDatabase
import com.tau.ephuapp.models.ItemCount
import com.tau.ephuapp.models.TaskType
import com.tau.ephuapp.services.MyClient
import com.tau.ephuapp.services.MyDataService
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.*


class UploadMultipleCountsWorker
    (
    val appContext: Context,
    val workerParams: WorkerParameters
) : Worker(appContext, workerParams) {
    private val TAG = "UPLOAD_MULTIPLE_COUNTS_WORKER"
    private val MAX_REINTENT = 3
    private var failedRequestsCounter = 0
    var db: AppDatabase? = null

    override fun doWork(): Result {
        try {
            db = AppDatabase.getDatabase(appContext)
        } catch (ex: SQLiteDatabaseLockedException) {
            Log.e(TAG, "Database error found", ex)
        } catch (ex: SQLiteAccessPermException) {
            Log.e(TAG, "Database error found", ex)
        } catch (ex: SQLiteCantOpenDatabaseException) {
            Log.e(TAG, "Database error found", ex)
        }
        if (inputData.hasKeyWithValueOfType<Array<String>>("counts") && inputData.hasKeyWithValueOfType<Int>("taskId")) {
            Log.i(TAG, "conteos recibidos con exito")
            val localIds = inputData.getStringArray("counts")
            val taskId = inputData.getInt("taskId", 0)
            if(localIds == null){
                Log.e(TAG, "Error con los local ids recibidos en el worker")
                return Result.failure()
            }
            if(taskId == 0){
                Log.e(TAG, "Error con el id de tarea recibida en el worker")
                return Result.failure()
            }
            val pendingToUploadCounts = db?.itemCountDao()?.loadAllByLocalIds(localIds)
            if(pendingToUploadCounts == null){
                Log.e(TAG, "Error con los registros de conteos obtenidos en el worker")
                return Result.failure()
            }
            Log.i(TAG, "Conteos cargados en el worker: $pendingToUploadCounts")
            val dataService: MyDataService = MyClient.getInstance(appContext).create(MyDataService::class.java)
            Log.i(TAG, "guardando conteos...")
            try {
                val call = dataService.saveCounts(pendingToUploadCounts).execute()
                if (call.code() == 500 || call.code() == 400 || call.code() == 404 || call.code() == 403 || call.code() == 401) {
                    Log.e(TAG, "upload counts error response: ${call.errorBody()}")
                    return if (failedRequestsCounter < MAX_REINTENT) {
                        Log.i(TAG, "reintendando envio de conteos...")
                        failedRequestsCounter++
                        Result.retry()
                    } else {
                        Log.i(TAG, "guardando count en la tabla de requests fallidas")
                        Log.i(TAG, "count no pudo ser guardado en la BD remota")
                        Result.failure(workDataOf(
                                "error" to appContext.getString(R.string.fail_sending_counts)
                        ))
                    }
                } else if (call.code() == 200 || call.code() == 201 || call.code() == 202) {
                    val uploadedCounts = call.body()
                    Log.i(TAG, "upload counts response: $uploadedCounts")
                    try {
                        db?.runInTransaction {
                            uploadedCounts?.forEach { uploadedCount ->
                                if(uploadedCount.hasError == false && uploadedCount.id != null) {
                                    db?.itemCountDao()
                                        ?.updateUploaded(uploadedCount.localId, uploadedCount.id!!)
                                } else {
                                    db?.itemCountDao()
                                        ?.updateWithError(uploadedCount.errorMessage ?: applicationContext.getString(R.string.unknown_error), uploadedCount.localId)
                                }
                            }
                            val task = db?.tasksDao()?.getById(taskId)
                            if (task?.taskType == TaskType.Recount) {
                                Log.i(TAG, "la tarea es de reconteo")
                                val locationsToUpdate = db?.taskLocationsDao()?.getAllByTask(task.id)
                                Log.i(TAG, "las ubicaciones a actualizar son: $locationsToUpdate")
                                locationsToUpdate?.forEach{location ->
                                    Log.i(
                                        TAG,
                                        "actualizando details de la ubicacion ${location.id}"
                                    )
                                    val locationRecounts = Gson().fromJson(location.details, JsonArray::class.java)?.map {jsonEl ->
                                        val itemCount = Gson().fromJson(jsonEl, ItemCount::class.java)
                                        val foundUploadedItemCount = uploadedCounts?.find {_itemCount ->
                                            _itemCount.localId == itemCount.localId
                                        }
                                        if (foundUploadedItemCount != null){
                                            itemCount.id = foundUploadedItemCount.id
                                            itemCount.uploaded = true
                                            itemCount.sent = false
                                            itemCount.dirty = false
                                        }
                                        itemCount
                                    }
                                    if (!locationRecounts.isNullOrEmpty()) {
                                        val details = Gson().toJson(locationRecounts).toString()
                                        db?.taskLocationsDao()
                                            ?.updateDetails(details, location.id)
                                        Log.i(
                                            TAG,
                                            "ubicacion ${location.id} actualizada con el details: $details"
                                        )
                                    }
                                }
                            }
                        }
                    } catch (e: Exception){
                        Log.e(TAG, appContext.getString(R.string.counts_uploaded_successfully_error_on_local_update) +" ${e.message}")
                        return Result.success(workDataOf(
                                "exception" to appContext.getString(R.string.counts_uploaded_successfully_error_on_local_update)
                        ))
                    }
                    val dataMap = mutableMapOf<String, Int?>()
                    uploadedCounts?.forEach {
                        dataMap.put(it.localId, it.id)
                    } ?: mutableMapOf<String, Int>()
                    val dataToReturn = Data.Builder().putAll(dataMap.toMap()).build()
                    return Result.success(dataToReturn)
                }
            } catch (toe: SocketTimeoutException) {
                Log.e(TAG, "Network error when uploading counts", toe)
                return if (failedRequestsCounter < MAX_REINTENT) {
                    Log.i(TAG, "reintentando enviar conteos...")
                    failedRequestsCounter++
                    Result.retry()
                } else {
                    Log.e(TAG, appContext.getString(R.string.fail_sending_counts))
                    Result.failure(workDataOf(
                            "error" to appContext.getString(R.string.fail_sending_counts)
                    ))
                }
            } catch (ioEx: IOException) {
                Log.e(
                    TAG,
                    "Network error when uploading counts",
                    ioEx
                )
                return if (failedRequestsCounter < MAX_REINTENT) {
                    Log.i(TAG, "reintentando enviar conteos...")
                    failedRequestsCounter++
                    Result.retry()
                } else {
                    Log.e(TAG, appContext.getString(R.string.fail_sending_counts))
                    Result.failure(workDataOf(
                            "error" to appContext.getString(R.string.fail_sending_counts)
                    ))
                }
            }
        }
        Log.e(TAG, appContext.getString(R.string.no_data_received))
        return Result.failure(workDataOf(
                "error" to appContext.getString(R.string.no_data_received)
        ))
    }
}