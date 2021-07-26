package kim.hanbin.gpstracker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
    var isBack = false
    var Permissions = arrayListOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        MyPreference.initPreference(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Permissions.add(Manifest.permission.ACCESS_MEDIA_LOCATION)
            Permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            Permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        MainScope().launch {

            delay(2000)
            permission()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isBack) next()
        isBack = false
    }

    fun permission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var isGranted = true
            for (perm in Permissions) {
                if (ContextCompat.checkSelfPermission(
                        this, perm
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    isGranted = false
                    break
                }
            }
            if (!isGranted) {
                requestPermissions(
                    Permissions.toTypedArray(),
                    0
                )
            } else {

                next()
            }
        } else {
            next()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        // If request is cancelled, the result arrays are empty.
        var isGranted = true
        for (perm in grantResults) {
            if (perm != PackageManager.PERMISSION_GRANTED
            ) {
                isGranted = false
                break
            }
        }
        if (grantResults.size == Permissions.size && isGranted
        ) {
            next()
        } else {
            // Explain to the user that the feature is unavailable because
            // the features requires a permission that the user has denied.
            // At the same time, respect the user's decision. Don't link to
            // system settings in an effort to convince the user to change
            // their decision.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                for (perm in permissions) {
                    if (ContextCompat.checkSelfPermission(
                            this, perm
                        ) != PackageManager.PERMISSION_GRANTED && shouldShowRequestPermissionRationale(
                            perm
                        )
                    ) {
                        AlertDialog.Builder(this)
                            .setTitle("권한이 필요합니다.")
                            .setMessage("앱을 이용하기 위해 반드시 필요한 권한입니다. 설정을 통해 허용해 주세요.")
                            .setPositiveButton(
                                "예"
                            ) { _, _ ->
                                val intent = Intent()
                                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                val uri = Uri.fromParts("package", packageName, null)
                                intent.data = uri
                                startActivity(intent)
                                finish()
                            }.setNegativeButton("아니오") { _, _ ->
                                finish()
                            }.setCancelable(false).create().show()
                        return
                    }
                }
            }
            val dialog = AlertDialog.Builder(this, R.style.MyDialogTheme)
                .setTitle("권한 없음")
                .setMessage("권한이 없습니다. 권한없이는 앱을 사용 할수 없습니다.")
                .setPositiveButton(
                    "확인"
                ) { dialogInterface, i -> finish() }.create()

            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            dialog.show()
        }
    }

    operator fun next() {
        if (isIgnoringBatteryOptimizations(this)) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent) //다음화면으로 넘어감
            finish()
        } else {
            val dialog = AlertDialog.Builder(this, R.style.MyDialogTheme)
                .setTitle("권한요청")
                .setMessage("정확한 위치 탐색을 위해선 백그라운드 실행에 동의를 해주세요.")
                .setPositiveButton(
                    "확인"
                ) { dialogInterface, i ->
                    val intent =
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                    isBack = true
                }.create()

            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.show()
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(POWER_SERVICE) as PowerManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else true
    }
}