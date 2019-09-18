/*
 * Copyright (c) 2018-2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */
package de.feelspace.fslib;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;

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
    private BluetoothScannerDelegate callback;

    // Scan timeout (not null when scanning)
    private ScheduledFuture scanTimeoutTask;
    private ScheduledThreadPoolExecutor executor;
    private long scanTimeoutMs = DEFAULT_SCAN_TIMEOUT_MS;
    private static final long DEFAULT_SCAN_TIMEOUT_MS = 5000;

    // BLE scanners
    private BluetoothLeScanner bleScannerPost21;
    private ScanSettings bleScanSettingsPost21;
    private BLEScanCallbackPost21 scanCallBackPost21;
    private BLEScanCallbackPre21 scanCallBackPre21;

    // Device name to identify belt
    public static final String BELT_NAME_PATTERN = "naviguertel.*"; // Lower-case for comparison

    // List of devices to avoid duplicates
    private ArrayList<BluetoothDevice> beltsFound = new ArrayList<>(5);

    /**
     * Constructor with a callback for results of the scan procedure.
     * @param callback The callback for returning results of scan.
     */
    public BluetoothScanner(@NonNull BluetoothScannerDelegate callback) {
        this.callback = callback;
        executor = new ScheduledThreadPoolExecutor(1);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            executor.setRemoveOnCancelPolicy(true);
        }
    }

    /**
     * Starts (or re-start) the scan procedure with a timeout.
     */
    public void startScan() {
        boolean failed = false;
        synchronized (this) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                // No Bluetooth available
                Log.e(DEBUG_TAG, "BluetoothScanner: No BT for scan.");
                callback.onScanFailed();
                return;
            }
            // Set state
            scanning = true;
            // Clear list of belts found
            beltsFound.clear();
            // Initialize scan callback
            if (android.os.Build.VERSION.SDK_INT < 21) {
                if (scanCallBackPre21 == null) {
                    scanCallBackPre21 = new BLEScanCallbackPre21();
                }
            } else {
                if (bleScannerPost21 == null) {
                    bleScannerPost21 = bluetoothAdapter.getBluetoothLeScanner();
                }
                if (bleScanSettingsPost21 == null) {
                    bleScanSettingsPost21 = new ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
                }
                if (scanCallBackPost21 == null) {
                    scanCallBackPost21 = new BLEScanCallbackPost21();
                }
            }
            // Start scan
            // Note: No scan filter because it does seem to be broken
            try {
                if (android.os.Build.VERSION.SDK_INT < 21) {
                    //noinspection deprecation
                    bluetoothAdapter.startLeScan(scanCallBackPre21);
                } else {
                    bleScannerPost21.startScan(null, bleScanSettingsPost21,
                            scanCallBackPost21);
                }
            } catch (Exception e) {
                scanning = false;
                Log.e(DEBUG_TAG, "Unable to start the scan procedure.", e);
                callback.onScanFailed();
                return;
            }
            // Cancel previous timeout task
            if (scanTimeoutTask != null) {
                scanTimeoutTask.cancel(true);
                scanTimeoutTask = null;
            }
            // Start timeout task
            try {
                scanTimeoutTask = executor.schedule(
                        new Runnable() {
                            @Override
                            public void run() {
                                synchronized (BluetoothScanner.this) {
                                    scanTimeoutTask = null;
                                }
                                stopScan(false);
                            }
                        }, scanTimeoutMs, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                Log.e(DEBUG_TAG, "Unable to start the scan timeout task.", e);
            }
        }
        callback.onScanStarted();
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
                if (android.os.Build.VERSION.SDK_INT < 21) {
                    if (scanCallBackPre21 != null) {
                        //noinspection deprecation
                        bluetoothAdapter.stopLeScan(scanCallBackPre21);
                    }
                } else {
                    if (bleScannerPost21 != null && scanCallBackPost21 != null) {
                        bleScannerPost21.stopScan(scanCallBackPost21);
                    }
                }
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
            String deviceName = device.getName();
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
     * Callback methods for BLE scan results, when API < 21.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private class BLEScanCallbackPre21 implements BluetoothAdapter.LeScanCallback {

        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            checkAndNotifyBeltFound(device);
        }
    }

    /**
     * Callback methods for BLE scan results, when API >= 21.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private class BLEScanCallbackPost21 extends ScanCallback {

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
