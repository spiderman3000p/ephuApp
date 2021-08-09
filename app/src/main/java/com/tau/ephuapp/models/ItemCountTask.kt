package com.tau.ephuapp.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity
data class ItemCountTask(
    @PrimaryKey
    var taskLineId: Int,// 2
    var taskId: Int, // 5
    var itemId: Int,// null
    var lpnCode: String? = null,// generado aut: EPHU_EMPTY_{UUID o idLocal}
    var lot: String? = null,// null
    var expiryDate: String? = null,// null
    var createdDate: String? = null,// null
    var serial: String? = null, // null
    var locationId: Int? = null,
    var editing: Boolean = false
): Serializable