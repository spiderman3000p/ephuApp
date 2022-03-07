package com.tau.ephuapp.models

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import java.io.Serializable

@Entity(primaryKeys = ["id", "taskId"], indices = [Index("taskId")])
data class Location(
        var id: Int,
        var code: String? = null,
        var lane: String? = null,
        var columnAt: Int? = null,
        var height: Int = 0,
        var deep: Int? = null,
        var locationType: String? = null,
        var locationId: Int? = null,
        var depotId: Int? = null,
        var taskId: Int = 0,
        var isEmpty: Boolean? = null
): Serializable {
        @Ignore
        var details: List<ItemCount>? = null

        fun initDetails(details: List<ItemCount>?){
                this.details = details
        }
}