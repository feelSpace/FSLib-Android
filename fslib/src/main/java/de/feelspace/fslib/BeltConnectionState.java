/*
 * Copyright (c) 2017-2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */
package de.feelspace.fslib;

/**
 * Enumeration of connection states.
 */
public enum BeltConnectionState {

    /**
     * Disconnected from belt.
     */
    STATE_DISCONNECTED(),

    /**
     * Scanning for a belt.
     */
    STATE_SCANNING(),

    /**
     * Pairing to the belt.
     */
    STATE_PAIRING(),

    /**
     * Connecting to a belt.
     */
    STATE_CONNECTING(),

    /**
     * Reconnecting to a belt after an unexpected disconnection.
     */
    STATE_RECONNECTING(),

    /**
     * Discovering services.
     */
    STATE_DISCOVERING_SERVICES(),

    /**
     * Handshake with the belt ongoing.
     */
    STATE_HANDSHAKE(),

    /**
     * Connected to a belt.
     */
    STATE_CONNECTED();

    @Override
    public String toString() {
        switch (this) {
            case STATE_DISCONNECTED:
                return "Disconnected";
            case STATE_SCANNING:
                return "Scanning";
            case STATE_PAIRING:
                return "Pairing";
            case STATE_CONNECTING:
                return "Connecting";
            case STATE_RECONNECTING:
                return "Reconnecting";
            case STATE_DISCOVERING_SERVICES:
                return "Discovering services";
            case STATE_HANDSHAKE:
                return "Handshake";
            case STATE_CONNECTED:
                return "Connected";
            default:
                return "Unknown";
        }
    }

}
