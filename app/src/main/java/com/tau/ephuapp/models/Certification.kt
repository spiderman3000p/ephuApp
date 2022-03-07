package com.tau.ephuapp.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity
data class Certification(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    var itemId: Int,
    var taskId: Int,
    var quantity: Int,
    var lot: Int? = null,
    var scannedOrder: Int = 0,
    var uploaded: Boolean = false,
    var dirty: Boolean = false,
    var hasError: Boolean = false,
    var storeId: Int = -1
): Serializable