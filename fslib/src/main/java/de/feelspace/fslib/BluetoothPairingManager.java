package de.feelspace.fslib;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BluetoothPairingManager {

    // Debug
    @SuppressWarnings("unused")
    private static final String DEBUG_TAG = "FeelSpace-Debug";
    @SuppressWarnings("unused")
    private static final boolean DEBUG = true;

    // Callback
    private final BluetoothPairingManager.BluetoothPairingDelegate callback;

    // Scan timeout (not null when scanning)
    private ScheduledFuture<?> pairingTimeoutTask;
    private final ScheduledThreadPoolExecutor executor;
    private static final long DEFAULT_PAIRING_TIMEOUT_MS = 20000;

    // Context for broadcast receiver
    private final Context context;

    // State
    private boolean pairing;

    // Device
    private BluetoothDevice device;

    // Broadcast receiver
    private final PairingStatusBroadcastReceiver pairingListener;

    /**
     * Constructor with a callback for results of the pairing procedure.
     * @param callback The callback for returning results of pairing.
     */
    public BluetoothPairingManager(@NonNull Context applicationContext,
                                   @NonNull ScheduledThreadPoolExecutor executor,
                                   @NonNull BluetoothPairingManager.BluetoothPairingDelegate callback) {
        this.context = applicationContext;
        this.callback = callback;
        this.executor = executor;
        pairingListener = new PairingStatusBroadcastReceiver();
    }

    public void startPairing(@NonNull BluetoothDevice device) throws SecurityException {
        boolean failed = false;
        synchronized (this) {
            this.device = device;
            this.pairing = false;
            // Cancel previous timeout task
            if (pairingTimeoutTask != null) {
                pairingTimeoutTask.cancel(true);
                pairingTimeoutTask = null;
            }
            // Check pairing state
            if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                // Start pairing and listen to bnd events
                if (pairingListener.register()) {
                    pairing = device.createBond();
                } else {
                    failed = true;
                }
                // Start timeout
                if (!failed) {
                    try {
                        pairingTimeoutTask = executor.schedule(
                                () -> {
                                    boolean notifyFailure;
                                    synchronized (BluetoothPairingManager.this) {
                                        pairingTimeoutTask = null;
                                        notifyFailure = pairing;
                                        pairing = false;
                                    }
                                    stopPairing();
                                    if (notifyFailure) {
                                        callback.onPairingFailed();
                                    }
                                }, DEFAULT_PAIRING_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        Log.e(DEBUG_TAG, "Unable to start the pairing timeout task.", e);
                    }
                }
            }
        }
        if (failed) {
            callback.onPairingFailed();
        } else if (!pairing) {
            callback.onPairingFinished(device);
        } // else, pairing started
    }

    public void stopPairing() {
        synchronized (this) {
            this.pairing = false;
            // Unregister listener
            pairingListener.unregister();
        }
    }

    /**
     * Broadcast receiver class for bonding/pairing events.
     */
    private class PairingStatusBroadcastReceiver extends BroadcastReceiver {

        private boolean registered = false;

        public boolean register() {
            if (!registered) {
                try {
                    context.registerReceiver(this,
                            new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
                    registered = true;
                } catch (Exception e) {
                    Log.e(DEBUG_TAG, "Unable to register for bond state events.", e);
                    registered = false;
                }
            }
            return registered;
        }

        public void unregister() {
            if (registered) {
                try {
                    context.unregisterReceiver(this);
                } catch (Exception e) {
                    Log.e(DEBUG_TAG, "Unable to unregister to bond state events.", e);
                }
                registered = false;
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null &&
                    intent.getAction().matches(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                // Retrieve device and state
                try {
                    int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                            BluetoothDevice.BOND_NONE);
                    BluetoothDevice deviceNotified = intent.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE);
                    if (Objects.equals(deviceNotified.getAddress(), device.getAddress()) ) {
                        if (bondState == BluetoothDevice.BOND_BONDED) {
                            // Completed
                            synchronized (BluetoothPairingManager.this) {
                                pairing = false;
                                // Cancel timeout task
                                if (pairingTimeoutTask != null) {
                                    pairingTimeoutTask.cancel(true);
                                    pairingTimeoutTask = null;
                                }
                            }
                            unregister();
                            callback.onPairingFinished(device);
                        } else if (bondState == BluetoothDevice.BOND_BONDING) {
                            Log.i(DEBUG_TAG, "Bonding start event notified.");
                        }
                    }
                } catch (Exception e) {
                    Log.e(DEBUG_TAG, "Unable to retrieve bond event parameters.", e);
                }
            }
        }

    }

    /**
     * Callback interface for belt pairing.
     */
    public interface BluetoothPairingDelegate {

        /**
         * Called when the pairing procedure terminates.
         * @param device The paired device.
         */
        void onPairingFinished(BluetoothDevice device);

        /**
         * Called when the pairing procedure fails.
         */
        void onPairingFailed();

    }
}
