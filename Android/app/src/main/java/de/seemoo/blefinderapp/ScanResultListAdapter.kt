package de.seemoo.blefinderapp

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.devicelist_item_row.view.*

class ScanResultListAdapter(val clickCallback: (bt:BluetoothDevice) -> Unit) :
        RecyclerView.Adapter<ScanResultListAdapter.Holder>()  {
    private val values = ArrayList<ScanResult>()
    private val macAddrs = HashMap<String,Int>()

    override fun getItemCount() = values.size

    fun add(item : ScanResult) {
        val index = macAddrs[item.device.address]
        index?.let {
            values[index] = item
            this.notifyItemChanged(index)
            return
        }

        values.add(item)
        macAddrs.put(item.device.address, values.size-1)

        this.notifyItemInserted(values.size-1)
    }

    fun getServiceData( rec: ScanRecord) :String{
        var out = ""
        var msd = rec.manufacturerSpecificData
        for(i in 0.. msd.size()-1) {
            val key = msd.keyAt(i);
            out += "$key: " + Helper.bytesToHex(msd.get(key))+ ", ";
        }

        return out
    }

    override fun onBindViewHolder(holder: ScanResultListAdapter.Holder, position: Int) {
        val item = values[position]
        holder.descView.text = item.device.address
        if (item.device.name != null && item.device.name != "")
            holder.nameView.text = item.device.name
        else
            holder.nameView.text = "(unnamed)"
        holder.additionalView.text = "RSSI=${item.rssi} | DevName=${item.scanRecord?.deviceName} | AdvFlags=${item.scanRecord?.advertiseFlags} | MfgSD=${getServiceData(item.scanRecord!!)} | SD=${item.scanRecord?.serviceData} | UUIDs=${item.scanRecord?.serviceUuids} | TXPwr=${item.scanRecord?.txPowerLevel}"
        holder.item = item
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder{
        val view = LayoutInflater.from(parent.context).inflate(R.layout.devicelist_item_row, parent, false)
        return Holder(view)
    }

    inner class Holder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        val nameView: TextView = view.itemName
        val descView: TextView = view.itemDescription
        val additionalView: TextView = view.itemAdditional
        lateinit var item: ScanResult
        init {
            view.setOnClickListener(this)
        }
        override fun onClick(v: View) {
            Log.d("RecyclerView", "CLICK!"+item.device.address)
            clickCallback(item.device)
        }
    }

}