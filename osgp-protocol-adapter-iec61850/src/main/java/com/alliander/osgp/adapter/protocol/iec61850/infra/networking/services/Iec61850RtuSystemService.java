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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alliander.osgp.adapter.protocol.iec61850.device.rtu.RtuCommand;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.Iec61850Client;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.SystemService;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.DeviceConnection;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.LogicalDevice;
import com.alliander.osgp.dto.valueobjects.microgrids.MeasurementDto;
import com.alliander.osgp.dto.valueobjects.microgrids.MeasurementFilterDto;
import com.alliander.osgp.dto.valueobjects.microgrids.SystemFilterDto;

public class Iec61850RtuSystemService implements SystemService {

    private static final Logger LOGGER = LoggerFactory.getLogger(Iec61850RtuSystemService.class);
    private static final String DEVICE = "RTU";

    @Override
    public List<MeasurementDto> GetData(final SystemFilterDto systemFilter, final Iec61850Client client,
            final DeviceConnection connection) {

        final List<MeasurementDto> measurements = new ArrayList<>();

        for (final MeasurementFilterDto filter : systemFilter.getMeasurementFilters()) {

            final RtuCommand command = Iec61850RtuCommandFactory.getInstance().getCommand(filter);
            if (command == null) {
                LOGGER.warn("Unsupported data attribute [{}], skip get data for it", filter.getNode());
            } else {
                measurements.add(command.execute(client, connection, this.getLogicalDevice(systemFilter.getId())));
            }

        }

        return measurements;
    }

    private LogicalDevice getLogicalDevice(final int id) {
        return LogicalDevice.fromString(DEVICE + id);
    }
}
