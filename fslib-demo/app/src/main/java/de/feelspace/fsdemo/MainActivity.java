/*
 * Copyright feelSpace GmbH, 2018
 *
 * @author David Meignan
 */
package de.feelspace.fsdemo;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;

import de.feelspace.fslib.BeltBatteryStatus;
import de.feelspace.fslib.BeltButtonPressEvent;
import de.feelspace.fslib.BeltSound;
import de.feelspace.fslib.BeltCommandListener;
import de.feelspace.fslib.BeltCommunicationInterface;
import de.feelspace.fslib.BeltCommunicationListener;
import de.feelspace.fslib.BeltConnectionInterface;
import de.feelspace.fslib.BeltConnectionListener;
import de.feelspace.fslib.BeltConnectionState;
import de.feelspace.fslib.BeltMode;
import de.feelspace.fslib.BeltOrientation;
import de.feelspace.fslib.BeltParameter;
import de.feelspace.fslib.BeltVibrationSignal;
import de.feelspace.fslib.BluetoothActivationFragment;
import de.feelspace.fslib.OnBluetoothActivationCallback;
import de.feelspace.fslib.R;

public class MainActivity extends AppCompatActivity implements BeltCommandListener,
        BeltConnectionListener, BeltCommunicationListener, OnBluetoothActivationCallback,
        SearchDialogFragment.OnBeltSelectedCallback {
    // Debug
    @SuppressWarnings("unused")
    private static final String DEBUG_TAG = "FeelSpace-Debug";
    @SuppressWarnings("unused")
    private static final boolean DEBUG = true;

    // Belt connection manager and belt controller
    private static BeltConnectionInterface beltConnection;
    private BeltCommunicationInterface beltController;

    // Fragment to check BT activation and check permissions
    private BluetoothActivationFragment bluetoothActivationFragment;
    protected static final String BLUETOOTH_ACTIVATION_FRAGMENT_TAG =
            "MainActivity.BLUETOOTH_ACTIVATION_FRAGMENT_TAG";

    // UI components
    private TextView connectionStateLabel;
    private TextView firmwareVersionLabel;
    private TextView currentBeltModeLabel;
    private TextView defaultIntensityLabel;
    private SeekBar setIntensitySlider;
    private Button setIntensityButton;
    private SeekBar vibrateAtAngleSlider;
    private Button vibrateAtAngleStartButton;
    private SeekBar vibrateAtMagneticBearingSlider;
    private Button vibrateAtMagneticBearingStartButton;
    private SeekBar pulseAtAngleSlider;
    private Button pulseAtAngleStartButton;
    private TextView beltHeadingLabel;
    private TextView boxHeadingLabel;
    private TextView boxPitchLabel;
    private TextView boxRollLabel;
    private TextView orientationAccurateLabel;
    private TextView fusionStatusLabel;
    private TextView magStatusLabel;
    private TextView accuracyLabel;
    private TextView powerStatusLabel;
    private TextView batteryLevelLabel;
    private TextView tteTtfLabel;
    private TextView currentLabel;

    // Belt orientation
    private BeltOrientation beltOrientation;

    // Belt battery status
    private BeltBatteryStatus beltBatteryStatus;

    // Formats
    private static final DecimalFormat integerFormat = new DecimalFormat("#0");
    private static final DecimalFormat integerDegreeFormat = new DecimalFormat("#000 '°'");
    private static final DecimalFormat integerPercentFormat = new DecimalFormat("#0 '%'");

    // Flag to distinguish BT activation for search-and-connect and scan
    boolean connectOnBtActivated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Retrieve the connection and command manager
        beltConnection = BeltConnectionInterface.create(getApplicationContext());
        beltController = beltConnection.getCommunicationInterface();

        // Add listeners
        beltConnection.addConnectionListener(this);
        beltController.addCommandListener(this);
        beltController.addCommunicationListener(this);

        // Add BT activation and permission-checker fragment
        FragmentManager fm = getSupportFragmentManager();
        bluetoothActivationFragment = (BluetoothActivationFragment) fm.findFragmentByTag(
                BLUETOOTH_ACTIVATION_FRAGMENT_TAG);
        if (bluetoothActivationFragment == null) {
            bluetoothActivationFragment = new BluetoothActivationFragment();
            fm.beginTransaction().add(bluetoothActivationFragment,
                    BLUETOOTH_ACTIVATION_FRAGMENT_TAG).commit();
        }

        // References to UI components
        connectionStateLabel = findViewById(R.id.activity_main_connection_status_label);
        firmwareVersionLabel = findViewById(R.id.activity_main_firmware_version_label);
        currentBeltModeLabel = findViewById(R.id.activity_main_current_belt_mode_label);
        defaultIntensityLabel = findViewById(R.id.activity_main_default_intensity_label);
        beltHeadingLabel = findViewById(R.id.activity_main_belt_heading_label);
        boxHeadingLabel = findViewById(R.id.activity_main_box_heading_label);
        boxPitchLabel = findViewById(R.id.activity_main_box_pitch_label);
        boxRollLabel = findViewById(R.id.activity_main_box_roll_label);
        orientationAccurateLabel = findViewById(R.id.activity_main_orientation_accurate_label);
        fusionStatusLabel = findViewById(R.id.activity_main_fusion_status_label);
        magStatusLabel = findViewById(R.id.activity_main_mag_status_label);
        accuracyLabel = findViewById(R.id.activity_main_accuracy_label);
        powerStatusLabel = findViewById(R.id.activity_main_power_status_label);
        batteryLevelLabel = findViewById(R.id.activity_main_battery_level_label);
        tteTtfLabel = findViewById(R.id.activity_main_tte_ttf_label);
        currentLabel = findViewById(R.id.activity_main_current_label);

        // *** Button event handlers ***

        // Scan for belt button
        Button scanForBeltButton = findViewById(R.id.activity_main_scan_for_belt_button);
        if (scanForBeltButton != null) {
            scanForBeltButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (beltConnection != null &&
                            beltConnection.getState() ==
                                    BeltConnectionState.STATE_DISCONNECTED) {
                        connectOnBtActivated = false;
                        bluetoothActivationFragment.activateBluetooth();
                    }
                }
            });
        }

        // Search and connect button
        Button searchAndConnectButton = findViewById(R.id.activity_main_search_and_connect_button);
        if (searchAndConnectButton != null) {
            searchAndConnectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (beltConnection != null &&
                            beltConnection.getState() ==
                                    BeltConnectionState.STATE_DISCONNECTED) {
                        // Check BT activation and permissions.
                        // Note: The search-and-connect procedure is started in the callback
                        // 'onBluetoothActivated'.
                        connectOnBtActivated = true;
                        bluetoothActivationFragment.activateBluetooth();
                    }
                }
            });
        }

        // Disconnect button
        Button disconnectButton = findViewById(R.id.activity_main_disconnect_button);
        if (disconnectButton != null) {
            disconnectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (beltConnection != null &&
                            beltConnection.getState() !=
                                    BeltConnectionState.STATE_DISCONNECTED) {
                        beltConnection.disconnect();
                    }
                }
            });
        }

        // Wait mode button
        Button waitModeButton = findViewById(R.id.activity_main_wait_mode_button);
        if (waitModeButton != null) {
            waitModeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (beltConnection != null &&
                            beltConnection.getState() ==
                                    BeltConnectionState.STATE_CONNECTED &&
                            beltController != null) {
                        beltController.changeMode(BeltMode.WAIT);
                    }
                }
            });
        }

        // Pause mode button
        Button pauseModeButton = findViewById(R.id.activity_main_pause_mode_button);
        if (pauseModeButton != null) {
            pauseModeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (beltConnection != null &&
                            beltConnection.getState() ==
                                    BeltConnectionState.STATE_CONNECTED &&
                            beltController != null) {
                        beltController.changeMode(BeltMode.PAUSE);
                    }
                }
            });
        }

        // Compass mode button
        Button compassModeButton = findViewById(R.id.activity_main_compass_mode_button);
        if (compassModeButton != null) {
            compassModeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (beltConnection != null &&
                            beltConnection.getState() ==
                                    BeltConnectionState.STATE_CONNECTED &&
                            beltController != null) {
                        beltController.changeMode(BeltMode.COMPASS);
                    }
                }
            });
        }

        // App mode button
        Button appModeButton = findViewById(R.id.activity_main_app_mode_button);
        if (appModeButton != null) {
            appModeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (beltConnection != null &&
                            beltConnection.getState() ==
                                    BeltConnectionState.STATE_CONNECTED &&
                            beltController != null) {
                        beltController.changeMode(BeltMode.APP);
                    }
                }
            });
        }

        // Default intensity slider and button
        setIntensitySlider = findViewById(R.id.activity_main_set_intensity_slider);
        if (setIntensitySlider != null) {
            setIntensitySlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    // Update button label
                    if (setIntensityButton != null) {
                        setIntensityButton.setText(getString(
                                R.string.activity_main_set_intensity_formatted_button_text,
                                setIntensitySlider.getProgress()+5));
                    }
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    // Nothing to do
                }
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    // Nothing to do
                }
            });
        }
        setIntensityButton = findViewById(R.id.activity_main_set_intensity_button);
        if (setIntensityButton != null) {
            setIntensityButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (setIntensitySlider != null && beltConnection != null &&
                            beltConnection.getState() ==
                                    BeltConnectionState.STATE_CONNECTED &&
                            beltController != null) {
                        beltController.changeDefaultVibrationIntensity(
                                setIntensitySlider.getProgress()+5, true);
                    }
                }
            });
        }

        // Warning signal button
        Button warningSignalButton = findViewById(R.id.activity_main_warning_signal_button);
        if (warningSignalButton != null) {
            warningSignalButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (beltConnection != null &&
                            beltConnection.getState() ==
                                    BeltConnectionState.STATE_CONNECTED &&
                            beltController != null) {
                        beltController.signal(BeltVibrationSignal.OPERATION_WARNING,
                                25, 0, null);
                    }
                }
            });
        }

        // Battery signal button
        Button batterySignalButton = findViewById(R.id.activity_main_battery_signal_button);
        if (batterySignalButton != null) {
            batterySignalButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (beltConnection != null &&
                            beltConnection.getState() ==
                                    BeltConnectionState.STATE_CONNECTED &&
                            beltController != null) {
                        beltController.signal(BeltVibrationSignal.BATTERY_LEVEL,
                                null, 0, null);
                    }
                }
            });
        }

        // Destination reached signal button
        Button destinationReachedSignalButton =
                findViewById(R.id.activity_main_destination_reached_signal_button);
        if (destinationReachedSignalButton != null) {
            destinationReachedSignalButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (beltConnection != null &&
                            beltConnection.getState() ==
                                    BeltConnectionState.STATE_CONNECTED &&
                            beltController != null) {
                        beltController.signal(BeltVibrationSignal.DESTINATION_REACHED_SINGLE,
                                null, 0, null);
                    }
                }
            });
        }

        // Vibrate at angle
        vibrateAtAngleSlider = findViewById(R.id.activity_main_vibrate_at_angle_slider);
        if (vibrateAtAngleSlider != null) {
            vibrateAtAngleSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    // Update button label
                    if (vibrateAtAngleStartButton != null) {
                        vibrateAtAngleStartButton.setText(getString(
                                R.string.activity_main_vibrate_at_angle_start_button_text,
                                vibrateAtAngleSlider.getProgress()));
                    }
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    // Nothing to do
                }
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    // Nothing to do
                }
            });
        }
        vibrateAtAngleStartButton = findViewById(R.id.activity_main_vibrate_at_angle_start_button);
        if (vibrateAtAngleStartButton != null) {
            vibrateAtAngleStartButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (vibrateAtAngleSlider != null && beltConnection != null &&
                            beltConnection.getState() ==
                                    BeltConnectionState.STATE_CONNECTED &&
                            beltController != null) {
                        if (beltController.getMode() != BeltMode.APP) {
                            beltController.changeMode(BeltMode.APP);
                        }
                        beltController.vibrateAtAngle(
                                vibrateAtAngleSlider.getProgress(),
                                null, // Default intensity
                                BeltVibrationSignal.CONTINUOUS,
                                1,
                                false);
                    }
                }
            });
        }

        // Stop vibration on channel 1
        Button stopVibrationCh1Button =
                findViewById(R.id.activity_main_vibrate_at_angle_stop_button);
        if (stopVibrationCh1Button != null) {
            stopVibrationCh1Button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (beltConnection != null &&
                            beltConnection.getState() ==
                                    BeltConnectionState.STATE_CONNECTED &&
                            beltController != null) {
                        beltController.stopVibration(1);
                    }
                }
            });
        }

        // Vibrate at magnetic bearing
        vibrateAtMagneticBearingSlider =
                findViewById(R.id.activity_main_vibrate_at_magnetic_bearing_slider);
        if (vibrateAtMagneticBearingSlider != null) {
            vibrateAtMagneticBearingSlider.setOnSeekBarChangeListener(
                    new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    // Update button label
                    if (vibrateAtMagneticBearingStartButton != null) {
                        vibrateAtMagneticBearingStartButton.setText(getString(
                            R.string.activity_main_vibrate_at_magnetic_bearing_start_button_text,
                                vibrateAtMagneticBearingSlider.getProgress()));
                    }
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    // Nothing to do
                }
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    // Nothing to do
                }
            });
        }
        vibrateAtMagneticBearingStartButton =
                findViewById(R.id.activity_main_vibrate_at_magnetic_bearing_start_button);
        if (vibrateAtMagneticBearingStartButton != null) {
            vibrateAtMagneticBearingStartButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (vibrateAtMagneticBearingSlider != null && beltConnection != null &&
                            beltConnection.getState() ==
                                    BeltConnectionState.STATE_CONNECTED &&
                            beltController != null) {
                        if (beltController.getMode() != BeltMode.APP) {
                            beltController.changeMode(BeltMode.APP);
                        }
                        beltController.vibrateAtMagneticBearing(
                                vibrateAtMagneticBearingSlider.getProgress(),
                                null, // Default intensity
                                BeltVibrationSignal.CONTINUOUS,
                                2,
                                false);
                    }
                }
            });
        }

        // Stop vibration on channel 2
        Button stopVibrationCh2Button =
                findViewById(R.id.activity_main_vibrate_at_magnetic_bearing_stop_button);
        if (stopVibrationCh2Button != null) {
            stopVibrationCh2Button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (beltConnection != null &&
                            beltConnection.getState() ==
                                    BeltConnectionState.STATE_CONNECTED &&
                            beltController != null) {
                        beltController.stopVibration(2);
                    }
                }
            });
        }

        // Pulse at angle
        pulseAtAngleSlider = findViewById(R.id.activity_main_pulse_at_angle_slider);
        if (pulseAtAngleSlider != null) {
            pulseAtAngleSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    // Update button label
                    if (pulseAtAngleStartButton != null) {
                        pulseAtAngleStartButton.setText(getString(
                                R.string.activity_main_pulse_at_angle_start_button_text,
                                pulseAtAngleSlider.getProgress()));
                    }
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    // Nothing to do
                }
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    // Nothing to do
                }
            });
        }
        pulseAtAngleStartButton = findViewById(R.id.activity_main_pulse_at_angle_start_button);
        if (pulseAtAngleStartButton != null) {
            pulseAtAngleStartButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (pulseAtAngleSlider != null && beltConnection != null &&
                            beltConnection.getState() ==
                                    BeltConnectionState.STATE_CONNECTED &&
                            beltController != null) {
                        if (beltController.getMode() != BeltMode.APP) {
                            beltController.changeMode(BeltMode.APP);
                        }
                        beltController.vibrateAtAngle(pulseAtAngleSlider.getProgress(),
                                null, // Default intensity
                                BeltVibrationSignal.TURN_ONGOING,
                                3,
                                false);
                    }
                }
            });
        }

        // Stop vibration on channel 3
        Button stopVibrationCh3Button =
                findViewById(R.id.activity_main_pulse_at_angle_stop_button);
        if (stopVibrationCh3Button != null) {
            stopVibrationCh3Button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (beltConnection != null &&
                            beltConnection.getState() ==
                                    BeltConnectionState.STATE_CONNECTED &&
                            beltController != null) {
                        beltController.stopVibration(3);
                    }
                }
            });
        }

        // Stop vibration
        Button stopVibrationButton =
                findViewById(R.id.activity_main_stop_vibration_button);
        if (stopVibrationButton != null) {
            stopVibrationButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (beltConnection != null &&
                            beltConnection.getState() ==
                                    BeltConnectionState.STATE_CONNECTED &&
                            beltController != null) {
                        beltController.stopVibration();
                    }
                }
            });
        }

        // Pulse East
        Button pulseEastButton = findViewById(R.id.activity_main_pulse_east_button);
        if (pulseEastButton != null) {
            pulseEastButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (beltConnection != null &&
                            beltConnection.getState() ==
                                    BeltConnectionState.STATE_CONNECTED &&
                            beltController != null) {
                        beltController.pulseAtMagneticBearing(
                                90,
                                250,
                                500,
                                3,
                                null,
                                null,
                                null
                        );
                    }
                }
            });
        }

        // Pulse Left
        Button pulseLeftButton = findViewById(R.id.activity_main_pulse_left_button);
        if (pulseLeftButton != null) {
            pulseLeftButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (beltConnection != null &&
                            beltConnection.getState() ==
                                    BeltConnectionState.STATE_CONNECTED &&
                            beltController != null) {
                        beltController.pulseAtAngle(
                                270,
                                100,
                                250,
                                2,
                                null,
                                null,
                                null
                        );
                    }
                }
            });
        }

        // Single beep
        Button singleBeepButton = findViewById(R.id.activity_main_single_beep_button);
        if (singleBeepButton != null) {
            singleBeepButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (beltConnection != null &&
                            beltConnection.getState() ==
                                    BeltConnectionState.STATE_CONNECTED &&
                            beltController != null) {
                        beltController.playSound(BeltSound.SINGLE_SHORT_BEEP);
                    }
                }
            });
        }

        // Tone pattern
        Button tonePatternButton = findViewById(R.id.activity_main_tone_pattern_button);
        if (tonePatternButton != null) {
            tonePatternButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (beltConnection != null &&
                            beltConnection.getState() ==
                                    BeltConnectionState.STATE_CONNECTED &&
                            beltController != null) {
                        beltController.playSound(BeltSound.SUCCESS);
                    }
                }
            });
        }


    }

    /**
     * Returns the connection interface singleton.
     * @return the connection interface singleton.
     */
    public synchronized static BeltConnectionInterface getBeltConnection() {
        return beltConnection;
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            // Remove listeners
            beltConnection.removeConnectionListener(this);
            beltController.removeCommandListener(this);
            beltController.removeCommunicationListener(this);
            // Disconnect the belt
            beltConnection.disconnect();
        }
    }

    @Override
    public void onScanFailed() {
        showToast(getString(R.string.toast_scan_failed));
    }

    @Override
    public void onNoBeltFound() {
        showToast(getString(R.string.toast_no_belt_found));
    }

    @Override
    public void onBeltFound(@NonNull BluetoothDevice belt) {
        // Nothing to do
    }

    @Override
    public void onConnectionStateChange(BeltConnectionState state) {
        if (state != BeltConnectionState.STATE_CONNECTED) {
            beltOrientation = null;
            beltBatteryStatus = null;
            beltController.setOrientationNotificationsActive(true);
        }
        updateUI();
    }

    @Override
    public void onConnectionLost() {
        showToast(getString(R.string.toast_connection_lost));
    }

    @Override
    public void onConnectionFailed() {
        showToast(getString(R.string.toast_connection_failed));
    }

    @Override
    public void onBeltModeChanged(BeltMode mode) {
        updateUI();
    }

    @Override
    public void onBeltButtonPressed(@NonNull BeltButtonPressEvent beltButtonPressEvent) {
        showToast(getString(R.string.toast_belt_button_click_text));
        updateUI();
    }

    @Override
    public void onBeltDefaultVibrationIntensityChanged(int intensity) {
        updateUI();
    }

    @Override
    public void onBeltBatteryStatusUpdated(BeltBatteryStatus status) {
        beltBatteryStatus = status;
        updateBatteryUI();
    }

    @Override
    public void onBeltParameterValueNotified(@NonNull BeltParameter beltParameter,
                                             @NonNull Object parameterValue) {
        // Nothing to do
    }

    @Override
    public void onBeltOrientationUpdated(@NonNull BeltOrientation orientation) {
        beltOrientation = orientation;
        updateOrientationUI();
    }

    @Override
    public void onBeltCompassAccuracySignalStateNotified(boolean signalEnabled) {
        // Nothing to do
    }

    @Override
    public void onBluetoothActivated() {
        if (beltConnection != null &&
                beltConnection.getState() ==
                        BeltConnectionState.STATE_DISCONNECTED) {
            // BT active, continue with the connection or scan
            if (connectOnBtActivated) {
                beltConnection.scanAndConnect();
            } else {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                SearchDialogFragment dialog = new SearchDialogFragment();
                dialog.show(ft, "SearchDialogFragment");
            }
        }
    }

    @Override
    public void onBluetoothActivationRejected() {
        showToast(getString(R.string.toast_bt_activation_rejected_text));
    }

    @Override
    public void onBluetoothActivationFailed() {
        showToast(getString(R.string.toast_bt_activation_failed_text));
    }

    @Override
    public void onBeltSelected(@NonNull BluetoothDevice device) {
        if (beltConnection != null) {
            beltConnection.connect(device);
        }
    }

    /**
     * Updates UI components according to the belt state and data.
     */
    private void updateUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Connection state
                if (connectionStateLabel != null && beltConnection != null) {
                    connectionStateLabel.setText(
                            beltConnection.getState().toString());
                    if (DEBUG) Log.i(DEBUG_TAG,"MainActivity: Update connection label to: "+
                            beltConnection.getState().toString());
                }
                // Firmware version
                if (firmwareVersionLabel != null) {
                    if (beltController != null && beltController.getFirmwareVersion() != null) {
                        firmwareVersionLabel.setText(
                                integerFormat.format(beltController.getFirmwareVersion()));
                    } else {
                        firmwareVersionLabel.setText("-");
                    }
                }
                // Current belt mode
                if (currentBeltModeLabel != null && beltController != null) {
                    currentBeltModeLabel.setText(beltController.getMode().toString());
                }
                // Default vibration intensity
                if (defaultIntensityLabel != null) {
                    if (beltController != null && beltController.getDefaultVibrationIntensity() != null) {
                        defaultIntensityLabel.setText(integerPercentFormat.format(
                                beltController.getDefaultVibrationIntensity()));
                    } else {
                        defaultIntensityLabel.setText("-");
                    }
                }
                // Orientation
                updateOrientationUI();
                updateBatteryUI();
            }
        });
    }

    /**
     * Updates Orientation-related UI components.
     */
    private void updateOrientationUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (beltHeadingLabel != null) {
                    if (beltOrientation == null) {
                        beltHeadingLabel.setText("-");
                    } else {
                        beltHeadingLabel.setText(
                                integerDegreeFormat.format(beltOrientation.getBeltHeading()));
                    }
                }
                if (orientationAccurateLabel != null) {
                    if (beltOrientation == null ||
                            beltOrientation.isOrientationAccurate() == null) {
                        orientationAccurateLabel.setText("-");
                        orientationAccurateLabel.setBackgroundColor(
                                ResourcesCompat.getColor(getResources(),
                                        R.color.colorBgUnknown, null));
                    } else {
                        orientationAccurateLabel.setText(
                                beltOrientation.isOrientationAccurate().toString());
                        if (beltOrientation.isOrientationAccurate()) {
                            orientationAccurateLabel.setBackgroundColor(
                                    ResourcesCompat.getColor(getResources(),
                                            R.color.colorBgAccurate, null));
                        } else {
                            orientationAccurateLabel.setBackgroundColor(
                                    ResourcesCompat.getColor(getResources(),
                                            R.color.colorBgInaccurate, null));
                        }
                    }
                }
                if (boxHeadingLabel != null) {
                    if (beltOrientation == null) {
                        boxHeadingLabel.setText("-");
                    } else {
                        boxHeadingLabel.setText(
                                integerDegreeFormat.format(beltOrientation.getControlBoxHeading()));
                    }
                }
                if (boxPitchLabel != null) {
                    if (beltOrientation == null) {
                        boxPitchLabel.setText("-");
                    } else {
                        boxPitchLabel.setText(
                                integerDegreeFormat.format(beltOrientation.getControlBoxPitch()));
                    }
                }
                if (boxRollLabel != null) {
                    if (beltOrientation == null) {
                        boxRollLabel.setText("-");
                    } else {
                        boxRollLabel.setText(
                                integerDegreeFormat.format(beltOrientation.getControlBoxRoll()));
                    }
                }
                if (fusionStatusLabel != null) {
                    if (beltOrientation == null || beltOrientation.getFusionStatus() == null ||
                            beltOrientation.getFusionStatus() < 0) {
                        fusionStatusLabel.setText("-");
                        fusionStatusLabel.setBackgroundColor(
                                ResourcesCompat.getColor(getResources(),
                                        R.color.colorBgUnknown, null));
                    } else {
                        fusionStatusLabel.setText(
                                integerFormat.format(beltOrientation.getFusionStatus()));
                        if (beltOrientation.getFusionStatus() <= 1) {
                            fusionStatusLabel.setBackgroundColor(
                                    ResourcesCompat.getColor(getResources(),
                                            R.color.colorBgLowAccuracy, null));
                        } else if (beltOrientation.getFusionStatus() == 2) {
                            fusionStatusLabel.setBackgroundColor(
                                    ResourcesCompat.getColor(getResources(),
                                            R.color.colorBgAverageAccuracy, null));
                        } else if (beltOrientation.getFusionStatus() >= 3) {
                            fusionStatusLabel.setBackgroundColor(
                                    ResourcesCompat.getColor(getResources(),
                                            R.color.colorBgHighAccuracy, null));
                        }
                    }
                }
                if (magStatusLabel != null) {
                    if (beltOrientation == null ||
                            beltOrientation.getMagnetometerStatus() == null ||
                            beltOrientation.getMagnetometerStatus() < 0) {
                        magStatusLabel.setText("-");
                        magStatusLabel.setBackgroundColor(
                                ResourcesCompat.getColor(getResources(),
                                        R.color.colorBgUnknown, null));
                    } else {
                        magStatusLabel.setText(
                                integerFormat.format(beltOrientation.getMagnetometerStatus()));
                        if (beltOrientation.getMagnetometerStatus() <= 1) {
                            magStatusLabel.setBackgroundColor(
                                    ResourcesCompat.getColor(getResources(),
                                            R.color.colorBgLowAccuracy, null));
                        } else if (beltOrientation.getMagnetometerStatus() == 2) {
                            magStatusLabel.setBackgroundColor(
                                    ResourcesCompat.getColor(getResources(),
                                            R.color.colorBgAverageAccuracy, null));
                        } else {
                            magStatusLabel.setBackgroundColor(
                                    ResourcesCompat.getColor(getResources(),
                                            R.color.colorBgHighAccuracy, null));
                        }
                    }
                }
                if (accuracyLabel != null) {
                    if (beltOrientation == null || beltOrientation.getAccuracy() == null ||
                            beltOrientation.getAccuracy() < 0) {
                        accuracyLabel.setText("-");
                    } else {
                        accuracyLabel.setText(integerDegreeFormat.format(
                                beltOrientation.getAccuracy()));
                        if (beltOrientation.getAccuracy() > 22) {
                            accuracyLabel.setBackgroundColor(
                                    ResourcesCompat.getColor(getResources(),
                                            R.color.colorBgLowAccuracy, null));
                        } else if (beltOrientation.getAccuracy() > 11) {
                            accuracyLabel.setBackgroundColor(
                                    ResourcesCompat.getColor(getResources(),
                                            R.color.colorBgAverageAccuracy, null));
                        } else {
                            accuracyLabel.setBackgroundColor(
                                    ResourcesCompat.getColor(getResources(),
                                            R.color.colorBgHighAccuracy, null));
                        }
                    }
                }
            }
        });
    }

    /**
     * Updates battery-related UI components.
     */
    private void updateBatteryUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (powerStatusLabel != null) {
                    if (beltBatteryStatus == null) {
                        powerStatusLabel.setText("-");
                    } else {
                        powerStatusLabel.setText(beltBatteryStatus.getPowerStatus().toString());
                    }
                }
                if (batteryLevelLabel != null) {
                    if (beltBatteryStatus == null) {
                        batteryLevelLabel.setText("-");
                    } else {
                        batteryLevelLabel.setText(integerPercentFormat.format(
                                (int)(beltBatteryStatus.getLevel())));
                    }
                }
                if (tteTtfLabel != null) {
                    if (beltBatteryStatus == null) {
                        tteTtfLabel.setText("-");
                    } else {
                        tteTtfLabel.setText(DateUtils.formatElapsedTime(
                                (long)(beltBatteryStatus.getTteTtf())));
                    }
                }
                if (currentLabel != null) {
                    if (beltBatteryStatus == null) {
                        currentLabel.setText("-");
                    } else {
                        currentLabel.setText(integerFormat.format(
                                (int)(beltBatteryStatus.getCurrent())));
                    }
                }
            }
        });
    }

    /**
     * Shows a toast.
     *
     * @param message The message to display in the toast.
     */
    private void showToast(@NonNull final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

}
