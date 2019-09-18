/*
 * Copyright (c) 2017-2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */
package de.feelspace.fslib;

/**
 * Interface to control the belt.
 *
 * <b>Belt mode for vibration command:</b>
 * Vibration signals can only be started in app mode except if the vibration signal has a limited
 * duration and is executed on channel index 0. However, it is not recommended to start a vibration
 * signal when the belt is not in app mode.
 */
public interface BeltCommandInterface {

    /**
     * Makes the belt vibrate toward a direction relative to the magnetic north.
     *
     * @param direction The relative direction of the vibration in degrees. The value 0 represents
     *                  the magnetic north and positive angles are clockwise.
     * @param intensity The intensity of the vibration in range 0 to 100 or <code>null</code> to use
     *                  the default intensity. When the intensity provided is 0 the vibration is
     *                  stopped for the channel index. The minimum intensity for a vibration is 5,
     *                  and any value between 1 and 4 will be changed to 5.
     * @param vibration The type of vibration signal to use. When <code>null</code> a continuous
     *                  vibration signal is used. Only directional signals can be used.
     * @param channelIndex The channel of the vibration in range [0-5] (to manage multiple
     *                     vibrations) or <code>null</code>. When <code>null</code> the channel
     *                     index 1 is used for signal with limited duration, channel index 2 is used
     *                     for continuous or repeated signals.
     * @param stopOtherChannels <code>true</code> to stop vibration on other channels. When
     *                          <code>null</code> other channels are not stopped.
     * @return <code>true</code> if the command for the signal has been sent, <code>false</code> if
     * no connection is available, the mode of the belt is incorrect, the signal is not directional,
     * or the channel index is out of range.
     */
    boolean vibrateAtMagneticBearing(
            float direction, Integer intensity, BeltVibrationSignal vibration,
            Integer channelIndex, Boolean stopOtherChannels);

    /**
     * Makes the belt vibrate at a specific place (vibro-motor) given by an angle.
     *
     * @param angle The angle in degrees at which the belt must vibrate. The value 0 represents the
     *              front of the belt, and positive angles are clockwise.
     * @param intensity The intensity of the vibration in range 0 to 100 or <code>null</code> to use
     *                  the default intensity. When the intensity provided is 0 the vibration is
     *                  stopped for the channel index. The minimum intensity for a vibration is 5,
     *                  and any value between 1 and 4 will be changed to 5.
     * @param vibration The type of vibration signal to use. When <code>null</code> a continuous
     *                  vibration signal is used. Only directional signals can be used.
     * @param channelIndex The channel of the vibration in range [0-5] (to manage multiple
     *                     vibrations) or <code>null</code>. When <code>null</code> the channel
     *                     index 1 is used for signal with limited duration, channel index 2 is used
     *                     for continuous or repeated signals.
     * @param stopOtherChannels <code>true</code> to stop vibration on other channels. When
     *                          <code>null</code> other channels are not stopped.
     * @return <code>true</code> if the command for the signal has been sent, <code>false</code> if
     * no connection is available, the mode of the belt is incorrect, the signal is not directional,
     * or the channel index is out of range.
     */
    boolean vibrateAtAngle(
            float angle, Integer intensity, BeltVibrationSignal vibration, Integer channelIndex,
            Boolean stopOtherChannels);

    /**
     * Makes the belt vibrate at specific positions (vibration motors) given by index.
     *
     * @param positions The positions as index. Index 0 represents the front of the belt, and
     *                  vibration motors are numbered clockwise. A maximum of 6 motors can vibrate
     *                  simultaneously. If an index is outside the range [0-15] a modulo 16
     *                  operation is applied. If no position is given the vibration is stopped on
     *                  the channel.
     * @param intensity The intensity of the vibration in range 0 to 100 or <code>null</code> to use
     *                  the default intensity. When the intensity provided is 0 the vibration is
     *                  stopped for the channel index. The minimum intensity for a vibration is 5,
     *                  and any value between 1 and 4 will be changed to 5.
     * @param vibration The type of vibration signal to use. When <code>null</code> a continuous
     *                  vibration signal is used. Only directional signals can be used.
     * @param channelIndex The channel of the vibration in range [0-5] (to manage multiple
     *                     vibrations) or <code>null</code>. When <code>null</code> the channel
     *                     index 1 is used for signal with limited duration, channel index 2 is used
     *                     for continuous or repeated signals.
     * @param stopOtherChannels <code>true</code> to stop vibration on other channels. When
     *                          <code>null</code> other channels are not stopped.
     * @return <code>true</code> if the command for the signal has been sent, <code>false</code> if
     * no connection is available, the mode of the belt is incorrect, the signal is not directional,
     * or the channel index is out of range.
     */
    boolean vibrateAtPositions(
            int[] positions, Integer intensity, BeltVibrationSignal vibration,
            Integer channelIndex, Boolean stopOtherChannels);

    /**
     * Starts a vibration pulse toward a direction relative to the magnetic north.
     *
     * @param direction The relative direction of the vibration in degrees. The value 0 represents
     *                  the magnetic north and positive angles are clockwise.
     * @param onDurationMs The duration of the vibration for one pulse in milliseconds. The
     *                        value must be in range [1-65535].
     * @param periodMs The repetition period of the pulse in milliseconds. The value must be in
     *                      range [1-65535] and at least the value of the pulse duration.
     * @param iterations The number of pulse to perform in range [0-127]. If <code>null</code>, or 0
     *                  the pulse is repeated indefinitely.
     * @param intensity The intensity of the vibration in range 0 to 100 or <code>null</code> to use
     *                  the default intensity. When the intensity provided is 0 the vibration is
     *                  stopped for the channel index. The minimum intensity for a vibration is 5,
     *                  and any value between 1 and 4 will be changed to 5.
     * @param channelIndex The channel of the vibration in range [0-5] (to manage multiple
     *                     vibrations) or <code>null</code>. When <code>null</code> the channel
     *                     index 1 is used for limited iterations, channel index 2 is used for
     *                     undetermined iterations.
     * @param stopOtherChannels <code>true</code> to stop vibration on other channels. When
     *                          <code>null</code> other channels are not stopped.
     * @return <code>true</code> if the command for the pulse has been sent, <code>false</code> if
     * no connection is available or a parameter is incorrect.
     */
    boolean pulseAtMagneticBearing(
            float direction, int onDurationMs, int periodMs, Integer iterations,
            Integer intensity, Integer channelIndex, Boolean stopOtherChannels);

    /**
     * Starts a vibration pulse at a specific place (vibro-motor) given by an angle.
     *
     * @param angle The angle in degrees at which the belt must vibrate. The value 0 represents the
     *              front of the belt, and positive angles are clockwise.
     * @param onDurationMs The duration of the vibration for one pulse in milliseconds. The
     *                        value must be in range [1-65535].
     * @param periodMs The repetition period of the pulse in milliseconds. The value must be in
     *                      range [1-65535] and at least the value of the pulse duration.
     * @param iterations The number of pulse to perform in range [0-127]. If <code>null</code>, or 0
     *                  the pulse is repeated indefinitely.
     * @param intensity The intensity of the vibration in range 0 to 100 or <code>null</code> to use
     *                  the default intensity. When the intensity provided is 0 the vibration is
     *                  stopped for the channel index. The minimum intensity for a vibration is 5,
     *                  and any value between 1 and 4 will be changed to 5.
     * @param channelIndex The channel of the vibration in range [0-5] (to manage multiple
     *                     vibrations) or <code>null</code>. When <code>null</code> the channel
     *                     index 1 is used for limited iterations, channel index 2 is used for
     *                     undetermined iterations.
     * @param stopOtherChannels <code>true</code> to stop vibration on other channels. When
     *                          <code>null</code> other channels are not stopped.
     * @return <code>true</code> if the command for the pulse has been sent, <code>false</code> if
     * no connection is available or a parameter is incorrect.
     */
    boolean pulseAtAngle(
            float angle, int onDurationMs, int periodMs, Integer iterations,
            Integer intensity, Integer channelIndex, Boolean stopOtherChannels);

    /**
     * Starts a vibration pulse at specific positions (vibration motors) given by index.
     *
     * @param positions The positions as index. Index 0 represents the front of the belt, and
     *                  vibration motors are numbered clockwise. A maximum of 6 motors can vibrate
     *                  simultaneously. If an index is outside the range [0-15] a modulo 16
     *                  operation is applied. If no position is given the vibration is stopped on
     *                  the channel.
     * @param onDurationMs The duration of the vibration for one pulse in milliseconds. The
     *                        value must be in range [1-65535].
     * @param periodMs The repetition period of the pulse in milliseconds. The value must be in
     *                      range [1-65535] and at least the value of the pulse duration.
     * @param iterations The number of pulse to perform in range [0-127]. If <code>null</code>, or 0
     *                  the pulse is repeated indefinitely.
     * @param intensity The intensity of the vibration in range 0 to 100 or <code>null</code> to use
     *                  the default intensity. When the intensity provided is 0 the vibration is
     *                  stopped for the channel index. The minimum intensity for a vibration is 5,
     *                  and any value between 1 and 4 will be changed to 5.
     * @param channelIndex The channel of the vibration in range [0-5] (to manage multiple
     *                     vibrations) or <code>null</code>. When <code>null</code> the channel
     *                     index 1 is used for limited iterations, channel index 2 is used for
     *                     undetermined iterations.
     * @param stopOtherChannels <code>true</code> to stop vibration on other channels. When
     *                          <code>null</code> other channels are not stopped.
     * @return <code>true</code> if the command for the pulse has been sent, <code>false</code> if
     * no connection is available or a parameter is incorrect.
     */
    boolean pulseAtPositions(
            int[] positions, int onDurationMs, int periodMs, Integer iterations,
            Integer intensity, Integer channelIndex, Boolean stopOtherChannels);

    /**
     * Starts a non-directional vibration signal.
     *
     * @param vibration The vibration signal to start. Only non-directional signals can be used.
     * @param intensity The intensity of the vibration in range 0 to 100 or <code>null</code> to use
     *                  the default intensity. When the intensity provided is 0 the vibration is
     *                  stopped for the channel index. The minimum intensity for a vibration is 5,
     *                  and any value between 1 and 4 will be changed to 5.
     * @param channelIndex The channel of the vibration in range [0-5] (to manage multiple
     *                     vibrations) or <code>null</code>. When <code>null</code> the channel
     *                     index 1 is used for signal with limited duration, channel index 2 is used
     *                     for continuous or repeated signals.
     * @param stopOtherChannels <code>true</code> to stop vibration on other channels. When
     *                          <code>null</code> other channels are not stopped.
     * @return <code>true</code> if the command for the signal has been sent, <code>false</code> if
     * no connection is available, the mode of the belt is incorrect, the signal is directional,
     * or the channel index is out of range.
     */
    boolean signal(
            BeltVibrationSignal vibration, Integer intensity, Integer channelIndex,
            Boolean stopOtherChannels);


    /**
     * Stops the vibration for all or specific channels. Only vibration in app mode can be stopped.
     *
     * @param channelIndex The channel index for which the vibration must stop, or no parameter
     *                     to stop the vibration on all channels.
     */
    boolean stopVibration(int... channelIndex);

    /**
     * Requests the belt to change its mode. Listeners are notified of the mode change using
     * {@link BeltCommandListener#onBeltModeChanged(BeltMode)} when the mode change is
     * effective.
     *
     * @param mode the mode requested.
     * @return <code>true</code> if the request has been sent successfully, <code>false</code> if
     * no connection is available or the mode in parameter is <code>null</code>.
     */
    boolean changeMode(BeltMode mode);

    /**
     * Returns the current mode of the belt.
     *
     * @return the current mode of the belt. Returns {@link BeltMode#UNKNOWN} if
     * no belt is connected.
     */
    BeltMode getMode();

    /**
     * Sets the default intensity of vibration. The default intensity value is saved on the belt and
     * used in compass and crossing mode. The default intensity can also be changed by the user
     * directly on the belt. Listeners are notified of intensity changes with the callback
     * {@link BeltCommandListener#onBeltDefaultVibrationIntensityChanged(int)} when the change is
     * acknowledged by the belt.
     *
     * @param intensity the default intensity to be set. The value must be in range [5-100]. Values
     *                  below 5 are not allowed to avoid having no vibration in compass and crossing
     *                  modes.
     * @param vibrationFeedback <code>true</code> to obtain a vibration feedback signal for the new
     *                          intensity, <code>false</code> otherwise.
     *
     */
    boolean changeDefaultVibrationIntensity(int intensity, boolean vibrationFeedback);

    /**
     * Returns the last known default intensity value of the belt.
     *
     * @return the last known default intensity value of the belt, or <code>null</code> if the belt
     * is not connected or the default intensity is unknown.
     */
    Integer getDefaultVibrationIntensity();

    /**
     * Returns the last known battery status of the belt.
     *
     * @return the last known battery status of the belt, or <code>null</code> if the belt is not
     * connected or the battery status is unknown.
     */
    BeltBatteryStatus getBatteryStatus();

    /**
     * Returns the last known orientation of the belt. To obtain the orientation of the belt the
     * notifications must be activated using
     * {@link BeltCommandInterface#setOrientationNotificationsActive(boolean)}.
     *
     * @return the last known orientation of the belt, or <code>null</code> if the belt is not
     * connected or the orientation is unknown.
     */
    BeltOrientation getOrientation();

    /**
     * Sets the state of orientation notifications.
     *
     * @param active <code>true</code> to activate orientation notifications, <code>false</code> to
     *               deactivate.
     * @return <code>true</code> if the request to activate or deactivate orientation has been sent,
     * <code>false</code> if no belt is connected.
     */
    boolean setOrientationNotificationsActive(boolean active);

    /**
     * Returns <code>true</code> if the orientation notifications are active, <code>false</code>
     * otherwise.
     *
     * @return <code>true</code> if the orientation notifications are active, <code>false</code>
     * otherwise.
     */
    boolean areOrientationNotificationsActive();

    /**
     * Plays a sound on the belt buzzer.
     *
     * @param sound the sound to play.
     * @return <code>true</code> if the request to play a sound has been sent, <code>false</code> if
     * no belt is connected.
     */
    boolean playSound(BeltSound sound);

    /**
     * Adds a listener to belt notifications.
     *
     * @param listener the listener to add.
     */
    void addCommandListener(BeltCommandListener listener);

    /**
     * Removes a listener from belt notifications.
     *
     * @param listener the listener to remove.
     */
    void removeCommandListener(BeltCommandListener listener);

    /**
     * Enables or disables the vibration signal for inaccurate compass. The state of the signal is
     * changed only for the current power cycle of the belt, i.e. this
     * configuration is reset when the belt is powered off. Disabling the inaccurate compass signal
     * is useful for non-navigation application.
     *
     * Listeners are informed when the state of the signal is changed with the callback
     * {@link BeltCommandListener#onBeltCompassAccuracySignalStateNotified(boolean)}.
     *
     * @param enableSignal <code>true</code> to enable the inaccurate compass signal,
     *                     <code>false</code> to disable it.
     * @return <code>true</code> if the request has been sent, <code>false</code> if no belt is
     * connected.
     */
    boolean changeCompassAccuracySignalState(boolean enableSignal);

    /**
     * Requests the state of the inaccurate compass signal. The state of the signal is notified with
     * the callback {@link BeltCommandListener#onBeltCompassAccuracySignalStateNotified(boolean)}.
     *
     * @return <code>true</code> if the request has been sent, <code>false</code> if no belt is
     * connected.
     */
    boolean requestCompassAccuracySignalState();

}
