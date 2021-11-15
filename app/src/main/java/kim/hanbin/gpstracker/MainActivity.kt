package kim.hanbin.gpstracker

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.facebook.login.LoginManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.gson.JsonObject
import kim.hanbin.gpstracker.RetrofitFactory.Companion.retrofit
import kim.hanbin.gpstracker.databinding.ActivityMainBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.Semaphore


class MainActivity : AppCompatActivity(), Application.ActivityLifecycleCallbacks {
    // 전역 변수로 바인딩 객체 선언
    companion object {
        var instance: MainActivity? = null
        val semaphore = Semaphore(1)
    }

    val mAuth: FirebaseAuth by lazy { Firebase.auth }
    private lateinit var popup: ProgressPopup

    val btns: ArrayList<Button> by lazy {
        arrayListOf(
            binding.photo,
            binding.album,
            binding.map,
            binding.tracking
        )
    }
    val loginResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            login()
        }

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
        binding.openBtn.setOnClickListener {
            binding.drawer.openDrawer(GravityCompat.START)
        }
        binding.logoutBtn.setOnClickListener {
            logout()
        }
        binding.withdrawalBtn.setOnClickListener {
            binding.drawer.closeDrawer(GravityCompat.START)
            AlertDialog.Builder(this, R.style.MyDialogTheme)
                .setTitle("회원 탈퇴")
                .setMessage("정말로 회원탈퇴를 하시겠습니까? 회원탈퇴시 모든 정보는 삭제되며 복구가 불가능합니다.")
                .setPositiveButton(
                    "예"
                ) { dialogInterface, i ->
                    showProgress()
                    retrofit.deleteUser().enqueue(object : Callback<JsonObject> {
                        override fun onResponse(
                            call: Call<JsonObject>,
                            response: Response<JsonObject>
                        ) {
                            hideProgress()
                            val json = response.body()!!
                            if (json["success"].asBoolean) {
                                Toast.makeText(
                                    this@MainActivity,
                                    "회원탈퇴 되었습니다.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                setNickname(null)
                            } else {
                                Toast.makeText(
                                    this@MainActivity,
                                    "문제가 발생했습니다. 잠시후 다시 시도해주세요.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                        override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                            hideProgress()
                            Toast.makeText(
                                this@MainActivity,
                                "문제가 발생했습니다. 잠시후 다시 시도해주세요.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    })
                }.setNegativeButton("아니요") { dialogInterface, i -> }.create().show()

        }
        binding.loginBtn.setOnClickListener {
            binding.drawer.closeDrawer(GravityCompat.START)
            if (mAuth.currentUser == null) {
                loginResult.launch(Intent(this, LoginActivity::class.java))
            } else {
                login()
            }
        }
        binding.friendListBtn.setOnClickListener {
            binding.drawer.closeDrawer(GravityCompat.START)
            startActivity(Intent(this@MainActivity, FriendActivity::class.java))
        }
        if (mAuth.currentUser != null) {
            showProgress()
            mAuth.currentUser!!.getIdToken(true)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        val token = it.result.token!!
                        val res: Call<JsonObject> =
                            retrofit
                                .login(token)

                        res.enqueue(object : Callback<JsonObject?> {
                            override fun onResponse(
                                call: Call<JsonObject?>?,
                                response: Response<JsonObject?>
                            ) {
                                hideProgress()
                                val json = response.body()!!

                                if (json.get("success").asBoolean) {
                                    val nickname = json.get("result").asString
                                    Toast.makeText(
                                        this@MainActivity,
                                        "${nickname}님 환영합니다!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    setNickname(nickname)
                                } else {
                                    mAuth.signOut()
                                    LoginManager.getInstance().logOut();
                                }

                            }

                            override fun onFailure(
                                call: Call<JsonObject?>?,
                                t: Throwable?
                            ) {
                                hideProgress()
                                Toast.makeText(
                                    this@MainActivity,
                                    "문제가 발생했습니다. 잠시후 다시 시도해주세요.",
                                    Toast.LENGTH_LONG
                                ).show()
                                mAuth.signOut()
                                LoginManager.getInstance().logOut();
                                finish()

                            }
                        })

                    } else {
                        hideProgress()
                        Toast.makeText(
                            this@MainActivity,
                            "문제가 발생했습니다. 잠시후 다시 시도해주세요.",
                            Toast.LENGTH_SHORT
                        ).show()
                        mAuth.signOut()
                        LoginManager.getInstance().logOut();
                        finish()
                    }
                }
        }

    }

    fun logout() {
        retrofit.logout().enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                binding.drawer.closeDrawer(GravityCompat.START)
                if (response.body() == "success") {
                    Toast.makeText(
                        this@MainActivity,
                        "로그아웃 되었습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                    setNickname(null)
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "문제가 발생했습니다. 잠시후 다시 시도해주세요.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                binding.drawer.closeDrawer(GravityCompat.START)
                Toast.makeText(
                    this@MainActivity,
                    "문제가 발생했습니다. 잠시후 다시 시도해주세요.",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
        mAuth.signOut()
        LoginManager.getInstance().logOut();
    }

    fun login() {
        showProgress()
        mAuth.currentUser!!.getIdToken(true)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val token = it.result.token!!
                    val res: Call<JsonObject> =
                        retrofit
                            .login(token)

                    res.enqueue(object : Callback<JsonObject> {
                        override fun onResponse(
                            call: Call<JsonObject>?,
                            response: Response<JsonObject>
                        ) {
                            hideProgress()


                            val json = response.body()!!

                            if (json.get("success").asBoolean) {
                                val nickname = json.get("result").asString
                                Toast.makeText(
                                    this@MainActivity,
                                    "${nickname}님 환영합니다!",
                                    Toast.LENGTH_LONG
                                ).show()
                                setNickname(nickname)
                            } else {

                                when (json.get("result").asString) {
                                    "noUID" -> {

                                        Toast.makeText(
                                            this@MainActivity,
                                            "닉네임을 설정해주세요!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        val dialog = NicknameDialog(this@MainActivity)
                                        dialog.setOkListener {
                                            val res2: Call<JsonObject> =
                                                retrofit
                                                    .register(token, dialog.nickname)
                                            res2.enqueue(object : Callback<JsonObject> {
                                                override fun onResponse(
                                                    call: Call<JsonObject>,
                                                    response: Response<JsonObject>
                                                ) {
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        "${dialog.nickname}님 환영합니다!",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                    setNickname(dialog.nickname)
                                                }

                                                override fun onFailure(
                                                    call: Call<JsonObject>,
                                                    t: Throwable
                                                ) {
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        "문제가 발생했습니다. 잠시후 다시 시도해주세요.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            })
                                        }.show()
                                    }
                                    "invalidUser" -> Toast.makeText(
                                        this@MainActivity,
                                        "문제가 발생했습니다. 잠시후 다시 시도해주세요.",
                                        Toast.LENGTH_LONG
                                    ).show()

                                }
                            }

                        }

                        override fun onFailure(call: Call<JsonObject>?, t: Throwable?) {
                            hideProgress()
                            Toast.makeText(
                                this@MainActivity,
                                "문제가 발생했습니다. 잠시후 다시 시도해주세요.",
                                Toast.LENGTH_LONG
                            ).show()

                        }
                    })

                } else {
                    hideProgress()
                    Toast.makeText(
                        this@MainActivity,
                        "문제가 발생했습니다. 잠시후 다시 시도해주세요.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    fun setNickname(nickName: String?) {
        binding.nicknameTV.text = if (nickName == null) {
            binding.loginLayout.visibility = View.GONE
            binding.logoutLayout.visibility = View.VISIBLE
            "로그인을 해주세요."
        } else {
            binding.logoutLayout.visibility = View.GONE
            binding.loginLayout.visibility = View.VISIBLE
            "${nickName}님 환영합니다!"
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

    private fun showProgress() {
        popup = ProgressPopup(this)
        try {
            popup.show()
        } catch (e: IllegalArgumentException) {
            Log.e(this.javaClass.simpleName, "showProgress IllegalArgumentException")
        } catch (e: RuntimeException) {
            Log.e(this.javaClass.simpleName, "showProgress RuntimeException")
        } catch (e: Exception) {
            Log.e(this.javaClass.simpleName, "showProgress Exception")
        }
    }


    private fun hideProgress() {
        try {
            popup.dismiss()
        } catch (e: IllegalArgumentException) {
            Log.e(this.javaClass.simpleName, "hideProgress IllegalArgumentException")
        } catch (e: RuntimeException) {
            Log.e(this.javaClass.simpleName, "hideProgress RuntimeException")
        } catch (e: Exception) {
            Log.e(this.javaClass.simpleName, "hideProgress Exception")
        }
    }

}