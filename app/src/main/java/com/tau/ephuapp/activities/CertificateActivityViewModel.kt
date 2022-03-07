package com.tau.ephuapp.activities

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

class CertificateActivityViewModel(application: Application): AndroidViewModel(application){
    val task: MutableLiveData<Task?> = MutableLiveData<Task?>()
    val currentCertificationTaskItems: MutableLiveData<ArrayList<CertificationTaskItem>?> = MutableLiveData<ArrayList<CertificationTaskItem>?>()
    val pendingCertificationTaskItems: MutableLiveData<ArrayList<CertificationTaskItem>?> = MutableLiveData<ArrayList<CertificationTaskItem>?>()
    val certifiedItems: MutableLiveData<ArrayList<Certification>?> = MutableLiveData<ArrayList<Certification>?>()
    var savingCertificationWorkProgress: LiveData<List<WorkInfo>> = WorkManager.getInstance(application).getWorkInfosByTagLiveData(
            Constants.SAVING_CERTIFICATION)
}