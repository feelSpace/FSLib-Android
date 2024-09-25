/*
 * Copyright (c) 2017-2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */
package de.feelspace.fslib;

import static de.feelspace.fslib.GattConnectionState.GATT_CONNECTED;
import static de.feelspace.fslib.GattConnectionState.GATT_CONNECTING;
import static de.feelspace.fslib.GattConnectionState.GATT_DISCONNECTED;
import static de.feelspace.fslib.GattConnectionState.GATT_DISCONNECTING;
import static de.feelspace.fslib.GattConnectionState.GATT_DISCOVERING_SERVICES;
import static de.feelspace.fslib.GattConnectionState.GATT_PAIRING;
import static de.feelspace.fslib.GattConnectionState.GATT_RECONNECTING;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Encapsulation of the GATT server with the addition of a queue of operations.
 */
public class GattController extends BluetoothGattCallback implements BluetoothPairingManager.BluetoothPairingDelegate {

    // Debug
    @SuppressWarnings("unused")
    private static final String DEBUG_TAG = "FeelSpace-Debug";
    @SuppressWarnings("unused")
    private static final boolean DEBUG = true;

    // Connection state
    private @NonNull GattConnectionState connectionState =
            GattConnectionState.GATT_DISCONNECTED;

    // GATT server and device
    private @Nullable BluetoothGatt gattServer;
    private @Nullable BluetoothDevice device;
    private @Nullable Context context;

    // Pairing manager
    private BluetoothPairingManager pairingManager = null;

    // Operation queue
    private final @NonNull LinkedList<GattOperation> operationQueue = new LinkedList<>();
    private @Nullable GattOperation runningOperation;

    // Executor for callback and timeout
    private final @NonNull ScheduledThreadPoolExecutor executor;

    // Listeners
    private final @NonNull ArrayList<GattEventListener> listeners = new ArrayList<>();

    // Connection timeout
    public static final long CONNECTION_TIMEOUT_MS = 2000;
    private ScheduledFuture<?> connectionTimeoutTask;

    // Disconnection timeout
    public static final long DISCONNECTION_TIMEOUT_MS = 2000;
    private ScheduledFuture<?> disconnectionTimeoutTask;

    // Flag for connection termination
    private boolean initialConnection = true;
    private boolean connectionLost = false;
    private boolean connectionFailed = false;

    // Service discovery timeout
    public static final long SERVICE_DISCOVERY_TIMEOUT_MS = 10000; // 4000;
    private ScheduledFuture<?> serviceDiscoveryTimeoutTask;
    public static final long SERVICE_DISCOVERY_DELAY_MS = 1500; // 500 ?
    private static final boolean CLEAR_GATT_CACHE_ON_DISCOVERY_ERROR = false;

    public static final boolean SERVICE_DISCOVERY_RETRY = true;
    public static final int SERVICE_DISCOVERY_RETRY_PERIOD_MS = 5000;
    private ScheduledFuture<?> serviceDiscoveryRetryTask;

    // GATT supervision timeout
    public static final long GATT_SUPERVISION_TIMEOUT_MS = 6000;
    private ScheduledFuture<?> gattSupervisionTask;
    private long lastGattServerActivityTimeNano;
    private long gattSupervisionStartTimeNano;

    // Reconnection
    public static final long RECONNECTION_DELAY_MS = 2000;
    public static final int RECONNECTION_ATTEMPTS = 2;
    public static final int INITIAL_RECONNECTION_ATTEMPTS = 1;
    private ScheduledFuture<?> reconnectionTask;
    private int remainingReconnectionAttempts = 0;

    // Operation timeout
    public static final long GATT_OPERATION_TIMEOUT_MS = 500;
    private ScheduledFuture<?> gattOperationTimeoutTask;

    /**
     * Constructor.
     */
    GattController(@NonNull ScheduledThreadPoolExecutor executor) {
        this.executor = executor;
    }

    /**
     * Requests a connection to the GATT server for the given device.
     *
     * The state must be `GATT_DISCONNECTED` to start the connection.
     *
     * Instead of raising an exception or returning a termination code on failure, this method uses
     * the callback {@link GattEventListener#onGattConnectionFailed()}.
     *
     * @param context The context, because Android team decided that the BLE API is not awful
     *                enough. Fun fact, it does not seem to be used but for API compatibility it
     *                must be given.
     * @param device The device to connect to.
     */
    @SuppressLint("MissingPermission")
    public void connect(@NonNull Context context, @NonNull BluetoothDevice device) {
        if (DEBUG) Log.i(DEBUG_TAG, "GattController: Connection request.");
        List<GattOperation> canceledOperations = null;
        boolean success = true;
        // Initialize pairing manager
        if (pairingManager == null) {
            pairingManager = new BluetoothPairingManager(context, executor, this);
        }
        synchronized (this) {
            if (connectionState != GattConnectionState.GATT_DISCONNECTED) {
                // Do nothing if not disconnected
                if (DEBUG) Log.e(DEBUG_TAG, "GattController: Not disconnected to attempt a connection!");
                return;
            }
            connectionState = GATT_CONNECTING;
            remainingReconnectionAttempts = INITIAL_RECONNECTION_ATTEMPTS;
            initialConnection = true;
            connectionLost = false;
            connectionFailed = false;
            this.device = device;
            this.context = context;
            try {
                gattServer = device.connectGatt(context, false, this,
                        BluetoothDevice.TRANSPORT_LE, BluetoothDevice.PHY_LE_1M,
                        new Handler(Looper.getMainLooper()));
            } catch (Exception e) {
                Log.e(DEBUG_TAG, "GattController: Unable to call connection method for" +
                        " GATT server.", e);
                gattServer = null;
            }
            if (gattServer != null) {
                scheduleConnectionTimeout();
            } else {
                // Yep, 'connectGatt' can return 'null' but it is not documented
                // No reconnection attempt when an error occurs with the Bluetooth service
                Log.e(DEBUG_TAG, "GattController: Unable to connect to GATT server.");
                connectionState = GattConnectionState.GATT_DISCONNECTED;
                success = false;
            }
        }
        if (!success) {
            notifyConnectionFailed();
        }
        notifyGattConnectionStateChange();
    }

    /**
     * Schedules a connection timeout.
     */
    private void scheduleConnectionTimeout() {
        try {
            connectionTimeoutTask = executor.schedule(() -> {
                Log.w(DEBUG_TAG, "GattController: Connection timeout.");
                boolean reconnect = false;
                synchronized (GattController.this) {
                    connectionTimeoutTask = null;
                    if (connectionState != GATT_CONNECTING &&
                            connectionState != GATT_RECONNECTING) {
                        // Ignore timeout if not connecting or reconnecting
                        return;
                    }
                    if (remainingReconnectionAttempts > 0) {
                        remainingReconnectionAttempts--;
                        reconnect = true;
                    } else if (initialConnection) {
                        Log.w(DEBUG_TAG, "GattController: Set flag for connection failed.");
                        connectionFailed = true;
                    }
                }
                if (reconnect) {
                    if (DEBUG) Log.i(DEBUG_TAG, "GattController: New connection attempt after connection timeout.");
                    reconnect();
                } else {
                    if (DEBUG) Log.i(DEBUG_TAG, "GattController: Disconnecting after connection timeout.");
                    disconnect();
                }
            }, CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "GattController: Unable to schedule the connection timeout.");
        }
    }

    /**
     * Requests a disconnection of the GATT server.
     */
    @SuppressLint("MissingPermission")
    public void disconnect() {
        if (DEBUG) Log.i(DEBUG_TAG, "GattController: Disconnection request.");
        List<GattOperation> canceledOperations = null;
        synchronized (this) {
            if (connectionState == GATT_DISCONNECTED) {
                return;
            }
            cancelAllTimeoutTasks();
            canceledOperations = cancelAllGattOperations();
            connectionState = GATT_DISCONNECTING;
            if (gattServer != null) {
                // First disconnect, then wait asynchronous notification to close
                scheduleDisconnectionTimeout();
                gattServer.disconnect();
            }
        }
        if (canceledOperations != null) {
            for (GattOperation operation: canceledOperations) {
                notifyOperationCompletion(operation);
            }
        }
        notifyGattConnectionStateChange();
    }


    /**
     * Schedules a disconnection timeout.
     */
    @SuppressLint("MissingPermission")
    private void scheduleDisconnectionTimeout() {
        try {
            disconnectionTimeoutTask = executor.schedule(() -> {
                Log.w(DEBUG_TAG, "GattController: Disconnection timeout.");
                boolean lost;
                boolean failed;
                synchronized (GattController.this) {
                    disconnectionTimeoutTask = null;
                    if (connectionState != GATT_DISCONNECTING) {
                        // Ignore timeout if not disconnecting
                        return;
                    }
                    connectionState = GATT_DISCONNECTED;
                    lost = connectionLost;
                    failed = connectionFailed;
                    connectionLost = false;
                    connectionFailed = false;
                    if (gattServer != null) {
                        try {
                            gattServer.close();
                        } catch (Exception e) {
                            Log.e(DEBUG_TAG, "GattController: Unable to close GATT server!", e);
                        }
                        gattServer = null;
                    }
                }
                if (failed) {
                    notifyConnectionFailed();
                } else if (lost) {
                    notifyConnectionLost();
                }
                notifyGattConnectionStateChange();

            }, DISCONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "GattController: Unable to schedule the disconnection timeout.");
            connectionState = GATT_DISCONNECTED;
            if (connectionLost) {
                notifyConnectionLost();
            } else if (connectionFailed) {
                notifyConnectionFailed();
            }
            notifyGattConnectionStateChange();
        }
    }

    /**
     * Schedules a reconnection.
     */
    @SuppressLint("MissingPermission")
    protected void reconnect() {
        if (DEBUG) Log.i(DEBUG_TAG, "GattController: Schedule reconnection.");
        List<GattOperation> canceledOperations = null;
        synchronized (this) {
            // First, disconnect
            cancelAllTimeoutTasks();
            canceledOperations = cancelAllGattOperations();
            connectionState = GATT_RECONNECTING;
            // After `disconnect` an event will be received to close the GATT server
            if (gattServer != null) {
                gattServer.disconnect();
            }
            // Schedule reconnection
            scheduleReconnection();
        }
        if (canceledOperations != null) {
            for (GattOperation operation: canceledOperations) {
                notifyOperationCompletion(operation);
            }
        }
        notifyGattConnectionStateChange();
    }

    /**
     * Clears the GATT cached data.
     */
    protected void clearGattCache(BluetoothGatt gatt) {
        Log.w(DEBUG_TAG, "GattController: Clear GATT cache.");
        // From: https://stackoverflow.com/a/50745997/8477032
        try {
            final Method refresh = gatt.getClass().getMethod("refresh");
            if (refresh != null) {
                refresh.invoke(gatt);
                Thread.sleep(1000);
            } else {
                Log.w(DEBUG_TAG, "GattController: No method to clear GATT cache.");
            }
        } catch (Exception e) {
            Log.w(DEBUG_TAG, "GattController: Unable to clear GATT cache.");
        }
    }

    /**
     * Schedules a reconnection.
     */
    @SuppressLint("MissingPermission")
    private void scheduleReconnection() {
        try {
            reconnectionTask = executor.schedule(() -> {
                if (DEBUG) Log.i(DEBUG_TAG, "GattController: Reconnection attempt.");
                boolean lost = false;
                boolean failed = false;
                boolean reconnect = false;
                synchronized (GattController.this) {
                    reconnectionTask = null;
                    if (connectionState == GATT_RECONNECTING ||
                            connectionState == GATT_CONNECTING) {
                        try {
                            if (device != null) {
                                gattServer = device.connectGatt(context, false, GattController.this,
                                        BluetoothDevice.TRANSPORT_LE, BluetoothDevice.PHY_LE_1M,
                                        new Handler(Looper.getMainLooper()));
                            } else {
                                gattServer = null;
                            }
                        } catch (Exception e) {
                            Log.e(DEBUG_TAG, "GattController: Unable to call " +
                                    "connection method for GATT server.", e);
                            gattServer = null;
                        }
                        if (gattServer != null) {
                            scheduleConnectionTimeout();
                        } else {
                            // Yep, 'connectGatt' can return 'null' but it is not documented
                            // No reconnection attempt when an error occurs with the Bluetooth service
                            Log.e(DEBUG_TAG, "GattController: Unable to reconnect to " +
                                    "GATT server.");
                            // Schedule new reconnection
                            if (remainingReconnectionAttempts > 0) {
                                remainingReconnectionAttempts--;
                                reconnect = true;
                            } else if (initialConnection) {
                                failed = true;
                                connectionState = GATT_DISCONNECTED;
                            } else {
                                lost = true;
                                connectionState = GATT_DISCONNECTED;
                            }
                        }
                    }
                }
                if (reconnect) {
                    reconnect();
                } else if (failed) {
                    notifyConnectionFailed();
                    notifyGattConnectionStateChange();
                } else if (lost) {
                    notifyConnectionLost();
                    notifyGattConnectionStateChange();
                }
            }, RECONNECTION_DELAY_MS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "GattController: Unable to schedule the reconnection.", e);
            connectionState = GATT_DISCONNECTED;
            if (initialConnection) {
                notifyConnectionFailed();
                notifyGattConnectionStateChange();
            } else {
                notifyConnectionLost();
                notifyGattConnectionStateChange();
            }
        }
    }

    private void startServiceDiscovery() {
        if (DEBUG) Log.i(DEBUG_TAG, "GattController: Schedule service discovery.");
        synchronized (this) {
            // Change state
            cancelAllTimeoutTasks();
            connectionState = GATT_DISCOVERING_SERVICES;
            // Delayed service discovery in main UI thread
            Handler mainHandler = new Handler(Looper.getMainLooper());
            @SuppressLint("MissingPermission") Runnable disc = () -> {
                boolean reconnect = false;
                boolean disconnect = false;
                synchronized (this) {
                    if (connectionState != GATT_DISCOVERING_SERVICES) {
                        return;
                    }
                    if (DEBUG) Log.i(DEBUG_TAG, "GattController: Request service discovery.");
                    if (gattServer != null && gattServer.discoverServices()) {
                        scheduleServiceDiscoveryTimeout();
                        if (SERVICE_DISCOVERY_RETRY) {
                            scheduleServiceDiscoveryRetry();
                        }
                    } else {
                        if (DEBUG) Log.i(DEBUG_TAG, "GattController: Failed to request service discovery.");
                        if (remainingReconnectionAttempts > 0) {
                            remainingReconnectionAttempts--;
                            reconnect = true;
                        } else {
                            if (initialConnection) {
                                connectionFailed = true;
                            } else {
                                connectionLost = true;
                            }
                            disconnect = true;
                        }
                    }
                }
                if (reconnect) {
                    if (CLEAR_GATT_CACHE_ON_DISCOVERY_ERROR) {
                        Log.i(DEBUG_TAG, "GattController: Clear GATT cache after service discovery failure.");
                        clearGattCache(gattServer);
                    }
                    Log.i(DEBUG_TAG, "GattController: Reconnect after service discovery failure.");
                    reconnect();
                } else if (disconnect) {
                    if (CLEAR_GATT_CACHE_ON_DISCOVERY_ERROR) {
                        Log.i(DEBUG_TAG, "GattController: Clear GATT cache after service discovery failure.");
                        clearGattCache(gattServer);
                    }
                    Log.i(DEBUG_TAG, "GattController: Disconnect after service discovery failure.");
                    disconnect();
                }
            };
            mainHandler.postDelayed(disc, SERVICE_DISCOVERY_DELAY_MS);
        }
        notifyGattConnectionStateChange();
    }

    /**
     * Schedules a service discovery timeout.
     * On service discovery. Android caches services, and it's full of bugs. When service discovery
     * fails, we could try clearing the cache:
     * <a href="https://stackoverflow.com/a/50745997/8477032">How to refresh services / clear cache?</a>
     */
    private void scheduleServiceDiscoveryTimeout() {
        try {
            serviceDiscoveryTimeoutTask = executor.schedule(() -> {
                Log.e(DEBUG_TAG, "GattController: Service discovery timeout!");
                boolean reconnect = false;
                synchronized (GattController.this) {
                    serviceDiscoveryTimeoutTask = null;
                    if (connectionState != GATT_DISCOVERING_SERVICES) {
                        // Should not happen, ignore obsolete timeout
                        return;
                    }
                    if (remainingReconnectionAttempts > 0) {
                        remainingReconnectionAttempts--;
                        reconnect = true;
                    } else if (initialConnection) {
                        connectionFailed = true;
                    } else {
                        connectionLost = true;
                    }
                }
                // Try reconnection after possibly clearing GATT cache
                if (CLEAR_GATT_CACHE_ON_DISCOVERY_ERROR) {
                    Log.i(DEBUG_TAG, "GattController: Clear GATT cache after service discovery timeout.");
                    clearGattCache(gattServer);
                }
                if (reconnect) {
                    Log.i(DEBUG_TAG, "GattController: Reconnect after service discovery timeout.");
                    reconnect();
                } else {
                    Log.i(DEBUG_TAG, "GattController: Disconnect after service discovery timeout.");
                    disconnect();
                }
            }, SERVICE_DISCOVERY_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "GattController: Unable to schedule the service discovery" +
                    " timeout.");
        }
    }

    /**
     * Hey, it's so f***ing buggy on API 30 that we should implement stupidly complex things.
     */
    private void scheduleServiceDiscoveryRetry() {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.postDelayed(retryServiceDiscovery, SERVICE_DISCOVERY_RETRY_PERIOD_MS);
    }

    /**
     * Hey, try it again!
     */
    private final Runnable retryServiceDiscovery = new Runnable() {
        @Override
        public void run() {
            try {
                if (connectionState == GATT_DISCOVERING_SERVICES) {
                    try {
                        Log.w(DEBUG_TAG, "GattController: Retry service discovery.");
                        gattServer.discoverServices();
                    } catch (SecurityException s) {
                        Log.e(DEBUG_TAG, "GattController: No permission to retry service discovery!", s);
                    } catch (Exception e) {
                        Log.e(DEBUG_TAG, "GattController: Unable to retry service discovery!", e);
                    }
                }
            } finally {
                if (connectionState == GATT_DISCOVERING_SERVICES) {
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    mainHandler.postDelayed(retryServiceDiscovery, SERVICE_DISCOVERY_RETRY_PERIOD_MS);
                }
            }
        }
    };

    /**
     * Starts the GATT supervision task.
     */
    private void startGattSupervision() {
        cancelGattSupervision();
        if (DEBUG) Log.i(DEBUG_TAG, "GattController: Start GATT supervision.");
        gattSupervisionStartTimeNano = System.nanoTime();
        lastGattServerActivityTimeNano = gattSupervisionStartTimeNano;
        try {
            gattSupervisionTask = executor.scheduleWithFixedDelay(() -> {
                if (connectionState != GATT_CONNECTED) {
                    // Should not happen
                    cancelGattSupervision();
                }
                // Check last activity time
                long timeFromLastActivity = (System.nanoTime()-lastGattServerActivityTimeNano)/
                        1_000_000L;
                if (timeFromLastActivity > GATT_SUPERVISION_TIMEOUT_MS) {
                    Log.w(DEBUG_TAG, "GattController: GATT supervision timeout.");
                    synchronized (GattController.this) {
                        connectionLost = true;
                        remainingReconnectionAttempts = RECONNECTION_ATTEMPTS - 1;
                    }
                    if (RECONNECTION_ATTEMPTS > 0) {
                        Log.w(DEBUG_TAG, "GattController: Start reconnection after supervision timeout.");
                        reconnect();
                    } else {
                        Log.w(DEBUG_TAG, "GattController: Disconnect after supervision timeout.");
                        disconnect();
                    }
                }
            }, GATT_SUPERVISION_TIMEOUT_MS, GATT_SUPERVISION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "GattController: Unable to schedule the GATT supervision.");
        }
    }

    /**
     * Cancels all timeout tasks.
     */
    private void cancelAllTimeoutTasks() {
        cancelConnectionTimeout();
        cancelDisconnectionTimeout();
        cancelServiceDiscoveryTimeout();
        cancelReconnectionTask();
        cancelGattSupervision();
        cancelGattOperationTimeout();
    }

//    /**
//     * Closes and clears the GATT server.
//     */
//    @SuppressLint("MissingPermission")
//    private void closeAndClearGattServer() {
//        if (gattServer != null) {
//            gattServer.disconnect();
//            gattServer.close();
//        }
//        gattServer = null;
//    }

    /**
     * Cancels the timeout for the connection.
     */
    private void cancelConnectionTimeout() {
        ScheduledFuture<?> task = connectionTimeoutTask;
        connectionTimeoutTask = null;
        if (task != null) {
            task.cancel(false);
            // Note: `executor.purge();` should not be necessary
        }
    }

    private void cancelDisconnectionTimeout() {
        ScheduledFuture<?> task = disconnectionTimeoutTask;
        disconnectionTimeoutTask = null;
        if (task != null) {
            task.cancel(false);
            // Note: `executor.purge();` should not be necessary
        }
    }

    /**
     * Cancels the reconnection task
     */
    private void cancelReconnectionTask() {
        ScheduledFuture<?> task = reconnectionTask;
        reconnectionTask = null;
        if (task != null) {
            task.cancel(false);
            // Note: `executor.purge();` should not be necessary
        }
    }

    /**
     * Cancels the service discovery timeout.
     */
    private void cancelServiceDiscoveryTimeout() {
        ScheduledFuture<?> task = serviceDiscoveryTimeoutTask;
        serviceDiscoveryTimeoutTask = null;
        if (task != null) {
            task.cancel(false);
            // Note: `executor.purge();` should not be necessary
        }
        task = serviceDiscoveryRetryTask;
        serviceDiscoveryRetryTask = null;
        if (task != null) {
            task.cancel(false);
            // Note: `executor.purge();` should not be necessary
        }
    }

    /**
     * Cancels the GATT supervision task.
     */
    private void cancelGattSupervision() {
        ScheduledFuture<?> task = gattSupervisionTask;
        gattSupervisionTask = null;
        if (task != null) {
            task.cancel(false);
            // Note: `executor.purge();` should not be necessary
        }
    }

    /**
     * Cancels the GATT operation timeout.
     */
    private void cancelGattOperationTimeout() {
        ScheduledFuture<?> task = gattOperationTimeoutTask;
        gattOperationTimeoutTask = null;
        if (task != null) {
            task.cancel(false);
            // Note: `executor.purge();` should not be necessary
        }
    }

    /**
     * Returns the connected device.
     * @return the connected device.
     */
    public @Nullable BluetoothDevice getDevice() {
        return device;
    }

    /**
     * Returns a reference to a given characteristic from its UUID.
     * @param serviceUuid The service UUID.
     * @param charUuid The characteristic UUID.
     * @return The characteristic.
     */
    public @Nullable BluetoothGattCharacteristic getCharacteristic(UUID serviceUuid, UUID charUuid) {
        if (gattServer == null) {
            return null;
        }
        BluetoothGattService service = gattServer.getService(serviceUuid);
        if (service == null) {
            return null;
        }
        return service.getCharacteristic(charUuid);
    }

    /**
     * Returns the connection state.
     * @return the connection state.
     */
    public @NonNull GattConnectionState getConnectionState() {
        return connectionState;
    }

    /**
     * Check if a GATT operation must be started. This method must NOT be called inside a
     * synchronization block.
     */
    private void checkAndStartGattOperation() {
        ArrayList<GattOperation> completedOperations = null;
        synchronized (this) {
            while ((runningOperation == null && !operationQueue.isEmpty()) ||
                    (runningOperation != null && runningOperation.isDone())) {
                if (runningOperation != null && runningOperation.isDone()) {
                    cancelGattOperationTimeout();
                    if (completedOperations == null) {
                        completedOperations = new ArrayList<>();
                    }
                    completedOperations.add(runningOperation);
                    runningOperation = null;
                }
                if (!operationQueue.isEmpty()) {
                    runningOperation = operationQueue.remove();
                    runningOperation.start();
                    if (!runningOperation.isDone()) {
                        try {
                            gattOperationTimeoutTask = executor.schedule(
                                    new GattOperationTimeoutRunnable(runningOperation),
                                    runningOperation.getOperationTimeoutMs(),
                                    TimeUnit.MILLISECONDS);
                        } catch (Exception e) {
                            Log.e(DEBUG_TAG, "GattController: Failed to schedule GATT " +
                                    "operation timeout.", e);
                            runningOperation.setState(GattOperationState.STATE_CANCELLED);
                        }
                    }
                }
            }
        }
        if (completedOperations != null) {
            for (GattOperation operation: completedOperations) {
                notifyOperationCompletion(operation);
            }
        }
    }

    /**
     * Runnable for the timeout of an operation.
     */
    class GattOperationTimeoutRunnable implements Runnable {

        // Operation to timeout
        @NonNull GattOperation operation;

        /**
         * Constructor of the timeout with the operation to cancel in case of timeout.
         * @param operation the operation to timeout.
         */
        GattOperationTimeoutRunnable(@NonNull GattOperation operation) {
            this.operation = operation;
        }

        @Override
        public void run() {
            // Check and cancel running operation
            synchronized (GattController.this) {
                if (runningOperation == operation) {
                    cancelGattOperationTimeout();
                    runningOperation.setState(GattOperationState.STATE_CANCELLED);
                    Log.w(DEBUG_TAG, "GattController: Operation timeout for "+runningOperation.toString());
                } else {
                    // Should not happen
                    Log.w(DEBUG_TAG, "GattController: Timeout of non running operation.");
                }
            }
            // Check for operation to schedule
            checkAndStartGattOperation();
        }
    }

    /**
     * Cancels and returns all operations. Completion of operations is NOT notified. This method
     * MUST be called inside a synchronization block.
     * @return The list of canceled operations.
     */
    private List<GattOperation> cancelAllGattOperations() {
        ArrayList<GattOperation> operations = null;
        if (runningOperation != null || !operationQueue.isEmpty()) {
            operations = new ArrayList<>();
            if (runningOperation != null) {
                cancelGattOperationTimeout();
                runningOperation.setState(GattOperationState.STATE_CANCELLED);
                operations.add(runningOperation);
            }
            for (GattOperation operation: operationQueue) {
                operation.setState(GattOperationState.STATE_CANCELLED);
                operations.add(operation);
            }
            operationQueue.clear();
        }
        return operations;
    }

    /**
     * Notifies the completion of an operation. This method must NOT be called inside a
     * synchronization block.
     * @param operation The completed operation.
     */
    private void notifyOperationCompletion(@Nullable GattOperation operation) {
        if (operation != null) {
            ArrayList<GattEventListener> targets;
            synchronized (listeners) {
                if (listeners.isEmpty()) {
                    return;
                }
                targets = new ArrayList<>(listeners);
            }
            if (operation instanceof GattOperationSetNotificationIndication) {
                GattOperationSetNotificationIndication setNotification =
                        (GattOperationSetNotificationIndication) operation;
                for (GattEventListener l: targets) {
                    l.onCharacteristicNotificationSet(
                            setNotification.getCharacteristic(), setNotification.getValue(),
                            setNotification.succeed());
                }
            } else if (operation instanceof GattOperationReadCharacteristic) {
                GattOperationReadCharacteristic readCharacteristic =
                        (GattOperationReadCharacteristic)operation;
                for (GattEventListener l: targets) {
                    l.onCharacteristicRead(readCharacteristic.getCharacteristic(),
                            readCharacteristic.getValue(), operation.succeed());
                }
            } else if (operation instanceof GattOperationWriteCharacteristic) {
                GattOperationWriteCharacteristic writeCharacteristic =
                        (GattOperationWriteCharacteristic) operation;
                for (GattEventListener l : targets) {
                    l.onCharacteristicWrite(writeCharacteristic.getCharacteristic(),
                            writeCharacteristic.getValue(), operation.succeed());
                }
            } else if (operation instanceof GattOperationRequest) {
                GattOperationRequest request = (GattOperationRequest) operation;
                for (GattEventListener l : targets) {
                    l.onRequestCompleted(request.getRequestId(),
                            request.getNotifiedValue(), operation.succeed());
                }
            } else if (operation instanceof GattOperationRequestMtu) {
                GattOperationRequestMtu mtuRequest = (GattOperationRequestMtu) operation;
                for (GattEventListener l : targets) {
                    l.onMtuChanged(mtuRequest.getRequestedMtu(), operation.succeed());
                }
            } else {
                // Should not happen
                Log.e(DEBUG_TAG, "GattController: Unsupported GATT operation.");
            }
        }
    }

    /**
     * Enables or disables the notifications on a characteristic.
     *
     * @param characteristic The characteristic on which the notifications must be enabled or
     *                       disabled.
     * @param enableNotification <code>true</code> to enable notifications, <code>false</code>
     *                           to disable them.
     * @param enableIndication <code>true</code> to enable indications, <code>false</code>
     *                         to disable them.
     * @return <code>true</code> if the request has been sent.
     */
    public boolean setCharacteristicNotificationIndication(
            @Nullable BluetoothGattCharacteristic characteristic,
            boolean enableNotification, boolean enableIndication) {
        synchronized (this) {
            if (characteristic == null) {
                Log.e(DEBUG_TAG, "GattController: Operation on null characteristic.");
                return false;
            }
            if (connectionState != GATT_CONNECTED) {
                Log.w(DEBUG_TAG, "GattController: No connection for the operation.");
                return false;
            }
            if (gattServer == null) {
                Log.w(DEBUG_TAG, "GattController: No GATT server for the operation.");
                return false;
            }
            if (characteristic.getDescriptors().isEmpty()) {
                Log.w(DEBUG_TAG, "GattController: No descriptor for the operation.");
                return false;
            }
            BluetoothGattDescriptor descriptor = characteristic.getDescriptors().get(0);
            operationQueue.add(new GattOperationSetNotificationIndication(gattServer, descriptor,
                    enableNotification, enableIndication));
        }
        checkAndStartGattOperation();
        return true;
    }

    /**
     * Reads a characteristic.
     *
     * @param characteristic The characteristic to read.
     * @return <code>true</code> if the request has been correctly been sent.
     */
    public boolean readCharacteristic(@Nullable BluetoothGattCharacteristic characteristic) {
        synchronized (this) {
            if (characteristic == null) {
                Log.e(DEBUG_TAG, "GattController: Operation on null characteristic.");
                return false;
            }
            if (connectionState != GATT_CONNECTED) {
                Log.w(DEBUG_TAG, "GattController: No connection for the operation.");
                return false;
            }
            if (gattServer == null) {
                Log.w(DEBUG_TAG, "GattController: No GATT server for the operation.");
                return false;
            }
            operationQueue.add(new GattOperationReadCharacteristic(gattServer, characteristic));
        }
        checkAndStartGattOperation();
        return true;
    }

    /**
     * Writes a characteristic.
     *
     * @param characteristic The characteristic to write.
     * @param value The value to write.
     * @return <code>true</code> if the request has been correctly been sent.
     */
    public boolean writeCharacteristic(@Nullable BluetoothGattCharacteristic characteristic,
                                       @NonNull byte[] value) {
        synchronized (this) {
            if (characteristic == null) {
                Log.e(DEBUG_TAG, "GattController: Operation on null characteristic.");
                return false;
            }
            if (connectionState != GATT_CONNECTED) {
                Log.w(DEBUG_TAG, "GattController: No connection for the operation.");
                return false;
            }
            if (gattServer == null) {
                Log.w(DEBUG_TAG, "GattController: No GATT server for the operation.");
                return false;
            }
            operationQueue.add(
                    new GattOperationWriteCharacteristic(gattServer, characteristic, value));
        }
        checkAndStartGattOperation();
        return true;
    }

    /**
     * Sends a request on a characteristic and wait for a notification that acknowledge the request.
     *
     * @param writeCharacteristic The characteristic to write.
     * @param notifyCharacteristic The characteristic for the notification.
     * @param writeValue The value to write.
     * @param notifyPattern The notification pattern to wait.
     * @param requestId The request ID.
     * @return <code>true</code> if the request has been correctly been sent.
     */
    public boolean request(
            @Nullable BluetoothGattCharacteristic writeCharacteristic,
            @Nullable BluetoothGattCharacteristic notifyCharacteristic,
            @NonNull byte[] writeValue,
            @Nullable Byte[] notifyPattern,
            int requestId) {
        synchronized (this) {
            if (writeCharacteristic == null || notifyCharacteristic == null) {
                Log.e(DEBUG_TAG, "GattController: Operation on null characteristic.");
                return false;
            }
            if (connectionState != GATT_CONNECTED) {
                Log.w(DEBUG_TAG, "GattController: No connection for the operation.");
                return false;
            }
            if (gattServer == null) {
                Log.w(DEBUG_TAG, "GattController: No GATT server for the operation.");
                return false;
            }
            operationQueue.add(new GattOperationRequest(gattServer, writeCharacteristic,
                    notifyCharacteristic, writeValue, notifyPattern, requestId));
        }
        checkAndStartGattOperation();
        return true;
    }

    /**
     * Requests a new MTU size.
     *
     * @param mtu The requested MTU size.
     * @return <code>true</code> if the request has been sent.
     */
    public boolean requestMtu(int mtu) {
        synchronized (this) {
            if (connectionState != GATT_CONNECTED) {
                Log.w(DEBUG_TAG, "GattController: No connection for the operation.");
                return false;
            }
            if (gattServer == null) {
                Log.w(DEBUG_TAG, "GattController: No GATT server for the operation.");
                return false;
            }
            operationQueue.add(new GattOperationRequestMtu(gattServer, mtu));
        }
        checkAndStartGattOperation();
        return true;
    }

    /**
     * Requests new connection parameters with fast connection intervals.
     * @return <code>true</code> if the request has been correctly been sent.
     */
    @SuppressLint("MissingPermission")
    public boolean requestFastConnectionIntervals() {
        if (connectionState != GATT_CONNECTED) {
            Log.w(DEBUG_TAG, "GattController: No connection for the operation.");
            return false;
        }
        if (gattServer == null) {
            Log.w(DEBUG_TAG, "GattController: No GATT server for the operation.");
            return false;
        }
        return gattServer.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {

        if (gatt != gattServer) {
            // Obsolete event
            if (DEBUG) Log.i(DEBUG_TAG, "GattController: Obsolete connection state " +
                    "change event. Event ignored.");
            return;
        }

        boolean reconnect = false;
        boolean failed = false;
        boolean lost = false;

        switch (newState) {

            case BluetoothProfile.STATE_CONNECTED:
                switch (connectionState) {
                    case GATT_DISCONNECTED:
                    case GATT_DISCONNECTING:
                        // Should not happen
                        Log.e(DEBUG_TAG, "GattController: Unexpected connection.");
                        if (gatt != null) {
                            gatt.disconnect();
                            gatt.close();
                        }
                        return;

                    case GATT_CONNECTING:
                    case GATT_RECONNECTING:
                        // Start service discovery
                        startServiceDiscovery();
                        break;

                    case GATT_DISCOVERING_SERVICES:
                    case GATT_PAIRING:
                    case GATT_CONNECTED:
                        // Should not happen, ignore event.
                        Log.w(DEBUG_TAG, "GattController: Unexpected connection " +
                                "event.");
                        return;
                }
                break;

            case BluetoothProfile.STATE_DISCONNECTED:
                switch (connectionState) {
                    case GATT_DISCONNECTING:
                        // Expected disconnection event
                        synchronized (this) {
                            cancelAllTimeoutTasks();
                            if (gattServer != null) {
                                try {
                                    gattServer.close();
                                } catch (Exception e) {
                                    Log.e(DEBUG_TAG, "GattController: Unable to close GATT server!", e);
                                }
                                gattServer = null;
                            }
                            connectionState = GATT_DISCONNECTED;
                            failed = connectionFailed;
                            lost = connectionLost;
                            remainingReconnectionAttempts = INITIAL_RECONNECTION_ATTEMPTS;
                            connectionLost = false;
                            connectionFailed = false;
                            initialConnection = false;
                        }
                        if (failed) {
                            notifyConnectionFailed();
                        } else if (lost) {
                            notifyConnectionLost();
                        }
                        notifyGattConnectionStateChange();
                        break;

                    case GATT_DISCONNECTED:
                        // Ignore event
                        break;

                    case GATT_RECONNECTING:
                        // Expected event, close GATT server without changing state
                        if (gattServer != null) {
                            try {
                                gattServer.close();
                            } catch (Exception e) {
                                Log.e(DEBUG_TAG, "GattController: Unable to close GATT server!", e);
                            }
                            gattServer = null;
                        }
                        break;

                    case GATT_CONNECTING:
                    case GATT_DISCOVERING_SERVICES:
                    case GATT_PAIRING:
                    case GATT_CONNECTED:
                        synchronized (this) {
                            cancelAllTimeoutTasks();
                            if (gattServer != null) {
                                try {
                                    gattServer.close();
                                } catch (Exception e) {
                                    Log.e(DEBUG_TAG, "GattController: Unable to close GATT server!", e);
                                }
                                gattServer = null;
                            }
                            if (remainingReconnectionAttempts > 0) {
                                remainingReconnectionAttempts--;
                                reconnect = true;
                            } else {
                                connectionState = GATT_DISCONNECTED;
                                failed = initialConnection;
                                remainingReconnectionAttempts = INITIAL_RECONNECTION_ATTEMPTS;
                                connectionLost = false;
                                connectionFailed = false;
                                initialConnection = false;
                            }
                        }
                        if (reconnect) {
                            reconnect();
                        } else {
                            if (failed) {
                                notifyConnectionFailed();
                            } else {
                                notifyConnectionLost();
                            }
                            notifyGattConnectionStateChange();
                        }
                        break;
                }
                break;
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        Log.i(DEBUG_TAG, "GattController: Service discovered, status :"+status+" (0=OK).");
        // Note: No update of last GATT server activity time because services may be in cache
        boolean reconnect = false;
        boolean disconnect = false;
        synchronized (this) {
            if (connectionState == GATT_DISCOVERING_SERVICES) {
                // Check status and completion
                if (status == BluetoothGatt.GATT_SUCCESS &&
                        gatt.getService(
                                BeltCommunicationController.BELT_CONTROL_SERVICE_UUID) != null &&
                        gatt.getService(
                                BeltCommunicationController.SENSOR_SERVICE_UUID) != null &&
                        gatt.getService(
                                BeltCommunicationController.DEBUG_SERVICE_UUID) != null) {
                    // Service discovery completed
                    cancelServiceDiscoveryTimeout();
                    if (device != null && device.getBondState() != BluetoothDevice.BOND_BONDED) {
                        connectionState = GATT_PAIRING;
                        pairingManager.startPairing(device);
                    } else {
                        connectionState = GATT_CONNECTED;
                        remainingReconnectionAttempts = RECONNECTION_ATTEMPTS;
                        connectionLost = false;
                        connectionFailed = false;
                        initialConnection = false;
                        startGattSupervision();
                    }
                } else {
                    Log.e(DEBUG_TAG, "GattController: Service discovery failed.");
                    if (remainingReconnectionAttempts > 0) {
                        remainingReconnectionAttempts--;
                        reconnect = true;
                    } else if (initialConnection) {
                        connectionFailed = true;
                        disconnect = true;
                    } else {
                        connectionLost = true;
                        disconnect = true;
                    }
                }
            }
        }
        if (reconnect) {
            reconnect();
        } else if (disconnect) {
            disconnect();
        } else {
            notifyGattConnectionStateChange();
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                                     int status) {
        synchronized (this) {
            // Update last GATT server activity time
            if (status == BluetoothGatt.GATT_SUCCESS) {
                lastGattServerActivityTimeNano = System.nanoTime();
            }
            // Propagate event to operation
            if (runningOperation != null) {
                runningOperation.onCharacteristicRead(gatt, characteristic, status);
            }
        }
        checkAndStartGattOperation();
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt,
                                      BluetoothGattCharacteristic characteristic, int status) {
        synchronized (this) {
            // Propagate event to operation queue
            if (runningOperation != null) {
                runningOperation.onCharacteristicWrite(gatt, characteristic, status);
            }
        }
        checkAndStartGattOperation();
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic) {
//        Log.d(DEBUG_TAG, "GattController: Notification on " + characteristic.getUuid() +
//                ", value: " + Arrays.toString(characteristic.getValue()));
        synchronized (this) {
            // Update last GATT server activity time
            lastGattServerActivityTimeNano = System.nanoTime();
            // Propagate event to operation
            if (runningOperation != null) {
                runningOperation.onCharacteristicChanged(gatt, characteristic);
            }
        }
        checkAndStartGattOperation();
        // Inform listeners
        ArrayList<GattEventListener> targets;
        synchronized (listeners) {
            if (listeners.isEmpty()) {
                return;
            }
            targets = new ArrayList<>(listeners);
        }
        for (GattEventListener l: targets) {
            l.onCharacteristicChanged(characteristic, characteristic.getValue());
        }
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt,
                                 BluetoothGattDescriptor descriptor, int status) {
        synchronized (this) {
            // Update last GATT server activity time
            if (status == BluetoothGatt.GATT_SUCCESS) {
                lastGattServerActivityTimeNano = System.nanoTime();
            }
            // Propagate event to operation
            if (runningOperation != null) {
                runningOperation.onDescriptorRead(gatt, descriptor, status);
            }
        }
        checkAndStartGattOperation();
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                  int status) {
        synchronized (this) {
            // Propagate event to operation
            if (runningOperation != null) {
                runningOperation.onDescriptorWrite(gatt, descriptor, status);
            }
        }
        checkAndStartGattOperation();
    }

    @Override
    public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
        synchronized (this) {
            // Update last GATT server activity time
            if (status == BluetoothGatt.GATT_SUCCESS) {
                lastGattServerActivityTimeNano = System.nanoTime();
            }
            // Propagate event to operation
            if (runningOperation != null) {
                runningOperation.onReliableWriteCompleted(gatt, status);
            }
        }
        checkAndStartGattOperation();
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        synchronized (this) {
            // Update last GATT server activity time
            if (status == BluetoothGatt.GATT_SUCCESS) {
                lastGattServerActivityTimeNano = System.nanoTime();
            }
            // Propagate event to operation
            if (runningOperation != null) {
                runningOperation.onMtuChanged(gatt, mtu, status);
            }
        }
        checkAndStartGattOperation();
    }

    /**
     * Adds a listener for GATT events.
     *
     * @param listener the listener to add.
     */
    public void addGattEventListener(GattEventListener listener) {
        synchronized (listeners) {
            if (listener != null && !listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    /**
     * Removes a listener.
     *
     * @param listener the listener to remove.
     */
    public void removeGattEventListener(GattEventListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * Notifies listeners that the last connection attempt failed.
     */
    private void notifyConnectionFailed() {
        if (DEBUG) Log.i(DEBUG_TAG, "GattController: Notify connection failed.");
        ArrayList<GattEventListener> targets;
        synchronized (listeners) {
            if (listeners.isEmpty()) {
                return;
            }
            targets = new ArrayList<>(listeners);
        }
        for (GattEventListener l: targets) {
            l.onGattConnectionFailed();
        }
    }

    /**
     * Notifies listeners that the connection has been lost after possible reconnection attempts.
     */
    private void notifyConnectionLost() {
        if (DEBUG) Log.i(DEBUG_TAG, "GattController: Notify connection lost.");
        ArrayList<GattEventListener> targets;
        synchronized (listeners) {
            if (listeners.isEmpty()) {
                return;
            }
            targets = new ArrayList<>(listeners);
        }
        for (GattEventListener l: targets) {
            l.onGattConnectionLost();
        }
    }

    /**
     * Notifies listeners of a connection state change.
     */
    private void notifyGattConnectionStateChange() {
        if (DEBUG) Log.i(DEBUG_TAG, "GattController: Connection state changed to: " +
                connectionState.toString());
        ArrayList<GattEventListener> targets;
        GattConnectionState state;
        synchronized (listeners) {
            if (listeners.isEmpty()) {
                return;
            }
            targets = new ArrayList<>(listeners);
            state = connectionState;
        }
        for (GattEventListener l: targets) {
            l.onGattConnectionStateChange(state);
        }
    }

    /**
     * Callback interface for GATT events.
     */
    public interface GattEventListener {

        /**
         * Callback for connection state change.
         *
         * @param state The connection state.
         */
        void onGattConnectionStateChange(GattConnectionState state);

        /**
         * Callback for connection failure.
         */
        void onGattConnectionFailed();

        /**
         * Callback for connection lost after possible reconnection attempts.
         */
        void onGattConnectionLost();

        /**
         * Callback for notification state change.
         * @param characteristic The characteristic for which the state of notification is expected
         *                       to change.
         * @param enable The expected state of notification.
         * @param success <code>true</code> on success, <code>false</code> when the operation
         *                failed.
         */
        void onCharacteristicNotificationSet(@NonNull BluetoothGattCharacteristic characteristic,
                                             boolean enable, boolean success);

        /**
         * Callback for read operation completion.
         * @param characteristic The characteristic read.
         * @param value The value of the characteristic.
         * @param success <code>true</code> on success, <code>false</code> when the operation
         *                failed.
         */
        void onCharacteristicRead(@NonNull BluetoothGattCharacteristic characteristic,
                                  @Nullable byte[] value, boolean success);

        /**
         * Callback for write operation completion.
         * @param characteristic The characteristic written.
         * @param value The value written.
         * @param success <code>true</code> on success, <code>false</code> when the operation
         *                failed.
         */
        void onCharacteristicWrite(@NonNull BluetoothGattCharacteristic characteristic,
                                   @Nullable byte[] value, boolean success);

        /**
         * Callback for characteristic notifications.
         * @param characteristic The notified characteristic.
         * @param value The notified value.
         */
        void onCharacteristicChanged(@Nullable BluetoothGattCharacteristic characteristic,
                                     @Nullable byte[] value);

        /**
         * Callback for the completion of a request.
         * @param requestId The request ID.
         * @param notifiedValue The notified value.
         */
        void onRequestCompleted(int requestId, @Nullable byte[] notifiedValue, boolean success);

        /**
         * Callback for the completion of MTU size request.
         *
         * @param mtu The requested MTU size.
         * @param success Success flag.
         */
        void onMtuChanged(int mtu, boolean success);
    }

    // MARK: Implementation of `BluetoothPairingDelegate`

    @Override
    public void onPairingFinished(BluetoothDevice device) {
        Log.e(DEBUG_TAG, "GattController: Pairing completed.");
        synchronized (this) {
            if (connectionState != GATT_PAIRING) {
                return;
            }
            connectionState = GATT_CONNECTED;
            remainingReconnectionAttempts = RECONNECTION_ATTEMPTS;
            connectionLost = false;
            connectionFailed = false;
            initialConnection = false;
            startGattSupervision();
        }
        notifyGattConnectionStateChange();
    }

    @Override
    public void onPairingFailed() {
        Log.e(DEBUG_TAG, "GattController: Pairing failed.");
        boolean reconnect = false;
        synchronized (this) {
            if (connectionState != GATT_PAIRING) {
                return;
            }
            if (remainingReconnectionAttempts > 0) {
                remainingReconnectionAttempts--;
                reconnect = true;
            } else if (initialConnection) {
                connectionFailed = true;
            } else {
                connectionLost = true;
            }
        }
        if (reconnect) {
            reconnect();
        } else {
            disconnect();
        }
    }

}
