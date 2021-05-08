package com.tau.ephuapp.repositories

import android.content.Context
import android.text.format.DateUtils
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tau.ephuapp.classes.Utilities
import com.tau.ephuapp.database.AppDatabase
import com.tau.ephuapp.models.FetchedDataHistory
import com.tau.ephuapp.models.Device
import com.tau.ephuapp.models.HistoryType
import com.tau.ephuapp.services.MyClient
import com.tau.ephuapp.services.MyDataService
import org.jetbrains.anko.doAsync

class MainRepository {
    private val TAG = "MAIN_REPOSITORY"
    private var device = MutableLiveData<Device?>()

    fun getDevice(): LiveData<Device?> {
        return device
    }

    // device fetching ...
    fun fetchOwnerData(context: Context){
        doAsync {
            Log.i(TAG, "fetching device data...")
            if(shouldFetchRemoteOwnerData(context)) {
                fetchRemoteOwnerData(context)
            } else {
                fetchLocalOwnerData(context)
            }
        }
    }

    private fun fetchLocalOwnerData(context: Context){
        Log.i(TAG, "fetching local owner...")
        val db: AppDatabase = AppDatabase.getDatabase(context)
        val androidId = Utilities.getAndroidId(context)
        db.deviceDao().getByDevice(androidId).let {
            device.postValue(it)
        }
    }

    private fun shouldFetchRemoteOwnerData(context: Context): Boolean{
        Log.i(TAG, "should fetch remote owner data?")
        val db = AppDatabase.getDatabase(context)
        val history = db.fetchedHistoryDao().getByTag(HistoryType.DEVICES.toString())
        Log.i(TAG, "local owner history: $history")
        val androidId = Utilities.getAndroidId(context)
        val count = db.deviceDao().countAllByDevice(androidId)
        Log.i(TAG, "local owner count: $count")
        val isFromToday = DateUtils.isToday(history?.lastUpdate ?: 0)
        Log.i(TAG, "local owner is from today: $isFromToday")
        return !isFromToday || (isFromToday && count == 0)
    }

    private fun fetchRemoteOwnerData(context: Context){
        val client = MyClient.getInstance(context).create(MyDataService::class.java)
        val androidId = Utilities.getAndroidId(context)
        Log.i(TAG, "fetching remote device data for devide $androidId...")
        val url = "obtenerDevice/${androidId}"
        val call = client.getDevice(url).execute()
        val response = call.body()
        Log.i(TAG, "respuesta fetching device: $response")
        response?.let {
            val db: AppDatabase = AppDatabase.getDatabase(context)
            db.fetchedHistoryDao().insert(FetchedDataHistory(
                tag = HistoryType.DEVICES.toString(),
                lastUpdate = System.currentTimeMillis()
            ))
            db.deviceDao().insert(it)
            device.postValue(it)
        }
    }
    // end device fetching
}