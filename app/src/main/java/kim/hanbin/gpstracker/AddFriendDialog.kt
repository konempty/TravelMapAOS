package kim.hanbin.gpstracker

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import kim.hanbin.gpstracker.RetrofitFactory.Companion.retrofit
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddFriendDialog(private val context: FriendActivity) {
    private val builder: AlertDialog.Builder by lazy {
        AlertDialog.Builder(context).setView(view)
    }

    private val view: View by lazy {
        View.inflate(context, R.layout.dialog_add_friend, null)
    }

    private var dialog: AlertDialog? = null

    val nicknameET: EditText by lazy {
        view.findViewById(R.id.nickname)
    }
    var nickname = ""

    private lateinit var popup: ProgressPopup


    fun show() {
        dialog = builder.create()
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.setCancelable(true)
        nicknameET.addTextChangedListener(object : TextWatcher {


            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(charSequence: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (charSequence!!.length > 10) {
                    Toast.makeText(context, "10자이상 입력이 불가능 합니다.", Toast.LENGTH_SHORT).show()
                    nicknameET.setText(nicknameET.text.toString().substring(0, 10));
                    nicknameET.setSelection(nicknameET.length());
                }
            }

            override fun afterTextChanged(p0: Editable?) {
            }
        });
        view.findViewById<Button>(R.id.okBtn).setOnClickListener {
            showProgress()

            nickname = nicknameET.text.toString()
            val res: Call<String> =
                retrofit
                    .getUserId(nickname)

            res.enqueue(object : Callback<String> {
                override fun onResponse(
                    call: Call<String>?,
                    response: Response<String>
                ) {
                    val result = response.body()!!.toLong()
                    if (result == -1L) {

                        Toast.makeText(
                            context,
                            "존재하지 않는 사용자입니다.",
                            Toast.LENGTH_LONG
                        ).show()
                        hideProgress()
                    } else {
                       retrofit.addFriendRequest(result).enqueue(object:Callback<String>{
                           override fun onResponse(call: Call<String>, response: Response<String>) {
                               hideProgress()
                               val result = response.body()!!
                               when(result){
                                   "alreadyRequested"->{
                                       Toast.makeText(
                                           context,
                                           "이미 친구신청이 되어있습니다.",
                                           Toast.LENGTH_LONG
                                       ).show()
                                   }
                                   "success"->{
                                       Toast.makeText(
                                           context,
                                           "신청되었습니다.",
                                           Toast.LENGTH_SHORT
                                       ).show()
                                       dismiss()
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

                               hideProgress()
                           }
                       })
                    }
                }

                override fun onFailure(call: Call<String>?, t: Throwable?) {
                    Toast.makeText(
                        context,
                        "문제가 발생했습니다. 잠시후 다시 시도해주세요.",
                        Toast.LENGTH_LONG
                    ).show()

                    hideProgress()
                }
            })
        }
        dialog?.show()
    }




    fun dismiss() {
        dialog?.dismiss()
    }

    private fun showProgress() {
        popup = ProgressPopup(context)
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