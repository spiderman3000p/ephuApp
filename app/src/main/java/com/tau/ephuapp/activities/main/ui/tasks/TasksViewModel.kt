package com.tau.ephuapp.activities.main.ui.tasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.tau.ephuapp.classes.Constants
import com.tau.ephuapp.models.Item
import com.tau.ephuapp.models.Task
import com.tau.ephuapp.models.Location
import com.tau.ephuapp.repositories.TasksRepository

class TasksViewModel(application: Application): AndroidViewModel(application) {
    val repository = TasksRepository()
    val tasksList: LiveData<ArrayList<Task>?> = repository.getTasks()
    val currentLocation: LiveData<Location?> = repository.getCurrentLocation()
    //val currentLocationCounts: LiveData<ArrayList<ItemCount>> = repository.getCurrentLocationCounts()
    val currentTaskLocations: LiveData<ArrayList<Location>?> = repository.getCurrentTaskLocations()
    val currentTask: LiveData<Task?> = repository.getCurrentTask()
    val currentItem: LiveData<Item?> = repository.getCurrentItem()
    var savingCountsWorkProgress: LiveData<List<WorkInfo>> = WorkManager.getInstance(application).getWorkInfosByTagLiveData(Constants.SAVING_COUNTS_PROGRESS)
    var savingEditCountWorkProgress: LiveData<List<WorkInfo>> = WorkManager.getInstance(application).getWorkInfosByTagLiveData(Constants.SAVING_EDIT_COUNT_PROGRESS)
    var changingTaskStateWorkProgress: LiveData<List<WorkInfo>> = WorkManager.getInstance(application).getWorkInfosByTagLiveData(Constants.CHANGIN_TASK_STATUS_PROGRESS)
}