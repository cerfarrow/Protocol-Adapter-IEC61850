/**
 * Copyright 2016 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.alliander.osgp.adapter.protocol.iec61850.infra.networking.reporting;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.openmuc.openiec61850.ClientAssociation;
import org.openmuc.openiec61850.Rcb;
import org.openmuc.openiec61850.ServerModel;
import org.openmuc.openiec61850.ServiceError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alliander.osgp.adapter.protocol.iec61850.domain.entities.Iec61850Device;
import com.alliander.osgp.adapter.protocol.iec61850.domain.entities.Iec61850DeviceReportGroup;
import com.alliander.osgp.adapter.protocol.iec61850.domain.entities.Iec61850Report;
import com.alliander.osgp.adapter.protocol.iec61850.domain.entities.Iec61850ReportGroup;
import com.alliander.osgp.adapter.protocol.iec61850.domain.repositories.Iec61850DeviceReportGroupRepository;
import com.alliander.osgp.adapter.protocol.iec61850.domain.repositories.Iec61850DeviceRepository;
import com.alliander.osgp.adapter.protocol.iec61850.exceptions.NodeWriteException;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.DeviceConnection;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.IED;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.NodeContainer;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.SubDataAttribute;

@Service
public class Iec61850RtuDeviceReportingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(Iec61850RtuDeviceReportingService.class);

    @Autowired
    private Iec61850DeviceRepository iec61850DeviceRepository;

    @Autowired
    private Iec61850DeviceReportGroupRepository iec61850DeviceReportRepository;

    public void enableReportingForDevice(final DeviceConnection connection, final String deviceIdentification,
            final String serverName) {
        if (connection.getConnection().getIed() != null && IED.FLEX_OVL.equals(connection.getConnection().getIed())) {
            // We don't need 'enableReportingForDevice()' logic for FLEX_OVL
            // devices.
            return;
        }

        try {
            final Iec61850Device device = this.iec61850DeviceRepository
                    .findByDeviceIdentification(deviceIdentification);

            if (device.isEnableAllReportsOnConnect()) {
                this.enableAllReports(connection, deviceIdentification);
            } else {
                this.enableSpecificReports(connection, deviceIdentification, serverName);
            }
        } catch (final NullPointerException npe) {
            LOGGER.error(
                    "Caught null pointer exception, is Iec61850Device.enableAllReportsOnConnect not set in database?",
                    npe);
        } catch (final Exception e) {
            LOGGER.error("Caught unexpected exception", e);
        }
    }

    private void enableAllReports(final DeviceConnection connection, final String deviceIdentification) {
        final ServerModel serverModel = connection.getConnection().getServerModel();

        this.enableReports(connection, deviceIdentification, serverModel.getBrcbs());
        this.enableReports(connection, deviceIdentification, serverModel.getUrcbs());
    }

    private void enableReports(final DeviceConnection connection, final String deviceIdentification,
            final Collection<? extends Rcb> reports) {
        for (final Rcb report : reports) {
            final String reportReference = report.getReference().toString();
            try {
                LOGGER.info("Enable reporting for report {} on device {}.", reportReference, deviceIdentification);
                final NodeContainer node = new NodeContainer(connection, report);
                node.writeBoolean(SubDataAttribute.ENABLE_REPORTING, true);
            } catch (final NullPointerException e) {
                LOGGER.debug("NullPointerException", e);
                LOGGER.warn("Skip enable reporting for report {} on device {}.", reportReference, deviceIdentification);
            } catch (final NodeWriteException e) {
                LOGGER.debug("NodeWriteException", e);
                LOGGER.error("Enable reporting for report {} on device {}, failed with exception: {}", reportReference,
                        deviceIdentification, e.getMessage());
            }
        }
    }

    private void enableSpecificReports(final DeviceConnection connection, final String deviceIdentification,
            final String serverName) {

        final ServerModel serverModel = connection.getConnection().getServerModel();
        final ClientAssociation clientAssociation = connection.getConnection().getClientAssociation();

        final List<Iec61850DeviceReportGroup> deviceReportGroups = this.iec61850DeviceReportRepository
                .findByDeviceIdentificationAndEnabled(deviceIdentification, true);
        for (final Iec61850DeviceReportGroup deviceReportGroup : deviceReportGroups) {
            this.enableReportGroup(serverName, deviceIdentification, deviceReportGroup.getIec61850ReportGroup(),
                    serverModel, clientAssociation);
        }
    }

    private void enableReportGroup(final String serverName, final String deviceIdentification,
            final Iec61850ReportGroup reportGroup, final ServerModel serverModel,
            final ClientAssociation clientAssociation) {
        for (final Iec61850Report iec61850Report : reportGroup.getIec61850Reports()) {
            this.enableReport(serverName, deviceIdentification, iec61850Report, serverModel, clientAssociation);
        }
    }

    private void enableReport(final String serverName, final String deviceIdentification,
            final Iec61850Report iec61850Report, final ServerModel serverModel,
            final ClientAssociation clientAssociation) {
        int i = 1;
        Rcb rcb = this.getRcb(serverModel,
                this.getReportNode(serverName, iec61850Report.getLogicalDevice(), i, iec61850Report.getLogicalNode()));
        while (rcb != null) {
            this.enableRcb(deviceIdentification, clientAssociation, rcb);
            i += 1;
            rcb = this.getRcb(serverModel, this.getReportNode(serverName, iec61850Report.getLogicalDevice(), i,
                    iec61850Report.getLogicalNode()));
        }
    }

    private String getReportNode(final String serverName, final String logicalDevice, final int index,
            final String reportNode) {
        return serverName + logicalDevice + index + "/" + reportNode;
    }

    private Rcb getRcb(final ServerModel serverModel, final String node) {
        Rcb rcb = serverModel.getBrcb(node);
        if (rcb == null) {
            rcb = serverModel.getUrcb(node);
        }
        return rcb;
    }

    private void enableRcb(final String deviceIdentification, final ClientAssociation clientAssociation,
            final Rcb rcb) {
        try {
            clientAssociation.enableReporting(rcb);
        } catch (final IOException e) {
            LOGGER.error("IOException: unable to enable reporting for deviceIdentification " + deviceIdentification, e);
        } catch (final ServiceError e) {
            LOGGER.error("ServiceError: unable to enable reporting for deviceIdentification " + deviceIdentification,
                    e);
        }
    }

}
