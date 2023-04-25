/*
 * Copyright (c) 2018-2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */

package de.feelspace.fslib;

/**
 * Advanced parameters of the belt.
 */
public enum BeltParameter {

    /**
     * The heading offset applied to determine the direction of magnetic North. The default value is
     * 45 degrees, and the parameter value must be an <code>Integer</code> in range [0-360].
     *
     * <b>Important:</b> This parameter is not meant to take into account the magnetic declination
     * nor to change how the belt is worn. It is only to compensate the fact that the waist of the
     * user is not round.
     */
    HEADING_OFFSET((byte)1),

    /**
     * Parameter that determines in which mode the compass accuracy signal is enabled. The default
     * value is 3, and the parameter value must be an <code>Integer</code> in range [0-3].
     * <ul>
     *     <li>0 is for no inaccurate compass signal,</li>
     *     <li>1 to signal inaccurate compass in compass mode only,</li>
     *     <li>2 to signal inaccurate compass in app mode only,</li>
     *     <li>3 to signal inaccurate compass in both compass and app modes.</li>
     * </ul>
     */
    ACCURACY_SIGNAL_STATE((byte)3);

    // Value of the item
    private byte value;

    /**
     * Constructor of enum items.
     * @param v The value of the item.
     */
    BeltParameter(byte v) {
        value = v;
    }

    /**
     * Returns the value of the item.
     *
     * @return The value of the item.
     */
    byte getValue() {
        return value;
    }
}
