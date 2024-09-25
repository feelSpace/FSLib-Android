/*
 * Copyright (c) 2017-2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */
package de.feelspace.fslib;

/**
 * States of the GATT controller.
 */
public enum GattConnectionState {

    /**
     * No GATT connection.
     */
    GATT_DISCONNECTED(),

    /**
     * Connecting to GATT server.
     */
    GATT_CONNECTING(),

    /**
     * Discovering services.
     */
    GATT_DISCOVERING_SERVICES(),

    /**
     * Pairing.
     */
    GATT_PAIRING(),

    /**
     * Connected to GATT services.
     */
    GATT_CONNECTED(),

    /**
     * Reconnecting to GATT server.
     */
    GATT_RECONNECTING(),

    /**
     * Disconnecting.
     */
    GATT_DISCONNECTING(),

}
