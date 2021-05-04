package kim.hanbin.gpstracker

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import kim.hanbin.gpstracker.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    // 전역 변수로 바인딩 객체 선언
    val btns: ArrayList<Button> by lazy {
        arrayListOf(
            binding.photo,
            binding.album,
            binding.map,
            binding.tracking
        )
    }

    private var mBinding: ActivityMainBinding? = null
    private val binding get() = mBinding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
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
    }

    override fun onResume() {
        super.onResume()
        startService(Intent(this, PhotoService::class.java))
    }

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

        override fun getItemCount(): Int = 4

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> PhotoListFragment()
                1 -> AlbumFragment()
                2 -> MapFragment()
                else -> TrackingMenuFragment()
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, PhotoService::class.java))
    }

    fun changeFragment(idx: Int) {
        for (btn in btns) {
            btn.background = null
        }
        btns[idx].setBackgroundColor(Color.YELLOW)

        binding.pager.currentItem = idx
    }

}