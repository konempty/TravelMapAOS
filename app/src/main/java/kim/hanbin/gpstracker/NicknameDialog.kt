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
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NicknameDialog(private val context: Context) {
    private val builder: AlertDialog.Builder by lazy {
        AlertDialog.Builder(context).setView(view)
    }

    private val view: View by lazy {
        View.inflate(context, R.layout.dialog_nickname, null)
    }
    val name: EditText by lazy {
        view.findViewById(R.id.name)
    }

    private var dialog: AlertDialog? = null


    private lateinit var okClickListener: View.OnClickListener

    val nicknameET: EditText by lazy {
        view.findViewById(R.id.nickname)
    }
    var nickname = ""

    private lateinit var popup: ProgressPopup
    private val SERVER_API_URL = "https://124.54.119.156/"


    fun show() {
        dialog = builder.create()
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.setCancelable(true)
        nicknameET.addTextChangedListener(object : TextWatcher {


            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(charSequence: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (charSequence!!.length > 10) {
                    Toast.makeText(context,"10자이상 입력이 불가능 합니다.",Toast.LENGTH_SHORT).show()
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
                    .checkNickname(nickname)

            res.enqueue(object : Callback<String> {
                override fun onResponse(
                    call: Call<String>?,
                    response: Response<String>
                ) {
                    val result = response.body()!!
                    if (result == "available") {

                        Toast.makeText(
                            context,
                            "${nickname}님 환영합니다!",
                            Toast.LENGTH_LONG
                        ).show()
                        dismiss()
                        okClickListener.onClick(it)
                    } else {
                        Toast.makeText(
                            context,
                            "중복된 닉네임입니다. 다른 닉네임을 입력해주세요.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    hideProgress()
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
        dialog?.setOnCancelListener {
            Toast.makeText(context, "닉네임을 설정해야 공유기능을 사용할 수 있습니다.", Toast.LENGTH_SHORT).show()
        }
        dialog?.show()
    }


    fun setOkListener(listener: View.OnClickListener): NicknameDialog {
        okClickListener = listener
        return this
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