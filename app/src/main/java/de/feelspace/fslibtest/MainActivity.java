package de.feelspace.fslibtest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.feelspace.fslib.BeltCommandInterface;
import de.feelspace.fslib.BeltConnectionState;
import de.feelspace.fslib.BeltOrientation;
import de.feelspace.fslib.NavigationController;
import de.feelspace.fslib.NavigationEventListener;
import de.feelspace.fslib.NavigationState;
import de.feelspace.fslib.PowerStatus;

public class MainActivity extends BluetoothCheckActivity implements BluetoothCheckCallback,
        NavigationEventListener {
    // Debug
    @SuppressWarnings("unused")
    private static final String DEBUG_TAG = "FeelSpace-Debug";
    @SuppressWarnings("unused")
    private static final boolean DEBUG = true;

    // Application controller
    private AppController appController;

    // UI components
    private Button connectButton;
    private Button disconnectButton;
    private TextView connectionStateTextView;
    private TextView beltHeadingTextView;
    private TextView boxOrientationTextView;
    private TextView sensorStatusTextView;

    // UI update parameters
    private long lastOrientationUpdateTimeMillis;
    private static final long MIN_PERIOD_ORIENTATION_UPDATE_MILLIS = 250;

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

        // Calibration
        beltHeadingTextView = findViewById(R.id.activity_main_belt_heading_text_view);
        boxOrientationTextView = findViewById(R.id.activity_main_box_orientation_text_view);
        sensorStatusTextView = findViewById(R.id.activity_main_sensor_status_text_view);

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
        updateOrientationTextView();
        updateSensorStatusTextView();
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

    @SuppressLint("SetTextI18n")
    private void updateOrientationTextView() {
        runOnUiThread(() -> {
            BeltCommandInterface beltCommand = appController.getNavigationController()
                    .getBeltConnection().getCommandInterface();
            BeltOrientation orientation = beltCommand.getOrientation();
            Integer h = (orientation==null)?null:orientation.getControlBoxHeading();
            Integer p = (orientation==null)?null:orientation.getControlBoxPitch();
            Integer r = (orientation==null)?null:orientation.getControlBoxRoll();
            if (orientation == null) {
                beltHeadingTextView.setText("Belt orientation: -");
                beltHeadingTextView.setBackgroundResource(R.color.white);
            } else {
                beltHeadingTextView.setText(
                        String.format(Locale.ENGLISH, "Belt orientation: %+03d ± %+02d",
                                orientation.getBeltHeading(),
                                ((orientation.getAccuracy() == null) ? 0 : orientation.getAccuracy())));
                if (orientation.isOrientationAccurate() == null) {
                    beltHeadingTextView.setBackgroundResource(R.color.white);
                } else if (orientation.isOrientationAccurate()) {
                    beltHeadingTextView.setBackgroundResource(R.color.bg_accurate);
                } else {
                    beltHeadingTextView.setBackgroundResource(R.color.bg_inaccurate);
                }
            }
            if (h == null || p == null || r == null) {
                boxOrientationTextView.setText("H.: -, P: -, R: -");
            } else {
                boxOrientationTextView.setText(
                        String.format(Locale.ENGLISH, "H.: %+03d, P: %+03d, R: %+03d", h, p, r));
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void updateSensorStatusTextView() {
        runOnUiThread(() -> {
            BeltCommandInterface beltCommand = appController.getNavigationController()
                    .getBeltConnection().getCommandInterface();
            BeltOrientation orientation = beltCommand.getOrientation();
            if (orientation == null || orientation.getMagnetometerStatus() == null) {
                sensorStatusTextView.setText("Mag. status: -");
            } else {
                sensorStatusTextView.setText(
                        String.format(Locale.ENGLISH, "Mag. Status: %d",
                                orientation.getMagnetometerStatus()));
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
        long timeMillis = (System.nanoTime()/1000000);
        if ((timeMillis-lastOrientationUpdateTimeMillis) > MIN_PERIOD_ORIENTATION_UPDATE_MILLIS) {
            updateOrientationTextView();
            updateSensorStatusTextView();
            lastOrientationUpdateTimeMillis = timeMillis;
        }
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

}
