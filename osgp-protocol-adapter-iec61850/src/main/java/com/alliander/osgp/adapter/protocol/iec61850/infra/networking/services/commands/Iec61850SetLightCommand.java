/**
 * Copyright 2014-2016 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.alliander.osgp.adapter.protocol.iec61850.infra.networking.services.commands;

import org.openmuc.openiec61850.BdaBoolean;
import org.openmuc.openiec61850.Fc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class Iec61850SetLightCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(Iec61850SetLightCommand.class);

    public Boolean switchLightRelay(final Iec61850Client iec61850Client, final DeviceConnection deviceConnection,
            final int index, final boolean on) throws ProtocolAdapterException {
        // Commands don't return anything, so returnType is Void.
        final Function<Boolean> function = new Function<Boolean>() {

            @Override
            public Boolean apply(final DeviceMessageLog deviceMessageLog) throws Exception {

                try {
                    final LogicalNode logicalNode = LogicalNode.getSwitchComponentByIndex(index);

                    // Check if CfSt.enbOper [CF] is set to true. If it is not
                    // set to true, the relay can not be operated.
                    final NodeContainer masterControl = deviceConnection.getFcModelNode(LogicalDevice.LIGHTING,
                            logicalNode, DataAttribute.MASTER_CONTROL, Fc.CF);
                    iec61850Client.readNodeDataValues(deviceConnection.getConnection().getClientAssociation(),
                            masterControl.getFcmodelNode());

                    final BdaBoolean enbOper = masterControl.getBoolean(SubDataAttribute.ENABLE_OPERATION);
                    if (enbOper.getValue()) {
                        LOGGER.info("masterControl.enbOper is true, switching of relay {} is enabled", index);
                    } else {
                        LOGGER.info("masterControl.enbOper is false, switching of relay {} is disabled", index);
                        // Set the value to true.
                        masterControl.writeBoolean(SubDataAttribute.ENABLE_OPERATION, true);
                        LOGGER.info("set masterControl.enbOper to true to enable switching of relay {}", index);

                        deviceMessageLog.addVariable(logicalNode, DataAttribute.MASTER_CONTROL, Fc.CF,
                                SubDataAttribute.ENABLE_OPERATION, true + "");
                    }

                    // Switch the relay using Pos.Oper.ctlVal [CO].
                    final NodeContainer position = deviceConnection.getFcModelNode(LogicalDevice.LIGHTING, logicalNode,
                            DataAttribute.POSITION, Fc.CO);
                    iec61850Client.readNodeDataValues(deviceConnection.getConnection().getClientAssociation(),
                            position.getFcmodelNode());

                    final NodeContainer operation = position.getChild(SubDataAttribute.OPERATION);

                    final BdaBoolean controlValue = operation.getBoolean(SubDataAttribute.CONTROL_VALUE);

                    LOGGER.info(String.format("Switching relay %d %s", index, on ? "on" : "off"));
                    controlValue.setValue(on);
                    operation.write();

                    deviceMessageLog.addVariable(logicalNode, DataAttribute.POSITION, Fc.CO,
                            SubDataAttribute.OPERATION, SubDataAttribute.CONTROL_VALUE, on + "");

                    DeviceMessageLoggingService.logMessage(deviceMessageLog,
                            deviceConnection.getDeviceIdentification(),
                            deviceConnection.getOrganisationIdentification(), false);

                    return true;
                } catch (final Exception e) {
                    LOGGER.error("Exception during switchLightRelay()", e);
                    return false;
                }
            }
        };

        return iec61850Client.sendCommandWithRetry(function, "SetLight", deviceConnection.getDeviceIdentification());
    }
}
