/*
 * Copyright (c) 2018-2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */
package de.feelspace.fslib;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Scanner for belt.
 */
class BluetoothScanner {

    // Debug
    @SuppressWarnings("unused")
    private static final String DEBUG_TAG = "FeelSpace-Debug";
    @SuppressWarnings("unused")
    private static final boolean DEBUG = true;

    // State
    private boolean scanning = false;

    // Callback
    private final BluetoothScannerDelegate callback;

    // Scan timeout (not null when scanning)
    private ScheduledFuture<?> scanTimeoutTask;
    private final ScheduledThreadPoolExecutor executor;
    private static final long DEFAULT_SCAN_TIMEOUT_MS = 5000;

    // BLE scanners
    private BluetoothLeScanner bleScanner;
    private ScanSettings bleScanSettings;
    private BLEScanCallback scanCallBack;

    // Device name to identify belt
    public static final String BELT_NAME_PATTERN = "(?i)naviguertel.*|(?i)vs.*"; // Lower-case for comparison

    // List of devices to avoid duplicates
    private final ArrayList<BluetoothDevice> beltsFound = new ArrayList<>(5);

    /**
     * Constructor with a callback for results of the scan procedure.
     * @param callback The callback for returning results of scan.
     */
    public BluetoothScanner(@NonNull ScheduledThreadPoolExecutor executor,
            @NonNull BluetoothScannerDelegate callback) {
        this.callback = callback;
        this.executor = executor;
    }

    /**
     * Starts (or re-start) the scan procedure with a timeout.
     */
    public void startScan() {
        boolean failed = false;
        synchronized (this) {
            // Cancel previous timeout task
            if (scanTimeoutTask != null) {
                scanTimeoutTask.cancel(true);
                scanTimeoutTask = null;
            }
            // Retrieve BLE adapter
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                // No Bluetooth available
                Log.e(DEBUG_TAG, "BluetoothScanner: No BT for scan.");
                failed = true;
            }
            // Start scan
            if (!failed) {
                // Set state
                scanning = true;
                // Clear list of belts found
                beltsFound.clear();
                // Initialize scan callback
                if (bleScanner == null) {
                    bleScanner = bluetoothAdapter.getBluetoothLeScanner();
                }
                if (bleScanSettings == null) {
                    bleScanSettings = new ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
                }
                if (scanCallBack == null) {
                    scanCallBack = new BLEScanCallback();
                }
                // Start scan
                // Note: No scan filter because it does seem to be broken
                try {
                    bleScanner.startScan(null, bleScanSettings,
                            scanCallBack);
                } catch (SecurityException securityException) {
                    scanning = false;
                    Log.e(DEBUG_TAG, "Missing permissions for scanning.", securityException);
                    failed = true;
                } catch (Exception e) {
                    scanning = false;
                    Log.e(DEBUG_TAG, "Unable to start the scan procedure.", e);
                    failed = true;
                }
            }
            // Start timeout task
            if (!failed) {
                try {
                    scanTimeoutTask = executor.schedule(
                            () -> {
                                synchronized (BluetoothScanner.this) {
                                    scanTimeoutTask = null;
                                }
                                stopScan(false);
                            }, DEFAULT_SCAN_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    Log.e(DEBUG_TAG, "Unable to start the scan timeout task.", e);
                }
            }
        }
        if (failed) {
            callback.onScanFailed();
        } else {
            callback.onScanStarted();
        }
    }

    /**
     * Stops the scan procedure and cancel the timeout.
     */
    public void stopScan() {
        stopScan(false);
    }

    /**
     * Stops the scan procedure after a request or failure.
     * @param failed <code>true</code> if the scan failed.
     */
    private void stopScan(boolean failed) {
        boolean notify = false;
        if (DEBUG) Log.i(DEBUG_TAG, "BluetoothScanner: Stop scan.");
        synchronized (this) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                // No Bluetooth available
                return;
            }
            // Check if notify
            if (scanning) {
                scanning = false;
                notify = true;
            }
            // Stop BLE scan, even if not scanning
            try {
                if (bleScanner != null) {
                    bleScanner.stopScan(scanCallBack);
                }
            } catch (SecurityException s) {
                if (DEBUG) Log.e(DEBUG_TAG, "Missing permission for stopping scan.", s);
            } catch (IllegalStateException i) {
                if (DEBUG) Log.d(DEBUG_TAG, "Scan procedure already stopped.");
            } catch (Exception e) {
                Log.e(DEBUG_TAG, "Failed to stop the scan procedure.", e);
            }
            // Cancel timeout task
            if (scanTimeoutTask != null) {
                scanTimeoutTask.cancel(true);
                scanTimeoutTask = null;
            }
            // Clear list of belts found
            beltsFound.clear();
        }
        if (failed) {
            callback.onScanFailed();
        } else if (notify) {
            callback.onScanFinished();
        }
    }

    /**
     * Checks the device found and notifies it to the callback if it is a belt.
     *
     * @param device The device to check and possibly notify.
     */
    private void checkAndNotifyBeltFound(BluetoothDevice device) {
        if (device == null) {
            return;
        }
        if (DEBUG) Log.d(DEBUG_TAG, "Advertisement received from: " + device.getAddress());
        // Check name
        boolean isNewBelt = false;
        synchronized (this) {
            String deviceName = null;
            try {
                deviceName = device.getName();
            } catch (SecurityException s) {
                if (DEBUG) Log.e(DEBUG_TAG, "Missing permission to get device name.", s);
            }
            if (deviceName != null && deviceName.toLowerCase().matches(BELT_NAME_PATTERN) &&
                    scanTimeoutTask != null) {
                // Check for duplicate
                for (BluetoothDevice b: beltsFound) {
                    if (b.getAddress().equals(device.getAddress())) {
                        return;
                    }
                }
                // New belt found
                isNewBelt = true;
                beltsFound.add(device);
            }
        }
        if (isNewBelt) {
            callback.onBeltFound(device);
        }
    }

    /**
     * Callback methods for BLE scan results, when API >= 21.
     */
    private class BLEScanCallback extends ScanCallback {

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult r: results) {
                BluetoothDevice device = r.getDevice();
                checkAndNotifyBeltFound(device);
            }
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (result == null) {
                return;
            }
            BluetoothDevice device = result.getDevice();
            checkAndNotifyBeltFound(device);
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(DEBUG_TAG, "Scan failed.");
            stopScan(true);
        }
    }

    /**
     * Callback interface for belt scanner.
     */
    public interface BluetoothScannerDelegate {

        /**
         * Called when the scan procedure is started.
         */
        void onScanStarted();

        /**
         * Called when an advertising belt is found.
         *
         * @param device The belt found.
         */
        void onBeltFound(@NonNull BluetoothDevice device);

        /**
         * Called when the scan procedure terminates.
         */
        void onScanFinished();

        /**
         * Called when the scan procedure fails
         */
        void onScanFailed();

    }
}
