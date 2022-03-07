package com.tau.ephuapp.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import java.io.Serializable

@Entity(primaryKeys = ["deliveryLineId", "planificationId", "index"])
data class PendingToUploadCertification(
    var quantity: Int = 1,
    var index: Int = 0,
    var deliveryLineId: Long = 0,
    var planificationId: Long = 0,
    var deliveryId: Long = 0): Serializable {

}