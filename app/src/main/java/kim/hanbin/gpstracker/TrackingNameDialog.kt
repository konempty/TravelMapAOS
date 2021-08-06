package kim.hanbin.gpstracker

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

class TrackingNameDialog(private val context: Context) {
    var cancelable = false
    private val builder: AlertDialog.Builder by lazy {
        AlertDialog.Builder(context).setView(view)
    }

    private val view: View by lazy {
        View.inflate(context, R.layout.dialog_tracking_name, null)
    }
    val name: EditText by lazy {
        view.findViewById(R.id.name)
    }

    private var dialog: AlertDialog? = null

    private var cancelListener: DialogInterface.OnCancelListener? = null

    private var okClickListener: View.OnClickListener? = null

    fun show() {
        dialog = builder.create()
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.setCancelable(cancelable)
        view.findViewById<Button>(R.id.okBtn).setOnClickListener {
            dismiss()
            okClickListener?.onClick(it)
        }
        name.addTextChangedListener(object : TextWatcher {


            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(charSequence: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (charSequence!!.length > 25) {
                    Toast.makeText(context,"25자이상 입력이 불가능 합니다.",Toast.LENGTH_SHORT).show()
                    name.setText(name.text.toString().substring(0, 25));
                    name.setSelection(name.length());
                }
            }

            override fun afterTextChanged(p0: Editable?) {
            }
        });
        dialog?.setOnCancelListener(cancelListener)
        dialog?.show()
    }

    fun setTitle(str: String): TrackingNameDialog {
        val title = view.findViewById<TextView>(R.id.title)
        title.visibility = View.VISIBLE
        title.text = str
        return this
    }

    fun setDefaultName(str: String, isChange: Boolean): TrackingNameDialog {

        if (isChange) {
            name.setText(str)
        }
        name.hint = str
        return this
    }

    fun setOkListener(listener: View.OnClickListener): TrackingNameDialog {
        okClickListener = listener
        return this
    }

    fun setMsg(str: String): TrackingNameDialog {
        val msg = view.findViewById<TextView>(R.id.msg)
        msg.visibility = View.VISIBLE
        msg.text = str
        msg.gravity = Gravity.CENTER
        return this
    }

    fun setCancelListener(listener: DialogInterface.OnCancelListener): TrackingNameDialog {
        this.cancelListener = listener
        return this
    }

    fun setCancelable(b: Boolean): TrackingNameDialog {
        cancelable = b
        return this
    }

    fun dismiss() {
        dialog?.dismiss()
    }

}