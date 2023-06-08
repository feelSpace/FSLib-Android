package de.feelspace.fslibtest;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Extension of <code>AppCompatActivity</code> to check and activate Bluetooth.
 * <p>
 * This class is meant to be derived to add the remaining activity logic.
 * <p>
 */
public class BluetoothCheckActivity extends AppCompatActivity {
    // Debug
    @SuppressWarnings("unused")
    private static final String DEBUG_TAG = "FeelSpace-Debug";
    @SuppressWarnings("unused")
    private static final boolean DEBUG = true;

    // Codes for dialog callbacks
    protected static final int ENABLE_LOCATION_PERMISSION_CODE = 71;
    protected static final int ENABLE_BLUETOOTH_PERMISSIONS_CODE = 72;
    protected static final int ENABLE_BLUETOOTH_REQUEST_CODE = 73;

    // Activation callback
    private BluetoothCheckCallback activationCallback;

    // Flag when waiting for activation
    private boolean pendingActivation = false;

    // Flag when the location service callback failed
    private boolean locationServiceCallbackFailed = false;

    // Broadcast receiver for location enabled
    private final LocationStatusChangeListener locationStatusChangeListener =
            new LocationStatusChangeListener();


    @Override
    protected void onResume() {
        super.onResume();
        // Check for location service callback failed
        if (pendingActivation && locationServiceCallbackFailed) {
            locationStatusChangeListener.unregisterListener();
            activateBluetooth(activationCallback);
        }
    }

    @Override
    protected void onDestroy() {
        locationStatusChangeListener.unregisterListener();
        super.onDestroy();
    }

    /**
     * Checks the permissions and activates the Bluetooth.
     *
     * @param callback The callback for Bluetooth activation.
     */
    protected void activateBluetooth(BluetoothCheckCallback callback) {
        this.activationCallback = callback;
        if (callback == null) {
            Log.w(DEBUG_TAG, "Bluetooth activation requested without callback!");
        }

        // Flag
        pendingActivation = true;
        locationServiceCallbackFailed = false;

        // Check for BLE support
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !isBLESupported()) {
            // Not supported
            pendingActivation = false;
            if (activationCallback != null) {
                activationCallback.onUnsupportedFeature();
            }
            // Stop activation procedure
            return;
        }

        // Check location permission
        if (Build.VERSION.SDK_INT <= 30) {
            if (checkSelfPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED) {
                // Request permission
                requestPermissions(new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION},
                        ENABLE_LOCATION_PERMISSION_CODE);
                return;
            }
        }

        // Check scan and connect permissions
        if (Build.VERSION.SDK_INT >= 31) {
            if (checkSelfPermission(
                    Manifest.permission.BLUETOOTH_CONNECT) !=
                    PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(
                            Manifest.permission.BLUETOOTH_SCAN) !=
                            PackageManager.PERMISSION_GRANTED) {
                // Request permission
                requestPermissions(new String[]{
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN},
                        ENABLE_BLUETOOTH_PERMISSIONS_CODE);
                return;
            }
        }

        // Check if Bluetooth enabled
        if (!bluetoothAdapter.isEnabled()) {
            // Request to switch-on Bluetooth
            if (DEBUG) Log.d(DEBUG_TAG, "Request to switch-on Bluetooth.");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE);
            return;
        }

        // Check if location service is active
        if (!isLocationServiceActive()) {
            try {
                if (DEBUG) Log.d(DEBUG_TAG,"Request to enable Location.");
                // Show menu to enable location
                locationStatusChangeListener.registerListener();
                Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(myIntent);
            } catch (Exception e) {
                Log.e(DEBUG_TAG, "Unable to show location settings.", e);
                pendingActivation = false;
                if (activationCallback != null) {
                    activationCallback.onUnsupportedFeature();
                }
            }
            return;
        }

        // Bluetooth enabled
        pendingActivation = false;
        if (activationCallback != null) {
            activationCallback.onBluetoothReady();
        }

    }

    /**
     * Checks if the device supports BLE.
     * @return 'true' if BLE is supported.
     */
    private boolean isBLESupported() {
        // Get bluetooth adapter
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return (bluetoothAdapter != null &&
                getPackageManager().hasSystemFeature(
                        PackageManager.FEATURE_BLUETOOTH_LE));
    }

    /**
     * Checks if location service is on.
     * @return 'true' if location is enabled.
     */
    private boolean isLocationServiceActive() {
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
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
                activateBluetooth(BluetoothCheckActivity.this.activationCallback);
            } else {
                if (DEBUG) Log.e(DEBUG_TAG, "Location permission not granted.");
                pendingActivation = false;
                if (activationCallback != null) {
                    activationCallback.onBluetoothActivationRejected();
                }
            }
        } else if (requestCode == ENABLE_BLUETOOTH_PERMISSIONS_CODE) {
            // continue activation if permissions enabled
            if (grantResults.length > 1 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                if (DEBUG) Log.d(DEBUG_TAG, "Bluetooth permissions granted.");
                activateBluetooth(BluetoothCheckActivity.this.activationCallback);
            } else {
                if (DEBUG) Log.e(DEBUG_TAG, "Bluetooth permissions not granted.");
                pendingActivation = false;
                if (activationCallback != null) {
                    activationCallback.onBluetoothActivationRejected();
                }
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result for bluetooth activation
        if (requestCode == ENABLE_BLUETOOTH_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth enabled, continue activation procedure
                if (DEBUG) Log.d(DEBUG_TAG, "Bluetooth has been activated on the device.");
                activateBluetooth(BluetoothCheckActivity.this.activationCallback);
            } else { // RESULT_CANCELED
                // Bluetooth has been disabled or request for enabling bluetooth has
                // been rejected.
                if (DEBUG) Log.e(DEBUG_TAG, "Request for activating Bluetooth has been " +
                        "rejected.");
                pendingActivation = false;
                if (activationCallback != null) {
                    activationCallback.onBluetoothActivationRejected();
                }
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
                    registerReceiver(this,
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
                    unregisterReceiver(this);
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
                            activateBluetooth(BluetoothCheckActivity.this.activationCallback);
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
