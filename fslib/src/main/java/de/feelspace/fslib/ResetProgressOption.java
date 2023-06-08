/*
 * Copyright (c) 2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */
package de.feelspace.fslib;

/**
 * Enumeration of reset options for pulse command.
 */
public enum ResetProgressOption {

    RESET_PROGRESS((byte)0),
    RESET_PROGRESS_ON_DIFFERENT_PERIOD((byte)1),
    KEEP_PROGRESS((byte)2);

    // Value of the item
    private byte value;

    /**
     * Constructor of enum items.
     * @param v The value of the item.
     */
    ResetProgressOption(byte v) {
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
    public static ResetProgressOption fromValue(int v, ResetProgressOption defaultItem) {
        for (ResetProgressOption item: ResetProgressOption.values()) {
            if (item.value == v) {
                return item;
            }
        }
        return defaultItem;
    }


    @Override
    public String toString() {
        switch (this) {
            case RESET_PROGRESS:
                return "Reset pattern";
            case RESET_PROGRESS_ON_DIFFERENT_PERIOD:
                return "Reset on different period";
            case KEEP_PROGRESS:
                return "No reset";
            default:
                return "Unknown reset option";
        }
    }

}
