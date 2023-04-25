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
        NavigationEventListener {

    // Application controller
    private AppController appController;

    // TODO TBR
//    // For permission and pairing
//    private static final int ENABLE_LOCATION_PERMISSION_CODE = 5;
//    private static final int BLUETOOTH_CONNECT_PERMISSION_CODE = 6;
//    private static final int SELECT_DEVICE_REQUEST_CODE = 7;
//    private Executor executor = new Executor() {
//        @Override
//        public void execute(Runnable runnable) {
//            runnable.run();
//        }
//    };
//    private PairingStatusBroadcastReceiver pairingStatusBroadcastReceiver;

    // UI components
    private Button connectButton;
    private Button disconnectButton;
    private TextView connectionStateTextView;

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

        // TODO TBR
//        // Receiver for pairing events
//        pairingStatusBroadcastReceiver = new PairingStatusBroadcastReceiver();
//        pairingStatusBroadcastReceiver.register();
//
//        // Set navigation controller
//        navigationController = new NavigationController(getApplicationContext());

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

        // Update UI
        updateUI();
    }

    // TODO TBR
//
//    @Override
//    protected void onDestroy() {
//        if (pairingStatusBroadcastReceiver != null) {
//            pairingStatusBroadcastReceiver.unregister();
//        }
//        super.onDestroy();
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        if (requestCode == SELECT_DEVICE_REQUEST_CODE) {
//            if (resultCode == Activity.RESULT_OK && data != null) {
//                try {
//                    android.bluetooth.le.ScanResult deviceToPair = data.getParcelableExtra(
//                            CompanionDeviceManager.EXTRA_DEVICE
//                    );
//                    BluetoothDevice ble = deviceToPair.getDevice();
//                    if (Build.VERSION.SDK_INT <= 30) {
//                        if (ActivityCompat.checkSelfPermission(
//                                this, Manifest.permission.ACCESS_FINE_LOCATION) !=
//                                PackageManager.PERMISSION_GRANTED) {
//                            showToast("No location permission to connect!");
//                            return;
//                        }
//                        ble.createBond();
//                    } else {
//                        if (ActivityCompat.checkSelfPermission(
//                                this, android.Manifest.permission.BLUETOOTH_CONNECT) !=
//                                PackageManager.PERMISSION_GRANTED) {
//                            showToast("No BLE connect permission to connect!");
//                            return;
//                        }
//                        ble.createBond();
//                    }
//                } catch (Exception e) {
//                    showToast("Error when retrieving BLE device!");
//                }
//            }
//        } else {
//            super.onActivityResult(requestCode, resultCode, data);
//        }
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == ENABLE_LOCATION_PERMISSION_CODE) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                startConnectionProcedure();
//            } else {
//                showToast("Location permission rejected!");
//            }
//        } else if (requestCode == BLUETOOTH_CONNECT_PERMISSION_CODE) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                startConnectionProcedure();
//            } else {
//                showToast("BLE connection permission rejected!");
//            }
//        }
//    }

    // MARK: Private methods

    // TODO TBR
//    private void checkPermissionAndConnect() {
//
//        if (Build.VERSION.SDK_INT <= 30) {
//
//            if (ActivityCompat.checkSelfPermission(
//                    this, Manifest.permission.ACCESS_FINE_LOCATION) !=
//                    PackageManager.PERMISSION_GRANTED) {
//                requestPermissions(new String[]{
//                        Manifest.permission.ACCESS_FINE_LOCATION},
//                        ENABLE_LOCATION_PERMISSION_CODE);
//            } else {
//                startConnectionProcedure();
//            }
//
//        } else {
//
//            if (ActivityCompat.checkSelfPermission(
//                    this, Manifest.permission.BLUETOOTH_CONNECT) !=
//                    PackageManager.PERMISSION_GRANTED) {
//                requestPermissions(new String[]{
//                                Manifest.permission.BLUETOOTH_CONNECT},
//                        BLUETOOTH_CONNECT_PERMISSION_CODE);
//            } else {
//                startConnectionProcedure();
//            }
//
//        }
//    }

    // TODO TBR
//    private void startConnectionProcedure() {
//
//        // From: https://developer.android.com/guide/topics/connectivity/companion-device-pairing#implement
//        if (Build.VERSION.SDK_INT >= 33) {
//
//            CompanionDeviceManager deviceManager =
//                    (CompanionDeviceManager) getSystemService(
//                            Context.COMPANION_DEVICE_SERVICE
//                    );
//
//            // To skip filtering based on name and supported feature flags,
//            // do not include calls to setNamePattern() and addServiceUuid(),
//            // respectively. This example uses Bluetooth.
//            BluetoothLeDeviceFilter deviceFilter =
//                    new BluetoothLeDeviceFilter.Builder()
//                            .setNamePattern(Pattern.compile("(?i)naviguertel.*"))
//                            .build();
//
//            // The argument provided in setSingleDevice() determines whether a single
//            // device name or a list of device names is presented to the user as
//            // pairing options.
//            AssociationRequest pairingRequest = new AssociationRequest.Builder()
//                    .addDeviceFilter(deviceFilter)
//                    .setSingleDevice(true)
//                    .build();
//
//            // When the app tries to pair with the Bluetooth device, show the
//            // appropriate pairing request dialog to the user.
//            deviceManager.associate(pairingRequest, executor, new CompanionDeviceManager.Callback() {
//
//                // Called when a device is found. Launch the IntentSender so the user can
//                // select the device they want to pair with.
//                @Override
//                public void onDeviceFound(IntentSender chooserLauncher) {
//                    try {
//                        startIntentSenderForResult(
//                                chooserLauncher, SELECT_DEVICE_REQUEST_CODE, null, 0, 0, 0
//                        );
//                    } catch (IntentSender.SendIntentException e) {
//                        Log.e("MainActivity", "Failed to send intent");
//                    }
//                }
//
//                @Override
//                public void onAssociationCreated(AssociationInfo associationInfo) {
//                    // AssociationInfo object is created and get association id and the
//                    // macAddress.
//                    int associationId = associationInfo.getId();
//                    MacAddress macAddress = associationInfo.getDeviceMacAddress();
//                }
//
//                @Override
//                public void onFailure(CharSequence errorMessage) {
//                    // Handle the failure.
//                    showToast("Belt not found!");
//                }
//
//            });
//
//        } else {
//
//            CompanionDeviceManager deviceManager =
//                    (CompanionDeviceManager) getSystemService(
//                            Context.COMPANION_DEVICE_SERVICE
//                    );
//
//            // To skip filtering based on name and supported feature flags,
//            // don't include calls to setNamePattern() and addServiceUuid(),
//            // respectively. This example uses Bluetooth.
//            BluetoothLeDeviceFilter deviceFilter =
//                    new BluetoothLeDeviceFilter.Builder()
//                            .setNamePattern(Pattern.compile("(?i)naviguertel.*"))
//                            .build();
//
//            // The argument provided in setSingleDevice() determines whether a single
//            // device name or a list of device names is presented to the user as
//            // pairing options.
//            AssociationRequest pairingRequest = new AssociationRequest.Builder()
//                    .addDeviceFilter(deviceFilter)
//                    .setSingleDevice(true)
//                    .build();
//
//            // When the app tries to pair with the Bluetooth device, show the
//            // appropriate pairing request dialog to the user.
//            deviceManager.associate(pairingRequest,
//                    new CompanionDeviceManager.Callback() {
//                        @Override
//                        public void onDeviceFound(IntentSender chooserLauncher) {
//                            try {
//                                startIntentSenderForResult(chooserLauncher,
//                                        SELECT_DEVICE_REQUEST_CODE, null, 0, 0, 0);
//                            } catch (IntentSender.SendIntentException e) {
//                                // failed to send the intent
//                                showToast("Scan request failed!");
//                            }
//                        }
//
//                        @Override
//                        public void onFailure(CharSequence error) {
//                            // handle failure to find the companion device
//                            showToast("Belt not found!");
//                        }
//                    }, null);
//
//        }
//    }

    private void showToast(final String message) {
        runOnUiThread(() -> Toast.makeText(
                MainActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    private void updateUI() {
        updateConnectionLabel();
        updateConnectionButtons();
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

    // TODO TBR
//    // MARK: Pairing status broadcast receiver
//
//    private class PairingStatusBroadcastReceiver extends BroadcastReceiver {
//
//        private boolean registered = false;
//
//        public void register() {
//            if (!registered) {
//                try {
//                    MainActivity.this.registerReceiver(this,
//                            new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
//                    registered = true;
//                } catch (Exception e) {
//                    // TODO Log error
//                }
//            }
//        }
//
//        public void unregister() {
//            if (registered) {
//                try {
//                    MainActivity.this.unregisterReceiver(this);
//                    registered = false;
//                } catch (Exception e) {
//                    // TODO Log error
//                }
//            }
//        }
//
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            if (intent.getAction() != null &&
//                    intent.getAction().matches(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
//                // TODO get extra `EXTRA_BOND_STATE`
//                // If value is BOND_BONDED, connect belt
//                // Device is given in EXTRA_DEVICE
//                // Add a property pendingBelt to check that it is the expected device.
//            }
//            // TODO connect!
//        }
//
//    }
}
