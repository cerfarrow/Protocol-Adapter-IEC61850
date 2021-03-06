/**
 * Copyright 2017 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.alliander.osgp.adapter.protocol.iec61850.infra.networking.services;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alliander.osgp.adapter.protocol.iec61850.device.rtu.RtuReadCommand;
import com.alliander.osgp.adapter.protocol.iec61850.device.rtu.RtuWriteCommand;
import com.alliander.osgp.adapter.protocol.iec61850.exceptions.NodeException;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.Iec61850Client;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.SystemService;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.DeviceConnection;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.LogicalDevice;
import com.alliander.osgp.dto.valueobjects.microgrids.GetDataSystemIdentifierDto;
import com.alliander.osgp.dto.valueobjects.microgrids.MeasurementDto;
import com.alliander.osgp.dto.valueobjects.microgrids.MeasurementFilterDto;
import com.alliander.osgp.dto.valueobjects.microgrids.ProfileDto;
import com.alliander.osgp.dto.valueobjects.microgrids.ProfileFilterDto;
import com.alliander.osgp.dto.valueobjects.microgrids.SetDataSystemIdentifierDto;
import com.alliander.osgp.dto.valueobjects.microgrids.SetPointDto;
import com.alliander.osgp.dto.valueobjects.microgrids.SystemFilterDto;

@Service
public class Iec61850BoilerSystemService implements SystemService {

    private static final Logger LOGGER = LoggerFactory.getLogger(Iec61850BoilerSystemService.class);
    private static final LogicalDevice DEVICE = LogicalDevice.BOILER;

    @Autowired
    private Iec61850BoilerCommandFactory iec61850BoilerCommandFactory;

    @Override
    public GetDataSystemIdentifierDto getData(final SystemFilterDto systemFilter, final Iec61850Client client,
            final DeviceConnection connection) throws NodeException {
        final int logicalDeviceIndex = systemFilter.getId();

        LOGGER.info("Get data called for logical device {}{}", DEVICE.getDescription(), logicalDeviceIndex);

        final List<MeasurementDto> measurements = new ArrayList<>();

        for (final MeasurementFilterDto filter : systemFilter.getMeasurementFilters()) {

            final RtuReadCommand<MeasurementDto> command = this.iec61850BoilerCommandFactory.getCommand(filter);
            if (command == null) {
                LOGGER.warn("Unsupported data attribute [{}], skip get data for it", filter.getNode());
            } else {
                measurements.add(command.execute(client, connection, DEVICE, logicalDeviceIndex));
            }
        }

        final List<ProfileDto> profiles = new ArrayList<>();

        for (final ProfileFilterDto filter : systemFilter.getProfileFilters()) {

            final RtuReadCommand<ProfileDto> command = Iec61850RtuReadProfileCommandFactory.getInstance()
                    .getCommand(filter);
            if (command == null) {
                LOGGER.warn("Unsupported data attribute [{}], skip get data for it", filter.getNode());
            } else {
                profiles.add(command.execute(client, connection, DEVICE, logicalDeviceIndex));
            }
        }

        return new GetDataSystemIdentifierDto(systemFilter.getId(), systemFilter.getSystemType(), measurements,
                profiles);
    }

    @Override
    public void setData(final SetDataSystemIdentifierDto systemIdentifier, final Iec61850Client client,
            final DeviceConnection connection) throws NodeException {
        final int logicalDeviceIndex = systemIdentifier.getId();

        LOGGER.info("Set data called for logical device {}{}", DEVICE.getDescription(), logicalDeviceIndex);

        for (final SetPointDto sp : systemIdentifier.getSetPoints()) {

            final RtuWriteCommand<SetPointDto> command = Iec61850SetPointCommandFactory.getInstance()
                    .getCommand(sp.getNode() + sp.getId());

            if (command == null) {
                LOGGER.warn("Unsupported set point [{}], skip set data for it.", sp.getNode() + sp.getId());
            } else {
                command.executeWrite(client, connection, DEVICE, logicalDeviceIndex, sp);
            }
        }

        for (final ProfileDto p : systemIdentifier.getProfiles()) {
            final RtuWriteCommand<ProfileDto> command = Iec61850WriteProfileCommandFactory.getInstance()
                    .getCommand(p.getNode() + p.getId());
            if (command == null) {
                LOGGER.warn("Unsupported profile [{}], skip set data for it.", p.getNode() + p.getId());
            } else {
                command.executeWrite(client, connection, DEVICE, logicalDeviceIndex, p);
            }
        }
    }

}
