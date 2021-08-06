package kim.hanbin.gpstracker

import android.content.Context
import android.os.Bundle
import android.widget.ProgressBar

open class ProgressPopup(context: Context?) : BasePopup(context) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.popup_progress)
        findViewById<ProgressBar>(R.id.progressBar1).animate()
        setCancelable(false)
    }
}