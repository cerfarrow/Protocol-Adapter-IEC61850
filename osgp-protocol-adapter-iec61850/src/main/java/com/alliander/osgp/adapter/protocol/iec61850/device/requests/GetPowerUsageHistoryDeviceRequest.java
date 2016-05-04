/**
 * Copyright 2014-2016 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.alliander.osgp.adapter.protocol.iec61850.device.requests;

import com.alliander.osgp.adapter.protocol.iec61850.device.DeviceRequest;
import com.alliander.osgp.dto.valueobjects.PowerUsageHistoryMessageDataContainerDto;

public class GetPowerUsageHistoryDeviceRequest extends DeviceRequest {

    private PowerUsageHistoryMessageDataContainerDto powerUsageHistoryContainer;

    public GetPowerUsageHistoryDeviceRequest(final String organisationIdentification,
            final String deviceIdentification, final String correlationUid,
            final PowerUsageHistoryMessageDataContainerDto powerUsageHistoryContainer) {
        super(organisationIdentification, deviceIdentification, correlationUid);
        this.powerUsageHistoryContainer = powerUsageHistoryContainer;
    }

    public GetPowerUsageHistoryDeviceRequest(final String organisationIdentification,
            final String deviceIdentification, final String correlationUid,
            final PowerUsageHistoryMessageDataContainerDto powerUsageHistoryContainer, final String domain,
            final String domainVersion, final String messageType, final String ipAddress, final int retryCount,
            final boolean isScheduled) {
        super(organisationIdentification, deviceIdentification, correlationUid, domain, domainVersion, messageType,
                ipAddress, retryCount, isScheduled);
        this.powerUsageHistoryContainer = powerUsageHistoryContainer;
    }

    public PowerUsageHistoryMessageDataContainerDto getPowerUsageHistoryContainer() {
        return this.powerUsageHistoryContainer;
    }
}
