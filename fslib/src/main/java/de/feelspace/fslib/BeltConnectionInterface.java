/*
 * Copyright (c) 2015-2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */
package de.feelspace.fslib;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * The belt connection interface provides a set of methods for connecting a belt via Bluetooth
 * and informing about the state of the connection.
 */
public abstract class BeltConnectionInterface {
    // Debug
    @SuppressWarnings("unused")
    private static final String DEBUG_TAG = "FeelSpace-Debug";
    @SuppressWarnings("unused")
    private static final boolean DEBUG = true;

    // State of the connexion
    protected @NonNull volatile BeltConnectionState state = BeltConnectionState.STATE_DISCONNECTED;

    // State change listeners
    protected final ArrayList<BeltConnectionListener> listeners = new ArrayList<>();

    /**
     * Preference key for the last connected device address.
     */
    public static final String KEY_PREF_LAST_CONNECTED_BELT_ADDRESS =
            "KEY_PREF_LAST_CONNECTED_BELT_ADDRESS";

    /**
     * Creates a connection interface to connect a belt. The connection interface can be used for
     * successive connection.
     *
     * @param applicationContext The application context used to access shared preferences.
     * @return a connection interface.
     * @throws IllegalArgumentException if the context passed in argument is <code>null</code>.
     */
    public static BeltConnectionInterface create(Context applicationContext)
            throws IllegalArgumentException {
        return new BeltConnectionController(applicationContext);
    }

    /**
     * Searches for advertising belts. Location permission must be granted and Bluetooth enabled
     * before calling this method.
     *
     * Advertising belts are reported to listeners using
     * {@link BeltConnectionListener#onBeltFound(BluetoothDevice)}.
     */
    public abstract void scan();

    /**
     * Stops the scan procedures if ongoing.
     */
    public abstract void stopScan();

    /**
     * Connects a belt.
     *
     * @param device the bluetooth device to connect to.
     * @throws IllegalArgumentException if the device passed in argument is <code>null</code>.
     */
    public abstract void connect(BluetoothDevice device) throws IllegalArgumentException;

    /**
     * Searches for advertising belts and connects to the first found.
     *
     * @throws IllegalStateException if an error occurs with the Bluetooth service.
     */
    public abstract void scanAndConnect() throws IllegalStateException;

    /**
     * Closes the current connection.
     */
    public abstract void disconnect();

    /**
     * Returns the state of the connection.
     *
     * @return the state of the connection.
     */
    public BeltConnectionState getState() {
        return state;
    }

    /**
     * Notifies the listeners of a connection state change.
     */
    protected void notifyState() {
        if (DEBUG) Log.i(DEBUG_TAG, "BeltConnectionInterface: Connection state changed to " +
                state.toString());
        ArrayList<BeltConnectionListener> targets;
        synchronized (this) {
            if (listeners.isEmpty()) {
                return;
            }
            targets = new ArrayList<>(listeners);
        }
        for (BeltConnectionListener l: targets) {
            l.onConnectionStateChange(state);
        }
    }

    /**
     * Notifies listeners that the connection has been lost.
     */
    protected void notifyConnectionLost() {
        if (DEBUG) Log.i(DEBUG_TAG, "BeltConnectionInterface: Connection lost.");
        ArrayList<BeltConnectionListener> targets;
        synchronized (this) {
            if (listeners.isEmpty()) {
                return;
            }
            targets = new ArrayList<>(listeners);
        }
        for (BeltConnectionListener l: targets) {
            l.onConnectionLost();
        }
    }

    /**
     * Notifies listeners that the connection procedure failed to start.
     */
    protected void notifyConnectionFailed() {
        if (DEBUG) Log.i(DEBUG_TAG, "BeltConnectionInterface: Connection failed.");
        ArrayList<BeltConnectionListener> targets;
        synchronized (this) {
            if (listeners.isEmpty()) {
                return;
            }
            targets = new ArrayList<>(listeners);
        }
        for (BeltConnectionListener l: targets) {
            l.onConnectionFailed();
        }
    }

    /**
     * Notifies listeners that the scan procedure failed to start.
     */
    protected void notifyScanFailed() {
        if (DEBUG) Log.i(DEBUG_TAG, "BeltConnectionInterface: Scan failed.");
        ArrayList<BeltConnectionListener> targets;
        synchronized (this) {
            if (listeners.isEmpty()) {
                return;
            }
            targets = new ArrayList<>(listeners);
        }
        for (BeltConnectionListener l: targets) {
            l.onScanFailed();
        }
    }

    /**
     * Notifies listeners that the scan procedure failed to start.
     */
    protected void notifyPairingFailed() {
        if (DEBUG) Log.i(DEBUG_TAG, "BeltConnectionInterface: Pairing failed.");
        ArrayList<BeltConnectionListener> targets;
        synchronized (this) {
            if (listeners.isEmpty()) {
                return;
            }
            targets = new ArrayList<>(listeners);
        }
        for (BeltConnectionListener l: targets) {
            l.onPairingFailed();
        }
    }

    /**
     * Notifies listeners that no belt was found during the scan.
     */
    protected void notifyNoBeltFound() {
        if (DEBUG) Log.i(DEBUG_TAG, "BeltConnectionInterface: No belt found.");
        ArrayList<BeltConnectionListener> targets;
        synchronized (this) {
            if (listeners.isEmpty()) {
                return;
            }
            targets = new ArrayList<>(listeners);
        }
        for (BeltConnectionListener l: targets) {
            l.onNoBeltFound();
        }
    }

    /**
     * Saves the device address of the last connected belt.
     * @param applicationContext The application context to access shared preferences.
     * @param device The last connected device.
     */
    protected void saveDeviceAddress(@NonNull Context applicationContext,
                                     @NonNull BluetoothDevice device) {
        try {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(
                    applicationContext);
            pref.edit().putString(KEY_PREF_LAST_CONNECTED_BELT_ADDRESS,
                    device.getAddress()).apply();
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "BeltConnectionInterface: Unable to save the address of the " +
                    "last connected belt.", e);
        }
    }

    /**
     * Notifies listeners that a belt has been found during the scan procedure.
     *
     * @param belt the belt found.
     */
    protected void notifyBeltFound(@NonNull BluetoothDevice belt) {
        if (DEBUG) Log.i(DEBUG_TAG, "BeltConnectionInterface: Belt found during scan: " +
                belt.getAddress());
        ArrayList<BeltConnectionListener> targets;
        synchronized (this) {
            if (listeners.isEmpty()) {
                return;
            }
            targets = new ArrayList<>(listeners);
        }
        for (BeltConnectionListener l: targets) {
            l.onBeltFound(belt);
        }
    }

    /**
     * Returns an executor to run timeout task and other delayed tasks related to this connection.
     *
     * @return an executor to run timeout task and other delayed tasks related to this connection.
     */
    protected abstract @NonNull ScheduledThreadPoolExecutor getExecutor();

    /**
     * Adds a listener for connection events.
     *
     * @param listener the listener to add.
     */
    public void addConnectionListener(BeltConnectionListener listener) {
        synchronized (this) {
            if (listener != null && !listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    /**
     * Removes a listener.
     *
     * @param listener the listener to remove.
     */
    public void removeConnectionListener(BeltConnectionListener listener) {
        synchronized (this) {
            listeners.remove(listener);
        }
    }

    /**
     * Returns the command interface for this connection. The command interface provides a simple
     * way to control the belt mode and vibration.
     *
     * Note: The same command interface can be used for successive connections established by
     * the {@link BeltConnectionInterface}.
     *
     * @return the command interface.
     */
    public abstract BeltCommandInterface getCommandInterface();

    /**
     * Returns the communication interface for this connection. The communication interface provides
     * advanced methods to control the belt. For most application it is recommended to use the
     * {@link BeltCommandInterface} returned by {@link #getCommandInterface()}.
     *
     * Note: The same communication interface can be used for successive connections established by
     * the {@link BeltConnectionInterface}.
     *
     * @return the communication interface.
     */
    public abstract BeltCommunicationInterface getCommunicationInterface();

}
