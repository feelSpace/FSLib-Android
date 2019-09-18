/*
 * Copyright (c) 2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */
package de.feelspace.fslib;

import android.support.annotation.NonNull;

/**
 * Describes a buzzer tone pattern to be played by the belt.
 */
public class BuzzerTonePattern {

    // Number of steps in the pattern
    int steps;

    // Frequencies in kHz with a Q-point at 5 bits.
    int[] frequenciesKHzQ5;

    // Durations of steps in centiseconds (10^-2 seconds)
    int[] durationsCs;

    /**
     * Constructor.
     *
     * @param stepDurationsSec The list of step durations in seconds.
     * @param frequenciesHz  The list of frequencies in Hz.
     * @throws IllegalArgumentException If an argument has an illegal value.
     */
    public BuzzerTonePattern(@NonNull float[] stepDurationsSec, @NonNull float[] frequenciesHz)
            throws IllegalArgumentException {
        if (stepDurationsSec.length != frequenciesHz.length) {
            throw new IllegalArgumentException("Different number of steps.");
        }
        if (stepDurationsSec.length > 9) {
            throw new IllegalArgumentException("Number of steps out of range.");
        }
        this.steps = stepDurationsSec.length;
        frequenciesKHzQ5 = new int[steps];
        durationsCs = new int[steps];
        for (int i=0; i<steps; i++) {
            if (frequenciesHz[i] <= 0.f) {
                frequenciesHz[i] = 0;
            } else {
                frequenciesKHzQ5[i] = (int)((frequenciesHz[i]/1000.f)*32.f);
                if (frequenciesKHzQ5[i] > 255) {
                    throw new IllegalArgumentException("Frequency out of range.");
                }
            }
            if (stepDurationsSec[i] < 0 || stepDurationsSec[i] > 2.55f) {
                throw new IllegalArgumentException("Step duration out of range.");
            }
            durationsCs[i] = (int)(stepDurationsSec[i]*100.f);
        }
    }


    /**
     * Returns the command packet for this tone pattern.
     *
     * @return the command packet for this tone pattern.
     */
    public byte[] getPacket() {
        byte[] packet = new byte[2+2*steps];
        packet[0] = 0x01;
        packet[1] = (byte) steps;
        for (int i=0; i<steps; i++) {
            packet[i+2] = (byte) frequenciesKHzQ5[i];
        }
        for (int i=0; i<steps; i++) {
            packet[i+2+steps] = (byte) durationsCs[i];
        }
        return packet;
    }
}
