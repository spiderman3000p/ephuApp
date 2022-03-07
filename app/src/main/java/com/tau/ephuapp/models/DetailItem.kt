package com.tau.ephuapp.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity/*(indices = [Index("ean13", unique = true), Index("ean14", unique = true), Index("sku", unique = true)])*/
data class DetailItem(
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null,
    var taskLineId: Int? = null,
    var itemId: Int? = null,
    var lpnCode: String? = null,
    var lot: String? = null,
    var createdDate: String? = null,
    var expiryDate: String? = null,
    var serial: String? = null
): Serializable