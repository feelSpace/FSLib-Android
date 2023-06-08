/*
 * Copyright (c) 2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */

package de.feelspace.fslib;

/**
 * Belt channel configuration.
 */
public class ChannelConfigurationCommand {

    // Channel index
    private int channelIndex;

    // Vibration pattern
    private BeltVibrationPattern vibrationPattern;

    // Intensity of the vibration
    private int intensity;

    // Type of orientation
    private OrientationType orientationType;

    // Orientation of the vibration
    private int orientationValue;

    // Number of iterations
    private int iterations;

    // Period of the pattern in milliseconds
    private int periodMs;

    // Initial value of the pattern timer in milliseconds
    private int initialTimerValueMs;

    // Combination of the vibration with other channels
    private boolean exclusiveChannel;

    // Clear other channels
    private boolean clearOtherChannels;

    /**
     * Constructor.
     *
     * @throws IllegalArgumentException If an argument has illegal value.
     */
    public ChannelConfigurationCommand(
            int channelIndex, BeltVibrationPattern vibrationPattern, int intensity,
            OrientationType orientationType, int orientationValue, int iterations,
            int periodMs, int initialTimerValueMs, boolean exclusiveChannel,
            boolean clearOtherChannels) {

        if (channelIndex < 0 || channelIndex > 5) {
            throw new IllegalArgumentException("Channel index must be in range [0-5].");
        }
        if (vibrationPattern == null) {
            throw new IllegalArgumentException("Illegal vibration pattern.");
        }
        if (orientationType == null) {
            throw new IllegalArgumentException("Illegal orientation type.");
        }
        if (orientationType == OrientationType.BINARY_MASK && vibrationPattern.isBinaryMask()) {
            throw new IllegalArgumentException("Incompatible orientation type and pattern.");
        }
        switch (orientationType) {
            case BINARY_MASK:
                if (orientationValue > 0xFFFF || Integer.bitCount(orientationValue) > 6) {
                    throw new IllegalArgumentException("Illegal orientation value.");
                }
                break;
            case VIBROMOTOR_INDEX:
                if (orientationValue < 0 || orientationValue > 16) {
                    throw new IllegalArgumentException("Illegal orientation value.");
                }
                break;
            case ANGLE:
            case BEARING:
                orientationValue = orientationValue%360;
                if (orientationValue < 0) {
                    orientationValue += 360;
                }
                break;
        }
        if (intensity < 0 || (intensity>100 &&
                intensity!=BeltCommunicationInterface.DEFAULT_INTENSITY_CODE)) {
            throw new IllegalArgumentException("Illegal intensity value.");
        }
        if (iterations > 127 || iterations < -128) {
            throw new IllegalArgumentException("Illegal pattern iterations value.");
        }
        if (iterations < 0) {
            iterations = 0;
        }
        if (periodMs < 0 || periodMs > 0xFFFF) {
            throw new IllegalArgumentException("Illegal pattern period value.");
        }
        if (initialTimerValueMs < 0 || initialTimerValueMs > 0xFFFF) {
            throw new IllegalArgumentException("Illegal pattern timer initial value.");
        }
        this.channelIndex = channelIndex;
        this.vibrationPattern = vibrationPattern;
        this.orientationType = orientationType;
        this.orientationValue = orientationValue;
        this.intensity = intensity;
        this.iterations = iterations;
        this.periodMs = periodMs;
        this.initialTimerValueMs = initialTimerValueMs;
        this.exclusiveChannel = exclusiveChannel;
        this.clearOtherChannels = clearOtherChannels;
    }

    /**
     * Returns the packet for this channel configuration command.
     *
     * @return the packet for this command.
     */
    public byte[] getPacket() {
        return new byte[] {
                (byte) channelIndex,
                vibrationPattern.getValue(),
                (byte) (intensity & 0xFF),
                (byte) ((intensity >> 8) & 0xFF),
                (byte) 0x00,
                (byte) 0x00,
                orientationType.getValue(),
                (byte) (orientationValue & 0xFF),
                (byte) ((orientationValue >> 8) & 0xFF),
                (byte) 0x00,
                (byte) 0x00,
                (byte) iterations,
                (byte) (periodMs & 0xFF),
                (byte) ((periodMs >> 8) & 0xFF),
                (byte) (initialTimerValueMs & 0xFF),
                (byte) ((initialTimerValueMs >> 8) & 0xFF),
                (byte) ((exclusiveChannel)?(0x01):(0x00)),
                (byte) ((clearOtherChannels)?(0x01):(0x00))
        };
    }

}
