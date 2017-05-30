/**
 * Copyright 2014-2016 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.alliander.osgp.adapter.protocol.iec61850.infra.networking.services.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.openmuc.openiec61850.BdaBoolean;
import org.openmuc.openiec61850.Fc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alliander.osgp.adapter.protocol.iec61850.domain.valueobjects.DeviceMessageLog;
import com.alliander.osgp.adapter.protocol.iec61850.domain.valueobjects.EventType;
import com.alliander.osgp.adapter.protocol.iec61850.exceptions.ProtocolAdapterException;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.Iec61850Client;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.DataAttribute;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.DeviceConnection;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.Function;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.LogicalDevice;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.LogicalNode;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.NodeContainer;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.SubDataAttribute;
import com.alliander.osgp.adapter.protocol.iec61850.services.DeviceMessageLoggingService;
import com.alliander.osgp.core.db.api.iec61850.entities.DeviceOutputSetting;
import com.alliander.osgp.core.db.api.iec61850.entities.Ssld;
import com.alliander.osgp.dto.valueobjects.DeviceStatusDto;
import com.alliander.osgp.dto.valueobjects.EventNotificationTypeDto;
import com.alliander.osgp.dto.valueobjects.LightTypeDto;
import com.alliander.osgp.dto.valueobjects.LightValueDto;
import com.alliander.osgp.dto.valueobjects.LinkTypeDto;

public class Iec61850GetStatusCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(Iec61850GetStatusCommand.class);

    public DeviceStatusDto getStatusFromDevice(final Iec61850Client iec61850Client,
            final DeviceConnection deviceConnection, final Ssld ssld) throws ProtocolAdapterException {
        final Function<DeviceStatusDto> function = new Function<DeviceStatusDto>() {

            @Override
            public DeviceStatusDto apply(final DeviceMessageLog deviceMessageLog) throws Exception {
                // getting the light relay values
                final List<LightValueDto> lightValues = new ArrayList<>();

                for (final DeviceOutputSetting deviceOutputSetting : ssld.getOutputSettings()) {
                    final LogicalNode logicalNode = LogicalNode.getSwitchComponentByIndex(deviceOutputSetting
                            .getInternalId());
                    final NodeContainer position = deviceConnection.getFcModelNode(LogicalDevice.LIGHTING, logicalNode,
                            DataAttribute.POSITION, Fc.ST);
                    iec61850Client.readNodeDataValues(deviceConnection.getConnection().getClientAssociation(),
                            position.getFcmodelNode());
                    final BdaBoolean state = position.getBoolean(SubDataAttribute.STATE);
                    final boolean on = state.getValue();
                    lightValues.add(new LightValueDto(deviceOutputSetting.getExternalId(), on, null));

                    LOGGER.info(String.format("Got status of relay %d => %s", deviceOutputSetting.getInternalId(),
                            on ? "on" : "off"));

                    deviceMessageLog.addVariable(logicalNode, DataAttribute.POSITION, Fc.ST, on + "");
                }

                final NodeContainer eventBuffer = deviceConnection.getFcModelNode(LogicalDevice.LIGHTING,
                        LogicalNode.STREET_LIGHT_CONFIGURATION, DataAttribute.EVENT_BUFFER, Fc.CF);
                iec61850Client.readNodeDataValues(deviceConnection.getConnection().getClientAssociation(),
                        eventBuffer.getFcmodelNode());
                final String filter = eventBuffer.getString(SubDataAttribute.EVENT_BUFFER_FILTER);
                LOGGER.info("Got EvnBuf.enbEvnType filter {}", filter);

                deviceMessageLog.addVariable(LogicalNode.STREET_LIGHT_CONFIGURATION, DataAttribute.EVENT_BUFFER, Fc.CF,
                        filter);

                final Set<EventNotificationTypeDto> notificationTypes = EventType.getNotificationTypesForFilter(filter);
                int eventNotificationsMask = 0;
                for (final EventNotificationTypeDto notificationType : notificationTypes) {
                    eventNotificationsMask |= notificationType.getValue();
                }

                final NodeContainer softwareConfiguration = deviceConnection.getFcModelNode(LogicalDevice.LIGHTING,
                        LogicalNode.STREET_LIGHT_CONFIGURATION, DataAttribute.SOFTWARE_CONFIGURATION, Fc.CF);
                iec61850Client.readNodeDataValues(deviceConnection.getConnection().getClientAssociation(),
                        softwareConfiguration.getFcmodelNode());
                String lightTypeValue = softwareConfiguration.getString(SubDataAttribute.LIGHT_TYPE);
                // Fix for Kaifa bug KI-31
                if (lightTypeValue == null || lightTypeValue.isEmpty()) {
                    lightTypeValue = "RELAY";
                }
                final LightTypeDto lightType = LightTypeDto.valueOf(lightTypeValue);

                deviceMessageLog.addVariable(LogicalNode.STREET_LIGHT_CONFIGURATION,
                        DataAttribute.SOFTWARE_CONFIGURATION, Fc.CF, lightTypeValue);

                DeviceMessageLoggingService.logMessage(deviceMessageLog, deviceConnection.getDeviceIdentification(),
                        deviceConnection.getOrganisationIdentification(), false);

                /*
                 * The preferredLinkType and actualLinkType are hard-coded to
                 * LinkTypeDto.ETHERNET, other link types do not apply to the
                 * device type in use.
                 */
                return new DeviceStatusDto(lightValues, LinkTypeDto.ETHERNET, LinkTypeDto.ETHERNET, lightType,
                        eventNotificationsMask);
            }
        };

        return iec61850Client.sendCommandWithRetry(function, "GetStatus", deviceConnection.getDeviceIdentification());
    }
}
