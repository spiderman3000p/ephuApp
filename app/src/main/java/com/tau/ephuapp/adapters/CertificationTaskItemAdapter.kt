package com.tau.ephuapp.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tau.ephuapp.R
import com.tau.ephuapp.databinding.ScannedItemBinding
import com.tau.ephuapp.models.CertificationTaskItem
import java.util.*

class CertificationTaskItemAdapter(private val dataList: MutableList<CertificationTaskItem>, val context: Context, val readed: Boolean = true) :
    RecyclerView.Adapter<CertificationTaskItemAdapter.MyViewHolder>() {

    class MyViewHolder(val itemView: View, val readed: Boolean): RecyclerView.ViewHolder(itemView) {
        val binding = ScannedItemBinding.bind(itemView)
        fun bindItems(item: CertificationTaskItem) {
            binding.skuDescriptionTv.text = item.itemDescription
            binding.deliveryBtn.text = "${itemView.context.getString(R.string.sku)} ${item.itemSku}"
            binding.transactionNumberTv.text = item.taskId.toString()
            binding.timestampTv.text = Date().toString()
            if (readed) {
                binding.doneImage.visibility = View.VISIBLE
            } else {
                binding.doneImage.visibility = View.GONE
            }
            binding.quantitiesTv.text = "${item.totalQuantity}/${item.totalUnits}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.scanned_item, parent, false)
        return MyViewHolder(view, readed)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bindItems(dataList[position])
    }

    override fun getItemCount(): Int {
        return dataList.size
    }
}