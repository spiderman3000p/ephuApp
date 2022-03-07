package com.tau.ephuapp.services

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.tau.ephuapp.classes.Utilities

class MySettings(val context: Context) {
    private var deviceId: String?
    private var password: String?
    var defaultDeviceId = "0000000000"
    var defaultBaseUrl = "https://operation.ephu-ccl.com/"
    //var defaultBaseUrl = "http:192.168.10.13:8080/"
    var defaultPassword = "12345"

    init {
        deviceId = null
        password = null
        resetValues(context)
    }

    fun setBaseUrl(value: String) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit {
            putString("base_url", value)
            commit()
        }
    }

    fun getBaseUrl(context: Context): String {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        Log.i(TAG, "preferences: $preferences")
        return if(!preferences.contains("base_url")){
            preferences.edit {
                putString("base_url", defaultBaseUrl)
                commit()
            }
            defaultBaseUrl
        } else {
            preferences.getString("base_url", defaultBaseUrl) ?: defaultBaseUrl
        }
    }

    fun resetValues(context: Context){
        Log.i(TAG, "reseteando valores de settings...")
        defaultDeviceId = Utilities.getAndroidId(context)
        //val preferences = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        Log.i(TAG, "preferences: $preferences")
        if(!preferences.contains("base_url")){
            preferences.edit {
                putString("base_url", defaultBaseUrl)
                commit()
            }
        }
        if(!preferences.contains("device_id")){
            preferences.edit {
                putString("device_id", defaultDeviceId)
                commit()
            }
        }
        if(!preferences.contains("password")){
            preferences.edit {
                putString("password", defaultPassword)
                commit()
            }
        }
        setBaseUrl(preferences.getString("base_url", defaultBaseUrl) ?: defaultBaseUrl)
        deviceId = preferences.getString("device_id", defaultDeviceId) ?: defaultDeviceId
        password = preferences.getString("password", defaultPassword) ?: defaultPassword
        Log.i(TAG, "baseUrl: ${getBaseUrl(context)}")
        Log.i(TAG, "deviceId: $deviceId")
        Log.i(TAG, "password: $password")
    }

    companion object{
        const val TAG = "MY_SETTINGS"
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