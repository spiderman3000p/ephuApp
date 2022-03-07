package com.tau.ephuapp.classes

import android.annotation.SuppressLint
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
import com.tau.ephuapp.services.MyDataService
import com.tau.ephuapp.services.MyClient
import java.io.IOException
import java.net.SocketTimeoutException

@SuppressLint("LongLogTag")
class UploadSingleCountWorker
    (val appContext: Context,
    val workerParams: WorkerParameters
) : Worker(appContext, workerParams) {
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
        if (inputData.hasKeyWithValueOfType<String>("count") && inputData.hasKeyWithValueOfType<Int>("taskId")) {
            Log.i(TAG, "conteo recibido con exito")
            val pendingToUploadCount = Gson().fromJson(inputData.getString("count"), ItemCount::class.java)
            val taskId = inputData.getInt("taskId", 0)
            Log.i(TAG, "conteo: $pendingToUploadCount")
            val dataService: MyDataService = MyClient.getInstance(appContext).create(MyDataService::class.java)
            Log.i(TAG, "guardando conteo $pendingToUploadCount")
            try {
                val call = dataService.saveCounts(listOf(pendingToUploadCount)).execute()
                if (call.code() == 500 || call.code() == 400 || call.code() == 404 || call.code() == 403 || call.code() == 401) {
                    Log.e(TAG, "upload count error response: ${call.errorBody()}")
                    return if (failedRequestsCounter < MAX_REINTENT) {
                        Log.i(TAG, "reintendando envio de conteo...")
                        failedRequestsCounter++
                        Result.retry()
                    } else {
                        Log.e(TAG, "Numero maximo de reintentos alcanzado para guardar conteo: $pendingToUploadCount")
                        var dataToReturn: Data?
                        try {
                            dataToReturn = generateFailureData(pendingToUploadCount, taskId)
                        } catch (e: Exception){
                            Log.e(TAG, appContext.getString(R.string.counts_uploaderror_error_on_localupdate) +" ${e.message}")
                            return Result.failure(workDataOf(
                                "exception" to appContext.getString(R.string.counts_uploaderror_error_on_localupdate)
                            ))
                        }
                        Result.failure(dataToReturn)
                    }
                } else if (call.code() == 200 || call.code() == 201 || call.code() == 202) {
                    Log.i(TAG, "upload count response: ${call.body()}")
                    pendingToUploadCount.uploaded = true
                    pendingToUploadCount.sent = false
                    pendingToUploadCount.dirty = false
                    db?.itemCountDao()?.insert(pendingToUploadCount)
                    //return Result.success()

                    val uploadedCounts = call.body()

                    try {
                        db?.runInTransaction {
                            uploadedCounts?.forEach { uploadedCount ->
                                if ((uploadedCount.hasError != true && uploadedCount.id != null) ||
                                    (uploadedCount.hasError == true && uploadedCount.id != null && uploadedCount.errorMessage?.contains(
                                        "ya esta registrado"
                                    ) == true)
                                ) {
                                    if (uploadedCount.hasError == true && uploadedCount.id != null && uploadedCount.errorMessage?.contains(
                                            "ya esta registrado"
                                        ) == true
                                    ) {
                                        uploadedCount.hasError = null
                                        uploadedCount.errorMessage = null
                                    }
                                    db?.itemCountDao()
                                        ?.updateUploaded(uploadedCount.localId, uploadedCount.id!!)
                                } else {
                                    db?.itemCountDao()
                                        ?.updateWithError(
                                            uploadedCount.errorMessage
                                                ?: applicationContext.getString(R.string.unknown_error),
                                            uploadedCount.localId
                                        )
                                }
                            }
                        }
                    } catch (e: Exception){
                        Log.e(TAG, appContext.getString(R.string.counts_uploaded_successfully_error_on_local_update) +" ${e.message}")
                        return Result.success(workDataOf(
                            "exception" to appContext.getString(R.string.counts_uploaded_successfully_error_on_local_update)
                        ))
                    }
                    var dataToReturn: Data = workDataOf()
                    if (!uploadedCounts.isNullOrEmpty()) {
                        val dataMap = mutableMapOf<String, String?>()
                        uploadedCounts[0].lastUpdateTimestamp =
                            pendingToUploadCount.lastUpdateTimestamp
                        dataMap.put("localId-${pendingToUploadCount.localId}", Gson().toJson(pendingToUploadCount))
                        dataToReturn = Data.Builder().putAll(dataMap.toMap()).build()
                    }
                    return Result.success(dataToReturn)
                }
            } catch(toe: SocketTimeoutException) {
                Log.e(TAG, "Network error when uploading count $pendingToUploadCount", toe)
                return if (failedRequestsCounter < MAX_REINTENT) {
                    Log.i(TAG, "reintentando enviar conteo...")
                    failedRequestsCounter++
                    Result.retry()
                } else {
                    Log.e(TAG, appContext.getString(R.string.fail_sending_counts))
                    var dataToReturn: Data?
                    try {
                        dataToReturn = generateFailureData(pendingToUploadCount, taskId)
                    } catch (e: Exception){
                        Log.e(TAG, appContext.getString(R.string.counts_uploaderror_error_on_localupdate) +" ${e.message}")
                        return Result.failure(workDataOf(
                            "exception" to appContext.getString(R.string.counts_uploaderror_error_on_localupdate)
                        ))
                    }
                    Result.failure(dataToReturn)
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
                    Log.e(TAG, appContext.getString(R.string.fail_sending_counts))
                    var dataToReturn: Data?
                    try {
                        dataToReturn = generateFailureData(pendingToUploadCount, taskId)
                    } catch (e: Exception){
                        Log.e(TAG, appContext.getString(R.string.counts_uploaderror_error_on_localupdate) +" ${e.message}")
                        return Result.failure(workDataOf(
                            "exception" to appContext.getString(R.string.counts_uploaderror_error_on_localupdate)
                        ))
                    }
                    Result.failure(dataToReturn)
                }
            }
        }
        Log.e(TAG, "No se recibio el conteo")
        return Result.failure()
    }

    private fun generateFailureData(pendingToUploadCount: ItemCount, taskId: Int, uploaded: Boolean = false): Data {
        db?.runInTransaction {
            pendingToUploadCount.hasError = true
            pendingToUploadCount.errorMessage = applicationContext.getString(R.string.error_uploading_count)
            db?.itemCountDao()?.update(pendingToUploadCount)
            val task = db?.tasksDao()?.getById(taskId)
            db?.itemCountDao()?.deleteAllByTaskId(taskId)
            if (task?.taskType == TaskType.Recount) {
                Log.i(TAG, "la tarea es de reconteo")
                val locationsToUpdate = db?.taskLocationsDao()?.getAllByTask(task.id)
                Log.i(TAG, "las ubicaciones a actualizar son: $locationsToUpdate")
                locationsToUpdate?.forEach{location ->
                    Log.i(
                        TAG,
                        "actualizando details de la ubicacion ${location.locationId}"
                    )
                    val locationRecounts = location.details?.map { itemCount ->
                        if (pendingToUploadCount.localId == itemCount.localId){
                            itemCount.id = pendingToUploadCount.id
                            itemCount.uploaded = uploaded
                            itemCount.sent = false
                            itemCount.dirty = false
                            itemCount.hasError = true
                            itemCount.errorMessage = applicationContext.getString(R.string.error_uploading_count)
                        }
                        itemCount
                    }
                    if (!locationRecounts.isNullOrEmpty()) {
                        val details = arrayListOf<ItemCount>()
                        details.addAll(location.details!!)
                        db?.itemCountDao()?.insertAll(details)
                        Log.i(
                            TAG,
                            "ubicacion ${location.locationId} actualizada con el details: $details"
                        )
                    }
                }
            }
        }
        val dataMap = mutableMapOf<String, String?>()
        dataMap.put("localId-${pendingToUploadCount.localId}", Gson().toJson(pendingToUploadCount))
        return Data.Builder().putAll(dataMap.toMap()).build()
    }

    companion object{
        private const val TAG = "UPLOAD_SINGLE_COUNT_WORKER"
        private const val MAX_REINTENT = 3
    }
}