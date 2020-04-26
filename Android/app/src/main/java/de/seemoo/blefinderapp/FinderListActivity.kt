package de.seemoo.blefinderapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import de.seemoo.blefinderapp.db.KnownDevice
import kotlinx.android.synthetic.main.activity_finder_list.*
import kotlinx.android.synthetic.main.finder_list.*
import kotlinx.android.synthetic.main.finder_list_content.view.*

private const val TAG="FinderListActivity"
/**
 * An activity representing a list of Pings. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a [FinderDetailActivity] representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
class FinderListActivity : AppCompatActivity() {

    private val handler = Handler()

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private var twoPane: Boolean = false

    val broadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {

            when (intent?.action) {
                "FINDER_CHANGED" -> updateFinderList()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_finder_list)

        setSupportActionBar(toolbar)
        toolbar.title = title


        fab.setOnClickListener { view ->
            //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
            //        .setAction("Action", null).show()
            startActivity(Intent(this, AddDeviceActivity::class.java));
        }

        if (finder_detail_container != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            twoPane = true
        }



        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(NotificationChannel("test","Test",NotificationManager.IMPORTANCE_DEFAULT))

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(broadCastReceiver, IntentFilter("FINDER_CHANGED"))
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(broadCastReceiver)
    }

    var idTest=1
    fun notifyTest(prio :Int) {
        Log.d("FinderListActivity","Testing notification ${prio}")
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val test = NotificationCompat.Builder(this , "test")
                .setPriority(prio)
                .setContentTitle("Prio ${prio}")
                .setTicker("Ticker")
                .setContentText("ContentText")
                .setSubText("SubText")
                .setSmallIcon(R.drawable.ic_notify_icon_bt)
                //.setLargeIcon(R.drawable.ic_notify_icon_bt)
                .build();
        notificationManager.notify(idTest++, test)

    }

    private val backgroundScannerCallback = Runnable { BleReceiver.startBackgroundScanner(this) }

    override fun onStart() {
        super.onStart()
        updateFinderList()

        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (!adapter.isEnabled) {
            startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), NOTIFY_ID_BLUETOOTH_DISABLED)
        } else {
            val noti = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            noti.cancel(NOTIFY_ID_BLUETOOTH_DISABLED)
        }

        Log.i(TAG, "onStart")
        //TrackerService.startBgScanner(this)
        TrackerBgScanJobService.schedule(this)

        if (TrackerBgScanJobService.isAutomaticBgScanningEnabled(this))
            handler.postDelayed(backgroundScannerCallback, 3000)


        val ctrl = applicationContext as MyApplication
        Log.i(TAG, "App Context: ${ctrl}")

        TrackerIntentService.connectAllKnownDevices(this)

        /*notifyTest(Notification.PRIORITY_MIN)
        notifyTest(Notification.PRIORITY_LOW)
        notifyTest(Notification.PRIORITY_DEFAULT)
        notifyTest(Notification.PRIORITY_HIGH)
        notifyTest(Notification.PRIORITY_MAX)
*/
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(backgroundScannerCallback)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == NOTIFY_ID_BLUETOOTH_DISABLED){
            val noti = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            noti.cancel(NOTIFY_ID_BLUETOOTH_DISABLED)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.finder_list_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                return true
            }
            R.id.test -> {
                BleReceiver.startBackgroundScanner(this)
            }
        }
        return false
    }

    override fun onContextItemSelected(item: MenuItem?): Boolean {

        when (item!!.getItemId()) {
            R.id.rename -> {
                //Helper.MessageBox(this, "cm_id=${contextMenuId}  id=${item.itemId} text=${item.title}")
                val item = dbSession.knownDeviceDao.load(contextMenuId)
                if(item == null) return true
                Helper.InputBox(this, "", item.displayName) { result ->
                    if (result != null) {
                        item.displayName = result
                        dbSession.knownDeviceDao.update(item)
						updateFinderList()
                    }
                }
                return true
            }
            R.id.delete -> {
                dbSession.knownDeviceDao.deleteByKey(contextMenuId)
                updateFinderList()
                return true
            }
            else -> return super.onContextItemSelected(item)
        }
    }

    private fun retrieveFinders(): List<KnownDevice> {
        return dbSession.knownDeviceDao.loadAll()
    }

    private fun updateFinderList() {
        finder_list.adapter = SimpleItemRecyclerViewAdapter(this, retrieveFinders(), twoPane)
    }

    var contextMenuId : Long = 0

    inner class SimpleItemRecyclerViewAdapter(private val parentActivity: FinderListActivity,
                                        private val values: List<KnownDevice>,
                                        private val twoPane: Boolean) :
            RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>(),
            View.OnClickListener,
            View.OnCreateContextMenuListener {


        override fun onClick(v: View?) {
            val item = v!!.tag as KnownDevice
            if (twoPane) {
                val fragment = FinderDetailFragment().apply {
                    arguments = Bundle().apply {
                        putLong(ARG_ITEM_ID, item.id)
                    }
                }
                parentActivity.supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.finder_detail_container, fragment)
                        .commit()
            } else {
                val intent = Intent(v.context, FinderDetailActivity::class.java).apply {
                    putExtra(ARG_ITEM_ID, item.id)
                }
                v.context.startActivity(intent)
            }
        }

        override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
            val item = v!!.tag as KnownDevice
            Log.d(TAG, "onCreateContextMenu call for ${v!!}")
            val inflater = parentActivity.menuInflater
            inflater.inflate(R.menu.finderlist_context_menu, menu)
             contextMenuId = item.id
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.finder_list_content, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = values[position]
            holder.idView.text = "(${item.id}) ${item.bluetoothAddress}"
            holder.contentView.text = item.displayName

            holder.itemView.tag = item
            holder.itemView.setOnClickListener(this)
            holder.itemView.setOnCreateContextMenuListener(this)
            val app = applicationContext as MyApplication
            val color : Int = if(app.getConnectedDeviceInstance(item).isConnected())  0xFF009900.toInt() else Color.RED
            holder.iconView.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
            //holder.iconView.setImageDrawable(icon)
        }

        override fun getItemCount() = values.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val idView: TextView = view.id_text
            val contentView: TextView = view.content
            val iconView: ImageView = view.icon
        }



    }
}
