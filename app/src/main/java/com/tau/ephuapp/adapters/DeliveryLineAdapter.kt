package com.tau.ephuapp.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tau.ephuapp.models.DeliveryLine
import com.tau.ephuapp.R
import com.tau.ephuapp.databinding.ScannedItemBinding
import java.util.*

class DeliveryLineAdapter(private val dataList: MutableList<DeliveryLine>, val context: Context, val readed: Boolean = true) :
    RecyclerView.Adapter<DeliveryLineAdapter.MyViewHolder>() {

    class MyViewHolder(val itemView: View, val readed: Boolean): RecyclerView.ViewHolder(itemView) {
        val binding = ScannedItemBinding.bind(itemView)
        fun bindItems(deliveryLine: DeliveryLine) {
            binding.skuDescriptionTv.text = deliveryLine.description
            binding.deliveryBtn.text = "${itemView.context.getString(R.string.delivery)} ${deliveryLine.deliveryId}"
            binding.transactionNumberTv.text = deliveryLine.reference
            binding.timestampTv.text = Date().toString()
            if (readed) {
                binding.doneImage.visibility = View.VISIBLE
                binding.quantitiesTv.visibility = View.VISIBLE
            } else {
                binding.doneImage.visibility = View.GONE
                binding.quantitiesTv.visibility = View.GONE
            }
            binding.quantitiesTv.text = "${deliveryLine.scannedOrder}/${deliveryLine.quantity}"
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