package com.alliander.osgp.adapter.protocol.iec61850.infra.networking.translation;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.DataAttribute;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.NodeContainer;
import com.alliander.osgp.adapter.protocol.iec61850.infra.networking.helper.SubDataAttribute;
import com.alliander.osgp.dto.valueobjects.microgrids.MeasurementDto;

public class Iec61850LmgcTranslator {

    public static MeasurementDto translateBehavior(final NodeContainer containingNode) {
        return new MeasurementDto(1, DataAttribute.BEHAVIOR.getDescription(), 0,
                new DateTime(containingNode.getDate(SubDataAttribute.TIME), DateTimeZone.UTC),
                containingNode.getByte(SubDataAttribute.STATE).getValue());
    }

    public static MeasurementDto translateHealth(final NodeContainer containingNode) {
        return new MeasurementDto(1, DataAttribute.HEALTH.getDescription(), 0,
                new DateTime(containingNode.getDate(SubDataAttribute.TIME), DateTimeZone.UTC),
                containingNode.getByte(SubDataAttribute.STATE).getValue());
    }

    // public static MeasurementDto translateIscso(final NodeContainer
    // containingNode) {
    // return new MeasurementDto(1,
    // DataAttribute.INTEGER_STATUS_CONTROLLABLE_STATUS_OUTPUT.getDescription(),
    // 0,
    // new DateTime(containingNode.getDate(SubDataAttribute.TIME),
    // DateTimeZone.UTC),
    // containingNode.getInteger(SubDataAttribute.STATE).getValue());
    // }
}
