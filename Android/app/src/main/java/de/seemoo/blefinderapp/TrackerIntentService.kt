package de.seemoo.blefinderapp

import android.app.IntentService
import android.app.Notification
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.support.v4.app.NotificationCompat
import android.util.Log
import android.widget.Toast
import de.seemoo.blefinderapp.db.KnownDevice
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// TODO: Rename actions, choose action names that describe tasks that this
// IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
private const val ACTION_CONNECT_KNOWN = "de.seemoo.blefinderapp.action.ACTION_CONNECT_KNOWN"
private const val ACTION_CONNECT_ALL_KNOWN = "de.seemoo.blefinderapp.action.ACTION_CONNECT_ALL_KNOWN"
private const val ACTION_RING_DEVICE = "de.seemoo.blefinderapp.action.ACTION_RING_DEVICE"
private const val ACTION_REPORT_FOUND_DEVICE = "de.seemoo.blefinderapp.action.REPORT_FOUND_DEVICE"

// TODO: Rename parameters
private const val EXTRA_PARAM1 = "de.seemoo.blefinderapp.extra.PARAM1"
private const val EXTRA_PARAM2 = "de.seemoo.blefinderapp.extra.PARAM2"
private const val EXTRA_BLUETOOTH_DEVICE = "de.seemoo.blefinderapp.extra.BLUETOOTH_DEVICE"
private const val EXTRA_BLUETOOTH_MAC_ADDRESS = "de.seemoo.blefinderapp.extra.BLUETOOTH_MAC_ADDRESS"

const val DEVICE_REPORT_INTERVAL = 120000 //millisec

/**
 * An [IntentService] subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
class TrackerIntentService : IntentService("TrackerIntentService") {
    private val TAG = "TrackerIntentService"
    private val FOREGROUND_ID = 123

    private val cloud = Helper.getLostService()



    private fun app():MyApplication {
        return applicationContext as MyApplication
    }


    override fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_CONNECT_ALL_KNOWN -> {
                handleConnectAllKnownDevices()
            }
            ACTION_CONNECT_KNOWN -> {
                val device = intent.getParcelableExtra<BluetoothDevice>(EXTRA_BLUETOOTH_DEVICE)

                handleConnectKnownDevice(device)
            }
            ACTION_RING_DEVICE -> {
                val addr = intent.getStringExtra(EXTRA_BLUETOOTH_MAC_ADDRESS)

                handleRingDevice(addr)
            }
            ACTION_REPORT_FOUND_DEVICE -> {
                val device = intent.getParcelableExtra<BluetoothDevice>(EXTRA_BLUETOOTH_DEVICE)

                handleReportFoundDevice(device)
            }
        }
    }

    private fun buildForegroundNotification(device: BluetoothDevice, message:String): Notification? {
        val b = NotificationCompat.Builder(this, CHAN_REPORTING_FINDER)

        b.setOngoing(true)
                .setContentTitle("Reporting lost finder ")
                .setContentText(device.address+"("+message+")")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setTicker("Reporting a lost finder")

        return b.build()
    }

    private fun handleConnectAllKnownDevices() {
        Log.i(TAG, "handleConnectAllKnownDevices")
        val list = dbSession.knownDeviceDao.loadAll()
        for (dbDev in list) {
            val dev = app().getConnectedDeviceInstance(dbDev)
            dev.connect()
        }
    }

    private fun handleConnectKnownDevice(device:BluetoothDevice) {
        Log.i(TAG, "handleConnectKnownDevice")
        val addr = device.address
        val connDevice = app().getConnectedDeviceInstance(addr)
        connDevice!!.connect()
    }

    private fun handleRingDevice(deviceAddr:String) {
        Log.i(TAG, "handleRingDevice")
        val connDevice = app().getConnectedDeviceInstance(deviceAddr)
        connDevice!!.ringDevice()
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private fun handleReportFoundDevice(device:BluetoothDevice) {
        startForeground(FOREGROUND_ID,
                buildForegroundNotification(device,"unknown"))

        val locProv = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location = locProv.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        Log.i(TAG, "handleReportFoundDevice ${device}, location ${location}")

        val foundDev = FoundDevice(this, device, location)
        foundDev.connect()
    }

    companion object {

        val deviceReportingDebounce = HashMap<String,Long>()

        /**
         * Starts this service to perform action Foo with the given parameters. If
         * the service is already performing a task this action will be queued.
         *
         * @see IntentService
         */
        @JvmStatic
        fun connectAllKnownDevices(context: Context) {
            val intent = Intent(context, TrackerIntentService::class.java).apply {
                action = ACTION_CONNECT_ALL_KNOWN
            }
            context.startService(intent)
        }

        @JvmStatic
        fun connectKnownDevice(context: Context, param1: BluetoothDevice) {
            val intent = Intent(context, TrackerIntentService::class.java).apply {
                action = ACTION_CONNECT_KNOWN
                putExtra(EXTRA_BLUETOOTH_DEVICE, param1)
            }

            context.startService(intent)
        }

        /**
         * Starts this service to perform action Baz with the given parameters. If
         * the service is already performing a task this action will be queued.
         *
         * @see IntentService
         */
        // TODO: Customize helper method
        @JvmStatic
        fun reportFoundDevice(context: Context, param1: BluetoothDevice) {
            if (deviceReportingDebounce.containsKey(param1.address) && deviceReportingDebounce[param1.address]!! > System.currentTimeMillis() - DEVICE_REPORT_INTERVAL) return
            deviceReportingDebounce[param1.address] = System.currentTimeMillis()

            val intent = Intent(context, TrackerIntentService::class.java).apply {
                action = ACTION_REPORT_FOUND_DEVICE
                putExtra(EXTRA_BLUETOOTH_DEVICE, param1)
            }

            context.startForegroundService(intent)
        }

        @JvmStatic
        fun getRingDeviceIntent(context: Context, addr:String) : Intent{
            return Intent(context, TrackerIntentService::class.java).apply {
                action = ACTION_RING_DEVICE
                putExtra(EXTRA_BLUETOOTH_MAC_ADDRESS, addr)
            }
        }
    }
}
