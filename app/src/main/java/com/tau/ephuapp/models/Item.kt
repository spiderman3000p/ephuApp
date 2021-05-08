package com.tau.ephuapp.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity/*(indices = [Index("ean13", unique = true), Index("ean14", unique = true), Index("sku", unique = true)])*/
data class Item(
    @PrimaryKey
    var id: Int? = null,
    var ownerId: Int? = null,
    var sku: String? = null,
    var description: String? = null,
    var uomCode: String? = null,
    var packaging: Int? = null,
    var cost: Double? = null,
    var log: Boolean? = null,
    var serial: Boolean? = null,
    var expiryDate: Boolean? = null,
    var createdDate: Boolean? = null,
    var ean13: String? = null,
    var ean14: String? = null
): Serializable