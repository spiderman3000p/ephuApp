package com.tau.ephuapp.services

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.tau.ephuapp.classes.Utilities

class MySettings {
    val TAG = "MY_SETTINGS"
    var baseUrl: String?
    private var deviceId: String?
    var defaultDeviceId = "0000000000"
    var defaultBaseUrl = "https://operation.ephu-ccl.com/"
    constructor(context: Context){
        baseUrl = null
        deviceId = null
        resetValues(context)
    }

    fun resetValues(context: Context){
        Log.i(TAG, "reseteando valores de settings...")
        defaultDeviceId = Utilities.getAndroidId(context)
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        if(!preferences.contains("base_url")){
            preferences.edit {
                putString("base_url", defaultBaseUrl)
            }
        }
        if(!preferences.contains("device_id")){
            preferences.edit {
                putString("device_id", defaultDeviceId)
            }
        }
        baseUrl = preferences.getString("base_url", defaultBaseUrl) ?: defaultBaseUrl
        deviceId = preferences.getString("device_id", defaultDeviceId) ?: defaultDeviceId
        Log.i(TAG, "baseUrl: $baseUrl")
        Log.i(TAG, "deviceId: $baseUrl")
    }

    companion object{
        private var instance: MySettings? = null
        fun getInstance(context: Context): MySettings{
            if(instance == null){
                instance = MySettings(context)
            }
            return instance!!
        }

        fun resetValues(context: Context){
            instance?.resetValues(context)
        }
    }
}