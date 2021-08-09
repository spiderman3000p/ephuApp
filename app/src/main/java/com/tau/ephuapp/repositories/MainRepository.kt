package com.tau.ephuapp.repositories

import android.content.Context
import android.text.format.DateUtils
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tau.ephuapp.R
import com.tau.ephuapp.classes.Utilities
import com.tau.ephuapp.database.AppDatabase
import com.tau.ephuapp.models.*
import com.tau.ephuapp.services.MyClient
import com.tau.ephuapp.services.MyDataService
import org.jetbrains.anko.doAsync
import org.joda.time.DateTime
import java.lang.Exception

class MainRepository {
    private val TAG = "MAIN_REPOSITORY"
    private var device = MutableLiveData<Device?>()
    private var tasksList = MutableLiveData<ArrayList<Task>?>()
    private var currentLocation = MutableLiveData<Location?>()
    private var currentItem = MutableLiveData<Item?>()
    private var currentTaskLocations = MutableLiveData<ArrayList<Location>?>()
    private var currentLocationCounts = MutableLiveData<ArrayList<ItemCount>>()
    private var currentLocationRecountTasks = MutableLiveData<ArrayList<ItemCountTask>>()
    private var currentTask = MutableLiveData<Task?>()
    private var itemsLoaded = MutableLiveData<Boolean?>()
    private var isSavingCounts = MutableLiveData<Boolean?>()

    fun getDevice(): LiveData<Device?> {
        return device
    }

    // device fetching ...
    fun fetchOwnerData(context: Context, forceRemote: Boolean = false){
        doAsync {
            Log.i(TAG, "fetching device data...")
            if(forceRemote || shouldFetchRemoteOwnerData(context)) {
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

    fun getTasks(): LiveData<ArrayList<Task>?> {
        return tasksList
    }

    fun setTasks(tasks: ArrayList<Task>) {
        tasksList.postValue(tasks)
    }

    fun getCurrentLocation(): LiveData<Location?> {
        return currentLocation
    }

    fun getCurrentItem(): LiveData<Item?> {
        return currentItem
    }

    fun getCurrentTask(): LiveData<Task?> {
        return currentTask
    }

    fun getItemsLoaded(): LiveData<Boolean?> {
        return itemsLoaded
    }

    fun getIsSavingCounts(): LiveData<Boolean?> {
        return isSavingCounts
    }

    fun setCurrentTask(task: Task?) {
        currentTask.postValue(task)
    }

    fun setCurrentLocation(location: Location?) {
        currentLocation.postValue(location)
    }

    fun setCurrentLocationCounts(counts: ArrayList<ItemCount>) {
        currentLocationCounts.postValue(counts)
    }

    fun setCurrentLocationRecountTasks(tasks: ArrayList<ItemCountTask>) {
        currentLocationRecountTasks.postValue(tasks)
    }

    fun setIsSavingCounts(value: Boolean?) {
        isSavingCounts.postValue(value)
    }

    fun addCountToCurrentLocationCounts(count: ItemCount) {
        currentLocationCounts.value?.add(count)
        currentLocationCounts.postValue(currentLocationCounts.value)
    }

    fun setCurrentItem(item: Item?) {
        currentItem.postValue(item)
    }

    fun getCurrentTaskLocations(): LiveData<ArrayList<Location>?> {
        return currentTaskLocations
    }

    fun setCurrentTaskLocations(locations: ArrayList<Location>?) {
        currentTaskLocations.setValue(locations)
    }

    fun getCurrentLocationCounts(): LiveData<ArrayList<ItemCount>> {
        return currentLocationCounts
    }

    fun getCurrentLocationRecountTasks(): LiveData<ArrayList<ItemCountTask>> {
        return currentLocationRecountTasks
    }

    // tasks fetching ...
    fun fetchTasksList(context: Context, forceRemote: Boolean = false){
        doAsync {
            Log.i(TAG, "fetching tasks...")
            if (forceRemote || shouldFetchRemoteTasksList(context)) {
                fetchRemoteTasksList(context)
            } else {
                fetchLocalTasksList(context)
            }
        }
    }

    private fun shouldFetchRemoteTasksList(context: Context): Boolean{
        Log.i(TAG, "should fetch remote tasks?")
        val db = AppDatabase.getDatabase(context)
        val history = db.fetchedHistoryDao().getByTag(HistoryType.TASKS.toString())
        Log.i(TAG, "local tasks history: $history")
        val androidId = Utilities.getAndroidId(context)
        val count = db.tasksDao().countAllByDevice(androidId)
        Log.i(TAG, "local tasks count: $count")
        val isFromToday = DateUtils.isToday(history?.lastUpdate ?: 0)
        Log.i(TAG, "local tasks is from today: $isFromToday")
        return !isFromToday || (isFromToday && count == 0)
    }

    private fun fetchLocalTasksList(context: Context){
        Log.i(TAG, "fetching local tasks...")
        val db: AppDatabase = AppDatabase.getDatabase(context)
        val androidId = Utilities.getAndroidId(context)
        db.tasksDao().getAllByDevice(androidId).let {
            tasksList.postValue(ArrayList(it))
        }
    }

    private fun fetchRemoteTasksList(context: Context){
        Log.i(TAG, "fetching remote tasks...")
        val client = MyClient.getInstance(context).create(MyDataService::class.java)
        val androidId = Utilities.getAndroidId(context)
        val url = "obtenerTareas/${androidId}"
        try {
            val call = client.getTasks(url).execute()
            val response = call.body()
            Log.i(TAG, "respuesta fetching tasks: $response")
            response?.let { tasks ->
                val db: AppDatabase = AppDatabase.getDatabase(context)
                db.fetchedHistoryDao().insert(FetchedDataHistory(
                    tag = HistoryType.TASKS.toString(),
                    lastUpdate = System.currentTimeMillis()
                ))
                db.tasksDao().deleteAll()
                db.tasksParameterDao().deleteAll()
                db.taskLocationsDao().deleteAll()
                db.taskLocationsDao().deleteAll()
                db.itemCountDao().deleteAll()
                if(tasks.isNotEmpty()) {
                    db.tasksDao().insertAll(tasks)
                    val parameters = tasks.flatMap { task ->
                        task.parameters?.map { parameter ->
                            parameter.taskId = task.id
                            parameter
                        } ?: listOf()
                    }
                    if (!parameters.isNullOrEmpty()) {
                        db.tasksParameterDao().insertAll(parameters)
                    }
                    // obtener ubicaciones de las tareas
                    tasks.forEach {task ->
                        if(task.taskType == TaskType.Inventory) {
                            fetchRemoteTaskLocations(context, task.id)
                        } else if(task.taskType == TaskType.Recount) {
                            fetchRemoteTaskLocationsRecount(context, task.id)
                        }
                        fetchRemoteTaskCounts(context, task.id)
                    }
                }
                tasksList.postValue(tasks)
            }
        } catch (e: Exception){
            e.printStackTrace()
            Log.e(TAG, "error al obtener tareas  remotas ${e.message}")
            Utilities.showAlert(context, context.getString(R.string.error), context.getString(R.string.error_fetching_remote_tasks))
            tasksList.postValue(null)
        }
    }
    // end tasks fetching

    // task locations fetching
    fun fetchTaskLocations(context: Context, taskId: Int, forceRemote: Boolean = false){
        doAsync {
            Log.i(TAG, "fetching task locations for task $taskId...")
            if (forceRemote || shouldFetchRemoteTaskLocations(context, taskId)) {
                fetchRemoteTaskLocations(context, taskId)
            } else {
                fetchLocalTaskLocations(context, taskId)
            }
        }
    }

    private fun shouldFetchRemoteTaskLocations(context: Context, taskId: Int): Boolean{
        Log.i(TAG, "should fetch remote task locations for task $taskId?")
        val db = AppDatabase.getDatabase(context)
        val history = db.fetchedHistoryDao().getByTag(HistoryType.LOCATIONS.toString().plus("-${taskId}"))
        Log.i(TAG, "local locations history: $history")
        val count = db.taskLocationsDao().countAllByTask(taskId)
        Log.i(TAG, "local locations count: $count")
        val isFromToday = DateUtils.isToday(history?.lastUpdate ?: 0)
        Log.i(TAG, "local locations is from today: $isFromToday")
        return !isFromToday || (isFromToday && count == 0)
    }

    private fun fetchLocalTaskLocations(context: Context, taskId: Int){
        Log.i(TAG, "fetching local task locations of $taskId...")
        val db: AppDatabase = AppDatabase.getDatabase(context)
        db.taskLocationsDao().getAllByTask(taskId).let {
            currentTaskLocations.postValue(ArrayList(it))
        }
    }

    private fun fetchRemoteTaskLocations(context: Context, taskId: Int){
        Log.i(TAG, "fetching remote task locations for task $taskId...")
        val client = MyClient.getInstance(context).create(MyDataService::class.java)
        val url = "obtenerUbicaciones/${taskId}"
        try {
            val call = client.getTaskLines(url).execute()
            val response = call.body()
            Log.i(TAG, "respuesta fetching task locations: $response")
            response?.let { locations ->
                val db: AppDatabase = AppDatabase.getDatabase(context)
                db.fetchedHistoryDao().insert(FetchedDataHistory(
                    tag = HistoryType.LOCATIONS.toString().plus("-${taskId}"),
                    lastUpdate = System.currentTimeMillis()
                ))
                db.taskLocationsDao().deleteAllByTask(taskId)
                // TODO: Eliminar en el futuro. Aqui se inyecta manualmente el id de la tarea relacionada, esto relentiza
                locations.forEach { location ->
                    location.taskId = taskId
                }
                if(locations.isNotEmpty()) {
                    db.taskLocationsDao().insertAll(locations)
                }
                val orderedLocationsList = db.taskLocationsDao().getAllByTask(taskId)
                val orderedLocationsArrayList = arrayListOf<Location>()
                orderedLocationsArrayList.addAll(orderedLocationsList)
                currentTaskLocations.postValue(orderedLocationsArrayList)
            }
        } catch (e: Exception){
            e.printStackTrace()
            Log.e(TAG, "error al obtener ubicaciones del servidor ${e.message}")
            Utilities.showAlert(context, context.getString(R.string.error), context.getString(R.string.error_fetching_remote_locations))
            currentTaskLocations.postValue(null)
        }
    }
    // end task locations fetching

    // fetch task locations recount
    fun fetchTaskLocationsRecount(context: Context, taskId: Int, forceRemote: Boolean = false){
        doAsync {
            Log.i(TAG, "fetching task locations recount for task $taskId...")
            if (forceRemote || shouldFetchRemoteTaskLocationsRecount(context, taskId)) {
                fetchRemoteTaskLocationsRecount(context, taskId)
            } else {
                fetchLocalTaskLocationsRecount(context, taskId)
            }
        }
    }

    private fun shouldFetchRemoteTaskLocationsRecount(context: Context, taskId: Int): Boolean{
        Log.i(TAG, "should fetch remote task locations recount for task $taskId?")
        val db = AppDatabase.getDatabase(context)
        val history = db.fetchedHistoryDao().getByTag(HistoryType.LOCATIONS_RECOUNT.toString().plus("-${taskId}"))
        Log.i(TAG, "local locations recount history: $history")
        val count = db.taskLocationsDao().countAllRecountByTask(taskId)
        Log.i(TAG, "local locations recount count: $count")
        val isFromToday = DateUtils.isToday(history?.lastUpdate ?: 0)
        Log.i(TAG, "local locations recount is from today: $isFromToday")
        return !isFromToday || (isFromToday && count == 0)
    }

    private fun fetchLocalTaskLocationsRecount(context: Context, taskId: Int){
        Log.i(TAG, "fetching local task locations recount of $taskId...")
        val db: AppDatabase = AppDatabase.getDatabase(context)
        db.taskLocationsDao().getAllRecountByTask(taskId).let {
            currentTaskLocations.postValue(ArrayList(it))
        }
    }

    private fun fetchRemoteTaskLocationsRecount(context: Context, taskId: Int){
        Log.i(TAG, "fetching remote task locations recount for task $taskId...")
        val client = MyClient.getInstance(context).create(MyDataService::class.java)
        val url = "obtenerUbicacionesReconteo/${taskId}"
        try {
            val call = client.getTaskLines(url).execute()
            val response = call.body()
            Log.i(TAG, "respuesta fetching task locations recounts: $response")
            response?.let { locations ->
                val db: AppDatabase = AppDatabase.getDatabase(context)
                db.fetchedHistoryDao().insert(FetchedDataHistory(
                    tag = HistoryType.LOCATIONS_RECOUNT.toString().plus("-${taskId}"),
                    lastUpdate = System.currentTimeMillis()
                ))
                db.taskLocationsDao().deleteAllRecountByTask(taskId)
                // TODO: Eliminar en el futuro. Aqui se inyecta manualmente el id de la tarea relacionada, esto relentiza
                locations.forEach { location ->
                    location.taskId = taskId
                    location.id = location.locationId!!
                }
                if(locations.isNotEmpty()) {
                    db.taskLocationsDao().insertAll(locations)
                }
                val orderedLocationsList = db.taskLocationsDao().getAllRecountByTask(taskId)
                val orderedLocationsArrayList = arrayListOf<Location>()
                orderedLocationsArrayList.addAll(orderedLocationsList)
                currentTaskLocations.postValue(orderedLocationsArrayList)
            }
        } catch (e: Exception){
            Log.e(TAG, "error al obtener ubicaciones de reconteo del servidor ${e.message}")
            Utilities.showAlert(context, context.getString(R.string.error), context.getString(R.string.error_fetching_remote_locations))
            currentTaskLocations.postValue(null)
        }
    }
    // end task locations recount fetching

    private fun shouldFetchRemoteTaskCounts(context: Context, taskId: Int): Boolean{
        Log.i(TAG, "should fetch remote task counts for task $taskId?")
        val db = AppDatabase.getDatabase(context)
        val history = db.fetchedHistoryDao().getByTag(HistoryType.TASK_COUNTS.toString().plus("-${taskId}"))
        Log.i(TAG, "local task counts history: $history")
        val countsCount = db.itemCountDao().countAllByTask(taskId)
        Log.i(TAG, "local task counts count: $countsCount")
        val isFromToday = DateUtils.isToday(history?.lastUpdate ?: 0)
        Log.i(TAG, "local locations recount is from today: $isFromToday")
        return !isFromToday || (isFromToday && countsCount == 0)
    }

    private fun fetchRemoteTaskCounts(context: Context, taskId: Int){
        Log.i(TAG, "fetching remote task locations for task $taskId...")
        val client = MyClient.getInstance(context).create(MyDataService::class.java)
        val url = "obtenerConteos/${taskId}"
        try {
            val call = client.getTaskCounts(url).execute()
            val response = call.body()
            Log.i(TAG, "respuesta fetching task counts: $response")
            response?.let { counts ->
                val db: AppDatabase = AppDatabase.getDatabase(context)
                db.fetchedHistoryDao().insert(FetchedDataHistory(
                    tag = HistoryType.COUNTS.toString().plus("-${taskId}"),
                    lastUpdate = System.currentTimeMillis()
                ))
                db.itemCountDao().deleteAllByTaskId(taskId)
                // TODO: Eliminar en el futuro. Aqui se inyecta manualmente el id de la tarea relacionada, esto relentiza
                counts.forEach { count ->
                    var item: Item? = null
                    count.itemId?.let{itemId ->
                        item = db.itemDao().getById(itemId)
                        item?.let {
                            count.sku = it.sku
                            count.description = it.description
                        }
                    }
                    count.lastUpdateTimestamp = count.lastUpdateTimestamp ?: DateTime.now().millis
                    count.taskId = taskId
                    count.uploaded = true
                    count.dirty = false
                    count.sent = false
                    count.editing = false
                }
                if(counts.isNotEmpty()) {
                    db.itemCountDao().insertAll(counts)
                }
            }
        } catch (e: Exception){
            Log.e(TAG, "error al obtener conteos del servidor ${e.message}")
            Utilities.showAlert(context, context.getString(R.string.error), context.getString(R.string.error_fetching_remote_counts))
            currentTaskLocations.postValue(null)
        }
    }
    // end task counts fetching

    // fetch items
    fun fetchItems(context: Context, ownerId: Int, forceRemote: Boolean = false){
        doAsync {
            Log.i(TAG, "fetching items...")
            if (forceRemote || shouldFetchRemoteItems(context, ownerId)) {
                fetchRemoteItems(context, ownerId)
            } else {
                fetchLocalItems(context)
            }
        }
    }

    private fun shouldFetchRemoteItems(context: Context, ownerId: Int): Boolean{
        Log.i(TAG, "should fetch remote items?")
        val db = AppDatabase.getDatabase(context)
        val history = db.fetchedHistoryDao().getByTag(HistoryType.ITEMS.toString())
        Log.i(TAG, "local items history: $history")
        val count = db.itemDao().countAllByOwner(ownerId)
        Log.i(TAG, "local items count: $count")
        val isFromToday = DateUtils.isToday(history?.lastUpdate ?: 0)
        Log.i(TAG, "local items are from today: $isFromToday")
        return !isFromToday || (isFromToday && count == 0)
    }

    private fun fetchLocalItems(context: Context){
        Log.i(TAG, "fetching local items...")
        val db: AppDatabase = AppDatabase.getDatabase(context)
        db.itemDao().getAll().let {
            itemsLoaded.postValue(true)
        }
    }

    private fun fetchRemoteItems(context: Context, ownerId: Int){
        Log.i(TAG, "fetching all remote items...")
        val client = MyClient.getInstance(context).create(MyDataService::class.java)
        val url = "obtenerItems/$ownerId"
        doAsync {
            try {
                val call = client.getItems(url).execute()
                if(call.code() == 400 || call.code() == 500 || call.code() == 403 || call.code() == 404 || call.code() == 502){
                    Log.e(TAG, "error al obtener items remotos. codigo: ${call.code()}")
                    Utilities.showAlert(context, context.getString(R.string.error), context.getString(
                        R.string.error_fetching_remote_items))
                    itemsLoaded.postValue(null)
                } else if (call.code() == 200 || call.code() == 201){
                    val response = call.body()
                    Log.i(TAG, "respuesta owner $ownerId items: $response")
                    response?.let { items ->
                        val db: AppDatabase = AppDatabase.getDatabase(context)
                        db.fetchedHistoryDao().insert(
                            FetchedDataHistory(
                                tag = HistoryType.ITEMS.toString(),
                                lastUpdate = System.currentTimeMillis()
                            )
                        )
                        db.itemDao().deleteAll()
                        if(items.isNotEmpty()) {
                            db.itemDao().insertAll(items)
                        }
                        itemsLoaded.postValue(true)
                    }
                }
            } catch (e: Exception){
                Log.e(TAG, "error al obtener items remotos ${e.message}")
                Utilities.showAlert(context, context.getString(R.string.error), context.getString(R.string.error_fetching_remote_items))
                itemsLoaded.postValue(null)
            }
        }
    }
}