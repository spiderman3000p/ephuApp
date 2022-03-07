package com.tau.ephuapp.adapters

import android.content.Context
import android.database.sqlite.SQLiteAccessPermException
import android.database.sqlite.SQLiteCantOpenDatabaseException
import android.database.sqlite.SQLiteDatabaseLockedException
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getColorStateList
import androidx.core.content.ContextCompat.getDrawable
import androidx.recyclerview.widget.RecyclerView
import com.tau.ephuapp.R
import com.tau.ephuapp.classes.Utilities
import com.tau.ephuapp.database.AppDatabase
import com.tau.ephuapp.databinding.CountCardItemBinding
import com.tau.ephuapp.models.ItemCount
import com.tau.ephuapp.models.ItemCountTask
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import kotlin.collections.ArrayList

class RecountAdapter(
    _list: ArrayList<ItemCountTask>,
    _context: Context,
    _onDeleteCallback: ((ItemCount, Int) -> Unit)? = null,
    _onEditCallback: ((ItemCountTask, ItemCount?, Int) -> Unit)? = null
) : RecyclerView.Adapter<RecountAdapter.TaskHolder>() {
    private var list: ArrayList<ItemCountTask> = _list
    private var context: Context = _context
    private var onDeleteCallback: ((ItemCount, Int) -> Unit)? = _onDeleteCallback
    private var onEditCallback: ((ItemCountTask, ItemCount?, Int) -> Unit)? = _onEditCallback
    private lateinit var db: AppDatabase

    init {
        try {
            db = AppDatabase.getDatabase(context)
        } catch (ex: SQLiteDatabaseLockedException) {
            Log.e(TAG, "Database error found", ex)
        } catch (ex: SQLiteAccessPermException) {
            Log.e(TAG, "Database error found", ex)
        } catch (ex: SQLiteCantOpenDatabaseException) {
            Log.e(TAG, "Database error found", ex)
        }
    }

    class TaskHolder(val binding: CountCardItemBinding, val onDeleteCallback: ((ItemCount, Int) -> Unit)? = null,
                     onEditCallback: ((ItemCountTask, ItemCount, Int) -> Unit)? = null): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskHolder {
        val binding = CountCardItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskHolder(binding, onDeleteCallback, onEditCallback)
    }

    override fun onBindViewHolder(holder: TaskHolder, position: Int) {
        with(holder){
            list[position].let{itemCountTask ->
                //Log.i(TAG, "onBindViewHolder recount task: $itemCountTask")
                doAsync {
                    val _itemCount = db.itemCountDao().getByTaskLineAndItem(itemCountTask.taskLineId, itemCountTask.itemId)
                    val item = db.itemDao().getById(itemCountTask.itemId)
                    if (itemCountTask.editing) {
                        /*Log.i(
                            TAG,
                            "el reconteo ${itemCountTask.taskLineId} en la posicion $position esta siendo editado"
                        )*/
                        uiThread {
                            binding.deleteBtn.visibility = View.GONE
                            binding.itemCard.setCardBackgroundColor(
                                ContextCompat.getColor(
                                    context,
                                    R.color.editing_count
                                )
                            )
                        }
                    } else {
                        binding.itemCard.setCardBackgroundColor(
                            ContextCompat.getColor(
                                context,
                                R.color.design_default_color_background
                            )
                        )
                        if(_itemCount != null){
                            uiThread {
                                if (_itemCount.uploaded) {
                                    binding.deleteBtn.visibility = View.VISIBLE
                                } else {
                                    binding.deleteBtn.visibility = View.GONE
                                }
                                if (_itemCount.uploaded && _itemCount.dirty) {
                                    Log.i(
                                        TAG,
                                        "el reconteo ${_itemCount.id} en la posicion $position ha sido modificado"
                                    )
                                    binding.itemCard.setCardBackgroundColor(
                                        ContextCompat.getColor(
                                            context,
                                            R.color.teal_201
                                        )
                                    )
                                }
                            }
                        } else {
                            uiThread {
                                binding.deleteBtn.visibility = View.GONE
                            }
                        }
                    }
                    uiThread {
                        binding.quantityTv.text = (_itemCount?.quantity ?: 0).toString()
                        binding.statusBtn.visibility = View.GONE
                        binding.skuTv.text = item?.sku ?: ""
                        binding.descriptionTv.text = item?.description ?: ""
                        binding.itemCard.setOnClickListener {
                            onEditCallback?.let {
                                Log.i(
                                    TAG,
                                    "click realizado en item en la posicion $position: $_itemCount"
                                )
                                if (!itemCountTask.editing && (_itemCount == null || !_itemCount.uploaded)) {
                                    it(itemCountTask, _itemCount, position)
                                }
                            }
                        }
                        _itemCount?.let { itemCount ->
                            if (itemCount.hasError == true || itemCount.dirty || itemCount.uploaded) {
                                if (itemCount.hasError != true && (itemCount.recount || itemCount.dirty || itemCount.uploaded)) {
                                    //Log.i(TAG, "el reconteo ${itemCount.id} no tiene error")
                                    if (itemCount.uploaded) {
                                        //Log.i(TAG, "el reconteo ${itemCount.id} ha sido subido")
                                        binding.statusBtn.setImageDrawable(
                                            getDrawable(
                                                context,
                                                R.drawable.ic_baseline_cloud_done_24
                                            )
                                        )
                                        if (itemCount.recount) {
                                            Log.i(TAG, "el reconteo ${itemCount.id} es un reconteo")
                                            binding.statusBtn.setOnClickListener {
                                                Utilities.showAlert(
                                                    context,
                                                    context.getString(R.string.information),
                                                    context.getString(R.string.recount_already_uploaded_msg)
                                                )
                                            }
                                        } else {
                                            Log.i(
                                                TAG,
                                                "el reconteo ${itemCount.id} no es un reconteo"
                                            )
                                            binding.statusBtn.setOnClickListener {
                                                Utilities.showAlert(
                                                    context,
                                                    context.getString(R.string.information),
                                                    context.getString(R.string.count_already_uploaded_msg)
                                                )
                                            }
                                        }
                                        binding.statusBtn.visibility = View.VISIBLE
                                    } else if (itemCount.recount && itemCount.dirty) {
                                        Log.i(
                                            TAG,
                                            "el reconteo ${itemCount.id} no ha sido subido y es un reconteo modificado"
                                        )
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
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                            binding.statusBtn.foregroundTintList =
                                                getColorStateList(context, R.color.cp_state)
                                        }
                                        binding.statusBtn.visibility = View.VISIBLE
                                    }
                                } else if (itemCount.hasError == true) {
                                    Log.i(TAG, "el reconteo ${itemCount.id} tiene un error")
                                    binding.statusBtn.setImageDrawable(
                                        getDrawable(
                                            context,
                                            R.drawable.ic_baseline_cancel_24
                                        )
                                    )
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                        binding.statusBtn.foregroundTintList =
                                            getColorStateList(context, R.color.pe_cancelled)
                                    }
                                    binding.statusBtn.setOnClickListener {
                                        Utilities.showAlert(
                                            context,
                                            context.getString(R.string.error),
                                            itemCount.errorMessage
                                                ?: context.getString(R.string.unknown_error)
                                        )
                                    }
                                    binding.statusBtn.visibility = View.VISIBLE
                                }
                            } else {
                                binding.statusBtn.visibility = View.GONE
                            }
                            binding.statusBtn.setOnClickListener {
                                if (itemCount.hasError == true) {
                                    Utilities.showAlert(
                                        context,
                                        context.getString(R.string.error),
                                        itemCount.errorMessage
                                            ?: context.getString(R.string.unknown_error)
                                    )
                                }
                            }
                            binding.deleteBtn.setOnClickListener {
                                if (!itemCount.editing) {
                                    Utilities.showAlert(
                                        context,
                                        context.getString(R.string.confirmation),
                                        context.getString(R.string.delete_count_confirm_msg),
                                        {
                                            onDeleteCallback?.let {
                                                it(itemCount, position)
                                            }
                                        }
                                    )
                                } else {
                                    Utilities.showToast(
                                        context,
                                        context.getString(R.string.you_are_editing_error)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun getItemAtPosition(position: Int): ItemCountTask?{
        return list.getOrNull(position)
    }

    companion object{
        const val TAG = "RECOUNT_ADAPTER"
    }
}