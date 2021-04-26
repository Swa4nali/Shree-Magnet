package com.s2labs.shreemagnetfota.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.s2labs.shreemagnetfota.R
import com.s2labs.shreemagnetfota.databinding.BluetoothDeviceItemBinding

class DeviceListAdapter(
	var clickListener: OnClickListener? = null
) : ListAdapter<DeviceListAdapter.DeviceItem, DeviceListAdapter.ViewHolder>(DiffCallback()) {

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.bluetooth_device_item, parent, false))
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val item = getItem(position)

		holder.binding?.data = item
		holder.binding?.root?.setOnClickListener {
			clickListener?.onClick(item, position)
		}
	}


	class DiffCallback : DiffUtil.ItemCallback<DeviceItem>() {
		override fun areItemsTheSame(oldItem: DeviceItem, newItem: DeviceItem): Boolean {
			return oldItem.mac == newItem.mac
		}

		override fun areContentsTheSame(oldItem: DeviceItem, newItem: DeviceItem): Boolean {
			return oldItem == newItem
		}
	}

	class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
		val binding: BluetoothDeviceItemBinding? = DataBindingUtil.bind(v)
	}

	data class DeviceItem(
		val name: String,
		val mac: String
	)

	interface OnClickListener {
		fun onClick(item: DeviceItem, position: Int)
	}
}