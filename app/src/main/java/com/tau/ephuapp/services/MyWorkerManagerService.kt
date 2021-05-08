package com.tau.ephuapp.services

import android.content.Context
import android.util.Log
import androidx.work.*
import com.google.gson.Gson
import com.tau.ephuapp.classes.*
import com.tau.ephuapp.models.ItemCount
import com.tau.ephuapp.models.TaskState
import java.io.File
import java.util.concurrent.TimeUnit

class MyWorkerManagerService {
    companion object{
        private val TAG = "MY_WORKER_MANAGER_SERVICE"
        val filesToUpload: MutableMap<String, File> = mutableMapOf()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        fun enqueCountToUploadWork(context: Context, count: ItemCount){
            Log.i(TAG, "encolando work para subir count $count")
            val countJSON = Gson().toJson(count)
            val data = workDataOf(
                "countJSON" to countJSON
            )
            val uploadWorkRequest =
                OneTimeWorkRequestBuilder<UploadSingleCountWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                    .addTag("uploadCountRequest-${count.localId}")
                    .setInputData(data)
                    .build()
            WorkManager
                .getInstance(context)
                .enqueueUniqueWork("uploadCountRequest-${count.localId}", ExistingWorkPolicy.APPEND_OR_REPLACE, uploadWorkRequest)
        }

        fun enqueEditCountToUploadWork(context: Context, count: ItemCount){
            Log.i(TAG, "encolando work para subir edicion de count $count")
            val countJSON = Gson().toJson(count)
            val data = workDataOf(
                    "countJSON" to countJSON
            )
            Log.i(TAG, "data enviada al work de editar conteo: $data")
            val uploadWorkRequest =
                    OneTimeWorkRequestBuilder<UploadEditSingleCountWorker>()
                            .setConstraints(constraints)
                            .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                            .addTag("uploadEditCountRequest-${count.localId}")
                            .setInputData(data)
                            .build()
            WorkManager
                    .getInstance(context)
                    .enqueueUniqueWork("uploadEditCountRequest-${count.localId}", ExistingWorkPolicy.REPLACE, uploadWorkRequest)
        }

        fun enqueDeleteCountWork(context: Context, count: ItemCount){
            Log.i(TAG, "encolando work para eliminacion de conteo $count")
            val countJSON = Gson().toJson(count)
            val data = workDataOf(
                    "countJSON" to countJSON
            )
            Log.i(TAG, "data enviada al work de eliminar conteo: $data")
            val uploadWorkRequest =
                    OneTimeWorkRequestBuilder<DeleteCountWorker>()
                            .setConstraints(constraints)
                            .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                            .addTag("deleteCountRequest-${count.localId}")
                            .setInputData(data)
                            .build()
            WorkManager
                    .getInstance(context)
                    .enqueueUniqueWork("deleteCountRequest-${count.localId}", ExistingWorkPolicy.REPLACE, uploadWorkRequest)
        }

        fun enqueCountToUploadArrayWork(context: Context, counts: List<ItemCount>, tag: String){
            Log.i(TAG, "encolando work para subir lista de counts $counts")
            val countsJSON = Gson().toJson(counts)
            val data = workDataOf(
                "countsJSON" to countsJSON
            )
            Log.i(TAG, "data enviada al work de subir conteos: $data")
            val uploadWorkRequest =
                OneTimeWorkRequestBuilder<UploadMultipleCountsWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                    .addTag(tag)
                    .setInputData(data)
                    .build()
            WorkManager
                .getInstance(context)
                .enqueueUniqueWork(tag, ExistingWorkPolicy.REPLACE, uploadWorkRequest)
        }

        fun enqueChangeTaskStateWork(context: Context, taskId: Int, state: TaskState, tag: String){
            Log.i(TAG, "encolando work para cambiar estado de la tarea $taskId a $state")
            val data = workDataOf(
                "taskId" to taskId,
                "state" to state.toString()
            )
            Log.i(TAG, "data enviada al work de cambiar estado de tarea: $data")
            val uploadWorkRequest =
                    OneTimeWorkRequestBuilder<ChangeTaskStateWorker>()
                            .setConstraints(constraints)
                            .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                            .addTag(tag)
                            .setInputData(data)
                            .build()
            WorkManager
                    .getInstance(context)
                    .enqueueUniqueWork(tag, ExistingWorkPolicy.REPLACE, uploadWorkRequest)
        }
    }
}