package com.tau.ephuapp.classes

import android.annotation.SuppressLint
import android.content.Context
import android.database.sqlite.SQLiteAccessPermException
import android.database.sqlite.SQLiteCantOpenDatabaseException
import android.database.sqlite.SQLiteDatabaseLockedException
import android.util.Log
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.hasKeyWithValueOfType
import com.google.gson.Gson
import com.tau.ephuapp.database.AppDatabase
import com.tau.ephuapp.models.Certification
import com.tau.ephuapp.repositories.MainRepository
import com.tau.ephuapp.services.MyClient
import com.tau.ephuapp.services.MyDataService
import com.tau.ephuapp.services.MySettings
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.*
@SuppressLint("LongLogTag")
class UploadSingleCertificationWorker
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
        if (inputData.hasKeyWithValueOfType<String>("certification")) {
            val certification: Certification = Gson().fromJson(inputData.getString("certification"), Certification::class.java)
            val taskId: Int = certification.taskId
            val androidId = Utilities.getAndroidId(appContext)
            Log.i(TAG, "fetching remote device data for devide $androidId...")
            val url = "saveTaskCount/$taskId/$androidId"
            val dataService: MyDataService? = MyClient.getInstance(appContext).create(
                MyDataService::class.java)
            if (dataService != null) {
                Log.i(TAG, "guardando certificacion $certification")
                    try {
                        val call = dataService.saveCertification(url, certification)
                            .execute()
                        if (call.code() == 500 || call.code() == 400 || call.code() == 404 || call.code() == 403 || call.code() == 401) {
                            Log.e(TAG, "upload certification error response: ${call.errorBody()}")
                            return if (failedRequestsCounter < MAX_REINTENT) {
                                failedRequestsCounter++
                                Result.retry()
                            } else {
                                Log.i(TAG, "guardando certificacion en la tabla de requests fallidas $certification")
                                Result.failure()
                            }
                        } else if (call.code() == 200 || call.code() == 201 || call.code() == 202) {
                            db?.certificationTaskItemsDao()?.setAsUploaded(certification.itemId, taskId)
                            db?.certificationTaskItemsDao()?.updateTaskQuantity(certification.itemId, taskId, certification.quantity)
                            val dataMap = mutableMapOf<String?, Int?>()
                            dataMap.put(certification.itemId.toString(), certification.quantity)
                            val dataToReturn = Data.Builder().putAll(dataMap.toMap()).build()
                            return Result.success(dataToReturn)
                        }
                    } catch(toe: SocketTimeoutException) {
                        Log.e(TAG, "Network error when uploading certification $certification", toe)
                        return if (failedRequestsCounter < MAX_REINTENT) {
                            failedRequestsCounter++
                            Result.retry()
                        } else {
                            Result.failure()
                        }
                    } catch (ioEx: IOException) {
                        Log.e(TAG,
                            "Network error when uploading certification $certification",
                            ioEx)
                        return if (failedRequestsCounter < MAX_REINTENT) {
                            failedRequestsCounter++
                            Result.retry()
                        } else {
                            Result.failure()
                        }
                    }
                } else {
                Log.e(TAG, "El cliente http o la autenticacion de usuario son invalidos. No se puede realizar el proceso")
            }
        }
        return Result.failure()
    }

    companion object{
        private const val TAG = "UPLOAD_SINGLE_CERTIFICATION_WORKER"
        private const val MAX_REINTENT = 3
    }
}