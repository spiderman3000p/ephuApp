package com.tau.ephuapp.activities.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.tau.ephuapp.models.Device
import com.tau.ephuapp.repositories.MainRepository

class MainActivityViewModel(): ViewModel(){
    val repository = MainRepository()
    val device: LiveData<Device?> = repository.getDevice()
}