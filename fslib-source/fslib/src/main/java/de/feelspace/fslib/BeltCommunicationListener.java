/*
 * Copyright (c) 2017-2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */
package de.feelspace.fslib;

import android.support.annotation.NonNull;

/**
 * Interface to listen to belt communication events.
 */
public interface BeltCommunicationListener {

    /**
     * Informs that a belt parameter value was changed or retrieved.
     *
     * @param beltParameter The parameter.
     * @param parameterValue The value of the parameter.
     */
    void onBeltParameterValueNotified(BeltParameter beltParameter, Object parameterValue);

}
