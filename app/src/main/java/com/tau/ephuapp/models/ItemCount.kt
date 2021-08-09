package com.tau.ephuapp.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(indices = [Index("locationId"), Index("localId", unique = true)]/*, foreignKeys = [ForeignKey(entity = Location::class,
    parentColumns = ["id"], childColumns = ["locationId"],
    onDelete = ForeignKey.NO_ACTION, onUpdate = ForeignKey.CASCADE)]*/)
data class ItemCount(
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null,// 2
    var taskId: Int? = null, // 5
    var taskLineId: Int? = null,
    var itemId: Int? = null,// null
    var lpnCode: String? = null,// generado aut: EPHU_EMPTY_{UUID o idLocal}
    var ephuDeviceId: String? = null,// lo mismo
    var location: String? = null,// lo mismo
    var readTimestamp: String? = null,//
    var lastUpdateTimestamp: Long? = null,//
    var quantity: Float = 0f,// 0
    var lot: String? = null,// null
    var expiryDate: String? = null,// null
    var createdDate: String? = null,// null
    var multireference: Boolean? = null, // null
    var serial: String? = null, // null
    var hasError: Boolean? = null, // null
    var errorMessage: String? = null, // null
    var localId: String = "",
    //var lpn: String? = null,
    // for local use
    var recount: Boolean = false,
    var locationId: Int? = null,
    var description: String? = null,
    var sku: String? = null,
    var editing: Boolean = false,
    var uploaded: Boolean = false, // identifica cuando un registro ha sido subido satisfactoriamente
    var sent: Boolean = false, // identifica cuando un registro ha sido enviado al servidor remoto
    var dirty: Boolean = false // identifica cuando un registro ha sido modificado en la app
): Serializable