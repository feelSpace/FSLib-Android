package de.feelspace.fslibtest;

/**
 * Callback interface for checking and activating Bluetooth.
 */
public interface BluetoothCheckCallback {

    /**
     * Called when Bluetooth is ready.
     */
    void onBluetoothReady();

    /**
     * Called when one of the steps for activating Bluetooth has been rejected.
     */
    void onBluetoothActivationRejected();

    /**
     * Called when one of the steps for activating Bluetooth has failed.
     */
    void onBluetoothActivationFailed();

    /**
     * Called when BLE or a related feature is not supported.
     */
    void onUnsupportedFeature();

}
