/**
 * Copyright 2016 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.alliander.osgp.adapter.protocol.iec61850.infra.networking.services;

import java.util.ArrayList;
import java.util.List;

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
import com.alliander.osgp.adapter.protocol.iec61850.device.rtu.RtuDeviceService;
import com.alliander.osgp.adapter.protocol.iec61850.device.rtu.requests.GetDataDeviceRequest;
import com.alliander.osgp.adapter.protocol.iec61850.device.rtu.requests.SetDataDeviceRequest;
import com.alliander.osgp.adapter.protocol.iec61850.device.ssld.responses.EmptyDeviceResponse;
import com.alliander.osgp.adapter.protocol.iec61850.device.ssld.responses.GetDataDeviceResponse;
import com.alliander.osgp.adapter.protocol.iec61850.exceptions.ConnectionFailureException;
import com.alliander.osgp.adapter.protocol.iec61850.exceptions.ProtocolAdapterException;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.Iec61850Client;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.Iec61850ClientAssociation;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.Iec61850Connection;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.SystemService;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.DeviceConnection;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.Function;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.IED;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.LogicalDevice;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.reporting.Iec61850RtuDeviceReportingService;
import com.alliander.osgp.dto.valueobjects.microgrids.GetDataRequestDto;
import com.alliander.osgp.dto.valueobjects.microgrids.GetDataResponseDto;
import com.alliander.osgp.dto.valueobjects.microgrids.GetDataSystemIdentifierDto;
import com.alliander.osgp.dto.valueobjects.microgrids.SetDataRequestDto;
import com.alliander.osgp.dto.valueobjects.microgrids.SetDataSystemIdentifierDto;
import com.alliander.osgp.dto.valueobjects.microgrids.SystemFilterDto;

@Component
public class Iec61850RtuDeviceService implements RtuDeviceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(Iec61850RtuDeviceService.class);

    @Autowired
    private Iec61850DeviceConnectionService iec61850DeviceConnectionService;

    @Autowired
    private Iec61850SystemServiceFactory systemServiceFactory;

    @Autowired
    private Iec61850Client iec61850Client;

    @Override
    public void getData(final GetDataDeviceRequest deviceRequest, final DeviceResponseHandler deviceResponseHandler)
            throws JMSException {
        try {
            final ServerModel serverModel = this.connectAndRetrieveServerModel(deviceRequest);

            final ClientAssociation clientAssociation = this.iec61850DeviceConnectionService
                    .getClientAssociation(deviceRequest.getDeviceIdentification());

            final GetDataResponseDto getDataResponse = this.handleGetData(new DeviceConnection(
                    new Iec61850Connection(new Iec61850ClientAssociation(clientAssociation, null), serverModel),
                    deviceRequest.getDeviceIdentification(), IED.ZOWN_RTU), deviceRequest);

            final GetDataDeviceResponse deviceResponse = new GetDataDeviceResponse(
                    deviceRequest.getOrganisationIdentification(), deviceRequest.getDeviceIdentification(),
                    deviceRequest.getCorrelationUid(), DeviceMessageStatus.OK, getDataResponse);

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

    @Override
    public void setData(final SetDataDeviceRequest deviceRequest, final DeviceResponseHandler deviceResponseHandler)
            throws JMSException {
        try {
            final ServerModel serverModel = this.connectAndRetrieveServerModel(deviceRequest);
            final ClientAssociation clientAssociation = this.iec61850DeviceConnectionService
                    .getClientAssociation(deviceRequest.getDeviceIdentification());

            this.handleSetData(new DeviceConnection(
                    new Iec61850Connection(new Iec61850ClientAssociation(clientAssociation, null), serverModel),
                    deviceRequest.getDeviceIdentification(), IED.ZOWN_RTU), deviceRequest);

            final EmptyDeviceResponse deviceResponse = new EmptyDeviceResponse(
                    deviceRequest.getOrganisationIdentification(), deviceRequest.getDeviceIdentification(),
                    deviceRequest.getCorrelationUid(), DeviceMessageStatus.OK);

            deviceResponseHandler.handleResponse(deviceResponse);
        } catch (final ConnectionFailureException se) {
            LOGGER.error("Could not connect to device after all retries", se);

            final EmptyDeviceResponse deviceResponse = new EmptyDeviceResponse(
                    deviceRequest.getOrganisationIdentification(), deviceRequest.getDeviceIdentification(),
                    deviceRequest.getCorrelationUid(), DeviceMessageStatus.FAILURE);

            deviceResponseHandler.handleConnectionFailure(se, deviceResponse);
        } catch (final Exception e) {
            LOGGER.error("Unexpected exception during Set Data", e);

            final EmptyDeviceResponse deviceResponse = new EmptyDeviceResponse(
                    deviceRequest.getOrganisationIdentification(), deviceRequest.getDeviceIdentification(),
                    deviceRequest.getCorrelationUid(), DeviceMessageStatus.FAILURE);

            deviceResponseHandler.handleException(e, deviceResponse);
        }
    }

    // ======================================
    // PRIVATE DEVICE COMMUNICATION METHODS =
    // ======================================

    private ServerModel connectAndRetrieveServerModel(final DeviceRequest deviceRequest)
            throws ProtocolAdapterException {

        this.iec61850DeviceConnectionService.connect(deviceRequest.getIpAddress(),
                deviceRequest.getDeviceIdentification(), IED.ZOWN_RTU, LogicalDevice.RTU_ONE);
        return this.iec61850DeviceConnectionService.getServerModel(deviceRequest.getDeviceIdentification());
    }

    // ========================
    // PRIVATE HELPER METHODS =
    // ========================

    private GetDataResponseDto handleGetData(final DeviceConnection connection,
            final GetDataDeviceRequest deviceRequest) throws ProtocolAdapterException {

        final GetDataRequestDto requestedData = deviceRequest.getDataRequest();

        final Function<GetDataResponseDto> function = new Function<GetDataResponseDto>() {

            @Override
            public GetDataResponseDto apply() throws Exception {
                final Iec61850RtuDeviceReportingService reportingService = new Iec61850RtuDeviceReportingService();
                reportingService.enableReportingOnDevice(connection, deviceRequest.getDeviceIdentification());

                final List<GetDataSystemIdentifierDto> identifiers = new ArrayList<>();
                for (final SystemFilterDto systemFilter : requestedData.getSystemFilters()) {
                    final SystemService systemService = Iec61850RtuDeviceService.this.systemServiceFactory
                            .getSystemService(systemFilter);
                    final GetDataSystemIdentifierDto getDataSystemIdentifier = systemService.getData(systemFilter,
                            Iec61850RtuDeviceService.this.iec61850Client, connection);
                    identifiers.add(getDataSystemIdentifier);
                }
                return new GetDataResponseDto(identifiers);
            }
        };

        return this.iec61850Client.sendCommandWithRetry(function, deviceRequest.getDeviceIdentification());
    }

    private void handleSetData(final DeviceConnection connection, final SetDataDeviceRequest deviceRequest)
            throws ProtocolAdapterException {

        final SetDataRequestDto setDataRequest = deviceRequest.getSetDataRequest();

        final Function<Void> function = new Function<Void>() {

            @Override
            public Void apply() throws Exception {

                final Iec61850RtuDeviceReportingService reportingService = new Iec61850RtuDeviceReportingService();
                reportingService.enableReportingOnDevice(connection, deviceRequest.getDeviceIdentification());

                for (final SetDataSystemIdentifierDto identifier : setDataRequest.getSetDataSystemIdentifiers()) {

                    final SystemService systemService = Iec61850RtuDeviceService.this.systemServiceFactory
                            .getSystemService(identifier.getId(), identifier.getSystemType());

                    systemService.setData(identifier, Iec61850RtuDeviceService.this.iec61850Client, connection);
                }

                return null;
            }
        };

        this.iec61850Client.sendCommandWithRetry(function, deviceRequest.getDeviceIdentification());
    }

}
