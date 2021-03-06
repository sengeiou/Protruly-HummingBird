/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.keyguard;

// add by wxue 20170426 start
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
// add by wxue 20170426 end
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.mediatek.keyguard.AntiTheft.AntiTheftManager;

import java.lang.ref.WeakReference;

/***
 * Manages a number of views inside of the given layout. See below for a list of widgets.
 */
public class KeyguardMessageArea extends TextView implements SecurityMessageDisplay {
    /** Handler token posted with accessibility announcement runnables. */
    private static final Object ANNOUNCE_TOKEN = new Object();

    /**
     * Delay before speaking an accessibility announcement. Used to prevent
     * lift-to-type from interrupting itself.
     */
    private static final long ANNOUNCEMENT_DELAY = 250;

    public static final int SECURITY_MESSAGE_DURATION = 5000;

    private final KeyguardUpdateMonitor mUpdateMonitor;
    private final Handler mHandler;

    // Timeout before we reset the message to show charging/owner info
    long mTimeout = SECURITY_MESSAGE_DURATION;
    CharSequence mMessage;

    private CharSequence mSeparator;
    private KeyguardSecurityModel mSecurityModel;
    // add by wxue 20170428 start
    private ObjectAnimator mShakeAnimator;
    // add by wxue 20170428 end

    private final Runnable mClearMessageRunnable = new Runnable() {
        @Override
        public void run() {
            mMessage = null;
            update();
        }
    };

    private KeyguardUpdateMonitorCallback mInfoCallback = new KeyguardUpdateMonitorCallback() {
        public void onFinishedGoingToSleep(int why) {
            setSelected(false);
        };
        public void onStartedWakingUp() {
            setSelected(true);
        };
    };

    public KeyguardMessageArea(Context context) {
        this(context, null);
    }

    public KeyguardMessageArea(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayerType(LAYER_TYPE_HARDWARE, null); // work around nested unclipped SaveLayer bug

        mSecurityModel = new KeyguardSecurityModel(context) ;
        mUpdateMonitor = KeyguardUpdateMonitor.getInstance(getContext());
        mUpdateMonitor.registerCallback(mInfoCallback);
        mHandler = new Handler(Looper.myLooper());

        mSeparator = getResources().getString(
                com.android.internal.R.string.kg_text_message_separator);
        
        update();
    }

    @Override
    public void setMessage(CharSequence msg, boolean important) {
        if (!TextUtils.isEmpty(msg) && important) {
            securityMessageChanged(msg);
        } else {
            clearMessage();
        }
    }

    @Override
    public void setMessage(int resId, boolean important) {
        if (resId != 0 && important) {
            CharSequence message = getContext().getResources().getText(resId);
            securityMessageChanged(message);
        } else {
            clearMessage();
        }
    }

    @Override
    public void setMessage(int resId, boolean important, Object... formatArgs) {
        if (resId != 0 && important) {
            String message = getContext().getString(resId, formatArgs);
            securityMessageChanged(message);
        } else {
            clearMessage();
        }
    }

    @Override
    public void setTimeout(int timeoutMs) {
        mTimeout = timeoutMs;
    }

    public static SecurityMessageDisplay findSecurityMessageDisplay(View v) {
        KeyguardMessageArea messageArea = (KeyguardMessageArea) v.findViewById(
                R.id.keyguard_message_area);
        if (messageArea == null) {
            throw new RuntimeException("Can't find keyguard_message_area in " + v.getClass());
        }
        return messageArea;
    }
    
    // add by wxue 20170323 start
    public static SecurityMessageDisplay findUnlockFailedMessageDisplay(View v) {
        KeyguardMessageArea messageArea = (KeyguardMessageArea) v.findViewById(
                R.id.unlock_failed_text);
        if (messageArea == null) {
            throw new RuntimeException("Can't find keyguard_message_area in " + v.getClass());
        }
        return messageArea;
    }
    // add by wxue 20170323 end
    
    @Override
    protected void onFinishInflate() {
        boolean shouldMarquee = KeyguardUpdateMonitor.getInstance(mContext).isDeviceInteractive();
        setSelected(shouldMarquee); // This is required to ensure marquee works
    }

    private void securityMessageChanged(CharSequence message) {
        mMessage = message;
        update();
        mHandler.removeCallbacks(mClearMessageRunnable);
        if (mTimeout > 0) {
            mHandler.postDelayed(mClearMessageRunnable, mTimeout);
        }
        mHandler.removeCallbacksAndMessages(ANNOUNCE_TOKEN);
        mHandler.postAtTime(new AnnounceRunnable(this, getText()), ANNOUNCE_TOKEN,
                (SystemClock.uptimeMillis() + ANNOUNCEMENT_DELAY));
    }

    private void clearMessage() {
        mHandler.removeCallbacks(mClearMessageRunnable);
        mHandler.post(mClearMessageRunnable);
    }

    private void update() {
        CharSequence status = mMessage;
        setVisibility(TextUtils.isEmpty(status) ? INVISIBLE : VISIBLE);
        //setText(status);

        /// M: fix ALPS01897062
        /// M: If dm lock or PPL Lock is on, we should tell user here @{
        if (mSecurityModel.getSecurityMode() == KeyguardSecurityModel.SecurityMode.AntiTheft) {
            setText(AntiTheftManager.getAntiTheftMessageAreaText(status, mSeparator));
        } else {
            setText(status);
        }
        /// @}
    }


    /**
     * Runnable used to delay accessibility announcements.
     */
    private static class AnnounceRunnable implements Runnable {
        private final WeakReference<View> mHost;
        private final CharSequence mTextToAnnounce;

        AnnounceRunnable(View host, CharSequence textToAnnounce) {
            mHost = new WeakReference<View>(host);
            mTextToAnnounce = textToAnnounce;
        }

        @Override
        public void run() {
            final View host = mHost.get();
            if (host != null) {
                host.announceForAccessibility(mTextToAnnounce);
            }
        }
    }
    
    // add by wxue 20170426 start
    private void startShakeAnimation(){
    	if(mShakeAnimator == null){
    		mShakeAnimator = getShakeAnimation(this);
    		mShakeAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                	mShakeAnimator = null;
                }
            });
    		mShakeAnimator.start();
    	}
    }
    
    private void cancelShakeAiamtion(){
    	if(mShakeAnimator != null){
    		mShakeAnimator.cancel();
    	}
    }
    
    @Override
    public void setFingerprintAuthFailedMessage(String msg, boolean animate){
    	if(animate){
    		setMessage(msg, true);
    		startShakeAnimation();
    	}else{
    		cancelShakeAiamtion();
    		setMessage(msg, true);
    	}
    }
    
    private ObjectAnimator getShakeAnimation(View view) {
        int delta = 50;
        PropertyValuesHolder pvhTranslateX = PropertyValuesHolder.ofKeyframe(View.TRANSLATION_X,
                Keyframe.ofFloat(0f, 0),
                Keyframe.ofFloat(.10f, -delta),
                /*Keyframe.ofFloat(.26f, delta),
                Keyframe.ofFloat(.42f, -delta),
                Keyframe.ofFloat(.58f, delta),
                Keyframe.ofFloat(.74f, -delta),*/
                Keyframe.ofFloat(.90f, delta),
                Keyframe.ofFloat(1f, 0f)
        );
        return ObjectAnimator.ofPropertyValuesHolder(view, pvhTranslateX).setDuration(300);
    }
    // add by wxue 20170426 end
}
