package de.seemoo.blefinderapp

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.location.Location
import android.util.Log
import android.widget.Toast
import de.seemoo.blefinderapp.db.KnownDevice
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


private const val TAG="FoundDevice"
class FoundDevice(val parent: Context, val btDev : BluetoothDevice, val location: Location) : WiederFinderCallback {

	val address: String = btDev.address
	var manager = WiederFinder(parent)

	init {
		manager.setGattCallbacks(this)
	}

	fun connect() {
		synchronized(this) {
			Log.i(TAG, "connecting Found Device - btDev for ${address}: $btDev  ${btDev?.name}  ${btDev?.bluetoothClass}  ${btDev?.bondState}")
			manager.connect(btDev).enqueue()
		}
	}
	fun isConnected() : Boolean {
		return manager.isConnected
	}

	override fun onIAmLostResponse(device: BluetoothDevice, response: String) {
		val cloud = Helper.getLostService()
		cloud.Found(device.address.toLowerCase().replace(":", ""), 123, "", ""
				//Base64.encodeToString(characteristic.value, Base64.DEFAULT)
		).enqueue(object: Callback<Unit> {
			override fun onFailure(call: Call<Unit>?, t: Throwable?) {
				Log.e( "TrackerService", "ReportFound Server error: "+t.toString())
				Toast.makeText(parent, "Failed to report lost tracker!", Toast.LENGTH_SHORT).show()
			}

			override fun onResponse(call: Call<Unit>?, response: Response<Unit>?) {
				if (!response!!.isSuccessful) {
					Log.e( "TrackerService", "ReportFound  Failed: "+response.message())
					Log.e("TrackerService", "ReportFound not successful ${response.code()} ${response.raw()}")
					return
				}
				Log.i( "TrackerService", "ReportFound  Done!")
				Log.i("TrackerService", "reponse: ${response} " + response.toString())
				Log.i("TrackerService", "body: ${response?.body()} " + response?.body()?.toString())

				Toast.makeText(parent, "Reported lost tracker!", Toast.LENGTH_SHORT).show()
			}


		})
	}

	override fun onDeviceReady(device: BluetoothDevice) {
		manager.sendGeoloc(location)
	}

	override fun onLinkLossAlertSet(device: BluetoothDevice, value: Int) {
	}

	override fun onSetupResponse(device: BluetoothDevice, setupResponse: String) {
	}

	override fun onDeviceDisconnecting(device: BluetoothDevice) {
	}

	override fun onDeviceDisconnected(device: BluetoothDevice) {
	}

	override fun onDeviceConnected(device: BluetoothDevice) {
	}

	override fun onDeviceNotSupported(device: BluetoothDevice) {
	}

	override fun onBondingFailed(device: BluetoothDevice) {
	}

	override fun onServicesDiscovered(device: BluetoothDevice, optionalServicesFound: Boolean) {
	}

	override fun onBondingRequired(device: BluetoothDevice) {
	}

	override fun onLinkLossOccurred(device: BluetoothDevice) {
	}

	override fun onBonded(device: BluetoothDevice) {
	}


	override fun onError(device: BluetoothDevice, message: String, errorCode: Int) {
	}

	override fun onDeviceConnecting(device: BluetoothDevice) {
	}

	override fun onBatteryLevelChanged(device: BluetoothDevice, batteryLevel: Int) {
	}

}


