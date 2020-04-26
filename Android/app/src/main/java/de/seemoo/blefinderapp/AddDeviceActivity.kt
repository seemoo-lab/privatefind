package de.seemoo.blefinderapp

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.opengl.Visibility
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import android.widget.Toast
import de.seemoo.blefinderapp.db.KnownDevice
import kotlinx.android.synthetic.main.activity_add_device.*
import kotlinx.android.synthetic.main.content_add_device.*

const val BLE_SCAN_REQ_CODE = 42

class AddDeviceActivity : AppCompatActivity() {
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var adapter: ScanResultListAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_device)
        setSupportActionBar(toolbar)

        // Show the Up button in the action bar.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        fab.setOnClickListener { view ->
            //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
            //        .setAction("Action", null).show()
            Helper.InputBox(this, "Enter mac address", "") {result ->
                val dev = KnownDevice()
                dev.generateRandomKeys()
                dev.bluetoothAddress = result
                dev.bluetoothName = result
                dev.displayName = "Fake Device"
                dbSession.knownDeviceDao.insert(dev)
                finish()
            }
        }

        linearLayoutManager = LinearLayoutManager(this)
        newdevList.layoutManager = linearLayoutManager
        adapter = ScanResultListAdapter {
            startPairing(it)
        }
        newdevList.adapter = adapter
    }

    private fun startPairing(it: BluetoothDevice) {
        val i = Intent(this, PairingProgressActivity::class.java)
        i.putExtra("BluetoothDevice" , it)
        startActivityForResult(i, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) finish() // success
        }
    }


    fun requestPermissions(): Boolean {
        val CODE = 5 // app defined constant used for onRequestPermissionsResult

        val permissionsToRequest = arrayOf<String>(
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION)

        var allPermissionsGranted = true

        for (permission in permissionsToRequest) {
            allPermissionsGranted = allPermissionsGranted && ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }

        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(this, permissionsToRequest, CODE)
        }
        return allPermissionsGranted
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        runBleScanner()
    }

    companion object {
        @JvmStatic
        var inhibitBackgroundScanner = false
    }


    override fun onStart() {
        super.onStart()

        BleReceiver.stopBackgroundScanner(this)

        inhibitBackgroundScanner=true
        if (requestPermissions())
            runBleScanner()
    }

    override fun onStop() {
        super.onStop()

        inhibitBackgroundScanner=false
        stopBleScanner()
    }

    private fun stopBleScanner() {
        Log.i("NewDeviceActivity", "stopBleScanner");

        val bluetoothManager = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        try {
            val bleScanner = bluetoothAdapter.bluetoothLeScanner
            //bleScanner.stopScan(getPendingIntent())
            bleScanner.stopScan(scanCb)
            Toast.makeText(this, "stopBleScanner", Toast.LENGTH_SHORT).show()
        }catch( ex:Exception){
            Toast.makeText(this, "stopBleScanner failed: $ex", Toast.LENGTH_LONG).show()

        }
    }

    private val scanCb = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val info = "${result?.device?.name} | ${result?.device?.address} | ${result?.rssi} | ${result?.scanRecord?.deviceName} | ${result?.scanRecord?.advertiseFlags} | ${result?.scanRecord?.manufacturerSpecificData} | ${result?.scanRecord?.serviceData} | ${result?.scanRecord?.serviceUuids} | ${result?.scanRecord?.txPowerLevel}"
            Log.d("NewDeviceActivity", "onScanResult(): ${info}")
            //Toast.makeText(this@AddDeviceActivity, "scanResult : $info", Toast.LENGTH_SHORT).show()
            adapter.add(result!!)
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w("NewDeviceActivity", "onScanFailed(${errorCode})")
            AlertDialog.Builder(this@AddDeviceActivity)
                    .setTitle("Could not scan for bluetooth devices :-(")
                    .setMessage("Maybe try to switch bluetooth off an on again, or restart the phone if that doesn't help. error code ${errorCode}")
                    .setPositiveButton("OK", DialogInterface.OnClickListener { dialog, which ->
                        dialog.dismiss()
                        finish()
                    })
                    .show()
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            Log.d("NewDeviceActivity", "onBatchScanResults(): ${results?.size}")

            for (result in results!!) {
                Log.i("NewDeviceActivity", "- "+result.device.address+" | "+result.device.name+" | "+result.rssi)
                adapter.add(result)
                setIsEmpty(false)
            }
        }
    }

    fun setIsEmpty(empty:Boolean) {
        emptyBackground.visibility = if (empty) View.VISIBLE else View.GONE
        newdevList.visibility = if (empty) View.GONE else View.VISIBLE
    }

    fun runBleScanner() {
        Log.i("NewDeviceActivity", "runBleScanner");

        val bluetoothManager = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        val bleScanner = bluetoothAdapter.bluetoothLeScanner
        var settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .setReportDelay(2000)
                .build()
        bleScanner.startScan(BleReceiver.getScanFilter(), settings, scanCb)

        Toast.makeText(this, "Started scanning for devices... ", Toast.LENGTH_SHORT).show()
        Log.i("NewDeviceActivity", "startScan called");
    }

}
