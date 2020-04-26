package de.seemoo.blefinderapp

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.*
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.location.LocationManager
import android.location.LocationProvider
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import de.seemoo.blefinderapp.db.HistoryItem
import de.seemoo.blefinderapp.db.KnownDevice
import no.nordicsemi.android.ble.BleManager

private const val TAG="ConnectedDevice"
class ConnectedDevice(val parent: Context, val dbDevice: KnownDevice) : WiederFinderCallback {

    val dbId: Long = dbDevice.id
    val address: String = dbDevice.bluetoothAddress!!
    var manager = WiederFinder(parent)
    var connRateLimitTimestamp : Long = 0L
    var wasConnected = false

    init {
        manager.setGattCallbacks(this)
    }

    fun connect() {
        synchronized(this) {
            Log.d(TAG, "connecting (maybe??)  $connRateLimitTimestamp / "+System.currentTimeMillis())
            if (connRateLimitTimestamp + 2000 > System.currentTimeMillis()) return
            Log.d(TAG, "connecting!")

            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val btDev = bluetoothAdapter.getRemoteDevice(address)

            Log.i(TAG, "btDev for ${address}: $btDev  ${btDev?.name}  ${btDev?.bluetoothClass}  ${btDev?.bondState}")
            manager.connect(btDev).enqueue()

            connRateLimitTimestamp = System.currentTimeMillis()
        }
    }
    fun isConnected() : Boolean {
        return manager.isConnected
    }

    fun ringDevice() {
        manager.ring()
    }

    fun broadcastChangeNotification(key:String,value:String){
        val intent = Intent("FINDER_CHANGED")
        intent.putExtra("id", dbId)
        intent.putExtra("address", address)
        intent.putExtra(key, value)
        LocalBroadcastManager.getInstance(parent).sendBroadcast(intent)
    }

    private fun storeLocationHistory(eventType:String) {
        val locProv = parent.getSystemService(LOCATION_SERVICE) as LocationManager
        val location = locProv.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        val event = HistoryItem(dbId, eventType, location)
        dbSession.historyItemDao.insert(event)
    }


    override fun onServicesDiscovered(device: BluetoothDevice, optionalServicesFound: Boolean) {
    }

    override fun onLinkLossAlertSet(device: BluetoothDevice, value: Int) {
    }

    override fun onSetupResponse(device: BluetoothDevice, setupResponse: String) {
    }

    override fun onDeviceDisconnecting(device: BluetoothDevice) {
    }

    override fun onDeviceDisconnected(device: BluetoothDevice) {
        if (wasConnected) {
            val b = NotificationCompat.Builder(parent, if (dbDevice.localLinkLossAlertLevel > 0) CHAN_LOST_ALERT else CHAN_LOST_ALERT_SILENT)

            val mapIntent = Intent(parent, MapsActivity::class.java)
            mapIntent.putExtra(ARG_ITEM_ID, dbId)
            b.setPriority(Notification.PRIORITY_HIGH)
                    .setContentTitle("DISCONNECTED !!1")
                    .setContentText("Name:" + dbDevice.displayName)
                    .setContentInfo("Addr:" + dbDevice.bluetoothAddress)
                    .setSmallIcon(android.R.drawable.stat_notify_error)
                    .addAction(R.drawable.ic_action_dark_location, "Map", PendingIntent.getActivity(parent, 0, mapIntent, 0))

            val noti = parent.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            noti.notify(1, b.build())
            wasConnected = false

            storeLocationHistory("lost")
        }

        broadcastChangeNotification("newState", "DISCONNECTED")
    }

    override fun onDeviceConnected(device: BluetoothDevice) {
        wasConnected = true
    }

    override fun onDeviceNotSupported(device: BluetoothDevice) {
    }

    override fun onBondingFailed(device: BluetoothDevice) {
    }

    override fun onBondingRequired(device: BluetoothDevice) {
    }

    override fun onLinkLossOccurred(device: BluetoothDevice) {
    }

    override fun onBonded(device: BluetoothDevice) {
    }

    override fun onDeviceReady(device: BluetoothDevice) {
        val b = NotificationCompat.Builder(parent, CHAN_CONNECTED)

        val ringIntent = TrackerIntentService.getRingDeviceIntent(parent, address)
        val configIntent = Intent(parent, FinderDetailActivity::class.java)
        configIntent.putExtra(ARG_ITEM_ID, dbId)

        b.setPriority(Notification.PRIORITY_LOW)
                .setContentTitle("Connected")
                .setContentText("Name:"+dbDevice.displayName)
                .setContentInfo("Addr:"+dbDevice.bluetoothAddress)
                .setSmallIcon(R.drawable.ic_notify_icon_bt)
                .addAction(R.drawable.ic_action_dark_bell_ring, "Ring", PendingIntent.getService(parent, 0, ringIntent, 0))
                .addAction(R.drawable.ic_action_dark_config, "Configure", PendingIntent.getActivity(parent, 0, configIntent, 0))

        val noti = parent.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        noti.notify(1, b.build())

        storeLocationHistory("connected")
        broadcastChangeNotification("newState", "CONNECTED")

        manager.setLinkLossAlertLevel(dbDevice.remoteLinkLossAlertLevel)
    }

    override fun onError(device: BluetoothDevice, message: String, errorCode: Int) {
    }

    override fun onDeviceConnecting(device: BluetoothDevice) {
    }

    override fun onBatteryLevelChanged(device: BluetoothDevice, batteryLevel: Int) {
        broadcastChangeNotification("batteryLevel", batteryLevel.toString())
    }

    override fun onIAmLostResponse(device: BluetoothDevice, response: String) {
    }

    fun getBatteryLevel() : Int {
        return manager.battery
    }
    fun getSetupMessage() : String? {
        return manager.setupMessage
    }

}