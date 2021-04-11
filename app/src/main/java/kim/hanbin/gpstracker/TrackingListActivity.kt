package kim.hanbin.gpstracker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import kim.hanbin.gpstracker.databinding.ActivityTrackingListBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class TrackingListActivity : AppCompatActivity() {

    private var mBinding: ActivityTrackingListBinding? = null
    private val binding get() = mBinding!!
    lateinit var trackingNumList: List<Int>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityTrackingListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val db = InnerDB.getInstance(this)
        GlobalScope.launch {

            trackingNumList = db.getAllTrackList()
            val list = arrayListOf<String>()
            for (i in trackingNumList)
                list.add(i.toString())
            val adapter: ArrayAdapter<String> =
                ArrayAdapter<String>(
                    this@TrackingListActivity,
                    android.R.layout.simple_list_item_1,
                    list
                )
            launch(Dispatchers.Main) {
                binding.trackingList.adapter = adapter
            }
        }
        binding.trackingList.setOnItemClickListener { _: AdapterView<*>, _: View, i: Int, _: Long ->
            startActivity(
                Intent(
                    this@TrackingListActivity,
                    MapsActivity::class.java
                ).putExtra("trackingNum", trackingNumList[i])
            )
        }
    }
}