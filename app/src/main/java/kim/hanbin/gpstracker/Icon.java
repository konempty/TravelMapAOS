package kim.hanbin.gpstracker;

import android.content.Context;
import android.util.AttributeSet;

public class Icon extends androidx.appcompat.widget.AppCompatImageView {

    public Icon(final Context context) {
        super(context);
    }

    public Icon(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public Icon(final Context context, final AttributeSet attrs,
                final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int width, int height) {
        super.onMeasure(width, height);
        int measuredWidth = getMeasuredWidth();
        setMeasuredDimension(measuredWidth, measuredWidth);


    }

}
