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

package com.android.settings.bluetooth;

import android.app.Activity;
import hb.app.dialog.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Toast;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.bluetooth.DockService.DockBluetoothCallback;
import com.android.settings.search.Index;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager.BluetoothManagerCallback;
import com.android.settingslib.bluetooth.Utils.ErrorListener;

/**
 * Utils is a helper class that contains constants for various
 * Android resource IDs, debug logging flags, and static methods
 * for creating dialogs.
 */
public final class Utils {
    static final boolean V = com.android.settingslib.bluetooth.Utils.V; // verbose logging
    static final boolean D =  com.android.settingslib.bluetooth.Utils.D;  // regular logging

    private static final String KEY_ERROR = "errorMessage";
    private static final String TAG = "Bluetooth.Utils";

    private Utils() {
    }

    public static int getConnectionStateSummary(int connectionState) {
        switch (connectionState) {
        case BluetoothProfile.STATE_CONNECTED:
            return R.string.bluetooth_connected;
        case BluetoothProfile.STATE_CONNECTING:
            return R.string.bluetooth_connecting;
        case BluetoothProfile.STATE_DISCONNECTED:
            return R.string.bluetooth_disconnected;
        case BluetoothProfile.STATE_DISCONNECTING:
            return R.string.bluetooth_disconnecting;
        default:
            return 0;
        }
    }

    // Create (or recycle existing) and show disconnect dialog.
    static AlertDialog showDisconnectDialog(Context context,
            AlertDialog dialog,
            DialogInterface.OnClickListener disconnectListener,
            CharSequence title, CharSequence message) {
        if (dialog == null) {
            dialog = new AlertDialog.Builder(context)
                    .setPositiveButton(android.R.string.ok, disconnectListener)
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
        } else {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            // use disconnectListener for the correct profile(s)
            CharSequence okText = context.getText(android.R.string.ok);
            dialog.setButton(DialogInterface.BUTTON_POSITIVE,
                    okText, disconnectListener);
        }
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.show();
        return dialog;
    }

    // TODO: wire this up to show connection errors...
    static void showConnectingError(Context context, String name) {
        // if (!mIsConnectingErrorPossible) {
        //     return;
        // }
        // mIsConnectingErrorPossible = false;

        showError(context, name, R.string.bluetooth_connecting_error_message);
    }

    static void showError(Context context, String name, int messageResId) {
        String message = context.getString(messageResId, name);
        LocalBluetoothManager manager = getLocalBtManager(context);
        if (manager == null) {
            return;
        }
        Activity activity = (Activity) (manager.getForegroundActivity());
        if(manager.isForegroundActivity()) {
            /// M: Use DialogFragment instead of AlertDialog @{
            Log.d(TAG, "show ErrorDialogFragment, message is " + message);
            ErrorDialogFragment dialog = new ErrorDialogFragment();
            final Bundle args = new Bundle();
            args.putString(KEY_ERROR, message);
            dialog.setArguments(args);
            dialog.show(activity.getFragmentManager(), "Error");
            /// @}
        } else {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Update the search Index for a specific class name and resources.
     */
    public static void updateSearchIndex(Context context, String className, String title,
            String screenTitle, int iconResId, boolean enabled) {
        SearchIndexableRaw data = new SearchIndexableRaw(context);
        data.className = className;
        data.title = title;
        data.screenTitle = screenTitle;
        data.iconResId = iconResId;
        data.enabled = enabled;

        Index.getInstance(context).updateFromSearchIndexableData(data);
    }

    public static class ErrorDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final String message = getArguments().getString(KEY_ERROR);

            return new  AlertDialog.Builder(getActivity())
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(R.string.bluetooth_error_title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show();
        }
    }

    public static LocalBluetoothManager getLocalBtManager(Context context) {
        return LocalBluetoothManager.getInstance(context, mOnInitCallback);
    }

    private static final ErrorListener mErrorListener = new ErrorListener() {
        @Override
        public void onShowError(Context context, String name, int messageResId) {
            showError(context, name, messageResId);
        }
    };

    private static final BluetoothManagerCallback mOnInitCallback = new BluetoothManagerCallback() {
        @Override
        public void onBluetoothManagerInitialized(Context appContext,
                LocalBluetoothManager bluetoothManager) {
            bluetoothManager.getEventManager().registerCallback(
                    new DockBluetoothCallback(appContext));
            com.android.settingslib.bluetooth.Utils.setErrorListener(mErrorListener);
        }
    };
}
