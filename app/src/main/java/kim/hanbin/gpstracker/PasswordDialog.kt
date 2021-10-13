package kim.hanbin.gpstracker

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.InputFilter
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import java.util.regex.Matcher
import java.util.regex.Pattern

class PasswordDialog(private val context: Context) {
    private val builder: AlertDialog.Builder by lazy {
        AlertDialog.Builder(context).setView(view)
    }

    private val view: View by lazy {
        View.inflate(context, R.layout.dialog_password, null)
    }

    private var dialog: AlertDialog? = null


    private lateinit var okClickListener: View.OnClickListener

    val passwordET: EditText by lazy {
        view.findViewById(R.id.password)
    }
    var password = ""


    fun show() {
        dialog = builder.create()
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.setCancelable(false)

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
        passwordET.filters = arrayOf(filter)
        passwordET.transformationMethod = TMPasswordTransformationMethod()
        view.findViewById<Button>(R.id.okBtn).setOnClickListener {
            password = passwordET.text.toString()
            dismiss()
            okClickListener.onClick(it)
        }
        dialog?.show()
    }


    fun setOkListener(listener: View.OnClickListener): PasswordDialog {
        okClickListener = listener
        return this
    }


    fun dismiss() {
        dialog?.dismiss()
    }

}