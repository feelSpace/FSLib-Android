/*
 * Copyright (c) 2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */
package de.feelspace.fslib;

/**
 * Enumeration of predefined buzzer sound.
 */
public enum BeltSound {

    /**
     * Sound to indicate the start of a procedure.
     */
    START(new BuzzerTonePattern(
            new float[]{0.05f, 0.10f, 0.05f},
            new float[]{800.f, 0.f, 1000.f}
    )),

    /**
     * Sound to indicate the end of a procedure.
     */
    STOP(new BuzzerTonePattern(
            new float[]{0.05f, 0.10f, 0.05f},
            new float[]{1000.f, 0.f, 800.f}
    )),

    /**
     * Sound to indicate the success of a procedure.
     */
    SUCCESS(new BuzzerTonePattern(
            new float[]{0.05f, 0.10f, 0.05f, 0.10f, 0.05f},
            new float[]{1000.f, 0.f, 1500.f, 0.f, 2000.f}
    )),

    /**
     * Sound to indicate an error.
     */
    ERROR(new BuzzerTonePattern(
            new float[]{0.25f, 0.25f, 0.25f, 0.25f, 0.25f},
            new float[]{1000.f, 0.f, 1000.f, 0.f, 1000.f}
    )),

    /**
     * A single short beep.
     */
    SINGLE_SHORT_BEEP(new BuzzerTonePattern(
            new float[]{0.05f},
            new float[]{1500.f}
    )),

    /**
     * Two short beeps.
     */
    DOUBLE_SHORT_BEEP(new BuzzerTonePattern(
            new float[]{0.05f, 0.10f, 0.05f},
            new float[]{1500.f, 0.f, 1500.f}
    )),

    /**
     * Three short beeps.
     */
    TRIPLE_SHORT_BEEP(new BuzzerTonePattern(
            new float[]{0.05f, 0.10f, 0.05f, 0.10f, 0.05f},
            new float[]{1500.f, 0.f, 1500.f, 0.f, 1500.f}
    )),

    /**
     * A single long beep.
     */
    SINGLE_LONG_BEEP(new BuzzerTonePattern(
            new float[]{0.25f},
            new float[]{1500.f}
    )),

    /**
     * Two long beeps.
     */
    DOUBLE_LONG_BEEP(new BuzzerTonePattern(
            new float[]{0.25f, 0.25f, 0.25f},
            new float[]{1500.f, 0.f, 1500.f}
    )),

    /**
     * Three long beeps.
     */
    TRIPLE_LONG_BEEP(new BuzzerTonePattern(
            new float[]{0.25f, 0.25f, 0.25f, 0.25f, 0.25f},
            new float[]{1500.f, 0.f, 1500.f, 0.f, 1500.f}
    ));

    /**
     * The pattern of the sound.
     */
    private BuzzerTonePattern tone;


    /**
     * Constructor.
     * @param tone The tone pattern.
     */
    BeltSound(BuzzerTonePattern tone) {
        this.tone = tone;
    }

    /**
     * Returns the tone pattern to play the sound.
     * @return the tone pattern of the sound.
     */
    protected BuzzerTonePattern getBuzzerTonePattern() {
        return tone;
    }

    /**
     * Returns the command packet for this sound.
     * @return the command packet.
     */
    protected byte[] getPacket() {
        return tone.getPacket();
    }
}
