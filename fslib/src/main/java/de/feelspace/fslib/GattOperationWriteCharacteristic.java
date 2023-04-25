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
 * Implementation of BLE operation to read characteristic.
 */
class GattOperationWriteCharacteristic extends GattOperation {

    // Debug
    @SuppressWarnings("unused")
    private static final String DEBUG_TAG = "FeelSpace-Debug";
    @SuppressWarnings("unused")
    private static final boolean DEBUG = true;

    /** The target characteristic of the operation. */
    private @NonNull BluetoothGattCharacteristic characteristic;

    /** Store the value to be written. */
    private @NonNull byte[] value;

    /**
     * Creates a write characteristic operation.
     *
     * @param gatt The GATT service.
     * @param characteristic The characteristic to write.
     * @param value The value to be written.
     */
    GattOperationWriteCharacteristic(@NonNull BluetoothGatt gatt,
                                     @NonNull BluetoothGattCharacteristic characteristic,
                                     @NonNull byte[] value) {
        super(gatt, null);
        this.characteristic = characteristic;
        this.value = Arrays.copyOf(value, value.length);
    }

    @Override
    protected void start() {
        setState(STATE_STARTED);
        try {
            characteristic.setValue(value);
            if(!gatt.writeCharacteristic(characteristic)) {
                setState(STATE_FAILED);
            }
        } catch (Exception e) {
            // Operation failed
            setState(STATE_FAILED);
        }
    }

    /**
     * Returns the value to write.
     * @return the value to write.
     */
    protected @Nullable byte[] getValue() {
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
    public void onCharacteristicWrite(BluetoothGatt gatt,
                                      BluetoothGattCharacteristic characteristic, int status) {
        if (getState() == STATE_STARTED && characteristic == this.characteristic) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
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
            return "Write characteristic '"+ characteristic.getUuid().toString()+"' with '"+
                    Arrays.toString(value)+"'";
        } catch (Exception e) {
            // In case the characteristic UUID is no more accessible
            return "Write characteristic 'Unknown UUID'";
        }
    }
}
