package com.tau.ephuapp.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable
@Entity
data class CertificationTaskItem(
        @PrimaryKey
        var itemId: Int,
        var taskId: Int,
        var itemSku: String? = null,
        var itemDescription: String? = null,
        var itemUom: String? = null,
        var totalPackaging: String? = null,
        var totalUnits: Int = 0,
        var totalBalance: Int? = null,
        var boxesBalance: Int? = null,
        var totalBoxesRequested: Int? = null,
        var taskQuantity: Int = 0,
        // for internal query
        var totalQuantity: Int = 0
): Serializable