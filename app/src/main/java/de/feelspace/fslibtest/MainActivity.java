package de.feelspace.fslibtest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanFilter;
import android.companion.AssociationInfo;
import android.companion.AssociationRequest;
import android.companion.BluetoothLeDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.net.MacAddress;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.Executor;
import java.util.regex.Pattern;

import de.feelspace.fslib.BeltConnectionState;
import de.feelspace.fslib.NavigationController;
import de.feelspace.fslib.NavigationEventListener;
import de.feelspace.fslib.NavigationState;
import de.feelspace.fslib.PowerStatus;

public class MainActivity extends AppCompatActivity implements NavigationEventListener {

    // Belt navigation controller
    private NavigationController navigationController;

    // For permission and pairing
    private static final int ENABLE_LOCATION_PERMISSION_CODE = 5;
    private static final int BLUETOOTH_CONNECT_PERMISSION_CODE = 6;
    private static final int SELECT_DEVICE_REQUEST_CODE = 7;
    private Executor executor = new Executor() {
        @Override
        public void execute(Runnable runnable) {
            runnable.run();
        }
    };
    private PairingStatusBroadcastReceiver pairingStatusBroadcastReceiver;

    // UI components
    private Button connectButton;
    private Button disconnectButton;
    private TextView connectionStateTextView;

    // MARK: Activity methods overriding

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Receiver for pairing events
        pairingStatusBroadcastReceiver = new PairingStatusBroadcastReceiver();
        pairingStatusBroadcastReceiver.register();

        // Set navigation controller
        navigationController = new NavigationController(getApplicationContext());

        // Connection state
        connectionStateTextView = findViewById(R.id.activity_main_connection_state_text_view);

        // Connect button
        connectButton = findViewById(R.id.activity_main_connect_button);
        connectButton.setOnClickListener(view -> checkPermissionAndConnect());


        // Disconnect button
        disconnectButton = findViewById(R.id.activity_main_disconnect_button);
        disconnectButton.setOnClickListener(view -> navigationController.disconnectBelt());

        // Update UI
        updateConnectionLabel();
    }

    @Override
    protected void onDestroy() {
        if (pairingStatusBroadcastReceiver != null) {
            pairingStatusBroadcastReceiver.unregister();
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == SELECT_DEVICE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                try {
                    android.bluetooth.le.ScanResult deviceToPair = data.getParcelableExtra(
                            CompanionDeviceManager.EXTRA_DEVICE
                    );
                    BluetoothDevice ble = deviceToPair.getDevice();
                    if (Build.VERSION.SDK_INT <= 30) {
                        if (ActivityCompat.checkSelfPermission(
                                this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                                PackageManager.PERMISSION_GRANTED) {
                            showToast("No location permission to connect!");
                            return;
                        }
                        ble.createBond();
                    } else {
                        if (ActivityCompat.checkSelfPermission(
                                this, android.Manifest.permission.BLUETOOTH_CONNECT) !=
                                PackageManager.PERMISSION_GRANTED) {
                            showToast("No BLE connect permission to connect!");
                            return;
                        }
                        ble.createBond();
                    }
                } catch (Exception e) {
                    showToast("Error when retrieving BLE device!");
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ENABLE_LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startConnectionProcedure();
            } else {
                showToast("Location permission rejected!");
            }
        } else if (requestCode == BLUETOOTH_CONNECT_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startConnectionProcedure();
            } else {
                showToast("BLE connection permission rejected!");
            }
        }
    }

    // MARK: Private methods

    private void checkPermissionAndConnect() {

        if (Build.VERSION.SDK_INT <= 30) {

            if (ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION},
                        ENABLE_LOCATION_PERMISSION_CODE);
            } else {
                startConnectionProcedure();
            }

        } else {

            if (ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.BLUETOOTH_CONNECT) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                                Manifest.permission.BLUETOOTH_CONNECT},
                        BLUETOOTH_CONNECT_PERMISSION_CODE);
            } else {
                startConnectionProcedure();
            }

        }
    }

    private void startConnectionProcedure() {

        // From: https://developer.android.com/guide/topics/connectivity/companion-device-pairing#implement
        if (Build.VERSION.SDK_INT >= 33) {

            CompanionDeviceManager deviceManager =
                    (CompanionDeviceManager) getSystemService(
                            Context.COMPANION_DEVICE_SERVICE
                    );

            // To skip filtering based on name and supported feature flags,
            // do not include calls to setNamePattern() and addServiceUuid(),
            // respectively. This example uses Bluetooth.
            BluetoothLeDeviceFilter deviceFilter =
                    new BluetoothLeDeviceFilter.Builder()
                            .setNamePattern(Pattern.compile("(?i)naviguertel.*"))
                            .build();

            // The argument provided in setSingleDevice() determines whether a single
            // device name or a list of device names is presented to the user as
            // pairing options.
            AssociationRequest pairingRequest = new AssociationRequest.Builder()
                    .addDeviceFilter(deviceFilter)
                    .setSingleDevice(true)
                    .build();

            // When the app tries to pair with the Bluetooth device, show the
            // appropriate pairing request dialog to the user.
            deviceManager.associate(pairingRequest, executor, new CompanionDeviceManager.Callback() {

                // Called when a device is found. Launch the IntentSender so the user can
                // select the device they want to pair with.
                @Override
                public void onDeviceFound(IntentSender chooserLauncher) {
                    try {
                        startIntentSenderForResult(
                                chooserLauncher, SELECT_DEVICE_REQUEST_CODE, null, 0, 0, 0
                        );
                    } catch (IntentSender.SendIntentException e) {
                        Log.e("MainActivity", "Failed to send intent");
                    }
                }

                @Override
                public void onAssociationCreated(AssociationInfo associationInfo) {
                    // AssociationInfo object is created and get association id and the
                    // macAddress.
                    int associationId = associationInfo.getId();
                    MacAddress macAddress = associationInfo.getDeviceMacAddress();
                }

                @Override
                public void onFailure(CharSequence errorMessage) {
                    // Handle the failure.
                    showToast("Belt not found!");
                }

            });

        } else {

            CompanionDeviceManager deviceManager =
                    (CompanionDeviceManager) getSystemService(
                            Context.COMPANION_DEVICE_SERVICE
                    );

            // To skip filtering based on name and supported feature flags,
            // don't include calls to setNamePattern() and addServiceUuid(),
            // respectively. This example uses Bluetooth.
            BluetoothLeDeviceFilter deviceFilter =
                    new BluetoothLeDeviceFilter.Builder()
                            .setNamePattern(Pattern.compile("(?i)naviguertel.*"))
                            .build();

            // The argument provided in setSingleDevice() determines whether a single
            // device name or a list of device names is presented to the user as
            // pairing options.
            AssociationRequest pairingRequest = new AssociationRequest.Builder()
                    .addDeviceFilter(deviceFilter)
                    .setSingleDevice(true)
                    .build();

            // When the app tries to pair with the Bluetooth device, show the
            // appropriate pairing request dialog to the user.
            deviceManager.associate(pairingRequest,
                    new CompanionDeviceManager.Callback() {
                        @Override
                        public void onDeviceFound(IntentSender chooserLauncher) {
                            try {
                                startIntentSenderForResult(chooserLauncher,
                                        SELECT_DEVICE_REQUEST_CODE, null, 0, 0, 0);
                            } catch (IntentSender.SendIntentException e) {
                                // failed to send the intent
                                showToast("Scan request failed!");
                            }
                        }

                        @Override
                        public void onFailure(CharSequence error) {
                            // handle failure to find the companion device
                            showToast("Belt not found!");
                        }
                    }, null);

        }
    }

    private void showToast(final String message) {
        runOnUiThread(() -> Toast.makeText(
                MainActivity.this, message, Toast.LENGTH_SHORT).show());
    }
    private void updateConnectionLabel() {
        runOnUiThread(() -> {
            BeltConnectionState state = MainActivity.this.navigationController.getConnectionState();
            switch (state) {
                case STATE_DISCONNECTED:
                    connectionStateTextView.setText(R.string.disconnected);
                    break;
                case STATE_SCANNING:
                    connectionStateTextView.setText(R.string.scanning);
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
        updateConnectionLabel();
    }

    @Override
    public void onBeltConnectionLost() {
        showToast("Belt connection lost!");
    }

    @Override
    public void onBeltConnectionFailed() {
        showToast("Belt connection failes!");
    }

    @Override
    public void onNoBeltFound() {
        showToast("No belt found!");
    }

    // MARK: Pairing status broadcast receiver

    private class PairingStatusBroadcastReceiver extends BroadcastReceiver {

        private boolean registered = false;

        public void register() {
            if (!registered) {
                try {
                    MainActivity.this.registerReceiver(this,
                            new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
                    registered = true;
                } catch (Exception e) {
                    // TODO Log error
                }
            }
        }

        public void unregister() {
            if (registered) {
                try {
                    MainActivity.this.unregisterReceiver(this);
                    registered = false;
                } catch (Exception e) {
                    // TODO Log error
                }
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null &&
                    intent.getAction().matches(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                // TODO get extra `EXTRA_BOND_STATE`
                // If value is BOND_BONDED, connect belt
                // Device is given in EXTRA_DEVICE
                // Add a property pendingBelt to check that it is the expected device.
            }
            // TODO connect!
        }

    }
}
