/*
 * Copyright (c) 2017-2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */
package de.feelspace.fslib;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static de.feelspace.fslib.GattConnectionState.GATT_CONNECTED;
import static de.feelspace.fslib.GattConnectionState.GATT_CONNECTING;
import static de.feelspace.fslib.GattConnectionState.GATT_DISCONNECTED;
import static de.feelspace.fslib.GattConnectionState.GATT_DISCOVERING_SERVICES;
import static de.feelspace.fslib.GattConnectionState.GATT_RECONNECTING;

/**
 * Encapsulation of the GATT server with the addition of a queue of operations.
 */
class GattController extends BluetoothGattCallback {

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

    // Operation queue
    private @NonNull LinkedList<GattOperation> operationQueue = new LinkedList<>();
    private @Nullable GattOperation runningOperation;

    // Executor for callback and timeout
    private @NonNull ScheduledThreadPoolExecutor executor;

    // Listeners
    private final @NonNull ArrayList<GattEventListener> listeners = new ArrayList<>();

    // Connection timeout
    private long connectionTimeoutMs = DEFAULT_CONNECTION_TIMEOUT_MS;
    public static final long DEFAULT_CONNECTION_TIMEOUT_MS = 2000;
    private ScheduledFuture connectionTimeoutTask;

    // Service discovery timeout
    private long serviceDiscoveryTimeoutMs = DEFAULT_SERVICE_DISCOVERY_TIMEOUT_MS;
    public static final long DEFAULT_SERVICE_DISCOVERY_TIMEOUT_MS = 4000;
    private ScheduledFuture serviceDiscoveryTimeoutTask;

    // GATT supervision timeout
    private long gattSupervisionTimeoutMs = DEFAULT_GATT_SUPERVISION_TIMEOUT_MS;
    public static final long DEFAULT_GATT_SUPERVISION_TIMEOUT_MS = 6000;
    private ScheduledFuture gattSupervisionTask;
    private long lastGattServerActivityTimeNano;
    private long gattSupervisionStartTimeNano;

    // Reconnection
    private boolean reconnectOnInitialConnection = DEFAULT_RECONNECT_ON_INITIAL_CONNECTION;
    public static final boolean DEFAULT_RECONNECT_ON_INITIAL_CONNECTION = false;
    private long reconnectionDelayMs = DEFAULT_RECONNECTION_DELAY_MS;
    public static final long DEFAULT_RECONNECTION_DELAY_MS = 2000;
    private int reconnectionAttempts = DEFAULT_RECONNECTION_ATTEMPTS;
    public static final int DEFAULT_RECONNECTION_ATTEMPTS = 2;
    private ScheduledFuture reconnectionTask;
    private int reconnectionCount = 0;
    private boolean initialConnection = true;

    // Operation timeout
    private long operationTimeoutMs = DEFAULT_GATT_OPERATION_TIMEOUT_MS;
    public static final long DEFAULT_GATT_OPERATION_TIMEOUT_MS = 500;
    private ScheduledFuture gattOperationTimeoutTask;

    /**
     * Constructor.
     */
    GattController() {
        executor = new ScheduledThreadPoolExecutor(1);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            executor.setRemoveOnCancelPolicy(true);
        }
    }

    /**
     * Requests a connection to the GATT server for the given device. Previous connection will be
     * closed if necessary.
     *
     * Instead of raising an exception or returning a termination code on failure, this method uses
     * the callback {@link GattEventListener#onGattConnectionFailed()}.
     *
     * @param context The context, because Android team decided that the BLE API is not awful
     *                enough. Fun fact, it does not seem to be used but for API compatibility it
     *                must be given.
     * @param device The device to connect to.
     */
    public void connect(@NonNull Context context, @NonNull BluetoothDevice device) {
        if (DEBUG) Log.i(DEBUG_TAG, "GattController: Connection request.");
        List<GattOperation> canceledOperations = null;
        boolean success = true;
        synchronized (this) {
            if (connectionState != GattConnectionState.GATT_DISCONNECTED) {
                cancelAllTimeoutTasks();
                canceledOperations = cancelAllGattOperations();
                closeAndClearGattServer();
            }
            connectionState = GATT_CONNECTING;
            reconnectionCount = 0;
            initialConnection = true;
            this.device = device;
            this.context = context;
            try {
                gattServer = device.connectGatt(context, false, this);
            } catch (Exception e) {
                Log.e(DEBUG_TAG, "GattController: Unable to call connection method for" +
                        " GATT server.", e);
                gattServer = null;
            }
            if (gattServer != null) {
                scheduleConnectionTimeout(connectionTimeoutMs);
            } else {
                // Yep, 'connectGatt' can return 'null' but it is not documented
                // No reconnection attempt when an error occurs with the Bluetooth service
                Log.e(DEBUG_TAG, "GattController: Unable to connect to GATT server.");
                closeAndClearGattServer();
                connectionState = GattConnectionState.GATT_DISCONNECTED;
                success = false;
            }
        }
        if (canceledOperations != null) {
            for (GattOperation operation: canceledOperations) {
                notifyOperationCompletion(operation);
            }
        }
        if (!success) {
            notifyConnectionFailed();
        }
        notifyGattConnectionStateChange();
    }

    /**
     * Schedules a connection timeout.
     *
     * @param timeout The timeout delay.
     */
    private void scheduleConnectionTimeout(long timeout) {
        try {
            connectionTimeoutTask = executor.schedule(new Runnable() {
                @Override
                public void run() {
                    Log.w(DEBUG_TAG, "GattController: Connection timeout.");
                    synchronized (GattController.this) {
                        connectionTimeoutTask = null;
                        if (connectionState != GATT_CONNECTING &&
                                connectionState != GATT_RECONNECTING) {
                            // Ignore timeout if not connecting or reconnecting
                            return;
                        }
                    }
                    reconnect();
                }
            }, timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "GattController: Unable to schedule the connection timeout.");
        }
    }

    /**
     * Requests a disconnection of the GATT server.
     */
    public void disconnect() {
        if (DEBUG) Log.i(DEBUG_TAG, "GattController: Disconnection request.");
        List<GattOperation> canceledOperations = null;
        synchronized (this) {
            if (connectionState == GATT_DISCONNECTED) {
                return;
            }
            cancelAllTimeoutTasks();
            canceledOperations = cancelAllGattOperations();
            connectionState = GATT_DISCONNECTED;
            closeAndClearGattServer();
        }
        if (canceledOperations != null) {
            for (GattOperation operation: canceledOperations) {
                notifyOperationCompletion(operation);
            }
        }
        notifyGattConnectionStateChange();
    }

    /**
     * Schedules a reconnection if the maximum number of reconnection attempts has not been reached.
     */
    protected void reconnect() {
        boolean notifyConnectionFailed = false;
        boolean notifyConnectionLost = false;
        List<GattOperation> canceledOperations = null;
        synchronized (this) {
            // First disconnect
            cancelAllTimeoutTasks();
            canceledOperations = cancelAllGattOperations();
            connectionState = GATT_DISCONNECTED;
            closeAndClearGattServer();
            // Check for reconnection condition
            if (device == null || context == null) {
                // Should not happen
                Log.w(DEBUG_TAG, "GattReconnect: Cannot reconnect without initial " +
                        "connection.");
                notifyConnectionFailed = true;
            } else if (initialConnection && (!reconnectOnInitialConnection ||
                    (reconnectionCount >= reconnectionAttempts))) {
                // Initial connection failed
                notifyConnectionFailed = true;
            } else if (!initialConnection && (reconnectionCount >= reconnectionAttempts)) {
                // Reconnection attempts failed
                notifyConnectionLost = true;
            } else {
                // Schedule a reconnection attempt
                reconnectionCount++;
                if (initialConnection) {
                    connectionState = GATT_CONNECTING;
                } else {
                    connectionState = GATT_RECONNECTING;
                }
                if (!scheduleReconnection()) {
                    // Should not happen
                    connectionState = GATT_DISCONNECTED;
                    if (initialConnection) {
                        notifyConnectionFailed = true;
                    } else {
                        notifyConnectionLost = true;
                    }
                }
            }
        }
        if (canceledOperations != null) {
            for (GattOperation operation: canceledOperations) {
                notifyOperationCompletion(operation);
            }
        }
        if (notifyConnectionFailed) {
            notifyConnectionFailed();
        }
        if (notifyConnectionLost) {
            notifyConnectionLost();
        }
        notifyGattConnectionStateChange();
    }

    /**
     * Schedules a reconnection.
     *
     * @return <code>true</code> if the task has been successfully scheduled.
     */
    private boolean scheduleReconnection() {
        try {
            reconnectionTask = executor.schedule(new Runnable() {
                @Override
                public void run() {
                    if (DEBUG) Log.i(DEBUG_TAG, "GattController: Reconnection attempt.");
                    boolean reconnectionFailed = false;
                    synchronized (GattController.this) {
                        reconnectionTask = null;
                        if (connectionState == GATT_RECONNECTING ||
                                connectionState == GATT_CONNECTING) {
                            try {
                                gattServer = device.connectGatt(context, false,
                                        GattController.this);
                            } catch (Exception e) {
                                Log.e(DEBUG_TAG, "GattController: Unable to call " +
                                        "connection method for GATT server.", e);
                                gattServer = null;
                            }
                            if (gattServer != null) {
                                scheduleConnectionTimeout(connectionTimeoutMs);
                            } else {
                                // Yep, 'connectGatt' can return 'null' but it is not documented
                                // No reconnection attempt when an error occurs with the Bluetooth service
                                Log.e(DEBUG_TAG, "GattController: Unable to reconnect to " +
                                        "GATT server.");
                                closeAndClearGattServer();
                                connectionState = GattConnectionState.GATT_DISCONNECTED;
                                reconnectionFailed = true;
                            }
                        }
                    }
                    if (reconnectionFailed) {
                        notifyGattConnectionStateChange();
                    }
                }
            }, reconnectionDelayMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "GattController: Unable to schedule the reconnection.");
            return false;
        }
        return true;
    }

    /**
     * Schedules a service discovery timeout.
     */
    private void scheduleServiceDiscoveryTimeout() {
        try {
            serviceDiscoveryTimeoutTask = executor.schedule(new Runnable() {
                @Override
                public void run() {
                    Log.w(DEBUG_TAG, "GattController: Service discovery timeout.");
                    synchronized (GattController.this) {
                        serviceDiscoveryTimeoutTask = null;
                        if (connectionState != GATT_DISCOVERING_SERVICES) {
                            // Should not happen, ignore obsolete timeout
                            return;
                        }
                    }
                    reconnect();
                }
            }, serviceDiscoveryTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "GattController: Unable to schedule the service discovery" +
                    " timeout.");
        }
    }

    /**
     * Starts the GATT supervision task.
     */
    private void startGattSupervision() {
        cancelGattSupervision();
        if (DEBUG) Log.i(DEBUG_TAG, "GattController: Start GATT supervision.");
        gattSupervisionStartTimeNano = System.nanoTime();
        lastGattServerActivityTimeNano = gattSupervisionStartTimeNano;
        try {
            gattSupervisionTask = executor.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    if (connectionState != GATT_CONNECTED) {
                        // Should not happen
                        cancelGattSupervision();
                    }
                    // Check last activity time
                    long timeFromLastActivity = (System.nanoTime()-lastGattServerActivityTimeNano)/
                            1_000_000L;
                    if (timeFromLastActivity > gattSupervisionTimeoutMs) {
                        Log.w(DEBUG_TAG, "GattController: GATT supervision timeout.");
                        reconnect();
                    }
                }
            }, gattSupervisionTimeoutMs, gattSupervisionTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "GattController: Unable to schedule the GATT supervision.");
        }
    }

    /**
     * Cancels all timeout tasks.
     */
    private void cancelAllTimeoutTasks() {
        cancelConnectionTimeout();
        cancelServiceDiscoveryTimeout();
        cancelReconnectionTask();
        cancelGattSupervision();
        cancelGattOperationTimeout();
    }

    /**
     * Closes and clears the GATT server.
     */
    private void closeAndClearGattServer() {
        if (gattServer != null) {
            gattServer.disconnect();
            gattServer.close();
        }
        gattServer = null;
    }

    /**
     * Cancels the timeout for the connection.
     */
    private void cancelConnectionTimeout() {
        ScheduledFuture task = connectionTimeoutTask;
        connectionTimeoutTask = null;
        if (task != null) {
            task.cancel(false);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                executor.purge();
            }
        }
    }

    /**
     * Cancels the reconnection task
     */
    private void cancelReconnectionTask() {
        ScheduledFuture task = reconnectionTask;
        reconnectionTask = null;
        if (task != null) {
            task.cancel(false);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                executor.purge();
            }
        }
    }

    /**
     * Cancels the service discovery timeout.
     */
    private void cancelServiceDiscoveryTimeout() {
        ScheduledFuture task = serviceDiscoveryTimeoutTask;
        serviceDiscoveryTimeoutTask = null;
        if (task != null) {
            task.cancel(false);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                executor.purge();
            }
        }
    }

    /**
     * Cancels the GATT supervision task.
     */
    private void cancelGattSupervision() {
        ScheduledFuture task = gattSupervisionTask;
        gattSupervisionTask = null;
        if (task != null) {
            task.cancel(false);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                executor.purge();
            }
        }
    }

    /**
     * Cancels the GATT operation timeout.
     */
    private void cancelGattOperationTimeout() {
        ScheduledFuture task = gattOperationTimeoutTask;
        gattOperationTimeoutTask = null;
        if (task != null) {
            task.cancel(false);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                executor.purge();
            }
        }
    }

    /**
     * Returns the GATT server.
     * @return the GATT server.
     */
    public @Nullable BluetoothGatt getGattServer() {
        return gattServer;
    }

    /**
     * Returns the connected device.
     * @return the connected device.
     */
    public @Nullable BluetoothDevice getDevice() {
        return device;
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
            if (operation instanceof GattOperationSetNotification) {
                GattOperationSetNotification setNotification =
                        (GattOperationSetNotification) operation;
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
                    l.onCharacteristicWrite(request.getNotifiedCharacteristic(),
                            request.getNotifiedValue(), operation.succeed());
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
     * @param enable <code>true</code> to enable the notifications, <code>false</code> to disable.
     * @return <code>true</code> if the request has been sent.
     */
    public boolean setCharacteristicNotification(
            @Nullable BluetoothGattCharacteristic characteristic,
            boolean enable) {
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
            operationQueue.add(new GattOperationSetNotification(gattServer, descriptor, enable));
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
     * @return <code>true</code> if the request has been correctly been sent.
     */
    public boolean request(@Nullable BluetoothGattCharacteristic writeCharacteristic,
                           @Nullable BluetoothGattCharacteristic notifyCharacteristic,
                           @NonNull byte[] writeValue,
                           @Nullable Byte[] notifyPattern) {
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
                    notifyCharacteristic, writeValue, notifyPattern));
        }
        checkAndStartGattOperation();
        return true;
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        boolean reconnect = false;
        synchronized (this) {
            if (gatt != gattServer) {
                // Obsolete event
                if (DEBUG) Log.i(DEBUG_TAG, "GattController: Obsolete connection state " +
                        "change event. Event ignored.");
                return;
            }
            switch (newState){

                case BluetoothProfile.STATE_CONNECTED:
                    switch (connectionState) {
                        case GATT_DISCONNECTED:
                            // Should not happen
                            Log.e(DEBUG_TAG, "GattController: Unexpected connection.");
                            if (gatt != null) {
                                gatt.disconnect();
                                gatt.close();
                            }
                            return;
                        case GATT_CONNECTING:
                        case GATT_RECONNECTING:
                            // Continue with service discovery
                            cancelConnectionTimeout();
                            if (gatt.discoverServices()) {
                                connectionState = GATT_DISCOVERING_SERVICES;
                                scheduleServiceDiscoveryTimeout();
                            } else {
                                reconnect = true;
                            }
                            break;
                        case GATT_DISCOVERING_SERVICES:
                        case GATT_CONNECTED:
                            // Should not happen, ignore event.
                            Log.w(DEBUG_TAG, "GattController: Unexpected connection " +
                                    "event.");
                            return;
                    }
                    break;

                case BluetoothProfile.STATE_DISCONNECTED:
                    switch (connectionState) {
                        case GATT_DISCONNECTED:
                            // Ignore event
                            break;
                        case GATT_CONNECTING:
                        case GATT_RECONNECTING:
                        case GATT_DISCOVERING_SERVICES:
                        case GATT_CONNECTED:
                            // Connection lost
                            reconnect = true;
                            break;
                    }
                    break;
            }
        }
        if (reconnect) {
            reconnect();
        } else {
            notifyGattConnectionStateChange();
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        // Note: No update of last GATT server activity time because services may be in cache
        boolean reconnect = false;
        synchronized (this) {
            if (connectionState == GATT_DISCOVERING_SERVICES) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    cancelServiceDiscoveryTimeout();
                    connectionState = GATT_CONNECTED;
                    startGattSupervision();
                } else {
                    Log.e(DEBUG_TAG, "GattController: Service discovery failed.");
                    reconnect = true;
                }
            }
        }
        if (reconnect) {
            reconnect();
        } else {
            notifyGattConnectionStateChange();
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                                     int status) {
        synchronized (this) {
            // Update last GATT server activity time and reset reconnection count
            if (status == BluetoothGatt.GATT_SUCCESS) {
                lastGattServerActivityTimeNano = System.nanoTime();
                reconnectionCount = 0;
                initialConnection = false;
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
        synchronized (this) {
            // Update last GATT server activity time and reset reconnection count
            lastGattServerActivityTimeNano = System.nanoTime();
            reconnectionCount = 0;
            initialConnection = false;
            // Propagate event to operation
            if (runningOperation != null) {
                runningOperation.onCharacteristicChanged(gatt, characteristic);
            }
        }
        checkAndStartGattOperation();
        // Notification callback
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
            // Update last GATT server activity time and reset reconnection count
            if (status == BluetoothGatt.GATT_SUCCESS) {
                lastGattServerActivityTimeNano = System.nanoTime();
                reconnectionCount = 0;
                initialConnection = false;
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
            // Update last GATT server activity time and reset reconnection count
            if (status == BluetoothGatt.GATT_SUCCESS) {
                lastGattServerActivityTimeNano = System.nanoTime();
                reconnectionCount = 0;
                initialConnection = false;
            }
            // Propagate event to operation
            if (runningOperation != null) {
                runningOperation.onReliableWriteCompleted(gatt, status);
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
    protected interface GattEventListener {

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
         * @param notifiedCharacteristic The notified characteristic.
         * @param notifiedValue The notified value.
         */
        void onRequestCompleted(@Nullable BluetoothGattCharacteristic notifiedCharacteristic,
                                @Nullable byte[] notifiedValue);
    }
}
