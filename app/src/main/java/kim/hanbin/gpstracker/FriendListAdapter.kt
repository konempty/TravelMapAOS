package kim.hanbin.gpstracker

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import kim.hanbin.gpstracker.RetrofitFactory.Companion.retrofit
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FriendListAdapter(val context: FriendActivity) : BaseAdapter() {
    class FriendItem(val id: Long, val nickname: String, val isPartially: Boolean)

    var isFriendList = true
    var friendList = arrayListOf<FriendItem>()

    fun changeData(friendList: ArrayList<FriendItem>, isFriendList: Boolean) {
        this.isFriendList = isFriendList
        this.friendList = friendList
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return friendList.size
    }

    override fun getItem(p0: Int): Any {
        return friendList[p0]
    }

    override fun getItemId(p0: Int): Long {
        return p0.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var convertView: View? = convertView
        val user = friendList[position]

        if (convertView == null) {
            val inflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = inflater.inflate(R.layout.friend_item, parent, false)
        }
        convertView!!.findViewById<TextView>(R.id.nickname).text = user.nickname

        val acceptBtn = convertView.findViewById<Button>(R.id.acceptBtn)
        if(isFriendList){
            acceptBtn.visibility = View.GONE
        }else{
            acceptBtn.visibility = View.VISIBLE
            acceptBtn.setOnClickListener {
                context.showProgress()
                retrofit.addFriendRequest(user.id).enqueue(object:Callback<String>{
                    override fun onResponse(call: Call<String>, response: Response<String>) {
                        val result = response.body()!!
                        context.hideProgress()
                        when(result){
                            "alreadyRequested"->{
                                Toast.makeText(
                                    context,
                                    "이미 친구신청이 되어있습니다.",
                                    Toast.LENGTH_LONG
                                ).show()
                                context.refreshData()
                            }
                            "success"->{
                                Toast.makeText(
                                    context,
                                    "친구가 되었습니다.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                context.refreshData()
                            }
                            else->{

                                Toast.makeText(
                                    context,
                                    "문제가 발생했습니다. 잠시후 다시 시도해주세요.",
                                    Toast.LENGTH_LONG
                                ).show()
                                context.finish()
                                MainActivity.instance?.logout()
                            }
                        }
                    }

                    override fun onFailure(call: Call<String>, t: Throwable) {
                        Toast.makeText(
                            context,
                            "문제가 발생했습니다. 잠시후 다시 시도해주세요.",
                            Toast.LENGTH_LONG
                        ).show()
                        context.hideProgress()
                    }
                })
            }
        }
        convertView.findViewById<ConstraintLayout>(R.id.background).setBackgroundColor(if(user.isPartially){
           Color.GRAY
        }else{
            ContextCompat.getColor(context, R.color.dark)

        })
        convertView.findViewById<Button>(R.id.deleteBtn).setOnClickListener {
            AlertDialog.Builder(context, R.style.MyDialogTheme)
                .setTitle("친구 리스트")
                .setMessage(user.nickname + if (isFriendList) if (user.isPartially) "님 친구신청을 취소하시겠습니까?" else "님을 친구 삭제하시겠습니까?" else "님 친구신청을 거절하시겠습니까?")
                .setPositiveButton(
                    "예"
                ) { dialog, i ->
                    context.showProgress()
                    println("?")
                    retrofit.deleteFriend(user.id).enqueue(object : Callback<String> {
                        override fun onResponse(call: Call<String>, response: Response<String>) {
                            context.hideProgress()
                            if (response.body() == "noUID") {
                                Toast.makeText(
                                    context,
                                    "문제가 발생했습니다. 잠시후 다시 시도해주세요.",
                                    Toast.LENGTH_LONG
                                ).show()
                                context.finish()
                                MainActivity.instance?.logout()
                            } else {
                                context.refreshData()
                            }
                        }

                        override fun onFailure(call: Call<String>, t: Throwable) {
                            Toast.makeText(
                                context,
                                "문제가 발생했습니다. 잠시후 다시 시도해주세요.",
                                Toast.LENGTH_LONG
                            ).show()
                            context.hideProgress()
                        }
                    })
                }
                .setNegativeButton("아니요") { dialog, i ->

                }.create().show()
        }

        return convertView
    }
}

