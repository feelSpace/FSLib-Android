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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Arrays;

import static de.feelspace.fslib.GattOperationState.STATE_FAILED;
import static de.feelspace.fslib.GattOperationState.STATE_STARTED;
import static de.feelspace.fslib.GattOperationState.STATE_SUCCESS;

/**
 * Implementation of BLE operation to read a characteristic.
 */
class GattOperationReadCharacteristic extends GattOperation {

    // Debug
    @SuppressWarnings("unused")
    private static final String DEBUG_TAG = "FeelSpace-Debug";
    @SuppressWarnings("unused")
    private static final boolean DEBUG = true;

    /** The target characteristic of the operation. */
    private @NonNull BluetoothGattCharacteristic characteristic;

    /** Store the value to be read. */
    private @Nullable byte[] value = null;

    /**
     * Creates a read characteristic operation.
     *
     * @param gatt The GATT service.
     * @param characteristic The characteristic to read.
     */
    GattOperationReadCharacteristic(@NonNull BluetoothGatt gatt,
                                    @NonNull BluetoothGattCharacteristic characteristic) {
        super(gatt, null);
        this.characteristic = characteristic;
    }

    /**
     * Returns the value read.
     * @return The value read.
     */
    public @Nullable byte[] getValue() {
        return value;
    }

    /**
     * Returns the characteristic targeted by the operation.
     * @return the characteristic targeted by the operation.
     */
    protected @NonNull BluetoothGattCharacteristic getCharacteristic() {
        return characteristic;
    }

    @Override
    protected void start() {
        setState(STATE_STARTED);
        try {
            if(!gatt.readCharacteristic(characteristic)) {
                setState(STATE_FAILED);
            }
        } catch (Exception e) {
            // Operation failed
            setState(STATE_FAILED);
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt,
                                     BluetoothGattCharacteristic characteristic, int status) {
        if (getState() == STATE_STARTED && characteristic == this.characteristic) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (characteristic.getValue() != null) {
                    value = Arrays.copyOf(characteristic.getValue(),
                            characteristic.getValue().length);
                }
                // Operation callback
                setState(STATE_SUCCESS);
            } else {
                // Operation callback
                setState(STATE_FAILED);
            }
        }
    }

    @Override
    public String toString() {
        try {
            return "Read characteristic '" + characteristic.getUuid().toString()+"'";
        } catch (Exception e) {
            return "Read characteristic 'Unknown UUID'";
        }
    }

}
