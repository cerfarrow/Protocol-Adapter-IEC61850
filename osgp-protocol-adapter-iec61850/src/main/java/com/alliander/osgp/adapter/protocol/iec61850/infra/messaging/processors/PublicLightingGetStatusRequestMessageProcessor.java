/**
 * Copyright 2014-2016 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.alliander.osgp.adapter.protocol.iec61850.infra.messaging.processors;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alliander.osgp.adapter.protocol.iec61850.device.DeviceRequest;
import com.alliander.osgp.adapter.protocol.iec61850.device.DeviceResponse;
import com.alliander.osgp.adapter.protocol.iec61850.device.DeviceResponseHandler;
import com.alliander.osgp.adapter.protocol.iec61850.infra.messaging.SsldDeviceRequestMessageProcessor;
import com.alliander.osgp.adapter.protocol.iec61850.infra.messaging.DeviceRequestMessageType;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.RequestMessageData;
import com.alliander.osgp.shared.exceptionhandling.ComponentType;
import com.alliander.osgp.shared.exceptionhandling.ConnectionFailureException;
import com.alliander.osgp.shared.infra.jms.Constants;

/**
 * Class for processing public lighting get status request messages
 */
@Component("iec61850PublicLightingGetStatusRequestMessageProcessor")
public class PublicLightingGetStatusRequestMessageProcessor extends SsldDeviceRequestMessageProcessor {
    /**
     * Logger for this class
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PublicLightingGetStatusRequestMessageProcessor.class);

    public PublicLightingGetStatusRequestMessageProcessor() {
        super(DeviceRequestMessageType.GET_LIGHT_STATUS);
    }

    @Override
    public void processMessage(final ObjectMessage message) {
        LOGGER.info("Processing public lighting get status request message");

        String correlationUid = null;
        String domain = null;
        String domainVersion = null;
        String messageType = null;
        String organisationIdentification = null;
        String deviceIdentification = null;
        String ipAddress = null;
        int retryCount = 0;
        boolean isScheduled = false;

        try {
            correlationUid = message.getJMSCorrelationID();
            domain = message.getStringProperty(Constants.DOMAIN);
            domainVersion = message.getStringProperty(Constants.DOMAIN_VERSION);
            messageType = message.getJMSType();
            organisationIdentification = message.getStringProperty(Constants.ORGANISATION_IDENTIFICATION);
            deviceIdentification = message.getStringProperty(Constants.DEVICE_IDENTIFICATION);
            ipAddress = message.getStringProperty(Constants.IP_ADDRESS);
            retryCount = message.getIntProperty(Constants.RETRY_COUNT);
            isScheduled = message.propertyExists(Constants.IS_SCHEDULED) ? message
                    .getBooleanProperty(Constants.IS_SCHEDULED) : false;
        } catch (final JMSException e) {
            LOGGER.error("UNRECOVERABLE ERROR, unable to read ObjectMessage instance, giving up.", e);
            LOGGER.debug("correlationUid: {}", correlationUid);
            LOGGER.debug("domain: {}", domain);
            LOGGER.debug("domainVersion: {}", domainVersion);
            LOGGER.debug("messageType: {}", messageType);
            LOGGER.debug("organisationIdentification: {}", organisationIdentification);
            LOGGER.debug("deviceIdentification: {}", deviceIdentification);
            LOGGER.debug("ipAddress: {}", ipAddress);
            return;
        }

        final RequestMessageData requestMessageData = new RequestMessageData(null, domain, domainVersion, messageType,
                retryCount, isScheduled, correlationUid, organisationIdentification, deviceIdentification);

        LOGGER.info("Calling DeviceService function: {} for domain: {} {}", messageType, domain, domainVersion);

        final DeviceResponseHandler deviceResponseHandler = new DeviceResponseHandler() {

            @Override
            public void handleResponse(final DeviceResponse deviceResponse) {
                PublicLightingGetStatusRequestMessageProcessor.this.handleGetStatusDeviceResponse(deviceResponse,
                        PublicLightingGetStatusRequestMessageProcessor.this.responseMessageSender,
                        requestMessageData.getDomain(), requestMessageData.getDomainVersion(),
                        requestMessageData.getMessageType(), requestMessageData.getRetryCount());
            }

            @Override
            public void handleException(final Throwable t, final DeviceResponse deviceResponse, final boolean expected) {
                if (expected) {
                    PublicLightingGetStatusRequestMessageProcessor.this.handleExpectedError(
                            new ConnectionFailureException(ComponentType.PROTOCOL_IEC61850, t.getMessage()),
                            requestMessageData.getCorrelationUid(), requestMessageData.getOrganisationIdentification(),
                            requestMessageData.getDeviceIdentification(), requestMessageData.getDomain(),
                            requestMessageData.getDomainVersion(), requestMessageData.getMessageType());
                } else {
                    PublicLightingGetStatusRequestMessageProcessor.this.handleUnExpectedError(deviceResponse, t,
                            requestMessageData.getMessageData(), requestMessageData.getDomain(),
                            requestMessageData.getDomainVersion(), requestMessageData.getMessageType(),
                            requestMessageData.isScheduled(), requestMessageData.getRetryCount());
                }
            }

        };

        final DeviceRequest deviceRequest = new DeviceRequest(organisationIdentification, deviceIdentification,
                correlationUid, domain, domainVersion, messageType, ipAddress, retryCount, isScheduled);

        this.deviceService.getStatus(deviceRequest, deviceResponseHandler);
    }

}
