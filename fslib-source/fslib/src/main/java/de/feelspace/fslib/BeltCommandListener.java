/*
 * Copyright (c) 2017-2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */
package de.feelspace.fslib;

/**
 * Interface to listen to belt notifications.
 */
public interface BeltCommandListener {

    /**
     * This method is called when the mode of the belt has changed. If the mode is changed with a
     * button press, the method
     * {@link BeltCommandListener#onBeltButtonPressed(BeltButtonPressEvent)} is called instead.
     *
     * @param mode the new mode of the belt.
     */
    void onBeltModeChanged(BeltMode mode);

    /**
     * This method is called when a button on the belt is pressed.
     *
     * Important: If the mode of the belt was changed by the press event, information about the
     * previous and new modes is given by the press event parameter. The callback
     * {@link BeltCommandListener#onBeltModeChanged(BeltMode)} is NOT called when the mode
     * changes from a button press.
     *
     * @param beltButtonPressEvent the button pressed with information on the current belt mode.
     */
    void onBeltButtonPressed(BeltButtonPressEvent beltButtonPressEvent);

    /**
     * Informs that the default intensity value has changed. The default vibration intensity is used
     * in compass and crossing mode. The range of values is [5-100], and value below 5 are not
     * allowed to avoid having no vibration in compass and crossing modes. The default intensity can
     * be changed directly on the belt or by the application, and in both cases this callback is
     * used to inform about the new default intensity.
     *
     * @param intensity The new default intensity set on the belt.
     */
    void onBeltDefaultVibrationIntensityChanged(int intensity);

    /**
     * Informs that the status (level or state) of the belt's battery has been updated.
     *
     * @param status The new status of the belt's battery.
     */
    void onBeltBatteryStatusUpdated(BeltBatteryStatus status);

    /**
     * Called when a notification of the belt's orientation has been received.
     *
     * @param orientation The notified orientation.
     */
    void onBeltOrientationUpdated(BeltOrientation orientation);

    /**
     * Called when the inaccurate compass signal has been requested or changed.
     *
     * @param signalEnabled <code>true</code> if the inaccurate compass signal is enabled in app
     *                      mode, <code>false</code> otherwise.
     */
    void onBeltCompassAccuracySignalStateNotified(boolean signalEnabled);

}
