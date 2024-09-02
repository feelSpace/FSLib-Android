/*
 * Copyright (c) 2017-2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */
package de.feelspace.fslib;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

import androidx.annotation.NonNull;

import static de.feelspace.fslib.GattOperationState.STATE_FAILED;
import static de.feelspace.fslib.GattOperationState.STATE_STARTED;
import static de.feelspace.fslib.GattOperationState.STATE_SUCCESS;

/**
 * Implementation of BLE operation to enable or disable notifications.
 */
class GattOperationSetNotificationIndication extends GattOperation {

    /** The target characteristic of the operation. */
    private BluetoothGattDescriptor descriptor;

    /** Parameters to be set. */
    private boolean enableNotification;
    private boolean enableIndication;

    /**
     * Creates a operation.
     *
     * @param gatt The GATT service.
     * @param descriptor The descriptor.
     * @param enableNotification <code>true</code> to enable notifications, <code>false</code> to
     *                           disable them.
     * @param enableIndication <code>true</code> to enable indication, <code>false</code> to disable
     *                         them.
     */
    GattOperationSetNotificationIndication(@NonNull BluetoothGatt gatt,
                                           @NonNull BluetoothGattDescriptor descriptor,
                                           boolean enableNotification,
                                           boolean enableIndication) {
        super(gatt, null);
        this.descriptor = descriptor;
        this.enableNotification = enableNotification;
        this.enableIndication = enableIndication;
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void start() {
        setState(STATE_STARTED);
        try {
            if (gatt.setCharacteristicNotification(descriptor.getCharacteristic(),
                    enableNotification || enableIndication)) {
                if (enableNotification && enableIndication) {
                    descriptor.setValue(new byte[] {0x03, 0x00});
                } else if (enableIndication) {
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                } else if (enableNotification) {
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                } else {
                    descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                }
                if (!gatt.writeDescriptor(descriptor)) {
                    setState(STATE_FAILED);
                }
            } else {
                setState(STATE_FAILED);
            }
        } catch (Exception e) {
            // Operation failed
            setState(STATE_FAILED);
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                 int status) {
        if (getState() == STATE_STARTED && descriptor == this.descriptor) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Operation callback
                setState(STATE_SUCCESS);
            } else {
                // Operation callback
                setState(STATE_FAILED);
            }
        }
    }

    /**
     * Returns the enable value.
     * @return the enable value.
     */
    protected boolean getValue() {
        return (enableIndication || enableNotification);
    }

    /**
     * Returns the characteristic targeted by the operation.
     * @return the characteristic targeted by the operation.
     */
    protected BluetoothGattCharacteristic getCharacteristic() {
        return descriptor.getCharacteristic();
    }

    @Override
    public String toString() {
        try {
            return "Set notification on characteristic '"+
                    descriptor.getCharacteristic().getUuid().toString()+"'";
        } catch (Exception e) {
            return "Set notification on characteristic 'Unknown UUID'";
        }
    }
}
