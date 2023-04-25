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
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 * Implementation of the communication interface.
 */
public class BeltCommunicationController implements BeltCommunicationInterface,
        GattController.GattEventListener {

    // Debug
    @SuppressWarnings("unused")
    private static final String DEBUG_TAG = "FeelSpace-Debug";
    @SuppressWarnings("unused")
    private static final boolean DEBUG = true;

    /**
     * The GATT controller.
     */
    private @NonNull GattController gattController;

    /**
     * Handshake callback.
     */
    private @Nullable HandshakeCallback handshakeCallback;

    /**
     * Flag for ongoing handshake.
     */
    private boolean handshakeStarted = false;

    /** Service UUID advertised by a belt. */
    protected static final UUID ADVERTISED_SERVICE_UUID =
            UUID.fromString("65333333-A115-11E2-9E9A-0800200CA100");

    /** Belt control service UUID. */
    // TODO: This should be changed to private
    public static final UUID BELT_CONTROL_SERVICE_UUID =
            UUID.fromString("0000FE51-0000-1000-8000-00805F9B34FB");

    /** Firmware information characteristic UUID. */
    private static final UUID FIRMWARE_INFO_CHAR_UUID =
            UUID.fromString("0000FE01-0000-1000-8000-00805F9B34FB");
    private @Nullable BluetoothGattCharacteristic firmwareInfoChar;

    /** Keep alive characteristic UUID. */
    private static final UUID KEEP_ALIVE_CHAR_UUID =
            UUID.fromString("0000FE02-0000-1000-8000-00805F9B34FB");
    private @Nullable BluetoothGattCharacteristic keepAliveChar;
    private boolean keepAliveNotificationsActive = false;

    /** Vibration command characteristic UUID. */
    private static final UUID VIBRATION_COMMAND_CHAR_UUID =
            UUID.fromString("0000FE03-0000-1000-8000-00805F9B34FB");
    private @Nullable BluetoothGattCharacteristic vibrationCommandChar;

    /** Button press notification characteristic UUID. */
    private static final UUID BUTTON_PRESS_NOTIFICATION_CHAR_UUID =
            UUID.fromString("0000FE04-0000-1000-8000-00805F9B34FB");
    private @Nullable BluetoothGattCharacteristic buttonPressNotificationChar;
    private boolean buttonPressNotificationsActive = false;

    /** Parameter request characteristic UUID. */
    private static final UUID PARAMETER_REQUEST_CHAR_UUID =
            UUID.fromString("0000FE05-0000-1000-8000-00805F9B34FB");
    private @Nullable BluetoothGattCharacteristic parameterRequestChar;

    /** Parameter notification characteristic UUID. */
    private static final UUID PARAMETER_NOTIFICATION_CHAR_UUID =
            UUID.fromString("0000FE06-0000-1000-8000-00805F9B34FB");
    private @Nullable BluetoothGattCharacteristic parameterNotificationChar;
    private boolean parameterNotificationsActive = false;

    /** Buzzer-LED command characteristic UUID */
    private static final UUID BUZZER_LED_COMMAND_CHAR_UUID =
            UUID.fromString("0000FE07-0000-1000-8000-00805F9B34FB");
    private @Nullable BluetoothGattCharacteristic buzzerLedCommandChar;

    /** Battery level characteristic UUID. */
    private static final UUID BATTERY_STATUS_CHAR_UUID =
            UUID.fromString("0000FE09-0000-1000-8000-00805F9B34FB");
    private @Nullable BluetoothGattCharacteristic batteryStatusChar;
    private boolean batteryStatusNotificationsActive = false;

    /** Belt sensor service UUID. */
    // TODO: This should be changed to private
    public static final UUID SENSOR_SERVICE_UUID =
            UUID.fromString("0000FE52-0000-1000-8000-00805F9B34FB");

    /** Sensor parameter request characteristic UUID. */
    public static final UUID SENSOR_PARAM_REQUEST_CHAR_UUID =
            UUID.fromString("0000FE0A-0000-1000-8000-00805F9B34FB");
    private @Nullable BluetoothGattCharacteristic sensorParamRequestChar;

    /** Sensor parameter notification characteristic UUID. */
    public static final UUID SENSOR_PARAM_NOTIFICATION_CHAR_UUID =
            UUID.fromString("0000FE0B-0000-1000-8000-00805F9B34FB");
    private @Nullable BluetoothGattCharacteristic sensorParamNotificationChar;
    private boolean sensorParamNotificationsActive = false;

    /** Orientation data characteristic UUID. */
    private static final UUID ORIENTATION_DATA_CHAR_UUID =
            UUID.fromString("0000FE0C-0000-1000-8000-00805F9B34FB");
    private @Nullable BluetoothGattCharacteristic orientationDataChar;
    private boolean orientationDataNotificationsActive = false;

    /** Debug service */
    public static final UUID DEBUG_SERVICE_UUID =
            UUID.fromString("0000FE53-0000-1000-8000-00805F9B34FB");
    public static final UUID DEBUG_INPUT_CHAR_UUID =
            UUID.fromString("0000FE13-0000-1000-8000-00805F9B34FB");
    private @Nullable BluetoothGattCharacteristic debugInputChar;
    public static final UUID DEBUG_OUTPUT_CHAR_UUID =
            UUID.fromString("0000FE14-0000-1000-8000-00805F9B34FB");
    private @Nullable BluetoothGattCharacteristic debugOutputChar;
    private boolean debugOutputNotificationsActive = false;
    /**
     * Local values of belt state and parameters.
     */
    private @NonNull BeltMode beltMode = BeltMode.UNKNOWN;
    private Integer defaultIntensity = null;
    private Integer beltFirmwareVersion = null;
    private BeltBatteryStatus beltBatteryStatus = null;
    private BeltOrientation beltOrientation = null;
    private @NonNull HashMap<BeltParameter, Object> parameterValues = new HashMap<>();

    /* Static packet */

    // Keep-alive acknowledgment packet
    private static final byte[] KEEP_ALIVE_PACKET = new byte[] {0x00, 0x00};

    // Request belt mode packet
    private static final byte[] REQUEST_BELT_MODE_PACKET = new byte[] {0x01, 0x01};

    // Request default intensity packet
    private static final byte[] REQUEST_DEFAULT_INTENSITY_PACKET = new byte[] {0x01, 0x02};

    /** Command listeners */
    private @NonNull ArrayList<BeltCommandListener> commandListeners = new ArrayList<>();

    /** Communication listeners */
    private @NonNull ArrayList<BeltCommunicationListener> communicationListeners =
            new ArrayList<>();

    /**
     * Constructor.
     * @param gattController The GATT controller to communicate with the belt.
     */
    BeltCommunicationController(@NonNull GattController gattController) {
        this.gattController = gattController;
        gattController.addGattEventListener(this);
    }

    /**
     * Returns the GATT controller.
     * <p>
     * This is only for development and debug purposes.
     * </p>
     * @return the GATT controller.
     */
    public GattController getGattController() {
        return gattController;
    }

    /**
     * Sends the command for handshake.
     * @param handshakeCallback the callback for handshake completion.
     */
    protected void startHandshake(@NonNull HandshakeCallback handshakeCallback) {
        this.handshakeCallback = handshakeCallback;
        handshakeStarted = true;
        // First retrieve characteristics
        if (!retrieveGattCharacteristics()) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: Cannot start handshake " +
                    "with incomplete GATT profile.");
            handshakeStarted = false;
            this.handshakeCallback.onHandshakeCompleted(false);
            return;
        }
        // Handshake with notification registrations and parameter requests
        // Notification registrations:
        // 1st -> KeepAlive
        // 2nd -> ButtonPress
        // 3rd -> ParameterNotification
        // 4th -> BatteryStatus
        // Initial parameter requests:
        // 1st -> Mode
        // 2nd -> Intensity
        // 3rd -> Firmware (read characteristic, no notification)
        // Handshake finished when firmware version is read
        if (gattController.getConnectionState() != GattConnectionState.GATT_CONNECTED) {
            handshakeStarted = false;
            this.handshakeCallback.onHandshakeCompleted(false);
            return;
        }
        // Notifications
        if (!gattController.setCharacteristicNotification(keepAliveChar, true)) {
            handshakeStarted = false;
            this.handshakeCallback.onHandshakeCompleted(false);
            return;
        }
        if (!gattController.setCharacteristicNotification(
                buttonPressNotificationChar, true)) {
            handshakeStarted = false;
            this.handshakeCallback.onHandshakeCompleted(false);
            return;
        }
        if (!gattController.setCharacteristicNotification(parameterNotificationChar,
                true)) {
            handshakeStarted = false;
            this.handshakeCallback.onHandshakeCompleted(false);
            return;
        }
        if (!gattController.setCharacteristicNotification(batteryStatusChar, true)) {
            handshakeStarted = false;
            this.handshakeCallback.onHandshakeCompleted(false);
            return;
        }
        // Parameter requests
        if(!requestBeltMode()) {
            handshakeStarted = false;
            this.handshakeCallback.onHandshakeCompleted(false);
            return;
        }
        if (!requestDefaultIntensity()) {
            handshakeStarted = false;
            this.handshakeCallback.onHandshakeCompleted(false);
            return;
        }
        if (!requestBeltFirmwareVersion()) {
            handshakeStarted = false;
            this.handshakeCallback.onHandshakeCompleted(false);
        }
    }

    /**
     * Callback interface for the completion of the handshake procedure.
     */
    interface HandshakeCallback {

        /**
         * Callback method for the completion of the handshake.
         * @param success <code>true</code> if the handshake is successful.
         */
        void onHandshakeCompleted(boolean success);
    }

    /**
     * Retrieves the GATT characteristics from newly connected GATT server.
     * @return <code>true</code> if all characteristics were retrieved, <code>false</code> if
     * some characteristics or services are missing.
     */
    private boolean retrieveGattCharacteristics() {
        BluetoothGatt gattServer = gattController.getGattServer();
        if (gattServer == null) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: No GATT server after " +
                    "connection event.");
            return false;
        }
        BluetoothGattService controlService = gattServer.getService(
                BELT_CONTROL_SERVICE_UUID);
        if (controlService == null) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: No control service found " +
                    "in GATT profile.");
            return false;
        }
        firmwareInfoChar = controlService.getCharacteristic(FIRMWARE_INFO_CHAR_UUID);
        keepAliveChar = controlService.getCharacteristic(KEEP_ALIVE_CHAR_UUID);
        vibrationCommandChar = controlService.getCharacteristic(
                VIBRATION_COMMAND_CHAR_UUID);
        buttonPressNotificationChar = controlService.getCharacteristic(
                BUTTON_PRESS_NOTIFICATION_CHAR_UUID);
        parameterRequestChar = controlService.getCharacteristic(
                PARAMETER_REQUEST_CHAR_UUID);
        parameterNotificationChar = controlService.getCharacteristic(
                PARAMETER_NOTIFICATION_CHAR_UUID);
        buzzerLedCommandChar = controlService.getCharacteristic(
                BUZZER_LED_COMMAND_CHAR_UUID);
        batteryStatusChar = controlService.getCharacteristic(BATTERY_STATUS_CHAR_UUID);
        if (firmwareInfoChar == null ||
                keepAliveChar == null ||
                vibrationCommandChar == null ||
                buttonPressNotificationChar == null ||
                parameterRequestChar == null ||
                parameterNotificationChar == null ||
                buzzerLedCommandChar == null ||
                batteryStatusChar == null) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: Missing characteristic " +
                    "in GATT profile.");
            return false;
        }
        BluetoothGattService sensorService = gattServer.getService(SENSOR_SERVICE_UUID);
        if (sensorService == null) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: No sensor service found " +
                    "in GATT profile.");
            return false;
        }
        sensorParamRequestChar = sensorService.getCharacteristic(
                SENSOR_PARAM_REQUEST_CHAR_UUID);
        sensorParamNotificationChar = sensorService.getCharacteristic(
                SENSOR_PARAM_NOTIFICATION_CHAR_UUID);
        orientationDataChar = sensorService.getCharacteristic(ORIENTATION_DATA_CHAR_UUID);
        if (sensorParamRequestChar == null ||
                sensorParamNotificationChar == null ||
                orientationDataChar == null) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: Missing characteristic " +
                    "in GATT profile.");
            return false;
        }
        BluetoothGattService debugService = gattServer.getService(DEBUG_SERVICE_UUID);
        if (debugService == null) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: No debug service found " +
                    "in GATT profile.");
            /* Not critical */
        } else {
            debugInputChar = debugService.getCharacteristic(DEBUG_INPUT_CHAR_UUID);
            debugOutputChar = debugService.getCharacteristic(DEBUG_OUTPUT_CHAR_UUID);
            if (debugInputChar == null || debugOutputChar == null) {
                /* Not critical */
                Log.e(DEBUG_TAG, "BeltCommunicationController: Missing debug characteristic " +
                        "in GATT profile.");
            }
        }

        return true;
    }

    /**
     * Sends a request to obtain the mode of the belt.
     * @return <code>true</code> if the request has been successfully placed in operation queue.
     */
    private boolean requestBeltMode() {
        return gattController.writeCharacteristic(parameterRequestChar, REQUEST_BELT_MODE_PACKET);
    }

    /**
     * Sends a request to obtain the default vibration intensity.
     * @return <code>true</code> if the request has been successfully placed in operation queue.
     */
    private boolean requestDefaultIntensity() {
        return gattController.writeCharacteristic(parameterRequestChar,
                REQUEST_DEFAULT_INTENSITY_PACKET);
    }

    /**
     * Sends a request to obtain the firmware version of the belt.
     * @return <code>true</code> if the request has been successfully placed in operation queue.
     */
    private boolean requestBeltFirmwareVersion() {
        return gattController.readCharacteristic(firmwareInfoChar);
    }

    @Override
    public Integer getFirmwareVersion() {
        return beltFirmwareVersion;
    }

    @Override
    public boolean requestParameterValue(BeltParameter beltParameter) {
        if (beltParameter == null) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: Cannot send a parameter request " +
                    "for null parameter.");
            return false;
        }
        if (gattController.getConnectionState() != GattConnectionState.GATT_CONNECTED ||
                parameterRequestChar == null) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: Cannot send a parameter request " +
                    "when disconnected.");
            return false;
        }
        byte[] packet = new byte[] {
                (byte) 0x10,                // Command
                (byte) 0x01,                // Parameter count
                beltParameter.getValue()
        };
        return gattController.writeCharacteristic(parameterRequestChar, packet);
    }

    @Nullable
    @Override
    public Object getParameterValue(@Nullable BeltParameter beltParameter) {
        return parameterValues.get(beltParameter);
    }

    @Override
    public boolean changeParameterValue(
            BeltParameter beltParameter, Object parameterValue,
            boolean persistent) {
        if (beltParameter == null) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: Cannot send a parameter request " +
                    "for null parameter.");
            return false;
        }
        if (gattController.getConnectionState() != GattConnectionState.GATT_CONNECTED ||
                parameterRequestChar == null) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: Cannot send a parameter request " +
                    "when disconnected.");
            return false;
        }
        byte[] packet = null;
        switch (beltParameter) {

            case HEADING_OFFSET:
                if (!(parameterValue instanceof Integer)) {
                    Log.e(DEBUG_TAG, "BeltCommunicationController: The parameter value " +
                            "for heading offset must be an Integer.");
                    return false;
                }
                int offset = (Integer) parameterValue;
                if (offset < 0 || offset >= 360) {
                    Log.e(DEBUG_TAG, "BeltCommunicationController: The parameter value " +
                            "for heading offset must be in range [0-359].");
                    return false;
                }
                // Create packet
                packet = new byte[] {
                        (byte) 0x11,                // Command
                        (byte) 0x01,                // Parameter ID
                        (byte) ((persistent)?(0x01):(0x00)),  // Store on EEPROM
                        (byte) (offset & 0xFF),     // LSB
                        (byte) ((offset >> 8) & 0xFF)   // MSB
                };
                break;

            case ACCURACY_SIGNAL_STATE:
                // Check value
                if (!(parameterValue instanceof Integer)) {
                    Log.e(DEBUG_TAG, "BeltCommunicationController: The parameter value " +
                            "for accuracy signal state must be an Integer.");
                    return false;
                }
                int signalState = (Integer) parameterValue;
                if (signalState < 0 || signalState > 3) {
                    Log.e(DEBUG_TAG, "BeltCommunicationController: The parameter value " +
                            "for accuracy signal state must be in range [0-3].");
                    return false;
                }
                // Create packet
                packet = new byte[] {
                        (byte) 0x11,                // Command
                        (byte) 0x03,                // Parameter ID
                        (byte) ((persistent)?(0x01):(0x00)),  // Store on EEPROM
                        (byte) signalState          // Value
                };
                break;

        }
        return gattController.writeCharacteristic(parameterRequestChar, packet);
    }

    @Override
    public boolean reset(boolean parameterReset, boolean bluetoothReset, boolean sensorReset) {
        if (gattController.getConnectionState() != GattConnectionState.GATT_CONNECTED ||
                parameterRequestChar == null) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: Cannot send a reset " +
                    "command when not connected.");
            return false;
        }
        byte[] packet = new byte[] {
                (byte) 0x12,                // Command
                (byte) ((parameterReset)?(0x01):(0x00)),    // Reset parameters
                (byte) ((bluetoothReset)?(0x01):(0x00)),    // Reset Bluetooth
                (byte) ((sensorReset)?(0x01):(0x00)),    // Reset sensors
        };
        return gattController.writeCharacteristic(parameterRequestChar, packet);
    }

    @Override
    public boolean playTonePattern(BuzzerTonePattern tonePattern) {
        if (tonePattern == null) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: Cannot send a null tone " +
                    "pattern.");
            return false;
        }
        if (gattController.getConnectionState() != GattConnectionState.GATT_CONNECTED ||
                buzzerLedCommandChar == null) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: Cannot send a tone pattern " +
                    "command when not connected.");
            return false;
        }
        return gattController.writeCharacteristic(buzzerLedCommandChar, tonePattern.getPacket());
    }

    @Override
    public boolean sendPulseCommand(@Nullable PulseCommand command) {
        if (command == null) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: Cannot send a null pulse " +
                    "command.");
            return false;
        }
        if (gattController.getConnectionState() != GattConnectionState.GATT_CONNECTED ||
                vibrationCommandChar == null) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: Cannot send a pulse command " +
                    "when not connected.");
            return false;
        }
        return gattController.writeCharacteristic(vibrationCommandChar, command.getPacket());
    }

    @Override
    public boolean sendChannelConfigurationCommand(@Nullable ChannelConfigurationCommand command) {
        if (command == null) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: Cannot send a null channel " +
                    "configuration command.");
            return false;
        }
        if (gattController.getConnectionState() != GattConnectionState.GATT_CONNECTED ||
                vibrationCommandChar == null) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: Cannot send a pulse command " +
                    "when not connected.");
            return false;
        }
        return gattController.writeCharacteristic(vibrationCommandChar, command.getPacket());
    }

    @Override
    public boolean startSystemSignal(BeltSystemSignal signal) {
        if (signal == null) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: Cannot send a null signal " +
                    "command.");
            return false;
        }
        if (gattController.getConnectionState() != GattConnectionState.GATT_CONNECTED ||
                vibrationCommandChar == null) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: Cannot send a signal command " +
                    "when not connected.");
            return false;
        }
        byte[] packet = new byte[] {
                (byte) 0x20,        // System signal request
                signal.getValue()
        };
        return gattController.writeCharacteristic(vibrationCommandChar, packet);
    }


    @Override
    public void addCommandListener(@Nullable BeltCommandListener listener) {
        synchronized (this) {
            if (listener != null && !commandListeners.contains(listener)) {
                commandListeners.add(listener);
            }
        }
    }

    @Override
    public void removeCommandListener(@Nullable BeltCommandListener listener) {
        synchronized (this) {
            commandListeners.remove(listener);
        }
    }

    @Override
    public void addCommunicationListener(@Nullable BeltCommunicationListener listener) {
        synchronized (this) {
            if (listener != null && !communicationListeners.contains(listener)) {
                communicationListeners.add(listener);
            }
        }
    }

    @Override
    public void removeCommunicationListener(@Nullable BeltCommunicationListener listener) {
        synchronized (this) {
            communicationListeners.remove(listener);
        }
    }

    /**
     * Sets the belt firmware version.
     */
    private void setFirmwareVersion(@Nullable Integer version) {
        synchronized (this) {
            if (gattController.getConnectionState() != GattConnectionState.GATT_CONNECTED ||
                    version == null) {
                return;
            }
            beltFirmwareVersion = version;
        }
    }

    /**
     * Sets the battery status and notifies listeners that the battery status has been update.
     */
    private void setBatteryStatus(@Nullable BeltBatteryStatus status) {
        ArrayList<BeltCommandListener> targets;
        synchronized (this) {
            beltBatteryStatus = status;
            if (commandListeners.isEmpty() ||
                    gattController.getConnectionState() != GattConnectionState.GATT_CONNECTED ||
                    status == null || handshakeStarted) {
                return;
            }
            targets = new ArrayList<>(commandListeners);
        }
        for (BeltCommandListener l: targets) {
            l.onBeltBatteryStatusUpdated(status);
        }
    }

    /**
     * Sets the belt mode and notifies listeners that the belt mode has changed.
     */
    private void setBeltMode(@NonNull BeltMode mode) {
        ArrayList<BeltCommandListener> targets;
        synchronized (this) {
            if (beltMode == mode) {
                return;
            }
            beltMode = mode;
            if (commandListeners.isEmpty() ||
                    gattController.getConnectionState() != GattConnectionState.GATT_CONNECTED ||
                    handshakeStarted) {
                return;
            }
            targets = new ArrayList<>(commandListeners);
        }
        for (BeltCommandListener l: targets) {
            l.onBeltModeChanged(mode);
        }
    }

    /**
     * Notifies listeners of a button press event and updates the belt mode.
     */
    private void notifyButtonPressed(@NonNull BeltButtonPressEvent event) {
        ArrayList<BeltCommandListener> targets;
        synchronized (this) {
            if (gattController.getConnectionState() != GattConnectionState.GATT_CONNECTED) {
                return;
            }
            beltMode = event.getSubsequentMode();
            if (commandListeners.isEmpty() || handshakeStarted) {
                return;
            }
            targets = new ArrayList<>(commandListeners);
        }
        for (BeltCommandListener l: targets) {
            l.onBeltButtonPressed(event);
        }
    }

    /**
     * Sets the belt default intensity value and notifies listeners that the default intensity
     * has changed.
     */
    private void setDefaultIntensity(@Nullable Integer intensity) {
        ArrayList<BeltCommandListener> targets;
        synchronized (this) {
            defaultIntensity = intensity;
            if (commandListeners.isEmpty() ||
                    gattController.getConnectionState() != GattConnectionState.GATT_CONNECTED ||
                    intensity == null || handshakeStarted) {
                return;
            }
            targets = new ArrayList<>(commandListeners);
        }
        for (BeltCommandListener l: targets) {
            l.onBeltDefaultVibrationIntensityChanged(intensity);
        }
    }

    /**
     * Updates the orientation and notifies listeners that the belt orientation has been updated.
     */
    private void setOrientation(@Nullable BeltOrientation orientation) {
        ArrayList<BeltCommandListener> targets;
        synchronized (this) {
            beltOrientation = orientation;
            if (commandListeners.isEmpty() ||
                    gattController.getConnectionState() != GattConnectionState.GATT_CONNECTED ||
                    orientation == null || handshakeStarted) {
                return;
            }
            targets = new ArrayList<>(commandListeners);
        }
        for (BeltCommandListener l: targets) {
            l.onBeltOrientationUpdated(orientation);
        }
    }

    /**
     * Sets the value of a parameter and notifies listeners of the value.
     *
     * @param parameter The advanced parameter to set.
     * @param value The value of the parameter.
     */
    private void setParameterValue(@NonNull BeltParameter parameter, @NonNull Object value) {
        // Set parameter and notify communication listeners
        ArrayList<BeltCommunicationListener> communicationListenersCopy = null;
        synchronized (this) {
            parameterValues.put(parameter, value);
            if (gattController.getConnectionState() != GattConnectionState.GATT_CONNECTED ||
                    handshakeStarted) {
                return;
            }
            if (!communicationListeners.isEmpty()) {
                communicationListenersCopy = new ArrayList<>(communicationListeners);
            }
        }
        if (communicationListenersCopy != null) {
            for (BeltCommunicationListener l: communicationListenersCopy) {
                l.onBeltParameterValueNotified(parameter, value);
            }
        }
        // Notifies command listeners if parameter is accuracy signal state
        if (parameter == BeltParameter.ACCURACY_SIGNAL_STATE) {
            ArrayList<BeltCommandListener> commandListenersCopy = null;
            synchronized (this) {
                if (!commandListeners.isEmpty()) {
                    commandListenersCopy = new ArrayList<>(commandListeners);
                }
            }
            if (commandListenersCopy != null) {
                try {
                    boolean signalEnabled = ((Integer) value) >= 2;
                    for (BeltCommandListener l: commandListenersCopy) {
                        l.onBeltCompassAccuracySignalStateNotified(signalEnabled);
                    }
                } catch (Exception e) {
                    Log.e(DEBUG_TAG, "BeltCommunicationController: Unexpected parameter " +
                            "object type.", e);
                }
            }
        }
    }


    @Override
    public boolean vibrateAtMagneticBearing(float direction, @Nullable Integer intensity,
                                            @Nullable BeltVibrationSignal vibration,
                                            @Nullable Integer channelIndex,
                                            @Nullable Boolean stopOtherChannels) {
        return vibrateAt(((int)direction), OrientationType.BEARING, intensity, vibration,
                channelIndex, stopOtherChannels);
    }

    @Override
    public boolean vibrateAtAngle(float angle, @Nullable Integer intensity,
                                  @Nullable BeltVibrationSignal vibration,
                                  @Nullable Integer channelIndex,
                                  @Nullable Boolean stopOtherChannels) {
        return vibrateAt((int)angle, OrientationType.ANGLE, intensity, vibration,
                channelIndex, stopOtherChannels);
    }

    @Override
    public boolean vibrateAtPositions(@Nullable int[] positions, @Nullable Integer intensity,
                                      @Nullable BeltVibrationSignal vibration,
                                      @Nullable Integer channelIndex,
                                      @Nullable Boolean stopOtherChannels) {
        return vibrateAt(BinaryUtils.toBinaryMask(positions), OrientationType.BINARY_MASK,
                intensity, vibration, channelIndex, stopOtherChannels);
    }

    /**
     * Sends a command to start the vibration.
     */
    private boolean vibrateAt(
            int orientation, @NonNull OrientationType orientationType, @Nullable Integer intensity,
            @Nullable BeltVibrationSignal vibration, @Nullable Integer channelIndex,
            @Nullable Boolean stopOtherChannels
    ) {
        if (gattController.getConnectionState() != GattConnectionState.GATT_CONNECTED ||
                vibrationCommandChar == null) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: Cannot send a vibration command " +
                    "when not connected.");
            return false;
        }
        // Only directional signal
        if (vibration != null && !vibration.isDirectional()) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: Non-direction signal cannot be " +
                    "used for oriented vibration command.");
            return false;
        }
        // Default intensity if intensity parameter is null
        intensity = (intensity == null)?(BeltCommunicationInterface.DEFAULT_INTENSITY_CODE):
                (intensity);
        // Check intensity range
        if (intensity < 0 || (intensity > 100 && intensity !=
                BeltCommunicationInterface.DEFAULT_INTENSITY_CODE)) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: Intensity value out of range.");
            return false;
        } else if (intensity > 0 && intensity < 5) {
            intensity = 5;
        }
        // Continuous signal if vibration signal parameter is null
        vibration = (vibration==null)?(BeltVibrationSignal.CONTINUOUS):(vibration);
        // Channel index 1 for non-repeated signal and 2 for repeated signal as default
        channelIndex = (channelIndex == null)?((vibration.isRepeated())?(2):(1)):(channelIndex);
        // Don't stop other channel as default
        stopOtherChannels = (stopOtherChannels == null)?(false):(stopOtherChannels);
        // Only temporary signal on channel 0 when not in app mode
        if (beltMode != BeltMode.APP) {
            if (channelIndex != 0 || vibration.isRepeated() || stopOtherChannels) {
                Log.e(DEBUG_TAG, "BeltCommunicationController: The vibration command is " +
                        "invalid outside App mode.");
                return false;
            }
        }
        // Stop vibration if intensity is 0 or binary mask is 0
        if (intensity == 0 ||
                (orientationType == OrientationType.BINARY_MASK && orientation == 0)) {
            if (stopOtherChannels) {
                return stopVibration();
            } else {
                return stopVibration(channelIndex);
            }
        }

        try {
            switch (vibration) {
                case CONTINUOUS:
                    return gattController.writeCharacteristic(vibrationCommandChar,
                            new ChannelConfigurationCommand(
                                    channelIndex,
                                    BeltVibrationPattern.CONTINUOUS,
                                    intensity,
                                    orientationType,
                                    orientation,
                                    0,
                                    500,
                                    0,
                                    false,
                                    stopOtherChannels
                            ).getPacket());
                case NAVIGATION:
                    return gattController.writeCharacteristic(vibrationCommandChar,
                            new ChannelConfigurationCommand(
                                    channelIndex,
                                    BeltVibrationPattern.CONTINUOUS,
                                    intensity,
                                    orientationType,
                                    orientation,
                                    0,
                                    500,
                                    0,
                                    false,
                                    stopOtherChannels
                            ).getPacket());
                case APPROACHING_DESTINATION:
                    return gattController.writeCharacteristic(vibrationCommandChar,
                            new ChannelConfigurationCommand(
                                    channelIndex,
                                    BeltVibrationPattern.SINGLE_SHORT_PULSE,
                                    intensity,
                                    orientationType,
                                    orientation,
                                    0,
                                    500,
                                    0,
                                    false,
                                    stopOtherChannels
                            ).getPacket());
                case TURN_ONGOING:
                    return gattController.writeCharacteristic(vibrationCommandChar,
                            new ChannelConfigurationCommand(
                                    channelIndex,
                                    BeltVibrationPattern.SINGLE_LONG_PULSE,
                                    intensity,
                                    orientationType,
                                    orientation,
                                    0,
                                    750,
                                    0,
                                    false,
                                    stopOtherChannels
                            ).getPacket());
                case DIRECTION_NOTIFICATION:
                    return gattController.writeCharacteristic(vibrationCommandChar,
                            new ChannelConfigurationCommand(
                                    channelIndex,
                                    BeltVibrationPattern.CONTINUOUS,
                                    intensity,
                                    orientationType,
                                    orientation,
                                    1,
                                    1000,
                                    0,
                                    false,
                                    stopOtherChannels
                            ).getPacket());
                case NEXT_WAYPOINT_LONG_DISTANCE:
                    return gattController.writeCharacteristic(vibrationCommandChar,
                            new ChannelConfigurationCommand(
                                    channelIndex,
                                    BeltVibrationPattern.SINGLE_LONG_PULSE,
                                    intensity,
                                    orientationType,
                                    orientation,
                                    0,
                                    3000,
                                    0,
                                    false,
                                    stopOtherChannels
                            ).getPacket());
                case NEXT_WAYPOINT_MEDIUM_DISTANCE:
                    return gattController.writeCharacteristic(vibrationCommandChar,
                            new ChannelConfigurationCommand(
                                    channelIndex,
                                    BeltVibrationPattern.SINGLE_LONG_PULSE,
                                    intensity,
                                    orientationType,
                                    orientation,
                                    0,
                                    1500,
                                    0,
                                    false,
                                    stopOtherChannels
                            ).getPacket());
                case NEXT_WAYPOINT_SHORT_DISTANCE:
                    return gattController.writeCharacteristic(vibrationCommandChar,
                            new ChannelConfigurationCommand(
                                    channelIndex,
                                    BeltVibrationPattern.SINGLE_LONG_PULSE,
                                    intensity,
                                    orientationType,
                                    orientation,
                                    0,
                                    1000,
                                    0,
                                    false,
                                    stopOtherChannels
                            ).getPacket());
                case NEXT_WAYPOINT_AREA_REACHED:
                    return gattController.writeCharacteristic(vibrationCommandChar,
                            new ChannelConfigurationCommand(
                                    channelIndex,
                                    BeltVibrationPattern.SINGLE_LONG_PULSE,
                                    intensity,
                                    orientationType,
                                    orientation,
                                    0,
                                    750,
                                    0,
                                    false,
                                    stopOtherChannels
                            ).getPacket());
                case DESTINATION_REACHED_REPEATED:
                case DESTINATION_REACHED_SINGLE:
                case OPERATION_WARNING:
                case CRITICAL_WARNING:
                case BATTERY_LEVEL:
                    // Non-directional signal, unreachable
                    return false;
            }
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: Illegal argument for " +
                    "vibration command.", e);
            return false;
        }
        // Unreachable
        return false;
    }

    @Override
    public boolean pulseAtMagneticBearing(float direction, int onDurationMs,
                                          int periodMs, @Nullable Integer iterations,
                                          @Nullable Integer intensity,
                                          @Nullable Integer channelIndex,
                                          @Nullable Boolean stopOtherChannels) {
        return pulseAt((int)direction, OrientationType.BEARING, onDurationMs, periodMs, iterations,
                intensity, channelIndex, stopOtherChannels);
    }

    @Override
    public boolean pulseAtAngle(float angle, int onDurationMs, int periodMs,
                                @Nullable Integer iterations, @Nullable Integer intensity,
                                @Nullable Integer channelIndex,
                                @Nullable Boolean stopOtherChannels) {
        return pulseAt((int)angle, OrientationType.ANGLE, onDurationMs, periodMs, iterations,
                intensity, channelIndex, stopOtherChannels);
    }

    @Override
    public boolean pulseAtPositions(int[] positions, int onDurationMs, int periodMs,
                                    @Nullable Integer iterations, @Nullable Integer intensity,
                                    @Nullable Integer channelIndex,
                                    @Nullable Boolean stopOtherChannels) {
        return pulseAt(BinaryUtils.toBinaryMask(positions), OrientationType.BINARY_MASK,
                onDurationMs, periodMs, iterations, intensity, channelIndex, stopOtherChannels);
    }

    /**
     * Starts a pulse.
     */
    private boolean pulseAt(
            int orientation, @NonNull OrientationType orientationType, int onDurationMs,
            int periodMs, @Nullable Integer iterations, @Nullable Integer intensity,
            @Nullable Integer channelIndex, @Nullable Boolean stopOtherChannels) {
        if (gattController.getConnectionState() != GattConnectionState.GATT_CONNECTED ||
                vibrationCommandChar == null) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: Cannot send a pulse command " +
                    "when not connected.");
            return false;
        }
        // Repeated indefinitely as default
        if (iterations == null) {
            iterations = 0;
        }
        // Default intensity if intensity parameter is null
        intensity = (intensity == null)?(BeltCommunicationInterface.DEFAULT_INTENSITY_CODE):
                (intensity);
        // Check intensity range
        if (intensity < 0 || (intensity > 100 && intensity !=
                BeltCommunicationInterface.DEFAULT_INTENSITY_CODE)) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: Intensity value out of range.");
            return false;
        } else if (intensity > 0 && intensity < 5) {
            intensity = 5;
        }
        // Channel index 1 for non-repeated signal and 2 for repeated signal as default
        channelIndex = (channelIndex == null)?((iterations<=0)?(2):(1)):(channelIndex);
        // Don't stop other channel as default
        stopOtherChannels = (stopOtherChannels == null)?(false):(stopOtherChannels);
        // Only temporary signal on channel 0 when not in app mode
        if (beltMode != BeltMode.APP) {
            if (channelIndex != 0 || iterations <= 0 || stopOtherChannels) {
                Log.e(DEBUG_TAG, "BeltCommunicationController: The pulse command is " +
                        "invalid outside App mode.");
                return false;
            }
        }
        // Stop vibration if intensity is 0
        if (intensity == 0) {
            if (stopOtherChannels) {
                return stopVibration();
            } else {
                return stopVibration(channelIndex);
            }
        }

        try {
            return gattController.writeCharacteristic(vibrationCommandChar,
                    new PulseCommand(
                            channelIndex,
                            orientationType,
                            orientation,
                            intensity,
                            onDurationMs,
                            1,
                            iterations,
                            periodMs,
                            periodMs,
                            ResetProgressOption.RESET_PROGRESS_ON_DIFFERENT_PERIOD,
                            false,
                            stopOtherChannels
                    ).getPacket());
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: Illegal argument for " +
                    "vibration command.", e);
            return false;
        }
    }

    @Override
    public boolean signal(@Nullable BeltVibrationSignal vibration, @Nullable Integer intensity,
                          @Nullable Integer channelIndex, @Nullable Boolean stopOtherChannels) {
        if (gattController.getConnectionState() != GattConnectionState.GATT_CONNECTED ||
                vibrationCommandChar == null) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: Cannot send a vibration command " +
                    "when not connected.");
            return false;
        }
        // Only non-directional signal
        if (vibration == null || vibration.isDirectional()) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: Direction signal cannot be " +
                    "used for non-oriented vibration command.");
            return false;
        }
        // Special case of battery signal
        if (vibration == BeltVibrationSignal.BATTERY_LEVEL) {
            if (intensity != null || (channelIndex != null && channelIndex != 0) ||
                    (stopOtherChannels != null && stopOtherChannels)) {
                Log.w(DEBUG_TAG, "BeltCommunicationController: Parameters for battery " +
                        "signal are ignored.");
            }
            return startSystemSignal(BeltSystemSignal.BATTERY_LEVEL);
        }
        // Default intensity if intensity parameter is null
        intensity = (intensity == null)?(BeltCommunicationInterface.DEFAULT_INTENSITY_CODE):
                (intensity);
        // Check intensity range
        if (intensity < 0 || (intensity > 100 &&
                intensity != BeltCommunicationInterface.DEFAULT_INTENSITY_CODE)) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: Intensity value out of range.");
            return false;
        } else if (intensity > 0 && intensity < 5) {
            intensity = 5;
        }
        // Channel index 1 for non-repeated signal and 2 for repeated signal as default
        channelIndex = (channelIndex == null)?((vibration.isRepeated())?(2):(1)):(channelIndex);
        // Don't stop other channel as default
        stopOtherChannels = (stopOtherChannels == null)?(false):(stopOtherChannels);
        // Only temporary signal on channel 0 when not in app mode
        if (beltMode != BeltMode.APP) {
            if (channelIndex != 0 || vibration.isRepeated() || stopOtherChannels) {
                Log.e(DEBUG_TAG, "BeltCommunicationController: The vibration command is " +
                        "invalid outside App mode.");
                return false;
            }
        }
        // Stop vibration if intensity is 0
        if (intensity == 0) {
            if (stopOtherChannels) {
                return stopVibration();
            } else {
                return stopVibration(channelIndex);
            }
        }

        try {
            switch (vibration) {
                case CONTINUOUS:
                case NAVIGATION:
                case APPROACHING_DESTINATION:
                case TURN_ONGOING:
                case DIRECTION_NOTIFICATION:
                case NEXT_WAYPOINT_LONG_DISTANCE:
                case NEXT_WAYPOINT_MEDIUM_DISTANCE:
                case NEXT_WAYPOINT_SHORT_DISTANCE:
                case NEXT_WAYPOINT_AREA_REACHED:
                    // Directional signals, unreachable
                    return false;
                case DESTINATION_REACHED_REPEATED:
                    return gattController.writeCharacteristic(vibrationCommandChar,
                            new ChannelConfigurationCommand(
                                    channelIndex,
                                    BeltVibrationPattern.DESTINATION_REACHED,
                                    intensity,
                                    OrientationType.VIBROMOTOR_INDEX,
                                    0,
                                    0,
                                    5000,
                                    0,
                                    false,
                                    stopOtherChannels
                            ).getPacket());
                case DESTINATION_REACHED_SINGLE:
                    return gattController.writeCharacteristic(vibrationCommandChar,
                            new ChannelConfigurationCommand(
                                    channelIndex,
                                    BeltVibrationPattern.DESTINATION_REACHED,
                                    intensity,
                                    OrientationType.VIBROMOTOR_INDEX,
                                    0,
                                    1,
                                    2500,
                                    0,
                                    true,
                                    stopOtherChannels
                            ).getPacket());
                case OPERATION_WARNING:
                    // Note: exclusive channel is true because firmware 43 only support 4
                    // simultaneous vibration.
                    return gattController.writeCharacteristic(vibrationCommandChar,
                            new ChannelConfigurationCommand(
                                    channelIndex,
                                    BeltVibrationPattern.WARNING,
                                    intensity,
                                    OrientationType.VIBROMOTOR_INDEX,
                                    0,
                                    2,
                                    500,
                                    0,
                                    true,
                                    stopOtherChannels
                            ).getPacket());
                case CRITICAL_WARNING:
                    // Note: exclusive channel is true because firmware 43 only support 4
                    // simultaneous vibration.
                    return gattController.writeCharacteristic(vibrationCommandChar,
                            new ChannelConfigurationCommand(
                                    channelIndex,
                                    BeltVibrationPattern.SINGLE_LONG_PULSE,
                                    intensity,
                                    OrientationType.BINARY_MASK,
                                    0b0001000100010001,
                                    3,
                                    700,
                                    0,
                                    true,
                                    stopOtherChannels
                            ).getPacket());
                case BATTERY_LEVEL:
                    // System signal, unreachable
                    return false;
            }
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: Illegal argument for " +
                    "vibration command.", e);
            return false;
        }
        // Unreachable
        return false;
    }

    @Override
    public boolean stopVibration(int... channelIndex) {
        if (gattController.getConnectionState() != GattConnectionState.GATT_CONNECTED ||
                vibrationCommandChar == null) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: Cannot send a stop command " +
                    "when not connected.");
            return false;
        }
        if (beltMode != BeltMode.APP) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: The stop vibration command is " +
                    "invalid outside App mode.");
            return false;
        }
        if (channelIndex == null || channelIndex.length == 0) {
            // Stop all channels
            byte[] packet = new byte[] {
                    (byte) 0x30,
                    (byte) 0xFF     // Stop all channels
            };
            return gattController.writeCharacteristic(vibrationCommandChar, packet);
        } else {
            // Stop specific channels (up to 6)
            boolean success = true;
            byte[] packet = new byte[channelIndex.length+1];
            packet[0] = (byte) 0x30;                        // Stop channel command ID
            for (int channel: channelIndex) {
                if (channel < 0 || channel > 6) {
                    Log.e(DEBUG_TAG, "BeltCommunicationController: Channel index out of " +
                            "range for stop command.");
                    success = false;
                } else {
                    packet[1] = (byte) channel;
                    success &= gattController.writeCharacteristic(vibrationCommandChar, packet);
                }
            }
            return success;
        }
    }

    @Override
    public boolean changeMode(BeltMode mode) {
        if (mode == null) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: Cannot change the mode to null.");
            return false;
        }
        if (gattController.getConnectionState() != GattConnectionState.GATT_CONNECTED ||
                parameterRequestChar == null) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: Cannot change the mode " +
                    "when not connected.");
            return false;
        }
        if (mode == BeltMode.UNKNOWN) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: Cannot change the mode " +
                    "to unknown.");
            return false;
        }
        byte[] packet = new byte[] {
                (byte) 0x01,            // 1 parameter
                (byte) 0x81,            // Change mode parameter
                mode.getValue(),        // Value to set
                (byte) 0x00};
        return gattController.writeCharacteristic(parameterRequestChar, packet);
    }

    @NonNull
    @Override
    public BeltMode getMode() {
        return beltMode;
    }

    @Override
    public boolean changeDefaultVibrationIntensity(int intensity, boolean vibrationFeedback) {
        if (gattController.getConnectionState() != GattConnectionState.GATT_CONNECTED ||
                parameterRequestChar == null) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: Cannot change default intensity " +
                    "when not connected.");
            return false;
        }
        if (intensity < 5 || intensity > 100) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: Default intensity must be " +
                    "in range [5-100].");
            return false;
        }
        byte[] packet = new byte[] {
                (byte) 0x01,            // 1 parameter
                (byte) 0x82,            // Change default intensity
                (byte) (intensity & 0xFF),        // Value to set
                (byte) 0x00,
                (byte) ((vibrationFeedback)?(0x01):(0x00))
        };
        return gattController.writeCharacteristic(parameterRequestChar, packet);
    }

    @Nullable
    @Override
    public Integer getDefaultVibrationIntensity() {
        return defaultIntensity;
    }

    @Nullable
    @Override
    public BeltBatteryStatus getBatteryStatus() {
        return beltBatteryStatus;
    }

    @Nullable
    @Override
    public BeltOrientation getOrientation() {
        return beltOrientation;
    }

    @Override
    public boolean setOrientationNotificationsActive(boolean active) {
        return gattController.setCharacteristicNotification(orientationDataChar, true);
    }

    @Override
    public boolean areOrientationNotificationsActive() {
        return orientationDataNotificationsActive;
    }

    @Override
    public boolean playSound(BeltSound sound) {
        if (sound == null) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: Cannot start null sound.");
            return false;
        }
        try {
            return gattController.writeCharacteristic(buzzerLedCommandChar,
                    sound.getPacket());
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "BeltCommunicationController: Illegal sound argument.");
            return false;
        }
    }

    @Override
    public boolean changeCompassAccuracySignalState(boolean enableSignal, boolean persistent) {
        return changeParameterValue(
                BeltParameter.ACCURACY_SIGNAL_STATE,
                (enableSignal)?(3):(0),
                persistent);
    }

    @Override
    public boolean requestCompassAccuracySignalState() {
        return requestParameterValue(BeltParameter.ACCURACY_SIGNAL_STATE);
    }

    @Override
    public void onGattConnectionStateChange(GattConnectionState state) {
        switch (state) {
            case GATT_DISCONNECTED:
                // Clear handshake flag
                handshakeStarted = false;
                // Clear parameters values
                beltMode = BeltMode.UNKNOWN;
                defaultIntensity = null;
                beltFirmwareVersion = null;
                beltBatteryStatus = null;
                beltOrientation = null;
                parameterValues.clear();
                // Clear GATT references
                firmwareInfoChar = null;
                keepAliveChar = null;
                vibrationCommandChar = null;
                buttonPressNotificationChar = null;
                parameterRequestChar = null;
                parameterNotificationChar = null;
                buzzerLedCommandChar = null;
                batteryStatusChar = null;
                sensorParamRequestChar = null;
                sensorParamNotificationChar = null;
                orientationDataChar = null;
                keepAliveNotificationsActive = false;
                buttonPressNotificationsActive = false;
                parameterNotificationsActive = false;
                batteryStatusNotificationsActive = false;
                sensorParamNotificationsActive = false;
                orientationDataNotificationsActive = false;
                debugInputChar = null;
                debugOutputChar = null;
                debugOutputNotificationsActive = false;
                break;
            case GATT_CONNECTING:
                break;
            case GATT_DISCOVERING_SERVICES:
                break;
            case GATT_CONNECTED:
                // Note: Characteristics are retrieved when handshake is started (because handshake
                // may be started before the GATT connection state changed).
                break;
            case GATT_RECONNECTING:
                // Clear handshake flag
                handshakeStarted = false;
                // Clear parameters
                beltMode = BeltMode.UNKNOWN;
                defaultIntensity = null;
                beltFirmwareVersion = null;
                beltBatteryStatus = null;
                beltOrientation = null;
                parameterValues.clear();
                break;
        }
    }

    @Override
    public void onGattConnectionFailed() {
        // Nothing to do, event managed in 'onGattConnectionStateChange'
    }

    @Override
    public void onGattConnectionLost() {
        // Nothing to do, event managed in 'onGattConnectionStateChange'
    }

    @Override
    public void onCharacteristicNotificationSet(@NonNull BluetoothGattCharacteristic characteristic,
                                                boolean enable, boolean success) {
        if (success) {
            // Set notification flag
            if (characteristic == keepAliveChar) {
                keepAliveNotificationsActive = enable;
            } else if (characteristic == buttonPressNotificationChar) {
                buttonPressNotificationsActive = enable;
            } else if (characteristic == parameterNotificationChar) {
                parameterNotificationsActive = enable;
            } else if (characteristic == batteryStatusChar) {
                batteryStatusNotificationsActive = enable;
            } else if (characteristic == sensorParamNotificationChar) {
                sensorParamNotificationsActive = enable;
            } else if (characteristic == orientationDataChar) {
                orientationDataNotificationsActive = enable;
            } else if (characteristic == debugOutputChar) {
                debugOutputNotificationsActive = enable;
            }
        }
    }

    @Override
    public void onCharacteristicRead(@NonNull BluetoothGattCharacteristic characteristic,
                                     @Nullable byte[] value, boolean success) {
        // Retrieve value for firmware info and battery status characteristics
        if (success) {
            if (characteristic == firmwareInfoChar) {
                if (value == null || value.length < 2){
                    Log.e(DEBUG_TAG, "BeltCommunicationController: Malformed packet read on " +
                            "firmware info characteristic");
                    return;
                }
                int version = (value[0] & 0xFF) | ((value[1] & 0xFF) << 8);
                setFirmwareVersion(version);
            } else if (characteristic == batteryStatusChar) {
                try {
                    setBatteryStatus(new BeltBatteryStatus(value));
                } catch (Exception e) {
                    Log.e(DEBUG_TAG, "BeltCommunicationController: Malformed packet read " +
                                    "on battery status characteristic",
                            e);
                    return;
                }
            }
        }
        // Check for handshake completion that should end with firmware reading
        if (handshakeStarted) {
            if (isHandshakeCompleted()) {
                handshakeStarted = false;
                if (handshakeCallback != null) {
                    handshakeCallback.onHandshakeCompleted(true);
                }
            }
        }
    }

    @Override
    public void onCharacteristicWrite(@NonNull BluetoothGattCharacteristic characteristic,
                                      @Nullable byte[] value, boolean success) {
        // Nothing to do
    }

    @Override
    public void onCharacteristicChanged(@Nullable BluetoothGattCharacteristic characteristic,
                                        @Nullable byte[] value) {
        if (characteristic == keepAliveChar) {
            // Retrieve mode
            BeltMode currentMode = null;
            if (value != null && value.length >= 2) {
                currentMode = BeltMode.fromValue(value[1]);
                if (currentMode != null) {
                    setBeltMode(currentMode);
                }
            }
            if (currentMode == null) {
                Log.e(DEBUG_TAG, "BeltCommunicationController: Malformed packet received " +
                        "on keep-alive characteristic.");
            }
            // Acknowledge keep-alive
            if (!gattController.writeCharacteristic(keepAliveChar, KEEP_ALIVE_PACKET)) {
                Log.e(DEBUG_TAG, "BeltCommunicationController: Unable to send keep-alive " +
                        "acknowledgment.");
            }

        } else if (characteristic == buttonPressNotificationChar) {
            if (value == null || value.length < 5) {
                Log.e(DEBUG_TAG, "BeltCommunicationController: Malformed packet received " +
                        "on button press notification characteristic.");
                return;
            }
            BeltMode previousMode = BeltMode.fromValue(value[3]);
            BeltMode subsequentMode = BeltMode.fromValue(value[4]);
            BeltButton button = BeltButton.fromValue(value[0]);
            if (previousMode == null || subsequentMode == null || button == null) {
                Log.e(DEBUG_TAG, "BeltCommunicationController: Malformed packet received " +
                        "on button press notification characteristic.");
                return;
            }
            notifyButtonPressed(new BeltButtonPressEvent(button, previousMode, subsequentMode));
            if (subsequentMode == BeltMode.STANDBY) {
                gattController.disconnect();
            }


        } else if (characteristic == parameterNotificationChar) {
            if (value == null || value.length < 2) {
                Log.e(DEBUG_TAG, "BeltCommunicationController: Malformed packet received " +
                        "on parameter notification characteristic.");
                return;
            }
            if (value[0]==0x01 && value[1]==0x01) {
                // Belt mode
                if (value.length < 3) {
                    Log.e(DEBUG_TAG, "BeltCommunicationController: Malformed packet " +
                            "received on parameter notification characteristic.");
                    return;
                }
                BeltMode mode = BeltMode.fromValue(value[2]);
                if (mode == null) {
                    Log.e(DEBUG_TAG, "BeltCommunicationController: Malformed packet " +
                            "received on parameter notification characteristic.");
                } else {
                    setBeltMode(mode);
                }
            } else if ((value[0]==0x01 && value[1]==0x02) ||
                    (value[0]==0x10 && value[1]==0x00)) {
                // Default vibration intensity
                if (value.length < 3) {
                    Log.e(DEBUG_TAG, "BeltCommunicationController: Malformed packet " +
                            "received on parameter notification characteristic.");
                    return;
                }
                int intensity = value[2];
                if (intensity < 0 || intensity > 100) {
                    Log.e(DEBUG_TAG, "BeltCommunicationController: Malformed packet " +
                            "received on parameter notification characteristic.");
                } else {
                    setDefaultIntensity(intensity);
                }
            } else if ((value[0]==0x01 && value[1]==0x03) ||
                    (value[0]==0x10 && value[1]==0x01)) {
                // Heading offset
                if (value.length < 4) {
                    Log.e(DEBUG_TAG, "BeltCommunicationController: Malformed packet " +
                            "received on parameter notification characteristic.");
                }
                Integer offset = ((((int) value[3]) << 8) | (0xFF & value[2]));
                setParameterValue(BeltParameter.HEADING_OFFSET, offset);
            } else if (value[0]==0x01 && value[1]==0x04) {
                // Bluetooth name
                Log.w(DEBUG_TAG, "BeltCommunicationController: Parameter not supported.");
            } else if (value[0]==0x10 && value[1]==0x02) {
                // Buzzer active
                Log.w(DEBUG_TAG, "BeltCommunicationController: Parameter not supported.");
            } else if (value[0]==0x10 && value[1]==0x03) {
                // Accuracy signal state
                if (value.length < 3) {
                    Log.e(DEBUG_TAG, "BeltCommunicationController: Malformed packet " +
                            "received on parameter notification characteristic.");
                }
                Integer state = (0xFF & value[2]);
                if (state < 0 || state > 3) {
                    Log.e(DEBUG_TAG, "BeltCommunicationController: Malformed packet " +
                            "received on parameter notification characteristic.");
                }
                setParameterValue(BeltParameter.ACCURACY_SIGNAL_STATE, state);
            }

        } else if (characteristic == orientationDataChar) {
            if (value == null || value.length < 16) {
                Log.e(DEBUG_TAG, "BeltCommunicationController: Malformed packet received " +
                        "on orientation data characteristic.");
                return;
            }
            int id = (value[0] & 0xFF);
            int beltHeading = (((int) value[2]) << 8) | (0xFF & value[1]);
            int heading = (((int) value[4]) << 8) | (0xFF & value[3]);
            int roll = (((int) value[6]) << 8) | (0xFF & value[5]);
            int pitch = (((int) value[8]) << 8) | (0xFF & value[7]);
            int accuracy = (((int) value[10]) << 8) | (0xFF & value[9]);
            int magStatus = value[11];
            int accelStatus = value[12];
            int gyroStatus = value[13];
            int fusionStatus = value[14];
            boolean inaccurate = (value[15]!=0);
            setOrientation(new BeltOrientation(
                    id, beltHeading, heading, roll, pitch, accuracy, magStatus, accelStatus,
                    gyroStatus, fusionStatus, inaccurate
            ));

        } else if (characteristic == batteryStatusChar) {
            try {
                setBatteryStatus(new BeltBatteryStatus(value));
            } catch (Exception e) {
                Log.e(DEBUG_TAG, "BeltCommunicationController: Malformed packet read on " +
                                "battery status characteristic",
                        e);
            }
        }
    }

    @Override
    public void onRequestCompleted(int requestId, @Nullable byte[] notifiedValue, boolean success) {
        // Nothing to do, the notifications are handled in 'onCharacteristicChanged'
    }

    /**
     * Checks if the handshake is completed.
     * @return <code>true</code> if handshake is completed.
     */
    private boolean isHandshakeCompleted() {
        // Check active notifications
        if (!keepAliveNotificationsActive) {
            return false;
        }
        if (!buttonPressNotificationsActive) {
            return false;
        }
        if (!parameterNotificationsActive) {
            return false;
        }
        if (!batteryStatusNotificationsActive) {
            return false;
        }
        // Check parameters
        if (beltMode == BeltMode.UNKNOWN) {
            return false;
        }
        if (defaultIntensity == null) {
            return false;
        }
        if (beltFirmwareVersion == null) {
            return false;
        }
        return true;
    }

}
