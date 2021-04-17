package kim.hanbin.gpstracker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import kim.hanbin.gpstracker.databinding.ActivityTrackingListBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TrackingListActivity : AppCompatActivity() {

    private var mBinding: ActivityTrackingListBinding? = null
    private val binding get() = mBinding!!
    lateinit var trackingNumList: List<TrackingListData>
    val db by lazy { InnerDB.getInstance(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityTrackingListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        refreshList()
        binding.trackingList.setOnItemClickListener { _: AdapterView<*>, _: View, i: Int, _: Long ->
            startActivity(
                Intent(
                    this@TrackingListActivity,
                    MapsActivity::class.java
                ).putExtra("trackingNum", trackingNumList[i].trackingNum)
            )
        }
        binding.trackingList.setOnItemLongClickListener { adapterView, view, i, l ->
            TrackingListMenuDialog(this, trackingNumList[i]).show()
            true
        }
    }

    fun refreshList() {
        CoroutineScope(Dispatchers.IO).
        launch {

            trackingNumList = db.getAllTrackList()
            val list = arrayListOf<String>()
            val sdf = SimpleDateFormat("yyyy.MM.dd hh:mm:ss", Locale.KOREAN)
            for (i in trackingNumList)
                list.add("${i.name}\n${sdf.format(i.startTime)} ~ ${sdf.format(i.endTime)}")
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
    }
}