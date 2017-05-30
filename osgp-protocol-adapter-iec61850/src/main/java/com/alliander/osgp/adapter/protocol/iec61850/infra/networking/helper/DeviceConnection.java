/**
 * Copyright 2014-2016 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper;

import org.openmuc.openiec61850.Fc;
import org.openmuc.openiec61850.FcModelNode;
import org.openmuc.openiec61850.ObjectReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.Iec61850Connection;

public class DeviceConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceConnection.class);

    private final String serverName;
    private final Iec61850Connection connection;
    private final String deviceIdentification;
    private final String organisationIdentification;

    public static final String LOGICAL_NODE_SEPARATOR = "/";
    public static final String DATA_ATTRIBUTE_SEPARATOR = ".";

    public DeviceConnection(final Iec61850Connection connection, final String deviceIdentification,
            final String organisationIdentification, final String serverName) {
        this.connection = connection;
        this.deviceIdentification = deviceIdentification;
        this.organisationIdentification = organisationIdentification;
        this.serverName = serverName;
    }

    /**
     * Returns a {@link NodeContainer} for the given {@link ObjectReference}
     * data and the Functional constraint.
     */
    public NodeContainer getFcModelNode(final LogicalDevice logicalDevice, final LogicalNode logicalNode,
            final DataAttribute dataAttribute, final Fc fc) {
        final FcModelNode fcModelNode = (FcModelNode) this.connection.getServerModel().findModelNode(
                this.createObjectReference(logicalDevice, logicalNode, dataAttribute), fc);
        if (fcModelNode == null) {
            LOGGER.error("FcModelNode is null, most likely the data attribute: {} does not exist",
                    dataAttribute.getDescription());
        }

        return new NodeContainer(this, fcModelNode);
    }

    /**
     * Returns a {@link NodeContainer} for the given {@link ObjectReference}
     * data and the Functional constraint.
     */
    public NodeContainer getFcModelNode(final LogicalDevice logicalDevice, final int logicalDeviceIndex,
            final LogicalNode logicalNode, final DataAttribute dataAttribute, final Fc fc) {
        final FcModelNode fcModelNode = (FcModelNode) this.connection.getServerModel().findModelNode(
                this.createObjectReference(logicalDevice, logicalDeviceIndex, logicalNode, dataAttribute), fc);
        if (fcModelNode == null) {
            LOGGER.error("FcModelNode is null, most likely the data attribute: {} does not exist",
                    dataAttribute.getDescription());
        }

        return new NodeContainer(this, fcModelNode);
    }

    /**
     * Creates a correct ObjectReference.
     */
    private ObjectReference createObjectReference(final LogicalDevice logicalDevice, final LogicalNode logicalNode,
            final DataAttribute dataAttribute) {
        final String logicalDevicePrefix = this.serverName + logicalDevice.getDescription();

        final String objectReference = logicalDevicePrefix.concat(LOGICAL_NODE_SEPARATOR)
                .concat(logicalNode.getDescription()).concat(DATA_ATTRIBUTE_SEPARATOR)
                .concat(dataAttribute.getDescription());

        LOGGER.info("Device: {}, ObjectReference: {}", this.deviceIdentification, objectReference);

        return new ObjectReference(objectReference);
    }

    /**
     * Creates a correct ObjectReference.
     */
    private ObjectReference createObjectReference(final LogicalDevice logicalDevice, final int logicalDeviceIndex,
            final LogicalNode logicalNode, final DataAttribute dataAttribute) {
        final String logicalDevicePrefix = this.serverName + logicalDevice.getDescription() + logicalDeviceIndex;

        final String objectReference = logicalDevicePrefix.concat(LOGICAL_NODE_SEPARATOR)
                .concat(logicalNode.getDescription()).concat(DATA_ATTRIBUTE_SEPARATOR)
                .concat(dataAttribute.getDescription());

        LOGGER.info("Device: {}, ObjectReference: {}", this.deviceIdentification, objectReference);

        return new ObjectReference(objectReference);
    }

    // GETTERS AND SETTERS

    public String getDeviceIdentification() {
        return this.deviceIdentification;
    }

    public String getOrganisationIdentification() {
        return this.organisationIdentification;
    }

    public Iec61850Connection getConnection() {
        return this.connection;
    }
}
