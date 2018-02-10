/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.pageindicators;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Property;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.Workspace;
import com.android.launcher3.colors.ColorManager;

/**
 * {@link PageIndicator} which shows dots per page. The active page is shown with the current
 * accent color.
 */
public class PageIndicatorDots extends PageIndicator implements ColorManager.IWallpaperChange{

    private static final float SHIFT_PER_ANIMATION = 0.5f;
    private static final float SHIFT_THRESHOLD = 0.1f;
    private static final long ANIMATION_DURATION = 0;//liuzuo 0>>150
    //lijun add start
    private static final float GAP_COEFFICIENT_MAX = 2.5f;
    private static final float GAP_COEFFICIENT_MIN = 0.25f;
    private float gapCoefficient = GAP_COEFFICIENT_MAX;
    //lijun add end

    private static final int ENTER_ANIMATION_START_DELAY = 0;//liuzuo 0>>300
    private static final int ENTER_ANIMATION_STAGGERED_DELAY = 0;//liuzuo 0>>150
    private static final int ENTER_ANIMATION_DURATION = 400;

    // This value approximately overshoots to 1.5 times the original size.
    private static final float ENTER_ANIMATION_OVERSHOOT_TENSION = 4.9f;

    private static final RectF sTempRect = new RectF();

    //lijun add for move to default screen start
    private boolean isAnimationgToDefault = false;
    private long currentDuration = ANIMATION_DURATION;
    private int currentIndex,lastIndex;
    private int parentWidth;
    Launcher mLauncher;
    //lijun add end

    private static final Property<PageIndicatorDots, Float> CURRENT_POSITION
            = new Property<PageIndicatorDots, Float>(float.class, "current_position") {
        @Override
        public Float get(PageIndicatorDots obj) {
            return obj.mCurrentPosition;
        }

        @Override
        public void set(PageIndicatorDots obj, Float pos) {
            obj.mCurrentPosition = pos;
            obj.invalidate();
            obj.invalidateOutline();
        }
    };

    /**
     * Listener for keep running the animation until the final state is reached.
     */
    private final AnimatorListenerAdapter mAnimCycleListener = new AnimatorListenerAdapter() {

        @Override
        public void onAnimationEnd(Animator animation) {
            mAnimator = null;
            animateToPostion(mFinalPosition);
        }
    };

    private final Paint mCirclePaint;
    private final float mDotRadius;
    private final float mDotRadiusInActive;
    private int mActiveColor;
    private int mInActiveColor;
    private final boolean mIsRtl;

    private int mActivePage;

    /**
     * The current position of the active dot including the animation progress.
     * For ex:
     *   0.0  => Active dot is at position 0
     *   0.33 => Active dot is at position 0 and is moving towards 1
     *   0.50 => Active dot is at position [0, 1]
     *   0.77 => Active dot has left position 0 and is collapsing towards position 1
     *   1.0  => Active dot is at position 1
     */
    private float mCurrentPosition;
    private float mFinalPosition;
    private ObjectAnimator mAnimator;

    private float[] mEntryAnimationRadiusFactors;

    public PageIndicatorDots(Context context) {
        this(context, null);
    }

    public PageIndicatorDots(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PageIndicatorDots(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCirclePaint.setStyle(Style.FILL);
        mDotRadius = getResources().getDimension(R.dimen.page_indicator_dot_size) / 2;
        mDotRadiusInActive = getResources().getDimension(R.dimen.page_indicator_dot_size_active) / 2;
        setOutlineProvider(new MyOutlineProver());

        //mActiveColor = Utilities.getColorAccent(context);
        //lijun modify for colorchange
//        mActiveColor = getResources().getColor(R.color.page_indicator_dot_color_active);//liuzuo add
//        mInActiveColor = getResources().getColor(R.color.page_indicator_dot_color);
        if (ColorManager.getInstance().isBlackText()) {
            mActiveColor = getResources().getColor(R.color.page_indicator_dot_light_color_active);//liuzuo add
            mInActiveColor = getResources().getColor(R.color.page_indicator_dot_light_color);
        } else {
            mActiveColor = getResources().getColor(R.color.page_indicator_dot_color_active);//liuzuo add
            mInActiveColor = getResources().getColor(R.color.page_indicator_dot_color);
        }

        if(context instanceof Launcher){
            mLauncher = (Launcher) context;
        }else {
            mLauncher = Launcher.getLauncher(context);
        }
        //lijun modify end

        mIsRtl = Utilities.isRtl(getResources());
        setCaretDrawable(new CaretDrawable(context));//lijun add for pageindicator
    }

    @Override
    public void setScroll(int currentScroll, int totalScroll) {
        if (mNumPages > 1) {
            //lijun modify start
            /*if (mIsRtl) {
                currentScroll = totalScroll - currentScroll;
            }
            int scrollPerPage = totalScroll / (mNumPages - 1);
            int absScroll = mActivePage * scrollPerPage;
            float scrollThreshold = SHIFT_THRESHOLD * scrollPerPage;

            if ((absScroll - currentScroll) > scrollThreshold) {
                // current scroll is before absolute scroll
                animateToPostion(mActivePage - SHIFT_PER_ANIMATION);
            } else if ((currentScroll - absScroll) > scrollThreshold) {
                // current scroll is ahead of absolute scroll
                animateToPostion(mActivePage + SHIFT_PER_ANIMATION);
            } else {
                animateToPostion(mActivePage);
            }*/
            animateToPostion(mActivePage);
            //lijun modify end
        }
    }

    private void animateToPostion(float position) {
        mFinalPosition = position;
        if (Math.abs(mCurrentPosition - mFinalPosition) < SHIFT_THRESHOLD) {
            mCurrentPosition = mFinalPosition<0?0:mFinalPosition;
            isAnimationgToDefault = false;//lijun add for move to default screen
        }
        if (mAnimator == null && Float.compare(mCurrentPosition, mFinalPosition) != 0) {
            float positionForThisAnim = mCurrentPosition > mFinalPosition ?
                    mCurrentPosition - SHIFT_PER_ANIMATION : mCurrentPosition + SHIFT_PER_ANIMATION;
            mAnimator = ObjectAnimator.ofFloat(this, CURRENT_POSITION, positionForThisAnim);
            mAnimator.addListener(mAnimCycleListener);

            mAnimator.setDuration(isAnimationgToDefault?currentDuration:ANIMATION_DURATION);//lijun add isAnimationgToDefault?currentDuration:
            mAnimator.start();
        }
    }

    public void animateToDefaultPostion(float position) {
        if (position != mActivePage && Math.abs(mActivePage - position) > 1) {
            currentDuration = (long) (ANIMATION_DURATION/(Math.abs(mActivePage-position)));
            isAnimationgToDefault = true;//lijun add for move to default screen
        }
    }

    public void stopAllAnimations() {
        if (mAnimator != null) {
            mAnimator.removeAllListeners();
            mAnimator.cancel();
            mAnimator = null;
        }
        mFinalPosition = mActivePage;
        CURRENT_POSITION.set(this, mFinalPosition);
    }

    /**
     * Sets up up the page indicator to play the entry animation.
     * {@link #playEntryAnimation()} must be called after this.
     */
    public void prepareEntryAnimation() {
        mEntryAnimationRadiusFactors = new float[mNumPages];
        invalidate();
    }

    public void playEntryAnimation() {
        int count  = mEntryAnimationRadiusFactors.length;
        if (count == 0) {
            mEntryAnimationRadiusFactors = null;
            invalidate();
            return;
        }

        Interpolator interpolator = new OvershootInterpolator(ENTER_ANIMATION_OVERSHOOT_TENSION);
        AnimatorSet animSet = new AnimatorSet();
        for (int i = 0; i < count; i++) {
            ValueAnimator anim = ValueAnimator.ofFloat(0, 1).setDuration(ENTER_ANIMATION_DURATION);
            final int index = i;
            anim.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mEntryAnimationRadiusFactors[index] = (Float) animation.getAnimatedValue();
                    invalidate();
                }
            });
            anim.setInterpolator(interpolator);
            anim.setStartDelay(ENTER_ANIMATION_START_DELAY + ENTER_ANIMATION_STAGGERED_DELAY * i);
            animSet.play(anim);
        }

        animSet.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animation) {
                mEntryAnimationRadiusFactors = null;
                invalidateOutline();
                invalidate();
            }
        });
        animSet.start();
    }

    @Override
    public void setActiveMarker(int activePage) {
        if (mActivePage != activePage) {
            mActivePage = activePage;
            if(mAnimator!=null){
                mAnimator.cancel();
                mAnimator = null;
            }
            animateToPostion(mActivePage);
        }
    }

    @Override
    protected void onPageCountChanged() {
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Add extra spacing of mDotRadius on all sides so than entry animation could be run.
        int preWidth = (int) (mNumPages * (mDotRadius*(2 +GAP_COEFFICIENT_MAX)));//0II00II00...00II0
        parentWidth = Launcher.getLauncher(getContext()).getDeviceProfile().widthPx;
        if(R.id.folder_page_indicator == getId()){
            parentWidth = Launcher.getLauncher(getContext()).getDeviceProfile().folderCellWidthPx;
        }
        if(preWidth > parentWidth){
            gapCoefficient = (parentWidth - mNumPages*mDotRadius*2)/(float)mNumPages/mDotRadius;
            if(gapCoefficient < GAP_COEFFICIENT_MIN){
                gapCoefficient = GAP_COEFFICIENT_MIN;
                preWidth = (int) ((gapCoefficient+2)*mDotRadius)*mNumPages;
            }else {
                preWidth = parentWidth;
            }
        }else {
            gapCoefficient = GAP_COEFFICIENT_MAX;
        }

//        int width = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY ?
//                MeasureSpec.getSize(widthMeasureSpec) : preWidth;
        int width = preWidth;
        int height= MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY ?
                MeasureSpec.getSize(heightMeasureSpec) : (int) (4 * mDotRadius);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Draw all page indicators;
        float circleGap = mDotRadius*(gapCoefficient+2);
        float startX = mDotRadius*gapCoefficient / 2;


        float x = startX + mDotRadius;
        float y = canvas.getHeight() / 2;
        if (mEntryAnimationRadiusFactors != null) {
            // During entry animation, only draw the circles
            if (mIsRtl) {
                x = getWidth() - x;
                circleGap = -circleGap;
            }
            for (int i = 0; i < mEntryAnimationRadiusFactors.length; i++) {
                mCirclePaint.setColor(i == mActivePage ? mActiveColor : mInActiveColor);
                float radius = i ==  mActivePage ? mDotRadiusInActive : mDotRadius;
                canvas.drawCircle(x, y, radius * mEntryAnimationRadiusFactors[i], mCirclePaint);
                x += circleGap;
            }
        } else {
            mCirclePaint.setColor(mInActiveColor);
            for (int i = 0; i < mNumPages; i++) {
                canvas.drawCircle(x, y, mDotRadiusInActive, mCirclePaint);
                x += circleGap;
            }

            mCirclePaint.setColor(mActiveColor);
            canvas.drawRoundRect(getActiveRect(), mDotRadius, mDotRadius, mCirclePaint);
        }
    }

    private RectF getActiveRect() {
        //lijun modify start
        float startCircle = (int) mCurrentPosition;
        //float startCircle = mCurrentPosition >= 0 ? (int) mCurrentPosition : (int) mCurrentPosition ;
        //lijun modify end
        float delta = mCurrentPosition - startCircle;
        float diameter = 2 * mDotRadius;
        float circleGap = mDotRadius*(gapCoefficient+2);
        float startX = mDotRadius*gapCoefficient / 2;

        sTempRect.top = getHeight() * 0.5f - mDotRadius;
        sTempRect.bottom = getHeight() * 0.5f + mDotRadius;
        sTempRect.left = startX + startCircle * circleGap;
        sTempRect.right = sTempRect.left + diameter;
        /*
        liuzuo remove dot animation begin
         */
/*        if (delta < SHIFT_PER_ANIMATION) {
            // dot is capturing the right circle.
            sTempRect.right += delta * circleGap * 2;
        } else {
            // Dot is leaving the left circle.
            //sTempRect.right += circleGap;

            //delta -= SHIFT_PER_ANIMATION;
            sTempRect.left += delta * circleGap * 2;
        }*/
         /*
        liuzuo remove dot animation end
         */
        if (mIsRtl) {
            float rectWidth = sTempRect.width();
            sTempRect.right = getWidth() - sTempRect.left;
            sTempRect.left = sTempRect.right - rectWidth;
        }
        return sTempRect;
    }

    private class MyOutlineProver extends ViewOutlineProvider {

        @Override
        public void getOutline(View view, Outline outline) {
            if (mEntryAnimationRadiusFactors == null) {
                RectF activeRect = getActiveRect();
                outline.setRoundRect(
                        (int) activeRect.left,
                        (int) activeRect.top,
                        (int) activeRect.right,
                        (int) activeRect.bottom,
                        mDotRadius
                );
            }
        }
    }

    @Override
    public void removeMarker() {
        super.removeMarker();
        if (mActivePage > (mNumPages - 1)) {
            mActivePage = mNumPages - 1;
            mCurrentPosition = mActivePage;
        } else if (mActivePage < 0) {
            mActivePage = 0;
            mCurrentPosition = 0;
        }
    }

    @Override
    public void onWallpaperChange() {

    }

    @Override
    public void onColorChange(int[] colors) {
        if (ColorManager.getInstance().isBlackText()) {
            mActiveColor = getResources().getColor(R.color.page_indicator_dot_light_color_active);//liuzuo add
            mInActiveColor = getResources().getColor(R.color.page_indicator_dot_light_color);
        } else {
            mActiveColor = getResources().getColor(R.color.page_indicator_dot_color_active);//liuzuo add
            mInActiveColor = getResources().getColor(R.color.page_indicator_dot_color);
        }
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (ColorManager.getInstance().isBlackText()) {
            mActiveColor = getResources().getColor(R.color.page_indicator_dot_light_color_active);//liuzuo add
            mInActiveColor = getResources().getColor(R.color.page_indicator_dot_light_color);
        } else {
            mActiveColor = getResources().getColor(R.color.page_indicator_dot_color_active);//liuzuo add
            mInActiveColor = getResources().getColor(R.color.page_indicator_dot_color);
        }
        invalidate();
        ColorManager.getInstance().addWallpaperCallback(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ColorManager.getInstance().removeWallpaperCallback(this);
    }

    //lijun add
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mNumPages <= 0 || mPagedView == null || !isVisible()) return false;
        float x ;
        int width;
        if (getMeasuredWidth() > parentWidth) {
            x = event.getRawX();
            width = parentWidth;
        } else {
            x = (int) event.getX();
            width = getMeasuredWidth();
        }
        x = x < 0 ? 0 : x;
        x = x >= width ? (width - 1) : x;

        float perX = width / mNumPages;
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            lastIndex = currentIndex = (int) (x / perX);
            if (mIsRtl) {
                lastIndex = currentIndex = mNumPages - (int) (x / perX) -1;
            }
            mPagedView.snapToPage(currentIndex);
            if (mPagedView instanceof Workspace && mPagedView.getCurrentPage() != currentIndex && mLauncher != null) {
                mLauncher.showPageIndicatorDiagital(currentIndex);
            }
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            currentIndex = (int) (x / perX);
            if (mIsRtl) {
                currentIndex = mNumPages - (int) (x / perX) -1;
            }
            if (lastIndex != currentIndex) {
                mPagedView.snapToPage(currentIndex);
                if (mPagedView instanceof Workspace && mLauncher != null) {
                    mLauncher.showPageIndicatorDiagital(currentIndex);
                }
                lastIndex = currentIndex;
            }
        } else {
            if (mPagedView instanceof Workspace && mLauncher != null) {
                mLauncher.hidePageIndicatorDiagital();
            }
        }
        return true;
    }

    private boolean isVisible() {
        return this.getVisibility() == View.VISIBLE && this.getAlpha() > 0.01f;
    }
}
