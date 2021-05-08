package com.tau.ephuapp.dialogs

import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.database.sqlite.SQLiteAccessPermException
import android.database.sqlite.SQLiteCantOpenDatabaseException
import android.database.sqlite.SQLiteDatabaseLockedException
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateUtils
import android.util.Log
import android.view.*
import android.view.View.OnTouchListener
import android.view.inputmethod.EditorInfo
import android.widget.CompoundButton
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getColor
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.WorkInfo
import androidx.work.hasKeyWithValueOfType
import com.tau.ephuapp.R
import com.tau.ephuapp.activities.BarcodeScannerActivity
import com.tau.ephuapp.activities.main.ui.tasks.TasksViewModel
import com.tau.ephuapp.adapters.CountAdapter
import com.tau.ephuapp.classes.Constants
import com.tau.ephuapp.classes.Utilities
import com.tau.ephuapp.classes.Utilities.Companion.showAlert
import com.tau.ephuapp.classes.Utilities.Companion.showToast
import com.tau.ephuapp.database.AppDatabase
import com.tau.ephuapp.databinding.FragmentTaskCountBinding
import com.tau.ephuapp.models.*
import com.tau.ephuapp.services.MyWorkerManagerService
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.joda.time.DateTime
import java.text.SimpleDateFormat
import java.util.*


class TaskCountDialog(var task: Task?) : DialogFragment() {
    private val isEmptyLocations = mutableListOf<Int>()
    private var isEditing: Int = -1
    private var _binding: FragmentTaskCountBinding? = null
    private val TAG = "TASK_COUNT_DIALOG"
    private val BARCODE_SCANNER = 99
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding
    private lateinit var viewModel: TasksViewModel
    private val taskLocations = arrayListOf<Location>()
    private val currentLocationCounts = arrayListOf<ItemCount>()
    private var currentLocationPosition: Int = 0
    private var totalPendingCounts = 0
    private var totalCounts = 0
    private var mAdapter: CountAdapter? = null
    private lateinit var db: AppDatabase
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme_FullScreenDialog)
        val _viewModel: TasksViewModel by activityViewModels()
        viewModel = _viewModel
        try {
            db = AppDatabase.getDatabase(requireContext())
        } catch (ex: SQLiteDatabaseLockedException) {
            Log.e(TAG, "Database error found", ex)
        } catch (ex: SQLiteAccessPermException) {
            Log.e(TAG, "Database error found", ex)
        } catch (ex: SQLiteCantOpenDatabaseException) {
            Log.e(TAG, "Database error found", ex)
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog: Dialog? = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.getWindow()?.setLayout(width, height)
            dialog.getWindow()?.setWindowAnimations(R.style.AppThemeSlide);
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?,
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentTaskCountBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.toolbar?.setNavigationOnClickListener { v: View? -> dismiss() }
        binding?.toolbar?.setTitleTextColor(getColor(requireContext(), R.color.white))
        // Inflate a menu to be displayed in the toolbar
        binding?.toolbar?.inflateMenu(R.menu.main)
        // Set an OnMenuItemClickListener to handle menu item clicks
        binding?.toolbar?.setOnMenuItemClickListener {
            onOptionsItemSelected(it)
        }
        hideUi()
        binding?.descriptionEt?.keyListener = null // readonly
        binding?.countsLabelTv?.text = getString(R.string.counts, currentLocationCounts.size)
        binding?.totalCountsTv?.text = getString(R.string.total_counts, totalCounts)
        binding?.doneBtn?.text = getString(R.string.save_pending_counts, totalPendingCounts)
        val twCreatedDate: TextWatcher = object: TextWatcher {
            var current: String = "";
            var ddmmyyyy: String = "DDMMYYYY";
            var cal: Calendar = Calendar.getInstance();
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.toString().equals(current)) {
                    var clean: String = s.toString().replace("[^\\d.]|\\.".toRegex(), "")
                    val cleanC = current.replace("[^\\d.]|\\.".toRegex(), "")
                    val cl = clean.length
                    var sel = cl
                    var i = 2
                    while (i <= cl && i < 6) {
                        sel++
                        i += 2
                    }
                    //Fix for pressing delete next to a forward slash
                    if (clean == cleanC) sel--
                    if (clean.length < 8) {
                        clean = clean + ddmmyyyy.substring(clean.length)
                    } else {
                        //This part makes sure that when we finish entering numbers
                        //the date is correct, fixing it otherwise
                        var day = clean.substring(0, 2).toInt()
                        var mon = clean.substring(2, 4).toInt()
                        var year = clean.substring(4, 8).toInt()
                        mon = if (mon < 1) 1 else if (mon > 12) 12 else mon
                        cal[Calendar.MONTH] = mon - 1
                        year = if (year < 1900) 1900 else if (year > 2100) 2100 else year
                        cal[Calendar.YEAR] = year
                        // ^ first set year for the line below to work correctly
                        //with leap years - otherwise, date e.g. 29/02/2012
                        //would be automatically corrected to 28/02/2012
                        day = if (day > cal.getActualMaximum(Calendar.DATE)) cal.getActualMaximum(Calendar.DATE) else day
                        clean = String.format("%02d%02d%02d", day, mon, year)
                    }
                    clean = String.format("%s/%s/%s", clean.substring(0, 2),
                            clean.substring(2, 4),
                            clean.substring(4, 8))
                    sel = if (sel < 0) 0 else sel
                    current = clean
                    binding?.createdDateEt?.setText(current)
                    binding?.createdDateEt?.setSelection(if (sel < current.length) sel else current.length)
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }
        }
        val twExpiryDate: TextWatcher = object: TextWatcher {
            var current: String = "";
            var ddmmyyyy: String = "DDMMYYYY";
            var cal: Calendar = Calendar.getInstance();
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.toString().equals(current)) {
                    var clean: String = s.toString().replace("[^\\d.]|\\.".toRegex(), "")
                    val cleanC = current.replace("[^\\d.]|\\.".toRegex(), "")
                    val cl = clean.length
                    var sel = cl
                    var i = 2
                    while (i <= cl && i < 6) {
                        sel++
                        i += 2
                    }
                    //Fix for pressing delete next to a forward slash
                    if (clean == cleanC) sel--
                    if (clean.length < 8) {
                        clean = clean + ddmmyyyy.substring(clean.length)
                    } else {
                        //This part makes sure that when we finish entering numbers
                        //the date is correct, fixing it otherwise
                        var day = clean.substring(0, 2).toInt()
                        var mon = clean.substring(2, 4).toInt()
                        var year = clean.substring(4, 8).toInt()
                        mon = if (mon < 1) 1 else if (mon > 12) 12 else mon
                        cal[Calendar.MONTH] = mon - 1
                        year = if (year < 1900) 1900 else if (year > 2100) 2100 else year
                        cal[Calendar.YEAR] = year
                        // ^ first set year for the line below to work correctly
                        //with leap years - otherwise, date e.g. 29/02/2012
                        //would be automatically corrected to 28/02/2012
                        day = if (day > cal.getActualMaximum(Calendar.DATE)) cal.getActualMaximum(Calendar.DATE) else day
                        clean = String.format("%02d%02d%02d", day, mon, year)
                    }
                    clean = String.format("%s/%s/%s", clean.substring(0, 2),
                            clean.substring(2, 4),
                            clean.substring(4, 8))
                    sel = if (sel < 0) 0 else sel
                    current = clean
                    binding?.expiryDateEt?.setText(current)
                    binding?.expiryDateEt?.setSelection(if (sel < current.length) sel else current.length)
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }
        }
        binding?.createdDateEt?.addTextChangedListener(twCreatedDate)
        binding?.expiryDateEt?.addTextChangedListener(twExpiryDate)
        binding?.rightBtn?.setOnClickListener {
            Log.i(TAG, "flecha derecha presionada...")
            Log.i(
                    TAG,
                    "ubicaciones: ${taskLocations.size} posicion siguiente: ${(currentLocationPosition + 1)}"
            )
            if (taskLocations.size > (currentLocationPosition + 1)){
                currentLocationPosition += 1
                viewModel.repository.setCurrentLocation(taskLocations[currentLocationPosition])
                viewModel.repository.setCurrentItem(null)
            } else {
                showToast(requireContext(), getString(R.string.not_more_locations))
            }
        }
        binding?.leftBtn?.setOnClickListener {
            Log.i(TAG, "flecha izquierda presionada...")
            Log.i(
                    TAG,
                    "ubicaciones: ${taskLocations.size} posicion siguiente: ${(currentLocationPosition - 1)}"
            )
            if (currentLocationPosition > 0 && taskLocations.size > (currentLocationPosition - 1)){
                currentLocationPosition -= 1
                viewModel.repository.setCurrentLocation(taskLocations[currentLocationPosition])
                viewModel.repository.setCurrentItem(null)
            } else {
                showToast(requireContext(), getString(R.string.not_more_locations))
            }
        }
        binding?.saveBtn?.setOnClickListener {
            val item = viewModel.currentItem.value
            val location = viewModel.currentLocation.value
            val quantity = if(binding?.quantityEt?.text?.isNotEmpty() == true) {
                binding?.quantityEt?.text?.toString()?.toInt() ?: 0
            } else {
                0
            }
            if(item != null && location != null && quantity > 0 && hasValidParams()){
                if (isEditing == -1) {
                    showAlert(
                            requireContext(), getString(R.string.confirmation), getString(
                            R.string.save_count_confirm_msg,
                            quantity,
                            item.description
                    ), this::saveCount
                    )
                } else {
                    showAlert(
                            requireContext(), getString(R.string.confirmation), getString(
                            R.string.edit_count_confirmation_msg), this::saveCount
                    )
                }
            } else {
                showToast(requireContext(), getString(R.string.invalid_item_count))
            }
        }
        binding?.skuEt?.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH){
                if(v.text.isNotEmpty()){
                    searchItemBySku(v.text.toString())
                }
            }
            true
        }
        binding?.skuEt?.setOnTouchListener(OnTouchListener { v, event ->
            val DRAWABLE_LEFT = 0
            val DRAWABLE_TOP = 1
            val DRAWABLE_RIGHT = 2
            val DRAWABLE_BOTTOM = 3
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (binding?.skuEt?.getRight() ?: 0) - (binding?.skuEt?.getCompoundDrawables()?.get(DRAWABLE_RIGHT)?.bounds?.width() ?: 0)) {
                    startActivityForResult(Intent(requireContext(), BarcodeScannerActivity::class.java), BARCODE_SCANNER)
                    //ZxingOrient(this).initiateScan()
                    return@OnTouchListener true
                }
            }
            false
        })
        binding?.quantityEt?.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE){
                if(binding?.skuEt?.text?.isNotEmpty() == true){
                    searchItemBySku(binding?.skuEt?.text?.toString() ?: "")
                }
            }
            true
        }
        binding?.doneBtn?.setOnClickListener {
            showAlert(requireContext(), getString(R.string.confirmation), getString(R.string.save_all_counts_confirm_msg), this::saveAllCounts)
            //saveAllCounts()
        }
        /*binding?.emptySw?.setOnCheckedChangeListener { buttonView, isChecked ->
            Log.i(TAG, "on changed empty slide toogle. is checked: $isChecked")
            if(isChecked) {
                Utilities.showAlert(requireContext(), getString(R.string.confirmation), getString(R.string.set_location_empty_confirm_msg), {
                    viewModel.currentLocation.value?.let {
                        toggleLocationAsEmpty(it.id)
                    }
                }, {
                    binding?.emptySw?.isChecked = false
                })
            } else {
                Utilities.showAlert(requireContext(), getString(R.string.confirmation), getString(R.string.set_location_empty_confirm_msg), {
                    viewModel.currentLocation.value?.let{
                        toggleLocationAsEmpty(it.id)
                    }
                }, {
                    binding?.emptySw?.isChecked = true
                })
            }
        }*/
        viewModel.currentLocation.observe(viewLifecycleOwner, Observer { location ->
            Log.i(TAG, "current location observed $location")
            if (location != null) {
                loadCurrentLocationCounts(location.id)
                binding?.locationCodeTv?.text = location.code
                binding?.paginationTv?.text = getString(
                        R.string.location_pagination,
                        (currentLocationPosition + 1),
                        taskLocations.size
                )
                if (currentLocationPosition == taskLocations.size - 1) {
                    binding?.rightBtn?.visibility = View.GONE
                } else {
                    binding?.rightBtn?.visibility = View.VISIBLE
                }
                if (currentLocationPosition == 0) {
                    binding?.leftBtn?.visibility = View.GONE
                } else {
                    binding?.leftBtn?.visibility = View.VISIBLE
                }
            } else {
                binding?.locationCodeTv?.text = ""
                binding?.paginationTv?.text = ""
                binding?.leftBtn?.visibility = View.GONE
                binding?.rightBtn?.visibility = View.GONE
            }
        })
        viewModel.currentItem.observe(viewLifecycleOwner, Observer {
            Log.i(TAG, "current item observed $it")
            presentItem(it)
        })
        viewModel.currentTaskLocations.observe(viewLifecycleOwner, Observer {
            Log.i(TAG, "current task locations observed $it")
            taskLocations.clear()
            currentLocationPosition = 0
            when {
                it == null -> {
                    hideUi()
                    viewModel.repository.setCurrentLocation(null)
                }
                it.isNotEmpty() -> {
                    taskLocations.addAll(it)
                    viewModel.repository.setCurrentLocation(taskLocations[currentLocationPosition])
                    showUi()
                }
                it.isEmpty() -> {
                    hideUi()
                    viewModel.repository.setCurrentLocation(null)
                    Utilities.showAlert(
                            requireContext(),
                            getString(R.string.information),
                            getString(R.string.empy_or_null_task_lines)
                    )
                }
            }
        })
        viewModel.currentTask.observe(viewLifecycleOwner, {
            Log.i(TAG, "task observada: $it")
            task = it
            resetFormData()
            viewModel.repository.setCurrentTaskLocations(null)// provisional mientras carga
            binding?.toolbar?.title = getString(R.string.task_title, it?.id ?: 0, it?.count ?: 0)
            if (task != null) {
                Log.i(TAG, "Cargando locations de tarea...")
                viewModel.repository.fetchTaskLocations(requireContext(), it?.id!!)
                loadTaskParameters()
            } else {
                viewModel.repository.setCurrentTaskLocations(arrayListOf<Location>())
            }
        })
        viewModel.repository.getCurrentLocationCounts().observe(viewLifecycleOwner, {
            checkCountsTotals()
        })
        viewModel.savingCountsWorkProgress.observe(viewLifecycleOwner, {
            /*
            Este observable probablemente no corra ya que el dialogo se cierra cuando se presiona
            el boton guardar, y luego es que se hace la peticion al servidor remoto
            y aqui se observan los resultados de dicha peticion
            */
            Log.i(TAG, "progreso de subida de conteos observado...")
            it.forEach { workInfo ->
                if (WorkInfo.State.RUNNING == workInfo.state) {
                    binding?.progressBar?.visibility = View.VISIBLE
                    Utilities.showToast(requireContext(), getString(R.string.uploading_counts))
                } else {
                    binding?.progressBar?.visibility = View.INVISIBLE
                }
                if (WorkInfo.State.SUCCEEDED == workInfo.state) {
                    var msg = getString(R.string.counts_uploaded_successfully)
                    if(workInfo.outputData.hasKeyWithValueOfType("exception", String::class.java)){
                        msg = workInfo.outputData.getString("exception").toString()
                    } else if (workInfo.outputData.hasKeyWithValueOfType("error", String::class.java)) {
                        msg = workInfo.outputData.getString("error").toString()
                    } else {
                        // si todo salio bien, actualizar id's remotos
                        workInfo.outputData.keyValueMap.forEach{mapEntry ->
                            val newRemoteId = mapEntry.value as Int
                            val localId = mapEntry.key
                            currentLocationCounts.find { itemCount ->
                                itemCount.localId == localId
                            }?.apply{
                                id = newRemoteId
                                uploaded = true
                                sent = false
                            }
                        }
                    }
                    Utilities.showToast(requireContext(), msg)
                }
                if (WorkInfo.State.FAILED == workInfo.state) {
                    var msg = getString(R.string.error_uploading_counts)
                    if(workInfo.outputData.hasKeyWithValueOfType("exception", String::class.java)){
                        msg = workInfo.outputData.getString("exception").toString()
                    } else if (workInfo.outputData.hasKeyWithValueOfType("error", String::class.java)) {
                        msg = workInfo.outputData.getString("error").toString()
                    }
                    Utilities.showToast(requireContext(), msg)
                }
                if (WorkInfo.State.CANCELLED == workInfo.state) {
                    Utilities.showToast(requireContext(), getString(R.string.counts_uploading_cancelled))
                }
            }
        })
        viewModel.savingEditCountWorkProgress.observe(viewLifecycleOwner, {
            Log.i(TAG, "progreso de subida de edicion de conteo observado...")
            it.forEach { workInfo ->
                if (WorkInfo.State.RUNNING == workInfo.state) {
                    binding?.progressBar?.visibility = View.VISIBLE
                    Utilities.showToast(requireContext(), getString(R.string.uploading_count_edit))
                } else {
                    binding?.progressBar?.visibility = View.INVISIBLE
                }
                if (WorkInfo.State.SUCCEEDED == workInfo.state) {
                    var msg = getString(R.string.count_edited_successfully)
                    if(workInfo.outputData.hasKeyWithValueOfType("exception", String::class.java)){
                        msg = workInfo.outputData.getString("exception").toString()
                    } else if (workInfo.outputData.hasKeyWithValueOfType("error", String::class.java)) {
                        msg = workInfo.outputData.getString("error").toString()
                    } else {
                        // si todo salio bien, agregar conteos nuevos, si es que se generaron
                        //currentLocationCounts.forEach {itemCount ->
                        val vars = workInfo.outputData.keyValueMap
                        vars.forEach {mapEntry ->
                            val index = currentLocationCounts.indexOfFirst {count ->
                                count.localId == mapEntry.key
                            }
                            if(index > -1){
                                // si el id remoto es distinto, entonces se creo un nuevo conteo y hay
                                // que eliminar el anterior agregar el nuevo a la lista, previamente guardado por el worker
                                if(currentLocationCounts[index].id != mapEntry.value as Int) {
                                    doAsync {
                                        db.itemCountDao().getById(mapEntry.value as Int)?.let {newItemCount ->
                                            uiThread {
                                                currentLocationCounts[index] = newItemCount
                                                mAdapter?.notifyDataSetChanged()
                                            }
                                        }
                                    }
                                } else {
                                    currentLocationCounts[index].dirty = false
                                    currentLocationCounts[index].sent = false
                                    mAdapter?.notifyDataSetChanged()
                                }
                            }
                        }
                    }
                    Utilities.showToast(requireContext(), msg)
                }
                if (WorkInfo.State.FAILED == workInfo.state) {
                    var msg = getString(R.string.error_uploading_counts)
                    if(workInfo.outputData.hasKeyWithValueOfType("exception", String::class.java)){
                        msg = workInfo.outputData.getString("exception").toString()
                    } else if (workInfo.outputData.hasKeyWithValueOfType("error", String::class.java)) {
                        msg = workInfo.outputData.getString("error").toString()
                    }
                    Utilities.showToast(requireContext(), msg)
                }
                if (WorkInfo.State.CANCELLED == workInfo.state) {
                    Utilities.showToast(requireContext(), getString(R.string.counts_uploading_cancelled))
                }
            }
        })
        Log.i(TAG, "lista de tareas: ${viewModel.tasksList.value}")
        viewModel.repository.setCurrentTask(task)
        initAdapter()
    }

    private fun toggleLocationAsEmpty(locationId: Int){
        Log.i(TAG, "on toggleLocationAsEmpty. locationId: $locationId. is checked: ${binding?.emptySw?.isChecked}")
        if (!isEmptyLocations.contains(locationId) && binding?.emptySw?.isChecked == true) {
            binding?.editLy?.visibility = View.GONE
            binding?.countsLabelTv?.visibility = View.GONE
            binding?.currentLocationCountRv?.visibility = View.GONE
            binding?.totalCountsTv?.visibility = View.GONE
            binding?.doneBtn?.visibility = View.GONE
            binding?.emptyLocationTv?.visibility = View.VISIBLE
            hideParametersViews()
            isEmptyLocations.add(locationId)
        } else if (isEmptyLocations.contains(locationId) && binding?.emptySw?.isChecked == false) {
            binding?.editLy?.visibility = View.VISIBLE
            binding?.countsLabelTv?.visibility = View.VISIBLE
            binding?.currentLocationCountRv?.visibility = View.VISIBLE
            binding?.totalCountsTv?.visibility = View.VISIBLE
            binding?.doneBtn?.visibility = View.VISIBLE
            binding?.emptyLocationTv?.visibility = View.GONE
            renderTaskParameters()
            isEmptyLocations.remove(locationId)
        }
    }

    private fun hasValidParams(): Boolean{
        return task?.parameters?.all { parameter ->
            when (parameter.parameterType) {
                ParameterType.Lot -> {
                    (parameter.value && binding?.lotEt?.text?.isNotEmpty() == true) || !parameter.value
                }
                ParameterType.Lpn -> {
                    (parameter.value && binding?.lpnEt?.text?.isNotEmpty() == true) || !parameter.value
                }
                ParameterType.CreatedDate -> {
                    (parameter.value && binding?.createdDateEt?.text?.isNotEmpty() == true && isValidDate(binding?.createdDateEt?.text.toString(), "dd/MM/yyyy")) || !parameter.value
                }
                ParameterType.ExpiryDate -> {
                    (parameter.value && binding?.expiryDateEt?.text?.isNotEmpty() == true && isValidDate(binding?.expiryDateEt?.text.toString(), "dd/MM/yyyy")) || !parameter.value
                }
                ParameterType.Serial -> {
                    (parameter.value && binding?.serialEt?.text?.isNotEmpty() == true) || !parameter.value
                }
                /*ParameterType.Empty -> {
                    binding?.emptySw
                }*/
                else -> true
            }
        } ?: true
    }

    private fun hideUi(){
        binding?.frameLayout?.visibility = View.GONE
        binding?.leftBtn?.visibility = View.GONE
        binding?.rightBtn?.visibility = View.GONE
        binding?.totalCountsTv?.visibility = View.GONE
        binding?.doneBtn?.visibility = View.GONE
    }

    private fun showUi(){
        binding?.frameLayout?.visibility = View.VISIBLE
        binding?.leftBtn?.visibility = View.VISIBLE
        binding?.rightBtn?.visibility = View.VISIBLE
        binding?.totalCountsTv?.visibility = View.VISIBLE
        binding?.doneBtn?.visibility = View.VISIBLE
    }

    private fun loadTaskParameters(){
        doAsync {
            db.tasksParameterDao().getAllByTask(task?.id!!).let {
                Log.i(TAG, "parametros de la tarea ${task?.id}: $it")
                task?.initParameters(it)
                renderTaskParameters()
            }
        }
    }

    private fun renderTaskParameters(){
        if(!task?.parameters.isNullOrEmpty()) {
            activity?.runOnUiThread {
                var controlView: View? = null
                task?.parameters?.forEach { parameter ->
                    controlView = when (parameter.parameterType) {
                        ParameterType.Lot -> {
                            binding?.lotContainer
                        }
                        ParameterType.Lpn -> {
                            binding?.lpnContainer
                        }
                        ParameterType.CreatedDate -> {
                            binding?.createdDateContainer
                        }
                        ParameterType.ExpiryDate -> {
                            binding?.expiryDateContainer
                        }
                        ParameterType.Serial -> {
                            binding?.serialContainer
                        }
                        ParameterType.Empty -> {
                            binding?.emptySw
                        }
                        else -> null
                    }
                    if(parameter.value) {
                        controlView?.visibility = View.VISIBLE
                    } else {
                        controlView?.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun hideParametersViews(){
        binding?.serialContainer?.visibility = View.GONE
        binding?.expiryDateContainer?.visibility = View.GONE
        binding?.lotContainer?.visibility = View.GONE
        binding?.createdDateContainer?.visibility = View.GONE
        binding?.lpnContainer?.visibility = View.GONE
    }

    private fun checkCountsTotals(){
        doAsync {
            totalCounts = db.itemCountDao().countAllByDevice(Utilities.getAndroidId(requireContext()))
            totalPendingCounts = db.itemCountDao().countAllPendingToUploadByDevice(Utilities.getAndroidId(requireContext()))
            val pendingToUpdate = db.itemCountDao().countAllPendingToUpdateByDevice(Utilities.getAndroidId(requireContext()))
            uiThread {
                binding?.totalCountsTv?.text = getString(R.string.total_counts, totalCounts)
                binding?.doneBtn?.text = getString(R.string.save_pending_counts, totalPendingCounts + pendingToUpdate)
            }
        }
    }

    private fun saveAllCounts(){
        Log.i(TAG, "saving all counts...")
        try {
            doAsync {
                val newCountsToSave = db.itemCountDao()
                    .getAllPendingToUploadByDevice(Utilities.getAndroidId(requireContext()))
                Log.i(TAG, "Nuevos conteos por subir y crear: $newCountsToSave")
                val countsToUpdate = db.itemCountDao()
                    .getAllPendingToUpdateByDevice(Utilities.getAndroidId(requireContext()))
                Log.i(TAG, "Conteos por subir y actualizar: $newCountsToSave")
                if (!newCountsToSave.isNullOrEmpty()) {
                    viewModel.repository.setIsSavingCounts(true)
                    MyWorkerManagerService.enqueCountToUploadArrayWork(
                        requireContext(),
                        newCountsToSave,
                        Constants.SAVING_COUNTS_PROGRESS
                    )
                }
                if (!countsToUpdate.isNullOrEmpty()) {
                    countsToUpdate.forEach { itemCount ->
                        MyWorkerManagerService.enqueEditCountToUploadWork(
                            requireContext(),
                            itemCount
                        )
                    }
                }
                dismiss()
            }
        } catch (e: Exception) {
            Utilities.showAlert(requireContext(), getString(R.string.error), e.printStackTrace().toString())
        }
    }

    private fun generateItemCount(): ItemCount{
        val item = viewModel.currentItem.value
        val location = viewModel.currentLocation.value
        val countToSave = ItemCount(localId = UUID.randomUUID().toString())
        countToSave.ephuDeviceId = Utilities.getAndroidId(requireContext())
        countToSave.itemId = item?.id
        countToSave.taskId = task?.id
        countToSave.location = location?.code
        countToSave.dirty = true
        countToSave.readTimestamp = DateTime().toLocalDateTime().toString()
        countToSave.quantity = binding?.quantityEt?.text.toString().toInt()
        // variable boolean values
        task?.parameters?.forEach { parameter ->
            if(parameter.value) {
                when (parameter.parameterType) {
                    ParameterType.Serial -> {
                        countToSave.serial = binding?.serialEt?.text.toString()
                    }
                    ParameterType.Lpn -> {
                        countToSave.lpnCode = binding?.lpnEt?.text.toString()
                    }
                    ParameterType.Lot -> {
                        countToSave.lot = binding?.lotEt?.text.toString()
                    }
                    ParameterType.CreatedDate -> {
                        val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(binding?.createdDateEt?.text.toString())
                        Log.i(TAG, "fecha de creacion parseada: $date")
                        countToSave.createdDate =  SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date ?: "")
                    }
                    ParameterType.ExpiryDate -> {
                        val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(binding?.expiryDateEt?.text.toString())
                        Log.i(TAG, "fecha de expiracion parseada: $date")
                        countToSave.expiryDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date ?: "")
                    }
                    /*ParameterType.Empty -> {
                    countToSave.empty = binding?.emptySw.isChecked
                }*/
                }
            }
        }
        countToSave.lpnCode = countToSave.lpnCode ?: location?.code
        // for local use
        countToSave.description = item?.description
        countToSave.sku = item?.sku
        countToSave.locationId = location?.id
        countToSave.uploaded = false
        countToSave.sent = false
        return countToSave
    }

    private fun saveCount(){
        doAsync {
            var itemCount: ItemCount? = null
            if (isEditing == -1) { // cuando se crea uno nuevo
                itemCount = generateItemCount()
                currentLocationCounts.add(0, itemCount)
                uiThread {
                    //mAdapter?.notifyItemInserted(0)
                    mAdapter?.notifyDataSetChanged()
                    binding?.countsLabelTv?.text = getString(R.string.counts, currentLocationCounts.size)
                }
                db.itemCountDao().insert(itemCount)
                viewModel.repository.setCurrentLocationCounts(currentLocationCounts)
                viewModel.repository.setCurrentItem(null)
                //MyWorkerManagerService.enqueCountToUploadWork(requireContext(), item)
            } else { // cuando se edita uno existente
                mAdapter?.getItemAtPosition(isEditing)?.let { countToSave ->
                    val item = viewModel.currentItem.value
                    countToSave.itemId = item?.id
                    countToSave.dirty = true
                    countToSave.sent = false
                    countToSave.editing = false
                    countToSave.quantity = binding?.quantityEt?.text.toString().toInt()
                    task?.parameters?.forEach { parameter ->
                        if(parameter.value) {
                            when (parameter.parameterType) {
                                ParameterType.Serial -> {
                                    countToSave.serial = binding?.serialEt?.text.toString()
                                }
                                ParameterType.Lpn -> {
                                    countToSave.lpnCode = binding?.lpnEt?.text.toString()
                                }
                                ParameterType.Lot -> {
                                    countToSave.lot = binding?.lotEt?.text.toString()
                                }
                                ParameterType.CreatedDate -> {
                                    binding?.createdDateEt?.text?.let { date ->
                                        val parsedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(date.toString())
                                        Log.i(TAG, "fecha de creacion parseada: $parsedDate")
                                        countToSave.createdDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(parsedDate
                                                ?: "")
                                    }
                                }
                                ParameterType.ExpiryDate -> {
                                    binding?.expiryDateEt?.text?.let { date ->
                                        val parsedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(date.toString())
                                        Log.i(TAG, "fecha de expiracion parseada: $parsedDate")
                                        countToSave.expiryDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(parsedDate
                                                ?: "")
                                    }
                                }
                            }
                        }
                    }
                    countToSave.lpnCode = countToSave.lpnCode ?: viewModel.currentLocation.value?.code
                    countToSave.description = item?.description
                    countToSave.sku = item?.sku
                    //countToSave.uploaded = false
                    itemCount = countToSave
                    db.itemCountDao().update(countToSave)
                    viewModel.repository.setCurrentItem(null)
                    //mAdapter?.notifyDataSetChanged()
                    finishEditing(itemCount!!, isEditing)
                }
            }
            Log.i(TAG, "conteo generado: $itemCount")
            checkCountsTotals()
        }
    }

    private fun loadCurrentLocationCounts(locationId: Int?){
        Log.i(TAG, "cargando conteos de la ubicacion $locationId...")
        if (locationId != null) {
            doAsync {
                val counts = db.taskLocationsDao().getLocationCounts(locationId, task?.id!!)
                uiThread {
                    currentLocationCounts.clear()
                    if(!counts.isNullOrEmpty()){
                        currentLocationCounts.addAll(counts)
                    }
                    mAdapter?.notifyDataSetChanged()
                    binding?.countsLabelTv?.text = getString(R.string.counts, currentLocationCounts.size)
                }
            }
        } else {
            currentLocationCounts.clear()
            mAdapter?.notifyDataSetChanged()
        }
        checkCountsTotals()
    }

    private fun onDeleteCountListener(item: ItemCount, pos: Int){
        doAsync {
            db.itemCountDao().delete(item)
            uiThread {
                currentLocationCounts.remove(item)
                binding?.countsLabelTv?.text = getString(R.string.counts, currentLocationCounts.size)
                viewModel.repository.setCurrentLocationCounts(currentLocationCounts)
                //mAdapter?.notifyItemRemoved(pos)
                mAdapter?.notifyDataSetChanged()
                binding?.currentLocationCountRv?.invalidate()
                if(item.uploaded == true) {
                    MyWorkerManagerService.enqueDeleteCountWork(requireContext(), item)
                }
            }
        }
    }

    private fun onEditCountListener(count: ItemCount, pos: Int){
        //@drawable/round_corners
        Log.i(TAG, "click en item en la posicion $pos: $count")
        if(isEditing > -1 && isEditing != pos) {
            val oldItemCount = mAdapter?.getItemAtPosition(isEditing)
            Log.i(TAG, "limpiando edicion en item en la posicion $isEditing: $oldItemCount")
            oldItemCount?.let {
                finishEditing(it, isEditing)
            }
        }
        Handler().postDelayed({
            activity?.runOnUiThread {
                isEditing = pos
                mAdapter?.notifyItemChanged(pos)
            }
        }, 50)
        binding?.finishEditingBtn?.visibility = View.VISIBLE
        binding?.editLy?.setBackgroundResource(R.drawable.round_corners)
        binding?.editLayoutLabelTv?.text = getString(R.string.editing_count)
        binding?.finishEditingBtn?.setOnClickListener{
            finishEditing(count, pos)
        }
        presentCount(count)
    }

    private fun finishEditing(count: ItemCount, position: Int){
        Log.i(TAG, "finishing... editing of count at position $position: $count")
        activity?.runOnUiThread {
            //val oldPosition = isEditing
            isEditing = -1
            currentLocationCounts.getOrNull(position)?.editing = false
            //mAdapter?.isEditing = isEditing
            Log.i(TAG, "finishing... refresh item at position $position")
            mAdapter?.notifyItemChanged(position)
            //mAdapter?.notifyDataSetChanged()
            //binding?.currentLocationCountRv.invalidate()
            resetFormData()
            binding?.editLy?.setBackgroundResource(R.color.white)
            binding?.finishEditingBtn?.visibility = View.INVISIBLE
            binding?.editLayoutLabelTv?.text = getString(R.string.adding_new_count)
        }
    }

    private fun initAdapter(){
        mAdapter = CountAdapter(
                currentLocationCounts,
                requireContext(),
                this::onDeleteCountListener,
                this::onEditCountListener
        )
        binding?.currentLocationCountRv?.layoutManager = LinearLayoutManager(requireContext())
        binding?.currentLocationCountRv?.adapter = mAdapter
    }

    private fun resetFormData(){
        activity?.runOnUiThread {
            binding?.descriptionEt?.setText("")
            binding?.quantityEt?.setText("")
            binding?.skuEt?.setText("")
            // cleaning values
            binding?.serialEt?.setText("")
            binding?.expiryDateEt?.setText("")
            binding?.lotEt?.setText("")
            binding?.createdDateEt?.setText("")
            binding?.lpnEt?.setText("")
        }
    }

    private fun searchItemBySku(searchStr: String){
        doAsync {
            db.itemDao().search(searchStr).let {
                if (it == null) {
                    showToast(requireContext(), getString(R.string.sku_not_found))
                } else {
                    uiThread {_ ->
                        binding?.skuEt?.setText(it.sku)
                        binding?.quantityEt?.requestFocus()
                    }
                }
                viewModel.repository.setCurrentItem(it)
            }
        }
    }

    private fun presentItem(item: Item?){
        binding?.descriptionEt?.setText(((item?.sku ?: getString(R.string.without_sku)) + ": " + (item?.description ?: getString(R.string.without_description))).toUpperCase())
        if(item == null){
            resetFormData()
        }
    }

    private fun presentCount(count: ItemCount?){
        Log.i(TAG, "presentando conteo...")
        binding?.descriptionEt?.setText(count?.description ?: "")
        if(count == null){
            resetFormData()
        } else {
            binding?.skuEt?.setText(count.sku)
            binding?.quantityEt?.setText(count.quantity.toString())
            binding?.lotEt?.setText(count.lot)
            binding?.lpnEt?.setText(count.lpnCode)
            count.createdDate?.let{date ->
                if(isValidDate(date)) {
                    binding?.createdDateEt?.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            .format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    .parse(date)!!
                            )
                    )
                }
            }
            count.expiryDate?.let {date ->
                if(isValidDate(date)) {
                    binding?.expiryDateEt?.setText(
                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                    .format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                            .parse(date)!!
                                    )
                    )
                }
            }
            binding?.serialEt?.setText(count.serial)
            doAsync {
                db.itemDao().getById(count.itemId!!).let{
                    viewModel.repository.setCurrentItem(it)
                }
            }
        }
    }

    private fun isValidDate(date: String, pattern: String = "yyyy-MM-dd"): Boolean {
        try{
            SimpleDateFormat(pattern, Locale.getDefault())
                    .parse(date)
        }catch (e: Exception){
            Log.e(TAG, "Error al parsear fecha $date")
            e.printStackTrace()
            return false
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        /*val scanResult = ZxingOrient.parseActivityResult(requestCode, resultCode, data);
        if (scanResult != null) {
            Log.i(TAG, "resultados del scanner: ${scanResult}")
        }*/
        if(resultCode == RESULT_OK && requestCode == BARCODE_SCANNER){
            val  barcode = data?.getStringExtra("barcode") ?: ""
            searchItemBySku(barcode)
        } else if (resultCode != RESULT_OK && requestCode == BARCODE_SCANNER){
            showToast(requireContext(), getString(R.string.barcode_scanner_error))
        }
    }

    private fun hideLoader() {
        activity?.runOnUiThread {
            binding?.progressBar?.visibility = View.GONE
        }
    }

    private fun showLoader() {
        activity?.runOnUiThread{
            binding?.progressBar?.visibility = View.VISIBLE
        }
    }

    companion object {
        fun display(fragmentManager: FragmentManager, task: Task): TaskCountDialog? {
            val dialog = TaskCountDialog(task)
            dialog.show(fragmentManager, dialog.TAG)
            return dialog
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var currentTaskPosition = viewModel.tasksList.value?.indexOf(task) ?: -1
        Log.i(TAG, "tarea actual: $task")
        Log.i(TAG, "posicion de tarea actual: $currentTaskPosition")
        Log.i(TAG, "tamanio de lista: ${viewModel.tasksList.value?.size}")
        when(item.itemId) {
            R.id.next_task -> {
                Log.i(TAG, "boton siguiente tarea presionado...")
                if (currentTaskPosition >= 0 && (viewModel.tasksList.value?.size
                                ?: 0) > (currentTaskPosition + 1)) {
                    currentTaskPosition += 1
                    viewModel.repository.setCurrentTask(viewModel.tasksList.value?.get(currentTaskPosition))
                } else {
                    showToast(requireContext(), getString(R.string.not_more_tasks))
                }
            }
            R.id.prev_task -> {
                Log.i(TAG, "boton tarea anterior presionado...")
                if (currentTaskPosition > 0 && (viewModel.tasksList.value?.size
                                ?: 0) > (currentTaskPosition - 1)) {
                    currentTaskPosition -= 1
                    viewModel.repository.setCurrentTask(viewModel.tasksList.value?.get(currentTaskPosition))
                } else {
                    showToast(requireContext(), getString(R.string.not_more_tasks))
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun changeTaskState(task: Task, state: TaskState) {
        when(state){
            TaskState.Paused -> {
                if(task.taskState != TaskState.WorkInProgress) {
                    Utilities.showAlert(requireContext(), getString(R.string.error), getString(R.string.error_trying_to_pause_task))
                    return
                }
            }
            TaskState.WorkInProgress -> {
                if(task.taskState != TaskState.Active && task.taskState != TaskState.Paused) {
                    Utilities.showAlert(requireContext(), getString(R.string.error), getString(R.string.error_trying_to_init_resume))
                    return
                }
            }
        }
        MyWorkerManagerService.enqueChangeTaskStateWork(requireContext(), task.id, state, Constants.CHANGIN_TASK_STATUS_PROGRESS)
        task.taskState = state
        doAsync {
            db.tasksDao().update(task)
            // TODO: informar en el viewModel
            viewModel.tasksList.value?.find {
                it.id == task.id
            }?.let{
                it.taskState = state
            }
            viewModel.tasksList.value?.let {
                viewModel.repository.setTasks(it)
            }
        }
    }

    /*override fun onStop() {
        super.onStop()
        // TODO: cambiar estado de de tarea actual
    }

    override fun onPause() {
        super.onPause()
        // TODO: cambiar estado de de tarea actual
    }*/

    override fun onDismiss(dialog: DialogInterface) {
        // TODO: cambiar estado de de tarea actual
        task?.let {
            changeTaskState(it, TaskState.Paused)
        }
        super.onDismiss(dialog)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}