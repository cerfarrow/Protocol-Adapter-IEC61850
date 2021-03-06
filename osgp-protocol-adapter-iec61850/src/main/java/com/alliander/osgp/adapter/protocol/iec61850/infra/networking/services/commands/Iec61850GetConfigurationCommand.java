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

import org.joda.time.DateTimeZone;
import org.openmuc.openiec61850.Fc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alliander.osgp.adapter.protocol.iec61850.application.mapping.Iec61850Mapper;
import com.alliander.osgp.adapter.protocol.iec61850.domain.valueobjects.DaylightSavingTimeTransition;
import com.alliander.osgp.adapter.protocol.iec61850.domain.valueobjects.DeviceMessageLog;
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
import com.alliander.osgp.core.db.api.iec61850valueobjects.RelayType;
import com.alliander.osgp.dto.valueobjects.ConfigurationDto;
import com.alliander.osgp.dto.valueobjects.DaliConfigurationDto;
import com.alliander.osgp.dto.valueobjects.DeviceFixedIpDto;
import com.alliander.osgp.dto.valueobjects.LightTypeDto;
import com.alliander.osgp.dto.valueobjects.LinkTypeDto;
import com.alliander.osgp.dto.valueobjects.LongTermIntervalTypeDto;
import com.alliander.osgp.dto.valueobjects.MeterTypeDto;
import com.alliander.osgp.dto.valueobjects.RelayConfigurationDto;
import com.alliander.osgp.dto.valueobjects.RelayMapDto;

public class Iec61850GetConfigurationCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(Iec61850GetConfigurationCommand.class);

    private static final int SWITCH_TYPE_TARIFF = 0;
    private static final int SWITCH_TYPE_LIGHT = 1;
    private static final DateTimeZone TIME_ZONE_AMSTERDAM = DateTimeZone.forID("Europe/Amsterdam");

    public ConfigurationDto getConfigurationFromDevice(final Iec61850Client iec61850Client,
            final DeviceConnection deviceConnection, final Ssld ssld, final Iec61850Mapper mapper)
            throws ProtocolAdapterException {
        final Function<ConfigurationDto> function = new Function<ConfigurationDto>() {

            @Override
            public ConfigurationDto apply(final DeviceMessageLog deviceMessageLog) throws Exception {
                // Keeping the hardcoded values and values that aren't fetched
                // from the device out of the Function.

                // Hardcoded (not supported)
                final MeterTypeDto meterType = MeterTypeDto.AUX;
                // Hardcoded (not supported)
                final Integer shortTermHistoryIntervalMinutes = 15;
                // Hardcoded (not supported)
                final LinkTypeDto preferredLinkType = LinkTypeDto.ETHERNET;
                // Hardcoded (not supported)
                final Integer longTermHistoryInterval = 1;
                // Hardcoded (not supported)
                final LongTermIntervalTypeDto longTermHistoryIntervalType = LongTermIntervalTypeDto.DAYS;

                final List<RelayMapDto> relayMaps = new ArrayList<>();

                for (final DeviceOutputSetting deviceOutputSetting : ssld.getOutputSettings()) {
                    Iec61850GetConfigurationCommand.this.checkRelayType(iec61850Client, deviceConnection,
                            deviceOutputSetting, deviceMessageLog);
                    relayMaps.add(mapper.map(deviceOutputSetting, RelayMapDto.class));
                }

                final RelayConfigurationDto relayConfiguration = new RelayConfigurationDto(relayMaps);

                // PSLD specific => just sending null so it'll be ignored
                final DaliConfigurationDto daliConfiguration = null;

                // getting the software configuration values
                LOGGER.info("Reading the software configuration values");
                final NodeContainer softwareConfiguration = deviceConnection.getFcModelNode(LogicalDevice.LIGHTING,
                        LogicalNode.STREET_LIGHT_CONFIGURATION, DataAttribute.SOFTWARE_CONFIGURATION, Fc.CF);
                iec61850Client.readNodeDataValues(deviceConnection.getConnection().getClientAssociation(),
                        softwareConfiguration.getFcmodelNode());

                String lightTypeValue = softwareConfiguration.getString(SubDataAttribute.LIGHT_TYPE);
                // Fix for Kaifa bug KI-31
                if (lightTypeValue == null || lightTypeValue.isEmpty()) {
                    lightTypeValue = "RELAY";
                }

                deviceMessageLog.addVariable(LogicalNode.STREET_LIGHT_CONFIGURATION,
                        DataAttribute.SOFTWARE_CONFIGURATION, Fc.CF, SubDataAttribute.LIGHT_TYPE, lightTypeValue);

                final LightTypeDto lightType = LightTypeDto.valueOf(lightTypeValue);
                final short astroGateSunRiseOffset = softwareConfiguration
                        .getShort(SubDataAttribute.ASTRONOMIC_SUNRISE_OFFSET).getValue();
                final short astroGateSunSetOffset = softwareConfiguration
                        .getShort(SubDataAttribute.ASTRONOMIC_SUNSET_OFFSET).getValue();

                deviceMessageLog.addVariable(LogicalNode.STREET_LIGHT_CONFIGURATION,
                        DataAttribute.SOFTWARE_CONFIGURATION, Fc.CF, SubDataAttribute.ASTRONOMIC_SUNRISE_OFFSET,
                        Short.toString(astroGateSunRiseOffset));
                deviceMessageLog.addVariable(LogicalNode.STREET_LIGHT_CONFIGURATION,
                        DataAttribute.SOFTWARE_CONFIGURATION, Fc.CF, SubDataAttribute.ASTRONOMIC_SUNSET_OFFSET,
                        Short.toString(astroGateSunSetOffset));

                final ConfigurationDto configuration = new ConfigurationDto(lightType, daliConfiguration,
                        relayConfiguration, shortTermHistoryIntervalMinutes, preferredLinkType, meterType,
                        longTermHistoryInterval, longTermHistoryIntervalType);

                // getting the registration configuration values
                LOGGER.info("Reading the registration configuration values");
                final NodeContainer registration = deviceConnection.getFcModelNode(LogicalDevice.LIGHTING,
                        LogicalNode.STREET_LIGHT_CONFIGURATION, DataAttribute.REGISTRATION, Fc.CF);
                iec61850Client.readNodeDataValues(deviceConnection.getConnection().getClientAssociation(),
                        registration.getFcmodelNode());

                final String serverAddress = registration.getString(SubDataAttribute.SERVER_ADDRESS);
                final int serverPort = registration.getInteger(SubDataAttribute.SERVER_PORT).getValue();

                configuration.setOsgpIpAddress(serverAddress);
                configuration.setOsgpPortNumber(serverPort);

                deviceMessageLog.addVariable(LogicalNode.STREET_LIGHT_CONFIGURATION, DataAttribute.REGISTRATION, Fc.CF,
                        SubDataAttribute.SERVER_ADDRESS, serverAddress);
                deviceMessageLog.addVariable(LogicalNode.STREET_LIGHT_CONFIGURATION, DataAttribute.REGISTRATION, Fc.CF,
                        SubDataAttribute.SERVER_PORT, Integer.toString(serverPort));

                // getting the IP configuration values
                LOGGER.info("Reading the IP configuration values");
                final NodeContainer ipConfiguration = deviceConnection.getFcModelNode(LogicalDevice.LIGHTING,
                        LogicalNode.STREET_LIGHT_CONFIGURATION, DataAttribute.IP_CONFIGURATION, Fc.CF);
                iec61850Client.readNodeDataValues(deviceConnection.getConnection().getClientAssociation(),
                        ipConfiguration.getFcmodelNode());

                final String deviceFixedIpAddress = ipConfiguration.getString(SubDataAttribute.IP_ADDRESS);
                final String deviceFixedIpNetmask = ipConfiguration.getString(SubDataAttribute.NETMASK);
                final String deviceFixedIpGateway = ipConfiguration.getString(SubDataAttribute.GATEWAY);
                final boolean isDhcpEnabled = ipConfiguration.getBoolean(SubDataAttribute.ENABLE_DHCP).getValue();

                final DeviceFixedIpDto deviceFixedIp = new DeviceFixedIpDto(deviceFixedIpAddress, deviceFixedIpNetmask,
                        deviceFixedIpGateway);

                configuration.setDeviceFixedIp(deviceFixedIp);
                configuration.setDhcpEnabled(isDhcpEnabled);

                deviceMessageLog.addVariable(LogicalNode.STREET_LIGHT_CONFIGURATION, DataAttribute.IP_CONFIGURATION,
                        Fc.CF, SubDataAttribute.IP_ADDRESS, deviceFixedIpAddress);
                deviceMessageLog.addVariable(LogicalNode.STREET_LIGHT_CONFIGURATION, DataAttribute.IP_CONFIGURATION,
                        Fc.CF, SubDataAttribute.NETMASK, deviceFixedIpNetmask);
                deviceMessageLog.addVariable(LogicalNode.STREET_LIGHT_CONFIGURATION, DataAttribute.IP_CONFIGURATION,
                        Fc.CF, SubDataAttribute.GATEWAY, deviceFixedIpGateway);
                deviceMessageLog.addVariable(LogicalNode.STREET_LIGHT_CONFIGURATION, DataAttribute.IP_CONFIGURATION,
                        Fc.CF, SubDataAttribute.ENABLE_DHCP, Boolean.toString(isDhcpEnabled));

                // setting the software configuration values
                configuration.setAstroGateSunRiseOffset((int) astroGateSunRiseOffset);
                configuration.setAstroGateSunSetOffset((int) astroGateSunSetOffset);

                // getting the clock configuration values
                LOGGER.info("Reading the clock configuration values");
                final NodeContainer clock = deviceConnection.getFcModelNode(LogicalDevice.LIGHTING,
                        LogicalNode.STREET_LIGHT_CONFIGURATION, DataAttribute.CLOCK, Fc.CF);
                iec61850Client.readNodeDataValues(deviceConnection.getConnection().getClientAssociation(),
                        clock.getFcmodelNode());

                final int timeSyncFrequency = clock.getUnsignedShort(SubDataAttribute.TIME_SYNC_FREQUENCY).getValue();
                final boolean automaticSummerTimingEnabled = clock
                        .getBoolean(SubDataAttribute.AUTOMATIC_SUMMER_TIMING_ENABLED).getValue();
                final String summerTimeDetails = clock.getString(SubDataAttribute.SUMMER_TIME_DETAILS);
                final String winterTimeDetails = clock.getString(SubDataAttribute.WINTER_TIME_DETAILS);

                configuration.setTimeSyncFrequency(timeSyncFrequency);
                configuration.setAutomaticSummerTimingEnabled(automaticSummerTimingEnabled);
                configuration
                        .setSummerTimeDetails(new DaylightSavingTimeTransition(TIME_ZONE_AMSTERDAM, summerTimeDetails)
                                .getDateTimeForNextTransition().toDateTime(DateTimeZone.UTC));
                configuration
                        .setWinterTimeDetails(new DaylightSavingTimeTransition(TIME_ZONE_AMSTERDAM, winterTimeDetails)
                                .getDateTimeForNextTransition().toDateTime(DateTimeZone.UTC));

                deviceMessageLog.addVariable(LogicalNode.STREET_LIGHT_CONFIGURATION, DataAttribute.CLOCK, Fc.CF,
                        SubDataAttribute.TIME_SYNC_FREQUENCY, Integer.toString(timeSyncFrequency));
                deviceMessageLog.addVariable(LogicalNode.STREET_LIGHT_CONFIGURATION, DataAttribute.CLOCK, Fc.CF,
                        SubDataAttribute.AUTOMATIC_SUMMER_TIMING_ENABLED,
                        Boolean.toString(automaticSummerTimingEnabled));
                deviceMessageLog.addVariable(LogicalNode.STREET_LIGHT_CONFIGURATION, DataAttribute.CLOCK, Fc.CF,
                        SubDataAttribute.SUMMER_TIME_DETAILS, summerTimeDetails);
                deviceMessageLog.addVariable(LogicalNode.STREET_LIGHT_CONFIGURATION, DataAttribute.CLOCK, Fc.CF,
                        SubDataAttribute.WINTER_TIME_DETAILS, winterTimeDetails);

                // getting the TLS configuration values
                // LOGGER.info("Reading the TLS configuration values");
                // final NodeContainer tls =
                // deviceConnection.getFcModelNode(LogicalDevice.LIGHTING,
                // LogicalNode.STREET_LIGHT_CONFIGURATION,
                // DataAttribute.TLS_CONFIGURATION, Fc.CF);
                //
                // iec61850Client.readNodeDataValues(deviceConnection.getConnection().getClientAssociation(),
                // tls.getFcmodelNode());
                //
                // final int tlsPortNumber = (int)
                // tls.getUnsignedInteger(SubDataAttribute.TLS_PORT_NUMBER).getValue();
                // final boolean tlsEnabled =
                // tls.getBoolean(SubDataAttribute.TLS_ENABLED).getValue();
                // final String commonName =
                // tls.getString(SubDataAttribute.TLS_COMMON_NAME);
                //
                // configuration.setTlsPortNumber(tlsPortNumber);
                // configuration.setTlsEnabled(tlsEnabled);
                // configuration.setCommonNameString(commonName);

                DeviceMessageLoggingService.logMessage(deviceMessageLog, deviceConnection.getDeviceIdentification(),
                        deviceConnection.getOrganisationIdentification(), false);

                return configuration;
            }
        };

        return iec61850Client.sendCommandWithRetry(function, "GetConfiguration",
                deviceConnection.getDeviceIdentification());
    }

    private void checkRelayType(final Iec61850Client iec61850Client, final DeviceConnection deviceConnection,
            final DeviceOutputSetting deviceOutputSetting, final DeviceMessageLog deviceMessageLog)
            throws ProtocolAdapterException {
        final RelayType registeredRelayType = deviceOutputSetting.getRelayType();

        final int expectedSwType;
        if (RelayType.LIGHT.equals(registeredRelayType)) {
            expectedSwType = SWITCH_TYPE_LIGHT;
        } else if (RelayType.TARIFF.equals(registeredRelayType)
                || RelayType.TARIFF_REVERSED.equals(registeredRelayType)) {
            expectedSwType = SWITCH_TYPE_TARIFF;
        } else {
            throw new ProtocolAdapterException("DeviceOutputSetting (internal index = "
                    + deviceOutputSetting.getInternalId() + ", external index = " + deviceOutputSetting.getExternalId()
                    + ") does not have a known RelayType: " + registeredRelayType);
        }

        final LogicalNode logicalNode = LogicalNode.getSwitchComponentByIndex(deviceOutputSetting.getInternalId());
        final NodeContainer switchType = deviceConnection.getFcModelNode(LogicalDevice.LIGHTING, logicalNode,
                DataAttribute.SWITCH_TYPE, Fc.ST);
        iec61850Client.readNodeDataValues(deviceConnection.getConnection().getClientAssociation(),
                switchType.getFcmodelNode());

        final int switchTypeValue = switchType.getByte(SubDataAttribute.STATE).getValue();
        if (expectedSwType != switchTypeValue) {
            throw new ProtocolAdapterException("DeviceOutputSetting (internal index = "
                    + deviceOutputSetting.getInternalId() + ", external index = " + deviceOutputSetting.getExternalId()
                    + ") has a RelayType (" + registeredRelayType + ") that does not match the SwType on the device: "
                    + (switchTypeValue == SWITCH_TYPE_TARIFF ? "Tariff switch (0)"
                            : (switchTypeValue == SWITCH_TYPE_LIGHT ? "Light switch (1)"
                                    : "Unknown value: " + switchTypeValue)));
        }

        deviceMessageLog.addVariable(logicalNode, DataAttribute.SWITCH_TYPE, Fc.ST, SubDataAttribute.STATE,
                Integer.toString(switchTypeValue));
    }
}
