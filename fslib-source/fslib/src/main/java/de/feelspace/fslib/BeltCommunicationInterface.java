/*
 * Copyright (c) 2015-2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */
package de.feelspace.fslib;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * The belt communication interface provides advanced methods for sending instructions to the belt
 * and receiving notifications. For most application it is recommended to only use the
 * {@link BeltCommandListener} to control the belt, that provides simpler methods.
 */
public interface BeltCommunicationInterface extends BeltCommandInterface {

    /**
     * Returns the firmware version of the connected belt. The firmware version is requested during
     * handshake.
     *
     * @return the firmware version of the connected belt or <code>null</code> if no belt is
     * connected or the firmware version is not yet known.
     */
    Integer getFirmwareVersion();

    /**
     * Requests the value of a parameter to the belt.
     *
     * @param beltParameter The requested parameter.
     * @return <code>true</code> if the request has been sent, <code>false</code> if no belt is
     * connected.
     */
    boolean requestParameterValue(BeltParameter beltParameter);

    /**
     * Returns the last known value of a parameter stored locally. Returns <code>null</code> if the
     * parameter value has yet been retrieved or there is no connection to a belt.
     *
     * @param beltParameter The parameter to retrieve.
     * @return the last value of a parameter stored locally.
     */
    Object getParameterValue(BeltParameter beltParameter);

    /**
     * Sends a request to the belt for changing the value of a parameter.
     *
     * @param beltParameter The ID of the parameter to change.
     * @param parameterValue The value to set.
     * @param persistent From firmware version 41. <code>true</code> if the value must be saved by
     *                   the belt, <code>false</code> to change temporarily the value. A temporary
     *                   value is reset when the belt is powered-off.
     * @return <code>true</code> if the request has been sent, <code>false</code> if no belt is
     * connected or the parameter value is not valid.
     */
    boolean changeParameterValue(
            BeltParameter beltParameter, Object parameterValue, boolean persistent);

    /**
     * Resets the parameters of the belt to their factory default.
     *
     * IMPORTANT: The reset command is only for testing purposes. It is recommended to
     * manually turn off and on the belt after a reset.
     *
     * (This command is available only from firmware 43 which is the commercial firmware version.)
     *
     * @param parameterReset <code>true</code> to reset the parameters.
     * @param bluetoothReset <code>true</code> to reset the bluetooth module, including the
     *                       Bluetooth name. The connection is closed when the Bluetooth module is
     *                       reset.
     * @param sensorReset <code>true</code> to reset the sensors.
     * @return <code>true</code> if the reset command has been sent.
     */
    boolean reset(boolean parameterReset, boolean bluetoothReset, boolean sensorReset);

    /**
     * Plays a buzzer tone pattern on the belt.
     *
     * IMPORTANT: This command is available from belt firmware version 45.
     *
     * @param tonePattern the tone pattern to play.
     * @return <code>true</code> if the request has been sent, <code>false</code> if no belt is
     * connected or the tone pattern is <code>null</code>.
     */
    boolean playTonePattern(BuzzerTonePattern tonePattern);

    /**
     * Sends a pulse command to the connected belt.
     *
     * IMPORTANT: This command is available only from belt firmware version 45. The application must
     * check the firmware version before using it. For general public applications it is recommended
     * to use {@link BeltCommunicationInterface#sendChannelConfigurationCommand(ChannelConfigurationCommand)}
     * instead.
     *
     * @param command The command to send.
     * @return <code>true</code> if the command has been sent, <code>false</code> if no belt is
     * connected.
     */
    boolean sendPulseCommand(PulseCommand command);

    /**
     * Sends a channel configuration command to the belt.
     *
     * @param command The command to send.
     * @return <code>true</code> if the command has been sent, <code>false</code> if no belt is
     * connected.
     */
    boolean sendChannelConfigurationCommand(ChannelConfigurationCommand command);

    /**
     * Requests the belt to start a system signal.
     *
     * @param signal The vibration signal to start.
     * @return <code>true</code> if the command has been sent, <code>false</code> if no belt is
     * connected.
     */
    boolean startSystemSignal(BeltSystemSignal signal);

    /**
     * Adds a listener to belt notifications.
     *
     * @param listener the listener to add.
     */
    void addCommunicationListener(BeltCommunicationListener listener);

    /**
     * Removes a listener from belt notifications.
     *
     * @param listener the listener to remove.
     */
    void removeCommunicationListener(BeltCommunicationListener listener);

}
