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
        NavigationEventListener, AdvancedBeltListener, SimpleLoggerListener {
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
    private Button startSelfTestButton;
    private TextView connectionStateTextView;
    private Button startSensorNotificationsButton;
    private Button stopSensorNotificationsButton;
    private Button startSensorRecordingButton;
    private Button stopSensorRecordingButton;
    private TextView sensorRecordingCountTextView;
    private TextView magOffsetsTextView;
    private TextView magGainsTextView;
    private TextView magErrorTextView;
    private TextView gyroOffsetsTextView;
    private TextView gyroStatusTextView;
    private TextView beltHeadingTextView;
    private TextView boxOrientationTextView;
    private TextView sensorStatusTextView;

    // UI update parameters
    private static final long MIN_PERIOD_ERROR_TOAST_MILLIS = 1000;
    private long lastErrorToastTimeMillis = 0;
    private static final long MIN_PERIOD_RECORDS_COUNT_UPDATE_MILLIS = 300;
    private long lastRecordsCountUpdateTimeMillis = 0;
    private long recordsCount = 0;
    private long lastOrientationUpdateTimeMillis;
    private static final long MIN_PERIOD_ORIENTATION_UPDATE_MILLIS = 250;

    // Request ID
    private static final int CREATE_FILE_REQUEST_CODE = 11;

    // MARK: Activity methods overriding

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Application controller
        appController = AppController.getInstance();
        appController.init(getApplicationContext());

        // Logger
        appController.getLogger().addListener(this);

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

        startSelfTestButton = findViewById(R.id.activity_main_test_button);
        startSelfTestButton.setOnClickListener(view -> {
            if (appController.getNavigationController().getConnectionState() ==
                    BeltConnectionState.STATE_CONNECTED) {
                appController.getAdvancedBeltController().startSelfTest();
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
            requestLogFileCreation();
        });

        // Stop recording
        stopSensorRecordingButton = findViewById(R.id.activity_main_stop_sensor_recording_button);
        stopSensorRecordingButton.setOnClickListener(view -> {
            SimpleLogger logger = AppController.getInstance().getLogger();
            if (logger.isLogging()) {
                logger.log(this, "", "\n", "# " + SimpleLogger.getTimeStamp(this));
                logger.log(this, "", "\n", "# Recording stopped.");
            }
            AppController.getInstance().getLogger().stopLog();
        });

        // Recordings count
        sensorRecordingCountTextView = findViewById(R.id.activity_main_sensor_recording_count_text_view);

        // Calibration
        magOffsetsTextView = findViewById(R.id.activity_main_mag_offset_text_view);
        magGainsTextView = findViewById(R.id.activity_main_mag_gain_text_view);
        magErrorTextView = findViewById(R.id.activity_main_mag_error_text_view);
        gyroOffsetsTextView = findViewById(R.id.activity_main_gyro_offset_text_view);
        gyroStatusTextView = findViewById(R.id.activity_main_gyro_status_text_view);
        beltHeadingTextView = findViewById(R.id.activity_main_belt_heading_text_view);
        boxOrientationTextView = findViewById(R.id.activity_main_box_orientation_text_view);
        sensorStatusTextView = findViewById(R.id.activity_main_sensor_status_text_view);

        // Update UI
        updateUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            AppController.getInstance().getLogger().stopLog();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CREATE_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                // Start logging
                try {
                    Uri logFileUri = data.getData();
                    AppController.getInstance().getLogger().startLog(
                            this, logFileUri, null);
                    writeLogFileHeader();
                } catch (Exception e) {
                    Log.e(DEBUG_TAG, "MainActivity: Unable to create log file.", e);
                    showToast("Log file creation failed!");
                }
            }
        }
    }

    // MARK: Private methods

    private void requestLogFileCreation() {
        // Use Storage Access Framework
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        // Generate log file name
        SimpleDateFormat logFileFormat = new SimpleDateFormat(
                this.getString(R.string.log_file_date_pattern_prefix), Locale.getDefault());
        String logFilePrefix = logFileFormat.format(new Date());
        String logFileName = "Raw_sensor_"+logFilePrefix+".txt";
        intent.putExtra(Intent.EXTRA_TITLE, logFileName);
        try {
            Uri uri = Uri.parse(
                    "content://com.android.externalstorage.documents/document/primary:Download");
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri);
        } catch (Exception e) {
            Log.w(DEBUG_TAG, "MainActivity: Unable to set default directory");
        }
        startActivityForResult(intent, CREATE_FILE_REQUEST_CODE);
    }

    private void writeLogFileHeader() {
        SimpleLogger logger = AppController.getInstance().getLogger();
        logger.log(this, "", "\n", "# RAW SENSOR RECORDS FROM NAVIBELT 2");
        logger.log(this, "", "\n", "#");
        logger.log(this, "", "\n", "# Start time: "+SimpleLogger.getTimeStamp(this));
        logger.log(this, "", "\n", "#");
        logger.log(this, "", "\n", "# Calibration data: ");
        logCalibrationData();
        logger.log(this, "", "\n", "");
    }

    private void logCalibrationData() {
        SimpleLogger logger = AppController.getInstance().getLogger();
        AdvancedBeltController beltController = appController.getAdvancedBeltController();
        if (logger.isLogging() && beltController != null) {
            Float[] magOffsets = beltController.getMagOffsets();
            if (magOffsets[0] != null && magOffsets[1] != null && magOffsets[2] != null) {
                logger.log(this, "", "\n",
                        String.format(Locale.ENGLISH, "# Mag. offsets: [%.2f, %.2f, %.2f]",
                                magOffsets[0], magOffsets[1], magOffsets[2]));
            } else {
                logger.log(this, "", "\n", "# Mag. offsets: ?");
            }
            Float[] magGains = beltController.getMagGains();
            if (magGains[0] != null && magGains[1] != null && magGains[2] != null) {
                logger.log(this, "", "\n",
                        String.format(Locale.ENGLISH, "# Mag. gains: [%.2f, %.2f, %.2f]",
                                magGains[0], magGains[1], magGains[2]));
            } else {
                logger.log(this, "", "\n", "# Mag. gains: ?");
            }
            Float magError = beltController.getMagError();
            if (magError != null) {
                logger.log(this, "", "\n",
                        String.format(Locale.ENGLISH, "# Mag. error: %.4f", magError));
            } else {
                logger.log(this, "", "\n", "# Mag. error: ?");
            }
            Float[] gyroOffsets = beltController.getGyroOffsets();
            if (gyroOffsets[0] != null && gyroOffsets[1] != null && gyroOffsets[2] != null) {
                logger.log(this, "", "\n",
                        String.format(Locale.ENGLISH, "# Gyro. offsets: [%.2f, %.2f, %.2f]",
                                gyroOffsets[0], gyroOffsets[1], gyroOffsets[2]));
            } else {
                logger.log(this, "", "\n", "# Gyro. offsets: ?");
            }
            Integer gyroStatus = beltController.getGyroStatus();
            if (gyroStatus != null) {
                logger.log(this, "", "\n",
                        String.format(Locale.ENGLISH, "# Gyro. status: %d", gyroStatus));
            } else {
                logger.log(this, "", "\n", "# Gyro. status: ?");
            }
        }
    }

    private void showToast(final String message) {
        runOnUiThread(() -> Toast.makeText(
                MainActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    private void updateUI() {
        updateConnectionLabel();
        updateConnectionButtons();
        updateTestButton();
        updateSensorNotificationsButtons();
        updateRecordsCountTextView();
        updateCalibrationTextViews();
        updateRecordingButtons();
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

    private void updateTestButton() {
        runOnUiThread(() -> {
            BeltConnectionState state = BeltConnectionState.STATE_DISCONNECTED;
            NavigationController navController = appController.getNavigationController();
            if (navController != null) {
                state = navController.getConnectionState();
            }
            switch (state) {
                case STATE_DISCONNECTED:
                case STATE_SCANNING:
                case STATE_CONNECTING:
                case STATE_RECONNECTING:
                case STATE_DISCOVERING_SERVICES:
                case STATE_HANDSHAKE:
                    startSelfTestButton.setEnabled(false);
                    break;
                case STATE_CONNECTED:
                    startSelfTestButton.setEnabled(true);
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

    private void updateRecordsCountTextView() {
        runOnUiThread(() -> sensorRecordingCountTextView.setText(
                String.format(Locale.ENGLISH, "Records: %d", recordsCount)));
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

    @SuppressLint({"SetTextI18n"})
    private void updateCalibrationTextViews() {
        runOnUiThread(() -> {
            AdvancedBeltController beltController = appController.getAdvancedBeltController();
            if (beltController != null) {
                Float[] magOffsets = beltController.getMagOffsets();
                if (magOffsets[0] != null && magOffsets[1] != null && magOffsets[2] != null) {
                    magOffsetsTextView.setText(
                            String.format(Locale.ENGLISH, "Mag. offsets: [%.2f, %.2f, %.2f]",
                                    magOffsets[0], magOffsets[1], magOffsets[2]));
                } else {
                    magOffsetsTextView.setText("Mag. offsets: ?");
                }
                Float[] magGains = beltController.getMagGains();
                if (magGains[0] != null && magGains[1] != null && magGains[2] != null) {
                    magGainsTextView.setText(
                            String.format(Locale.ENGLISH, "Mag. gains: [%.2f, %.2f, %.2f]",
                                    magGains[0], magGains[1], magGains[2]));
                } else {
                    magGainsTextView.setText("Mag. gains: ?");
                }
                Float magError = beltController.getMagError();
                if (magError != null) {
                    magErrorTextView.setText(
                            String.format(Locale.ENGLISH, "Mag. error: %.4f", magError));
                } else {
                    magErrorTextView.setText("Mag. error: ?");
                }
                Float[] gyroOffsets = beltController.getGyroOffsets();
                if (gyroOffsets[0] != null && gyroOffsets[1] != null && gyroOffsets[2] != null) {
                    gyroOffsetsTextView.setText(
                            String.format(Locale.ENGLISH, "Gyro. offsets: [%.2f, %.2f, %.2f]",
                                    gyroOffsets[0], gyroOffsets[1], gyroOffsets[2]));
                } else {
                    gyroOffsetsTextView.setText("Gyro. offsets: ?");
                }
                Integer gyroStatus = beltController.getGyroStatus();
                if (gyroStatus != null) {
                    gyroStatusTextView.setText(
                            String.format(Locale.ENGLISH, "Gyro. status: %d", gyroStatus));
                } else {
                    gyroStatusTextView.setText("Gyro. status: ?");
                }
            } else {
                magOffsetsTextView.setText("");
                magGainsTextView.setText("");
                magErrorTextView.setText("");
                gyroOffsetsTextView.setText("");
                gyroStatusTextView.setText("");
            }
        });
    }

    private void updateRecordingButtons() {
        runOnUiThread(() -> {
            SimpleLogger logger = AppController.getInstance().getLogger();
            if (logger.isLogging()) {
                startSensorRecordingButton.setEnabled(false);
                stopSensorRecordingButton.setEnabled(true);
            } else {
                startSensorRecordingButton.setEnabled(true);
                stopSensorRecordingButton.setEnabled(false);
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
        if (state == BeltConnectionState.STATE_CONNECTED) {
            // Reset record count
            recordsCount = 0;
        }
        SimpleLogger logger = AppController.getInstance().getLogger();
        if (logger.isLogging()) {
            logger.log(this, "", "\n", "# "+SimpleLogger.getTimeStamp(this));
            logger.log(this, "", "\n", "# Connection state changed: " + state.toString());
        }
    }

    @Override
    public void onBeltConnectionLost() {
        showToast("Belt connection lost!");
        SimpleLogger logger = AppController.getInstance().getLogger();
        if (logger.isLogging()) {
            logger.log(this, "", "\n", "# "+SimpleLogger.getTimeStamp(this));
            logger.log(this, "", "\n", "# Belt connection lost.");
        }
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
    public void onSensorCalibrationUpdated() {
        updateCalibrationTextViews();
        SimpleLogger logger = AppController.getInstance().getLogger();
        if (logger.isLogging()) {
            logger.log(this, "", "\n", "# "+SimpleLogger.getTimeStamp(this));
            logger.log(this, "", "\n", "# Calibration updated: ");
            logCalibrationData();
        }
    }

    @Override
    public void onRawSensorRecordNotified(int[][] records) {
        recordsCount += records.length;
        long timeMillis = (System.nanoTime()/1000000);
        if ((timeMillis-lastRecordsCountUpdateTimeMillis) > MIN_PERIOD_RECORDS_COUNT_UPDATE_MILLIS) {
            updateRecordsCountTextView();
            lastRecordsCountUpdateTimeMillis = timeMillis;
        }
        // Log records
        SimpleLogger logger = AppController.getInstance().getLogger();
        if (logger.isLogging()) {
            for (int i=0; i<records.length; i++) {
                logger.log(this, " ", "\n", records[i][0], records[i][1], records[i][2], records[i][3]);
            }
        }
    }

    @Override
    public void onRawSensorNotificationSequenceError() {
        long timeMillis = (System.nanoTime()/1000000);
        if ((timeMillis-lastErrorToastTimeMillis) > MIN_PERIOD_ERROR_TOAST_MILLIS) {
            showToast("Error on sensor notification sequence!");
            lastErrorToastTimeMillis = timeMillis;
        }
        SimpleLogger logger = AppController.getInstance().getLogger();
        if (logger.isLogging()) {
            logger.log(this, "", "\n", "# "+SimpleLogger.getTimeStamp(this));
            logger.log(this, "", "\n", "# ERROR: Missing sensor notification (bad sequence).");
        }
    }

    @Override
    public void onErrorNotified(int errorCode) {
        long timeMillis = (System.nanoTime()/1000000);
        if ((timeMillis-lastErrorToastTimeMillis) > MIN_PERIOD_ERROR_TOAST_MILLIS) {
            showToast("Error belt: 0x"+Integer.toHexString(errorCode));
            lastErrorToastTimeMillis = timeMillis;
        }
        SimpleLogger logger = AppController.getInstance().getLogger();
        if (logger.isLogging()) {
            logger.log(this, "", "\n", "# "+SimpleLogger.getTimeStamp(this));
            logger.log(this, "", "\n", "# ERROR: Belt error ("+errorCode+"): 0x"+Integer.toHexString(errorCode));
        }
    }

    // MARK: Implementation of `SimpleLoggerListener`

    @Override
    public void onLogStateChanged(boolean isLogging) {
        updateRecordingButtons();
    }
}
