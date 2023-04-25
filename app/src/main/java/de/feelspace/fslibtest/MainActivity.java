package de.feelspace.fslibtest;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import de.feelspace.fslib.BeltConnectionState;
import de.feelspace.fslib.NavigationController;
import de.feelspace.fslib.NavigationEventListener;
import de.feelspace.fslib.NavigationState;
import de.feelspace.fslib.PowerStatus;

public class MainActivity extends BluetoothCheckActivity implements BluetoothCheckCallback,
        NavigationEventListener, AdvancedBeltListener {

    // Application controller
    private AppController appController;

    // UI components
    private Button connectButton;
    private Button disconnectButton;
    private TextView connectionStateTextView;
    private Button startSensorNotificationsButton;
    private Button stopSensorNotificationsButton;
    private Button startSensorRecordingButton;
    private Button stopSensorRecordingButton;
    private TextView sensorRecordingCountTextView;

    // UI update parameters
    private static final int MIN_PERIOD_ERROR_TOAST_MILLIS = 1000;
    private long lastErrorToastTimeMillis = 0;

    // MARK: Activity methods overriding

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Application controller
        appController = AppController.getInstance();
        appController.init(getApplicationContext());

        // Navigation controller
        appController.getNavigationController().addNavigationEventListener(this);

        // Advanced belt controller
        appController.getAdvancedBeltController().addAdvancedBeltListener(this);

        // Connection state
        connectionStateTextView = findViewById(R.id.activity_main_connection_state_text_view);

        // Connect button
        connectButton = findViewById(R.id.activity_main_connect_button);
        connectButton.setOnClickListener(view -> {
            activateBluetooth(this);
        });

        // Disconnect button
        disconnectButton = findViewById(R.id.activity_main_disconnect_button);
        disconnectButton.setOnClickListener(view -> {
            NavigationController navController = appController.getNavigationController();
            if (navController != null) {
                navController.disconnectBelt();
            }
        });

        // Start notifications button
        startSensorNotificationsButton = findViewById(R.id.activity_main_start_sensor_notifications_button);
        startSensorNotificationsButton.setOnClickListener(view -> {
            appController.getAdvancedBeltController().setRawSensorNotificationsEnable(true);
        });

        // Stop notification button
        stopSensorNotificationsButton = findViewById(R.id.activity_main_stop_sensor_notifications_button);
        stopSensorNotificationsButton.setOnClickListener(view -> {
            appController.getAdvancedBeltController().setRawSensorNotificationsEnable(false);
        });

        // Start recording
        startSensorRecordingButton = findViewById(R.id.activity_main_start_sensor_recording_button);
        startSensorRecordingButton.setOnClickListener(view -> {
            // TODO
        });

        // Stop recording
        stopSensorRecordingButton = findViewById(R.id.activity_main_stop_sensor_recording_button);
        stopSensorRecordingButton.setOnClickListener(view -> {
            // TODO
        });

        // Recordings count
        sensorRecordingCountTextView = findViewById(R.id.activity_main_sensor_recording_count_text_view);

        // Update UI
        updateUI();
    }


    // MARK: Private methods

    private void showToast(final String message) {
        runOnUiThread(() -> Toast.makeText(
                MainActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    private void updateUI() {
        updateConnectionLabel();
        updateConnectionButtons();
        updateSensorNotificationsButtons();
    }

    private void updateConnectionLabel() {
        runOnUiThread(() -> {
            BeltConnectionState state = BeltConnectionState.STATE_DISCONNECTED;
            NavigationController navController = appController.getNavigationController();
            if (navController != null) {
                state = navController.getConnectionState();
            }
            switch (state) {
                case STATE_DISCONNECTED:
                    connectionStateTextView.setText(R.string.disconnected);
                    break;
                case STATE_SCANNING:
                    connectionStateTextView.setText(R.string.scanning);
                    break;
                case STATE_PAIRING:
                    connectionStateTextView.setText(R.string.pairing);
                    break;
                case STATE_CONNECTING:
                    connectionStateTextView.setText(R.string.connecting);
                    break;
                case STATE_RECONNECTING:
                    connectionStateTextView.setText(R.string.reconnecting);
                    break;
                case STATE_DISCOVERING_SERVICES:
                    connectionStateTextView.setText(R.string.discovering_services);
                    break;
                case STATE_HANDSHAKE:
                    connectionStateTextView.setText(R.string.handshake);
                    break;
                case STATE_CONNECTED:
                    connectionStateTextView.setText(R.string.connected);
                    break;
            }
        });
    }

    private void updateConnectionButtons() {
        runOnUiThread(() -> {
            BeltConnectionState state = BeltConnectionState.STATE_DISCONNECTED;
            NavigationController navController = appController.getNavigationController();
            if (navController != null) {
                state = navController.getConnectionState();
            }
            switch (state) {
                case STATE_DISCONNECTED:
                    connectButton.setEnabled(true);
                    disconnectButton.setEnabled(false);
                    break;
                case STATE_SCANNING:
                case STATE_CONNECTING:
                case STATE_RECONNECTING:
                case STATE_DISCOVERING_SERVICES:
                case STATE_HANDSHAKE:
                case STATE_CONNECTED:
                    connectButton.setEnabled(false);
                    disconnectButton.setEnabled(true);
                    break;
            }
        });
    }

    private void updateSensorNotificationsButtons() {
        runOnUiThread(() -> {
            AdvancedBeltController beltController = appController.getAdvancedBeltController();
            if (beltController != null) {
                if (beltController.areRawSensorNotificationsEnabled()) {
                    startSensorNotificationsButton.setEnabled(false);
                    stopSensorNotificationsButton.setEnabled(true);
                } else {
                    startSensorNotificationsButton.setEnabled(true);
                    stopSensorNotificationsButton.setEnabled(false);
                }
            } else {
                startSensorNotificationsButton.setEnabled(false);
                stopSensorNotificationsButton.setEnabled(false);
            }
        });
    }

    // MARK: Implementation of NavigationEventListener

    @Override
    public void onNavigationStateChanged(NavigationState state) {

    }

    @Override
    public void onBeltHomeButtonPressed(boolean navigating) {

    }

    @Override
    public void onBeltDefaultVibrationIntensityChanged(int intensity) {

    }

    @Override
    public void onBeltOrientationUpdated(int beltHeading, boolean accurate) {

    }

    @Override
    public void onBeltBatteryLevelUpdated(int batteryLevel, PowerStatus status) {

    }

    @Override
    public void onCompassAccuracySignalStateUpdated(boolean enabled) {

    }

    @Override
    public void onBeltConnectionStateChanged(BeltConnectionState state) {
        updateUI();
        if (state == BeltConnectionState.STATE_CONNECTED) {
            // Enable debug notifications
            appController.getAdvancedBeltController().setDebugNotificationsEnable(true);
            // Retrieve calibration
            appController.getAdvancedBeltController().getSensorCalibration();
        }
    }

    @Override
    public void onBeltConnectionLost() {
        showToast("Belt connection lost!");
    }

    @Override
    public void onBeltConnectionFailed() {
        showToast("Belt connection fails!");
    }

    @Override
    public void onNoBeltFound() {
        showToast("No belt found!");
    }

    // MARK: Implementation of `BluetoothCheckCallback`

    @Override
    public void onBluetoothReady() {
        NavigationController navController = appController.getNavigationController();
        if (navController != null) {
            navController.searchAndConnectBelt();
        }
    }

    @Override
    public void onBluetoothActivationRejected() {
        showToast("BLE activation rejected!");
    }

    @Override
    public void onBluetoothActivationFailed() {
        showToast("BLE activation failed!");
    }

    @Override
    public void onUnsupportedFeature() {
        showToast("Unsupported BLE feature!");
    }


    // MARK: Implementation of `AdvancedBeltListener`

    @Override
    public void onRawSensorNotificationsStateChanged(boolean enable) {
        updateSensorNotificationsButtons();
    }

    @Override
    public void onDebugNotificationsStateChanged(boolean enable) {

    }

    @Override
    public void onSensorCalibrationRetrieved(float[] magOffsets, float[] magGains, float magCalibError, float[] gyroOffsets) {

    }

    @Override
    public void onRawSensorRecordNotified(int sensorId, int[] values) {

    }

    @Override
    public void onRawSensorNotificationSequenceError() {
        long timeMillis = (System.nanoTime()/1000000);
        if ((timeMillis-lastErrorToastTimeMillis) > MIN_PERIOD_ERROR_TOAST_MILLIS) {
            showToast("Error on sensor notification sequence!");
            lastErrorToastTimeMillis = timeMillis;
        }
    }

    @Override
    public void onErrorNotified(int errorCode) {
        long timeMillis = (System.nanoTime()/1000000);
        if ((timeMillis-lastErrorToastTimeMillis) > MIN_PERIOD_ERROR_TOAST_MILLIS) {
            showToast("Error belt: 0x"+Integer.toHexString(errorCode));
            lastErrorToastTimeMillis = timeMillis;
        }
    }
}
