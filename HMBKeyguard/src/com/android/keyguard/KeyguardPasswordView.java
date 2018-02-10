/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.content.Context;
import android.graphics.Rect;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.TextKeyListener;
import android.util.AttributeSet;
import android.util.Log ;
import android.view.KeyEvent;
import android.view.View;
// add by wxue 20170323 start
import android.view.ViewGroup;
// add by wxue 20170323 end
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.android.internal.widget.TextViewInputDisabler;

import java.util.List;
/**
 * Displays an alphanumeric (latin-1) key entry for the user to enter
 * an unlock password
 */

public class KeyguardPasswordView extends KeyguardAbsKeyInputView
        implements KeyguardSecurityView, OnEditorActionListener, TextWatcher {

    private static final String TAG = "KeyguardPasswordView";
    private static final boolean DEBUG = true ;

    private final boolean mShowImeAtScreenOn;
    private final int mDisappearYTranslation;

    InputMethodManager mImm;
    private TextView mPasswordEntry;
    private TextViewInputDisabler mPasswordEntryDisabler;

    private Interpolator mLinearOutSlowInInterpolator;
    private Interpolator mFastOutLinearInInterpolator;
    // add by wxue 20170323 start
    private ViewGroup mContainer;
    private View mUnlockFailedContainer;
    private SecurityMessageDisplay mUnlockFailedMessageDisplay;
    private boolean mSoftInputHide;
    // add by wxue 20170323 end

    public KeyguardPasswordView(Context context) {
        this(context, null);
    }

    public KeyguardPasswordView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mShowImeAtScreenOn = context.getResources().
                getBoolean(R.bool.kg_show_ime_at_screen_on);
        mDisappearYTranslation = getResources().getDimensionPixelSize(
                R.dimen.disappear_y_translation);
        mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(
                context, android.R.interpolator.linear_out_slow_in);
        mFastOutLinearInInterpolator = AnimationUtils.loadInterpolator(
                context, android.R.interpolator.fast_out_linear_in);
    }

    protected void resetState() {
        /// M: [ALPS00594552] Indicate the user to input password.
    	// add by wxue 20170322 start
        if(getContainerView() != null){
        	getContainerView().setVisibility(View.VISIBLE);
        }
        if(getUnlockFailedMsgDisplay() != null){
        	getUnlockFailedMsgDisplay().setMessage("", false);
        }
        if(getUnlockFailedContainer() != null){
        	getUnlockFailedContainer().setVisibility(View.GONE);
        }
        // add by wxue 20170322 end
        mSecurityMessageDisplay.setMessage(R.string.kg_password_instructions, true);
        final boolean wasDisabled = mPasswordEntry.isEnabled();
        setPasswordEntryEnabled(true);
        setPasswordEntryInputEnabled(true);
        if (wasDisabled) {
            mImm.showSoftInput(mPasswordEntry, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    @Override
    protected int getPasswordTextViewId() {
        return R.id.passwordEntry;
    }
    
    // add by wxue 20170322 start
    @Override
    protected View getContainerView() {
    	// TODO Auto-generated method stub
    	return mContainer;
    }
    // add by wxue 20170322 end
    
    @Override
    public boolean needsInput() {
        Log.d(TAG, "needsInput() - returns true.");
        return true;
    }

    @Override
    public void onResume(final int reason) {
        super.onResume(reason);

        // Wait a bit to focus the field so the focusable flag on the window is already set then.
        post(new Runnable() {
            @Override
            public void run() {
                if (isShown() && mPasswordEntry.isEnabled()) {
                    mPasswordEntry.requestFocus();
                    Log.d(TAG, "reason = " + reason +
                        ", mShowImeAtScreenOn = " + mShowImeAtScreenOn);
                    if (reason != KeyguardSecurityView.SCREEN_ON || mShowImeAtScreenOn) {
                        Log.d(TAG, "onResume() - call showSoftInput()");
                        mImm.showSoftInput(mPasswordEntry, InputMethodManager.SHOW_IMPLICIT);
                    }
                }
            }
        });
    }

    @Override
    protected int getPromtReasonStringRes(int reason) {
        switch (reason) {
            case PROMPT_REASON_RESTART:
                return R.string.kg_prompt_reason_restart_password;
            default:
                return 0;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mImm.hideSoftInputFromWindow(getWindowToken(), 0);
    }

    @Override
    public void reset() {
        super.reset();
        mPasswordEntry.requestFocus();
    }
    
    // add by wxue 20170323 start
    @Override
    protected SecurityMessageDisplay getUnlockFailedMsgDisplay() {
    	// TODO Auto-generated method stub
    	return mUnlockFailedMessageDisplay;
    }
    
    @Override
    protected View getUnlockFailedContainer() {
    	// TODO Auto-generated method stub
    	return mUnlockFailedContainer;
    }
    // add by wxue 20170323 end

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        boolean imeOrDeleteButtonVisible = false;

        mImm = (InputMethodManager) getContext().getSystemService(
                Context.INPUT_METHOD_SERVICE);

        // add by wxue 20170323 start
        mContainer = (ViewGroup)findViewById(R.id.container);
        mUnlockFailedContainer = findViewById(R.id.unlock_failed_container);
        mUnlockFailedMessageDisplay = KeyguardMessageArea.findUnlockFailedMessageDisplay(this);
        // add by wxue 20170323 end
        mPasswordEntry = (TextView) findViewById(getPasswordTextViewId());
        mPasswordEntryDisabler = new TextViewInputDisabler(mPasswordEntry);
        mPasswordEntry.setKeyListener(TextKeyListener.getInstance());
        mPasswordEntry.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        mPasswordEntry.setOnEditorActionListener(this);
        mPasswordEntry.addTextChangedListener(this);

        // Poke the wakelock any time the text is selected or modified
        mPasswordEntry.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mCallback.userActivity();
            }
        });

        // Set selected property on so the view can send accessibility events.
        mPasswordEntry.setSelected(true);

        mPasswordEntry.requestFocus();

        // If there's more than one IME, enable the IME switcher button
        View switchImeButton = findViewById(R.id.switch_ime_button);
        if (switchImeButton != null && hasMultipleEnabledIMEsOrSubtypes(mImm, false)) {
            switchImeButton.setVisibility(View.VISIBLE);
            imeOrDeleteButtonVisible = true;
            switchImeButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    mCallback.userActivity(); // Leave the screen on a bit longer
                    // Do not show auxiliary subtypes in password lock screen.
                    mImm.showInputMethodPicker(false /* showAuxiliarySubtypes */);
                }
            });
        }

        // If no icon is visible, reset the start margin on the password field so the text is
        // still centered.
        if (!imeOrDeleteButtonVisible) {
            android.view.ViewGroup.LayoutParams params = mPasswordEntry.getLayoutParams();
            if (params instanceof MarginLayoutParams) {
                final MarginLayoutParams mlp = (MarginLayoutParams) params;
                mlp.setMarginStart(0);
                mPasswordEntry.setLayoutParams(params);
            }
        }
        
        // add by wxue 20170519 start
        mEcaView.addOnLayoutChangeListener(new OnLayoutChangeListener() {
			@Override
			public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight,
					int oldBottom) {
				/*Log.i(TAG, "onLayoutChange()--" + " old=" + new Rect(oldLeft, oldTop, oldRight, oldBottom).toShortString()
                        + " new=" + new Rect(left,top,right,bottom).toShortString());*/
				boolean changed = oldTop != top || oldBottom != bottom;
				if(changed){
					if(top > oldTop){
						mSoftInputHide = true;
					}else{
						mSoftInputHide = false;
					}
				}
			}
		});
        // add by wuxe 20170519 end
    }
    
    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        // send focus to the password field
        return mPasswordEntry.requestFocus(direction, previouslyFocusedRect);
    }

    @Override
    protected void resetPasswordText(boolean animate) {
        mPasswordEntry.setText("");
    }

    @Override
    protected String getPasswordText() {
        return mPasswordEntry.getText().toString();
    }

    @Override
    protected void setPasswordEntryEnabled(boolean enabled) {
        mPasswordEntry.setEnabled(enabled);
    }

    @Override
    protected void setPasswordEntryInputEnabled(boolean enabled) {
        mPasswordEntryDisabler.setInputEnabled(enabled);
    }

    /**
     * Method adapted from com.android.inputmethod.latin.Utils
     *
     * @param imm The input method manager
     * @param shouldIncludeAuxiliarySubtypes
     * @return true if we have multiple IMEs to choose from
     */
    private boolean hasMultipleEnabledIMEsOrSubtypes(InputMethodManager imm,
            final boolean shouldIncludeAuxiliarySubtypes) {
        final List<InputMethodInfo> enabledImis = imm.getEnabledInputMethodList();

        // Number of the filtered IMEs
        int filteredImisCount = 0;

        for (InputMethodInfo imi : enabledImis) {
            // We can return true immediately after we find two or more filtered IMEs.
            if (filteredImisCount > 1) return true;
            final List<InputMethodSubtype> subtypes =
                    imm.getEnabledInputMethodSubtypeList(imi, true);
            // IMEs that have no subtypes should be counted.
            if (subtypes.isEmpty()) {
                ++filteredImisCount;
                continue;
            }

            int auxCount = 0;
            for (InputMethodSubtype subtype : subtypes) {
                if (subtype.isAuxiliary()) {
                    ++auxCount;
                }
            }
            final int nonAuxCount = subtypes.size() - auxCount;

            // IMEs that have one or more non-auxiliary subtypes should be counted.
            // If shouldIncludeAuxiliarySubtypes is true, IMEs that have two or more auxiliary
            // subtypes should be counted as well.
            if (nonAuxCount > 0 || (shouldIncludeAuxiliarySubtypes && auxCount > 1)) {
                ++filteredImisCount;
                continue;
            }
        }

        return filteredImisCount > 1
        // imm.getEnabledInputMethodSubtypeList(null, false) will return the current IME's enabled
        // input method subtype (The current IME should be LatinIME.)
                || imm.getEnabledInputMethodSubtypeList(null, false).size() > 1;
    }

    @Override
    public void showUsabilityHint() {
    }

    @Override
    public int getWrongPasswordStringId() {
        return R.string.kg_wrong_password;
    }

    @Override
    public void startAppearAnimation() {
        setAlpha(0f);
        setTranslationY(0f);
        animate()
                .alpha(1)
                .withLayer()
                .setDuration(300)
                .setInterpolator(mLinearOutSlowInInterpolator);
    }

    @Override
    public boolean startDisappearAnimation(Runnable finishRunnable) {
        animate()
                .alpha(0f)
                .translationY(mDisappearYTranslation)
                .setInterpolator(mFastOutLinearInInterpolator)
                .setDuration(100)
                .withEndAction(finishRunnable);
        return true;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        if (mCallback != null) {
            mCallback.userActivity();
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        // Poor man's user edit detection, assuming empty text is programmatic and everything else
        // is from the user.
        if (!TextUtils.isEmpty(s)) {
            onUserInput();
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        // Check if this was the result of hitting the enter key
        final boolean isSoftImeEvent = event == null
                && (actionId == EditorInfo.IME_NULL
                || actionId == EditorInfo.IME_ACTION_DONE
                || actionId == EditorInfo.IME_ACTION_NEXT);
        final boolean isKeyboardEnterKey = event != null
                && KeyEvent.isConfirmKey(event.getKeyCode())
                && event.getAction() == KeyEvent.ACTION_DOWN;
        if (isSoftImeEvent || isKeyboardEnterKey) {
            verifyPasswordAndUnlock();
            return true;
        }
        return false;
    }
    
    // add by wxue 20170519 start
    public boolean onBackPressed(){
    	//Log.i(TAG,"---onBackPressed()--mImm.isActive() = " + mImm.isActive() + " mSoftInputHide = " + mSoftInputHide);
    	if(mImm.isActive() && !mSoftInputHide){
    		return true;
    	}
    	return false;
    }
    // add by wxue 20170519 end
}
