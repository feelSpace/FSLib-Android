/*
 * Copyright (c) 2017-2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */

package de.feelspace.fslib;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Set of binary utility functions.
 */
class BinaryUtils {

    /**
     * Converts a byte arrays to an integer.
     * @param bytes The byte array.
     * @param bigEndian <code>true</code> for big-endian (ordered from most significant to least
     *                  significant), <code>false</code> for little endian.
     * @param signed <code>true</code> if the byte array uses two's complement representation.
     * @return the integer value.
     */
    protected static int byteToInt(@NonNull byte[] bytes, boolean bigEndian, boolean signed) {
        int value = 0;
        byte[] leByteArray = new byte[]{0, 0, 0, 0};
        if (bigEndian) {

        } else {
            for (int i=0; i<bytes.length; i++) {
                leByteArray[i] = bytes[i];
            }
        }
        return value;
    }

    /**
     * Converts a list of bit indexes to a binary mask.
     *
     * @param bitIndex The list of bit indexes to convert.
     * @return A binary mask.
     */
    static int toBinaryMask(@Nullable int[] bitIndex) {
        if (bitIndex == null || bitIndex.length == 0) {
            return 0;
        }
        int mask = 0;
        for (int bit: bitIndex) {
            mask |= (1 << bit);
        }
        return mask;
    }

}
