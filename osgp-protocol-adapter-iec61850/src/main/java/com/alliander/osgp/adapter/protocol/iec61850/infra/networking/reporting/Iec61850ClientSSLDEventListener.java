/**
 * Copyright 2014-2016 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.alliander.osgp.adapter.protocol.iec61850.infra.networking.reporting;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.joda.time.DateTime;
import org.openmuc.openiec61850.BdaBoolean;
import org.openmuc.openiec61850.BdaInt8U;
import org.openmuc.openiec61850.BdaReasonForInclusion;
import org.openmuc.openiec61850.BdaTimestamp;
import org.openmuc.openiec61850.BdaVisibleString;
import org.openmuc.openiec61850.DataSet;
import org.openmuc.openiec61850.FcModelNode;
import org.openmuc.openiec61850.HexConverter;
import org.openmuc.openiec61850.Report;

import com.alliander.osgp.adapter.protocol.iec61850.application.services.DeviceManagementService;
import com.alliander.osgp.adapter.protocol.iec61850.domain.valueobjects.EventType;
import com.alliander.osgp.adapter.protocol.iec61850.exceptions.ProtocolAdapterException;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.services.Iec61850BdaOptFldsHelper;
import com.alliander.osgp.core.db.api.iec61850.entities.DeviceOutputSetting;
import com.alliander.osgp.dto.valueobjects.EventNotificationDto;
import com.alliander.osgp.dto.valueobjects.EventTypeDto;

public class Iec61850ClientSSLDEventListener extends Iec61850ClientBaseEventListener {

    /**
     * The EntryTime from IEC61850 has timestamp values relative to 01-01-1984.
     * TimeStamp values and Java date time values have milliseconds since
     * 01-01-1970. The milliseconds between these representations are in the
     * following offset.
     */
    private static final long IEC61850_ENTRY_TIME_OFFSET = 441763200000L;

    /*
     * Node names of EvnRpn nodes that occur as members of the report dataset.
     */
    private static final String EVENT_NODE_EVENT_TYPE = "evnType";
    private static final String EVENT_NODE_SWITCH_NUMBER = "swNum";
    private static final String EVENT_NODE_SWITCH_VALUE = "swVal";
    private static final String EVENT_NODE_TRIGGER_TIME = "trgTime";
    private static final String EVENT_NODE_TRIGGER_TYPE = "trgType";
    private static final String EVENT_NODE_REMARK = "remark";

    private static final Map<Short, String> TRG_TYPE_DESCRIPTION_PER_CODE = new TreeMap<>();

    private static final Comparator<EventNotificationDto> NOTIFICATIONS_BY_TIME = new Comparator<EventNotificationDto>() {
        @Override
        public int compare(final EventNotificationDto o1, final EventNotificationDto o2) {
            return o1.getDateTime().compareTo(o2.getDateTime());
        }
    };

    static {
        TRG_TYPE_DESCRIPTION_PER_CODE.put((short) 1, "light trigger (sensor trigger)");
        TRG_TYPE_DESCRIPTION_PER_CODE.put((short) 2, "ad-hoc trigger");
        TRG_TYPE_DESCRIPTION_PER_CODE.put((short) 3, "fixed time trigger");
        TRG_TYPE_DESCRIPTION_PER_CODE.put((short) 4, "autonomous trigger");
    }

    private final List<EventNotificationDto> eventNotifications = new ArrayList<>();
    private final Map<Integer, Integer> externalIndexByInternalIndex = new TreeMap<>();

    public Iec61850ClientSSLDEventListener(final String deviceIdentification,
            final DeviceManagementService deviceManagementService) throws ProtocolAdapterException {
        super(deviceIdentification, deviceManagementService, Iec61850ClientSSLDEventListener.class);
        this.externalIndexByInternalIndex
                .putAll(this.buildExternalByInternalIndexMap(this.deviceManagementService, this.deviceIdentification));
    }

    private Map<Integer, Integer> buildExternalByInternalIndexMap(final DeviceManagementService deviceManagementService,
            final String deviceIdentification) throws ProtocolAdapterException {

        final Map<Integer, Integer> indexMap = new TreeMap<>();
        indexMap.put(0, 0);

        final List<DeviceOutputSetting> deviceOutputSettings = deviceManagementService
                .getDeviceOutputSettings(deviceIdentification);
        for (final DeviceOutputSetting outputSetting : deviceOutputSettings) {
            indexMap.put(outputSetting.getInternalId(), outputSetting.getExternalId());
        }

        this.logger.info("Retrieved internal to external index map for device {}: {}", deviceIdentification, indexMap);

        return indexMap;
    }

    @Override
    public void newReport(final Report report) {

        final DateTime timeOfEntry = this.getTimeOfEntry(report);

        final String reportDescription = this.getReportDescription(report, timeOfEntry);

        this.logger.info("newReport for {}", reportDescription);
        boolean skipRecordBecauseOfOldSqNum = false;
        if (report.isBufOvfl()) {
            this.logger.warn("Buffer Overflow reported for {} - entries within the buffer may have been lost.",
                    reportDescription);
        } else if (this.firstNewSqNum != null && report.getSqNum() != null && report.getSqNum() < this.firstNewSqNum) {
            skipRecordBecauseOfOldSqNum = true;
        }
        this.logReportDetails(report);

        final DataSet dataSet = report.getDataSet();
        if (dataSet == null) {
            this.logger.warn("No DataSet available for {}", reportDescription);
            return;
        }
        final List<FcModelNode> members = dataSet.getMembers();
        if (members == null || members.isEmpty()) {
            this.logger.warn("No members in DataSet available for {}", reportDescription);
            return;
        } else {
            this.logger.debug("Handling {} DataSet members for {}", members.size(), reportDescription);
        }
        for (final FcModelNode member : members) {
            if (member == null) {
                this.logger.warn("Member == null in DataSet for {}", reportDescription);
                continue;
            }
            this.logger.info("Handle member {} for {}", member.getReference(), reportDescription);
            try {
                if (skipRecordBecauseOfOldSqNum) {
                    this.logger.warn(
                            "Skipping report because SqNum: {} is less than what should be the first new value: {}",
                            report.getSqNum(), this.firstNewSqNum);
                } else {
                    this.addEventNotificationForReportedData(member, timeOfEntry, reportDescription);
                }
            } catch (final Exception e) {
                this.logger.error("Error adding event notification for member {} from {}", member.getReference(),
                        reportDescription, e);
            }
        }
    }

    private DateTime getTimeOfEntry(final Report report) {
        return report.getTimeOfEntry() == null ? null
                : new DateTime(report.getTimeOfEntry().getTimestampValue() + IEC61850_ENTRY_TIME_OFFSET);
    }

    private String getReportDescription(final Report report, final DateTime timeOfEntry) {
        return String.format("device: %s, reportId: %s, timeOfEntry: %s, sqNum: %s%s%s", this.deviceIdentification,
                report.getRptId(), timeOfEntry == null ? "-" : timeOfEntry, report.getSqNum(),
                report.getSubSqNum() == null ? "" : " subSqNum: " + report.getSubSqNum(),
                report.isMoreSegmentsFollow() ? " (more segments follow for this sqNum)" : "");
    }

    private void addEventNotificationForReportedData(final FcModelNode evnRpn, final DateTime timeOfEntry,
            final String reportDescription) throws ProtocolAdapterException {

        final EventTypeDto eventType = this.determineEventType(evnRpn, reportDescription);
        final Integer index = this.determineRelayIndex(evnRpn, reportDescription);
        final String description = this.determineDescription(evnRpn);
        final DateTime dateTime = this.determineDateTime(evnRpn, timeOfEntry);

        final EventNotificationDto eventNotification = new EventNotificationDto(this.deviceIdentification, dateTime,
                eventType, description, index);
        synchronized (this.eventNotifications) {
            this.eventNotifications.add(eventNotification);
        }
    }

    private EventTypeDto determineEventType(final FcModelNode evnRpn, final String reportDescription) {

        final BdaInt8U evnTypeNode = (BdaInt8U) evnRpn.getChild(EVENT_NODE_EVENT_TYPE);
        if (evnTypeNode == null) {
            throw this.childNodeNotAvailableException(evnRpn, EVENT_NODE_EVENT_TYPE, reportDescription);
        }

        final short evnTypeCode = evnTypeNode.getValue();
        final EventType eventType = EventType.forCode(evnTypeCode);

        return eventType.getOsgpEventType();
    }

    private Integer determineRelayIndex(final FcModelNode evnRpn, final String reportDescription)
            throws ProtocolAdapterException {

        final BdaInt8U swNumNode = (BdaInt8U) evnRpn.getChild(EVENT_NODE_SWITCH_NUMBER);
        if (swNumNode == null) {
            throw this.childNodeNotAvailableException(evnRpn, EVENT_NODE_SWITCH_NUMBER, reportDescription);
        }

        final Short swNum = swNumNode.getValue();
        final Integer externalIndex = this.externalIndexByInternalIndex.get(swNum.intValue());
        if (externalIndex == null) {
            this.logger.error("No external index configured for internal index: {} for device: {}, using '0' for event",
                    swNum, this.deviceIdentification);
            return 0;
        }

        return externalIndex;
    }

    private String determineDescription(final FcModelNode evnRpn) {

        final StringBuilder sb = new StringBuilder();

        final BdaInt8U trgTypeNode = (BdaInt8U) evnRpn.getChild(EVENT_NODE_TRIGGER_TYPE);
        if (trgTypeNode != null && trgTypeNode.getValue() > 0) {
            final short trgType = trgTypeNode.getValue();
            final String trigger = TRG_TYPE_DESCRIPTION_PER_CODE.get(trgType);
            if (trigger == null) {
                sb.append("trgType=").append(trgType);
            } else {
                sb.append(trigger);
            }
        }

        final BdaInt8U evnTypeNode = (BdaInt8U) evnRpn.getChild(EVENT_NODE_EVENT_TYPE);
        if (evnTypeNode != null && evnTypeNode.getValue() > 0) {
            final short evnType = evnTypeNode.getValue();
            final String event = EventType.forCode(evnType).getDescription();
            if (event.startsWith("FUNCTION_FIRMWARE")) {
                if (sb.length() > 0) {
                    sb.append("; ");
                }
                sb.append("functional firmware");
            } else if (event.startsWith("SECURITY_FIRMWARE")) {
                if (sb.length() > 0) {
                    sb.append("; ");
                }
                sb.append("security firmware");
            }
        }

        final BdaVisibleString remarkNode = (BdaVisibleString) evnRpn.getChild(EVENT_NODE_REMARK);
        if (remarkNode != null && !EVENT_NODE_REMARK.equalsIgnoreCase(remarkNode.getStringValue())) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append('(').append(remarkNode.getStringValue()).append(')');
        }

        return sb.toString();
    }

    private DateTime determineDateTime(final FcModelNode evnRpn, final DateTime timeOfEntry) {

        final BdaTimestamp trgTimeNode = (BdaTimestamp) evnRpn.getChild(EVENT_NODE_TRIGGER_TIME);
        if (trgTimeNode != null && trgTimeNode.getDate() != null) {
            return new DateTime(trgTimeNode.getDate());
        }

        if (timeOfEntry != null) {
            /*
             * Use the reports time of entry for the event. The trigger time
             * will appear in the description with the event notification.
             *
             * See: determineDescription(FcModelNode)
             */
            return timeOfEntry;
        }

        /*
         * No time of entry or trigger time available for the report. As a
         * fallback use the time the report is processed here as event time.
         */
        return DateTime.now();
    }

    private IllegalArgumentException childNodeNotAvailableException(final FcModelNode evnRpn,
            final String childNodeName, final String reportDescription) {
        return new IllegalArgumentException("No '" + childNodeName + "' child in DataSet member "
                + evnRpn.getReference() + " from " + reportDescription);
    }

    private void logReportDetails(final Report report) {
        final StringBuilder sb = new StringBuilder("Report details for device ").append(this.deviceIdentification)
                .append(System.lineSeparator());
        sb.append("\t             RptId:\t").append(report.getRptId()).append(System.lineSeparator());
        sb.append("\t        DataSetRef:\t").append(report.getDataSetRef()).append(System.lineSeparator());
        sb.append("\t           ConfRev:\t").append(report.getConfRev()).append(System.lineSeparator());
        sb.append("\t           BufOvfl:\t").append(report.isBufOvfl()).append(System.lineSeparator());
        sb.append("\t           EntryId:\t").append(report.getEntryId()).append(System.lineSeparator());
        sb.append("\tInclusionBitString:\t").append(Arrays.toString(report.getInclusionBitString()))
                .append(System.lineSeparator());
        sb.append("\tMoreSegmentsFollow:\t").append(report.isMoreSegmentsFollow()).append(System.lineSeparator());
        sb.append("\t             SqNum:\t").append(report.getSqNum()).append(System.lineSeparator());
        sb.append("\t          SubSqNum:\t").append(report.getSubSqNum()).append(System.lineSeparator());
        sb.append("\t       TimeOfEntry:\t").append(report.getTimeOfEntry()).append(System.lineSeparator());
        if (report.getTimeOfEntry() != null) {
            sb.append("\t                   \t(")
                    .append(new DateTime(report.getTimeOfEntry().getTimestampValue() + IEC61850_ENTRY_TIME_OFFSET))
                    .append(')').append(System.lineSeparator());
        }
        final List<BdaReasonForInclusion> reasonCodes = report.getReasonCodes();
        if (reasonCodes != null && !reasonCodes.isEmpty()) {
            sb.append("\t       ReasonCodes:").append(System.lineSeparator());
            for (final BdaReasonForInclusion reasonCode : reasonCodes) {
                sb.append("\t                   \t")
                        .append(reasonCode.getReference() == null ? HexConverter.toHexString(reasonCode.getValue())
                                : reasonCode)
                        .append("\t(").append(new Iec61850BdaReasonForInclusionHelper(reasonCode).getInfo()).append(')')
                        .append(System.lineSeparator());
            }
        }
        sb.append("\t           optFlds:").append(report.getOptFlds()).append("\t(")
                .append(new Iec61850BdaOptFldsHelper(report.getOptFlds()).getInfo()).append(')')
                .append(System.lineSeparator());
        final DataSet dataSet = report.getDataSet();
        if (dataSet == null) {
            sb.append("\t           DataSet:\tnull").append(System.lineSeparator());
        } else {
            sb.append("\t           DataSet:\t").append(dataSet.getReferenceStr()).append(System.lineSeparator());
            final List<FcModelNode> members = dataSet.getMembers();
            if (members != null && !members.isEmpty()) {
                sb.append("\t   DataSet members:\t").append(members.size()).append(System.lineSeparator());
                for (final FcModelNode member : members) {
                    sb.append("\t            member:\t").append(member).append(System.lineSeparator());
                    if (member.getReference().toString().contains("CSLC.EvnRpn")) {
                        sb.append(this.evnRpnInfo("\t                   \t\t", member));
                    }
                }
            }
        }
        this.logger.info(sb.append(System.lineSeparator()).toString());
    }

    private String evnRpnInfo(final String linePrefix, final FcModelNode evnRpn) {
        final StringBuilder sb = new StringBuilder();

        final BdaInt8U evnTypeNode = (BdaInt8U) evnRpn.getChild(EVENT_NODE_EVENT_TYPE);
        sb.append(linePrefix).append(EVENT_NODE_EVENT_TYPE).append(": ");
        if (evnTypeNode == null) {
            sb.append("null");
        } else {
            final short evnType = evnTypeNode.getValue();
            sb.append(evnType).append(" = ").append(EventType.forCode(evnType).getDescription());
        }
        sb.append(System.lineSeparator());

        final BdaInt8U swNumNode = (BdaInt8U) evnRpn.getChild(EVENT_NODE_SWITCH_NUMBER);
        sb.append(linePrefix).append(EVENT_NODE_SWITCH_NUMBER).append(": ");
        if (swNumNode == null) {
            sb.append("null");
        } else {
            final short swNum = swNumNode.getValue();
            sb.append(swNum).append(" = ").append("get external index for switch " + swNum);
        }
        sb.append(System.lineSeparator());

        final BdaInt8U trgTypeNode = (BdaInt8U) evnRpn.getChild(EVENT_NODE_TRIGGER_TYPE);
        sb.append(linePrefix).append(EVENT_NODE_TRIGGER_TYPE).append(": ");
        if (trgTypeNode == null) {
            sb.append("null");
        } else {
            final short trgType = trgTypeNode.getValue();
            sb.append(trgType).append(" = ").append(TRG_TYPE_DESCRIPTION_PER_CODE.get(trgType));
        }
        sb.append(System.lineSeparator());

        final BdaBoolean swValNode = (BdaBoolean) evnRpn.getChild(EVENT_NODE_SWITCH_VALUE);
        sb.append(linePrefix).append(EVENT_NODE_SWITCH_VALUE).append(": ");
        if (swValNode == null) {
            sb.append("null");
        } else {
            final boolean swVal = swValNode.getValue();
            sb.append(swVal).append(" = ").append(swVal ? "ON" : "OFF");
        }
        sb.append(System.lineSeparator());

        final BdaTimestamp trgTimeNode = (BdaTimestamp) evnRpn.getChild(EVENT_NODE_TRIGGER_TIME);
        sb.append(linePrefix).append(EVENT_NODE_TRIGGER_TIME).append(": ");
        if (trgTimeNode == null || trgTimeNode.getDate() == null) {
            sb.append("null");
        } else {
            final DateTime trgTime = new DateTime(trgTimeNode.getDate());
            sb.append(trgTime);
        }
        sb.append(System.lineSeparator());

        final BdaVisibleString remarkNode = (BdaVisibleString) evnRpn.getChild(EVENT_NODE_REMARK);
        sb.append(linePrefix).append(EVENT_NODE_REMARK).append(": ");
        if (remarkNode == null) {
            sb.append("null");
        } else {
            final String remark = remarkNode.getStringValue();
            sb.append(remark);
        }
        sb.append(System.lineSeparator());

        return sb.toString();
    }

    @Override
    public void associationClosed(final IOException e) {
        this.logger.info("associationClosed for device: {}, {}", this.deviceIdentification,
                e == null ? "no IOException" : "IOException: " + e.getMessage());

        synchronized (this.eventNotifications) {
            if (this.eventNotifications.isEmpty()) {
                this.logger.info("No event notifications received from device: {}", this.deviceIdentification);
                return;
            }
            Collections.sort(this.eventNotifications, NOTIFICATIONS_BY_TIME);
            try {
                this.deviceManagementService.addEventNotifications(this.deviceIdentification, this.eventNotifications);
            } catch (final ProtocolAdapterException pae) {
                this.logger.error("Error adding device notifications for device: " + this.deviceIdentification, pae);
            }
        }
    }
}
