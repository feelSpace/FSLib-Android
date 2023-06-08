/*
 * Copyright (c) 2018-2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */
package de.feelspace.fslib;

/**
 * Enumeration of orientation types for vibration commands.
 */
public enum OrientationType {

    BINARY_MASK((byte)0),
    VIBROMOTOR_INDEX((byte)1),
    ANGLE((byte)2),
    BEARING((byte)3);

    // Value of the item
    private byte value;

    /**
     * Constructor of enum items.
     * @param v The value of the item.
     */
    OrientationType(byte v) {
        value = v;
    }

    /**
     * Returns the value of the item.
     *
     * @return The value of the item.
     */
    public byte getValue() {
        return value;
    }

    /**
     * Returns an item from its value.
     *
     * @param v The value of the item to retrieve.
     * @param defaultItem The default item when the value does not match any item.
     * @return The item corresponding to the value or the default item.
     */
    public static OrientationType fromValue(byte v, OrientationType defaultItem) {
        for (OrientationType item: OrientationType.values()) {
            if (item.value == v) {
                return item;
            }
        }
        return defaultItem;
    }


    @Override
    public String toString() {
        switch (this) {
            case BINARY_MASK:
                return "Binary mask";
            case VIBROMOTOR_INDEX:
                return "Vibromotor index";
            case ANGLE:
                return "Angle";
            case BEARING:
                return "Bearing";
            default:
                return "Unknown orientation type";
        }
    }

}
