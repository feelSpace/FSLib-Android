package de.feelspace.fslib;

/**
 * Interface for listening navigation events.
 */
public interface NavigationEventListener {

    /**
     * Called when the navigation state changed. The state may be changed by the application or with
     * a button press on the belt.
     *
     * @param state The new navigation state.
     */
    void onNavigationStateChanged(NavigationState state);

    /**
     * Called when the home button on the belt has been pressed and does not result in resuming the
     * navigation.
     *
     * @param navigating <code>true</code> if the home button has been pressed when navigating.
     */
    void onBeltHomeButtonPressed(boolean navigating);

    /**
     * Called when the default vibration intensity has been changed. The intensity can be changed
     * by the application or using buttons on the belt.
     *
     * @param intensity The new default vibration intensity.
     */
    void onBeltDefaultVibrationIntensityChanged(int intensity);

    /**
     * Called when the orientation of the belt has been notified.
     *
     * @param beltHeading The belt heading in degree (relative to magnetic North). Angles are
     *                    clockwise.
     * @param accurate The orientation accuracy flag.
     */
    void onBeltOrientationUpdated(int beltHeading, boolean accurate);

    /**
     * Called when the battery level of the belt has been notified.
     *
     * @param batteryLevel The battery levelt of the belt in percent.
     * @param status The power status of the belt.
     */
    void onBeltBatteryLevelUpdated(int batteryLevel, PowerStatus status);

    /**
     * Called when the connection state has changed.
     *
     * @param state The new connection state.
     */
    void onBeltConnectionStateChanged(BeltConnectionState state);

    /**
     * Called when the connection with a belt has been lost.
     */
    void onBeltConnectionLost();

    /**
     * Called when the connection with a belt failed.
     */
    void onBeltConnectionFailed();

    /**
     * Called when no belt has been found to start the connection.
     */
    void onNoBeltFound();

}
