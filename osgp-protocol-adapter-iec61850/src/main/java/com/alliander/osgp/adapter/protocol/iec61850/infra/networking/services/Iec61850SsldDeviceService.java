/**
 * Copyright 2014-2016 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.alliander.osgp.adapter.protocol.iec61850.infra.networking.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.jms.JMSException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alliander.osgp.adapter.protocol.iec61850.application.mapping.Iec61850Mapper;
import com.alliander.osgp.adapter.protocol.iec61850.device.DeviceMessageStatus;
import com.alliander.osgp.adapter.protocol.iec61850.device.DeviceRequest;
import com.alliander.osgp.adapter.protocol.iec61850.device.DeviceResponseHandler;
import com.alliander.osgp.adapter.protocol.iec61850.device.ssld.SsldDeviceService;
import com.alliander.osgp.adapter.protocol.iec61850.device.ssld.requests.GetPowerUsageHistoryDeviceRequest;
import com.alliander.osgp.adapter.protocol.iec61850.device.ssld.requests.SetConfigurationDeviceRequest;
import com.alliander.osgp.adapter.protocol.iec61850.device.ssld.requests.SetEventNotificationsDeviceRequest;
import com.alliander.osgp.adapter.protocol.iec61850.device.ssld.requests.SetLightDeviceRequest;
import com.alliander.osgp.adapter.protocol.iec61850.device.ssld.requests.SetScheduleDeviceRequest;
import com.alliander.osgp.adapter.protocol.iec61850.device.ssld.requests.SetTransitionDeviceRequest;
import com.alliander.osgp.adapter.protocol.iec61850.device.ssld.requests.UpdateDeviceSslCertificationDeviceRequest;
import com.alliander.osgp.adapter.protocol.iec61850.device.ssld.requests.UpdateFirmwareDeviceRequest;
import com.alliander.osgp.adapter.protocol.iec61850.device.ssld.responses.EmptyDeviceResponse;
import com.alliander.osgp.adapter.protocol.iec61850.device.ssld.responses.GetConfigurationDeviceResponse;
import com.alliander.osgp.adapter.protocol.iec61850.device.ssld.responses.GetFirmwareVersionDeviceResponse;
import com.alliander.osgp.adapter.protocol.iec61850.device.ssld.responses.GetPowerUsageHistoryDeviceResponse;
import com.alliander.osgp.adapter.protocol.iec61850.device.ssld.responses.GetStatusDeviceResponse;
import com.alliander.osgp.adapter.protocol.iec61850.domain.valueobjects.EventType;
import com.alliander.osgp.adapter.protocol.iec61850.exceptions.ConnectionFailureException;
import com.alliander.osgp.adapter.protocol.iec61850.exceptions.NodeWriteException;
import com.alliander.osgp.adapter.protocol.iec61850.exceptions.ProtocolAdapterException;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.Iec61850Client;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.DeviceConnection;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.IED;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.LogicalDevice;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.services.commands.Iec61850ClearReportCommand;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.services.commands.Iec61850EnableReportingCommand;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.services.commands.Iec61850GetConfigurationCommand;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.services.commands.Iec61850GetFirmwareVersionCommand;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.services.commands.Iec61850GetStatusCommand;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.services.commands.Iec61850PowerUsageHistoryCommand;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.services.commands.Iec61850RebootCommand;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.services.commands.Iec61850SetConfigurationCommand;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.services.commands.Iec61850SetEventNotificationFilterCommand;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.services.commands.Iec61850SetLightCommand;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.services.commands.Iec61850SetScheduleCommand;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.services.commands.Iec61850TransitionCommand;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.services.commands.Iec61850UpdateFirmwareCommand;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.services.commands.Iec61850UpdateSslCertificateCommand;
import com.alliander.osgp.core.db.api.iec61850.application.services.SsldDataService;
import com.alliander.osgp.core.db.api.iec61850.entities.DeviceOutputSetting;
import com.alliander.osgp.core.db.api.iec61850.entities.Ssld;
import com.alliander.osgp.core.db.api.iec61850valueobjects.RelayType;
import com.alliander.osgp.dto.valueobjects.ConfigurationDto;
import com.alliander.osgp.dto.valueobjects.DeviceStatusDto;
import com.alliander.osgp.dto.valueobjects.EventNotificationTypeDto;
import com.alliander.osgp.dto.valueobjects.FirmwareVersionDto;
import com.alliander.osgp.dto.valueobjects.LightValueDto;
import com.alliander.osgp.dto.valueobjects.PowerUsageDataDto;
import com.alliander.osgp.shared.exceptionhandling.ComponentType;
import com.alliander.osgp.shared.exceptionhandling.FunctionalException;
import com.alliander.osgp.shared.exceptionhandling.FunctionalExceptionType;
import com.alliander.osgp.shared.exceptionhandling.TechnicalException;

@Component
public class Iec61850SsldDeviceService implements SsldDeviceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(Iec61850SsldDeviceService.class);

    @Autowired
    private Iec61850DeviceConnectionService iec61850DeviceConnectionService;

    @Autowired
    private SsldDataService ssldDataService;

    @Autowired
    private Iec61850Client iec61850Client;

    @Autowired
    private Iec61850Mapper mapper;

    // Timeout between the setLight and getStatus during the device self-test
    @Autowired
    private int selftestTimeout;

    @Autowired
    private int disconnectDelay;

    @Override
    public void getStatus(final DeviceRequest deviceRequest, final DeviceResponseHandler deviceResponseHandler)
            throws JMSException {
        DeviceConnection devCon = null;
        try {
            final DeviceConnection deviceConnection = this.connectToDevice(deviceRequest);
            devCon = deviceConnection;

            // Getting the SSLD for the device output-settings.
            final Ssld ssld = this.ssldDataService.findDevice(deviceRequest.getDeviceIdentification());
            final DeviceStatusDto deviceStatus = new Iec61850GetStatusCommand().getStatusFromDevice(
                    this.iec61850Client, deviceConnection, ssld);

            final GetStatusDeviceResponse deviceResponse = new GetStatusDeviceResponse(
                    deviceRequest.getOrganisationIdentification(), deviceRequest.getDeviceIdentification(),
                    deviceRequest.getCorrelationUid(), deviceStatus);

            deviceResponseHandler.handleResponse(deviceResponse);

            this.enableReporting(deviceConnection, deviceRequest);
        } catch (final ConnectionFailureException se) {
            this.handleConnectionFailureException(deviceRequest, deviceResponseHandler, se);
            this.iec61850DeviceConnectionService.disconnect(devCon, deviceRequest);
        } catch (final Exception e) {
            this.handleException(deviceRequest, deviceResponseHandler, e);
            this.iec61850DeviceConnectionService.disconnect(devCon, deviceRequest);
        }
    }

    @Override
    public void getPowerUsageHistory(final GetPowerUsageHistoryDeviceRequest deviceRequest,
            final DeviceResponseHandler deviceResponseHandler) throws JMSException {
        DeviceConnection deviceConnection = null;
        try {
            deviceConnection = this.connectToDevice(deviceRequest);

            // Getting the SSLD for the device output-settings.
            final Ssld ssld = this.ssldDataService.findDevice(deviceRequest.getDeviceIdentification());
            final List<DeviceOutputSetting> deviceOutputSettingsLightRelays = this.ssldDataService.findByRelayType(
                    ssld, RelayType.LIGHT);

            final List<PowerUsageDataDto> powerUsageHistoryData = new Iec61850PowerUsageHistoryCommand()
            .getPowerUsageHistoryDataFromDevice(this.iec61850Client, deviceConnection,
                    deviceRequest.getPowerUsageHistoryContainer(), deviceOutputSettingsLightRelays);

            final GetPowerUsageHistoryDeviceResponse deviceResponse = new GetPowerUsageHistoryDeviceResponse(
                    deviceRequest.getOrganisationIdentification(), deviceRequest.getDeviceIdentification(),
                    deviceRequest.getCorrelationUid(), DeviceMessageStatus.OK, powerUsageHistoryData);

            deviceResponseHandler.handleResponse(deviceResponse);
        } catch (final ConnectionFailureException se) {
            this.handleConnectionFailureException(deviceRequest, deviceResponseHandler, se);
        } catch (final Exception e) {
            this.handleException(deviceRequest, deviceResponseHandler, e);
        }
        this.iec61850DeviceConnectionService.disconnect(deviceConnection, deviceRequest);
    }

    @Override
    public void setLight(final SetLightDeviceRequest deviceRequest, final DeviceResponseHandler deviceResponseHandler)
            throws JMSException {
        DeviceConnection deviceConnection = null;
        try {
            deviceConnection = this.connectToDevice(deviceRequest);

            // Getting the SSLD for the device output-settings.
            final Ssld ssld = this.ssldDataService.findDevice(deviceRequest.getDeviceIdentification());
            final List<DeviceOutputSetting> deviceOutputSettings = this.ssldDataService.findByRelayType(ssld,
                    RelayType.LIGHT);
            final List<LightValueDto> lightValues = deviceRequest.getLightValuesContainer().getLightValues();
            List<LightValueDto> relaysWithInternalIdToSwitch;

            // Check if external index 0 is used.
            final LightValueDto index0LightValue = this.checkForIndex0(lightValues);
            if (index0LightValue != null) {
                // If external index 0 is used, create a list of all light
                // relays according to the device output settings.
                relaysWithInternalIdToSwitch = this.createListOfInternalIndicesToSwitch(deviceOutputSettings,
                        index0LightValue.isOn());
            } else {
                // Else, create a list of internal indices based on the given
                // external indices in the light values list.
                relaysWithInternalIdToSwitch = this.createListOfInternalIndicesToSwitch(deviceOutputSettings,
                        lightValues);
            }

            // Switch light relays based on internal indices.
            final Iec61850SetLightCommand iec61850SetLightCommand = new Iec61850SetLightCommand();
            for (final LightValueDto relayWithInternalIdToSwitch : relaysWithInternalIdToSwitch) {
                LOGGER.info("Trying to switch light relay with internal index: {} for device: {}",
                        relayWithInternalIdToSwitch.getIndex(), deviceRequest.getDeviceIdentification());
                if (!iec61850SetLightCommand.switchLightRelay(this.iec61850Client, deviceConnection,
                        relayWithInternalIdToSwitch.getIndex(), relayWithInternalIdToSwitch.isOn())) {
                    throw new ProtocolAdapterException(String.format(
                            "Failed to switch light relay with internal index: %d for device: %s",
                            relayWithInternalIdToSwitch.getIndex(), deviceRequest.getDeviceIdentification()));
                }
            }

            this.createSuccessfulDefaultResponse(deviceRequest, deviceResponseHandler);
        } catch (final ConnectionFailureException se) {
            this.handleConnectionFailureException(deviceRequest, deviceResponseHandler, se);
        } catch (final Exception e) {
            this.handleException(deviceRequest, deviceResponseHandler, e);
        }
        this.iec61850DeviceConnectionService.disconnect(deviceConnection, deviceRequest);
    }

    private LightValueDto checkForIndex0(final List<LightValueDto> lightValues) {
        for (final LightValueDto lightValue : lightValues) {
            if (lightValue == null) {
                break;
            }
            if (lightValue.getIndex() == null) {
                return lightValue;
            }
            if (lightValue.getIndex() == 0) {
                return lightValue;
            }
        }
        return null;
    }

    private List<LightValueDto> createListOfInternalIndicesToSwitch(
            final List<DeviceOutputSetting> deviceOutputSettings, final boolean on) {
        LOGGER.info("creating list of internal indices using device output settings");
        final List<LightValueDto> relaysWithInternalIdToSwitch = new ArrayList<>();
        for (final DeviceOutputSetting deviceOutputSetting : deviceOutputSettings) {
            if (RelayType.LIGHT.equals(deviceOutputSetting.getRelayType())) {
                final LightValueDto relayWithInternalIdToSwitch = new LightValueDto(
                        deviceOutputSetting.getInternalId(), on, null);
                relaysWithInternalIdToSwitch.add(relayWithInternalIdToSwitch);
            }
        }
        return relaysWithInternalIdToSwitch;
    }

    private List<LightValueDto> createListOfInternalIndicesToSwitch(
            final List<DeviceOutputSetting> deviceOutputSettings, final List<LightValueDto> lightValues)
                    throws FunctionalException {
        final List<LightValueDto> relaysWithInternalIdToSwitch = new ArrayList<>();
        LOGGER.info("creating list of internal indices using device output settings and external indices from light values");
        for (final LightValueDto lightValue : lightValues) {
            if (lightValue == null) {
                break;
            }
            DeviceOutputSetting deviceOutputSettingForExternalId = null;
            for (final DeviceOutputSetting deviceOutputSetting : deviceOutputSettings) {
                if (deviceOutputSetting.getExternalId() == lightValue.getIndex()) {
                    // You can only switch LIGHT relays that are used.
                    this.checkRelay(deviceOutputSetting.getRelayType(), RelayType.LIGHT,
                            deviceOutputSetting.getInternalId());
                    deviceOutputSettingForExternalId = deviceOutputSetting;
                }
            }
            if (deviceOutputSettingForExternalId != null) {
                final LightValueDto relayWithInternalIdToSwitch = new LightValueDto(
                        deviceOutputSettingForExternalId.getInternalId(), lightValue.isOn(), lightValue.getDimValue());
                relaysWithInternalIdToSwitch.add(relayWithInternalIdToSwitch);
            }
        }
        return relaysWithInternalIdToSwitch;
    }

    @Override
    public void setConfiguration(final SetConfigurationDeviceRequest deviceRequest,
            final DeviceResponseHandler deviceResponseHandler) throws JMSException {
        DeviceConnection deviceConnection = null;
        try {
            deviceConnection = this.connectToDevice(deviceRequest);
            final ConfigurationDto configuration = deviceRequest.getConfiguration();

            // Ignoring required, unused fields DALI-configuration, meterType,
            // shortTermHistoryIntervalMinutes, preferredLinkType,
            // longTermHistoryInterval and longTermHistoryIntervalType.
            new Iec61850SetConfigurationCommand().setConfigurationOnDevice(this.iec61850Client, deviceConnection,
                    configuration);

            this.createSuccessfulDefaultResponse(deviceRequest, deviceResponseHandler);
        } catch (final ConnectionFailureException se) {
            this.handleConnectionFailureException(deviceRequest, deviceResponseHandler, se);
        } catch (final Exception e) {
            this.handleException(deviceRequest, deviceResponseHandler, e);
        }
        this.iec61850DeviceConnectionService.disconnect(deviceConnection, deviceRequest);
    }

    @Override
    public void getConfiguration(final DeviceRequest deviceRequest, final DeviceResponseHandler deviceResponseHandler)
            throws JMSException {
        DeviceConnection deviceConnection = null;
        try {
            deviceConnection = this.connectToDevice(deviceRequest);

            // Getting the SSLD for the device output-settings.
            final Ssld ssld = this.ssldDataService.findDevice(deviceRequest.getDeviceIdentification());

            final ConfigurationDto configuration = new Iec61850GetConfigurationCommand().getConfigurationFromDevice(
                    this.iec61850Client, deviceConnection, ssld, this.mapper);

            final GetConfigurationDeviceResponse response = new GetConfigurationDeviceResponse(
                    deviceRequest.getOrganisationIdentification(), deviceRequest.getDeviceIdentification(),
                    deviceRequest.getCorrelationUid(), DeviceMessageStatus.OK, configuration);

            deviceResponseHandler.handleResponse(response);
        } catch (final ConnectionFailureException se) {
            this.handleConnectionFailureException(deviceRequest, deviceResponseHandler, se);
        } catch (final Exception e) {
            this.handleException(deviceRequest, deviceResponseHandler, e);
        }
        this.iec61850DeviceConnectionService.disconnect(deviceConnection, deviceRequest);
    }

    @Override
    public void setReboot(final DeviceRequest deviceRequest, final DeviceResponseHandler deviceResponseHandler)
            throws JMSException {
        DeviceConnection deviceConnection = null;
        try {
            deviceConnection = this.connectToDevice(deviceRequest);

            new Iec61850RebootCommand().rebootDevice(this.iec61850Client, deviceConnection);

            this.createSuccessfulDefaultResponse(deviceRequest, deviceResponseHandler);
        } catch (final ConnectionFailureException se) {
            this.handleConnectionFailureException(deviceRequest, deviceResponseHandler, se);
        } catch (final Exception e) {
            this.handleException(deviceRequest, deviceResponseHandler, e);
        }
        this.iec61850DeviceConnectionService.disconnect(deviceConnection, deviceRequest);
    }

    @Override
    public void runSelfTest(final DeviceRequest deviceRequest, final DeviceResponseHandler deviceResponseHandler,
            final boolean startOfTest) throws JMSException {
        // Assuming all goes well.
        final DeviceMessageStatus status = DeviceMessageStatus.OK;
        DeviceConnection deviceConnection = null;

        try {
            deviceConnection = this.connectToDevice(deviceRequest);

            // Getting the SSLD for the device output-settings.
            final Ssld ssld = this.ssldDataService.findDevice(deviceRequest.getDeviceIdentification());

            // This list will contain the external indexes of all light relays.
            // It's used to interpret the deviceStatus data later on.
            final List<Integer> lightRelays = new ArrayList<>();

            LOGGER.info("Turning all lights relays {}", startOfTest ? "on" : "off");
            final Iec61850SetLightCommand iec61850SetLightCommand = new Iec61850SetLightCommand();

            // Turning all light relays on or off, depending on the value of
            // startOfTest.
            for (final DeviceOutputSetting deviceOutputSetting : this.ssldDataService.findByRelayType(ssld,
                    RelayType.LIGHT)) {
                lightRelays.add(deviceOutputSetting.getExternalId());
                if (!iec61850SetLightCommand.switchLightRelay(this.iec61850Client, deviceConnection,
                        deviceOutputSetting.getInternalId(), startOfTest)) {
                    throw new ProtocolAdapterException(String.format(
                            "Failed to switch light relay during self-test with internal index: %d for device: %s",
                            deviceOutputSetting.getInternalId(), deviceRequest.getDeviceIdentification()));
                }
            }

            // Sleep and wait.
            this.selfTestSleep();

            // Getting the status.
            final DeviceStatusDto deviceStatus = new Iec61850GetStatusCommand().getStatusFromDevice(
                    this.iec61850Client, deviceConnection, ssld);

            LOGGER.info("Fetching and checking the devicestatus");

            // Checking to see if all light relays have the correct state.
            for (final LightValueDto lightValue : deviceStatus.getLightValues()) {
                if (lightRelays.contains(lightValue.getIndex()) && lightValue.isOn() != startOfTest) {
                    // One the the light relays is not in the correct state,
                    // request failed.
                    throw new ProtocolAdapterException("not all relays are ".concat(startOfTest ? "on" : "off"));
                }
            }

            LOGGER.info("All lights relays are {}, returning OK", startOfTest ? "on" : "off");

            this.createSuccessfulDefaultResponse(deviceRequest, deviceResponseHandler, status);
        } catch (final ConnectionFailureException se) {
            LOGGER.info("Original ConnectionFailureException message: {}", se.getMessage());
            final ConnectionFailureException seGeneric = new ConnectionFailureException("Connection failure", se);

            this.handleConnectionFailureException(deviceRequest, deviceResponseHandler, seGeneric);
        } catch (final Exception e) {
            LOGGER.info("Selftest failure", e);
            final TechnicalException te = new TechnicalException(ComponentType.PROTOCOL_IEC61850, "Selftest failure - "
                    + e.getMessage());
            this.handleException(deviceRequest, deviceResponseHandler, te);
        }
        this.iec61850DeviceConnectionService.disconnect(deviceConnection, deviceRequest);
    }

    private void selfTestSleep() throws TechnicalException {
        try {
            LOGGER.info("Waiting {} milliseconds before getting the device status", this.selftestTimeout);
            Thread.sleep(this.selftestTimeout);
        } catch (final InterruptedException e) {
            LOGGER.error("An InterruptedException occurred during the device selftest timeout.", e);
            throw new TechnicalException(ComponentType.PROTOCOL_IEC61850,
                    "An error occurred during the device selftest timeout.");
        }
    }

    @Override
    public void setSchedule(final SetScheduleDeviceRequest deviceRequest,
            final DeviceResponseHandler deviceResponseHandler) throws JMSException {
        DeviceConnection deviceConnection = null;
        try {
            deviceConnection = this.connectToDevice(deviceRequest);

            // Getting the SSLD for the device output-settings.
            final Ssld ssld = this.ssldDataService.findDevice(deviceRequest.getDeviceIdentification());

            new Iec61850SetScheduleCommand().setScheduleOnDevice(this.iec61850Client, deviceConnection,
                    deviceRequest.getRelayType(), deviceRequest.getScheduleMessageDataContainer().getScheduleList(),
                    ssld, this.ssldDataService);

            this.createSuccessfulDefaultResponse(deviceRequest, deviceResponseHandler);
        } catch (final ConnectionFailureException se) {
            this.handleConnectionFailureException(deviceRequest, deviceResponseHandler, se);
        } catch (final ProtocolAdapterException e) {
            this.handleProtocolAdapterException(deviceRequest, deviceResponseHandler, e);
        } catch (final Exception e) {
            this.handleException(deviceRequest, deviceResponseHandler, e);
        }
        this.iec61850DeviceConnectionService.disconnect(deviceConnection, deviceRequest);
    }

    @Override
    public void getFirmwareVersion(final DeviceRequest deviceRequest, final DeviceResponseHandler deviceResponseHandler)
            throws JMSException {
        DeviceConnection deviceConnection = null;
        try {
            deviceConnection = this.connectToDevice(deviceRequest);

            final List<FirmwareVersionDto> firmwareVersions = new Iec61850GetFirmwareVersionCommand()
            .getFirmwareVersionFromDevice(this.iec61850Client, deviceConnection);

            final GetFirmwareVersionDeviceResponse deviceResponse = new GetFirmwareVersionDeviceResponse(
                    deviceRequest.getOrganisationIdentification(), deviceRequest.getDeviceIdentification(),
                    deviceRequest.getCorrelationUid(), firmwareVersions);

            deviceResponseHandler.handleResponse(deviceResponse);
        } catch (final ConnectionFailureException se) {
            this.handleConnectionFailureException(deviceRequest, deviceResponseHandler, se);
        } catch (final Exception e) {
            this.handleException(deviceRequest, deviceResponseHandler, e);
        }
        this.iec61850DeviceConnectionService.disconnect(deviceConnection, deviceRequest);
    }

    @Override
    public void setTransition(final SetTransitionDeviceRequest deviceRequest,
            final DeviceResponseHandler deviceResponseHandler) throws JMSException {
        DeviceConnection devCon = null;
        try {
            final DeviceConnection deviceConnection = this.connectToDevice(deviceRequest);
            devCon = deviceConnection;

            new Iec61850TransitionCommand().transitionDevice(this.iec61850Client, deviceConnection,
                    deviceRequest.getTransitionTypeContainer());

            this.createSuccessfulDefaultResponse(deviceRequest, deviceResponseHandler);

            this.enableReporting(deviceConnection, deviceRequest);
        } catch (final ConnectionFailureException se) {
            this.handleConnectionFailureException(deviceRequest, deviceResponseHandler, se);
            this.iec61850DeviceConnectionService.disconnect(devCon, deviceRequest);
        } catch (final Exception e) {
            this.handleException(deviceRequest, deviceResponseHandler, e);
            this.iec61850DeviceConnectionService.disconnect(devCon, deviceRequest);
        }
    }

    @Override
    public void updateFirmware(final UpdateFirmwareDeviceRequest deviceRequest,
            final DeviceResponseHandler deviceResponseHandler) throws JMSException {
        DeviceConnection deviceConnection = null;
        try {
            deviceConnection = this.connectToDevice(deviceRequest);

            new Iec61850UpdateFirmwareCommand().pushFirmwareToDevice(this.iec61850Client, deviceConnection,
                    deviceRequest.getFirmwareDomain().concat(deviceRequest.getFirmwareUrl()),
                    deviceRequest.getFirmwareModuleData());

            this.createSuccessfulDefaultResponse(deviceRequest, deviceResponseHandler);
        } catch (final ConnectionFailureException se) {
            this.handleConnectionFailureException(deviceRequest, deviceResponseHandler, se);
        } catch (final Exception e) {
            this.handleException(deviceRequest, deviceResponseHandler, e);
        }
        this.iec61850DeviceConnectionService.disconnect(deviceConnection, deviceRequest);
    }

    @Override
    public void updateDeviceSslCertification(final UpdateDeviceSslCertificationDeviceRequest deviceRequest,
            final DeviceResponseHandler deviceResponseHandler) throws JMSException {
        DeviceConnection deviceConnection = null;
        try {
            deviceConnection = this.connectToDevice(deviceRequest);

            new Iec61850UpdateSslCertificateCommand().pushSslCertificateToDevice(this.iec61850Client, deviceConnection,
                    deviceRequest.getCertification());

            this.createSuccessfulDefaultResponse(deviceRequest, deviceResponseHandler);
        } catch (final ConnectionFailureException se) {
            this.handleConnectionFailureException(deviceRequest, deviceResponseHandler, se);
        } catch (final Exception e) {
            this.handleException(deviceRequest, deviceResponseHandler, e);
        }
        this.iec61850DeviceConnectionService.disconnect(deviceConnection, deviceRequest);
    }

    @Override
    public void setEventNotifications(final SetEventNotificationsDeviceRequest deviceRequest,
            final DeviceResponseHandler deviceResponseHandler) throws JMSException {
        final List<EventNotificationTypeDto> eventNotifications = deviceRequest.getEventNotificationsContainer()
                .getEventNotifications();
        final String filter = EventType.getEventTypeFilterMaskForNotificationTypes(eventNotifications);

        DeviceConnection deviceConnection = null;
        try {
            deviceConnection = this.connectToDevice(deviceRequest);

            new Iec61850SetEventNotificationFilterCommand().setEventNotificationFilterOnDevice(this.iec61850Client,
                    deviceConnection, filter);

            this.createSuccessfulDefaultResponse(deviceRequest, deviceResponseHandler);
        } catch (final ConnectionFailureException se) {
            this.handleConnectionFailureException(deviceRequest, deviceResponseHandler, se);
        } catch (final Exception e) {
            this.handleException(deviceRequest, deviceResponseHandler, e);
        }
        this.iec61850DeviceConnectionService.disconnect(deviceConnection, deviceRequest);
    }

    // ======================================
    // PRIVATE DEVICE COMMUNICATION METHODS =
    // ======================================

    private DeviceConnection connectToDevice(final DeviceRequest deviceRequest) throws ConnectionFailureException {
        return this.iec61850DeviceConnectionService.connectWithoutConnectionCaching(deviceRequest.getIpAddress(),
                deviceRequest.getDeviceIdentification(), deviceRequest.getOrganisationIdentification(), IED.FLEX_OVL,
                IED.FLEX_OVL.getDescription(), LogicalDevice.LIGHTING.getDescription());
    }

    // ========================
    // PRIVATE HELPER METHODS =
    // ========================

    private EmptyDeviceResponse createDefaultResponse(final DeviceRequest deviceRequest,
            final DeviceMessageStatus deviceMessageStatus) {
        return new EmptyDeviceResponse(deviceRequest.getOrganisationIdentification(),
                deviceRequest.getDeviceIdentification(), deviceRequest.getCorrelationUid(), deviceMessageStatus);
    }

    private void createSuccessfulDefaultResponse(final DeviceRequest deviceRequest,
            final DeviceResponseHandler deviceResponseHandler) {
        this.createSuccessfulDefaultResponse(deviceRequest, deviceResponseHandler, DeviceMessageStatus.OK);
    }

    private void createSuccessfulDefaultResponse(final DeviceRequest deviceRequest,
            final DeviceResponseHandler deviceResponseHandler, final DeviceMessageStatus deviceMessageStatus) {
        final EmptyDeviceResponse deviceResponse = this.createDefaultResponse(deviceRequest, deviceMessageStatus);
        deviceResponseHandler.handleResponse(deviceResponse);
    }

    private void handleConnectionFailureException(final DeviceRequest deviceRequest,
            final DeviceResponseHandler deviceResponseHandler,
            final ConnectionFailureException connectionFailureException) throws JMSException {
        LOGGER.error("Could not connect to device", connectionFailureException);
        final EmptyDeviceResponse deviceResponse = this.createDefaultResponse(deviceRequest,
                DeviceMessageStatus.FAILURE);
        deviceResponseHandler.handleConnectionFailure(connectionFailureException, deviceResponse);
    }

    private void handleProtocolAdapterException(final SetScheduleDeviceRequest deviceRequest,
            final DeviceResponseHandler deviceResponseHandler, final ProtocolAdapterException protocolAdapterException) {
        LOGGER.error("Could not complete the request: " + deviceRequest.getMessageType() + " for device: "
                + deviceRequest.getDeviceIdentification(), protocolAdapterException);
        final EmptyDeviceResponse deviceResponse = this.createDefaultResponse(deviceRequest,
                DeviceMessageStatus.FAILURE);
        deviceResponseHandler.handleException(protocolAdapterException, deviceResponse);
    }

    private void handleException(final DeviceRequest deviceRequest, final DeviceResponseHandler deviceResponseHandler,
            final Exception exception) {
        LOGGER.error("Unexpected exception", exception);
        final EmptyDeviceResponse deviceResponse = this.createDefaultResponse(deviceRequest,
                DeviceMessageStatus.FAILURE);
        deviceResponseHandler.handleException(exception, deviceResponse);
    }

    // ========================
    // This method is duplicated in one of the command implementations. This
    // needs to be refactored. =
    // ========================

    /*
     * Checks to see if the relay has the correct type, throws an exception when
     * that't not the case
     */
    private void checkRelay(final RelayType actual, final RelayType expected, final Integer internalAddress)
            throws FunctionalException {
        if (!actual.equals(expected)) {
            if (RelayType.LIGHT.equals(expected)) {
                LOGGER.error("Relay with internal address: {} is not configured as light relay", internalAddress);
                throw new FunctionalException(FunctionalExceptionType.ACTION_NOT_ALLOWED_FOR_LIGHT_RELAY,
                        ComponentType.PROTOCOL_IEC61850);
            } else {
                LOGGER.error("Relay with internal address: {} is not configured as tariff relay", internalAddress);
                throw new FunctionalException(FunctionalExceptionType.ACTION_NOT_ALLOWED_FOR_TARIFF_RELAY,
                        ComponentType.PROTOCOL_IEC61850);
            }
        }
    }

    private void enableReporting(final DeviceConnection deviceConnection, final DeviceRequest deviceRequest)
            throws NodeWriteException {
        // Enabling device reporting.
        new Iec61850EnableReportingCommand().enableReportingOnDeviceWithoutUsingSequenceNumber(this.iec61850Client,
                deviceConnection);
        // Don't disconnect now! The device should be able to send reports.
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    new Iec61850ClearReportCommand().clearReportOnDevice(deviceConnection);
                } catch (final ProtocolAdapterException e) {
                    LOGGER.error("Unable to clear report for device: " + deviceRequest.getDeviceIdentification(), e);
                }
                Iec61850SsldDeviceService.this.iec61850DeviceConnectionService.disconnect(deviceConnection,
                        deviceRequest);
            }
        }, this.disconnectDelay);
    }
}
