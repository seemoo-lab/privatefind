package de.seemoo.blefinderapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import de.seemoo.blefinderapp.cloud.FindingsResponse
import de.seemoo.blefinderapp.db.HistoryItem
import de.seemoo.blefinderapp.db.KnownDevice
import de.seemoo.blefinderapp.db.gen.HistoryItemDao
import kotlinx.android.synthetic.main.activity_finder_detail.*
import kotlinx.android.synthetic.main.finder_detail.*
import kotlinx.android.synthetic.main.finder_detail.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.DateFormat
import java.util.*

/**
 * A fragment representing a single Finder detail screen.
 * This fragment is either contained in a [FinderListActivity]
 * in two-pane mode (on tablets) or a [FinderDetailActivity]
 * on handsets.
 */
const val ARG_ITEM_ID = "item_id"

private const val TAG = "FinderDetailFragment"
class FinderDetailFragment : Fragment() {

    /**
     * The dummy content this fragment is presenting.
     */
    private var item: KnownDevice? = null
    private var connDev: ConnectedDevice? = null
    private var item_id:Long=0

    private var rootView : View?=null

    val broadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {

            when (intent?.action) {
                "FINDER_CHANGED" -> loadItemData()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG,"onCreate")
        savedInstanceState?.let {
            item_id = savedInstanceState.getLong(ARG_ITEM_ID)
        }
        arguments?.let {
            if (it.containsKey(ARG_ITEM_ID)) {
                // Load the dummy content specified by the fragment
                // arguments. In a real-world scenario, use a Loader
                // to load content from a content provider.
                item_id = it.getLong(ARG_ITEM_ID)
                Log.d(TAG, "ARG_ITEM_ID = $item_id")
            }
        }
        Log.d(TAG, "item_id = $item_id")
        LocalBroadcastManager.getInstance(context!!)
                .registerReceiver(broadCastReceiver, IntentFilter("FINDER_CHANGED"))
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG,"onResume")

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Log.d(TAG,"onActivityCreated")
        loadItemData()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(ARG_ITEM_ID, item_id)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(context!!)
                .unregisterReceiver(broadCastReceiver)
    }

    fun loadItemData() {
        if (item_id == 0L) {
            Log.e(TAG, "loadItemData called with item_id=0, doing nothing...")
            return
        }
        item = dbSession.knownDeviceDao.load(item_id)
        activity?.toolbar_layout?.title = item?.displayName

        val app = context!!.applicationContext as MyApplication
        Log.d(TAG, "app: ${app}, id: ${item_id}, item: ${item}")
        connDev = app.getConnectedDeviceInstance(item!!)

        rootView?.finder_detail?.text = item.toString() + "\n " +
                (if (connDev?.isConnected() == true)  "Connected" else "Not Connected") + "\n" +
                "Battery Level: ${connDev?.getBatteryLevel()}\n" +
                "Setup Message: ${connDev?.getSetupMessage()}"
        (activity as FinderDetailActivity).setConnMode(connDev?.isConnected() == true)

        if (connDev?.isConnected() != true) {
            requestLastFindings(item!!.cloudAccessToken, item!!.e2eKey)
            showLocalMap(dbSession.historyItemDao.queryBuilder().where(HistoryItemDao.Properties.DeviceId.eq(item_id)).orderAsc(HistoryItemDao.Properties.Timestamp).list())
        }
    }


    fun requestLastFindings(token: String, e2eKey: ByteArray) {
        val cloud = Helper.getLostService()
        cloud.GetFindings(token).enqueue(object: Callback<FindingsResponse> {
            override fun onFailure(call: Call<FindingsResponse>?, t: Throwable?) {
                Log.e( TAG, "requestLastFindings Server error: "+t.toString())
                //Toast.makeText(parent, "Failed to report lost tracker!", Toast.LENGTH_SHORT).show()
            }

            override fun onResponse(call: Call<FindingsResponse>?, response: Response<FindingsResponse>?) {
                if (!response!!.isSuccessful) {
                    Log.e( TAG, "requestLastFindings  Failed: "+response.message())
                    Log.e(TAG, "requestLastFindings not successful ${response.code()} ${response.raw()}")
                    return
                }
                Log.i( TAG, "requestLastFindings Done!")
                Log.i(TAG, "reponse: ${response} " + response.toString())
                Log.i(TAG, "body: ${response?.body()} " + response?.body()?.toString())
                var findings = response.body()?.findings.orEmpty()
                showRemoteMap(findings, e2eKey)
                //Toast.makeText(parent, "Reported lost tracker!", Toast.LENGTH_SHORT).show()

            }


        })
    }

    private fun showLocalMap(list: List<HistoryItem>) {

        if (!list.isEmpty()) {
            rootView!!.card_localmap.visibility=View.VISIBLE
            rootView!!.post {
                val myFrag = childFragmentManager!!.findFragmentById(R.id.localmap)
                val mapFrag = childFragmentManager!!.findFragmentById(R.id.localmap) as SupportMapFragment?
                Log.d(TAG, "mapFrag  $myFrag  $mapFrag")
                mapFrag?.getMapAsync {
                    Log.i(TAG, "filling local map fragment")
                    for (f in list) {
                        Log.i(TAG, "historyItem: $f")
                        val latlng = LatLng(f.latitude, f.longitude)
                        it.addMarker(MarkerOptions().position(latlng).title(dateformat(f.timestamp)))
                        it.addCircle(CircleOptions().center(latlng).radius(f.locationAccuracy.toDouble()))
                    }
                    val recent = list.get(list.size - 1)
                    it.moveCamera(CameraUpdateFactory.newLatLng(LatLng(recent.latitude, recent.longitude)))
                    localtime.text = "Your last connection to the finder was at " +dateformat(recent.timestamp)
                }
            }
        }
    }
    private fun dateformat(date : Date):String{
        return java.text.DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(date)
    }
    private fun showRemoteMap(findings: List<FindingsResponse.FindReport>, e2eKey:ByteArray) {
        rootView!!.card_remoteloading.visibility = View.GONE
        if (findings.isEmpty()) {
            rootView!!.card_noremote.visibility = View.VISIBLE
        } else {
            rootView!!.card_remotemap.visibility = View.VISIBLE
            val mapFrag = childFragmentManager!!.findFragmentById(R.id.remotemap) as MapFragment?
            mapFrag?.getMapAsync {
                Log.i(TAG, "filling remote map fragment")
                for (f in findings) {
                    Log.i(TAG, "e2eMessage: $f")
                    val location = f.decryptAndParseGeoloc(e2eKey)
                    Log.i(TAG, "... location: $location")
                    val latlng=LatLng(location.latitude, location.longitude)
                    it.addMarker(MarkerOptions().position(latlng))
                    it.addCircle(CircleOptions().center(latlng).radius(location.accuracy.toDouble()))
                }
            }

            localtime.text = "The last report came in at " + dateformat(findings.get(findings.size - 1).serverTimestamp)
        }
    }

    fun ring() {
        connDev?.ringDevice()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.finder_detail, container, false)

        Log.d(TAG,"onCreateView")
        rootView!!.btnConnect.setOnClickListener {
            connDev?.connect()
        }
        return rootView
    }


}
