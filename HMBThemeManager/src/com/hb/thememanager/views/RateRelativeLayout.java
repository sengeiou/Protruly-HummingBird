package com.hb.thememanager.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hb.thememanager.R;

import java.util.List;



/**
 * 按比例设置大小的RelativeLayout
 *
 */
public class RateRelativeLayout extends RelativeLayout {
    private static final String TAG = "RateRelativeLayout";
    public static final int MODE_WIDTH = 0;
    public static final int MODE_HEIGHT = 1;
    private float mRate;
    private int mMode;

    public RateRelativeLayout(Context context) {
        super(context);
        mRate = 1f;
        mMode = 1;
    }

    public RateRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RateLayout);
        mRate = a.getFloat(R.styleable.RateLayout_rate,1f);
        mMode = a.getInt(R.styleable.RateLayout_mode,1);
        a.recycle();
    }

    public void setRate(float rate){
        mRate = rate;
    }

    public void setMode(int mode){
        mMode = mode;
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        int finalMeasureWidth = widthMeasureSpec;
        int finalMeasureHeight = heightMeasureSpec;

        switch (mMode){
            case MODE_WIDTH:
                finalMeasureWidth = MeasureSpec.makeMeasureSpec((int)(height * mRate), MeasureSpec.EXACTLY);
                break;
            case MODE_HEIGHT:
                finalMeasureHeight = MeasureSpec.makeMeasureSpec((int)(width * mRate), MeasureSpec.EXACTLY);
                break;
        }

        super.onMeasure(finalMeasureWidth, finalMeasureHeight);
    }
}
