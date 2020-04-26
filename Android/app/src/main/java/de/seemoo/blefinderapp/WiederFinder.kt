package de.seemoo.blefinderapp

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.location.Location
import android.util.Base64
import android.util.Log
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.BleManagerCallbacks
import no.nordicsemi.android.ble.callback.DataReceivedCallback
import no.nordicsemi.android.ble.callback.FailCallback
import no.nordicsemi.android.ble.callback.SuccessCallback
import no.nordicsemi.android.ble.common.callback.battery.BatteryLevelDataCallback
import no.nordicsemi.android.ble.common.profile.battery.BatteryLevelCallback
import no.nordicsemi.android.ble.data.Data
import java.nio.ByteBuffer


interface WiederFinderCallback : BleManagerCallbacks, BatteryLevelCallback {
	fun onLinkLossAlertSet(device : BluetoothDevice, value : Int)
	fun onSetupResponse(device : BluetoothDevice, setupResponse : String)
	fun onIAmLostResponse(device : BluetoothDevice, response : String)
}

private const val TAG="WiederFinder"
class WiederFinder(val parent: Context) : BleManager<WiederFinderCallback>(parent) {

	var mImmediateAlertChar : BluetoothGattCharacteristic? = null
	var mLinkLossAlertChar : BluetoothGattCharacteristic? = null
	var mBatteryLevelChar : BluetoothGattCharacteristic? = null

	var mSetupChallengeChar : BluetoothGattCharacteristic? = null
	var mSetupE2ekeyChar : BluetoothGattCharacteristic? = null
	var mSetupMessageChar : BluetoothGattCharacteristic? = null
	var mGeolocChar : BluetoothGattCharacteristic? = null
	var mIamlostChar: BluetoothGattCharacteristic? = null

	var mSupported = false

	var battery : Int = -1
	var setupMessage : String? = null

	override fun getGattCallback(): BleManagerGattCallback {
		return mGattCallback
	}


	val mBatteryLevelCallback = object: BatteryLevelDataCallback() {
		override fun onBatteryLevelChanged(device: BluetoothDevice, batteryLevel: Int) {
			Log.d(TAG, "BatteryLevel Read $batteryLevel")
			mCallbacks.onBatteryLevelChanged(device, batteryLevel)
			battery = batteryLevel
		}
	}
	val mSetupMessageReadCallback = DataReceivedCallback { device, data ->
		Log.d(TAG, "SetupMessage Read $data")
		setupMessage = data.getStringValue(0)
	}
	val mSetupChallengeCallback = DataReceivedCallback { device, data ->
		mCallbacks.onSetupResponse(device, Base64.encodeToString(data.value!!, Base64.DEFAULT))
	}
	val mIamlostCallback = DataReceivedCallback { device, data ->
		mCallbacks.onIAmLostResponse(device, Base64.encodeToString(data.value!!, Base64.DEFAULT))
	}

	protected val mGattCallback = object: BleManagerGattCallback() {

		override fun initialize() {
			setNotificationCallback(mSetupChallengeChar).with(mSetupChallengeCallback)
			setNotificationCallback(mBatteryLevelChar).with(mBatteryLevelCallback)
			setNotificationCallback(mIamlostChar).with(mIamlostCallback)
			enableNotifications(mSetupChallengeChar).enqueue()
			enableNotifications(mBatteryLevelChar).enqueue()
			enableNotifications(mIamlostChar).enqueue()

			readCharacteristic(mBatteryLevelChar).with(mBatteryLevelCallback).enqueue()
			readCharacteristic(mSetupMessageChar).with(mSetupMessageReadCallback).enqueue()
		}

		override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
			val ias= gatt.getService(MyBluetoothUUIDs.IMMEDIATE_ALERT_SERVICE)
			mImmediateAlertChar = ias?.getCharacteristic(MyBluetoothUUIDs.ALERT_LEVEL_CHAR)

			val lls= gatt.getService(MyBluetoothUUIDs.LINKLOSS_ALERT_SERVICE)
			mLinkLossAlertChar = lls?.getCharacteristic(MyBluetoothUUIDs.ALERT_LEVEL_CHAR)

			val bs= gatt.getService(MyBluetoothUUIDs.BATTERY_SERVICE)
			mBatteryLevelChar = bs?.getCharacteristic(MyBluetoothUUIDs.BATTERY_LEVEL_CHAR)

			val cfs= gatt.getService(MyBluetoothUUIDs.CRYPTO_FINDER_SERVICE)
			mSetupChallengeChar = cfs?.getCharacteristic(MyBluetoothUUIDs.SETUP_CHALLENGE_CHAR)
			mSetupE2ekeyChar = cfs?.getCharacteristic(MyBluetoothUUIDs.E2E_KEY_CHAR)
			mSetupMessageChar = cfs?.getCharacteristic(MyBluetoothUUIDs.MESSAGE_CHAR)
			mGeolocChar = cfs?.getCharacteristic(MyBluetoothUUIDs.GEOLOC_CHAR)
			mIamlostChar = cfs?.getCharacteristic(MyBluetoothUUIDs.IAMLOST_CHAR)


			/*
			var writeRequest = false
			if (mLedCharacteristic != null) {
				val rxProperties = mLedCharacteristic.getProperties()
				writeRequest = rxProperties and BluetoothGattCharacteristic.PROPERTY_WRITE > 0
			}
			*/
			mSupported = mImmediateAlertChar != null && mLinkLossAlertChar != null && mBatteryLevelChar != null &&
					mSetupChallengeChar != null && mSetupE2ekeyChar != null && mSetupMessageChar != null
			return mSupported
		}


		override fun onDeviceDisconnected() {
			mImmediateAlertChar = null
			mLinkLossAlertChar = null
			mBatteryLevelChar = null
			mSetupChallengeChar = null
			mSetupE2ekeyChar = null
			mSetupMessageChar = null
			mGeolocChar = null
		}

	}


	val ALERT_OFF = Data.opCode(0x00)
	val ALERT_MILD = Data.opCode(0x01)
	val ALERT_HIGH = Data.opCode(0x02)

	fun ring() {
		if (mImmediateAlertChar == null) return
		log(Log.VERBOSE, "Ringing")
		writeCharacteristic(mImmediateAlertChar, ALERT_HIGH).with { device, data ->
			log(Log.VERBOSE, "Ring request DataSentCallback $device $data")
		}.done({
			log(Log.VERBOSE, "Ring successful $it")
		}).fail(FailCallback { device, status ->
			log(Log.VERBOSE, "Ring FAILED $device $status")
		}).enqueue()

	}

	fun setup(e2ekey : ByteArray, setupChallenge : ByteArray, message : String) {
		var failCb = FailCallback { device, status ->
			mCallbacks.onError(device, "Setup failed", status)
		}
		createBond().fail(failCb).done(SuccessCallback {
			writeCharacteristic(mSetupE2ekeyChar, e2ekey).fail(failCb).enqueue()
			writeCharacteristic(mSetupMessageChar, message.toByteArray()).fail(failCb).enqueue()
			writeCharacteristic(mSetupChallengeChar, setupChallenge).fail(failCb).enqueue()
		}).enqueue()
	}

	fun setLinkLossAlertLevel(level: Byte) {
		writeCharacteristic(mLinkLossAlertChar, Data.opCode(level)).fail(FailCallback { device, status ->
			Log.e(TAG, "setLinkLossAlertLevel FAILED $status")
		}).enqueue()
	}

	fun sendGeoloc(loc : Location) {
		Log.i(TAG,"sendGeoloc: $loc")
		val buf = ByteBuffer.allocate(20).putInt(0).putFloat(loc.latitude.toFloat()).putFloat(loc.longitude.toFloat()).putFloat(loc.accuracy).putInt(0)
		writeCharacteristic(mGeolocChar, buf.array()).enqueue()
	}

	override fun log(priority: Int, message: String) {
		// Override to log events. Simple log can use Logcat:
		//
		Log.println(priority, TAG, message)
		//
		// You may also use Timber:
		//
		// Timber.log(priority, message);
		//
		// or nRF Logger:
		//
		// Logger.log(logSession, LogContract.Log.Level.fromPriority(priority), message);
		//
		// Starting from nRF Logger 2.1.3, you may use log-timber and plant nRFLoggerTree.
		// https://github.com/NordicSemiconductor/nRF-Logger-API
	}



	protected override fun onPairingRequestReceived(device: BluetoothDevice, variant: Int) {
		Log.e(TAG, "onPairingRequestReceived $variant")

		// The API below is available for Android 4.4 or newer.

		// An app may set the PIN here or set pairing confirmation (depending on the variant) using:
		// device.setPin(new byte[] { '1', '2', '3', '4', '5', '6' });
		// device.setPairingConfirmation(true);

		// However, setting the PIN here will not prevent from displaying the default pairing
		// dialog, which is shown by another application (Bluetooth Settings).
	}

}
