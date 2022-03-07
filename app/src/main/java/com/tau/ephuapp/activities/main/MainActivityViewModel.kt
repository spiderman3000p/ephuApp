package com.tau.ephuapp.activities.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.tau.ephuapp.classes.Constants
import com.tau.ephuapp.models.*
import com.tau.ephuapp.repositories.MainRepository

class MainActivityViewModel(application: Application): AndroidViewModel(application){
    val repository = MainRepository()
    val tasksList: LiveData<ArrayList<Task>?> = repository.getTasks()
    val currentLocation: LiveData<Location?> = repository.getCurrentLocation()
    //val filteredCounts = MutableLiveData<List<ItemCount>>()
    val filterCountsInput = MutableLiveData<String>()
    val currentTaskLocations: LiveData<ArrayList<Location>?> = repository.getCurrentTaskLocations()
    val currentTask: LiveData<Task?> = repository.getCurrentTask()
    val currentItem: LiveData<Item?> = repository.getCurrentItem()
    var savingCountsWorkProgress: LiveData<List<WorkInfo>> = WorkManager.getInstance(application).getWorkInfosByTagLiveData(
        Constants.SAVING_COUNTS_PROGRESS)
    val savingEditCountWorkProgress: LiveData<List<WorkInfo>> = WorkManager.getInstance(application).getWorkInfosByTagLiveData(
        Constants.SAVING_EDIT_COUNT_PROGRESS)
    val changingTaskStateWorkProgress: LiveData<List<WorkInfo>> = WorkManager.getInstance(application).getWorkInfosByTagLiveData(
        Constants.CHANGIN_TASK_STATUS_PROGRESS)
    val device: LiveData<Device?> = repository.getDevice()
    // certificacion
    val currentCertificationTaskItems: LiveData<ArrayList<CertificationTaskItem>?> = repository.getCurrentCertificationTaskItems()
    val pendingCertificationTaskItems: LiveData<ArrayList<CertificationTaskItem>?> = repository.getPendingCertificationTaskItems()
    val certifiedItems: LiveData<ArrayList<Certification>?> = repository.getCertifiedItems()
}