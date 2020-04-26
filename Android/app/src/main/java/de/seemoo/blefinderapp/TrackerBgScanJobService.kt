package de.seemoo.blefinderapp

import android.app.Service
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.support.v4.app.JobIntentService
import android.support.annotation.NonNull
import android.util.Log
import android.app.job.JobInfo
import android.content.ComponentName
import android.app.job.JobScheduler
import android.preference.PreferenceManager


private const val BGSCAN_JOB_ID = 1

private const val JOB_START_BG_SCANNER = 2
private const val TAG = "TrackerBgScanJobService"
class TrackerBgScanJobService : JobService() {
    override fun onStopJob(params: JobParameters?): Boolean {
        Log.i(TAG, "onStopJob")
        return true
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.i(TAG, "onStartJob handleStartBgScanner")
        if (isAutomaticBgScanningEnabled(this))
            BleReceiver.startBackgroundScanner(this)
        Helper.debugNotification(this,"onStartJob - StartBgScanner", "")



        return false
    }

    companion object {
        fun isAutomaticBgScanningEnabled( ctx:Context):Boolean {
            return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean("scan_in_background", true)
        }

        @JvmStatic
        fun schedule(context: Context) {
            Log.i(TAG, "scheduling JOB")
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler?
            jobScheduler!!.schedule(JobInfo.Builder(BGSCAN_JOB_ID ,
                    ComponentName(context, TrackerBgScanJobService::class.java!!))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPeriodic(1000 * 60 * 10, 1000 * 60 * 5)
                    .build())
        }


    }

}
