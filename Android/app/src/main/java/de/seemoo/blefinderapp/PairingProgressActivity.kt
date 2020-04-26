package de.seemoo.blefinderapp

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import de.seemoo.blefinderapp.cloud.RegisterInitResponse
import de.seemoo.blefinderapp.cloud.RegisterOKResponse
import de.seemoo.blefinderapp.db.KnownDevice
import kotlinx.android.synthetic.main.activity_pairing_progress.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PairingProgressActivity : AppCompatActivity(), WiederFinderCallback {

    private val cloud = Helper.getLostService()
    private lateinit var manager : WiederFinder
    private lateinit var registration: RegisterInitResponse
    private lateinit var dev  : KnownDevice


	companion object {
		@JvmStatic
		var inhibitBackgroundScanner = false
	}

	override fun onStop() {
		super.onStop()
		inhibitBackgroundScanner=false
		manager.close()
	}

	override fun onStart() {
		super.onStart()
		inhibitBackgroundScanner=true
	}


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        manager = WiederFinder(this)
        manager.setGattCallbacks(this)
        setContentView(R.layout.activity_pairing_progress)
        progressBar.max = 7
        progressBar.progress = 0

        if (intent.hasExtra("BluetoothDevice")) {
            val btDev = intent.getParcelableExtra<BluetoothDevice>("BluetoothDevice")
            registerInitAndConnectTo(btDev)
        }
    }

    private fun setProgress(text: String, value: Int) {
        conn_progress = value
        runOnUiThread {
            textView.setText(text)
            progressBar.progress = value
        }
    }
    private fun setError(text: String) {
        runOnUiThread {
            textView.setText(text)
            progressBar.progress = 0
            Handler().postDelayed({
                finish()
            }, 3000)
        }
    }


    private fun registerInitAndConnectTo(bt: BluetoothDevice) {
        setProgress("Connecting to cloud...", 1)
        cloud.RegisterInit().enqueue(object : Callback<RegisterInitResponse> {
            override fun onResponse(call: Call<RegisterInitResponse>?, response: Response<RegisterInitResponse>?) {
                Handler(Looper.getMainLooper()).postDelayed({ //wait a little to ensure the scanner had enough time to stop...
                    registration = response!!.body()!!
                    setProgress("Connecting to Bluetooth...", 2)
                    dev = KnownDevice(bt)
                    manager.connect(bt).retry(2,100).useAutoConnect(true).enqueue()
                }, 1000)
            }

            override fun onFailure(call: Call<RegisterInitResponse>?, t: Throwable?) {
                setError( "Cloud connection failed")
                t?.printStackTrace();
            }

        })
    }

    override fun onDeviceReady(device: BluetoothDevice) {
        setProgress("Pairing...", 3)
        val foo = "abcdefghijklm"
        manager.setup(dev.e2eKey!!, registration.getBinarySetupChallenge(), foo)
    }

    override fun onError(device: BluetoothDevice, message: String, errorCode: Int) {
        setError(message)
    }

    var conn_progress : Int = 0


    override fun onSetupResponse(device: BluetoothDevice, setupResponse: String) {
        setProgress("Registering device...", 6)
        cloud.RegisterResponse(registration.hmac,
                manager.bluetoothDevice!!.address.toLowerCase().replace(":", ""),
                setupResponse).enqueue(object: Callback<RegisterOKResponse> {
            override fun onFailure(call: Call<RegisterOKResponse>?, t: Throwable?) {
                setError( "Server error: "+t.toString())
            }

            override fun onResponse(call: Call<RegisterOKResponse>?, response: Response<RegisterOKResponse>?) {
                if (!response!!.isSuccessful) {
                    setError("Failed: "+response.message())
                    Log.e("Pairing", "Response not successful ${response.code()} ${response.raw()}")
                    return
                }
                setProgress("Done!", 7)
                Log.i("Pairing", "reponse: ${response} " + response.toString())
                Log.i("Pairing", "body: ${response?.body()} " + response?.body()?.toString())

                dev.cloudAccessToken = response!!.body()!!.token
                Log.i("Pairing", "token: ${dev.cloudAccessToken} ")
                dbSession.knownDeviceDao.insert(dev)
                Toast.makeText(this@PairingProgressActivity, "Registration successful", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }


        })
    }

    override fun onLinkLossAlertSet(device: BluetoothDevice, value: Int) {
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

    override fun onDeviceConnecting(device: BluetoothDevice) {
    }

    override fun onBatteryLevelChanged(device: BluetoothDevice, batteryLevel: Int) {
    }

    override fun onIAmLostResponse(device: BluetoothDevice, response: String) {
    }

}
