/*
 * Copyright feelSpace GmbH, 2018-2019
 *
 * @author David Meignan
 */
package de.feelspace.fslibandroiddemo;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;

import de.feelspace.fslib.BeltConnectionState;
import de.feelspace.fslib.BeltVibrationSignal;
import de.feelspace.fslib.BluetoothActivationFragment;
import de.feelspace.fslib.NavigationController;
import de.feelspace.fslib.NavigationEventListener;
import de.feelspace.fslib.NavigationState;
import de.feelspace.fslib.OnBluetoothActivationCallback;
import de.feelspace.fslib.PowerStatus;

public class MainActivity extends AppCompatActivity implements OnBluetoothActivationCallback,
        NavigationEventListener {
    // Debug
    @SuppressWarnings("unused")
    private static final String DEBUG_TAG = "FeelSpace-Debug";
    @SuppressWarnings("unused")
    private static final boolean DEBUG = true;

    // Belt navigation controller
    private NavigationController navigationController;

    // Fragment to check BT activation and check permissions
    private BluetoothActivationFragment bluetoothActivationFragment;
    protected static final String BLUETOOTH_ACTIVATION_FRAGMENT_TAG =
            "MainActivity.BLUETOOTH_ACTIVATION_FRAGMENT_TAG";

    // UI components
    private TextView connectionStateLabel;
    private TextView defaultIntensityLabel;
    private TextView beltHeadingLabel;
    private TextView orientationAccurateLabel;
    private TextView powerStatusLabel;
    private TextView batteryLevelLabel;
    private TextView navigationStateLabel;

    private SeekBar setIntensitySlider;
    private SeekBar navigationDirectionSlider;
    private SeekBar notificationOrientationSlider;
    private Switch magBearingNavigationSwitch;
    private Spinner navigationSignalTypeSpinner;
    private Button setIntensityButton;
    private Button enableCompassAccuracySignalButton;

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

        // Retrieve the navigation controller
        navigationController = new NavigationController(getApplicationContext(), false);

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
        defaultIntensityLabel = findViewById(R.id.activity_main_default_intensity_label);
        beltHeadingLabel = findViewById(R.id.activity_main_belt_heading_label);
        orientationAccurateLabel = findViewById(R.id.activity_main_orientation_accurate_label);
        powerStatusLabel = findViewById(R.id.activity_main_power_status_label);
        batteryLevelLabel = findViewById(R.id.activity_main_battery_level_label);
        navigationStateLabel = findViewById(R.id.activity_main_navigation_state_label);

        setIntensitySlider = findViewById(R.id.activity_main_set_intensity_slider);
        navigationDirectionSlider = findViewById(R.id.activity_main_navigation_direction_slider);
        notificationOrientationSlider = findViewById(R.id.activity_main_notify_direction_slider);
        magBearingNavigationSwitch = findViewById(R.id.activity_main_mag_bearing_navigation_switch);
        navigationSignalTypeSpinner = findViewById(R.id.activity_main_signal_type_spinner);
        setIntensityButton = findViewById(R.id.activity_main_set_intensity_button);
        enableCompassAccuracySignalButton = findViewById(
                R.id.activity_main_enable_compass_accuracy_signal_button);

        // *** UI event handlers ***

        // Search and connect button
        Button searchAndConnectButton = findViewById(R.id.activity_main_search_and_connect_button);
        if (searchAndConnectButton != null) {
            searchAndConnectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (navigationController.getConnectionState() ==
                                    BeltConnectionState.STATE_DISCONNECTED) {
                        // Check BT activation and permissions.
                        // Note: The search-and-connect procedure is started in the callback
                        // 'onBluetoothActivated'.
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
                    if (navigationController.getConnectionState() !=
                                    BeltConnectionState.STATE_DISCONNECTED) {
                        navigationController.disconnectBelt();
                    }
                }
            });
        }

        // Default intensity slider and button
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
        if (setIntensityButton != null) {
            setIntensityButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (setIntensitySlider != null && navigationController.getConnectionState() ==
                                    BeltConnectionState.STATE_CONNECTED) {
                        int intensity = setIntensitySlider.getProgress()+5;
                        navigationController.changeDefaultVibrationIntensity(intensity);
                    }
                }
            });
        }

        // Enable/Disable compass accuracy signal
        if (enableCompassAccuracySignalButton != null) {
            enableCompassAccuracySignalButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean signalEnabled = navigationController.isCompassAccuracySignalEnabled();
                    navigationController.setCompassAccuracySignal(!signalEnabled);
                    updateCompassAccuracySignalStateUI();
                }
            });
        }

        // Battery signal button
        Button batterySignalButton = findViewById(R.id.activity_main_notify_battery_button);
        if (batterySignalButton != null) {
            batterySignalButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (navigationController.getConnectionState() !=
                            BeltConnectionState.STATE_DISCONNECTED) {
                        navigationController.notifyBeltBatteryLevel();
                    }
                }
            });
        }

        // Navigation direction slider
        if (navigationDirectionSlider != null) {
            navigationDirectionSlider.setOnSeekBarChangeListener(
                    new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    // Nothing to do
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    // Nothing to do
                }
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    updateNavigation();
                }
            });
        }

        // Magnetic bearing switch
        if (magBearingNavigationSwitch != null) {
            magBearingNavigationSwitch.setOnCheckedChangeListener(
                    new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    updateNavigation();
                }
            });
        }

        // Signal type spinner
        if (navigationSignalTypeSpinner != null) {
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                    R.array.navigation_signal_types, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            navigationSignalTypeSpinner.setAdapter(adapter);
            navigationSignalTypeSpinner.setOnItemSelectedListener(
                    new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    updateNavigation();
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // Nothing to do
                }
            });
        }

        // Start navigation
        Button startNavigationButton = findViewById(R.id.activity_main_start_navigation_button);
        if (startNavigationButton != null && navigationDirectionSlider != null &&
                magBearingNavigationSwitch != null) {
            startNavigationButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startNavigation();
                }
            });
        }

        // Pause navigation
        Button pauseButton = findViewById(R.id.activity_main_pause_navigation_button);
        if (pauseButton != null) {
            pauseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigationController.pauseNavigation();
                }
            });
        }

        // Destination reached button
        Button destinationReachedButton = findViewById(
                R.id.activity_main_destination_reached_button);
        if (destinationReachedButton != null) {
            destinationReachedButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigationController.notifyDestinationReached(true);
                }
            });
        }

        // Stop navigation
        Button stopButton = findViewById(R.id.activity_main_stop_navigation_button);
        if (stopButton != null) {
            stopButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigationController.stopNavigation();
                }
            });
        }

        // Notify bearing button
        Button notifyBearingButton = findViewById(R.id.activity_main_notify_bearing_button);
        if (notifyBearingButton != null) {
            notifyBearingButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (notificationOrientationSlider != null) {
                        int direction = notificationOrientationSlider.getProgress();
                        navigationController.notifyDirection(direction, true);
                    }
                }
            });
        }

        // Notify direction button
        Button notifyDirectionButton = findViewById(R.id.activity_main_notify_direction_button);
        if (notifyDirectionButton != null) {
            notifyDirectionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (notificationOrientationSlider != null) {
                        int direction = notificationOrientationSlider.getProgress();
                        navigationController.notifyDirection(direction, false);
                    }
                }
            });
        }

        // Notify simple warning
        Button notifyWarning = findViewById(R.id.activity_main_notify_warning_button);
        if (notifyWarning != null) {
            notifyWarning.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigationController.notifyWarning(false);
                }
            });
        }

        // Notify critical warning
        Button notifyCriticalWarning = findViewById(
                R.id.activity_main_notify_warning_critical_button);
        if (notifyCriticalWarning != null) {
            notifyCriticalWarning.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigationController.notifyWarning(true);
                }
            });
        }

    }

    /**
     * Starts/Resume the navigation.
     */
    private void startNavigation() {
        int direction = navigationDirectionSlider.getProgress();
        boolean isMagBearing = magBearingNavigationSwitch.isChecked();
        BeltVibrationSignal signal = BeltVibrationSignal.CONTINUOUS;
        switch (navigationSignalTypeSpinner.getSelectedItemPosition()) {
            case 0:
                signal = BeltVibrationSignal.CONTINUOUS;
                break;
            case 1:
                signal = BeltVibrationSignal.NAVIGATION;
                break;
            case 2:
                signal = BeltVibrationSignal.APPROACHING_DESTINATION;
                break;
            case 3:
                signal = BeltVibrationSignal.TURN_ONGOING;
                break;
            case 4:
                signal = BeltVibrationSignal.NEXT_WAYPOINT_LONG_DISTANCE;
                break;
            case 5:
                signal = BeltVibrationSignal.NEXT_WAYPOINT_MEDIUM_DISTANCE;
                break;
            case 6:
                signal = BeltVibrationSignal.NEXT_WAYPOINT_SHORT_DISTANCE;
                break;
            case 7:
                signal = BeltVibrationSignal.NEXT_WAYPOINT_AREA_REACHED;
                break;
            case 8:
                signal = BeltVibrationSignal.DESTINATION_REACHED_REPEATED;
                break;
        }
        navigationController.startNavigation(direction, isMagBearing, signal);
    }

    /**
     * Updates the navigation signal.
     */
    private void updateNavigation() {
        int direction = navigationDirectionSlider.getProgress();
        boolean isMagBearing = magBearingNavigationSwitch.isChecked();
        BeltVibrationSignal signal = BeltVibrationSignal.CONTINUOUS;
        switch (navigationSignalTypeSpinner.getSelectedItemPosition()) {
            case 0:
                signal = BeltVibrationSignal.CONTINUOUS;
                break;
            case 1:
                signal = BeltVibrationSignal.NAVIGATION;
                break;
            case 2:
                signal = BeltVibrationSignal.APPROACHING_DESTINATION;
                break;
            case 3:
                signal = BeltVibrationSignal.TURN_ONGOING;
                break;
            case 4:
                signal = BeltVibrationSignal.NEXT_WAYPOINT_LONG_DISTANCE;
                break;
            case 5:
                signal = BeltVibrationSignal.NEXT_WAYPOINT_MEDIUM_DISTANCE;
                break;
            case 6:
                signal = BeltVibrationSignal.NEXT_WAYPOINT_SHORT_DISTANCE;
                break;
            case 7:
                signal = BeltVibrationSignal.NEXT_WAYPOINT_AREA_REACHED;
                break;
            case 8:
                signal = BeltVibrationSignal.DESTINATION_REACHED_REPEATED;
                break;
        }
        navigationController.updateNavigationSignal(direction, isMagBearing, signal);
    }

    @Override
    protected void onResume() {
        super.onResume();
        navigationController.addNavigationEventListener(this);
        updateUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        navigationController.removeNavigationEventListener(this);
        if (isFinishing()) {
            // Disconnect the belt
            navigationController.disconnectBelt();
        }
    }

    @Override
    public void onBluetoothActivated() {
        // Bluetooth is active, continue with connection
        navigationController.searchAndConnectBelt();
    }

    @Override
    public void onBluetoothActivationRejected() {
        showToast(getString(R.string.toast_bt_activation_rejected_text));
    }

    @Override
    public void onBluetoothActivationFailed() {
        showToast(getString(R.string.toast_bt_activation_failed_text));
    }

    /**
     * Updates UI components according to the belt state and data.
     */
    private void updateUI() {
        updateConnectionStateUI();
        updateCompassAccuracySignalStateUI();
        updateIntensityUI();
        updateOrientationUI();
        updateBatteryUI();
        updateNavigationStateUI();
    }

    /**
     * Updates connection state label.
     */
    private void updateConnectionStateUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Connection state label
                if (connectionStateLabel != null) {
                    connectionStateLabel.setText(
                            navigationController.getConnectionState().toString());
                }
            }
        });
    }

    /**
     *
     */
    private void updateCompassAccuracySignalStateUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Enable compass accuracy signal button
                if (enableCompassAccuracySignalButton != null) {
                    boolean signalEnabled = navigationController.isCompassAccuracySignalEnabled();
                    if (signalEnabled) {
                        enableCompassAccuracySignalButton.setText(
                                R.string.activity_main_disable_compass_accuracy_signal_button_text);
                    } else {
                        enableCompassAccuracySignalButton.setText(
                                R.string.activity_main_enable_compass_accuracy_signal_button_text);
                    }
                }
            }
        });
    }

    /**
     * Updates navigation state label.
     */
    private void updateNavigationStateUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Navigation state label
                if (navigationStateLabel != null) {
                    navigationStateLabel.setText(
                            navigationController.getNavigationState().toString());
                }
            }
        });
    }

    /**
     * Updates belt intensity label.
     */
    private void updateIntensityUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Default intensity label
                if (defaultIntensityLabel != null) {
                    Integer intensity = navigationController.getDefaultVibrationIntensity();
                    if (intensity == null) {
                        defaultIntensityLabel.setText("-");
                    } else {
                        defaultIntensityLabel.setText(integerPercentFormat.format(intensity));
                    }
                }
            }
        });
    }

    /**
     * Updates belt orientation labels.
     */
    private void updateOrientationUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (beltHeadingLabel != null) {
                    Integer beltHeading = navigationController.getBeltHeading();
                    if (beltHeading == null) {
                        beltHeadingLabel.setText("-");
                    } else {
                        beltHeadingLabel.setText(integerDegreeFormat.format(beltHeading));
                    }
                }
                if (orientationAccurateLabel != null) {
                    Boolean accurate = navigationController.isBeltOrientationAccurate();
                    if (accurate == null) {
                        orientationAccurateLabel.setText("-");
                        orientationAccurateLabel.setBackgroundColor(
                                ResourcesCompat.getColor(getResources(),
                                        R.color.colorBgUnknown, null));
                    } else if (accurate) {
                        orientationAccurateLabel.setText(accurate.toString());
                        orientationAccurateLabel.setBackgroundColor(
                                ResourcesCompat.getColor(getResources(),
                                        R.color.colorBgAccurate, null));
                    } else {
                        orientationAccurateLabel.setText(accurate.toString());
                        orientationAccurateLabel.setBackgroundColor(
                                ResourcesCompat.getColor(getResources(),
                                        R.color.colorBgInaccurate, null));
                    }
                }
            }
        });
    }

    /**
     * Updates battery labels.
     */
    private void updateBatteryUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (powerStatusLabel != null) {
                    PowerStatus powerStatus = navigationController.getBeltPowerStatus();
                    if (powerStatus == null) {
                        powerStatusLabel.setText("-");
                    } else {
                        powerStatusLabel.setText(powerStatus.toString());
                    }
                }
                if (batteryLevelLabel != null) {
                    Integer batteryLevel = navigationController.getBeltBatteryLevel();
                    if (batteryLevel == null) {
                        batteryLevelLabel.setText("-");
                    } else {
                        batteryLevelLabel.setText(integerPercentFormat.format(batteryLevel));
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

    @Override
    public void onNavigationStateChanged(NavigationState state) {
        updateNavigationStateUI();
    }

    @Override
    public void onBeltHomeButtonPressed(boolean navigating) {
        if (navigating) {
            showToast(getString(R.string.toast_home_button_pressed_in_navigation_text));
        } else {
            showToast(getString(R.string.toast_home_button_pressed_text));
        }
    }

    @Override
    public void onBeltDefaultVibrationIntensityChanged(int intensity) {
        updateIntensityUI();
    }

    @Override
    public void onBeltOrientationUpdated(int beltHeading, boolean accurate) {
        updateOrientationUI();
    }

    @Override
    public void onBeltBatteryLevelUpdated(int batteryLevel, PowerStatus status) {
        updateBatteryUI();
    }

    @Override
    public void onBeltConnectionStateChanged(BeltConnectionState state) {
        updateUI();
    }

    @Override
    public void onBeltConnectionLost() {
        showToast(getString(R.string.toast_connection_lost));
    }

    @Override
    public void onBeltConnectionFailed() {
        showToast(getString(R.string.toast_connection_failed));
    }

    @Override
    public void onNoBeltFound() {
        showToast(getString(R.string.toast_no_belt_found));
    }
}
