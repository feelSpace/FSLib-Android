package de.feelspace.fslibtest;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.UUID;

import de.feelspace.fslib.BeltCommunicationController;
import de.feelspace.fslib.BeltConnectionInterface;
import de.feelspace.fslib.BeltConnectionState;
import de.feelspace.fslib.GattConnectionState;
import de.feelspace.fslib.GattController;
import de.feelspace.fslib.NavigationEventListener;

public class AdvancedBeltController implements GattController.GattEventListener {
    // Debug
    @SuppressWarnings("unused")
    private static final String DEBUG_TAG = "FeelSpace-Debug";
    @SuppressWarnings("unused")
    private static final boolean DEBUG = true;

    private @NonNull BeltConnectionInterface connectionInterface;
    private @NonNull GattController gattController;

    private final ArrayList<AdvancedBeltListener> listeners = new ArrayList<>();

    // Notification state
    private boolean rawSensorNotificationsEnabled = false;
    private boolean debugNotificationsEnabled = false;

    // Sensor calibration
    private Float[] magOffsets = {null, null, null};
    private Float[] magGains = {null, null, null};
    private Float magCalibError = null;
    private Float[] gyroOffsets = {null, null, null};

    // Raw sensor notification sequence
    private Integer sensorSequence = null;

    public AdvancedBeltController(@NonNull BeltConnectionInterface connectionInterface) {
        this.connectionInterface = connectionInterface;
        gattController = ((BeltCommunicationController)
                connectionInterface.getCommunicationInterface()).getGattController();
        gattController.addGattEventListener(this);
    }

    public void addAdvancedBeltListener(AdvancedBeltListener listener) {
        synchronized (this) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    public void removeAdvancedBeltListener(AdvancedBeltListener listener) {
        synchronized (this) {
            listeners.remove(listener);
        }
    }

    public void setRawSensorNotificationsEnable(boolean enable) {
        // TODO
    }

    public boolean areRawSensorNotificationsEnabled() {
        if (connectionInterface.getState() == BeltConnectionState.STATE_CONNECTED) {
            return rawSensorNotificationsEnabled;
        } else {
            return false;
        }
    }

    public void setDebugNotificationsEnable(boolean enable) {
        if (connectionInterface.getState() == BeltConnectionState.STATE_CONNECTED) {
            BluetoothGattCharacteristic debugChar = getCharacteristic(
                    BeltCommunicationController.DEBUG_SERVICE_UUID,
                    BeltCommunicationController.DEBUG_OUTPUT_CHAR_UUID);
            if (debugChar == null) {
                Log.e(DEBUG_TAG, "AdvancedBeltController: No debug output char!");
            } else {
                if (!gattController.setCharacteristicNotification(debugChar, enable)) {
                    Log.e(DEBUG_TAG, "AdvancedBeltController: Unable to set debug output char notification state!");
                }
            }
        }
    }

    public boolean areDebugNotificationsEnabled() {
        if (connectionInterface.getState() == BeltConnectionState.STATE_CONNECTED) {
            return debugNotificationsEnabled;
        } else {
            return false;
        }
    }

    public void getSensorCalibration() {
        // TODO
    }

    private @Nullable BluetoothGattCharacteristic getCharacteristic(UUID serviceUuid, UUID charUuid) {
        BluetoothGatt gattServer = gattController.getBluetoothGatt();
        if (gattServer == null) {
            return null;
        }
        BluetoothGattService service = gattServer.getService(serviceUuid);
        if (service == null) {
            return null;
        }
        return service.getCharacteristic(charUuid);
    }

    // MARK: Implementation of `GattEventListener`

    @Override
    public void onGattConnectionStateChange(GattConnectionState state) {

    }

    @Override
    public void onGattConnectionFailed() {

    }

    @Override
    public void onGattConnectionLost() {

    }

    @Override
    public void onCharacteristicNotificationSet(@NonNull BluetoothGattCharacteristic characteristic, boolean enable, boolean success) {
        if (characteristic.getUuid() == BeltCommunicationController.DEBUG_OUTPUT_CHAR_UUID) {
            // TODO Notify listener
        } else if (characteristic.getUuid() == BeltCommunicationController.SENSOR_PARAM_NOTIFICATION_CHAR_UUID) {
            if (enable) {
                // TODO Start raw sensor notification with request if enabled
            } else {
                // TODO Notify notification stopped
            }
        }

    }

    @Override
    public void onCharacteristicRead(@NonNull BluetoothGattCharacteristic characteristic, @Nullable byte[] value, boolean success) {

    }

    @Override
    public void onCharacteristicWrite(@NonNull BluetoothGattCharacteristic characteristic, @Nullable byte[] value, boolean success) {

    }

    @Override
    public void onCharacteristicChanged(@Nullable BluetoothGattCharacteristic characteristic, @Nullable byte[] value) {

    }

    @Override
    public void onRequestCompleted(@Nullable BluetoothGattCharacteristic notifiedCharacteristic, @Nullable byte[] notifiedValue) {

    }
}
