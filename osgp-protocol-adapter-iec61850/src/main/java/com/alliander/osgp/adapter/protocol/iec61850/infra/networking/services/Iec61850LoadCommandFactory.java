/**
 * Copyright 2014-2016 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.alliander.osgp.adapter.protocol.iec61850.infra.networking.services;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alliander.osgp.adapter.protocol.iec61850.device.rtu.RtuCommand;
import com.alliander.osgp.adapter.protocol.iec61850.device.rtu.RtuCommandFactory;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.DataAttribute;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.services.commands.Iec61850ActualPowerCommand;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.services.commands.Iec61850BehaviourCommand;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.services.commands.Iec61850HealthCommand;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.services.commands.Iec61850LoadTotalEnergyCommand;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.services.commands.Iec61850ModeCommand;
import com.alliander.osgp.dto.valueobjects.microgrids.MeasurementFilterDto;

public class Iec61850LoadCommandFactory implements RtuCommandFactory {

    private static Logger LOGGER = LoggerFactory.getLogger(Iec61850LoadCommandFactory.class);

    private static Iec61850LoadCommandFactory instance;

    private Map<DataAttribute, RtuCommand> rtuCommandMap = new HashMap<>();

    private Iec61850LoadCommandFactory() {
        this.rtuCommandMap.put(DataAttribute.BEHAVIOR, new Iec61850BehaviourCommand());
        this.rtuCommandMap.put(DataAttribute.HEALTH, new Iec61850HealthCommand());
        this.rtuCommandMap.put(DataAttribute.MODE, new Iec61850ModeCommand());
        this.rtuCommandMap.put(DataAttribute.ACTUAL_POWER, new Iec61850ActualPowerCommand());
        this.rtuCommandMap.put(DataAttribute.TOTAL_ENERGY, new Iec61850LoadTotalEnergyCommand());
    }

    public static Iec61850LoadCommandFactory getInstance() {
        if (instance == null) {
            instance = new Iec61850LoadCommandFactory();
        }
        return instance;
    }

    @Override
    public RtuCommand getCommand(final MeasurementFilterDto filter) {
        return this.getCommand(DataAttribute.fromString(filter.getNode()));
    }

    @Override
    public RtuCommand getCommand(final String node) {
        return this.getCommand(DataAttribute.fromString(node));
    }

    private RtuCommand getCommand(final DataAttribute dataAttribute) {

        final RtuCommand command = this.rtuCommandMap.get(dataAttribute);

        if (command == null) {
            LOGGER.warn("No command found for data attribute {}", dataAttribute);
        }
        return command;
    }

}
