package com.tau.ephuapp.dialogs

import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.database.sqlite.SQLiteAccessPermException
import android.database.sqlite.SQLiteCantOpenDatabaseException
import android.database.sqlite.SQLiteDatabaseLockedException
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.View.OnTouchListener
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat.getColor
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.WorkInfo
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.tau.ephuapp.R
import com.tau.ephuapp.activities.BarcodeScannerActivity
import com.tau.ephuapp.activities.CalculatorActivity
import com.tau.ephuapp.activities.main.MainActivityViewModel
import com.tau.ephuapp.adapters.CountAdapter
import com.tau.ephuapp.adapters.RecountAdapter
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
import kotlin.random.Random


class TaskCountDialog(var task: Task?) : DialogFragment() {
    private var editingPosition: Int = -1
    private var _binding: FragmentTaskCountBinding? = null
    private val binding get() = _binding
    private lateinit var viewModel: MainActivityViewModel
    private val taskLocations = arrayListOf<Location>()
    private val currentLocationRecountTasks = arrayListOf<ItemCountTask>()
    private val currentLocationCounts = arrayListOf<ItemCount>()
    private var currentLocationPosition: Int = 0
    private var totalPendingCounts = 0
    private var pendingToUpdate: Int = 0
    private var totalCounts = 0
    private var mAdapter: CountAdapter? = null
    private var mRecountAdapter: RecountAdapter? = null
    private lateinit var db: AppDatabase
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme_FullScreenDialog)
        val _viewModel: MainActivityViewModel by activityViewModels()
        viewModel = _viewModel
        viewModel.repository.setCurrentTask(null)
        viewModel.repository.setCurrentLocation(null)
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

    fun initTaskData(){
        initAdapter()
        if (task?.taskType == TaskType.Recount) {
            binding?.skuEt?.setCompoundDrawables(null, null, null, null)
            binding?.quantityEt?.setCompoundDrawables(null, null, null, null)
            binding?.skuEt?.isEnabled = false
            binding?.lpnEt?.isEnabled = false
            binding?.lotEt?.isEnabled = false
            binding?.expiryDateEt?.isEnabled = false
            binding?.createdDateEt?.isEnabled = false
            binding?.quantityEt?.isEnabled = false
            binding?.saveBtn?.isEnabled = false
            binding?.editLayoutLabelTv?.text = getString(R.string.select_a_recount)
        } else {
            binding?.skuEt?.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_camera_alt_24, 0)
            binding?.quantityEt?.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_calculate_24, 0)
            binding?.skuEt?.isEnabled = true
            binding?.lpnEt?.isEnabled = true
            binding?.lotEt?.isEnabled = true
            binding?.expiryDateEt?.isEnabled = true
            binding?.createdDateEt?.isEnabled = true
            binding?.quantityEt?.isEnabled = true
            binding?.saveBtn?.isEnabled = true
            binding?.editLayoutLabelTv?.text = getString(R.string.adding_new_count)
        }
        resetFormData()
        viewModel.repository.setCurrentTaskLocations(null)// provisional mientras carga
        binding?.toolbar?.title = when(task?.taskType) {
            TaskType.Inventory -> getString(R.string.inventory_task_title, task?.id ?: 0, task?.count ?: 0)
            TaskType.Recount -> getString(R.string.recount_task_title, task?.id ?: 0, task?.count ?: 0)
            TaskType.Certification -> getString(R.string.certification_task_title, task?.id ?: 0, task?.count ?: 0)
            else -> getString(R.string.unknown_tasktype)
        }
        if (task != null) {
            Log.i(TAG, "Cargando locations de tarea...")
            if(task?.taskType == TaskType.Inventory) {
                //viewModel.repository.fetchTaskLocations(requireContext(), task?.id!!)
                viewModel.repository.setCurrentTaskLocations(task?.locations)
            } else if(task?.taskType == TaskType.Recount) {
                //viewModel.repository.fetchTaskLocationsRecount(requireContext(), task?.id!!)
                viewModel.repository.setCurrentTaskLocations(task?.locations)
            }
            loadTaskParameters()
        } else {
            viewModel.repository.setCurrentTaskLocations(arrayListOf<Location>())
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
        binding?.toolbar?.setBackgroundColor(getColor(requireContext(), R.color.purple_500))
        // Inflate a menu to be displayed in the toolbar
        binding?.toolbar?.inflateMenu(R.menu.main)
        // Set an OnMenuItemClickListener to handle menu item clicks
        binding?.toolbar?.setOnMenuItemClickListener {
            onOptionsItemSelected(it)
        }
        hideUi()
        binding?.descriptionEt?.keyListener = null // readonly
        binding?.countsLabelTv?.text = getString(R.string.counts,
            if(task?.taskType == TaskType.Inventory){
                currentLocationCounts.size
            } else {
                currentLocationRecountTasks.size
            }
        )
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
            try{
            val item = viewModel.currentItem.value
            val location = viewModel.currentLocation.value
            val quantity: Float = when {
                binding?.quantityEt?.text?.isNotEmpty() == true -> binding?.quantityEt?.text?.toString()
                    ?.toFloat() ?: 0f
                else -> 0f
            }
            if(item != null && location != null && quantity >= 0f && hasValidParams()){
                if (editingPosition == -1 || task?.taskType == TaskType.Recount) {
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
            } catch (e: Exception){
                e.printStackTrace()
                Log.e(TAG, "error al intentar guardar conteo", e)
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
        binding?.lotEt?.setOnTouchListener(OnTouchListener { v, event ->
            val DRAWABLE_LEFT = 0
            val DRAWABLE_TOP = 1
            val DRAWABLE_RIGHT = 2
            val DRAWABLE_BOTTOM = 3
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (binding?.lotEt?.getRight() ?: 0) - (binding?.lotEt?.getCompoundDrawables()?.get(DRAWABLE_RIGHT)?.bounds?.width() ?: 0)) {
                    startActivityForResult(Intent(requireContext(), BarcodeScannerActivity::class.java), BARCODE_SCANNER_LOT)
                    //ZxingOrient(this).initiateScan()
                    return@OnTouchListener true
                }
            }
            false
        })
        binding?.lpnEt?.setOnTouchListener(OnTouchListener { v, event ->
            val DRAWABLE_LEFT = 0
            val DRAWABLE_TOP = 1
            val DRAWABLE_RIGHT = 2
            val DRAWABLE_BOTTOM = 3
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (binding?.lpnEt?.getRight() ?: 0) - (binding?.lpnEt?.getCompoundDrawables()?.get(DRAWABLE_RIGHT)?.bounds?.width() ?: 0)) {
                    startActivityForResult(Intent(requireContext(), BarcodeScannerActivity::class.java), BARCODE_SCANNER_LPN)
                    //ZxingOrient(this).initiateScan()
                    return@OnTouchListener true
                }
            }
            false
        })
        binding?.quantityEt?.setOnTouchListener(OnTouchListener { v, event ->
            val DRAWABLE_LEFT = 0
            val DRAWABLE_TOP = 1
            val DRAWABLE_RIGHT = 2
            val DRAWABLE_BOTTOM = 3
            if (event.action == MotionEvent.ACTION_UP) {
                val touchedPosition = event.rawX
                val drawablePosition = (v.right + (binding?.skuEt?.right ?: 0)) -
                    (binding?.quantityEt?.getCompoundDrawables()
                    ?.get(DRAWABLE_RIGHT)?.bounds?.width() ?: 0)
                Log.i(TAG, "drawable position: $drawablePosition")
                Log.i(TAG, "touched position: $touchedPosition")
                Log.i(TAG, "touched in drawable position?: ${touchedPosition >= drawablePosition}")
                if (touchedPosition >= drawablePosition) {
                    startActivityForResult(
                        Intent(requireContext(), CalculatorActivity::class.java),
                        CALCULATOR
                    )
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
        /*binding?.doneBtn?.setOnClickListener {
            if((totalPendingCounts + pendingToUpdate) > 0) {
                showAlert(
                    requireContext(),
                    getString(R.string.confirmation),
                    getString(R.string.save_all_counts_confirm_msg),
                    this::saveAllTaskCounts
                )
            }
        }*/
        binding?.emptySw?.setOnCheckedChangeListener { buttonView, isChecked ->
            Log.i(TAG, "on changed empty slide toogle. is checked: $isChecked")
            if(buttonView.isPressed) {
                showAlert(requireContext(), getString(R.string.confirmation),
                    when (isChecked) {
                        true -> getString(R.string.set_location_empty_confirm_msg)
                        else -> getString(R.string.set_location_not_empty_confirm_msg)
                    },
                    {
                        viewModel.currentLocation.value?.let {
                            toggleLocationIsEmpty(it.id, isChecked)
                            if(it.isEmpty != true) {
                                doAsync {
                                    db.taskLocationsDao().updateLocationAsEmpty(it.id, isChecked)
                                }
                            }
                        }
                    }, {
                        viewModel.currentLocation.value?.let {
                            binding?.emptySw?.isChecked = !isChecked
                            it.isEmpty = !isChecked
                            if(it.isEmpty != true && isChecked) {
                                doAsync {
                                    db.taskLocationsDao().updateLocationAsEmpty(it.id, !isChecked)
                                }
                            }
                        }
                    }
                )
            }
        }
        viewModel.currentTask.observe(viewLifecycleOwner, {
            it?.let{
                task = it
                initTaskData()
            }
        })
        viewModel.currentLocation.observe(viewLifecycleOwner, Observer { location ->
            editingPosition = -1
            if (location != null && location.taskId == task?.id) {
                Log.i(TAG, "current location observed $location")
                when(task?.taskType) {
                    TaskType.Inventory -> loadCurrentLocationCounts(location.id)
                    TaskType.Recount -> {
                        loadCurrentLocationCounts(location.id)
                        loadCurrentLocationRecounts(location)
                    }
                }
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
                if (location.isEmpty == true) {
                    binding?.editLy?.visibility = View.GONE
                    binding?.countsLabelTv?.visibility = View.GONE
                    binding?.currentLocationCountRv?.visibility = View.GONE
                    binding?.totalCountsTv?.visibility = View.GONE
                    //binding?.doneBtn?.visibility = View.GONE
                    binding?.emptyLocationTv?.visibility = View.VISIBLE
                    binding?.emptySw?.isChecked = true
                    hideParametersViews()
                } else {
                    binding?.editLy?.visibility = View.VISIBLE
                    binding?.countsLabelTv?.visibility = View.VISIBLE
                    binding?.currentLocationCountRv?.visibility = View.VISIBLE
                    binding?.totalCountsTv?.visibility = View.VISIBLE
                    //binding?.doneBtn?.visibility = View.VISIBLE
                    binding?.emptyLocationTv?.visibility = View.GONE
                    binding?.emptySw?.isChecked = false
                    renderTaskParameters()
                }
            } else {
                binding?.locationCodeTv?.text = ""
                binding?.paginationTv?.text = ""
                binding?.leftBtn?.visibility = View.GONE
                binding?.rightBtn?.visibility = View.GONE
                binding?.emptySw?.isChecked = false
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
                    showAlert(
                        requireContext(),
                        getString(R.string.information),
                        getString(R.string.empy_or_null_task_lines)
                    )
                }
            }
        })
        viewModel.repository.getCurrentLocationCounts().observe(viewLifecycleOwner, {counts ->
            Log.i(TAG, "counts observados: $counts")
            currentLocationCounts.clear()
            activity?.runOnUiThread {
                if (!counts.isNullOrEmpty()) {
                    currentLocationCounts.addAll(counts)
                    binding?.countsLabelTv?.text =
                        getString(R.string.counts, if(task?.taskType == TaskType.Inventory){
                            currentLocationCounts.size
                        } else {
                            currentLocationRecountTasks.size
                        })
                    binding?.emptySw?.visibility = View.GONE
                    if (viewModel.currentLocation.value?.isEmpty == true) {
                        viewModel.currentLocation.value?.isEmpty = false
                        doAsync {
                            db.taskLocationsDao()
                                .updateLocationAsEmpty(viewModel.currentLocation.value?.id!!, false)
                        }
                    }
                } else {
                    task?.parameters?.find { parameter ->
                        parameter.parameterType == ParameterType.Empty
                    }?.let { parameter ->
                        Log.i(TAG, "La tarea tiene parametro empty = ${parameter.value}")
                        if (parameter.value == true) {
                            binding?.emptySw?.visibility = View.VISIBLE
                        } else {
                            binding?.emptySw?.visibility = View.GONE
                        }
                    }
                }
                if(task?.taskType == TaskType.Inventory) {
                    mAdapter?.notifyDataSetChanged()
                } else if(task?.taskType == TaskType.Recount){
                    mRecountAdapter?.notifyDataSetChanged()
                }
                checkCountsTotals()
            }
        })
        viewModel.repository.getCurrentLocationRecountTasks().observe(viewLifecycleOwner, {tasks ->
            Log.i(TAG, "recount tasks observados: $tasks")
            currentLocationRecountTasks.clear()
            activity?.runOnUiThread {
                if (!tasks.isNullOrEmpty()) {
                    currentLocationRecountTasks.addAll(tasks)
                    binding?.countsLabelTv?.text =
                        getString(R.string.counts, currentLocationRecountTasks.size)
                    binding?.emptySw?.visibility = View.GONE
                    if (viewModel.currentLocation.value?.isEmpty == true) {
                        viewModel.currentLocation.value?.isEmpty = false
                        doAsync {
                            db.taskLocationsDao()
                                .updateLocationAsEmpty(viewModel.currentLocation.value?.id!!, false)
                        }
                    }
                } else {
                    task?.parameters?.find { parameter ->
                        parameter.parameterType == ParameterType.Empty
                    }?.let { parameter ->
                        Log.i(TAG, "La tarea tiene parametro empty = ${parameter.value}")
                        if (parameter.value == true) {
                            binding?.emptySw?.visibility = View.VISIBLE
                        } else {
                            binding?.emptySw?.visibility = View.GONE
                        }
                    }
                }
                if(task?.taskType == TaskType.Recount) {
                    mRecountAdapter?.notifyDataSetChanged()
                }
                checkCountsTotals()
            }
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
                    //Utilities.showToast(requireContext(), getString(R.string.uploading_counts))
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
                        Log.i(TAG, "WORK exitoso!")
                        // si todo salio bien, actualizar id's remotos
                        doAsync {
                            val returnedDataMap: Map<String, Any?> = workInfo.outputData.keyValueMap
                            if(!returnedDataMap.containsKey("exception")) {
                                returnedDataMap.forEach { mapEntry ->
                                    if(mapEntry.key.contains("localId-")) {
                                        val uploadedCount = Gson().fromJson(mapEntry.value as String, ItemCount::class.java)
                                        val newRemoteId = uploadedCount.id
                                        val localId = mapEntry.key.removePrefix("localId-")
                                        currentLocationCounts.find { itemCount ->
                                            itemCount.localId == localId
                                        }?.apply {
                                            Log.i(TAG, "actualizando  conteo exitoso $localId")
                                            id = newRemoteId ?: uploadedCount?.id
                                            uploaded = true
                                            sent = false
                                            hasError = uploadedCount?.hasError ?: false
                                            errorMessage = uploadedCount?.errorMessage
                                            dirty = lastUpdateTimestamp != uploadedCount.lastUpdateTimestamp
                                        }
                                    }
                                }
                            }
                            uiThread {
                                if(task?.taskType == TaskType.Inventory) {
                                    mAdapter?.notifyDataSetChanged()
                                } else if(task?.taskType == TaskType.Recount) {
                                    mRecountAdapter?.notifyDataSetChanged()
                                }
                            }
                        }
                    }
                    //Utilities.showToast(requireContext(), msg)
                }
                if (WorkInfo.State.FAILED == workInfo.state) {
                    Log.e(TAG, "Work finalizado con error")
                    var msg = getString(R.string.error_uploading_counts)
                    if(workInfo.outputData.hasKeyWithValueOfType("exception", String::class.java)){
                        msg = workInfo.outputData.getString("exception").toString()
                    } else if (workInfo.outputData.hasKeyWithValueOfType("error", String::class.java)) {
                        msg = workInfo.outputData.getString("error").toString()
                    }
                    Log.e(TAG, "Error: $msg")
                    doAsync {
                        val returnedDataMap: Map<String, Any?> = workInfo.outputData.keyValueMap
                        returnedDataMap.forEach { mapEntry ->
                            if(mapEntry.key.contains("localId-")) {
                                //val uploadedCount = Gson().fromJson(mapEntry.value as String, ItemCount::class.java)
                                if(task?.taskType == TaskType.Inventory) {
                                    val localId = mapEntry.key.removePrefix("localId-")
                                    val dbItemCount = db.itemCountDao().getByLocalId(localId)
                                    currentLocationCounts.find { itemCount ->
                                        itemCount.localId == localId
                                    }?.apply {
                                        Log.i(TAG, "actualizando  conteo $localId")
                                        uploaded = false
                                        sent = false
                                        hasError = true
                                        errorMessage = dbItemCount?.errorMessage
                                        dirty = true
                                    }
                                }
                            }
                        }
                        uiThread {
                            if(task?.taskType == TaskType.Inventory) {
                                mAdapter?.notifyDataSetChanged()
                            } else if(task?.taskType == TaskType.Recount) {
                                mRecountAdapter?.notifyDataSetChanged()
                            }
                        }
                    }
                }
                if (WorkInfo.State.CANCELLED == workInfo.state) {
                    //Utilities.showToast(requireContext(), getString(R.string.counts_uploading_cancelled))
                }
            }
        })
        viewModel.savingEditCountWorkProgress.observe(viewLifecycleOwner, {
            Log.i(TAG, "progreso de subida de edicion de conteo observado...")
            it.forEach { workInfo ->
                if (WorkInfo.State.RUNNING == workInfo.state) {
                    binding?.progressBar?.visibility = View.VISIBLE
                    //Utilities.showToast(requireContext(), getString(R.string.uploading_count_edit))
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
                        val returnedDataMap = workInfo.outputData.keyValueMap
                        var lastUpdate = if(returnedDataMap.containsKey("lastUpdate")){
                            returnedDataMap.getValue("lastUpdate") as Long
                        } else {
                            null
                        }
                        returnedDataMap.remove("lastUpdate")
                        if(task?.taskType == TaskType.Inventory) {
                            returnedDataMap.forEach { mapEntry ->
                                val index = currentLocationCounts.indexOfFirst { count ->
                                    count.localId == mapEntry.key
                                }
                                if (index > -1) {
                                    // si el id remoto es distinto, entonces se creo un nuevo conteo y hay
                                    // que sustituir el anterior por el nuevo guardado por el worker
                                    val remoteId = mapEntry.value as Int?
                                    doAsync {
                                        db.itemCountDao().getByLocalId(mapEntry.key)?.let { newItemCount ->
                                            if (remoteId != null && currentLocationCounts[index].id != remoteId) {
                                                currentLocationCounts[index] =
                                                    newItemCount
                                            } else {
                                                //currentLocationCounts[index].dirty = newItemCount.dirty
                                                currentLocationCounts[index].dirty =
                                                    currentLocationCounts[index].lastUpdateTimestamp != lastUpdate
                                                currentLocationCounts[index].sent =
                                                    newItemCount.sent
                                                currentLocationCounts[index].hasError =
                                                    newItemCount.hasError
                                                currentLocationCounts[index].errorMessage =
                                                    newItemCount.errorMessage
                                            }
                                            uiThread {
                                                mAdapter?.notifyDataSetChanged()
                                            }
                                        }
                                    }
                                }
                            }
                        } else if(task?.taskType == TaskType.Recount) {
                            // TODO: no sabemos que va aqui, por los momentos
                            mRecountAdapter?.notifyDataSetChanged()
                        }
                    }
                }
                if (WorkInfo.State.FAILED == workInfo.state) {
                    var msg = getString(R.string.error_uploading_counts)
                    if(workInfo.outputData.hasKeyWithValueOfType("exception", String::class.java)){
                        msg = workInfo.outputData.getString("exception").toString()
                    } else if (workInfo.outputData.hasKeyWithValueOfType("error", String::class.java)) {
                        msg = workInfo.outputData.getString("error").toString()
                    }
                    doAsync {
                        val returnedDataMap = workInfo.outputData.keyValueMap
                        if(task?.taskType == TaskType.Inventory) {
                            returnedDataMap.forEach { mapEntry ->
                                val localId = mapEntry.key
                                val dbItemCount = db.itemCountDao().getByLocalId(localId)
                                currentLocationCounts.find { itemCount ->
                                    itemCount.localId == localId
                                }?.apply {
                                    Log.i(TAG, "actualizando  conteo $localId")
                                    sent = false
                                    hasError = true
                                    errorMessage = dbItemCount?.errorMessage
                                    dirty = true
                                }
                            }
                            uiThread {
                                mAdapter?.notifyDataSetChanged()
                            }
                        } else if(task?.taskType == TaskType.Recount) {
                            uiThread {
                                mRecountAdapter?.notifyDataSetChanged()
                            }
                        }
                    }
                }
                if (WorkInfo.State.CANCELLED == workInfo.state) {
                    //Utilities.showToast(requireContext(), getString(R.string.counts_uploading_cancelled))
                }
            }
        })
        Log.i(TAG, "lista de tareas: ${viewModel.tasksList.value}")
        Log.i(TAG, "tarea recibida: $task")
        viewModel.repository.setCurrentTask(task)
    }

    private fun toggleLocationIsEmpty(locationId: Int, isEmpty: Boolean){
        Log.i(TAG, "on toggleLocationAsEmpty. locationId: $locationId. is checked: $isEmpty")
        Log.i(TAG, "viewModel.currentLocation.value?.isEmpty: ${viewModel.currentLocation.value?.isEmpty}")
        if (viewModel.currentLocation.value?.isEmpty != true && isEmpty) {
            binding?.editLy?.visibility = View.GONE
            binding?.countsLabelTv?.visibility = View.GONE
            binding?.currentLocationCountRv?.visibility = View.GONE
            binding?.totalCountsTv?.visibility = View.GONE
            //binding?.doneBtn?.visibility = View.GONE
            binding?.emptyLocationTv?.visibility = View.VISIBLE
            binding?.emptySw?.isChecked = isEmpty
            viewModel.currentLocation.value?.isEmpty = isEmpty
            hideParametersViews()
            enqueIsEmptyLocation(arrayListOf(generateItemCountAsEmpty()), locationId, isEmpty)
        } else if (viewModel.currentLocation.value?.isEmpty == true && !isEmpty) {
            binding?.editLy?.visibility = View.VISIBLE
            binding?.countsLabelTv?.visibility = View.VISIBLE
            binding?.currentLocationCountRv?.visibility = View.VISIBLE
            binding?.totalCountsTv?.visibility = View.VISIBLE
            //binding?.doneBtn?.visibility = View.VISIBLE
            binding?.emptyLocationTv?.visibility = View.GONE
            binding?.emptySw?.isChecked = isEmpty
            renderTaskParameters()
        }
    }

    private fun hasValidParams(): Boolean{
        return task?.parameters?.all { parameter ->
            when (parameter.parameterType) {
                ParameterType.Lot -> {
                    (parameter.value == true && binding?.lotEt?.text?.isNotEmpty() == true) || parameter.value == false
                }
                ParameterType.Lpn -> {
                    (parameter.value == true && binding?.lpnEt?.text?.isNotEmpty() == true) || parameter.value == false
                }
                ParameterType.CreatedDate -> {
                    (parameter.value == true && binding?.createdDateEt?.text?.isNotEmpty() == true && isValidDate(binding?.createdDateEt?.text.toString(), "dd/MM/yyyy")) || parameter.value == false
                }
                ParameterType.ExpiryDate -> {
                    (parameter.value == true && binding?.expiryDateEt?.text?.isNotEmpty() == true && isValidDate(binding?.expiryDateEt?.text.toString(), "dd/MM/yyyy")) || parameter.value == false
                }
                ParameterType.Serial -> {
                    (parameter.value == true && binding?.serialEt?.text?.isNotEmpty() == true) || parameter.value == false
                }
                else -> true
            }
        } ?: true
    }

    private fun hideUi(){
        binding?.frameLayout?.visibility = View.GONE
        binding?.leftBtn?.visibility = View.GONE
        binding?.rightBtn?.visibility = View.GONE
        binding?.totalCountsTv?.visibility = View.GONE
    }

    private fun showUi(){
        binding?.frameLayout?.visibility = View.VISIBLE
        binding?.leftBtn?.visibility = View.VISIBLE
        binding?.rightBtn?.visibility = View.VISIBLE
        binding?.totalCountsTv?.visibility = View.VISIBLE
        //binding?.doneBtn?.visibility = View.VISIBLE
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
                        /*ParameterType.Empty -> {
                            binding?.emptySw
                        }*/
                        else -> null
                    }
                    if(parameter.value == true) {
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
            totalCounts = when(task?.taskType) {
                TaskType.Inventory -> db.itemCountDao().countAllByTask(task?.id!!)
                TaskType.Recount -> db.itemCountDao().countAllRecountByTask(task?.id!!)
                else -> 0
            }
            Log.i(TAG, "totalCounts: $totalCounts")
            totalPendingCounts = when(task?.taskType) {
                TaskType.Inventory -> db.itemCountDao().countAllPendingToUploadByTask(task?.id!!)
                TaskType.Recount -> db.itemCountDao()
                    .countAllPendingRecountToUploadByTask(task?.id!!)
                else -> 0
            }
            Log.i(TAG, "totalPendingCounts: $totalPendingCounts")
            pendingToUpdate = when(task?.taskType) {
                TaskType.Inventory -> db.itemCountDao().countAllPendingToUpdateByTask(task?.id!!)
                TaskType.Recount -> db.itemCountDao()
                    .countAllPendingRecountToUpdateByTask(task?.id!!)
                else -> 0
            }
            Log.i(TAG, "pendingToUpdate: $pendingToUpdate")
            uiThread {
                binding?.countsLabelTv?.text = getString(R.string.counts, if(task?.taskType == TaskType.Inventory){
                    currentLocationCounts.size
                } else {
                    currentLocationRecountTasks.size
                })
                binding?.totalCountsTv?.text = getString(R.string.total_task_counts, totalCounts)
                binding?.doneBtn?.text = getString(R.string.save_task_pending_counts, totalPendingCounts + pendingToUpdate)
                binding?.doneBtn?.isEnabled = totalPendingCounts + pendingToUpdate > 0
            }
        }
    }
    /*
    private fun saveAllTaskCounts(){
        Log.i(TAG, "saving all task counts...")
        try {
            doAsync {
                val newCountsToSave = when(task?.taskType) {
                    TaskType.Inventory -> {
                        db.itemCountDao()
                            .getAllPendingToUploadByTask(task?.id!!)
                    }
                    TaskType.Recount -> {
                        db.itemCountDao()
                            .getAllPendingRecountToUploadByTask(task?.id!!)
                    }
                    else -> null
                }
                Log.i(TAG, "Nuevos conteos por subir y crear: $newCountsToSave")
                val countsToUpdate = when(task?.taskType){
                    TaskType.Inventory -> db.itemCountDao()
                        .getAllPendingToUpdateByTask(task?.id!!)
                    TaskType.Recount -> db.itemCountDao()
                        .getAllPendingRecountToUpdateByTask(task?.id!!)
                    else -> null
                }
                Log.i(TAG, "Conteos por subir y actualizar: $countsToUpdate")
                if (!newCountsToSave.isNullOrEmpty()) {
                    Log.i(TAG, "Se encontraron conteos nuevos por subir: $countsToUpdate")
                    enqueCountsToUpload(newCountsToSave)
                }
                if (!countsToUpdate.isNullOrEmpty()) {
                    Log.i(TAG, "Se encontraron conteos por actualizar: $countsToUpdate")
                    countsToUpdate.forEach { itemCount ->
                        MyWorkerManagerService.enqueEditCountToUploadWork(
                            requireContext(),
                            itemCount,
                            Constants.SAVING_EDIT_COUNT_PROGRESS
                        )
                    }
                }
                dismiss()
            }
        } catch (e: Exception) {
            Utilities.showAlert(requireContext(), getString(R.string.error), getString(R.string.error_uploading_counts))
        }
    }

    private fun enqueCountsToUpload(counts: List<ItemCount>){
        Log.i(TAG, "Preparandose para poner en cola los conteos: $counts")
        viewModel.repository.setIsSavingCounts(true)
        MyWorkerManagerService.enqueCountToUploadArrayWork(
            requireContext(),
            counts,
            task?.id!!,
            Constants.SAVING_COUNTS_PROGRESS
        )
    }*/

    private fun enqueCountToUpload(count: ItemCount){
        Log.i(TAG, "Preparandose para poner en cola el conteo: $count")
        viewModel.repository.setIsSavingCount(true)
        MyWorkerManagerService.enqueSingleCountToUpload(
            requireContext(),
            task?.id!!,
            count
        )
    }

    private fun enqueIsEmptyLocation(counts: List<ItemCount>, locationId: Int, isEmpty: Boolean){
        viewModel.repository.setIsSavingCounts(true)
        MyWorkerManagerService.enqueChangeLocationIsEmptyWork(
            requireContext(),
            counts,
            task?.id!!,
            locationId,
            isEmpty,
            Constants.SENDING_EMPTY_PROGRESS
        )
    }

    private fun generateItemCount(baseItemCount: ItemCountTask? = null): ItemCount{
        Log.i(TAG, "on generateItemCount()...")
        val item = viewModel.currentItem.value
        val location = viewModel.currentLocation.value
        Log.i(TAG, "location: $location")
        val countToSave = ItemCount(id = Random.nextInt(999999, 99999999),
        localId = UUID.randomUUID().toString(), ephuDeviceId = Utilities.getAndroidId(requireContext()),
        itemId = item?.id, taskId = task?.id, location = location?.code, lot = baseItemCount?.lot,
        dirty = true, readTimestamp = DateTime().toLocalDateTime().toString(), lpnCode = location?.code,
        lastUpdateTimestamp = DateTime.now().millis, quantity = binding?.quantityEt?.text.toString().toFloat(),
        description = item?.description, sku = item?.sku, uploaded = false, sent = false,
        recount = task?.taskType == TaskType.Recount)
        countToSave.locationId = if(task?.taskType == TaskType.Recount){
            location?.locationId
        } else {
            location?.id
        }
        // variable boolean values
        task?.parameters?.forEach { parameter ->
            if(parameter.value == true) {
                when (parameter.parameterType) {
                    ParameterType.Serial -> {1
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
                }
            }
        }
        return countToSave
    }

    private fun generateItemCountAsEmpty(): ItemCount{
        val location = viewModel.currentLocation.value
        val countToSave = ItemCount(id = Random.nextInt(999999, 99999999), localId = UUID.randomUUID().toString())
        countToSave.ephuDeviceId = Utilities.getAndroidId(requireContext())
        countToSave.itemId = null
        countToSave.taskId = task?.id
        countToSave.location = location?.code
        countToSave.locationId = if(task?.taskType == TaskType.Recount){
            location?.locationId
        } else {
            location?.id
        }
        countToSave.dirty = true
        countToSave.readTimestamp = DateTime().toLocalDateTime().toString()
        countToSave.quantity = 0f
        countToSave.lpnCode = "EPHU_EMPTY_${countToSave.localId}"
        // for local use
        countToSave.description = null
        countToSave.sku = null
        countToSave.uploaded = false
        countToSave.sent = false
        return countToSave
    }

    private fun saveCount(){
        doAsync {
            var itemCount: ItemCount? = null
            Log.i(TAG, "isEditing: $editingPosition")
            Log.i(TAG, "Task type: ${task?.taskType}")
            if (editingPosition == -1 && task?.taskType == TaskType.Inventory) { // cuando se crea uno nuevo y es tarea de inventario
                itemCount = generateItemCount()
                Log.i(TAG, "conteo generado: $itemCount")
                db.itemCountDao().insert(itemCount)
                viewModel.repository.getCurrentLocationCounts().value?.let {
                    it.add(0, itemCount!!)
                    viewModel.repository.setCurrentLocationCounts(it)
                }
                viewModel.repository.setCurrentItem(null)
                //enqueCountsToUpload(listOf(itemCount))
                enqueCountToUpload(itemCount)
            } else if(editingPosition > -1) { // cuando se edita uno existente o un reconteo
                Log.i(TAG, "Editando un conteo existente o de reconteo")
                if(task?.taskType == TaskType.Inventory) {
                    Log.i(TAG, "Es conteo existente")
                    mAdapter?.getItemAtPosition(editingPosition)?.let { countToSave ->
                        val item = viewModel.currentItem.value
                        countToSave.dirty = true
                        countToSave.sent = true
                        countToSave.editing = false
                        countToSave.lastUpdateTimestamp = DateTime.now().millis
                        countToSave.recount = false
                        countToSave.locationId = viewModel.currentLocation.value?.id
                        countToSave.quantity = binding?.quantityEt?.text.toString().toFloat()
                        countToSave.itemId = item?.id
                        countToSave.lpnCode = countToSave.lpnCode ?: viewModel.currentLocation.value?.code
                        countToSave.description = item?.description
                        countToSave.sku = item?.sku
                        task?.parameters?.forEach { parameter ->
                            if (parameter.value == true) {
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
                                            val parsedDate = SimpleDateFormat(
                                                "dd/MM/yyyy",
                                                Locale.getDefault()
                                            ).parse(date.toString())
                                            Log.i(TAG, "fecha de creacion parseada: $parsedDate")
                                            countToSave.createdDate = SimpleDateFormat(
                                                "yyyy-MM-dd",
                                                Locale.getDefault()
                                            ).format(
                                                parsedDate
                                                    ?: ""
                                            )
                                        }
                                    }
                                    ParameterType.ExpiryDate -> {
                                        binding?.expiryDateEt?.text?.let { date ->
                                            val parsedDate = SimpleDateFormat(
                                                "dd/MM/yyyy",
                                                Locale.getDefault()
                                            ).parse(date.toString())
                                            Log.i(TAG, "fecha de expiracion parseada: $parsedDate")
                                            countToSave.expiryDate = SimpleDateFormat(
                                                "yyyy-MM-dd",
                                                Locale.getDefault()
                                            ).format(
                                                parsedDate
                                                    ?: ""
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        //countToSave.uploaded = false
                        itemCount = countToSave
                        db.itemCountDao().update(countToSave)
                        MyWorkerManagerService.enqueEditCountToUploadWork(
                            requireContext(),
                            itemCount!!,
                            Constants.SAVING_EDIT_COUNT_PROGRESS
                        )
                        viewModel.repository.setCurrentItem(null)
                        finishEditing(editingPosition)
                    }
                } else if(task?.taskType == TaskType.Recount){
                    Log.i(TAG, "Es reconteo")
                    mRecountAdapter?.getItemAtPosition(editingPosition)?.let { countTask ->
                        val item = viewModel.currentItem.value
                        val countToSave = db.itemCountDao().getByTaskLineAndItem(countTask.taskLineId, item?.id!!)
                        Log.i(TAG, "conteo existente para el reconteo: $countToSave")
                        if(countToSave == null){ // No existe el conteo, hay que crearlo
                            Log.i(TAG, "No existe conteo para el reconteo: $countTask")
                            itemCount = generateItemCount(countTask)
                            itemCount?.taskLineId = countTask.taskLineId
                            Log.i(TAG, "conteo creado para el reconteo: ${countTask.taskLineId}: $itemCount")
                            db.itemCountDao().insert(itemCount!!)
                            //enqueCountsToUpload(listOf(itemCount!!))
                            enqueCountToUpload(itemCount!!)
                        } else {
                            Log.i(TAG, "Ya existe conteo para el reconteo: ${countTask.taskLineId}: $countToSave")
                            countToSave.dirty = true
                            countToSave.sent = true
                            countToSave.editing = false
                            countToSave.lastUpdateTimestamp = DateTime.now().millis
                            countToSave.recount = true
                            countToSave.quantity = binding?.quantityEt?.text.toString().toFloat()
                            countToSave.itemId = item.id
                            countToSave.description = item.description
                            countToSave.sku = item.sku
                            task?.parameters?.forEach { parameter ->
                                if (parameter.value == true) {
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
                                                val parsedDate = SimpleDateFormat(
                                                    "dd/MM/yyyy",
                                                    Locale.getDefault()
                                                ).parse(date.toString())
                                                Log.i(TAG, "fecha de creacion parseada: $parsedDate")
                                                countToSave.createdDate = SimpleDateFormat(
                                                    "yyyy-MM-dd",
                                                    Locale.getDefault()
                                                ).format(
                                                    parsedDate
                                                        ?: ""
                                                )
                                            }
                                        }
                                        ParameterType.ExpiryDate -> {
                                            binding?.expiryDateEt?.text?.let { date ->
                                                val parsedDate = SimpleDateFormat(
                                                    "dd/MM/yyyy",
                                                    Locale.getDefault()
                                                ).parse(date.toString())
                                                Log.i(TAG, "fecha de expiracion parseada: $parsedDate")
                                                countToSave.expiryDate = SimpleDateFormat(
                                                    "yyyy-MM-dd",
                                                    Locale.getDefault()
                                                ).format(
                                                    parsedDate
                                                        ?: ""
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            itemCount = countToSave
                            db.itemCountDao().update(countToSave)
                            MyWorkerManagerService.enqueEditCountToUploadWork(
                                requireContext(),
                                itemCount!!,
                                Constants.SAVING_EDIT_COUNT_PROGRESS
                            )
                        }
                        viewModel.repository.setCurrentItem(null)
                        finishEditingRecountTask(editingPosition)
                    }
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
                counts.forEach {
                    it.editing = false
                }
                val countsArrayList = arrayListOf<ItemCount>()
                countsArrayList.addAll(counts)
                viewModel.repository.setCurrentLocationCounts(countsArrayList)
            }
        } else {
            if(task?.taskType == TaskType.Inventory) {
                currentLocationCounts.clear()
                mAdapter?.notifyDataSetChanged()
            } else if(task?.taskType == TaskType.Recount) {
                currentLocationRecountTasks.clear()
                mRecountAdapter?.notifyDataSetChanged()
            }
        }
        checkCountsTotals()
    }

    private fun loadCurrentLocationRecounts(location: Location?){
        Log.i(TAG, "cargando reconteos de la ubicacion ${location?.id} de la tarea ${task?.id}...")
        if (location?.id != null && task?.id != null) {
            doAsync {
                if(location.details.isNullOrEmpty()){
                    Log.e(
                        TAG,
                        "No hay reconteos para la tarea ${task?.id} en la ubicacion ${location.id}"
                    )
                } else {
                    val itemCountTasks = arrayListOf<ItemCountTask>()
                    location.details?.forEach { itemCount ->
                        var itemExists = db.itemDao().getById(itemCount.itemId!!) != null
                        if (!itemExists) {
                            Log.e(TAG, "el item no existe en la DB: $itemCount")
                            showAlert(
                                requireContext(),
                                getString(R.string.error),
                                getString(R.string.not_existent_item_error_msg)
                            )
                            return@doAsync
                        } else {
                            val itemCountTask = ItemCountTask(
                                    taskId = task?.id!!,
                                    taskLineId = itemCount.taskLineId!!,
                                    itemId = itemCount.itemId!!,
                                    lpnCode = itemCount.lpnCode,
                                    lot = itemCount.lot,
                                    localId = itemCount.localId,
                                    expiryDate = itemCount.expiryDate,
                                    createdDate = itemCount.createdDate,
                                    serial = itemCount.serial,
                                    locationId = itemCount.locationId,
                                    editing = false
                            )
                            itemCountTasks.add(itemCountTask)
                        }
                    }
                    //currentLocationRecountTasks.addAll(itemCountTasks)
                    Log.i(TAG, "reconteos cargados: ${location.details}}")
                    viewModel.repository.setCurrentLocationRecountTasks(itemCountTasks)
                }
            }
        } else {
            if(task?.taskType == TaskType.Inventory) {
                currentLocationCounts.clear()
                mAdapter?.notifyDataSetChanged()
            } else if(task?.taskType == TaskType.Recount){
                currentLocationRecountTasks.clear()
                mRecountAdapter?.notifyDataSetChanged()
            }
        }
        checkCountsTotals()
    }

    private fun onDeleteCountListener(item: ItemCount, pos: Int){
        doAsync {
            db.itemCountDao().delete(item)
            if(item.uploaded) {
                MyWorkerManagerService.enqueDeleteCountWork(requireContext(), item)
            }
            viewModel.repository.getCurrentLocationCounts().value?.remove(item)
            viewModel.repository.setCurrentLocationCounts(viewModel.repository.getCurrentLocationCounts().value!!)
        }
    }

    private fun onEditCountListener(count: ItemCount, pos: Int){
        //@drawable/round_corners
        Log.i(TAG, "click en item en la posicion $pos: $count")
        Log.i(TAG, "on edit count pos: $pos editing pos: $editingPosition")
        if(editingPosition != pos) {
            if(editingPosition > -1) {
                finishEditing(editingPosition)
            }
            editCount(count, pos)
        }
    }

    private fun onEditRecountTaskListener(itemCountTask: ItemCountTask, count: ItemCount?, pos: Int){
        //@drawable/round_corners
        Log.i(TAG, "click en item en la posicion $pos: $itemCountTask")
        Log.i(TAG, "on edit recount pos: $pos editing pos: $editingPosition")
        if(editingPosition != pos) {
            if(editingPosition > -1) {
                finishEditingRecountTask(editingPosition)
            }
            /*val count = currentLocationCounts.find {
                it.taskLineId == itemCountTask.taskLineId && it.itemId == itemCountTask.itemId
            }*/
            Log.i(TAG, "item count seleccionado: $count")
            if (count != null && count.dirty) {
                Utilities.showAlert(
                        requireContext(),
                        getString(R.string.warning),
                        getString(R.string.edit_recount_count_confirm_msg),
                        {
                            editRecountTask(itemCountTask, count, pos)
                        },
                        null
                )
            } else {
                editRecountTask(itemCountTask, count, pos)
            }
        }
    }

    private fun editCount(count: ItemCount, pos: Int){
        Log.i(TAG, "on editCountTask()...")
        Log.i(TAG, "pos: $pos, isEditing: $editingPosition")
        editingPosition = pos
        count.editing = true
        mAdapter?.notifyItemChanged(pos)
        binding?.editLayoutLabelTv?.text = getString(R.string.editing_count)
        binding?.finishEditingBtn?.visibility = View.VISIBLE
        binding?.editLy?.setBackgroundResource(R.drawable.round_corners)
        binding?.quantityEt?.isEnabled = true//task?.taskType == TaskType.Recount
        binding?.saveBtn?.isEnabled = true//task?.taskType == TaskType.Recount
        binding?.finishEditingBtn?.setOnClickListener{
            finishEditing(pos)
        }
        binding?.leftBtn?.isEnabled = false
        binding?.rightBtn?.isEnabled = false
        presentCount(count)
    }

    private fun editRecountTask(itemCountTask: ItemCountTask, count: ItemCount?, pos: Int){
        Log.i(TAG, "on editRecountTask()...")
        Log.i(TAG, "pos: $pos, isEditing: $editingPosition")
        /*this.currentLocationCounts.find{
            it.taskLineId == itemCountTask.taskLineId && it.itemId == itemCountTask.itemId
        }?.let{
            it.editing = true
            count = it
        }*/
        count?.editing = true
        editingPosition = pos
        itemCountTask.editing = true
        mRecountAdapter?.notifyItemChanged(pos)
        binding?.editLayoutLabelTv?.text = if(count != null){
            getString(R.string.editing_recount)
        } else {
            getString(R.string.adding_recount)
        }
        //currentLocationRecountTasks[pos].editing = false
        binding?.finishEditingBtn?.visibility = View.VISIBLE
        binding?.editLy?.setBackgroundResource(R.drawable.round_corners)
        binding?.quantityEt?.isEnabled = true //task?.taskType == TaskType.Recount
        binding?.saveBtn?.isEnabled = true //task?.taskType == TaskType.Recount
        binding?.finishEditingBtn?.setOnClickListener{
            finishEditingRecountTask(pos)
        }
        binding?.leftBtn?.isEnabled = false
        binding?.rightBtn?.isEnabled = false
        presentRecountTask(itemCountTask, count)
    }

    private fun finishEditing(position: Int){
        Log.i(TAG, "finishing... editing of count at position $position")
        activity?.runOnUiThread {
            currentLocationCounts[position].editing = false
            editingPosition = -1
            mAdapter?.notifyItemChanged(position)
            binding?.editLayoutLabelTv?.text = getString(R.string.select_a_recount)
            resetFormData()
            binding?.editLy?.setBackgroundResource(R.color.white)
            binding?.finishEditingBtn?.visibility = View.INVISIBLE
            binding?.leftBtn?.isEnabled = true
            binding?.rightBtn?.isEnabled = true
        }
    }

    private fun finishEditingRecountTask(position: Int){
        Log.i(TAG, "finishing... editing of recount at position $position")
        activity?.runOnUiThread {
            currentLocationRecountTasks[position].editing = false
            editingPosition = -1
            mRecountAdapter?.notifyItemChanged(position)
            binding?.editLayoutLabelTv?.text = getString(R.string.adding_new_count)
            resetFormData()
            binding?.editLy?.setBackgroundResource(R.color.white)
            binding?.finishEditingBtn?.visibility = View.INVISIBLE
            binding?.leftBtn?.isEnabled = true
            binding?.rightBtn?.isEnabled = true
        }
    }

    private fun initAdapter(){
        task?.let {
            if(it.taskType == TaskType.Inventory) {
                mAdapter = CountAdapter(
                    currentLocationCounts,
                    requireContext(),
                    this::onDeleteCountListener,
                    this::onEditCountListener
                )
                binding?.currentLocationCountRv?.layoutManager =
                    LinearLayoutManager(requireContext())
                binding?.currentLocationCountRv?.adapter = mAdapter
            } else if(it.taskType == TaskType.Recount){
                mRecountAdapter = RecountAdapter(
                    currentLocationRecountTasks,
                    requireContext(),
                    this::onDeleteCountListener,
                    this::onEditRecountTaskListener
                )
                binding?.currentLocationCountRv?.layoutManager =
                    LinearLayoutManager(requireContext())
                binding?.currentLocationCountRv?.adapter = mRecountAdapter
            }
        }
    }

    private fun resetFormData(){
        activity?.runOnUiThread {
            binding?.descriptionEt?.setText("")
            binding?.packagingTv?.text = ""
            binding?.quantityEt?.setText("")
            binding?.skuEt?.setText("")
            // cleaning values
            binding?.serialEt?.setText("")
            binding?.expiryDateEt?.setText("")
            binding?.lotEt?.setText("")
            binding?.createdDateEt?.setText("")
            binding?.lpnEt?.setText("")
            if(task?.taskType == TaskType.Recount){
                binding?.quantityEt?.isEnabled = false
                binding?.saveBtn?.isEnabled = false
            }
            binding?.skuEt?.requestFocus()
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
        binding?.skuEt?.setText(item?.sku ?: "")
        binding?.descriptionEt?.setText(((item?.sku ?: getString(R.string.without_sku)) + ": " + (item?.description ?: getString(R.string.without_description))).toUpperCase())
        if(item?.packaging != null && !item.primaryUnit.isNullOrBlank() && !item.secondaryUnit.isNullOrBlank()) {
            binding?.packagingTv?.visibility = View.VISIBLE
            binding?.packagingTv?.text = getString(R.string.packagin, item.primaryUnit
                    ?: "", item.packaging ?: 0, item.secondaryUnit ?: "")
        } else {
            binding?.packagingTv?.visibility = View.GONE
        }
        if(item?.requireDecimalQty == true){
            binding?.quantityEt?.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL.or(InputType.TYPE_CLASS_NUMBER))
        } else {
            binding?.quantityEt?.setInputType(InputType.TYPE_CLASS_NUMBER)
        }
        if(item == null){
            resetFormData()
        }
    }

    private fun presentCount(count: ItemCount?){
        Log.i(TAG, "presentando conteo: $count")
        binding?.descriptionEt?.setText(count?.description ?: "")
        binding?.packagingTv?.text = ""
        if(count == null){
            resetFormData()
        } else {
            binding?.skuEt?.setText(count.sku)
            binding?.quantityEt?.setText(count.quantity.toString())
            binding?.lotEt?.setText(count.lot)
            binding?.lpnEt?.setText(count.lpnCode)
            count.createdDate?.let{date ->
                if(isValidDate(date)) {
                    binding?.createdDateEt?.error = null
                    binding?.createdDateEt?.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            .format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    .parse(date)!!
                            )
                    )
                } else {
                    binding?.createdDateEt?.error = getString(R.string.invalid_date, date)
                }
            }
            count.expiryDate?.let {date ->
                if(isValidDate(date)) {
                    binding?.expiryDateEt?.error = null
                    val newDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            .format(
                                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                            .parse(date)!!
                            )
                    Log.i(TAG, "new formated date: $newDate")
                    binding?.expiryDateEt?.setText(newDate)
                } else {
                    binding?.expiryDateEt?.error = getString(R.string.invalid_date, date)
                }
            }
            binding?.serialEt?.setText(count.serial)
            count.itemId?.let { itemId ->
                doAsync {
                    db.itemDao().getById(itemId).let {
                        viewModel.repository.setCurrentItem(it)
                    }
                }
            }
        }
    }

    private fun presentRecountTask(countTask: ItemCountTask, count: ItemCount?){
        Log.i(TAG, "presentando conteo...")
        var item: Item? = null
        doAsync {
            item = db.itemDao().getById(countTask.itemId)
            viewModel.repository.setCurrentItem(item)
            if (item != null) {
                uiThread {
                    binding?.quantityEt?.setText((count?.quantity ?: 0).toString())
                    binding?.lotEt?.setText(countTask.lot ?: "")
                    binding?.lpnEt?.setText(countTask.lpnCode ?: "")
                    countTask.createdDate?.let { date ->
                        if (isValidDate(date)) {
                            binding?.createdDateEt?.setText(
                                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                    .format(
                                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                            .parse(date)!!
                                    )
                            )
                        }
                    }
                    countTask.expiryDate?.let { date ->
                        if (isValidDate(date)) {
                            val newDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                    .format(
                                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                                    .parse(date)!!
                                    )
                            Log.i(TAG, "new formated date: $newDate")
                            binding?.expiryDateEt?.error = null
                            binding?.expiryDateEt?.setText(
                                newDate
                            )
                        } else {
                            binding?.expiryDateEt?.error = getString(R.string.invalid_date, date)
                        }
                    }
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
        val  barcode = data?.getStringExtra("barcode") ?: ""
        if(resultCode == RESULT_OK && requestCode == BARCODE_SCANNER){
            searchItemBySku(barcode)
        } else if(resultCode == RESULT_OK && requestCode == BARCODE_SCANNER_LPN){
            binding?.lpnEt?.setText(barcode)
        } else if(resultCode == RESULT_OK && requestCode == BARCODE_SCANNER_LOT){
            binding?.lotEt?.setText(barcode)
        } else if(resultCode == RESULT_OK && requestCode == CALCULATOR){
            val result = data?.getDoubleExtra("result", 0.0) ?: 0.0
            binding?.quantityEt?.setText(result.toString())
        } else if (resultCode != RESULT_OK && (requestCode == BARCODE_SCANNER || requestCode == BARCODE_SCANNER_LPN || requestCode == BARCODE_SCANNER_LOT)){
            showToast(requireContext(), getString(R.string.barcode_scanner_error))
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

    override fun onPause() {
        Log.i(TAG, "onPause()...")
        super.onPause()
    }

    override fun onDismiss(dialog: DialogInterface) {
        Log.i(TAG, "onDismiss()...")
        task?.let {
            changeTaskState(it, TaskState.Paused)
        }
        super.onDismiss(dialog)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val TAG = "TASK_COUNT_DIALOG"
        private const val BARCODE_SCANNER = 99
        private const val BARCODE_SCANNER_LOT = 100
        private const val BARCODE_SCANNER_LPN = 101
        private const val CALCULATOR = 102

        fun display(fragmentManager: FragmentManager, task: Task): TaskCountDialog? {
            val dialog = TaskCountDialog(task)
            dialog.show(fragmentManager, TAG)
            return dialog
        }
    }
}