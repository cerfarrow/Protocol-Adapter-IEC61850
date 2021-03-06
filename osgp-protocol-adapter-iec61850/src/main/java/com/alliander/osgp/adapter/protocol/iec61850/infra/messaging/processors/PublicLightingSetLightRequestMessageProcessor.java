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

import com.alliander.osgp.adapter.protocol.iec61850.device.ssld.requests.SetLightDeviceRequest;
import com.alliander.osgp.adapter.protocol.iec61850.infra.messaging.DeviceRequestMessageType;
import com.alliander.osgp.adapter.protocol.iec61850.infra.messaging.SsldDeviceRequestMessageProcessor;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.RequestMessageData;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.services.Iec61850DeviceResponseHandler;
import com.alliander.osgp.dto.valueobjects.LightValueMessageDataContainerDto;
import com.alliander.osgp.shared.infra.jms.Constants;

/**
 * Class for processing public lighting set light request messages
 */
@Component("iec61850PublicLightingSetLightRequestMessageProcessor")
public class PublicLightingSetLightRequestMessageProcessor extends SsldDeviceRequestMessageProcessor {
    /**
     * Logger for this class
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PublicLightingSetLightRequestMessageProcessor.class);

    public PublicLightingSetLightRequestMessageProcessor() {
        super(DeviceRequestMessageType.SET_LIGHT);
    }

    @Override
    public void processMessage(final ObjectMessage message) throws JMSException {
        LOGGER.debug("Processing public lighting set light request message");

        String correlationUid = null;
        String domain = null;
        String domainVersion = null;
        String messageType = null;
        String organisationIdentification = null;
        String deviceIdentification = null;
        String ipAddress = null;
        int retryCount = 0;
        boolean isScheduled = false;
        LightValueMessageDataContainerDto lightValueMessageDataContainer = null;

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

            lightValueMessageDataContainer = (LightValueMessageDataContainerDto) message.getObject();

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

        final RequestMessageData requestMessageData = new RequestMessageData(lightValueMessageDataContainer, domain,
                domainVersion, messageType, retryCount, isScheduled, correlationUid, organisationIdentification,
                deviceIdentification);

        this.printDomainInfo(messageType, domain, domainVersion);

        final Iec61850DeviceResponseHandler iec61850DeviceResponseHandler = this.createIec61850DeviceResponseHandler(
                requestMessageData, message);

        final SetLightDeviceRequest deviceRequest = new SetLightDeviceRequest(organisationIdentification,
                deviceIdentification, correlationUid, lightValueMessageDataContainer, domain, domainVersion,
                messageType, ipAddress, retryCount, isScheduled);

        this.deviceService.setLight(deviceRequest, iec61850DeviceResponseHandler);
    }
}
