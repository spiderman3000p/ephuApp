package com.tau.ephuapp.classes

import android.annotation.SuppressLint
import android.content.Context
import android.database.sqlite.SQLiteAccessPermException
import android.database.sqlite.SQLiteCantOpenDatabaseException
import android.database.sqlite.SQLiteDatabaseLockedException
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.hasKeyWithValueOfType
import androidx.work.workDataOf
import com.google.gson.Gson
import com.tau.ephuapp.R
import com.tau.ephuapp.database.AppDatabase
import com.tau.ephuapp.models.ItemCount
import com.tau.ephuapp.models.TaskState
import com.tau.ephuapp.services.MyDataService
import com.tau.ephuapp.services.MyClient
import java.io.IOException
import java.net.SocketTimeoutException
@SuppressLint("LongLogTag")
class ChangeTaskStateWorker
    (val appContext: Context,
    val workerParams: WorkerParameters
) : Worker(appContext, workerParams) {
    private var failedRequestsCounter = 0

    override fun doWork(): Result {
        Log.i(TAG, "input data: $inputData")
        if (inputData.hasKeyWithValueOfType<Int>("taskId") &&
                inputData.hasKeyWithValueOfType<String>("state")) {
            Log.i(TAG, "tarea de cambio de estado recibida con exito")
            val taskId = inputData.getInt("taskId", 0)
            val state = inputData.getString("state")
            Log.i(TAG, "tarea: $taskId, state: $state")
            val dataService: MyDataService = MyClient.getInstance(appContext).create(MyDataService::class.java)
            try {
                val url = "inventories/task/changeState?newState=$state&taskId=$taskId"
                //val url = "inventories/task/$taskId/${state?.toLowerCase()}"
                val call = dataService.editTaskState(url).execute()
                if (call.code() == 500 || call.code() == 400 || call.code() == 404 || call.code() == 403 || call.code() == 401) {
                    Log.e(TAG, "change task state error response: ${call.errorBody()}")
                    return when {
                        call.body()?.errorCode == "008" -> {
                             Result.success(
                                workDataOf(
                                    "error" to appContext.getString(R.string.state_change_error)
                                )
                             )
                        }
                        failedRequestsCounter < MAX_REINTENT -> {
                            Log.i(TAG, "reintendando cambio de estado de tarea $taskId a $state...")
                            failedRequestsCounter++
                            Result.retry()
                        }
                        else -> {
                            Log.i(TAG, "el estado de la tarea $taskId no pudo ser cambiado a $state")
                            Result.failure()
                        }
                    }
                } else if (call.code() == 200 || call.code() == 201 || call.code() == 202) {
                    return Result.success()
                }
            } catch(toe: SocketTimeoutException) {
                Log.e(TAG, "Network error when changing task $taskId state to $state", toe)
                return if (failedRequestsCounter < MAX_REINTENT) {
                    Log.i(TAG, "reintentando cambio de estado de tarea $taskId a $state...")
                    failedRequestsCounter++
                    Result.retry()
                } else {
                    Log.e(TAG, "fallo al cambiar estado de la tarea $taskId a $state!")
                    Result.failure()
                }
            } catch (ioEx: IOException) {
                Log.e(TAG,
                    "Network error when changing task $taskId state to $state",
                    ioEx)
                return if (failedRequestsCounter < MAX_REINTENT) {
                    Log.i(TAG, "reintentando cambiar el estado de la tarea $taskId a $state...")
                    failedRequestsCounter++
                    Result.retry()
                } else {
                    Log.e(TAG, "fallo el cambio de estado de la tarea $taskId a $state!")
                    Result.failure()
                }
            }
        }
        Log.e(TAG, "No se recibio la tarea y/o el estado")
        return Result.failure()
    }

    companion object{
        private const val TAG = "CHANGE_TASK_STATE_WORKER"
        private const val MAX_REINTENT = 3
    }
}