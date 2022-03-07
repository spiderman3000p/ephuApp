package com.tau.ephuapp.models

import android.util.Log
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.io.Serializable

enum class TaskState{
    Complete, Paused, Active, Cancelled, Pending, WorkInProgress
}
enum class TaskType{
    Recount, Certification, Inventory
}
@Entity
data class Task(
        @PrimaryKey
    var id: Int = 0,
        var name: String? = null,
        var taskState: TaskState? = null,
        var taskType: TaskType? = null,
        var deviceCode: String? = null,
        var totalTime: Long = 0,
        var lines: Int? = null,
        var progress: Int? = 0,
        var count: Int? = 1,
        @Ignore
        var parameters: ArrayList<TaskParameter>? = null,
        @Ignore
        var locations: ArrayList<Location>? = null,
        @Ignore
        var items: ArrayList<CertificationTaskItem>? = null
): Serializable{
    fun initParameters(params: List<TaskParameter>){
        parameters = arrayListOf()
        parameters?.addAll(params)
    }

    fun initItems(params: List<CertificationTaskItem>){
        Log.i("Task Class", "initItems params: $params")
        items = arrayListOf()
        items?.addAll(params)
    }
}