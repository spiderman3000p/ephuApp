package com.tau.ephuapp.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(indices = [Index("code", unique = true)])
data class Device(
    @PrimaryKey
    var id: Int,
    var name: String? = null,
    var code: String? = null,
    var depotName: String? = null,
    var depotId: Int? = null,
    var ownerName: String? = null,
    var ownerId: Int? = null,
    var version: String? = null
): Serializable