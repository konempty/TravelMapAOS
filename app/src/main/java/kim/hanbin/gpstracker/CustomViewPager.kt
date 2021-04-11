package kim.hanbin.gpstracker

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

class CustomViewPager(context: Context, attrs: AttributeSet?) :
    ViewPager(context, attrs) {
    private var penabled = false


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return if (this.penabled) {
            super.onTouchEvent(event)
        } else false
    }

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        penabled = TouchImageView.isPager
        return if (this.penabled) {
            super.onInterceptTouchEvent(event)
        } else false
    }

    fun setPagingEnabled(enabled: Boolean) {
        this.penabled = enabled
    }
}