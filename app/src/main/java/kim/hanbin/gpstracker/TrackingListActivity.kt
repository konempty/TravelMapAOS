package kim.hanbin.gpstracker

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.gson.JsonObject
import kim.hanbin.gpstracker.RetrofitFactory.Companion.retrofit
import kim.hanbin.gpstracker.databinding.ActivityTrackingListBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*


class TrackingListActivity : AppCompatActivity() {

    private var mBinding: ActivityTrackingListBinding? = null
    private val binding get() = mBinding!!
    lateinit var trackingNumList: List<TrackingListData>
    val db by lazy { InnerDB.getInstance(this) }
    val loginResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            loginAndSendData()
        } else {
            Toast.makeText(this, "여행기록을 공유하기위해선 로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
        }

    }
    lateinit var dialog3: ShareSelectDialog

    val mAuth: FirebaseAuth by lazy { Firebase.auth }
    private lateinit var popup: ProgressPopup
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
        binding.trackingList.setOnItemLongClickListener { adapterView, view, position, l ->
            val item = trackingNumList[position]
            val actions = arrayOf<CharSequence>("삭제", "이름변경", "여행 공유")

            val builder: AlertDialog.Builder = AlertDialog.Builder(this, R.style.MyDialogTheme)

            builder.setTitle(item.name)
            builder.setItems(
                actions
            ) { dialog, i ->
                when (i) {
                    0 -> {
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
                    }
                    1 -> {
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
                    2 -> {
                        var datas: List<EventData> = arrayListOf()
                        CoroutineScope(Dispatchers.IO).launch {

                            datas = db.getTrackingLog(trackingNumList[position].trackingNum)
                        }
                        val dialog2 = AlertDialog.Builder(this, R.style.MyDialogTheme)
                            .setMessage("'${item.name}'을(를) 다른사람들에게 공유하시겠습니까?")
                            .setPositiveButton("예") { dialogInterface: DialogInterface, i: Int ->

                                dialog3 = ShareSelectDialog(this, datas)
                                dialog3.setOkListener {
                                    ShareProgressDialog(
                                        this,
                                        trackingNumList[position].name,
                                        datas,
                                        dialog3.shareSlectrion,
                                        dialog3.qualitySelection,
                                        dialog3.pswd
                                    )
                                }

                                if (mAuth.currentUser == null) {
                                    loginResult.launch(Intent(this, LoginActivity::class.java))
                                } else
                                    loginAndSendData()
                            }
                            .setNegativeButton("아니요") { dialogInterface: DialogInterface, i: Int -> }
                            .create()
                        dialog2.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                        dialog2.show()
                    }
                }
            }

            val dialog: AlertDialog = builder.create()
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            dialog.show()
            true
        }
        binding.back.setOnClickListener { finish() }
    }

    fun loginAndSendData() {
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
                                    this@TrackingListActivity,
                                    "${nickname}님 환영합니다!",
                                    Toast.LENGTH_LONG
                                ).show()
                                dialog3.show()
                            } else {

                                when (json.get("result").asString) {
                                    "noUID" -> {

                                        Toast.makeText(this@TrackingListActivity, "닉네임을 설정해주세요!", Toast.LENGTH_SHORT).show()
                                        val dialog = NicknameDialog(this@TrackingListActivity)
                                        dialog.setOkListener {
                                            val res2: Call<JsonObject> =
                                                retrofit
                                                    .register(token, dialog.nickname)
                                            res2.enqueue(object : Callback<JsonObject>{
                                                override fun onResponse(
                                                    call: Call<JsonObject>,
                                                    response: Response<JsonObject>
                                                ) {
                                                    dialog3.show()
                                                }

                                                override fun onFailure(
                                                    call: Call<JsonObject>,
                                                    t: Throwable
                                                ) {
                                                    Toast.makeText(
                                                        this@TrackingListActivity,
                                                        "문제가 발생했습니다. 잠시후 다시 시도해주세요.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            })
                                        }.show()
                                    }
                                    "invalidUser" -> Toast.makeText(
                                        this@TrackingListActivity,
                                        "문제가 발생했습니다. 잠시후 다시 시도해주세요.",
                                        Toast.LENGTH_LONG
                                    ).show()

                                }
                            }

                        }

                        override fun onFailure(call: Call<JsonObject?>?, t: Throwable?) {
                            hideProgress()
                            Toast.makeText(
                                this@TrackingListActivity,
                                "문제가 발생했습니다. 잠시후 다시 시도해주세요.",
                                Toast.LENGTH_LONG
                            ).show()

                        }
                    })

                } else {
                    hideProgress()
                    Toast.makeText(this, "문제가 발생했습니다. 잠시후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                }
            }

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