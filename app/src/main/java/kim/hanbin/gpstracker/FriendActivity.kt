package kim.hanbin.gpstracker

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.JsonObject
import kim.hanbin.gpstracker.RetrofitFactory.Companion.retrofit
import kim.hanbin.gpstracker.databinding.ActivityFriendBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FriendActivity : AppCompatActivity() {
    private var mBinding: ActivityFriendBinding? = null
    private val binding get() = mBinding!!
    private lateinit var popup: ProgressPopup
    lateinit var adapter: FriendListAdapter
    var isFiendList = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityFriendBinding.inflate(layoutInflater)
        setContentView(binding.root)
        adapter = FriendListAdapter(this)
        binding.friendList.adapter = adapter
        refreshData()
        binding.addFriendBtn.setOnClickListener {
            AddFriendDialog(this).show()
        }
        binding.toggleBtn.setOnClickListener {
            binding.toggleBtn.text = if (isFiendList) {
                "친구 리스트"
            } else {
                "받은 친구신청"
            }
            isFiendList = !isFiendList
            refreshData()
        }
        binding.back.setOnClickListener {
            finish()
        }
    }

    fun refreshData() {
        showProgress()
        if (isFiendList) {
            retrofit.getFriendList()
        } else {
            retrofit.getFriendRequestedList()
        }.enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                val json = response.body()!!
                if (json["success"].asBoolean) {
                    val list = ArrayList<FriendListAdapter.FriendItem>()
                    val arr = json["list"].asJsonArray
                    for (item in arr) {
                        val obj = item.asJsonObject
                        list.add(
                            FriendListAdapter.FriendItem(
                                obj["id"].asLong,
                                obj["nickname"].asString,
                                if (isFiendList) obj["isPartially"].asBoolean else true
                            )
                        )
                    }
                    adapter.changeData(list, isFiendList)
                } else {
                    Toast.makeText(
                        this@FriendActivity,
                        "문제가 발생했습니다. 잠시후 다시 시도해주세요.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                    MainActivity.instance?.logout()
                }
                hideProgress()
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                hideProgress()
                Toast.makeText(
                    this@FriendActivity,
                    "문제가 발생했습니다. 잠시후 다시 시도해주세요.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        })
    }

    fun showProgress() {
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


    fun hideProgress() {
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
