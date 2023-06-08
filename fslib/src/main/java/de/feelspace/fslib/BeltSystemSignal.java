/*
 * Copyright (c) 2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */
package de.feelspace.fslib;

/**
 * Enumeration of belt system signals.
 */
public enum BeltSystemSignal {

    /**
     * A light warning signal to use when an operation is not permitted or cannot be performed.
     * The default intensity for this signal is lower than the default intensity for navigation.
     */
    WARNING((byte)0),

    /**
     * Single iteration of the destination reached signal.
     */
    DESTINATION_REACHED((byte)1),

    /**
     * A system signal that indicates with a vibration the level of the belt's battery.
     */
    BATTERY_LEVEL((byte)2);

    /**
     * Value of the item
     */
    private byte value;

    /**
     * Constructor of enum items.
     * @param v The value of the item.
     */
    BeltSystemSignal(byte v) {
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
     * @return The item corresponding to the value or <code>null</code> if the value does not
     * correspond to any item.
     */
    static BeltSystemSignal fromValue(byte v) {
        for (BeltSystemSignal item: BeltSystemSignal.values()) {
            if (item.value == v) {
                return item;
            }
        }
        return null;
    }


    @Override
    public String toString() {
        switch (this) {
            case WARNING:
                return "Warning signal";
            case DESTINATION_REACHED:
                return "Destination reached signal";
            case BATTERY_LEVEL:
                return "Battery level signal";
        }
        return "Unknown signal";
    }

}
