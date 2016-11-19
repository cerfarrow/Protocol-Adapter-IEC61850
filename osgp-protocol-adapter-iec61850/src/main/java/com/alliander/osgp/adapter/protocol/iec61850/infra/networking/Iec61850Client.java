/**
 * Copyright 2014-2016 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.alliander.osgp.adapter.protocol.iec61850.infra.networking;

import java.io.IOException;
import java.net.InetAddress;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.openmuc.openiec61850.ClientAssociation;
import org.openmuc.openiec61850.ClientSap;
import org.openmuc.openiec61850.FcModelNode;
import org.openmuc.openiec61850.SclParseException;
import org.openmuc.openiec61850.ServerModel;
import org.openmuc.openiec61850.ServiceError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alliander.osgp.adapter.protocol.iec61850.exceptions.ConnectionFailureException;
import com.alliander.osgp.adapter.protocol.iec61850.exceptions.NodeReadException;
import com.alliander.osgp.adapter.protocol.iec61850.exceptions.NodeWriteException;
import com.alliander.osgp.adapter.protocol.iec61850.exceptions.ProtocolAdapterException;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.ConnectionState;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.Function;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.reporting.Iec61850ClientBaseEventListener;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.reporting.Iec61850ClientEventListenerFactory;

@Component
public class Iec61850Client {

    private static final Logger LOGGER = LoggerFactory.getLogger(Iec61850Client.class);

    @Autowired
    private int iec61850PortClient;

    @Autowired
    private int iec61850PortClientLocal;

    @Autowired
    private int iec61850SsldPortServer;

    @Autowired
    private int iec61850RtuPortServer;

    @Autowired
    private int maxRedeliveriesForIec61850Requests;

    @Autowired
    private int maxRetryCount;

    @PostConstruct
    private void init() {
        LOGGER.info(
                "portClient: {}, portClientLocal: {}, iec61850SsldPortServer: {}, iec61850RtuPortServer: {}, maxRetryCount: {}, maxRedeliveriesForIec61850Requests: {}",
                this.iec61850PortClient, this.iec61850PortClientLocal, this.iec61850SsldPortServer,
                this.iec61850RtuPortServer, this.maxRetryCount, this.maxRedeliveriesForIec61850Requests);
    }

    /**
     * Connect to a given device. This will try to establish the
     * {@link ClientAssociation} between client and IED.
     *
     * @param deviceIdentification
     *            The device identification.
     * @param ipAddress
     *            The IP address of the device.
     * @param reportListener
     *            The report listener instance which can be created using
     *            {@link Iec61850ClientEventListenerFactory}.
     * @param port
     *            The port number of the IED.
     *
     * @return An {@link Iec61850ClientAssociation} instance.
     *
     * @throws ConnectionFailureException
     *             In case the connection to the device could not be
     *             established.
     */
    public Iec61850ClientAssociation connect(final String deviceIdentification, final InetAddress ipAddress,
            final Iec61850ClientBaseEventListener reportListener, final int port) throws ConnectionFailureException {
        // Alternatively you could use ClientSap(SocketFactory factory) to e.g.
        // connect using SSL.
        final ClientSap clientSap = new ClientSap();
        final Iec61850ClientAssociation clientAssociation;
        LOGGER.info("Attempting to connect to server: {} on port: {}, max redelivery count: {} and max retry count: {}",
                ipAddress.getHostAddress(), port, this.maxRedeliveriesForIec61850Requests, this.maxRetryCount);

        try {
            final ClientAssociation association = clientSap.associate(ipAddress, port, null, reportListener);
            clientAssociation = new Iec61850ClientAssociation(association, reportListener);
        } catch (final IOException e) {
            // An IOException will always indicate a fatal exception. It
            // indicates that the association was closed and
            // cannot be recovered. You will need to create a new association
            // using ClientSap.associate() in order to
            // reconnect.
            LOGGER.error("Error connecting to device: " + deviceIdentification, e);
            throw new ConnectionFailureException(e.getMessage(), e);
        }

        LOGGER.info("Connected to device: {}", deviceIdentification);
        return clientAssociation;
    }

    /**
     * Disconnect from the device.
     *
     * @param clientAssociation
     *            The {@link ClientAssociation} instance.
     * @param deviceIdentification
     *            The device identification.
     */
    public void disconnect(final ClientAssociation clientAssociation, final String deviceIdentification) {
        LOGGER.info("disconnecting from device: {}...", deviceIdentification);
        clientAssociation.disconnect();
        LOGGER.info("disconnected from device: {}", deviceIdentification);
    }

    /**
     * Read the device model from the device.
     *
     * @param clientAssociation
     *            The {@link ClientAssociation} instance.
     *
     * @return A {@link ServerModel} instance.
     */
    public ServerModel readServerModelFromDevice(final ClientAssociation clientAssociation) {
        ServerModel serverModel;
        try {
            LOGGER.debug("Start reading server model from device");
            // RetrieveModel() will call all GetDirectory and GetDefinition ACSI
            // services needed to get the complete server model.
            serverModel = clientAssociation.retrieveModel();
            LOGGER.debug("Completed reading server model from device");
            return serverModel;
        } catch (final ServiceError e) {
            LOGGER.error("Service Error requesting model.", e);
            clientAssociation.close();
            return null;
        } catch (final IOException e) {
            LOGGER.error("Fatal IOException requesting model.", e);
            return null;
        }
    }

    /**
     * Use an ICD file (model file) to read the device model.
     *
     * @param clientAssociation
     *            Instance of {@link ClientAssociation}
     * @param filePath
     *            "../sampleServer/sampleModel.icd"
     *
     * @return Instance of {@link ServerModel}
     *
     * @throws ProtocolAdapterException
     *             In case the file path is empty.
     */
    public ServerModel readServerModelFromSclFile(final ClientAssociation clientAssociation, final String filePath)
            throws ProtocolAdapterException {
        if (StringUtils.isEmpty(filePath)) {
            throw new ProtocolAdapterException("File path is empty");
        }

        // Instead of calling retrieveModel you could read the model directly
        // from an SCL file.
        try {
            return clientAssociation.getModelFromSclFile(filePath);
        } catch (final SclParseException e) {
            LOGGER.error("Error parsing SCL file: " + filePath, e);
            return null;
        }
    }

    /**
     * Read the values of all data attributes of all data objects of all Logical
     * Nodes.
     *
     * @param clientAssociation
     *            An {@link ClientAssociation} instance.
     *
     * @throws NodeReadException
     *             In case the read action fails.
     */
    public void readAllDataValues(final ClientAssociation clientAssociation) throws NodeReadException {
        try {
            LOGGER.debug("Start getAllDataValues from device");
            clientAssociation.getAllDataValues();
            LOGGER.debug("Completed getAllDataValues from device");
        } catch (final ServiceError e) {
            LOGGER.error("ServiceError during readAllDataValues", e);
            throw new NodeReadException(e.getMessage(), e, ConnectionState.OK);
        } catch (final IOException e) {
            LOGGER.error("IOException during readAllDataValues", e);
            throw new NodeReadException(e.getMessage(), e, ConnectionState.BROKEN);
        }
    }

    /**
     * Read the values of all data attributes of a data object of a Logical
     * Node.
     *
     * @param clientAssociation
     *            An {@link ClientAssociation} instance.
     * @param modelNode
     *            The {@link FcModelNode} to read.
     *
     * @throws NodeReadException
     *             In case the read action fails.
     */
    public void readNodeDataValues(final ClientAssociation clientAssociation, final FcModelNode modelNode)
            throws NodeReadException {
        try {
            clientAssociation.getDataValues(modelNode);
        } catch (final ServiceError e) {
            LOGGER.error("ServiceError during readNodeDataValues", e);
            throw new NodeReadException(e.getMessage(), e, ConnectionState.OK);
        } catch (final IOException e) {
            LOGGER.error("IOException during readNodeDataValues", e);
            throw new NodeReadException(e.getMessage(), e, ConnectionState.BROKEN);
        }
    }

    /**
     * Executes the apply method of the given {@link Function} with retries.
     *
     * @return The given T.
     */
    public <T> T sendCommandWithRetry(final Function<T> function, final String deviceIdentification)
            throws ProtocolAdapterException {
        T output = null;

        try {
            output = function.apply();
        } catch (final NodeWriteException | NodeReadException e) {
            if (ConnectionState.OK.equals(e.getConnectionState())) {
                // ServiceError means we have to retry.
                LOGGER.error("Caught ServiceError, retrying", e);
                this.sendCommandWithRetry(function, deviceIdentification, 1);
            } else {
                LOGGER.error("Caught IOException, connection with device is broken.", e);
            }
        } catch (final ConnectionFailureException e) {
            throw e;
        } catch (final Exception e) {
            throw new ProtocolAdapterException(e == null ? "Could not execute command" : e.getMessage(), e);
        }

        return output;
    }

    /**
     * Basically the same as sendCommandWithRetry, but with a retry parameter.
     */
    private <T> T sendCommandWithRetry(final Function<T> function, final String deviceIdentification,
            final int retryCount) throws ProtocolAdapterException {

        T output = null;

        LOGGER.info("retry: {} of {} for deviceIdentification: {}", retryCount, this.maxRetryCount,
                deviceIdentification);

        try {
            output = function.apply();
        } catch (final ProtocolAdapterException e) {
            if (retryCount >= this.maxRetryCount) {
                throw e;
            } else {
                this.sendCommandWithRetry(function, deviceIdentification, retryCount + 1);
            }
        } catch (final Exception e) {
            throw new ProtocolAdapterException(e == null ? "Could not execute command" : e.getMessage(), e);
        }

        return output;
    }
}
