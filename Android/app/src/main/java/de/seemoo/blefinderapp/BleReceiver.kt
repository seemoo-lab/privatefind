package de.seemoo.blefinderapp

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.bluetooth.le.BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import de.seemoo.blefinderapp.db.gen.KnownDeviceDao
import java.util.*

const val NOTIFY_ID_BLUETOOTH_DISABLED = 42

private const val TAG = "BleReceiver"
class BleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i("BleReceiver"," onReceive called") //To change body of created functions use File | Settings | File Templates.
        //EXTRA_CALLBACK_TYPE, EXTRA_ERROR_CODE and EXTRA_LIST_SCAN_RESULT
        //var o=""
        /*
        if (intent!!.hasExtra(EXTRA_CALLBACK_TYPE)) {
            Log.i("BleReceiver", "EXTRA_CALLBACK_TYPE: " + intent!!.getIntExtra(EXTRA_CALLBACK_TYPE, 9999));
            o+=" cbType="+intent!!.getIntExtra(EXTRA_CALLBACK_TYPE, 9999)
        }
        if (intent!!.hasExtra(EXTRA_ERROR_CODE)) {
            Log.i("BleReceiver", "EXTRA_ERROR_CODE: " + intent!!.getIntExtra(EXTRA_ERROR_CODE, 9999));
            o+=" error="+intent!!.getIntExtra(EXTRA_ERROR_CODE, 9999)
        }*/
        val app = context!!.applicationContext as MyApplication

        if (intent!!.hasExtra(EXTRA_LIST_SCAN_RESULT)) {
            Log.i("BleReceiver", "EXTRA_LIST_SCAN_RESULT: ");
            val scanResults : ArrayList<ScanResult> = intent.getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT)
            for (result in scanResults) {
                //only handle a device every X seconds
                if (app.bluetoothReportRateLimit(result.device.address)) continue

                val knownDevice = dbSession.knownDeviceDao.queryBuilder().where(KnownDeviceDao.Properties.BluetoothAddress.eq(result.device.address)).unique()
                Log.i("BleReceiver", "- mac="+result.device.address+" | name="+result.device.name+" | rssi="+result.rssi+" | aSid="+result.advertisingSid+" | daSt="+result.dataStatus+" | leg="+result.isLegacy+" | con=${result.isConnectable} | db=${knownDevice}")
                //o+=", mac="+result.device.address+" rssi="+result.rssi+" db="+(knownDevice==null)

                if (knownDevice != null) {
                    //TrackerIntentService.connectKnownDevice(context!!, result.device)
                    knownDevice.lastSeenDate = Date()
                    dbSession.knownDeviceDao.update(knownDevice)

                    val connDevice = app.getConnectedDeviceInstance(knownDevice)
                    connDevice!!.connect()

                } else {
                    TrackerIntentService.reportFoundDevice(context!!, result.device)
                }


            }
        }
        Helper.dumpBundle("BleReceiver", intent.extras);

        //Toast.makeText(context, "recv "+o, Toast.LENGTH_LONG).show()
    }

    companion object {
        var pendingIntent : PendingIntent? = null


		fun isBackgroundScannerInhibited(): Boolean {
			return AddDeviceActivity.inhibitBackgroundScanner || PairingProgressActivity.inhibitBackgroundScanner
		}

        fun getScanFilter(): java.util.ArrayList<ScanFilter> {
            val filters = java.util.ArrayList<ScanFilter>()
            filters.add(ScanFilter.Builder().setServiceUuid(ParcelUuid(MyBluetoothUUIDs.CRYPTO_FINDER_SERVICE)).build())
            return filters
        }

        fun startBackgroundScanner(context: Context) {
            if (isBackgroundScannerInhibited()) {
                Log.i("BleReceiver", "startBackgroundScanner was INHIBITED");
                return
            }
            if (checkPermissions(context)) {
                Log.i("BleReceiver", "startBackgroundScanner...");
                runBleScanner(context)
            } else {
                Log.w("BleReceiver", "missing permissions, NOT starting on boot...");
            }
        }

        private fun checkPermissions(context: Context): Boolean {
            val permissionsToRequest = arrayOf<String>(
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION)

            var allPermissionsGranted = true

            for (permission in permissionsToRequest) {
                allPermissionsGranted = allPermissionsGranted && ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
            return allPermissionsGranted
        }


        private fun runBleScanner(context: Context) {
            Log.i("BleReceiver", "runBleScanner");

            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter

            if (!bluetoothAdapter.isEnabled) {
                Log.e(TAG, "bluetooth is not enabled!")

                val b = NotificationCompat.Builder(context, CHAN_CONNECTED)

                b.setPriority(Notification.PRIORITY_LOW)
                        .setContentTitle("Bluetooth not enabled")
                        .setContentText("Tap here to enable bluetooth so the Finder will work")
                        .setSmallIcon(R.drawable.ic_notify_icon_bt)
                        .setContentIntent(PendingIntent.getActivity(context, 0, Intent(context, FinderListActivity::class.java), 0))
                val noti = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                noti.notify(NOTIFY_ID_BLUETOOTH_DISABLED, b.build())
                return
            }

            val bleScanner = bluetoothAdapter.bluetoothLeScanner
            var settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                    .setReportDelay(60000)
                    .setMatchMode(ScanSettings.MATCH_MODE_STICKY)
                    //.setCallbackType(ScanSettings.CALLBACK_TYPE_MATCH_LOST) // https://stackoverflow.com/a/51750367/562836
                    .build()
            if (pendingIntent == null)
                pendingIntent = PendingIntent.getBroadcast(
                    context, BLE_SCAN_REQ_CODE,
                    Intent(context, BleReceiver::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT)

            val result = bleScanner.startScan(getScanFilter(), settings, pendingIntent);

            //Snackbar.make(findViewById(R.id.coord_layout), "startScan result: $result", Snackbar.LENGTH_LONG)
            //        .setAction("Action", null).show()
            Log.i(TAG, "startScan result: $result");
            if (result != 0) {
                Log.e(TAG, "startScan FAILED")
                if (result == ScanCallback.SCAN_FAILED_ALREADY_STARTED)
                    Log.e(TAG, "SCAN_FAILED_ALREADY_STARTED")
                else if (result == ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED)
                    Log.e(TAG, "SCAN_FAILED_APPLICATION_REGISTRATION_FAILED")
                else if (result == ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED)
                    Log.e(TAG, "SCAN_FAILED_FEATURE_UNSUPPORTED")
                else if (result == ScanCallback.SCAN_FAILED_INTERNAL_ERROR)
                    Log.e(TAG, "SCAN_FAILED_INTERNAL_ERROR")
                else if (result == 5)
                    Log.e(TAG, "SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES")
                else if (result == 6)
                    Log.e(TAG, "SCAN_FAILED_SCANNING_TOO_FREQUENTLY")

            }
        }

        fun stopBackgroundScanner(context:Context) {
            if (pendingIntent == null) return
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            val bleScanner = bluetoothAdapter.bluetoothLeScanner
            bleScanner.stopScan(pendingIntent)
            pendingIntent = null
        }
    }
}
