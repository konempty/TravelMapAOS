package kim.hanbin.gpstracker

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.InputFilter
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.util.regex.Matcher
import java.util.regex.Pattern


class ShareSelectDialog(private val context: Context, val datas: List<EventData>) {

    private val builder: AlertDialog.Builder by lazy {
        AlertDialog.Builder(context).setView(view)
    }

    private val view: View by lazy {
        View.inflate(context, R.layout.dialog_share_select, null)
    }
    private val shareAllBtn: Button by lazy {
        view.findViewById(R.id.shareAll)
    }
    private val shareFriendsBtn: Button by lazy {
        view.findViewById(R.id.shareFriends)
    }
    private val shareLockBtn: Button by lazy {
        view.findViewById(R.id.shareLock)
    }

    private val qualityOriginBtn: Button by lazy {
        view.findViewById(R.id.qualityOrigin)
    }
    private val qualityHighBtn: Button by lazy {
        view.findViewById(R.id.qualityHigh)
    }
    private val qualityLowBtn: Button by lazy {
        view.findViewById(R.id.qualityLow)
    }
    private val password_form: LinearLayout by lazy {
        view.findViewById(R.id.password_form)
    }
    private val password: EditText by lazy {
        view.findViewById(R.id.password)
    }
    private val password_check: EditText by lazy {
        view.findViewById(R.id.password_check)
    }
    private val btns1: ArrayList<Button> by lazy {
        arrayListOf(
            shareAllBtn,
            shareFriendsBtn,
            shareLockBtn
        )
    }
    private val btns2: ArrayList<Button> by lazy {
        arrayListOf(
            qualityOriginBtn,
            qualityHighBtn,
            qualityLowBtn
        )
    }

    var shareSlectrion = 0
    var qualitySelection = 0
    var pswd = ""

    private var dialog: AlertDialog? = null


    private var okClickListener: View.OnClickListener? = null

    fun show() {
        dialog = builder.create()
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val filter =
            InputFilter { source, start, end, dest, dstart, dend ->
                val p: Pattern =
                    Pattern.compile("^[a-zA-Z|0-9!@#\$%^&*()+_\\-\\\\.,/?;:'\"\\[{}\\]~`]*$")
                val m: Matcher = p.matcher(source.toString())
                if (!m.matches()) {
                    Toast.makeText(context, "영문,숫자,특수기호만 입력해주세요.", Toast.LENGTH_SHORT).show()
                    ""
                } else null
            }
        password.filters = arrayOf(filter)
        password.transformationMethod = TMPasswordTransformationMethod()
        password_check.filters = arrayOf(filter)
        password_check.transformationMethod = TMPasswordTransformationMethod()

        view.findViewById<Button>(R.id.okBtn).setOnClickListener {
            if (shareSlectrion == 2) {
                pswd = password.text.toString()
                val pswd_ch = password_check.text.toString()

                if (pswd.isEmpty() || pswd_ch.isEmpty()) {
                    Toast.makeText(context, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                } else if (pswd != pswd_ch) {
                    Toast.makeText(context, "비밀번호와 비밀번호 확인의 내용이 같지않습니다.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }
            dismiss()
            okClickListener?.onClick(it)
        }
        dialog?.show()
        changeColor1(0)
        changeColor2(0)

        for (i in btns1.indices) {
            btns1[i].setOnClickListener {
                shareSlectrion = i
                changeColor1(i)
            }
        }
        for (i in btns2.indices) {
            btns2[i].setOnClickListener {
                qualitySelection = i
                changeColor2(i)
            }
        }
    }

    fun setOkListener(listener: View.OnClickListener): ShareSelectDialog {
        okClickListener = listener
        return this
    }


    fun dismiss() {
        dialog?.dismiss()
    }

    fun changeColor1(num: Int) {
        for (btn in btns1) {
            btn.setBackgroundColor(ContextCompat.getColor(context, R.color.dark))
        }
        btns1[num].setBackgroundResource(R.drawable.dust_light_round)
        if (num == 2) {
            password_form.visibility = View.VISIBLE
        } else {
            password.setText( "")
            password_check.setText( "")
            password_form.visibility = View.GONE
        }
    }

    fun changeColor2(num: Int) {
        for (btn in btns2) {
            btn.setBackgroundColor(ContextCompat.getColor(context, R.color.dark))
        }
        btns2[num].setBackgroundResource(R.drawable.dust_light_round)
    }

}