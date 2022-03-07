package com.tau.ephuapp.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getColorStateList
import androidx.core.content.ContextCompat.getDrawable
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.tau.ephuapp.R
import com.tau.ephuapp.classes.Utilities
import com.tau.ephuapp.databinding.CountExtendedCardItemBinding
import com.tau.ephuapp.models.ItemCount
import kotlin.collections.ArrayList

class CountExtendedAdapter(
    _list: ArrayList<ItemCount>,
    _context: Context,
    _onDeleteCallback: ((ItemCount, Int) -> Unit)? = null,
    _onEditCallback: ((ItemCount, Int) -> Unit)? = null
) : RecyclerView.Adapter<CountExtendedAdapter.TaskHolder>() {
    private var list: ArrayList<ItemCount> = _list
    private var context: Context = _context
    private var onDeleteCallback: ((ItemCount, Int) -> Unit)? = _onDeleteCallback
    private var onEditCallback: ((ItemCount, Int) -> Unit)? = _onEditCallback

    class TaskHolder(val binding: CountExtendedCardItemBinding, val onDeleteCallback: ((ItemCount, Int) -> Unit)? = null,
                     onEditCallback: ((ItemCount, Int) -> Unit)? = null): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskHolder {
        val binding = CountExtendedCardItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskHolder(binding, onDeleteCallback, onEditCallback)
    }

    override fun onBindViewHolder(holder: TaskHolder, position: Int) {
        with(holder){
            with(list[position]){
                if(uploaded) {
                    if(dirty) {
                        Log.i(TAG, "el conteo $id en la posicion $position ha sido modificado")
                        binding.itemCard.setCardBackgroundColor(
                            ContextCompat.getColor(
                                context,
                                R.color.design_default_color_secondary
                            )
                        )
                    } else {
                        binding.itemCard.setCardBackgroundColor(MaterialColors.getColor(binding.itemCard, R.attr.cardForegroundColor))
                    }
                } else {
                    Log.i(TAG, "el conteo $id en la posicion $position esta normal")
                    binding.itemCard.setCardBackgroundColor(MaterialColors.getColor(binding.itemCard, R.attr.cardForegroundColor))
                }
                binding.taskIdTv.text = taskId.toString()
                binding.skuTv.text = sku ?: ""
                binding.descriptionTv.text = description ?: ""
                binding.quantityTv.text = (quantity).toString()
                binding.statusBtn.visibility = View.GONE
                if(hasError == true || (recount && dirty) || uploaded){
                    if(hasError == false && ((recount && dirty) || uploaded)){
                        if (uploaded) {
                            binding.statusBtn.setImageDrawable(
                                getDrawable(
                                    context,
                                    R.drawable.ic_baseline_cloud_done_24
                                )
                            )
                            if(recount){
                                binding.statusBtn.setOnClickListener {
                                    Utilities.showAlert(
                                        context,
                                        context.getString(R.string.information),
                                        context.getString(R.string.recount_already_uploaded_msg)
                                    )
                                }
                            } else {
                                binding.statusBtn.setOnClickListener {
                                    Utilities.showAlert(
                                        context,
                                        context.getString(R.string.information),
                                        context.getString(R.string.count_already_uploaded_msg)
                                    )
                                }
                            }
                            binding.statusBtn.visibility = View.VISIBLE
                        } else if(!uploaded && recount && dirty){
                            binding.statusBtn.setImageDrawable(
                                getDrawable(
                                    context,
                                    R.drawable.ic_baseline_check_circle_24
                                )
                            )
                            binding.statusBtn.setOnClickListener {
                                Utilities.showAlert(
                                    context,
                                    context.getString(R.string.information),
                                    context.getString(R.string.recount_already_made_msg)
                                )
                            }
                            binding.statusBtn.visibility = View.VISIBLE
                        }
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            binding.statusBtn.foregroundTintList = getColorStateList(context, R.color.cp_state)
                        }
                    } else if (hasError == true){
                        binding.statusBtn.setImageDrawable(getDrawable(context, R.drawable.ic_baseline_cancel_24))
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            binding.statusBtn.foregroundTintList = getColorStateList(context, R.color.pe_cancelled)
                        }
                        binding.statusBtn.setOnClickListener {
                            Utilities.showAlert(context, context.getString(R.string.error), errorMessage ?: context.getString(R.string.unknown_error))
                        }
                        binding.statusBtn.visibility = View.VISIBLE
                    }
                } else {
                    binding.statusBtn.visibility = View.GONE
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

    fun getItemList(): List<ItemCount>{
        return list
    }

    companion object{
        val TAG = "COUNT_ADAPTER"
    }
}