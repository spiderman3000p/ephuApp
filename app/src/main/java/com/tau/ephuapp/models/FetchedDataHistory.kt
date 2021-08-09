package com.tau.ephuapp.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

enum class HistoryType{
    ITEMS, TASKS, DEVICES, LOCATIONS, LOCATIONS_RECOUNT, COUNTS, TASK_COUNTS, LAST_PENDING_REVISION
}
@Entity()
data class FetchedDataHistory(
    @PrimaryKey
    var tag: String, // class name
    var lastUpdate: Long // timestamp
): Serializable