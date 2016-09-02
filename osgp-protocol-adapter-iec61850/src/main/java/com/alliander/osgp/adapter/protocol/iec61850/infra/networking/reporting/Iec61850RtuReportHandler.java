package com.alliander.osgp.adapter.protocol.iec61850.infra.networking.reporting;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alliander.osgp.adapter.protocol.iec61850.device.rtu.RtuCommand;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.ReadOnlyNodeContainer;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.services.Iec61850RtuCommandFactory;
import com.alliander.osgp.dto.valueobjects.microgrids.MeasurementDto;
import com.alliander.osgp.dto.valueobjects.microgrids.MeasurementResultSystemIdentifierDto;

public class Iec61850RtuReportHandler implements Iec61850ReportHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Iec61850RtuReportHandler.class);

    private static final String SYSTEM_TYPE = "RTU";

    private int systemId;

    public Iec61850RtuReportHandler(final int systemId) {
        this.systemId = systemId;
    }

    @Override
    public MeasurementResultSystemIdentifierDto createResult(final List<MeasurementDto> measurements) {
        final MeasurementResultSystemIdentifierDto systemResult = new MeasurementResultSystemIdentifierDto(
                this.systemId, SYSTEM_TYPE, measurements);
        final List<MeasurementResultSystemIdentifierDto> systems = new ArrayList<>();
        systems.add(systemResult);
        return systemResult;
    }

    @Override
    public MeasurementDto handleMember(final ReadOnlyNodeContainer member) {

        final RtuCommand command = Iec61850RtuCommandFactory.getInstance()
                .getCommand(member.getFcmodelNode().getName());

        if (command == null) {
            LOGGER.warn("No command found for node {}", member.getFcmodelNode().getName());
            return null;
        } else {
            return command.translate(member);
        }
    }
}
