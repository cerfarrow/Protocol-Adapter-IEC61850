/**
 * Copyright 2014-2016 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.alliander.osgp.adapter.protocol.iec61850.device.ssld.responses;

import com.alliander.osgp.adapter.protocol.iec61850.device.DeviceMessageStatus;
import com.alliander.osgp.adapter.protocol.iec61850.device.DeviceResponse;

public class EmptyDeviceResponse extends DeviceResponse {

    private DeviceMessageStatus status;

    public EmptyDeviceResponse(final String organisationIdentification, final String deviceIdentification,
            final String correlationUid, final DeviceMessageStatus status) {
        super(organisationIdentification, deviceIdentification, correlationUid);
        this.status = status;
    }

    public DeviceMessageStatus getStatus() {
        return this.status;
    }
}
