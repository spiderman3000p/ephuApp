package com.tau.ephuapp.services

import android.content.Context
import android.util.Log
import androidx.work.*
import com.google.gson.Gson
import com.tau.ephuapp.classes.*
import com.tau.ephuapp.models.ItemCount
import com.tau.ephuapp.models.TaskState
import org.jetbrains.anko.doAsync
import java.io.File
import java.util.concurrent.TimeUnit

class MyWorkerManagerService {
    companion object{
        private val TAG = "MY_WORKER_MANAGER_SERVICE"
        val filesToUpload: MutableMap<String, File> = mutableMapOf()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

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

        fun enqueCountToUploadArrayWork(context: Context, newCountsToSave: List<ItemCount>, taskId: Int, tag: String){
            Log.i(TAG, "encolando work para subir lista de counts $newCountsToSave")
            if(newCountsToSave.size > 50) {
                var from = 0
                var to = 49
                doAsync {
                    while (true) {
                        if (from == newCountsToSave.size - 1) {
                            break
                        }
                        enqueCountsToUpload(context, taskId, newCountsToSave.subList(from, to), tag)
                        from = to + 1
                        to = if (to + 50 >= newCountsToSave.size) {
                            newCountsToSave.size - 1
                        } else {
                            to + 50
                        }
                        Thread.sleep(5000)
                    }
                }
            } else {
                enqueCountsToUpload(context, taskId, newCountsToSave, tag)
            }
        }

        private fun enqueCountsToUpload(context: Context, taskId: Int, counts: List<ItemCount>, tag: String){
            val data = workDataOf(
                "counts" to counts.map {
                    it.localId
                }.toTypedArray(),
                "taskId" to taskId
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
                .enqueueUniqueWork(tag, ExistingWorkPolicy.APPEND, uploadWorkRequest)
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
                    .enqueueUniqueWork(tag, ExistingWorkPolicy.APPEND, uploadWorkRequest)
        }

        fun enqueChangeLocationIsEmptyWork(context: Context, counts: List<ItemCount>, taskId: Int, locationId: Int, isEmpty: Boolean, tag: String){
            Log.i(TAG, "encolando work para cambiar isEmpty de la ubicacion $locationId de la tarea $taskId a $isEmpty")
            val countsJSON = Gson().toJson(counts)
            val data = workDataOf(
                "taskId" to taskId,
                "isEmpty" to isEmpty,
                "locationId" to locationId,
                "countsJSON" to countsJSON
            )
            Log.i(TAG, "data enviada al work de cambiar isEmpty de ubicacion $locationId de la tarea $taskId: $data")
            val uploadWorkRequest =
                OneTimeWorkRequestBuilder<ChangeLocationIsEmptyWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                    .addTag(tag)
                    .setInputData(data)
                    .build()
            WorkManager
                .getInstance(context)
                .enqueueUniqueWork(tag, ExistingWorkPolicy.APPEND, uploadWorkRequest)
        }
    }
}