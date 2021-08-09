package com.tau.ephuapp.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONArray
import java.io.Serializable

@Entity(indices = [Index("taskId")]/*, foreignKeys = [ForeignKey(entity = Task::class,
    parentColumns = ["id"],
    childColumns = ["taskId"],
    onDelete = ForeignKey.CASCADE,
    onUpdate = ForeignKey.CASCADE)]*/)
data class Location(
    @PrimaryKey
    var id: Int,
    var code: String? = null,
    var lane: String? = null,
    var columnAt: Int? = null,
    var height: Int = 0,
    var deep: Int? = null,
    var locationType: String? = null,
    var locationId: Int? = null,
    var depotId: Int? = null,
    var taskId: Int? = null,
    var details: String? = null,
    var isEmpty: Boolean? = null
): Serializable