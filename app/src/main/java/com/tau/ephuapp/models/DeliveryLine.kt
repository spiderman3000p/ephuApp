package com.tau.ephuapp.models

import androidx.room.Entity
import java.io.Serializable

@Entity(primaryKeys = ["planificationId", "id", "deliveryId", "index"])
data class DeliveryLine(
    var id: Long = 0,
    var packetType: String? = "",
    var price: Double? = 0.0,
    var quantity: Int = 0,
    var deliveredQuantity: Int? = 0,
    var reference: String? = "",
    var description: String? = "",
    var weight: Double? = 0.0,
    var deliveryId: Long = 0,
    var planificationId: Long = 0,
    var uploaded: Boolean = false,
    var certified: Int = 0,
    var delivered: Boolean? = null,
    var index: Int = 0,
    var certificationType: String? = "",
    var scannedOrder: Int? = null,
    var sku: String? = "") : Serializable