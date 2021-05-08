package com.tau.ephuapp.models

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.io.Serializable

enum class TaskState{
    Complete, Paused, Active, Cancelled, Pending, WorkInProgress
}
@Entity
data class Task(
    @PrimaryKey
    var id: Int = 0,
    var name: String? = null,
    var taskState: TaskState? = null,
    var device: String? = null,
    var totalTime: Long = 0,
    var lines: Int? = null,
    var progress: Int? = 0,
    var count: Int? = 1,
    @Ignore
    var parameters: List<TaskParameter>? = null
): Serializable{
    fun initParameters(params: List<TaskParameter>){
        parameters = params
    }
}