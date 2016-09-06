/**
 * Copyright 2014-2016 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.alliander.osgp.adapter.protocol.iec61850.application.services;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alliander.osgp.adapter.protocol.iec61850.exceptions.ProtocolAdapterException;
import com.alliander.osgp.adapter.protocol.iec61850.infra.messaging.DeviceResponseMessageSender;
import com.alliander.osgp.adapter.protocol.iec61850.infra.messaging.OsgpRequestMessageSender;
import com.alliander.osgp.core.db.api.iec61850.entities.DeviceOutputSetting;
import com.alliander.osgp.core.db.api.iec61850.entities.Ssld;
import com.alliander.osgp.core.db.api.iec61850.repositories.SsldDataRepository;
import com.alliander.osgp.dto.valueobjects.DeviceFunctionDto;
import com.alliander.osgp.dto.valueobjects.EventNotificationDto;
import com.alliander.osgp.dto.valueobjects.microgrids.DataResponseDto;
import com.alliander.osgp.shared.infra.jms.DeviceMessageMetadata;
import com.alliander.osgp.shared.infra.jms.ProtocolResponseMessage;
import com.alliander.osgp.shared.infra.jms.RequestMessage;
import com.alliander.osgp.shared.infra.jms.ResponseMessageResultType;

@Service(value = "iec61850DeviceManagementService")
@Transactional(value = "iec61850OsgpCoreDbApiTransactionManager")
public class DeviceManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceManagementService.class);

    @Autowired
    private SsldDataRepository ssldDataRepository;

    @Autowired
    private OsgpRequestMessageSender osgpRequestMessageSender;

    @Autowired
    private DeviceResponseMessageSender responseSender;

    public DeviceManagementService() {
        // Parameterless constructor required for transactions...
    }

    /**
     * Send an event notification to OSGP Core.
     *
     * @param deviceIdentification
     *            The identification of the device.
     * @param eventNotifications
     *            The event notifications.
     *
     * @throws ProtocolAdapterException
     *             In case the device can not be found in the database.
     */
    public void addEventNotifications(final String deviceIdentification,
            final List<EventNotificationDto> eventNotifications) throws ProtocolAdapterException {

        final Ssld ssldDevice = this.ssldDataRepository.findByDeviceIdentification(deviceIdentification);
        if (ssldDevice == null) {
            throw new ProtocolAdapterException(
                    "Unable to find device using deviceIdentification: " + deviceIdentification);
        }

        LOGGER.info("addEventNotifications called for device {}: {}", deviceIdentification, eventNotifications);

        final RequestMessage requestMessage = new RequestMessage("no-correlationUid", "no-organisation",
                deviceIdentification, new ArrayList<>(eventNotifications));

        this.osgpRequestMessageSender.send(requestMessage, DeviceFunctionDto.ADD_EVENT_NOTIFICATION.name());
    }

    /**
     * Get the device output setting (relay configuration) for a given device.
     *
     * @param deviceIdentification
     *            The device identification.
     *
     * @return The {@link DeviceOutputSettings} for the device.
     *
     * @throws ProtocolAdapterException
     *             In case the device can not be found in the database.
     */
    public List<DeviceOutputSetting> getDeviceOutputSettings(final String deviceIdentification)
            throws ProtocolAdapterException {

        final Ssld ssldDevice = this.ssldDataRepository.findByDeviceIdentification(deviceIdentification);
        if (ssldDevice == null) {
            throw new ProtocolAdapterException(
                    "Unable to find device using deviceIdentification: " + deviceIdentification);
        }

        return ssldDevice.getOutputSettings();
    }

    public void sendMeasurements(final String deviceIdentification, final DataResponseDto response)
            throws ProtocolAdapterException {
        // Correlation ID is generated @ WS adapter, domain+version is hardcoded
        // for now
        final ProtocolResponseMessage responseMessage = new ProtocolResponseMessage.Builder().dataObject(response)
                .deviceMessageMetadata(new DeviceMessageMetadata(deviceIdentification, "no-organisation",
                        "no-correlationUid", DeviceFunctionDto.GET_DATA.name(), 0))
                .result(ResponseMessageResultType.OK).domain("MICROGRIDS").domainVersion("1.0").build();
        this.responseSender.send(responseMessage);
    }
}
