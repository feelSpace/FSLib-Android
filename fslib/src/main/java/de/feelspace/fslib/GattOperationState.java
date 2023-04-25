/*
 * Copyright (c) 2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */
package de.feelspace.fslib;

/**
 * States of Gatt operations.
 */
enum GattOperationState {
    STATE_NOT_STARTED(),
    STATE_STARTED(),
    STATE_FAILED(),
    STATE_CANCELLED(),
    STATE_TIMED_OUT(),
    STATE_SUCCESS();
}
