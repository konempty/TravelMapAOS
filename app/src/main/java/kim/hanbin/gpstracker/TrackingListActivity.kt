package kim.hanbin.gpstracker

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kim.hanbin.gpstracker.databinding.ActivityTrackingListBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
            val item = trackingNumList[i]
            val actions = arrayOf<CharSequence>("삭제", "이름변경")

            val builder: AlertDialog.Builder = AlertDialog.Builder(this, R.style.MyDialogTheme)

            builder.setTitle(item.name)
            builder.setItems(
                actions
            ) { dialog, i ->
                if (i == 0) {
                    val dialog2 = AlertDialog.Builder(this, R.style.MyDialogTheme)
                        .setMessage("정말로 '${item.name}'을(를) 삭제하시겠습니까?\n삭제후 복구는 불가능합니다.")
                        .setPositiveButton("예") { dialogInterface: DialogInterface, i: Int ->
                            CoroutineScope(Dispatchers.IO).launch {
                                db.deleteLogs(item.trackingNum)
                                refreshList()
                            }
                        }
                        .setNegativeButton("아니요") { dialogInterface: DialogInterface, i: Int -> }
                        .create()
                    dialog2.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    dialog2.show()
                } else {
                    val dialog = TrackingNameDialog(this)
                    dialog.setTitle("여행기록저장")
                        .setMsg("변경할 여행기록 이름을 입력해주세요.")
                        .setCancelable(true)
                        .setDefaultName(item.name, true)
                        .setOkListener {
                            var name = dialog.name.text.toString()
                            if (name.isEmpty()) {
                                name = item.name
                            }
                            CoroutineScope(Dispatchers.IO).launch {
                                db.updateTrckingData(name, item.id)
                                refreshList()
                            }


                        }.show()
                }
            }

            val dialog: AlertDialog = builder.create()
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            dialog.show()
            true
        }
        binding.back.setOnClickListener { finish() }
    }

    fun refreshList() {
        CoroutineScope(Dispatchers.IO).launch {

            trackingNumList = db.getAllTrackList()
            val list = arrayListOf<String>()
            val sdf = SimpleDateFormat("yyyy.MM.dd hh:mm", Locale.KOREAN)
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