package com.tau.ephuapp.models

import androidx.annotation.Nullable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.io.Serializable
enum class ParameterType{
    Lot, Lpn, CreatedDate, ExpiryDate, Serial, Empty, MultiReference
}
@Entity(primaryKeys = ["taskId", "parameterType"])
data class TaskParameter(
    var taskId: Int,
    var parameterType: ParameterType,
    var value: Boolean? = false
): Serializable