/*
 * Copyright (c) 2018-2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */
package de.feelspace.fslib;

/**
 * Callback interface for the 'BluetoothActivationFragment'.
 */
public interface OnBluetoothActivationCallback {

    /**
     * Called when Bluetooth is ready.
     */
    void onBluetoothActivated();

    /**
     * Called when one of the steps for activating Bluetooth has been rejected.
     */
    void onBluetoothActivationRejected();

    /**
     * Called when one of the steps for activating Bluetooth has failed.
     */
    void onBluetoothActivationFailed();

}
