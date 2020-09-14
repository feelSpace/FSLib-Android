package de.feelspace.fslib;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The navigation controller simplifies connection and interaction with a belt. It is design for
 * navigation application.
 */
public class NavigationController {
    // Debug
    @SuppressWarnings("unused")
    private static final String DEBUG_TAG = "FeelSpace-Debug";
    @SuppressWarnings("unused")
    private static final boolean DEBUG = true;

    // Connection interface
    private @NonNull BeltConnectionInterface beltConnection;

    // Belt controller
    private @NonNull BeltCommandInterface beltController;

    // Navigation signal
    private @Nullable BeltVibrationSignal navigationSignal;

    // Flag for scheduled vibration command
    private @NonNull AtomicBoolean isVibrationCommandScheduled = new AtomicBoolean(false);

    // Executor for delayed vibration command
    private @NonNull ScheduledThreadPoolExecutor executor;

    // Scheduled vibration command
    private @Nullable ScheduledFuture vibrationCommandTask;

    // Last vibration command send time
    private long lastVibrationCommandNanoTime = 0;

    // Minimum period between two vibration command
    private static final long MINIMUM_VIBRATION_COMMAND_UPDATE_PERIOD_NANO = 100*1000000;

    // Direction of the navigation
    private int navigationDirection = 0;

    // Flag for mag. bearing
    private boolean isMagneticBearingDirection = true;

    // State of the navigation
    private @NonNull NavigationState navigationState;

    // Flag that indicate that the current pause mode has been set by the application
    private boolean isPauseModeForNavigation = false;

    // Flag for compass accuracy signal
    private Boolean compassAccuracySignalEnabled = null;

    // Listeners
    private @NonNull ArrayList<NavigationEventListener> listeners = new ArrayList<>();

    /**
     * Channel index used for the navigation signal.
     */
    protected static final int NAVIGATION_SIGNAL_CHANNEL = 2;

    /**
     * Constructor.
     *
     * @param applicationContext The application context to access Bluetooth service.
     * @throws IllegalArgumentException If the context is <code>null</code>.
     */
    public NavigationController(
            Context applicationContext) throws NullPointerException {
        beltConnection = BeltConnectionInterface.create(applicationContext);
        beltController = beltConnection.getCommandInterface();
        BeltListener beltListener = new BeltListener();
        beltConnection.addConnectionListener(beltListener);
        beltController.addCommandListener(beltListener);
        this.navigationState = NavigationState.STOPPED;
        executor = beltConnection.getExecutor();
    }

    /**
     * Searches and connects a belt.
     */
    public void searchAndConnectBelt() {
        if (beltConnection.getState() != BeltConnectionState.STATE_DISCONNECTED) {
            return;
        }
        try {
            beltConnection.scanAndConnect();
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Connection failed", e);
            notifyBeltConnectionFailed();
        }
    }

    /**
     * Connects a belt.
     *
     * @param device The belt Bluetooth device.
     */
    public void connectBelt(BluetoothDevice device) {
        if (beltConnection.getState() != BeltConnectionState.STATE_DISCONNECTED) {
            return;
        }
        try {
            beltConnection.connect(device);
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Connection failed", e);
            notifyBeltConnectionFailed();
        }
    }

    /**
     * Disconnects the belt.
     */
    public void disconnectBelt() {
        if (navigationState == NavigationState.PAUSED && isPauseModeForNavigation &&
                beltConnection.getState() == BeltConnectionState.STATE_CONNECTED) {
            // TODO Check if mode change with timeout is necessary, or delayed disconnection
            beltController.changeMode(BeltMode.WAIT);
        }
        beltConnection.stopScan();
        beltConnection.disconnect();
    }

    /**
     * Returns the connection state.
     *
     * @return The connection state.
     */
    public BeltConnectionState getConnectionState() {
        return beltConnection.getState();
    }

    /**
     * Returns the navigation state.
     *
     * @return The navigation state.
     */
    public NavigationState getNavigationState() {
        return navigationState;
    }

    /**
     * Changes the default vibration intensity of the connected belt. The default vibration
     * intensity can be changed only when a belt is connected. Listeners are informed asynchronously
     * of the new intensity through the callback
     * {@link NavigationEventListener#onBeltDefaultVibrationIntensityChanged(int)}. When the
     * intensity is changed, a vibration feedback can be started on the belt.
     *
     * @param intensity The intensity to set in range [5-100]. This intensity is saved on the belt
     *                  and used for the navigation and compass mode.
     * @param vibrationFeedback <code>true</code> if a vibration feedback must be started when the
     *                          intensity has been changed, <code>false</code> otherwise.
     * @throws IllegalArgumentException If the intensity is not in range [5-100].
     */
    public void changeDefaultVibrationIntensity(int intensity, boolean vibrationFeedback)
            throws IllegalArgumentException {
        if (intensity < 5 || intensity > 100) {
            throw new IllegalArgumentException("The default vibration intensity must be in range " +
                    "[5-100].");
        }
        if (beltConnection.getState() != BeltConnectionState.STATE_CONNECTED) {
            return;
        }
        beltController.changeDefaultVibrationIntensity(intensity, vibrationFeedback);
    }

    /**
     * Returns the default vibration intensity. Returns <code>null</code> if no belt is connected.
     *
     * @return The default vibration intensity, or <code>null</code> if no belt is connected.
     */
    public Integer getDefaultVibrationIntensity() {
        return beltController.getDefaultVibrationIntensity();
    }

    /**
     * Returns the belt heading orientation (relative to magnetic North) in degrees. The angle is
     * positive clockwise.
     *
     * @return the belt heading, or <code>null</code> if the belt heading is unknown or no belt is
     * connected.
     */
    public Integer getBeltHeading() {
        BeltOrientation orientation = beltController.getOrientation();
        if (orientation == null) {
            return null;
        } else {
            return orientation.getBeltHeading();
        }
    }

    /**
     * Returns the accuracy flag for the belt orientation.
     *
     * @return the accuracy flag for the belt orientation, or <code>null</code> if the accuracy is
     * unknown or no belt is connected.
     */
    public Boolean isBeltOrientationAccurate() {
        BeltOrientation orientation = beltController.getOrientation();
        if (orientation == null) {
            return null;
        } else {
            return orientation.isOrientationAccurate();
        }
    }

    /**
     * Returns the battery level of the belt in percent.
     *
     * @return the battery level of the belt in percent, or <code>null</code> if the level is unknown
     * or no belt is connected.
     */
    public Integer getBeltBatteryLevel() {
        BeltBatteryStatus batteryStatus = beltController.getBatteryStatus();
        if (batteryStatus == null) {
            return null;
        } else {
            return (int) batteryStatus.getLevel();
        }
    }

    /**
     * Returns the power status of the belt.
     *
     * @return the power status of the belt, or <code>null</code> if the power status is unknown
     * or no belt is connected.
     */
    public PowerStatus getBeltPowerStatus() {
        BeltBatteryStatus batteryStatus = beltController.getBatteryStatus();
        if (batteryStatus == null) {
            return null;
        } else {
            return batteryStatus.getPowerStatus();
        }
    }

    /**
     * Enables or disables the compass accuracy signal on the belt. The compass accuracy signal
     * state is modified for the navigation, and also for compass and crossing modes.
     *
     * IMPORTANT: If the configuration of the compass accuracy signal is saved on the belt (i.e. the
     * <code>persistent</code> parameter is set to true), the user must be informed of this new
     * configuration as it will also impact the compass and crossing mode when no app is connected
     * to the belt.
     *
     * @param enable <code>true</code> to enable the compass accuracy signal, <code>false</code> to
     *               disable it.
     * @param persistent <code>true</code> to save the configuration on the belt, <code>false</code>
     *                   to set the configuration only for the current power-cycle of the belt (i.e.
     *                   this configuration is reset when the belt is powered off).
     */
    public void setCompassAccuracySignal(boolean enable, boolean persistent) {
        if (beltConnection.getState() == BeltConnectionState.STATE_CONNECTED) {
            beltController.changeCompassAccuracySignalState(enable, persistent);
        }
    }

    /**
     * Returns the state of the compass accuracy signal. The value may be unknown for a short period
     * after the connection to a belt.
     *
     * @return <code>true</code> if the compass accuracy signal is enable for the current and next
     * connections, <code>false</code> otherwise. Returns <code>null</code> if the value is unknown.
     */
    public Boolean isCompassAccuracySignalEnabled() {
        return compassAccuracySignalEnabled;
    }

    /**
     * Starts or resumes the navigation. The navigation can be started even when no belt is
     * connected. If the navigation is active when a belt is connected, the mode will be
     * automatically changed to the app mode with the right signal.
     *
     * @param direction The direction of the vibration signal in degrees. The value 0 represents the
     *                  magnetic North or heading of the belt, and angles are clockwise.
     * @param isMagneticBearing <code>true</code> if the direction is relative to magnetic North,
     *                          <code>false</code> if the direction is relative to the belt itself.
     * @param signal The type of vibration signal to use. If <code>null</code> is given, there is
     *               no vibration. Only repeated signals can be used.
     * @throws IllegalArgumentException If a temporary signal is given in argument.
     */
    public void startNavigation(int direction, boolean isMagneticBearing,
                                BeltVibrationSignal signal) throws IllegalArgumentException {
        if (signal != null && !signal.isRepeated()) {
            throw new IllegalArgumentException("The navigation signal must be a repeated signal.");
        }
        if (navigationState == NavigationState.NAVIGATING) {
            updateNavigationSignal(direction, isMagneticBearing, signal);
            return;
        }
        navigationDirection = direction;
        isMagneticBearingDirection = isMagneticBearing;
        navigationSignal = signal;
        navigationState = NavigationState.NAVIGATING;
        if (beltConnection.getState() == BeltConnectionState.STATE_CONNECTED) {
            if (beltController.getMode() == BeltMode.APP) {
                scheduleOrSendVibrationCommand();
            } else {
                beltController.changeMode(BeltMode.APP);
            }
        }
        notifyNavigationStateChanged();
    }

    /**
     * Updates the vibration signal.
     *
     * @param direction The direction of the vibration signal in degree. The value 0 represents the
     *                  magnetic North or heading of the belt, and angles are clockwise.
     * @param isMagneticBearing <code>true</code> if the direction is relative to magnetic North,
     *                          <code>false</code> if the direction is relative to the belt itself.
     * @param signal The type of vibration signal to use. If <code>null</code> is given, there is
     *               no vibration. Only repeated signals can be used.
     * @throws IllegalArgumentException If a temporary signal is given in argument.
     */
    public void updateNavigationSignal(int direction, boolean isMagneticBearing,
                                       BeltVibrationSignal signal) throws IllegalArgumentException {
        if (signal != null && !signal.isRepeated()) {
            throw new IllegalArgumentException("The navigation signal must be a repeated signal.");
        }
        navigationDirection = direction;
        isMagneticBearingDirection = isMagneticBearing;
        navigationSignal = signal;
        scheduleOrSendVibrationCommand();
    }

    /**
     * Pauses the navigation and changes the mode of the belt to Pause if connected and in App mode.
     */
    public void pauseNavigation() {
        if (navigationState != NavigationState.NAVIGATING) {
            return;
        }
        navigationState = NavigationState.PAUSED;
        if (beltConnection.getState() == BeltConnectionState.STATE_CONNECTED &&
                beltController.getMode() == BeltMode.APP) {
            beltController.changeMode(BeltMode.PAUSE);
        }
        notifyNavigationStateChanged();
    }

    /**
     * Stops the navigation and changes the mode of the belt to Wait if connected and in App mode.
     */
    public void stopNavigation() {
        if (navigationState == NavigationState.STOPPED) {
            return;
        }
        navigationState = NavigationState.STOPPED;
        if (beltConnection.getState() == BeltConnectionState.STATE_CONNECTED &&
                (beltController.getMode() == BeltMode.APP ||
                        (beltController.getMode() == BeltMode.PAUSE && isPauseModeForNavigation))) {
            beltController.changeMode(BeltMode.WAIT);
        }
        notifyNavigationStateChanged();
    }

    /**
     * Starts a destination reached signal.
     *
     * @param shouldStopNavigation <code>true</code> to stop the navigation.
     */
    public void notifyDestinationReached(boolean shouldStopNavigation) {
        if (shouldStopNavigation) {
            stopNavigation();
        }
        if (beltConnection.getState() == BeltConnectionState.STATE_CONNECTED) {
            beltController.signal(
                    BeltVibrationSignal.DESTINATION_REACHED_SINGLE,
                    null,
                    0,
                    null);
        }
    }

    /**
     * Starts a vibration notification in a given direction.
     *
     * @param direction The direction of the vibration signal in degree. The value 0 represents the
     *                  magnetic North or heading of the belt, and angles are clockwise.
     * @param isMagneticBearing <code>true</code> if the direction is relative to magnetic North,
     *                          <code>false</code> if the direction is relative to the belt itself.
     */
    public void notifyDirection(int direction, boolean isMagneticBearing) {
        if (beltConnection.getState() == BeltConnectionState.STATE_CONNECTED) {
            if (isMagneticBearing) {
                beltController.vibrateAtMagneticBearing(
                        direction,
                        null,
                        BeltVibrationSignal.DIRECTION_NOTIFICATION,
                        0,
                        null
                );
            } else {
                beltController.vibrateAtAngle(
                        direction,
                        null,
                        BeltVibrationSignal.DIRECTION_NOTIFICATION,
                        0,
                        null
                );
            }
        }
    }

    /**
     * Starts a warning vibration signal.
     *
     * @param critical <code>true</code> if a strong warning signal must be used.
     */
    public void notifyWarning(boolean critical) {
        if (beltConnection.getState() == BeltConnectionState.STATE_CONNECTED) {
            if (critical) {
                beltController.signal(
                        BeltVibrationSignal.CRITICAL_WARNING,
                        null,
                        0,
                        null);
            } else {
                beltController.signal(
                        BeltVibrationSignal.OPERATION_WARNING,
                        25,
                        0,
                        null);
            }

        }
    }

    /**
     * Starts a vibration signal to indicate the battery level of the belt.
     */
    public void notifyBeltBatteryLevel() {
        if (beltConnection.getState() == BeltConnectionState.STATE_CONNECTED) {
            beltController.signal(
                    BeltVibrationSignal.BATTERY_LEVEL,
                    null,
                    0,
                    null);
        }
    }

    /**
     * Returns the belt connection interface used by the navigation controller.
     *
     * IMPORTANT: Using the connection interface or command interface may interfere with the
     * navigation controller. You must check carefully the implementation of the navigation
     * controller before using the connection interface and associated command interface returned
     * by this method. If the navigation controller is too limited for your application,
     * you should consider using directly an instance of {@link BeltConnectionInterface} returned by
     * {@link BeltConnectionInterface#create(Context)}.
     *
     * @return the belt connection interface.
     */
    public BeltConnectionInterface getBeltConnection() {
        return beltConnection;
    }

    /**
     * Adds a listener for navigation events.
     *
     * @param listener The listener to add.
     */
    public void addNavigationEventListener(NavigationEventListener listener) {
        synchronized (this) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    /**
     * Removes a listener.
     *
     * @param listener The listener to remove.
     */
    public void removeNavigationEventListener(NavigationEventListener listener) {
        synchronized (this) {
            listeners.remove(listener);
        }
    }

    /**
     * Schedules or sends the vibration command for the navigation command according to the last
     * update time.
     *
     * This is used to avoid flooding the Bluetooth interface.
     */
    private void scheduleOrSendVibrationCommand() {
        if (isVibrationCommandScheduled.compareAndSet(false, true)) {
            long currentTimeNano = System.nanoTime();
            if (lastVibrationCommandNanoTime+MINIMUM_VIBRATION_COMMAND_UPDATE_PERIOD_NANO <
                    currentTimeNano) {
                // Send command
                lastVibrationCommandNanoTime = System.nanoTime();
                sendVibrationCommand(beltConnection, navigationDirection,
                        isMagneticBearingDirection, navigationSignal);
                isVibrationCommandScheduled.set(false);
            } else {
                // Schedule command
                try {
                    vibrationCommandTask = executor.schedule(new Runnable() {
                        @Override
                        public void run() {
                            sendVibrationCommand(beltConnection, navigationDirection,
                                    isMagneticBearingDirection, navigationSignal);
                            vibrationCommandTask = null;
                            isVibrationCommandScheduled.set(false);
                        }
                    }, MINIMUM_VIBRATION_COMMAND_UPDATE_PERIOD_NANO, TimeUnit.NANOSECONDS);
                } catch (Exception e) {
                    Log.e(DEBUG_TAG, "NavigationController: Unable to delay the " +
                            "vibration command.");
                    // Send command
                    lastVibrationCommandNanoTime = System.nanoTime();
                    sendVibrationCommand(beltConnection, navigationDirection,
                            isMagneticBearingDirection, navigationSignal);
                    isVibrationCommandScheduled.set(false);
                }
            }
        } // Else: a vibration command is already scheduled
    }

    /**
     * Sends the command for the navigation signal.
     *
     * <P>
     * This method can be overridden to customize the vibration signal.
     *
     * @param connection The belt connection.
     * @param direction The direction of the vibration signal in degree. The value 0 represents the
     *                  magnetic North or heading of the belt, and angles are clockwise.
     * @param isMagneticBearing <code>true</code> if the direction is relative to magnetic North,
     *                          <code>false</code> if the direction is relative to the belt itself.
     * @param signal The type of vibration signal to use. If <code>null</code> is given, there is
     *               no vibration. Only both directional and repeated signals can be used.
     */
    protected void sendVibrationCommand(BeltConnectionInterface connection, int direction,
            boolean isMagneticBearing, BeltVibrationSignal signal) {
        BeltCommandInterface controller = connection.getCommandInterface();
        if (connection.getState() != BeltConnectionState.STATE_CONNECTED ||
                controller.getMode() != BeltMode.APP) {
            return;
        }
        if (signal == null || !signal.isRepeated()) {
            // Stop the vibration
            controller.stopVibration(NAVIGATION_SIGNAL_CHANNEL);
        } else if (signal.isDirectional()) {
            if (isMagneticBearing) {
                controller.vibrateAtMagneticBearing(
                        direction,
                        null,
                        signal,
                        NAVIGATION_SIGNAL_CHANNEL,
                        null);
            } else {
                controller.vibrateAtAngle(
                        direction,
                        null,
                        signal,
                        NAVIGATION_SIGNAL_CHANNEL,
                        null);
            }
        } else {
            controller.signal(
                    signal,
                    null,
                    NAVIGATION_SIGNAL_CHANNEL,
                    null);
        }
    }

    /**
     * Notifies listeners of a navigation state change.
     */
    private void notifyNavigationStateChanged() {
        ArrayList<NavigationEventListener> targets;
        NavigationState state = navigationState;
        synchronized (this) {
            if (listeners.isEmpty()) {
                return;
            }
            targets = new ArrayList<>(listeners);
        }
        for (NavigationEventListener l: targets) {
            l.onNavigationStateChanged(state);
        }
    }

    /**
     * Notifies listeners that the home button has been pressed.
     *
     * @param navigating Flag to indicate that the home button has been pressed when navigating.
     */
    private void notifyHomeButtonPressed(boolean navigating) {
        ArrayList<NavigationEventListener> targets;
        synchronized (this) {
            if (listeners.isEmpty()) {
                return;
            }
            targets = new ArrayList<>(listeners);
        }
        for (NavigationEventListener l: targets) {
            l.onBeltHomeButtonPressed(navigating);
        }
    }

    /**
     * Notifies listeners that the default vibration intensity has changed.
     *
     * @param intensity The default vibration intensity.
     */
    private void notifyBeltDefaultVibrationIntensityChanged(int intensity) {
        ArrayList<NavigationEventListener> targets;
        synchronized (this) {
            if (listeners.isEmpty()) {
                return;
            }
            targets = new ArrayList<>(listeners);
        }
        for (NavigationEventListener l: targets) {
            l.onBeltDefaultVibrationIntensityChanged(intensity);
        }
    }

    /**
     * Notifies listeners that the belt battery level has been updated.
     * @param batteryLevel The battery level.
     * @param status The power status.
     */
    private void notifyBeltBatteryLevelUpdated(int batteryLevel, PowerStatus status) {
        ArrayList<NavigationEventListener> targets;
        synchronized (this) {
            if (listeners.isEmpty()) {
                return;
            }
            targets = new ArrayList<>(listeners);
        }
        for (NavigationEventListener l: targets) {
            l.onBeltBatteryLevelUpdated(batteryLevel, status);
        }
    }

    /**
     * Notifies listeners that the orientation has been updated.
     * @param beltHeading The belt heading.
     * @param accurate The accuracy flag.
     */
    private void notifyBeltOrientationUpdated(int beltHeading, boolean accurate) {
        ArrayList<NavigationEventListener> targets;
        synchronized (this) {
            if (listeners.isEmpty()) {
                return;
            }
            targets = new ArrayList<>(listeners);
        }
        for (NavigationEventListener l: targets) {
            l.onBeltOrientationUpdated(beltHeading, accurate);
        }
    }

    /**
     * Notifies listeners that the compass accuracy signal state has been retrieved or changed.
     * @param enabled <code>true</code> if the signal is enabled, <code>false</code> otherwise.
     */
    private void notifyCompassAccuracySignalStateUpdated(boolean enabled) {
        ArrayList<NavigationEventListener> targets;
        synchronized (this) {
            if (listeners.isEmpty()) {
                return;
            }
            targets = new ArrayList<>(listeners);
        }
        for (NavigationEventListener l: targets) {
            l.onCompassAccuracySignalStateUpdated(enabled);
        }
    }

    /**
     * Notifies listeners that the connection failed.
     */
    private void notifyBeltConnectionFailed() {
        ArrayList<NavigationEventListener> targets;
        synchronized (this) {
            if (listeners.isEmpty()) {
                return;
            }
            targets = new ArrayList<>(listeners);
        }
        for (NavigationEventListener l: targets) {
            l.onBeltConnectionFailed();
        }
    }

    /**
     * Notifies listeners that no belt has been found.
     */
    private void notifyNoBeltFound() {
        ArrayList<NavigationEventListener> targets;
        synchronized (this) {
            if (listeners.isEmpty()) {
                return;
            }
            targets = new ArrayList<>(listeners);
        }
        for (NavigationEventListener l: targets) {
            l.onNoBeltFound();
        }
    }

    /**
     * Notifies listeners that the connection has been lost.
     */
    private void notifyBeltConnectionLost() {
        ArrayList<NavigationEventListener> targets;
        synchronized (this) {
            if (listeners.isEmpty()) {
                return;
            }
            targets = new ArrayList<>(listeners);
        }
        for (NavigationEventListener l: targets) {
            l.onBeltConnectionLost();
        }
    }

    /**
     * Notifies listeners that the connection state has changed,
     * @param state The connection state.
     */
    private void notifyBeltConnectionStateChanged(BeltConnectionState state) {
        ArrayList<NavigationEventListener> targets;
        synchronized (this) {
            if (listeners.isEmpty()) {
                return;
            }
            targets = new ArrayList<>(listeners);
        }
        for (NavigationEventListener l: targets) {
            l.onBeltConnectionStateChanged(state);
        }
    }

    /**
     * Connection listener and command listener for the navigation controller.
     */
    class BeltListener implements BeltConnectionListener, BeltCommandListener {

        @Override
        public void onBeltModeChanged(BeltMode mode) {
            isPauseModeForNavigation = false;
            switch (mode) {
                case STANDBY:
                    // Nothing to do, the belt has been switched-off
                    break;
                case WAIT:
                    // The navigation should be in stop state
                    if (navigationState != NavigationState.STOPPED) {
                        Log.w(DEBUG_TAG, "NavigationController: Navigation state and " +
                                "belt mode out of sync.");
                        stopNavigation();
                    }
                    break;
                case APP:
                    // The navigation have been started
                    if (navigationState != NavigationState.NAVIGATING) {
                        Log.w(DEBUG_TAG, "NavigationController: Navigation state and " +
                                "belt mode out of sync.");
                        if (navigationState == NavigationState.STOPPED) {
                            beltController.changeMode(BeltMode.WAIT);
                        } else if (navigationState == NavigationState.PAUSED) {
                            beltController.changeMode(BeltMode.PAUSE);
                        }
                    } else {
                        scheduleOrSendVibrationCommand();
                    }
                    break;
                case PAUSE:
                    // The navigation has been paused
                    if (navigationState != NavigationState.PAUSED) {
                        pauseNavigation();
                    } else {
                        isPauseModeForNavigation = true;
                    }
                    break;
                case COMPASS:
                case CALIBRATION:
                case CROSSING:
                    // The navigation should be in pause or stop state
                    if (navigationState == NavigationState.NAVIGATING) {
                        Log.w(DEBUG_TAG, "NavigationController: Navigation state and " +
                                "belt mode out of sync.");
                        pauseNavigation();
                    }
                    break;
                case UNKNOWN:
                    // Nothing to do
                    break;
            }
        }

        @Override
        public void onBeltButtonPressed(BeltButtonPressEvent beltButtonPressEvent) {
            isPauseModeForNavigation = false;
            if (beltButtonPressEvent.getButton() == BeltButton.HOME &&
                    beltButtonPressEvent.getPreviousMode() ==
                            beltButtonPressEvent.getSubsequentMode()) {
                // Home button pressed for application action
                // Note: Home button can be pressed to stop calibration and return to wait mode.
                switch (navigationState) {
                    case STOPPED:
                        // Should not be in app mode
                        if (beltController.getMode() == BeltMode.APP) {
                            Log.w(DEBUG_TAG, "NavigationController: Navigation state and " +
                                    "belt mode out of sync.");
                            beltController.changeMode(BeltMode.WAIT);
                        } else {
                            notifyHomeButtonPressed(false);
                        }
                        break;
                    case PAUSED:
                        // Resume navigation
                        startNavigation(navigationDirection, isMagneticBearingDirection,
                                navigationSignal);
                        break;
                    case NAVIGATING:
                        // Should be in app mode
                        if (beltController.getMode() != BeltMode.APP) {
                            Log.w(DEBUG_TAG, "NavigationController: Navigation state and " +
                                    "belt mode out of sync.");
                            beltController.changeMode(BeltMode.APP);
                        } else {
                            notifyHomeButtonPressed(true);
                        }
                        break;
                }
            } else if (beltButtonPressEvent.getButton() == BeltButton.PAUSE &&
                    beltButtonPressEvent.getPreviousMode() ==
                            beltButtonPressEvent.getSubsequentMode()) {
                // Pause button pressed for application pause or resume
                if (beltButtonPressEvent.getSubsequentMode() == BeltMode.APP) {
                    // Pause request from belt (should be navigating)
                    if (navigationState == NavigationState.PAUSED) {
                        Log.w(DEBUG_TAG, "NavigationController: Navigation state and " +
                                "belt mode out of sync.");
                        beltController.changeMode(BeltMode.PAUSE);
                    } else if (navigationState == NavigationState.STOPPED) {
                        Log.w(DEBUG_TAG, "NavigationController: Navigation state and " +
                                "belt mode out of sync.");
                        beltController.changeMode(BeltMode.WAIT);
                    } else {
                        pauseNavigation();
                    }
                } else if (beltButtonPressEvent.getSubsequentMode() == BeltMode.PAUSE) {
                    // Resume request from belt (should be in pause state)
                    if (navigationState == NavigationState.NAVIGATING) {
                        Log.w(DEBUG_TAG, "NavigationController: Navigation state and " +
                                "belt mode out of sync.");
                        beltController.changeMode(BeltMode.APP);
                    } else if (navigationState == NavigationState.STOPPED) {
                        Log.w(DEBUG_TAG, "NavigationController: Navigation state and " +
                                "belt mode out of sync.");
                        beltController.changeMode(BeltMode.WAIT);
                    } else {
                        startNavigation(navigationDirection, isMagneticBearingDirection,
                                navigationSignal);
                    }
                } // Note: Pause button changes the mode in any other cases.
            } else if (beltButtonPressEvent.getSubsequentMode() != BeltMode.APP) {
                // Pause the navigation if navigating
                // Note: The mode cannot be changed to app mode with a button press.
                if (navigationState == NavigationState.NAVIGATING) {
                    pauseNavigation();
                }
            }
        }

        @Override
        public void onBeltDefaultVibrationIntensityChanged(int intensity) {
            notifyBeltDefaultVibrationIntensityChanged(intensity);
        }

        @Override
        public void onBeltBatteryStatusUpdated(BeltBatteryStatus status) {
            notifyBeltBatteryLevelUpdated((int) status.getLevel(), status.getPowerStatus());
        }

        @Override
        public void onBeltOrientationUpdated(BeltOrientation orientation) {
            notifyBeltOrientationUpdated(orientation.getBeltHeading(),
                    (orientation.isOrientationAccurate()==null)?(true):
                            (orientation.isOrientationAccurate()));
        }

        @Override
        public void onBeltCompassAccuracySignalStateNotified(boolean signalEnabled) {
            compassAccuracySignalEnabled = signalEnabled;
            notifyCompassAccuracySignalStateUpdated(compassAccuracySignalEnabled);
        }

        @Override
        public void onScanFailed() {
            notifyBeltConnectionFailed();
        }

        @Override
        public void onNoBeltFound() {
            notifyNoBeltFound();
        }

        @Override
        public void onBeltFound(BluetoothDevice belt) {
            // Nothing to do, connection should be automatic
        }

        @Override
        public void onConnectionStateChange(BeltConnectionState state) {
            isPauseModeForNavigation = false;
            compassAccuracySignalEnabled = null;
            if (state == BeltConnectionState.STATE_CONNECTED) {
                beltController.setOrientationNotificationsActive(true);
                beltController.requestCompassAccuracySignalState();
                if (navigationState == NavigationState.NAVIGATING) {
                    if (beltController.getMode() == BeltMode.APP) {
                        scheduleOrSendVibrationCommand();
                    } else {
                        beltController.changeMode(BeltMode.APP);
                    }
                }
            }
            notifyBeltConnectionStateChanged(state);
        }

        @Override
        public void onConnectionLost() {
            notifyBeltConnectionLost();
        }

        @Override
        public void onConnectionFailed() {
            notifyBeltConnectionFailed();
        }
    }
}
