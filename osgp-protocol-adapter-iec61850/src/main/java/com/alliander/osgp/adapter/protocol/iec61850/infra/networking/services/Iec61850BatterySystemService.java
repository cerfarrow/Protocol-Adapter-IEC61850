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

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alliander.osgp.adapter.protocol.iec61850.device.rtu.RtuReadCommand;
import com.alliander.osgp.adapter.protocol.iec61850.exceptions.NodeReadException;
import com.alliander.osgp.adapter.protocol.iec61850.exceptions.NodeWriteException;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.Iec61850Client;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.SystemService;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.DeviceConnection;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.LogicalDevice;
import com.alliander.osgp.dto.valueobjects.microgrids.GetDataSystemIdentifierDto;
import com.alliander.osgp.dto.valueobjects.microgrids.MeasurementDto;
import com.alliander.osgp.dto.valueobjects.microgrids.MeasurementFilterDto;
import com.alliander.osgp.dto.valueobjects.microgrids.SetDataSystemIdentifierDto;
import com.alliander.osgp.dto.valueobjects.microgrids.SystemFilterDto;

public class Iec61850BatterySystemService implements SystemService {

    private static final Logger LOGGER = LoggerFactory.getLogger(Iec61850BatterySystemService.class);
    private static final String DEVICE = "BATTERY";

    private int index;
    private LogicalDevice logicalDevice;

    public Iec61850BatterySystemService(final int index) {
        this.index = index;
        this.logicalDevice = LogicalDevice.fromString(DEVICE + index);
    }

    @Override
    public GetDataSystemIdentifierDto getData(final SystemFilterDto systemFilter, final Iec61850Client client,
            final DeviceConnection connection) throws NodeReadException {

        LOGGER.info("Get data called for logical device {}", DEVICE + this.index);

        final List<MeasurementDto> measurements = new ArrayList<>();

        for (final MeasurementFilterDto filter : systemFilter.getMeasurementFilters()) {

            final RtuReadCommand<MeasurementDto> command = Iec61850BatteryCommandFactory.getInstance()
                    .getCommand(filter);
            if (command == null) {
                LOGGER.warn("Unsupported data attribute [{}], skip get data for it", filter.getNode());
            } else {
                measurements.add(command.execute(client, connection, this.logicalDevice));
            }

        }

        return new GetDataSystemIdentifierDto(systemFilter.getId(), systemFilter.getSystemType(), measurements);
    }

    @Override
    public void setData(final SetDataSystemIdentifierDto systemIdentifier, final Iec61850Client client,
            final DeviceConnection connection) throws NodeWriteException {

        throw new NotImplementedException("Set data is not yet implemented for Battery.");

    }
}
