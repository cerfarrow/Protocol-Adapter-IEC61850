/**
 * Copyright 2017 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.alliander.osgp.adapter.protocol.iec61850.infra.networking.services;

import java.io.Serializable;

import javax.jms.JMSException;

import org.openmuc.openiec61850.ClientAssociation;
import org.openmuc.openiec61850.ServerModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alliander.osgp.adapter.protocol.iec61850.device.DeviceMessageStatus;
import com.alliander.osgp.adapter.protocol.iec61850.device.DeviceRequest;
import com.alliander.osgp.adapter.protocol.iec61850.device.DeviceResponseHandler;
import com.alliander.osgp.adapter.protocol.iec61850.device.da.rtu.DaDeviceRequest;
import com.alliander.osgp.adapter.protocol.iec61850.device.da.rtu.DaDeviceResponse;
import com.alliander.osgp.adapter.protocol.iec61850.device.da.rtu.DaRtuDeviceService;
import com.alliander.osgp.adapter.protocol.iec61850.device.ssld.responses.EmptyDeviceResponse;
import com.alliander.osgp.adapter.protocol.iec61850.domain.entities.Iec61850Device;
import com.alliander.osgp.adapter.protocol.iec61850.domain.repositories.Iec61850DeviceRepository;
import com.alliander.osgp.adapter.protocol.iec61850.exceptions.ConnectionFailureException;
import com.alliander.osgp.adapter.protocol.iec61850.exceptions.ProtocolAdapterException;
import com.alliander.osgp.adapter.protocol.iec61850.infra.messaging.DaRtuDeviceRequestMessageProcessor;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.Iec61850Client;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.Iec61850ClientAssociation;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.Iec61850Connection;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.DeviceConnection;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.Function;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.IED;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.LogicalDevice;

@Component
public class Iec61850DaRtuDeviceService implements DaRtuDeviceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(Iec61850DaRtuDeviceService.class);

    @Autowired
    private Iec61850DeviceConnectionService iec61850DeviceConnectionService;

    @Autowired
    private Iec61850Client iec61850Client;

    @Autowired
    private Iec61850DeviceRepository iec61850DeviceRepository;

    @Override
    public void getData(final DaDeviceRequest deviceRequest, final DeviceResponseHandler deviceResponseHandler,
            final DaRtuDeviceRequestMessageProcessor messageProcessor) throws JMSException {
        try {
            final String serverName = this.getServerName(deviceRequest);
            final ServerModel serverModel = this.connectAndRetrieveServerModel(deviceRequest, serverName);

            final ClientAssociation clientAssociation = this.iec61850DeviceConnectionService
                    .getClientAssociation(deviceRequest.getDeviceIdentification());

            final Serializable dataResponse = this
                    .handleGetData(
                            new DeviceConnection(
                                    new Iec61850Connection(new Iec61850ClientAssociation(clientAssociation, null),
                                            serverModel),
                                    deviceRequest.getDeviceIdentification(),
                                    deviceRequest.getOrganisationIdentification(), serverName),
                            deviceRequest, messageProcessor);

            final DaDeviceResponse deviceResponse = new DaDeviceResponse(deviceRequest.getOrganisationIdentification(),
                    deviceRequest.getDeviceIdentification(), deviceRequest.getCorrelationUid(), DeviceMessageStatus.OK,
                    dataResponse);

            deviceResponseHandler.handleResponse(deviceResponse);
        } catch (final ConnectionFailureException se) {
            LOGGER.error("Could not connect to device after all retries", se);

            final EmptyDeviceResponse deviceResponse = new EmptyDeviceResponse(
                    deviceRequest.getOrganisationIdentification(), deviceRequest.getDeviceIdentification(),
                    deviceRequest.getCorrelationUid(), DeviceMessageStatus.FAILURE);

            deviceResponseHandler.handleConnectionFailure(se, deviceResponse);
        } catch (final Exception e) {
            LOGGER.error("Unexpected exception during Get Data", e);

            final EmptyDeviceResponse deviceResponse = new EmptyDeviceResponse(
                    deviceRequest.getOrganisationIdentification(), deviceRequest.getDeviceIdentification(),
                    deviceRequest.getCorrelationUid(), DeviceMessageStatus.FAILURE);

            deviceResponseHandler.handleException(e, deviceResponse);
        }
    }

    // ======================================
    // PRIVATE DEVICE COMMUNICATION METHODS =
    // ======================================

    private ServerModel connectAndRetrieveServerModel(final DeviceRequest deviceRequest, final String serverName)
            throws ProtocolAdapterException {

        this.iec61850DeviceConnectionService.connect(deviceRequest.getIpAddress(),
                deviceRequest.getDeviceIdentification(), deviceRequest.getOrganisationIdentification(), IED.DA_RTU,
                serverName, LogicalDevice.RTU.getDescription() + 1);
        return this.iec61850DeviceConnectionService.getServerModel(deviceRequest.getDeviceIdentification());
    }

    // ========================
    // PRIVATE HELPER METHODS =
    // ========================

    private <T> T handleGetData(final DeviceConnection connection, final DaDeviceRequest deviceRequest,
            final DaRtuDeviceRequestMessageProcessor messageProcessor) throws ProtocolAdapterException {
        final Function<T> function = messageProcessor.getDataFunction(this.iec61850Client, connection, deviceRequest);
        return this.iec61850Client.sendCommandWithRetry(function, deviceRequest.getDeviceIdentification());
    }

    private String getServerName(final DeviceRequest deviceRequest) {
        final Iec61850Device iec61850Device = this.iec61850DeviceRepository
                .findByDeviceIdentification(deviceRequest.getDeviceIdentification());
        if (iec61850Device != null && iec61850Device.getServerName() != null) {
            return iec61850Device.getServerName();
        } else {
            return IED.DA_RTU.getDescription();
        }
    }
}
