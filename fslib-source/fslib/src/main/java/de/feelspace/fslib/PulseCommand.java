/*
 * Copyright (c) 2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */
package de.feelspace.fslib;

/**
 * Command to start a pulse on the belt.
 */
class PulseCommand {

    // Channel index
    private int channelIndex;

    // Type of orientation
    private OrientationType orientationType;

    // Orientation of the vibration
    private int orientationValue;

    // Intensity of the vibration
    private int intensity;

    // Duration of the vibration for one pulse
    private int onDurationMs;

    // Number of pulse iterations
    private int pulseIterations;

    // Number of pattern iterations
    private int patternIterations;

    // Period of pulses
    private int pulsePeriodMs;

    // Period of patterns
    private int patternPeriodMs;

    // Reset pulse timer option
    private ResetProgressOption resetProgressOption;

    // Combination of the vibration with other channels
    private boolean exclusiveChannel;

    // Clear other channels
    private boolean clearOtherChannels;

    /**
     * Constructor.
     *
     * @throws IllegalArgumentException If an argument has illegal value.
     */
    public PulseCommand(
            int channelIndex, OrientationType orientationType, int orientationValue,
            int  intensity, int onDurationMs, int pulseIterations, int patternIterations,
            int pulsePeriodMs, int patternPeriodMs, ResetProgressOption resetProgressOption,
            boolean exclusiveChannel, boolean clearOtherChannels) {
        if (channelIndex < 0 || channelIndex > 5) {
            throw new IllegalArgumentException("Channel index must be in range [0-5].");
        }
        if (orientationType == null) {
            throw new IllegalArgumentException("Illegal orientation type.");
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
        if (intensity < 0 || (intensity>100 && intensity!=0xAA)) {
            throw new IllegalArgumentException("Illegal intensity value.");
        }
        if (onDurationMs < 0 || onDurationMs > 0xFFFF) {
            throw new IllegalArgumentException("Illegal on-duration value.");
        }
        if (pulseIterations < 0 || pulseIterations > 9) {
            throw new IllegalArgumentException("Illegal pulse iterations value.");
        }
        if (patternIterations > 127 || patternIterations < -128) {
            throw new IllegalArgumentException("Illegal pattern iterations value.");
        }
        if (patternIterations < 0) {
            patternIterations = 0;
        }
        if (pulsePeriodMs < 0 || pulsePeriodMs > 0xFFFF) {
            throw new IllegalArgumentException("Illegal pulse period value.");
        }
        if (patternPeriodMs < 0 || patternPeriodMs > 0xFFFF) {
            throw new IllegalArgumentException("Illegal pattern period value.");
        }
        if (resetProgressOption == null) {
            throw new IllegalArgumentException("Illegal reset pattern option value.");
        }
        this.channelIndex = channelIndex;
        this.orientationType = orientationType;
        this.orientationValue = orientationValue;
        this.intensity = intensity;
        this.onDurationMs = onDurationMs;
        this.pulseIterations = pulseIterations;
        this.patternIterations = patternIterations;
        this.pulsePeriodMs = pulsePeriodMs;
        this.patternPeriodMs = patternPeriodMs;
        this.resetProgressOption = resetProgressOption;
        this.exclusiveChannel = exclusiveChannel;
        this.clearOtherChannels = clearOtherChannels;
    }

    /**
     * Returns the packet for this pulse command.
     *
     * @return the packet for this pulse command.
     */
    public byte[] getPacket() {
        return new byte[] {
                (byte) 0x40,
                (byte) channelIndex,
                orientationType.getValue(),
                (byte) (orientationValue & 0xFF),
                (byte) ((orientationValue >> 8) & 0xFF),
                (byte) intensity,
                (byte) (onDurationMs & 0xFF),
                (byte) ((onDurationMs >> 8) & 0xFF),
                (byte) pulseIterations,
                (byte) (patternIterations & 0xFF),
                (byte) (pulsePeriodMs & 0xFF),
                (byte) ((pulsePeriodMs >> 8) & 0xFF),
                (byte) (patternPeriodMs & 0xFF),
                (byte) ((patternPeriodMs >> 8) & 0xFF),
                resetProgressOption.getValue(),
                (byte) ((exclusiveChannel)?(0x01):(0x00)),
                (byte) ((clearOtherChannels)?(0x01):(0x00))
        };
    }

}
