/*
 * Copyright (c) 2017-2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */
package de.feelspace.fslib;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;

import static de.feelspace.fslib.GattOperationState.STATE_FAILED;
import static de.feelspace.fslib.GattOperationState.STATE_STARTED;
import static de.feelspace.fslib.GattOperationState.STATE_SUCCESS;

/**
 * Implementation of BLE operation to write a request on a characteristic and wait a notification.
 */
class GattOperationRequest extends GattOperation {

    // Debug
    @SuppressWarnings("unused")
    private static final String DEBUG_TAG = "FeelSpace-Debug";
    @SuppressWarnings("unused")
    private static final boolean DEBUG = true;

    /** The target characteristic of the operation. */
    private @NonNull BluetoothGattCharacteristic writeCharacteristic;

    /** The characteristic to receive the notification. */
    private @NonNull BluetoothGattCharacteristic notifyCharacteristic;

    /** Store the value to be written. */
    private @NonNull byte[] writeValue;

    /** Pattern for the response. */
    private @Nullable Byte[] notifyPattern;

    /** Request ID */
    private int requestId;

    /** Notified value. */
    private @Nullable byte[] notifyValue;

    /** Flag for write acknowledgment. */
    private boolean writeAcknowledged = false;

    /**
     * Creates a write characteristic operation.
     *
     * @param gatt The GATT service.
     * @param writeCharacteristic The characteristic to write.
     * @param notifyCharacteristic The characteristic to be notified.
     * @param writeValue The value to be written.
     * @param notifyPattern The pattern for the notification.
     * @param requestId ID to identify the request and callback.
     */
    GattOperationRequest(@NonNull BluetoothGatt gatt,
                         @NonNull BluetoothGattCharacteristic writeCharacteristic,
                         @NonNull BluetoothGattCharacteristic notifyCharacteristic,
                         @NonNull byte[] writeValue,
                         @Nullable Byte[] notifyPattern,
                         int requestId) {
        super(gatt, null);
        this.writeCharacteristic = writeCharacteristic;
        this.notifyCharacteristic = notifyCharacteristic;
        this.writeValue = Arrays.copyOf(writeValue, writeValue.length);
        this.notifyPattern = null;
        if (notifyPattern != null) {
            this.notifyPattern = Arrays.copyOf(notifyPattern, notifyPattern.length);
        }
        this.requestId = requestId;
    }

    @Override
    protected void start() {
        setState(STATE_STARTED);
        try {
            writeCharacteristic.setValue(writeValue);
            if(!gatt.writeCharacteristic(writeCharacteristic)) {
                setState(STATE_FAILED);
            }
        } catch (Exception e) {
            // Operation failed
            setState(STATE_FAILED);
        }
    }

    /**
     * Returns the characteristic notified.
     * @return the characteristic notified.
     */
    protected @NonNull BluetoothGattCharacteristic getNotifiedCharacteristic() {
        return notifyCharacteristic;
    }

    /**
     * Returns the value notified.
     * @return the value notified.
     */
    protected @Nullable byte[] getNotifiedValue() {
        return notifyValue;
    }

    /**
     * Returns the request ID.
     * @return the request ID.
     */
    protected int getRequestId() {
        return requestId;
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt,
                                      BluetoothGattCharacteristic characteristic, int status) {
        if (getState() == STATE_STARTED && characteristic == this.writeCharacteristic) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Write ok; wait for notification
                writeAcknowledged = true;
            } else {
                // Operation callback
                setState(STATE_FAILED);
            }
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic) {
        if (getState() == STATE_STARTED && characteristic == this.notifyCharacteristic &&
                writeAcknowledged) {
            // Check notification pattern
            byte[] notified = characteristic.getValue();
            if (notified == null) {
                if (notifyPattern != null && notifyPattern.length > 0) {
                    return;
                }
            } else if (notifyPattern != null && notifyPattern.length > 0) {
                if (notifyPattern.length > notified.length) {
                    return;
                }
                for (int i=0; i<notifyPattern.length; i++) {
                    if (notifyPattern[i] != null) {
                        if (notifyPattern[i] != notified[i]) {
                            return;
                        }
                    }
                }
            }
            // Pattern verified
            if (notified != null) {
                notifyValue = Arrays.copyOf(characteristic.getValue(),
                        characteristic.getValue().length);
            }
            setState(STATE_SUCCESS);
        }
    }

    @Override
    public String toString() {
        try {
            return "Write characteristic '"+ writeCharacteristic.getUuid().toString()+"' with '"+
                    Arrays.toString(writeValue)+"' and wait notification on '" +
                    notifyCharacteristic.getUuid().toString()+"'";
        } catch (Exception e) {
            // In case the characteristic UUID is no more accessible
            return "Write and wait characteristic 'Unknown UUID'";
        }
    }
}
