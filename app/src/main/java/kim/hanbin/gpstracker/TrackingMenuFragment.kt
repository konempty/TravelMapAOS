package kim.hanbin.gpstracker

import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kim.hanbin.gpstracker.databinding.FragmentTrackingMenuBinding
import kotlinx.coroutines.*
import java.util.*

class TrackingMenuFragment : Fragment() {

    private var mBinding: FragmentTrackingMenuBinding? = null
    private val binding get() = mBinding!!
    val db: EventDao by lazy { InnerDB.getInstance(requireContext()) }
    val time = arrayListOf<Long>(30000, 150000, 300000)


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        mBinding = FragmentTrackingMenuBinding.inflate(inflater, container, false)

        if (MyPreference.trackingState != 0) {
            binding.trackingStEn.text = "여행 기록 종료"
            changeColor(time.indexOf(MyPreference.trackingTime))

            if (MyPreference.trackingState == 1)
                binding.trackingPause.text = "여행 기록 재개"
            else {
                binding.trackingPause.text = "여행 기록 일시중지"
            }

            binding.trackingSetting.visibility = View.VISIBLE
        }
        binding.trackingStEn.setOnClickListener {

            if (MyPreference.trackingState == 0) {
                MyPreference.trackingState = 2
                binding.trackingStEn.text = "여행 기록 종료"
                binding.trackingPause.text = "여행 기록 일시중지"
                binding.trackingSetting.visibility = View.VISIBLE
                changeColor(time.indexOf(MyPreference.trackingTime))
                CoroutineScope(Dispatchers.IO).launch {
                    val trackingNum: Int = if (db.getTrackingNum() == null) {
                        1
                    } else {
                        db.getTrackingNum()!! + 1
                    }
                    db.insert(EventData(trackingNum, 0, Date()))

                    startService()
                }
            } else {

                AlertDialog.Builder(requireContext(), R.style.MyDialogTheme)
                    .setMessage("여행기록을 끝내시겠습니까?")
                    .setPositiveButton("예") { dialogInterface: DialogInterface, i: Int ->
                        val dialog = TrackingNameDialog(requireContext())

                        CoroutineScope(Dispatchers.IO).launch {
                            val trackingnum = db.getTrackingNum()!!
                            MainScope().launch {
                                dialog.setTitle("여행기록저장")
                                    .setMsg("이번 여행을 기억할수 있게 여행 기록에 이름을 정해주세요")
                                    .setDefaultName("여행기록$trackingnum", false)
                                    .setOkListener {
                                        var name = dialog.name.text.toString()
                                        if (name.isEmpty()) {
                                            name = "여행기록$trackingnum"
                                        }
                                        MyPreference.trackingState = 0
                                        binding.trackingStEn.text = "여행 기록 시작"
                                        binding.trackingSetting.visibility = View.GONE
                                        CoroutineScope(Dispatchers.IO).launch {
                                            db.insert(EventData(trackingnum, 2, name, Date()))
                                            GPSTrackingService.instance?.realStopSelf()
                                        }

                                    }.show()
                            }
                        }
                    }
                    .setNegativeButton("아니요") { dialogInterface: DialogInterface, i: Int -> }
                    .create().show()
            }

        }
        binding.trackingPause.setOnClickListener {
            if (MyPreference.trackingState == 1) {
                CoroutineScope(Dispatchers.IO).launch {
                    db.insert(EventData(db.getTrackingNum()!!, 0, Date()))

                    startService()

                }
                binding.trackingPause.text = "여행 기록 일시중지"
                MyPreference.trackingState = 2


            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    db.insert(EventData(db.getTrackingNum()!!, 1, Date()))

                }
                GPSTrackingService.instance!!.realStopSelf()
                binding.trackingPause.text = "여행 기록 재개"
                MyPreference.trackingState = 1
            }

        }
        binding.trackingList.setOnClickListener {
            startActivity(Intent(context, TrackingListActivity::class.java))
        }
        binding.speed1.setOnClickListener {

            changeSpeed(0)
        }
        binding.speed2.setOnClickListener {

            changeSpeed(1)
        }
        binding.speed3.setOnClickListener {

            changeSpeed(2)
        }
        return binding.root
    }

    override fun onDestroy() {
        mBinding = null
        super.onDestroy()
    }

    fun startService() {
        val i = Intent(context, GPSTrackingService::class.java);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(i);
        } else {
            requireContext().startService(i);
        }
    }

    fun changeSpeed(speed: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            MyPreference.trackingTime = time[speed]
            db.insert(EventData(db.getTrackingNum()!!, 4, speed, Date()))
            if (GPSTrackingService.instance != null) {
                GPSTrackingService.instance!!.realStopSelf()
                delay(100)
                startService()
            }

        }
        changeColor(speed)
    }

    fun changeColor(speed: Int) {
        val btns = arrayListOf(binding.speed1, binding.speed2, binding.speed3)
        for (btn in btns) {
            btn.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.dark))
        }
        btns[speed].setBackgroundResource(R.drawable.gradient_round)
    }

}