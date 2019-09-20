package de.feelspace.fslib;

public interface NavigationEventListener {

    void onNavigationStateChanged(NavigationState state);

    void onBeltHomeButtonPressed(boolean navigating);

    void onBeltDefaultVibrationIntensityChanged(int intensity);

    void onBeltOrientationUpdated(int beltHeading, boolean accurate);

    void onBeltBatteryLevelUpdated(int batteryLevel, PowerStatus status);

    void onBeltConnectionStateChanged(BeltConnectionState state);

    void onBeltConnectionLost();

    void onBeltConnectionFailed();

    void onNoBeltFound();

}
