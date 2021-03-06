/**
 * Copyright 2014-2016 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.alliander.osgp.adapter.protocol.iec61850.exceptions;

public class DeviceMessageFailedException extends Exception {

    /**
     * Serial Version UID.
     */
    private static final long serialVersionUID = 5129388216260738123L;

    private static final String MESSAGE = "Device Message Failed";

    public DeviceMessageFailedException() {
        super(MESSAGE);
    }
}
