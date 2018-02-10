/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.mediatek.wifi.hotspot;

import android.content.Context;
import hb.preference.SwitchPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import hb.widget.Switch;


public class HotspotSwitchPreference extends SwitchPreference {
    private static final String TAG = "HotspotSwitchPreference";

    public HotspotSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public HotspotSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HotspotSwitchPreference(Context context) {
        super(context);
    }

    @Override
    protected void onClick() {
        Log.d("@M_" + TAG, "onClick()");
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);
        Switch v = (Switch) view.findViewById(com.android.internal.R.id.switchWidget);
        if (v != null) {
            v.setClickable(true);
        }
        return view;
    }
}
