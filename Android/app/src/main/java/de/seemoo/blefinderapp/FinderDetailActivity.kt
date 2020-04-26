package de.seemoo.blefinderapp

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.content.res.AppCompatResources
import android.util.Log
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_finder_detail.*

/**
 * An activity representing a single Finder detail screen. This
 * activity is only used on narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a [FinderListActivity].
 */
private const val TAG="FinderDetailActivity"
class FinderDetailActivity : AppCompatActivity() {

    lateinit var child : FinderDetailFragment
    var connectedMode = false

    var itemId :Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_finder_detail)
        setSupportActionBar(detail_toolbar)

        fab.setOnClickListener { view ->
            if (connectedMode) {
                child.ring()
            } else {
                val i = Intent(this, MapsActivity::class.java)
                i.putExtra(ARG_ITEM_ID, itemId)
                startActivity(i)
            }
        }


        // Show the Up button in the action bar.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            val fragment = FinderDetailFragment().apply {
                arguments = Bundle().apply {
                    itemId = intent.getLongExtra(ARG_ITEM_ID, 0)
                    if (itemId == 0L) {
                        Log.e(TAG, "itemId is zero, closing activity")
                        finish()
                        return
                    }
                    Log.d(TAG, "ARG_ITEM_ID = $itemId")
                    putLong(ARG_ITEM_ID, itemId)
                }
            }

            child = fragment
            supportFragmentManager.beginTransaction()
                    .add(R.id.finder_detail_container, fragment)
                    .commit()
        }
    }

    fun setConnMode(connected:Boolean) {
        connectedMode = connected
        if (connected)
            fab?.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_action_white_bell_ring))
        else
            fab?.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_action_white_location))
    }

    override fun onOptionsItemSelected(item: MenuItem) =
            when (item.itemId) {
                android.R.id.home -> {
                    // This ID represents the Home or Up button. In the case of this
                    // activity, the Up button is shown. For
                    // more details, see the Navigation pattern on Android Design:
                    //
                    // http://developer.android.com/design/patterns/navigation.html#up-vs-back

                    navigateUpTo(Intent(this, FinderListActivity::class.java))
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
}
