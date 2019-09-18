/*
 * Copyright (c) 2017-2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */
package de.feelspace.fslib;

/**
 * Enumeration of belt buttons.
 */
public enum BeltButton {

    /**
     * Power button.
     */
    POWER((byte)1),

    /**
     * Compass button.
     */
    COMPASS((byte)2),

    /**
     * Home button.
     */
    HOME((byte)3),

    /**
     * Pause button.
     */
    PAUSE((byte)4);

    /**
     * Value of the item
     */
    private byte value;

    /**
     * Constructor of enum items.
     * @param v The value of the item.
     */
    BeltButton(byte v) {
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
    static BeltButton fromValue(byte v) {
        for (BeltButton item: BeltButton.values()) {
            if (item.value == v) {
                return item;
            }
        }
        return null;
    }


    @Override
    public String toString() {
        switch (this) {
            case POWER:
                return "Power button";
            case COMPASS:
                return "Compass button";
            case HOME:
                return "Home button";
            case PAUSE:
                return "Pause button";
            default:
                return "Unknown button";
        }
    }

}
