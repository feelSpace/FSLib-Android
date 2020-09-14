/*
 * Copyright (c) 2018-2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */
package de.feelspace.fslibandroiddemo;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

/**
 * Utility fragment to check and activate Bluetooth on the device.
 *
 * This fragment must be attached to an Activity or Fragment that implements
 * <code>OnBluetoothActivationCallback</code>.
 * The fragment is retained on configuration change.
 */
public class BluetoothActivationFragment extends Fragment {

    // Debug
    @SuppressWarnings("unused")
    private static final String DEBUG_TAG = "FeelSpace-Debug";
    @SuppressWarnings("unused")
    private static final boolean DEBUG = true;

    // Codes for dialog callbacks
    protected static final int ENABLE_LOCATION_PERMISSION_CODE = 71;
    protected static final int ENABLE_BLUETOOTH_REQUEST_CODE = 72;

    // Selection callback
    private OnBluetoothActivationCallback activationCallback;

    // Flag when waiting for activation
    private boolean pendingActivation = false;

    // Flag when the location service callback failed
    private boolean locationServiceCallbackFailed = false;

    // Broadcast receiver for location enabled
    private LocationStatusChangeListener locationStatusChangeListener =
            new LocationStatusChangeListener();

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            activationCallback = (OnBluetoothActivationCallback) context;
        } catch (ClassCastException e) {
            Log.e(DEBUG_TAG, "The fragment must be attached to a context that implements " +
                    "OnBluetoothActivationListener.");
            throw new ClassCastException(context.toString()+" must implement " +
                    "OnBluetoothActivationListener.");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retain this fragment
        setRetainInstance(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check for location service callback failed
        if (pendingActivation && locationServiceCallbackFailed) {
            locationStatusChangeListener.unregisterListener();
            activateBluetooth();
        }
    }

    /**
     * Checks and activates Bluetooth.
     */
    public synchronized void activateBluetooth() {

        // Flag
        pendingActivation = true;
        locationServiceCallbackFailed = false;

        // Check for BLE support
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !isBLESupported()) {
            // Dialog to explain that BLE is not supported
            Activity activity = this.getActivity();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(de.feelspace.fslib.R.string.dialog_bt_not_supported_message);
                            builder.setPositiveButton(de.feelspace.fslib.R.string.dialog_bt_not_supported_ok,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            pendingActivation = false;
                                            activationCallback.onBluetoothActivationFailed();
                                        }
                                    });
                            builder.create().show();
                        } catch (Exception e) {
                            Log.e(DEBUG_TAG, "Unable to show dialog to inform that BT is " +
                                    "not supported.", e);
                            pendingActivation = false;
                            activationCallback.onBluetoothActivationFailed();
                        }
                    }
                });
            } else {
                // Should not happen
                // Stop activation procedure without message
                pendingActivation = false;
                activationCallback.onBluetoothActivationFailed();
            }
            // Stop activation procedure
            return;
        }

        // Check location permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.getContext() != null &&
                    this.getContext().checkSelfPermission(
                            Manifest.permission.ACCESS_FINE_LOCATION) !=
                            PackageManager.PERMISSION_GRANTED) {
                showDialogLocationPermissionRationale();
//                if (this.shouldShowRequestPermissionRationale(
//                        Manifest.permission.ACCESS_FINE_LOCATION)) {
//                    showDialogLocationPermissionRationale();
//                }
//                // Request permission
//                if (DEBUG) Log.d(DEBUG_TAG, "Request Location permission.");
//                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
//                        ENABLE_LOCATION_PERMISSION_CODE);
                return;
            }
        }

        // Check if Bluetooth enabled
        if (!bluetoothAdapter.isEnabled()) {
            // Request to switch-on Bluetootsh
            if (DEBUG) Log.d(DEBUG_TAG, "Request to switch-on Bluetooth.");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE);
            return;
        }

        // Check if location service is active
        if (!isLocationServiceActive()) {
            requestLocationServiceActivation();
            return;
        }

        // Bluetooth enabled
        pendingActivation = false;
        activationCallback.onBluetoothActivated();

    }

    /**
     * Checks if the device supports BLE.
     * @return 'true' if BLE is supported.
     */
    private boolean isBLESupported() {
        // Get bluetooth adapter
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return (bluetoothAdapter != null && this.getContext() != null &&
                this.getContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE));
    }

    /**
     * Displays a dialog with the  rationale for Location permission request, then requests the
     * permission.
     */
    private void showDialogLocationPermissionRationale() {
        try {
            Activity activity = this.getActivity();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(de.feelspace.fslib.R.string.dialog_permission_request_rationale_message);
                            builder.setPositiveButton(de.feelspace.fslib.R.string.dialog_permission_request_rationale_ok,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            try {
                                                if (DEBUG) Log.d(DEBUG_TAG,
                                                        "Request Location permission.");
                                                requestPermissions(new String[]{
                                                        Manifest.permission.ACCESS_FINE_LOCATION},
                                                        ENABLE_LOCATION_PERMISSION_CODE);
                                            } catch (Exception e) {
                                                Log.e(DEBUG_TAG,
                                                        "Unable to request permission.", e);
                                                pendingActivation = false;
                                                activationCallback.onBluetoothActivationFailed();
                                            }
                                        }
                                    });
                            builder.create().show();
                        } catch (Exception e) {
                            Log.e(DEBUG_TAG, "Unable to request location permission.", e);
                            pendingActivation = false;
                            activationCallback.onBluetoothActivationFailed();
                        }
                    }
                });
            } else {
                // Should not happen
                // Request permission without information dialog
                requestPermissions(new String[]
                                {Manifest.permission.ACCESS_FINE_LOCATION},
                        ENABLE_LOCATION_PERMISSION_CODE);
            }
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Unable to request location permission.", e);
            pendingActivation = false;
            activationCallback.onBluetoothActivationFailed();
        }
    }

    /**
     * Displays a dialog to ask the user to enable location on the device.
     */
    private void requestLocationServiceActivation() {
        try {
            // Register broadcast receiver to listen to location status change
            locationStatusChangeListener.registerListener();

            // Shows dialog to explain that location must be enabled
            Activity activity = this.getActivity();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(de.feelspace.fslib.R.string.dialog_enable_location_message);
                            builder.setPositiveButton(de.feelspace.fslib.R.string.dialog_enable_location_ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        try {
                                            if (DEBUG) Log.d(DEBUG_TAG,
                                                    "Request to enable Location.");
                                            // Show menu to enable location
                                            Intent myIntent = new Intent(
                                                    Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                            getContext().startActivity(myIntent);
                                        } catch (Exception e) {
                                            Log.e(DEBUG_TAG,
                                                    "Unable to show location settings.", e);
                                            pendingActivation = false;
                                            activationCallback.onBluetoothActivationFailed();
                                        }
                                    }
                                });
                            builder.create().show();
                        } catch (Exception e) {
                            Log.e(DEBUG_TAG, "Unable to show dialog to enable Location.", e);
                            pendingActivation = false;
                            activationCallback.onBluetoothActivationFailed();
                        }
                    }
                });
            } else {
                // Should not happen
                // Show location settings activity without information dialog
                Intent myIntent = new Intent(
                        Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                getContext().startActivity(myIntent);
            }
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Unable to show dialog to enable Location.", e);
            pendingActivation = false;
            activationCallback.onBluetoothActivationFailed();
        }
    }

    /**
     * Checks if location service is on.
     * @return 'true' if location is enabled.
     */
    private boolean isLocationServiceActive() {
        try {
            LocationManager lm = (LocationManager) getContext()
                    .getSystemService(Context.LOCATION_SERVICE);
            return (lm != null) && (lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Unable to check for location enabled.", e);
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == ENABLE_LOCATION_PERMISSION_CODE) {
            // Continue the activation if permission enabled
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (DEBUG) Log.d(DEBUG_TAG, "Location permission granted.");
                activateBluetooth();
            } else {
                if (DEBUG) Log.e(DEBUG_TAG, "Location permission not granted.");
                pendingActivation = false;
                activationCallback.onBluetoothActivationRejected();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // Result for bluetooth activation
        if (requestCode == ENABLE_BLUETOOTH_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth enabled, continue activation procedure
                if (DEBUG) Log.d(DEBUG_TAG, "Bluetooth has been activated on the device.");
                activateBluetooth();
            } else { // RESULT_CANCELED
                // Bluetooth has been disabled or request for enabling bluetooth has
                // been rejected.
                if (DEBUG) Log.e(DEBUG_TAG, "Request for activating Bluetooth has been " +
                        "rejected.");
                pendingActivation = false;
                activationCallback.onBluetoothActivationRejected();
            }
        }

    }

    /**
     * Broadcast receiver for Location status change.
     */
    private class LocationStatusChangeListener extends BroadcastReceiver {

        private boolean registered = false;

        /**
         * Registers the broadcast receiver to location status change.
         */
        public void registerListener() {
            locationServiceCallbackFailed = false;
            if (!registered) {
                try {
                    BluetoothActivationFragment.this.getActivity().registerReceiver(this,
                            new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
                    registered = true;
                } catch (Exception e) {
                    Log.e(DEBUG_TAG, "Unable to register for location status change.", e);
                }
            }
        }

        /**
         * Unregisters the broadcast receiver to location status change.
         */
        public void unregisterListener() {
            locationServiceCallbackFailed = false;
            if (registered) {
                registered = false;
                try {
                    BluetoothActivationFragment.this.getActivity().unregisterReceiver(this);
                } catch (Exception e) {
                    Log.e(DEBUG_TAG, "Unable to unregister to location status change.", e);
                }
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null &&
                    intent.getAction().matches(LocationManager.PROVIDERS_CHANGED_ACTION)) {
                if (DEBUG) Log.d(DEBUG_TAG, "Location status changed.");
                try {
                    if (isLocationServiceActive()) {
                        if (DEBUG) Log.d(DEBUG_TAG, "Location service is now active.");
                        if (pendingActivation) {
                            // Continue the procedure to activate Bluetooth
                            unregisterListener();
                            activateBluetooth();
                        }
                    }
                } catch (Exception e) {
                    Log.e(DEBUG_TAG, "BluetoothActivationFragment: Unable to continue " +
                            "the activation procedure after location service state changed.");
                    locationServiceCallbackFailed = true;
                }
            }
        }
    }

}
