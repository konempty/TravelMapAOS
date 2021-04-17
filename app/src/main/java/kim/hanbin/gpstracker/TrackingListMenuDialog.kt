package kim.hanbin.gpstracker

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.Button
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class TrackingListMenuDialog(val context: TrackingListActivity, val data: TrackingListData) {
    var cancelable = false
    private val builder: AlertDialog.Builder by lazy {
        AlertDialog.Builder(context).setView(view)
    }

    private val view: View by lazy {
        View.inflate(context, R.layout.dialog_tracking_list_menu, null)
    }

    private var dialog: AlertDialog? = null
    private lateinit var db: EventDao


    fun show() {
        dialog = builder.create()
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.setCancelable(true)
        dialog?.show()
        CoroutineScope(Dispatchers.IO).launch {
            db = InnerDB.getInstance(context)
        }
        view.findViewById<Button>(R.id.delete_btn).setOnClickListener {
            dialog?.dismiss()
            AlertDialog.Builder(context)
                .setMessage("정말로 '${data.name}'을(를) 삭제하시겠습니까?\n삭제후 복구는 불가능합니다.")
                .setPositiveButton("예") { dialogInterface: DialogInterface, i: Int ->
                    CoroutineScope(Dispatchers.IO).launch {
                        db.deleteLogs(data.trackingNum)
                        context.refreshList()
                    }
                }
                .setNegativeButton("아니요") { dialogInterface: DialogInterface, i: Int -> }
                .create().show()

        }
        view.findViewById<Button>(R.id.change_btn).setOnClickListener {
            dialog?.dismiss()
            val dialog = TrackingNameDialog(context)
            dialog.setTitle("여행기록저장")
                .setMsg("변경할 여행기록 이름을 입력해주세요.")
                .setCancelable(true)
                .setDefaultName(data.name, true)
                .setOkListener {
                    var name = dialog.name.text.toString()
                    if (name.isEmpty()) {
                        name = data.name
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        db.updateTrckingData(name, data.id)
                        context.refreshList()
                    }


                }.show()
        }

    }


}