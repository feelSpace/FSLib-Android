/*
 * Copyright (c) 2015-2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */
package de.feelspace.fslib;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;

/**
 * Listener for connection state changes and scan events.
 */
public interface BeltConnectionListener {

    /**
     * Called when the scan procedure failed.
     */
    void onScanFailed();

    /**
     * Called when no belt was found during the scan procedure and the connection cannot be started.
     */
    void onNoBeltFound();

    /**
     * Called when an advertising belt has been found during the scan procedure.
     *
     * @param belt the belt found.
     */
    void onBeltFound(BluetoothDevice belt);

    /**
     * Called when the state of the connection changes.
     *
     * @param state The new state.
     */
    void onConnectionStateChange(BeltConnectionState state);

    /**
     * Called when the connection with the belt failed.
     */
    void onConnectionLost();

    /**
     * Called when the connection with the belt is lost, after possible reconnection attempts.
     */
    void onConnectionFailed();

}
