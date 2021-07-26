package kim.hanbin.gpstracker

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import kim.hanbin.gpstracker.databinding.ActivityMainBinding
import java.util.concurrent.Semaphore


class MainActivity : AppCompatActivity(), Application.ActivityLifecycleCallbacks {
    // 전역 변수로 바인딩 객체 선언
    companion object {
        var instance: MainActivity? = null
        val semaphore = Semaphore(1)
    }

    val btns: ArrayList<Button> by lazy {
        arrayListOf(
            binding.photo,
            binding.album,
            binding.map,
            binding.tracking
        )
    }

    private var activityReferences = 1
    private var isActivityChangingConfigurations = false

    private var mBinding: ActivityMainBinding? = null
    private val binding get() = mBinding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (instance != null)
            application.unregisterActivityLifecycleCallbacks(instance)
        application.registerActivityLifecycleCallbacks(this)
        val pagerAdapter = ScreenSlidePagerAdapter(this)
        binding.pager.adapter = pagerAdapter
        changeFragment(1)
        for (i in 0 until btns.size) {
            btns[i].setOnClickListener {
                changeFragment(i)
            }
        }
        binding.pager.isUserInputEnabled = false;
        binding.pager.setPageTransformer(null)
        binding.pager.animation = null
        instance = this
        semaphore.acquire()
        if (!PhotoService.isRunning) {
            startService(Intent(this, PhotoService::class.java))
        }
        semaphore.release()
        if (MyPreference.trackingState != 0) {

            if (MyPreference.trackingState != 1) {
                if (GPSTrackingService.instance == null) {
                    val i = Intent(this, GPSTrackingService::class.java);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(i);
                    } else {
                        startService(i);
                    }
                }
            }
        }
    }


    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

        override fun getItemCount(): Int = 4

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> DailyPhotoListFragment()
                1 -> AlbumFragment()
                2 -> MapFragment()
                else -> TrackingMenuFragment()
            }
        }

    }


    fun changeFragment(idx: Int) {
        for (btn in btns) {
            btn.setBackgroundColor(ContextCompat.getColor(this, R.color.dusk_light))
        }
        btns[idx].setBackgroundResource(R.drawable.gradient)

        binding.pager.currentItem = idx
        if (idx == 0)
            DailyPhotoListFragment.instance?.refreshHeight()
    }

    override fun onActivityCreated(p0: Activity, p1: Bundle?) {
    }

    override fun onActivityStarted(p0: Activity) {
        semaphore.acquire()
        if (++activityReferences == 1 && !isActivityChangingConfigurations && !PhotoService.isRunning) {

            startService(Intent(this, PhotoService::class.java))
        }
        semaphore.release()
    }

    override fun onActivityResumed(p0: Activity) {
    }

    override fun onActivityPaused(p0: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
        --activityReferences
    }

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
    }

    override fun onActivityDestroyed(p0: Activity) {
    }


}