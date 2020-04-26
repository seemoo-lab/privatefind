package de.seemoo.blefinderapp

import android.Manifest
import android.app.PendingIntent
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.widget.Toast

class ServiceStarter : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i("BLEFinder:ServiceStarter", "starting on boot...");
        //BleReceiver.startBackgroundScanner(context!!)
        //is this different???
        //TrackerService.startBgScanner(context!!)
        TrackerBgScanJobService.schedule(context!!)
        Helper.debugNotification(context!!,"scheduling job on boot", "");
    }



}