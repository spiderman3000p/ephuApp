package com.tau.ephuapp.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.tau.ephuapp.R
import com.tau.ephuapp.classes.Utilities
import com.tau.ephuapp.databinding.CountCardItemBinding
import com.tau.ephuapp.models.ItemCount
import kotlin.collections.ArrayList

class CountAdapter(
    _list: ArrayList<ItemCount>,
    _context: Context,
    _onDeleteCallback: ((ItemCount, Int) -> Unit)? = null,
    _onEditCallback: ((ItemCount, Int) -> Unit)? = null
) : RecyclerView.Adapter<CountAdapter.TaskHolder>() {
    private var list: ArrayList<ItemCount> = _list
    private var context: Context = _context
    private var onDeleteCallback: ((ItemCount, Int) -> Unit)? = _onDeleteCallback
    private var onEditCallback: ((ItemCount, Int) -> Unit)? = _onEditCallback
    //var isEditing = -1
    val TAG = "COUNT_ADAPTER"

    class TaskHolder(val binding: CountCardItemBinding, val onDeleteCallback: ((ItemCount, Int) -> Unit)? = null,
                     onEditCallback: ((ItemCount, Int) -> Unit)? = null): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskHolder {
        val binding = CountCardItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskHolder(binding, onDeleteCallback, onEditCallback)
    }

    override fun onBindViewHolder(holder: TaskHolder, position: Int) {
        with(holder){
            with(list[position]){
                if(editing){
                    binding.itemCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.editing_count))
                } else {
                    if(uploaded == true && dirty == true) {
                        binding.itemCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.design_default_color_secondary))
                    } else if (uploaded == false && dirty == false){
                        binding.itemCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
                    }
                }
                binding.skuTv.text = sku ?: ""
                binding.descriptionTv.text = description ?: ""
                binding.quantityTv.text = (quantity ?: 0).toString()
                binding.deleteBtn.setOnClickListener {
                    if (!editing) {
                        Utilities.showAlert(
                                context,
                                context.getString(R.string.confirmation),
                                context.getString(R.string.delete_count_confirm_msg),
                                {
                                    onDeleteCallback?.let {
                                        it(this, position)
                                    }
                                }
                        )
                    } else {
                        Utilities.showToast(context, context.getString(R.string.you_are_editing_error))
                    }
                }
                binding.itemCard.setOnClickListener {
                    onEditCallback?.let{
                        Log.i(TAG, "click realizado en item en la pocision $position: ${list[position]}")
                        if(!editing){
                            //isEditing = position
                            editing = true
                            //notifyItemChanged(position)
                            //notifyDataSetChanged()
                            it(this, position)
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun getItemAtPosition(position: Int): ItemCount?{
        return list.getOrNull(position)
    }
}