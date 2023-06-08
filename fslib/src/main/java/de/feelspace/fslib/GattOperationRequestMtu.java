/*
 * Copyright (c) 2017-2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */
package de.feelspace.fslib;

import static de.feelspace.fslib.GattOperationState.STATE_FAILED;
import static de.feelspace.fslib.GattOperationState.STATE_STARTED;
import static de.feelspace.fslib.GattOperationState.STATE_SUCCESS;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;

/**
 * Implementation of BLE operation to request a new MTU size.
 */
class GattOperationRequestMtu extends GattOperation {
    // Debug
    @SuppressWarnings("unused")
    private static final String DEBUG_TAG = "FeelSpace-Debug";
    @SuppressWarnings("unused")
    private static final boolean DEBUG = true;

    /** Requested size. */
    private final int requestMtuSize;

    /**
     * Creates a operation.
     *
     * @param gatt The GATT service.
     * @param requestMtuSize The requested MTU size.
     */
    GattOperationRequestMtu(@NonNull BluetoothGatt gatt,
                            int requestMtuSize) {
        super(gatt, null);
        this.requestMtuSize = requestMtuSize;
    }

    /**
     * Returns the requested MTU size.
     * @return the requested MTU size.
     */
    protected int getRequestedMtu() {
        return requestMtuSize;
    }

    @Override
    protected void start() {
        setState(STATE_STARTED);
        try {
            if (!gatt.requestMtu(requestMtuSize)) {
                setState(STATE_FAILED);
            }
        } catch (SecurityException e) {
            // Operation failed
            Log.e(DEBUG_TAG, "GattOperationRequestMtu: Missing permissions!", e);
            setState(STATE_FAILED);
        } catch (Exception e) {
            // Operation failed
            setState(STATE_FAILED);
        }
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        if (getState() == STATE_STARTED) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Operation callback
                setState(STATE_SUCCESS);
                Log.i(DEBUG_TAG, "GattOperationRequestMtu: MTU size set to "+mtu);
            } else {
                // Operation callback
                setState(STATE_FAILED);
            }
        }
    }

    @NonNull
    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        return String.format("Request MTU size %d.", requestMtuSize);
    }
}
