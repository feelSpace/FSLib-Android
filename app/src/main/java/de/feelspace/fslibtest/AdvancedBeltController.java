package de.feelspace.fslibtest;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import de.feelspace.fslib.BeltCommunicationController;
import de.feelspace.fslib.BeltConnectionInterface;
import de.feelspace.fslib.BeltConnectionState;
import de.feelspace.fslib.GattConnectionState;
import de.feelspace.fslib.GattController;
import de.feelspace.fslib.NavigationEventListener;
import de.feelspace.fslib.NavigationState;

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
    private final Float[] magOffsets = {null, null, null};
    private final Float[] magGains = {null, null, null};
    private Float magError = null;
    private final Float[] gyroOffsets = {null, null, null};
    private Integer gyroStatus = null;

    // Raw sensor notification sequence
    private Integer sensorSequence = null;

    // Request IDs
    private final static int START_SENSOR_NOTIFICATION_REQUEST_ID = 1;

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
        // Enable notifications, and send command for starting notifications
        if (connectionInterface.getState() == BeltConnectionState.STATE_CONNECTED) {
            BluetoothGattCharacteristic sensorNotifChar = getCharacteristic(
                    BeltCommunicationController.SENSOR_SERVICE_UUID,
                    BeltCommunicationController.SENSOR_PARAM_NOTIFICATION_CHAR_UUID);
            BluetoothGattCharacteristic sensorRequestChar = getCharacteristic(
                    BeltCommunicationController.SENSOR_SERVICE_UUID,
                    BeltCommunicationController.SENSOR_PARAM_REQUEST_CHAR_UUID);

            if (sensorNotifChar == null || sensorRequestChar == null) {
                Log.e(DEBUG_TAG, "AdvancedBeltController: Missing sensor characteristic!");
            } else {
                // Enable/Disable notification
                if (gattController.setCharacteristicNotification(sensorNotifChar, enable)) {
                    if (enable) {
                        // Command to start notifications
                        if (!gattController.request(
                                sensorRequestChar,
                                sensorNotifChar,
                                new byte[] {0x01, 0x01},
                                new Byte[] {0x01},
                                START_SENSOR_NOTIFICATION_REQUEST_ID
                        )) {
                            Log.e(DEBUG_TAG, "AdvancedBeltController: Unable to send sensor command!");
                        }
                    }
                } else {
                    Log.e(DEBUG_TAG, "AdvancedBeltController: Unable to set sensor notification state!");
                }
            }
        }
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
        boolean ret = true;
        // TODO
    }

    private @Nullable BluetoothGattCharacteristic getCharacteristic(UUID serviceUuid,
                                                                    UUID charUuid) {
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
        if (state == GattConnectionState.GATT_CONNECTED) {
            setDebugNotificationsEnable(true);
            getSensorCalibration();
        } else {
            // Reset state
            rawSensorNotificationsEnabled = false;
            debugNotificationsEnabled = false;
            magOffsets[0] = null; magOffsets[1] = null; magOffsets[2] = null;
            magGains[0] = null; magGains[1] = null; magGains[2] = null;
            magError = null;
            gyroOffsets[0] = null; gyroOffsets[1] = null; gyroOffsets[2] = null;
            gyroStatus = null;
            sensorSequence = null;
        }
    }

    @Override
    public void onGattConnectionFailed() {

    }

    @Override
    public void onGattConnectionLost() {

    }

    @Override
    public void onCharacteristicNotificationSet(@NonNull BluetoothGattCharacteristic characteristic,
                                                boolean enable, boolean success) {
        if (characteristic.getUuid() == BeltCommunicationController.DEBUG_OUTPUT_CHAR_UUID) {
            // Set value
            debugNotificationsEnabled = enable;
            // Notify listeners
            ArrayList<AdvancedBeltListener> targets;
            synchronized (this) {
                if (listeners.isEmpty()) {
                    return;
                }
                targets = new ArrayList<>(listeners);
            }
            for (AdvancedBeltListener l: targets) {
                l.onDebugNotificationsStateChanged(enable);
            }

        } else if (characteristic.getUuid() ==
                BeltCommunicationController.SENSOR_PARAM_NOTIFICATION_CHAR_UUID) {
            if (!enable) { // NOTE: Code for starting notification in `onRequestCompleted`
                // Set value
                rawSensorNotificationsEnabled = false;
                // Notify listeners
                ArrayList<AdvancedBeltListener> targets;
                synchronized (this) {
                    if (listeners.isEmpty()) {
                        return;
                    }
                    targets = new ArrayList<>(listeners);
                }
                for (AdvancedBeltListener l: targets) {
                    l.onRawSensorNotificationsStateChanged(false);
                }
            }
        }

    }

    @Override
    public void onCharacteristicRead(@NonNull BluetoothGattCharacteristic characteristic,
                                     @Nullable byte[] value, boolean success) {

    }

    @Override
    public void onCharacteristicWrite(@NonNull BluetoothGattCharacteristic characteristic,
                                      @Nullable byte[] value, boolean success) {

    }

    @Override
    public void onCharacteristicChanged(@Nullable BluetoothGattCharacteristic characteristic,
                                        @Nullable byte[] value) {
        if (characteristic == null || value == null) {
            return;
        }
        if (characteristic.getUuid() == BeltCommunicationController.DEBUG_OUTPUT_CHAR_UUID) {
            if (value.length >= 5 && value[0] == (byte) 0xA0) {
                // Error notification
                int errorCode = (
                        (((int) value[4]) << 24) |
                        (((int) value[3]) << 16) |
                        (((int) value[2]) << 8) |
                                (0xFF & value[1]));
                // Notify listeners
                ArrayList<AdvancedBeltListener> targets;
                synchronized (this) {
                    if (listeners.isEmpty()) {
                        return;
                    }
                    targets = new ArrayList<>(listeners);
                }
                for (AdvancedBeltListener l: targets) {
                    l.onErrorNotified(errorCode);
                }
            }

        } else if (characteristic.getUuid() ==
                BeltCommunicationController.SENSOR_PARAM_NOTIFICATION_CHAR_UUID) {
            if (value.length >= 2 && value[0] == (byte) 0x01) {
                // Raw sensor record notification
                // Check sequence
                int seq = value[1] & 0xFF; // Unsigned byte
                if (sensorSequence != null && ((sensorSequence + 1) % 256) != seq) {
                    // Error in sequence
                    // Notify listeners
                    ArrayList<AdvancedBeltListener> targets;
                    synchronized (this) {
                        if (listeners.isEmpty()) {
                            return;
                        }
                        targets = new ArrayList<>(listeners);
                    }
                    for (AdvancedBeltListener l: targets) {
                        l.onRawSensorNotificationSequenceError();
                    }
                }
                sensorSequence = seq;
                // Retrieve records
                int records_count = (value.length - 2) / 7;
                if ((value.length - 2) % 7 != 0) {
                    Log.w(DEBUG_TAG, "AdvancedBeltController: Partial raw sensor notification!");
                }
                int[][] records = new int[records_count][4];
                for (int r = 0; r < records_count; r++) {
                    records[r][0] = value[(r * 7) + 2] & 0xFF;
                    records[r][1] = ByteBuffer.wrap(Arrays.copyOfRange(value,
                                    (r * 7) + 3, (r * 7) + 5))
                            .order(ByteOrder.LITTLE_ENDIAN).getInt();
                    records[r][2] = ByteBuffer.wrap(Arrays.copyOfRange(value,
                                    (r * 7) + 5, (r * 7) + 7))
                            .order(ByteOrder.LITTLE_ENDIAN).getInt();
                    records[r][3] = ByteBuffer.wrap(Arrays.copyOfRange(value,
                                    (r * 7) + 7, (r * 7) + 9))
                            .order(ByteOrder.LITTLE_ENDIAN).getInt();
                }
                // Notify listeners
                ArrayList<AdvancedBeltListener> targets;
                synchronized (this) {
                    if (listeners.isEmpty()) {
                        return;
                    }
                    targets = new ArrayList<>(listeners);
                }
                for (AdvancedBeltListener l: targets) {
                    l.onRawSensorRecordNotified(records);
                }
            }
        }
    }

    @Override
    public void onRequestCompleted(int requestId, @Nullable byte[] notifiedValue, boolean success) {
        if (requestId == START_SENSOR_NOTIFICATION_REQUEST_ID && success) {
            // Set value
            rawSensorNotificationsEnabled = true;
            // Reset sequence
            sensorSequence = null;
            // Notify listeners
            ArrayList<AdvancedBeltListener> targets;
            synchronized (this) {
                if (listeners.isEmpty()) {
                    return;
                }
                targets = new ArrayList<>(listeners);
            }
            for (AdvancedBeltListener l: targets) {
                l.onRawSensorNotificationsStateChanged(true);
            }
        }
    }
}
