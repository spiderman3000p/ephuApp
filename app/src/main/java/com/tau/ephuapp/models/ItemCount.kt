package com.tau.ephuapp.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(indices = [Index("locationId")]/*, foreignKeys = [ForeignKey(entity = Location::class,
    parentColumns = ["id"], childColumns = ["locationId"],
    onDelete = ForeignKey.NO_ACTION, onUpdate = ForeignKey.CASCADE)]*/)
data class ItemCount(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var taskId: Int? = null,
    var itemId: Int? = null,
    var lpnCode: String? = null,
    var ephuDeviceId: String? = null,
    var location: String? = null,
    var readTimestamp: String? = null,
    var quantity: Int? = null,
    var lot: String? = null,
    var expiryDate: String? = null,
    var createdDate: String? = null,
    var multireference: Boolean? = null,
    var empty: Boolean? = null,
    var serial: String? = null,
    //var lpn: String? = null,
    // for local use
    var editing: Boolean = false,
    var localId: String = "",
    var locationId: Int? =null,
    var description: String? = null,
    var sku: String? = null,
    var uploaded: Boolean? = null, // identifica cuando un registro ha sido subido satisfactoriamente
    var sent: Boolean? = null, // identifica cuando un registro ha sido enviado al servidor remoto
    var dirty: Boolean? = null // identifica cuando un registro ha sido modificado en la app
): Serializable