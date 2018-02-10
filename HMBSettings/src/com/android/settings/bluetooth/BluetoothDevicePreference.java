/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings.bluetooth;

import static android.os.UserManager.DISALLOW_CONFIG_BLUETOOTH;

import hb.app.dialog.AlertDialog;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.UserManager;
import hb.preference.Preference;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.settings.R;
import com.android.settings.search.Index;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.HidProfile;
import com.android.settingslib.bluetooth.LocalBluetoothProfile;

import java.util.List;

/**
 * BluetoothDevicePreference is the preference type used to display each remote
 * Bluetooth device in the Bluetooth Settings screen.
 */
public final class BluetoothDevicePreference extends Preference implements
        CachedBluetoothDevice.Callback, OnClickListener {
    private static final String TAG = "BluetoothDevicePreference";

    private static int sDimAlpha = Integer.MIN_VALUE;

    private final CachedBluetoothDevice mCachedDevice;

    private OnClickListener mOnSettingsClickListener;

    private AlertDialog mDisconnectDialog;

    private ColorStateList mTint;

    public BluetoothDevicePreference(Context context, CachedBluetoothDevice cachedDevice) {
        super(context);

        if (sDimAlpha == Integer.MIN_VALUE) {
            TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.disabledAlpha, outValue, true);
            sDimAlpha = (int) (outValue.getFloat() * 255);
        }

        mCachedDevice = cachedDevice;

        setLayoutResource(R.layout.preference_bt_icon);

        if (cachedDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
            UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
            if (!um.hasUserRestriction(DISALLOW_CONFIG_BLUETOOTH)) {
//                setWidgetLayoutResource(R.layout.preference_bluetooth);
                setWidgetLayoutResource(R.layout.preference_widget_next_page_detail);
            }
        } else {
            setWidgetLayoutResource(R.layout.preference_widget_next_page);
        }

        mCachedDevice.registerCallback(this);

        onDeviceAttributesChanged();
    }

    CachedBluetoothDevice getCachedDevice() {
        return mCachedDevice;
    }

    public void setOnSettingsClickListener(OnClickListener listener) {
        mOnSettingsClickListener = listener;
    }

    @Override
    protected void onPrepareForRemoval() {
        super.onPrepareForRemoval();
        Log.d(TAG, "onPrepareForRemoval");
        mCachedDevice.unregisterCallback(this);
        if (mDisconnectDialog != null) {
            Log.d(TAG, "dismiss dialog");
            mDisconnectDialog.dismiss();
            mDisconnectDialog = null;
        }
    }

    public void onDeviceAttributesChanged() {
        /*
         * The preference framework takes care of making sure the value has
         * changed before proceeding. It will also call notifyChanged() if
         * any preference info has changed from the previous value.
         */
        setTitle(mCachedDevice.getName());

        int summaryResId = mCachedDevice.getConnectionSummary();
        if (summaryResId != 0) {
            setSummary(summaryResId);
        } else {
            setSummary(null);   // empty summary for unpaired devices
        }

        int iconResId = getBtClassDrawable();
        if (iconResId != 0) {
            setIcon(iconResId);
//            Drawable drawable = getContext().getDrawable(iconResId);
//            if (mTint == null) {
//                getTintColor();
//            }
//            drawable.setTintList(mTint);
//            setIcon(drawable);
        }

        // Used to gray out the item
        setEnabled(!mCachedDevice.isBusy());

        // This could affect ordering, so notify that
        notifyHierarchyChanged();
    }

    @Override
    protected void onBindView(View view) {
        // Disable this view if the bluetooth enable/disable preference view is off
        if (null != findPreferenceInHierarchy("bt_checkbox")) {
            setDependency("bt_checkbox");
        }

        if (mCachedDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
//            ImageView deviceDetails = (ImageView) view.findViewById(R.id.deviceDetails);
            ImageView deviceDetails = (ImageView) view.findViewById(R.id.next_widget_arrow);

            if (deviceDetails != null) {
                deviceDetails.setOnClickListener(this);
                deviceDetails.setTag(mCachedDevice);
            }
        }

        super.onBindView(view);
    }

    public void onClick(View v) {
        // Should never be null by construction
        if (mOnSettingsClickListener != null) {
            mOnSettingsClickListener.onClick(v);
        }
    }

    @Override
    public boolean equals(Object o) {
        if ((o == null) || !(o instanceof BluetoothDevicePreference)) {
            return false;
        }
        return mCachedDevice.equals(
                ((BluetoothDevicePreference) o).mCachedDevice);
    }

    @Override
    public int hashCode() {
        return mCachedDevice.hashCode();
    }

    @Override
    public int compareTo(Preference another) {
        if (!(another instanceof BluetoothDevicePreference)) {
            // Rely on default sort
            return super.compareTo(another);
        }

        return mCachedDevice
                .compareTo(((BluetoothDevicePreference) another).mCachedDevice);
    }

    void onClicked() {
        int bondState = mCachedDevice.getBondState();

        if (mCachedDevice.isConnected()) {
            askDisconnect();
        } else if (bondState == BluetoothDevice.BOND_BONDED) {
            Log.d(TAG, mCachedDevice.getName() + " connect");
            mCachedDevice.connect(true);
        } else if (bondState == BluetoothDevice.BOND_NONE) {
            pair();
        }
    }

    // Show disconnect confirmation dialog for a device.
    private void askDisconnect() {
        Context context = getContext();
        String name = mCachedDevice.getName();
        if (TextUtils.isEmpty(name)) {
            name = context.getString(R.string.bluetooth_device);
        }
        String message = context.getString(R.string.bluetooth_disconnect_all_profiles, name);
        String title = context.getString(R.string.bluetooth_disconnect_title);

        DialogInterface.OnClickListener disconnectListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                mCachedDevice.disconnect();
            }
        };

        mDisconnectDialog = Utils.showDisconnectDialog(context,
                mDisconnectDialog, disconnectListener, title, Html.fromHtml(Html.fromHtml(message).toString()));
    }

    private void pair() {
        if (!mCachedDevice.startPairing()) {
            Utils.showError(getContext(), mCachedDevice.getName(),
                    R.string.bluetooth_pairing_error_message);
        } else {
            final Context context = getContext();

            SearchIndexableRaw data = new SearchIndexableRaw(context);
            data.className = BluetoothSettings.class.getName();
            data.title = mCachedDevice.getName();
            data.screenTitle = context.getResources().getString(R.string.bluetooth_settings);
            data.iconResId = R.drawable.ic_settings_bluetooth;
            data.enabled = true;

            Index.getInstance(context).updateFromSearchIndexableData(data);
        }
    }

    private int getBtClassDrawable() {
        BluetoothClass btClass = mCachedDevice.getBtClass();
        if (btClass != null) {
            switch (btClass.getMajorDeviceClass()) {
                case BluetoothClass.Device.Major.COMPUTER:
                    return R.drawable.ic_bt_laptop_hb;

                case BluetoothClass.Device.Major.PHONE:
                    return R.drawable.ic_bt_cellphone_hb;

                case BluetoothClass.Device.Major.PERIPHERAL:
                    return getHidClassDrawable(btClass);

                case BluetoothClass.Device.Major.IMAGING:
                    return R.drawable.ic_bt_imaging_hb;

                default:
                    // unrecognized device class; continue
                    Log.d(TAG, "unrecognized device class " + btClass);
            }
        } else {
            Log.w(TAG, "mBtClass is null");
        }

        if (btClass != null) {
            if (btClass.doesClassMatch(BluetoothClass.PROFILE_A2DP)) {
                return R.drawable.ic_bt_headphones_a2dp_hb;

            }
            if (btClass.doesClassMatch(BluetoothClass.PROFILE_HEADSET)) {
                return R.drawable.ic_bt_headset_hfp_hb;
            }
        }
        List<LocalBluetoothProfile> profiles = mCachedDevice.getProfiles();
        for (LocalBluetoothProfile profile : profiles) {
            int resId = profile.getDrawableResource(btClass);
            if (resId != 0) {
                return resId;
            }
        }
        return R.drawable.ic_settings_bluetooth_alpha;
    }

    public static int getHidClassDrawable(BluetoothClass btClass) {
        switch (btClass.getDeviceClass()) {
            case BluetoothClass.Device.PERIPHERAL_KEYBOARD:
            case BluetoothClass.Device.PERIPHERAL_KEYBOARD_POINTING:
                return R.drawable.ic_lockscreen_ime_hb;
            case BluetoothClass.Device.PERIPHERAL_POINTING:
                return R.drawable.ic_bt_pointing_hid_hb;
            default:
                return R.drawable.ic_bt_misc_hid_hb;
        }
    }

    private void getTintColor() {
        TypedValue outValue = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.colorAccent, outValue, true);
        mTint = ColorStateList.valueOf(outValue.data);
    }
}