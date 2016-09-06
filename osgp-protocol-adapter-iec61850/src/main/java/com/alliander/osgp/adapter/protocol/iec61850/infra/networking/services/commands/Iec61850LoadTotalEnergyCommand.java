/**
 * Copyright 2014-2016 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.alliander.osgp.adapter.protocol.iec61850.infra.networking.services.commands;

import org.joda.time.DateTime;
import org.openmuc.openiec61850.Fc;

import com.alliander.osgp.adapter.protocol.iec61850.device.rtu.RtuCommand;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.Iec61850Client;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.DataAttribute;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.DeviceConnection;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.LogicalDevice;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.LogicalNode;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.NodeContainer;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.SubDataAttribute;
import com.alliander.osgp.dto.valueobjects.microgrids.MeasurementDto;

public class Iec61850LoadTotalEnergyCommand implements RtuCommand {

    @Override
    public MeasurementDto execute(final Iec61850Client client, final DeviceConnection connection,
            final LogicalDevice logicalDevice) {
        final NodeContainer containingNode = connection.getFcModelNode(logicalDevice, LogicalNode.METER_READING_ONE,
                DataAttribute.TOTAL_ENERGY, Fc.MX);
        client.readNodeDataValues(connection.getConnection().getClientAssociation(), containingNode.getFcmodelNode());
        return this.translate(containingNode);
    }

    @Override
    public MeasurementDto translate(final NodeContainer containingNode) {
        return new MeasurementDto(1, DataAttribute.TOTAL_ENERGY.getDescription(), 0,
                // TODO - Substitute value does not contain a datetime, like for
                // example Status fields, Determine if local datetime or null
                // should be used...
                new DateTime(),
                containingNode.getChild(SubDataAttribute.ACTUAL_VALUE).getFloat(SubDataAttribute.FLOAT).getFloat());
    }
}
