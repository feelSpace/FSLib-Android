/*
 * Copyright (c) 2017-2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */
package de.feelspace.fslib;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import static de.feelspace.fslib.GattOperationState.STATE_CANCELLED;
import static de.feelspace.fslib.GattOperationState.STATE_NOT_STARTED;
import static de.feelspace.fslib.GattOperationState.STATE_STARTED;
import static de.feelspace.fslib.GattOperationState.STATE_SUCCESS;
import static de.feelspace.fslib.GattOperationState.STATE_TIMED_OUT;

/**
 * Encapsulation of a GATT operation initiated by the client application.
 * An 'operation' abstraction is necessary for asynchronous BLE operations (a queue of operations
 * is required).
 */
abstract class GattOperation extends BluetoothGattCallback {

    // Debug
    @SuppressWarnings("unused")
    private static final String DEBUG_TAG = "FeelSpace-Debug";
    @SuppressWarnings("unused")
    private static final boolean DEBUG = true;

    /* GATT service for the execution of the operation. */
    protected @NonNull BluetoothGatt gatt;

    /* Timeout period */
    protected static final long GATT_OPERATION_DEFAULT_TIMEOUT_MS = 500;
    protected long operationTimeout = GATT_OPERATION_DEFAULT_TIMEOUT_MS;

    /* State of the operation */
    private volatile @NonNull GattOperationState state = STATE_NOT_STARTED;

    /**
     * References the GATT service and the callback.
     * @param gatt The GATT service.
     * @param operationTimeout The timeout period in milliseconds.
     */
    protected GattOperation(@NonNull BluetoothGatt gatt,
                            @Nullable Long operationTimeout) {
        this.gatt = gatt;
        if (operationTimeout != null && operationTimeout > 0) {
            this.operationTimeout = operationTimeout;
        }
    }

    /**
     * Returns the timeout period for this BLE operation.
     *
     * @return the timeout period for this BLE operation.
     */
    protected long getOperationTimeoutMs() {
        return operationTimeout;
    }

    /**
     * Sets the state of the operation. The completion callback is not called, instead the operation
     * queue must later check the state of the operation.
     *
     * @param state The state of the operation.
     */
    protected void setState(@NonNull GattOperationState state) {
        // Log for failure
        switch (state) {
            case STATE_CANCELLED:
                if (DEBUG) Log.w(DEBUG_TAG, "GattOperation: Cancelled BLE operation: "+toString());
                break;
            case STATE_FAILED:
                Log.e(DEBUG_TAG, "GattOperation: BLE operation failed: "+toString());
                break;
            case STATE_TIMED_OUT:
                Log.e(DEBUG_TAG, "GattOperation: BLE operation timed out: "+toString());
                break;
            case STATE_NOT_STARTED:
            case STATE_STARTED:
            case STATE_SUCCESS:
                break;
        }
        // Set state
        this.state = state;
    }

    /**
     * Returns the state of the operation.
     * @return the state of the operation.
     */
    protected @NonNull GattOperationState getState() {
        return state;
    }

    /**
     * Starts the operation.
     *
     * The state of the operation must be updated in the implementation.
     */
    abstract protected void start();

    /**
     * Checks if the operation is done and can be removed from the list of BLE operations.
     *
     * This method is called by the operation queue to schedule operations. The default
     * implementation checks the state of the operation.
     */
    protected boolean isDone() {
        return (state != STATE_NOT_STARTED && state != STATE_STARTED);
    }

    /**
     * Returns <code>true</code> if the operation succeed, <code>false</code> otherwise.
     * @return  <code>true</code> if the operation succeed, <code>false</code> otherwise.
     */
    protected boolean succeed() {
        return (state == STATE_SUCCESS);
    }

}
