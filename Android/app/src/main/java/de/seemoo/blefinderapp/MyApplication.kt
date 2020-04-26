package de.seemoo.blefinderapp;


import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.util.Log
import de.seemoo.blefinderapp.db.KnownDevice
import de.seemoo.blefinderapp.db.gen.DaoMaster
import de.seemoo.blefinderapp.db.gen.DaoSession
import de.seemoo.blefinderapp.db.gen.KnownDeviceDao


const val CHAN_REPORTING_FINDER = "reporting"
const val CHAN_DEBUG = "debug"
const val CHAN_LOST_ALERT = "lost"
const val CHAN_LOST_ALERT_SILENT = "lost_silent"

const val CHAN_CONNECTED = "connected"
lateinit var dbSession : DaoSession

class MyApplication : Application() {


	val TAG = "MyApplication"


	var lastReportTimestamp = HashMap<String, Long>()
	var connectedDevices = HashMap<String, ConnectedDevice>()

	fun getConnectedDeviceInstance(address: String): ConnectedDevice? {
		if (connectedDevices.containsKey(address))
			return connectedDevices[address]

		val dbDevice = dbSession.knownDeviceDao.queryBuilder().where(KnownDeviceDao.Properties.BluetoothAddress.eq(address)).unique()
				?: return null
		return getConnectedDeviceInstance(dbDevice)
	}
	fun getConnectedDeviceInstance(dbDevice: KnownDevice): ConnectedDevice {
		if (connectedDevices.containsKey(dbDevice.bluetoothAddress))
			return connectedDevices[dbDevice.bluetoothAddress]!!
		val dev = ConnectedDevice(this, dbDevice)
		Log.i(TAG,"New ConnectedDevice instance ${dbDevice.bluetoothAddress}")
		Log.i(TAG, "devices ${connectedDevices.size}")
		connectedDevices.put(dbDevice.bluetoothAddress!!, dev)
		return dev
	}

	fun bluetoothReportRateLimit(address:String):Boolean {
		if (lastReportTimestamp.containsKey(address) && lastReportTimestamp[address]!! + 2000 > System.currentTimeMillis())
			return true
		lastReportTimestamp[address] = System.currentTimeMillis()
		return false
	}



	override fun onCreate() {
		super.onCreate()
		Log.i(TAG, "onCreate")

		registerNotificationChannels()

		// do this once, for example in your Application class
		val helper = DaoMaster.DevOpenHelper(this, "notes-db", null)
		val db = helper.getWritableDatabase()
		val daoMaster = DaoMaster(db)
		dbSession = daoMaster.newSession()

		try {
			TrackerBgScanJobService.schedule(this)
			Helper.debugNotification(this, "scheduling job on app start", "")
		} catch (ex: IllegalStateException) {
			Log.e("MyApplication", "TrackerBgScanJobService.schedule was rejected")
			ex.printStackTrace()
		}

	}

	fun registerNotificationChannels() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			// Create the NotificationChannel

			val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

			val audioAttributes = AudioAttributes.Builder()
					.setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
					.setUsage(AudioAttributes.USAGE_NOTIFICATION)
					.build()

			var mChannel = NotificationChannel(CHAN_REPORTING_FINDER, "Found lost finder", NotificationManager.IMPORTANCE_LOW)
			mChannel.description = "When a finder is found that someone else lost"
			// Register the channel with the system; you can't change the importance
			// or other notification behaviors after this
			notificationManager.createNotificationChannel(mChannel)


			mChannel = NotificationChannel(CHAN_DEBUG, "Debug info", NotificationManager.IMPORTANCE_LOW)

			notificationManager.createNotificationChannel(mChannel)


			mChannel = NotificationChannel(CHAN_LOST_ALERT, "Lost Alert", NotificationManager.IMPORTANCE_HIGH)
			mChannel.vibrationPattern = longArrayOf(0, 200, 111, 200, 111, 200, 876, 200, 111, 200, 111, 200, 876, 200, 111, 200, 111, 200, 876, 200, 111, 200, 111, 200, 876, 200, 111, 200, 111, 200, 876)
			val soundUri1 = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + applicationContext.packageName + "/" + R.raw.telephone_ring_03a)
			mChannel.setSound(soundUri1, audioAttributes)
			notificationManager.createNotificationChannel(mChannel)

			mChannel = NotificationChannel(CHAN_LOST_ALERT_SILENT, "Lost Alert Silent", NotificationManager.IMPORTANCE_HIGH)
			notificationManager.createNotificationChannel(mChannel)



			mChannel = NotificationChannel(CHAN_CONNECTED, "Finder Connected", NotificationManager.IMPORTANCE_LOW)
			val soundUri2 = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + applicationContext.packageName + "/" + R.raw.button_35)
			mChannel.setSound(soundUri2, audioAttributes)
			mChannel.vibrationPattern = longArrayOf(0, 180, 100, 250)

			notificationManager.createNotificationChannel(mChannel)

		}

	}
}

