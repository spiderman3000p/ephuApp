package com.tau.ephuapp.models

import java.io.Serializable

data class ChangeTaskStateResponse(
    val errorCode: String? = null,
    val message: String? = null
): Serializable