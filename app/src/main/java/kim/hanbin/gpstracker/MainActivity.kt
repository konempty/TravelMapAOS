package kim.hanbin.gpstracker

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kim.hanbin.gpstracker.databinding.ActivityMainBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : AppCompatActivity() {
    // 전역 변수로 바인딩 객체 선언

    private var mBinding: ActivityMainBinding? = null
    private val binding get() = mBinding!!
    lateinit var db: EventDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = InnerDB.getInstance(this)
        if (MyPreference.trackingState != 0) {
            binding.trackingStEn.text = "여행 기록 종료"
            if (MyPreference.trackingState == 1)
                binding.trackingPause.text = "여행 기록 재개"
            else {
                if (GPSTrackingService.instance == null) {
                    val i = Intent(this, GPSTrackingService::class.java);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(i);
                    } else {
                        startService(i);
                    }
                }
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
                GlobalScope.launch {
                    val trackingNum: Int = if (db.getTrackingNum() == null) {
                        1
                    } else {
                        db.getTrackingNum()!! + 1
                    }
                    db.insert(EventData(trackingNum, 0, Date()))

                    startService()
                }
            } else {
                MyPreference.trackingState = 0
                binding.trackingStEn.text = "여행 기록 시작"
                binding.trackingSetting.visibility = View.GONE
                GlobalScope.launch {
                    db.insert(EventData(db.getTrackingNum()!!, 2, Date()))

                    GPSTrackingService.instance!!.realStopSelf()
                }
            }

        }
        binding.trackingPause.setOnClickListener {
            if (MyPreference.trackingState == 1) {
                GlobalScope.launch {
                    db.insert(EventData(db.getTrackingNum()!!, 0, Date()))

                    startService()

                }
                binding.trackingPause.text = "여행 기록 일시중지"
                MyPreference.trackingState = 2


            } else {
                GlobalScope.launch {
                    db.insert(EventData(db.getTrackingNum()!!, 1, Date()))

                }
                GPSTrackingService.instance!!.realStopSelf()
                binding.trackingPause.text = "여행 기록 재개"
                MyPreference.trackingState = 1
            }

        }
        binding.trackingList.setOnClickListener {
            startActivity(Intent(this, TrackingListActivity::class.java))
        }
        binding.spped1.setOnClickListener {

            GlobalScope.launch {
                MyPreference.trackingTime = 60000
                db.insert(EventData(db.getTrackingNum()!!, 4, 0, Date()))
                GPSTrackingService.instance!!.realStopSelf()
                delay(100)
                startService()
            }
        }
        binding.spped2.setOnClickListener {

            GlobalScope.launch {
                MyPreference.trackingTime = 300000
                db.insert(EventData(db.getTrackingNum()!!, 4, 1, Date()))
                GPSTrackingService.instance!!.realStopSelf()
                delay(100)
                startService()

            }
        }
        binding.spped3.setOnClickListener {

            GlobalScope.launch {
                MyPreference.trackingTime = 600000
                db.insert(EventData(db.getTrackingNum()!!, 4, 2, Date()))
                GPSTrackingService.instance!!.realStopSelf()
                delay(100)
                startService()

            }
        }
    }

    override fun onDestroy() {
        mBinding = null
        super.onDestroy()
    }

    fun startService() {
        val i = Intent(this, GPSTrackingService::class.java);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(i);
        } else {
            startService(i);
        }
    }


}