package com.tau.ephuapp.activities.main.ui.tasks

import android.database.sqlite.SQLiteAccessPermException
import android.database.sqlite.SQLiteCantOpenDatabaseException
import android.database.sqlite.SQLiteDatabaseLockedException
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.view.*
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.WorkInfo
import com.tau.ephuapp.R
import com.tau.ephuapp.activities.main.MainActivityViewModel
import com.tau.ephuapp.adapters.TaskAdapter
import com.tau.ephuapp.classes.Constants
import com.tau.ephuapp.classes.Utilities
import com.tau.ephuapp.database.AppDatabase
import com.tau.ephuapp.databinding.FragmentTasksBinding
import com.tau.ephuapp.dialogs.TaskCountDialog
import com.tau.ephuapp.interfaces.PopupMenuListener
import com.tau.ephuapp.models.Task
import com.tau.ephuapp.models.TaskState
import com.tau.ephuapp.models.TaskType
import com.tau.ephuapp.services.MyWorkerManagerService
import org.jetbrains.anko.doAsync


class TasksFragment : Fragment(), PopupMenuListener {
    private val TAG = "TASKS_FRAGMENT"
    private lateinit var viewModel: MainActivityViewModel
    private var mAdapter: TaskAdapter? = null
    private var filteredData = arrayListOf<Task>()
    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase
    //private lateinit var taskType: TaskType
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        binding.titleTv.text = getString(R.string.task_list_title, DateUtils.formatDateTime(context, System.currentTimeMillis(), 0), 0)
        val taskType = null//TaskType.valueOf(arguments?.getString("taskType")!!)
        activity?.actionBar?.title = when(taskType){
            TaskType.Inventory -> getString(R.string.menu_inventory_tasks)
            TaskType.Recount -> getString(R.string.menu_recount_tasks)
            else -> getString(R.string.menu_tasks)
        }
        val _viewModel: MainActivityViewModel by activityViewModels()
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
        //viewModel = ViewModelProvider(this).get(TasksViewModel::class.java)
        viewModel.tasksList.observe(viewLifecycleOwner, Observer { tasks ->
            Log.i(TAG, "tareas observadas: $tasks")
            filteredData.clear()
            if(!tasks.isNullOrEmpty()) {
                filteredData.addAll(tasks)
            }
            binding.titleTv.text = getString(R.string.task_list_title, DateUtils.formatDateTime(context, System.currentTimeMillis(), 0), filteredData.size)
            mAdapter?.notifyDataSetChanged()
        })
        viewModel.savingCountsWorkProgress.observe(viewLifecycleOwner, {
            it.forEach { workInfo ->
                if (WorkInfo.State.ENQUEUED == workInfo.state) {
                    Log.i(TAG, "progreso de subida de conteos observado...trabajo encolado")
                    binding.progressBar3.visibility = View.VISIBLE
                    //Utilities.showToast(requireContext(), getString(R.string.uploading_counts))
                } else {
                    binding.progressBar3.visibility = View.INVISIBLE
                }
                if (WorkInfo.State.SUCCEEDED == workInfo.state) {
                    Log.i(TAG, "progreso de subida de conteos observado...trabajo finalizado con exito")
                    var msg = getString(R.string.counts_uploaded_successfully)
                    if(workInfo.outputData.hasKeyWithValueOfType("exception", String::class.java)){
                        msg = workInfo.outputData.getString("exception").toString()
                    } else if (workInfo.outputData.hasKeyWithValueOfType("error", String::class.java)) {
                        msg = workInfo.outputData.getString("error").toString()
                    }
                    //Utilities.showToast(requireContext(), msg)
                }
                if (WorkInfo.State.FAILED == workInfo.state) {
                    Log.i(TAG, "progreso de subida de conteos observado...trabajo finalizado con error")
                    var msg = getString(R.string.error_uploading_counts)
                    if(workInfo.outputData.hasKeyWithValueOfType("exception", String::class.java)){
                        msg = workInfo.outputData.getString("exception").toString()
                    } else if (workInfo.outputData.hasKeyWithValueOfType("error", String::class.java)) {
                        msg = workInfo.outputData.getString("error").toString()
                    }
                    //Utilities.showToast(requireContext(), msg)
                }
                if (WorkInfo.State.CANCELLED == workInfo.state) {
                    Log.i(TAG, "progreso de subida de conteos observado...trabajo cancelado")
                    //Utilities.showToast(requireContext(), getString(R.string.counts_uploading_cancelled))
                }
            }
        })
        viewModel.changingTaskStateWorkProgress.observe(viewLifecycleOwner, {
            it.forEach { workInfo ->
                if (WorkInfo.State.ENQUEUED == workInfo.state) {
                    Log.i(TAG, "progreso de cambio de estado de tarea observado...trabajo encolado")
                    binding.progressBar3.visibility = View.VISIBLE
                    //Utilities.showToast(requireContext(), getString(R.string.changing_task_state))
                } else {
                    binding.progressBar3.visibility = View.INVISIBLE
                }
                if (WorkInfo.State.SUCCEEDED == workInfo.state) {
                    Log.i(TAG, "progreso de cambio de estado de tarea observado...trabajo finalizado con exito")
                    var msg = ""
                    if(workInfo.outputData.hasKeyWithValueOfType("exception", String::class.java)){
                        msg = workInfo.outputData.getString("exception").toString()
                    } else if (workInfo.outputData.hasKeyWithValueOfType("error", String::class.java)) {
                        msg = workInfo.outputData.getString("error").toString()
                    } else {
                        msg = getString(R.string.state_changed_successfully)
                    }
                    if(msg.isNotEmpty()){
                        //Utilities.showToast(requireContext(), msg)
                    }
                }
                if (WorkInfo.State.FAILED == workInfo.state) {
                    Log.i(TAG, "progreso de cambio de estado de tarea observado...trabajo finalizado con error")
                    var msg = getString(R.string.error_changing_task_state)
                    if(workInfo.outputData.hasKeyWithValueOfType("exception", String::class.java)){
                        msg = workInfo.outputData.getString("exception").toString()
                    } else if (workInfo.outputData.hasKeyWithValueOfType("error", String::class.java)) {
                        msg = workInfo.outputData.getString("error").toString()
                    }
                    //Utilities.showToast(requireContext(), msg)
                }
                if (WorkInfo.State.CANCELLED == workInfo.state) {
                    Log.i(TAG, "progreso de cambio de estado de tarea observado...trabajo cancelado")
                    //Utilities.showToast(requireContext(), getString(R.string.state_change_cancelled))
                }
            }
        })
        initAdapter()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    private fun initAdapter(){
        mAdapter = TaskAdapter(filteredData, requireContext(), this::onPopupMenuClick)
        binding.tasksRv.layoutManager = LinearLayoutManager(requireContext())
        binding.tasksRv.adapter = mAdapter
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onPopupMenuClick(view: View, position: Int) {
        val task = mAdapter?.getItemAtPosition(position)
        Log.i(TAG, "tarea seleccionada $task en la posicion $position")
        val popup = PopupMenu(requireContext(), view)
        val inflater: MenuInflater = popup.menuInflater
        inflater.inflate(R.menu.task_popup_menu, popup.menu)
        when(task?.taskState){
            TaskState.Paused -> { // solo sale la opcion de reanudar
                popup.menu.removeItem(R.id.start)
                popup.menu.removeItem(R.id.pause)
                popup.menu.removeItem(R.id.finalize)
                popup.menu.removeItem(R.id.cancel)
            }
            TaskState.WorkInProgress -> { // solo salen las opciones de pausar y completar
                popup.menu.removeItem(R.id.start)
                //popup.menu.removeItem(R.id.resume) // TODO: remover cuando se compruebe que todo el flujo de estados funcione bien
                popup.menu.removeItem(R.id.cancel)
                popup.menu.removeItem(R.id.finalize) // TODO: remover esto cuando pasar a complete este disponible
            }
            TaskState.Complete -> { // no sale ninguna opcion
                popup.menu.removeItem(R.id.start)
                popup.menu.removeItem(R.id.resume)
                popup.menu.removeItem(R.id.pause)
                popup.menu.removeItem(R.id.finalize)
                popup.menu.removeItem(R.id.cancel)
            }
            TaskState.Pending -> {// solo salen las opciones de iniciar y cancelar
                popup.menu.removeItem(R.id.finalize)
                popup.menu.removeItem(R.id.resume)
                popup.menu.removeItem(R.id.pause)
            }
            TaskState.Active -> { // sola sale la opcion de iniciar
                popup.menu.removeItem(R.id.resume)
                popup.menu.removeItem(R.id.pause)
                popup.menu.removeItem(R.id.finalize)
                popup.menu.removeItem(R.id.cancel)
            }
            TaskState.Cancelled -> { // no sale ninguna opcion
                popup.menu.removeItem(R.id.start)
                popup.menu.removeItem(R.id.resume)
                popup.menu.removeItem(R.id.pause)
                popup.menu.removeItem(R.id.finalize)
                popup.menu.removeItem(R.id.cancel)
            }
        }
        popup.setOnMenuItemClickListener { item -> //do your things in each of the following cases
            when (item.itemId) {
                R.id.start -> {
                    /*startActivityForResult(
                        Intent(requireContext(), CountActivity::class.java).putExtra("task", task),
                        COUNT_STARTED
                    )*/
                    task?.let{
                        if(changeTaskState(position,task,TaskState.WorkInProgress)) {
                            showTaskCountDialog(it)
                        }
                    }
                    true
                }
                R.id.pause -> {
                    task?.let {
                        changeTaskState(position, task, TaskState.Paused)
                    }
                    true
                }
                R.id.resume -> {
                    task?.let{
                        if(changeTaskState(position,task,TaskState.WorkInProgress)) {
                            showTaskCountDialog(it)
                        }
                    }
                    true
                }
                R.id.finalize -> true
                else -> false
            }
        }
        popup.show()
    }

    private fun changeTaskState(position: Int, task: Task, state: TaskState): Boolean {
        when(state){
            TaskState.Paused -> {
                if(task.taskState != TaskState.WorkInProgress) {
                    Utilities.showAlert(requireContext(), getString(R.string.error), getString(R.string.error_trying_to_pause_task))
                    return false
                }
            }
            TaskState.WorkInProgress -> {
                if(task.taskState == TaskState.WorkInProgress || task.taskState != TaskState.Active && task.taskState != TaskState.Paused) {
                    Utilities.showAlert(requireContext(), getString(R.string.error), getString(R.string.error_trying_to_init_resume))
                    return false
                }
            }
        }
        MyWorkerManagerService.enqueChangeTaskStateWork(requireContext(), task.id, state, Constants.CHANGIN_TASK_STATUS_PROGRESS)
        task.taskState = state
        mAdapter?.notifyItemChanged(position)
        doAsync {
            db.tasksDao().update(task)
        }
        return true
    }

    fun showTaskCountDialog(task: Task){
        TaskCountDialog.display(requireFragmentManager(), task)
    }
}