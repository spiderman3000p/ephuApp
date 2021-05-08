package com.tau.ephuapp.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.tau.ephuapp.R
import com.tau.ephuapp.classes.Utilities
import com.tau.ephuapp.databinding.TaskCardItemBinding
import com.tau.ephuapp.models.Task
import com.tau.ephuapp.models.TaskState
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class TaskAdapter(private val tasksList: ArrayList<Task>, val context: Context, val listener: ((View, Int) -> Unit)? = null) :
    RecyclerView.Adapter<TaskAdapter.TaskHolder>() {
    class TaskHolder(val binding: TaskCardItemBinding, val popupListener: ((View, Int) -> Unit)? = null): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskHolder {
        val binding = TaskCardItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: TaskHolder, position: Int) {
        with(holder){
            with(tasksList[position]){
                binding.taskNumberTv.text = context.getString(R.string.task_title, id, count)
                binding.taskResumeLineTv.text = context.getString(R.string.task_resume, lines,
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(totalTime))
                binding.taskStateTv.text = Utilities.getResumedState(taskState)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    binding.taskStateTv.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context,
                        Utilities.getStateColor(taskState)))
                } else {
                    binding.taskStateTv.setBackgroundColor(Utilities.getStateColor(taskState))
                }
                if(taskState != TaskState.Complete) {
                    popupListener?.let {
                        binding.optionsBtn.setOnClickListener { view ->
                            it(view, position)
                        }
                    }
                } else {
                    binding.optionsBtn.visibility = View.GONE
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return tasksList.size
    }

    fun getItemAtPosition(position: Int): Task{
        return tasksList[position]
    }
}