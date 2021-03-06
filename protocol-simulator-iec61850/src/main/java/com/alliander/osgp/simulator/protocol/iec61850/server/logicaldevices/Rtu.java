/**
 * Copyright 2014-2016 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.alliander.osgp.simulator.protocol.iec61850.server.logicaldevices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.openmuc.openiec61850.BasicDataAttribute;
import org.openmuc.openiec61850.Fc;
import org.openmuc.openiec61850.ServerModel;

import com.alliander.osgp.simulator.protocol.iec61850.server.QualityType;

public class Rtu extends LogicalDevice {

    private static final String DSCH1_SCHDID_SETVAL = "DSCH1.SchdId.setVal";
    private static final String DSCH1_SCHDTYP_SETVAL = "DSCH1.SchdTyp.setVal";
    private static final String DSCH1_SCHDCAT_SETVAL = "DSCH1.SchCat.setVal";
    private static final String DSCH1_SCHDABSTM_VAL_0 = "DSCH1.SchdAbsTm.val.0";
    private static final String DSCH1_SCHDABSTM_TIME_0 = "DSCH1.SchdAbsTm.time.0";
    private static final String DSCH1_SCHDABSTM_VAL_1 = "DSCH1.SchdAbsTm.val.1";
    private static final String DSCH1_SCHDABSTM_TIME_1 = "DSCH1.SchdAbsTm.time.1";
    private static final String DSCH1_SCHDABSTM_VAL_2 = "DSCH1.SchdAbsTm.val.2";
    private static final String DSCH1_SCHDABSTM_TIME_2 = "DSCH1.SchdAbsTm.time.2";
    private static final String DSCH1_SCHDABSTM_VAL_3 = "DSCH1.SchdAbsTm.val.3";
    private static final String DSCH1_SCHDABSTM_TIME_3 = "DSCH1.SchdAbsTm.time.3";

    private static final String DSCH2_SCHDID_SETVAL = "DSCH2.SchdId.setVal";
    private static final String DSCH2_SCHDTYP_SETVAL = "DSCH2.SchdTyp.setVal";
    private static final String DSCH2_SCHDCAT_SETVAL = "DSCH2.SchCat.setVal";
    private static final String DSCH2_SCHDABSTM_VAL_0 = "DSCH2.SchdAbsTm.val.0";
    private static final String DSCH2_SCHDABSTM_TIME_0 = "DSCH2.SchdAbsTm.time.0";
    private static final String DSCH2_SCHDABSTM_VAL_1 = "DSCH2.SchdAbsTm.val.1";
    private static final String DSCH2_SCHDABSTM_TIME_1 = "DSCH2.SchdAbsTm.time.1";
    private static final String DSCH2_SCHDABSTM_VAL_2 = "DSCH2.SchdAbsTm.val.2";
    private static final String DSCH2_SCHDABSTM_TIME_2 = "DSCH2.SchdAbsTm.time.2";
    private static final String DSCH2_SCHDABSTM_VAL_3 = "DSCH2.SchdAbsTm.val.3";
    private static final String DSCH2_SCHDABSTM_TIME_3 = "DSCH2.SchdAbsTm.time.3";

    private static final String DSCH3_SCHDID_SETVAL = "DSCH3.SchdId.setVal";
    private static final String DSCH3_SCHDTYP_SETVAL = "DSCH3.SchdTyp.setVal";
    private static final String DSCH3_SCHDCAT_SETVAL = "DSCH3.SchCat.setVal";
    private static final String DSCH3_SCHDABSTM_VAL_0 = "DSCH3.SchdAbsTm.val.0";
    private static final String DSCH3_SCHDABSTM_TIME_0 = "DSCH3.SchdAbsTm.time.0";
    private static final String DSCH3_SCHDABSTM_VAL_1 = "DSCH3.SchdAbsTm.val.1";
    private static final String DSCH3_SCHDABSTM_TIME_1 = "DSCH3.SchdAbsTm.time.1";
    private static final String DSCH3_SCHDABSTM_VAL_2 = "DSCH3.SchdAbsTm.val.2";
    private static final String DSCH3_SCHDABSTM_TIME_2 = "DSCH3.SchdAbsTm.time.2";
    private static final String DSCH3_SCHDABSTM_VAL_3 = "DSCH3.SchdAbsTm.val.3";
    private static final String DSCH3_SCHDABSTM_TIME_3 = "DSCH3.SchdAbsTm.time.3";

    private static final String DSCH4_SCHDID_SETVAL = "DSCH4.SchdId.setVal";
    private static final String DSCH4_SCHDTYP_SETVAL = "DSCH4.SchdTyp.setVal";
    private static final String DSCH4_SCHDCAT_SETVAL = "DSCH4.SchCat.setVal";
    private static final String DSCH4_SCHDABSTM_VAL_0 = "DSCH4.SchdAbsTm.val.0";
    private static final String DSCH4_SCHDABSTM_TIME_0 = "DSCH4.SchdAbsTm.time.0";
    private static final String DSCH4_SCHDABSTM_VAL_1 = "DSCH4.SchdAbsTm.val.1";
    private static final String DSCH4_SCHDABSTM_TIME_1 = "DSCH4.SchdAbsTm.time.1";
    private static final String DSCH4_SCHDABSTM_VAL_2 = "DSCH4.SchdAbsTm.val.2";
    private static final String DSCH4_SCHDABSTM_TIME_2 = "DSCH4.SchdAbsTm.time.2";
    private static final String DSCH4_SCHDABSTM_VAL_3 = "DSCH4.SchdAbsTm.val.3";
    private static final String DSCH4_SCHDABSTM_TIME_3 = "DSCH4.SchdAbsTm.time.3";

    private static final Set<String> FLOAT32_NODES = Collections.unmodifiableSet(new TreeSet<>(
            Arrays.asList(DSCH1_SCHDABSTM_VAL_0, DSCH1_SCHDABSTM_VAL_1, DSCH1_SCHDABSTM_VAL_2, DSCH1_SCHDABSTM_VAL_3,
                    DSCH2_SCHDABSTM_VAL_0, DSCH2_SCHDABSTM_VAL_1, DSCH2_SCHDABSTM_VAL_2, DSCH2_SCHDABSTM_VAL_3,
                    DSCH3_SCHDABSTM_VAL_0, DSCH3_SCHDABSTM_VAL_1, DSCH3_SCHDABSTM_VAL_2, DSCH3_SCHDABSTM_VAL_3)));

    private static final Set<String> INT32_NODES = Collections.unmodifiableSet(new TreeSet<>(
            Arrays.asList(DSCH1_SCHDID_SETVAL, DSCH1_SCHDTYP_SETVAL, DSCH1_SCHDCAT_SETVAL, DSCH2_SCHDID_SETVAL,
                    DSCH2_SCHDTYP_SETVAL, DSCH2_SCHDCAT_SETVAL, DSCH3_SCHDID_SETVAL, DSCH3_SCHDTYP_SETVAL,
                    DSCH3_SCHDCAT_SETVAL, DSCH4_SCHDID_SETVAL, DSCH4_SCHDTYP_SETVAL, DSCH4_SCHDCAT_SETVAL)));

    private static final Set<String> TIMESTAMP_NODES = Collections
            .unmodifiableSet(new TreeSet<>(Arrays.asList(DSCH1_SCHDABSTM_TIME_0, DSCH1_SCHDABSTM_TIME_1,
                    DSCH1_SCHDABSTM_TIME_2, DSCH1_SCHDABSTM_TIME_3, DSCH2_SCHDABSTM_TIME_0, DSCH2_SCHDABSTM_TIME_1,
                    DSCH2_SCHDABSTM_TIME_2, DSCH2_SCHDABSTM_TIME_3, DSCH3_SCHDABSTM_TIME_0, DSCH3_SCHDABSTM_TIME_1,
                    DSCH3_SCHDABSTM_TIME_2, DSCH3_SCHDABSTM_TIME_3, DSCH4_SCHDABSTM_TIME_0, DSCH4_SCHDABSTM_TIME_1,
                    DSCH4_SCHDABSTM_TIME_2, DSCH4_SCHDABSTM_TIME_3)));

    private static final Map<String, Fc> FC_BY_NODE;
    static {
        final Map<String, Fc> fcByNode = new TreeMap<>();

        fcByNode.put(DSCH1_SCHDID_SETVAL, Fc.SP);
        fcByNode.put(DSCH1_SCHDTYP_SETVAL, Fc.SP);
        fcByNode.put(DSCH1_SCHDCAT_SETVAL, Fc.SP);
        fcByNode.put(DSCH1_SCHDABSTM_VAL_0, Fc.SP);
        fcByNode.put(DSCH1_SCHDABSTM_TIME_0, Fc.SP);
        fcByNode.put(DSCH1_SCHDABSTM_VAL_1, Fc.SP);
        fcByNode.put(DSCH1_SCHDABSTM_TIME_1, Fc.SP);
        fcByNode.put(DSCH1_SCHDABSTM_VAL_2, Fc.SP);
        fcByNode.put(DSCH1_SCHDABSTM_TIME_2, Fc.SP);
        fcByNode.put(DSCH1_SCHDABSTM_VAL_3, Fc.SP);
        fcByNode.put(DSCH1_SCHDABSTM_TIME_3, Fc.SP);

        fcByNode.put(DSCH2_SCHDID_SETVAL, Fc.SP);
        fcByNode.put(DSCH2_SCHDTYP_SETVAL, Fc.SP);
        fcByNode.put(DSCH2_SCHDCAT_SETVAL, Fc.SP);
        fcByNode.put(DSCH2_SCHDABSTM_VAL_0, Fc.SP);
        fcByNode.put(DSCH2_SCHDABSTM_TIME_0, Fc.SP);
        fcByNode.put(DSCH2_SCHDABSTM_VAL_1, Fc.SP);
        fcByNode.put(DSCH2_SCHDABSTM_TIME_1, Fc.SP);
        fcByNode.put(DSCH2_SCHDABSTM_VAL_2, Fc.SP);
        fcByNode.put(DSCH2_SCHDABSTM_TIME_2, Fc.SP);
        fcByNode.put(DSCH2_SCHDABSTM_VAL_3, Fc.SP);
        fcByNode.put(DSCH2_SCHDABSTM_TIME_3, Fc.SP);

        fcByNode.put(DSCH3_SCHDID_SETVAL, Fc.SP);
        fcByNode.put(DSCH3_SCHDTYP_SETVAL, Fc.SP);
        fcByNode.put(DSCH3_SCHDCAT_SETVAL, Fc.SP);
        fcByNode.put(DSCH3_SCHDABSTM_VAL_0, Fc.SP);
        fcByNode.put(DSCH3_SCHDABSTM_TIME_0, Fc.SP);
        fcByNode.put(DSCH3_SCHDABSTM_VAL_1, Fc.SP);
        fcByNode.put(DSCH3_SCHDABSTM_TIME_1, Fc.SP);
        fcByNode.put(DSCH3_SCHDABSTM_VAL_2, Fc.SP);
        fcByNode.put(DSCH3_SCHDABSTM_TIME_2, Fc.SP);
        fcByNode.put(DSCH3_SCHDABSTM_VAL_3, Fc.SP);
        fcByNode.put(DSCH3_SCHDABSTM_TIME_3, Fc.SP);

        fcByNode.put(DSCH4_SCHDID_SETVAL, Fc.SP);
        fcByNode.put(DSCH4_SCHDTYP_SETVAL, Fc.SP);
        fcByNode.put(DSCH4_SCHDCAT_SETVAL, Fc.SP);
        fcByNode.put(DSCH4_SCHDABSTM_VAL_0, Fc.SP);
        fcByNode.put(DSCH4_SCHDABSTM_TIME_0, Fc.SP);
        fcByNode.put(DSCH4_SCHDABSTM_VAL_1, Fc.SP);
        fcByNode.put(DSCH4_SCHDABSTM_TIME_1, Fc.SP);
        fcByNode.put(DSCH4_SCHDABSTM_VAL_2, Fc.SP);
        fcByNode.put(DSCH4_SCHDABSTM_TIME_2, Fc.SP);
        fcByNode.put(DSCH4_SCHDABSTM_VAL_3, Fc.SP);
        fcByNode.put(DSCH4_SCHDABSTM_TIME_3, Fc.SP);

        FC_BY_NODE = Collections.unmodifiableMap(fcByNode);
    }

    public Rtu(final String physicalDeviceName, final String logicalDeviceName, final ServerModel serverModel) {
        super(physicalDeviceName, logicalDeviceName, serverModel);
    }

    @Override
    public List<BasicDataAttribute> getAttributesAndSetValues(final Date timestamp) {
        final List<BasicDataAttribute> values = new ArrayList<>();

        values.add(this.setRandomByte("LLN0.Health.stVal", Fc.ST, 1, 2));
        values.add(this.setQuality("LLN0.Health.q", Fc.ST, QualityType.VALIDITY_GOOD.getValue()));
        values.add(this.setTime("LLN0.Health.t", Fc.ST, timestamp));

        values.add(this.setRandomByte("LLN0.Beh.stVal", Fc.ST, 1, 2));
        values.add(this.setQuality("LLN0.Beh.q", Fc.ST, QualityType.VALIDITY_GOOD.getValue()));
        values.add(this.setTime("LLN0.Beh.t", Fc.ST, timestamp));

        values.add(this.setRandomByte("LLN0.Mod.stVal", Fc.ST, 1, 2));
        values.add(this.setQuality("LLN0.Mod.q", Fc.ST, QualityType.VALIDITY_GOOD.getValue()));
        values.add(this.setTime("LLN0.Mod.t", Fc.ST, timestamp));

        values.add(this.setBoolean("GGIO1.Alm1.stVal", Fc.ST, false));
        values.add(this.setQuality("GGIO1.Alm1.q", Fc.ST, QualityType.VALIDITY_GOOD.getValue()));
        values.add(this.setTime("GGIO1.Alm1.t", Fc.ST, timestamp));

        values.add(this.setBoolean("GGIO1.Alm2.stVal", Fc.ST, false));
        values.add(this.setQuality("GGIO1.Alm2.q", Fc.ST, QualityType.VALIDITY_GOOD.getValue()));
        values.add(this.setTime("GGIO1.Alm2.t", Fc.ST, timestamp));

        values.add(this.setBoolean("GGIO1.Alm3.stVal", Fc.ST, false));
        values.add(this.setQuality("GGIO1.Alm3.q", Fc.ST, QualityType.VALIDITY_GOOD.getValue()));
        values.add(this.setTime("GGIO1.Alm3.t", Fc.ST, timestamp));

        values.add(this.setBoolean("GGIO1.Alm4.stVal", Fc.ST, false));
        values.add(this.setQuality("GGIO1.Alm4.q", Fc.ST, QualityType.VALIDITY_GOOD.getValue()));
        values.add(this.setTime("GGIO1.Alm4.t", Fc.ST, timestamp));

        values.add(this.setRandomInt("GGIO1.IntIn1.stVal", Fc.ST, 1, 100));
        values.add(this.setQuality("GGIO1.IntIn1.q", Fc.ST, QualityType.VALIDITY_GOOD.getValue()));
        values.add(this.setTime("GGIO1.IntIn1.t", Fc.ST, timestamp));

        values.add(this.setBoolean("GGIO1.Wrn1.stVal", Fc.ST, false));
        values.add(this.setQuality("GGIO1.Wrn1.q", Fc.ST, QualityType.VALIDITY_GOOD.getValue()));
        values.add(this.setTime("GGIO1.Wrn1.t", Fc.ST, timestamp));

        values.add(this.setBoolean("GGIO1.Wrn2.stVal", Fc.ST, false));
        values.add(this.setQuality("GGIO1.Wrn2.q", Fc.ST, QualityType.VALIDITY_GOOD.getValue()));
        values.add(this.setTime("GGIO1.Wrn2.t", Fc.ST, timestamp));

        values.add(this.setBoolean("GGIO1.Wrn3.stVal", Fc.ST, false));
        values.add(this.setQuality("GGIO1.Wrn3.q", Fc.ST, QualityType.VALIDITY_GOOD.getValue()));
        values.add(this.setTime("GGIO1.Wrn3.t", Fc.ST, timestamp));

        values.add(this.setBoolean("GGIO1.Wrn4.stVal", Fc.ST, false));
        values.add(this.setQuality("GGIO1.Wrn4.q", Fc.ST, QualityType.VALIDITY_GOOD.getValue()));
        values.add(this.setTime("GGIO1.Wrn4.t", Fc.ST, timestamp));

        values.add(this.setRandomInt("GGIO1.IntIn2.stVal", Fc.ST, 1, 100));
        values.add(this.setQuality("GGIO1.IntIn2.q", Fc.ST, QualityType.VALIDITY_GOOD.getValue()));
        values.add(this.setTime("GGIO1.IntIn2.t", Fc.ST, timestamp));

        values.add(this.setRandomInt(DSCH1_SCHDID_SETVAL, Fc.SP, 1, 100));
        values.add(this.setRandomInt(DSCH1_SCHDTYP_SETVAL, Fc.SP, 1, 100));
        values.add(this.setRandomInt(DSCH1_SCHDCAT_SETVAL, Fc.SP, 1, 100));
        values.add(this.setRandomFloat(DSCH1_SCHDABSTM_VAL_0, Fc.SP, 0, 1000));
        values.add(this.setTime(DSCH1_SCHDABSTM_TIME_0, Fc.SP, timestamp));
        values.add(this.setRandomFloat(DSCH1_SCHDABSTM_VAL_1, Fc.SP, 0, 1000));
        values.add(this.setTime(DSCH1_SCHDABSTM_TIME_1, Fc.SP, timestamp));
        values.add(this.setRandomFloat(DSCH1_SCHDABSTM_VAL_2, Fc.SP, 0, 1000));
        values.add(this.setTime(DSCH1_SCHDABSTM_TIME_2, Fc.SP, timestamp));
        values.add(this.setRandomFloat(DSCH1_SCHDABSTM_VAL_3, Fc.SP, 0, 1000));
        values.add(this.setTime(DSCH1_SCHDABSTM_TIME_3, Fc.SP, timestamp));

        values.add(this.setRandomInt(DSCH2_SCHDID_SETVAL, Fc.SP, 1, 100));
        values.add(this.setRandomInt(DSCH2_SCHDTYP_SETVAL, Fc.SP, 1, 100));
        values.add(this.setRandomInt(DSCH2_SCHDCAT_SETVAL, Fc.SP, 1, 100));
        values.add(this.setRandomFloat(DSCH2_SCHDABSTM_VAL_0, Fc.SP, 0, 1000));
        values.add(this.setTime(DSCH2_SCHDABSTM_TIME_0, Fc.SP, timestamp));
        values.add(this.setRandomFloat(DSCH2_SCHDABSTM_VAL_1, Fc.SP, 0, 1000));
        values.add(this.setTime(DSCH2_SCHDABSTM_TIME_1, Fc.SP, timestamp));
        values.add(this.setRandomFloat(DSCH2_SCHDABSTM_VAL_2, Fc.SP, 0, 1000));
        values.add(this.setTime(DSCH2_SCHDABSTM_TIME_2, Fc.SP, timestamp));
        values.add(this.setRandomFloat(DSCH2_SCHDABSTM_VAL_3, Fc.SP, 0, 1000));
        values.add(this.setTime(DSCH2_SCHDABSTM_TIME_3, Fc.SP, timestamp));

        values.add(this.setRandomInt(DSCH3_SCHDID_SETVAL, Fc.SP, 1, 100));
        values.add(this.setRandomInt(DSCH3_SCHDTYP_SETVAL, Fc.SP, 1, 100));
        values.add(this.setRandomInt(DSCH3_SCHDCAT_SETVAL, Fc.SP, 1, 100));
        values.add(this.setRandomFloat(DSCH3_SCHDABSTM_VAL_0, Fc.SP, 0, 1000));
        values.add(this.setTime(DSCH3_SCHDABSTM_TIME_0, Fc.SP, timestamp));
        values.add(this.setRandomFloat(DSCH3_SCHDABSTM_VAL_1, Fc.SP, 0, 1000));
        values.add(this.setTime(DSCH3_SCHDABSTM_TIME_1, Fc.SP, timestamp));
        values.add(this.setRandomFloat(DSCH3_SCHDABSTM_VAL_2, Fc.SP, 0, 1000));
        values.add(this.setTime(DSCH3_SCHDABSTM_TIME_2, Fc.SP, timestamp));
        values.add(this.setRandomFloat(DSCH3_SCHDABSTM_VAL_3, Fc.SP, 0, 1000));
        values.add(this.setTime(DSCH3_SCHDABSTM_TIME_3, Fc.SP, timestamp));

        values.add(this.setRandomInt(DSCH4_SCHDID_SETVAL, Fc.SP, 1, 100));
        values.add(this.setRandomInt(DSCH4_SCHDTYP_SETVAL, Fc.SP, 1, 100));
        values.add(this.setRandomInt(DSCH4_SCHDCAT_SETVAL, Fc.SP, 1, 100));
        values.add(this.setRandomFloat(DSCH4_SCHDABSTM_VAL_0, Fc.SP, 0, 1000));
        values.add(this.setTime(DSCH4_SCHDABSTM_TIME_0, Fc.SP, timestamp));
        values.add(this.setRandomFloat(DSCH4_SCHDABSTM_VAL_1, Fc.SP, 0, 1000));
        values.add(this.setTime(DSCH4_SCHDABSTM_TIME_1, Fc.SP, timestamp));
        values.add(this.setRandomFloat(DSCH4_SCHDABSTM_VAL_2, Fc.SP, 0, 1000));
        values.add(this.setTime(DSCH4_SCHDABSTM_TIME_2, Fc.SP, timestamp));
        values.add(this.setRandomFloat(DSCH4_SCHDABSTM_VAL_3, Fc.SP, 0, 1000));
        values.add(this.setTime(DSCH4_SCHDABSTM_TIME_3, Fc.SP, timestamp));

        return values;
    }

    @Override
    public BasicDataAttribute getAttributeAndSetValue(final String node, final String value) {
        final Fc fc = this.getFunctionalConstraint(node);
        if (fc == null) {
            throw this.illegalNodeException(node);
        }

        if (FLOAT32_NODES.contains(node)) {
            return this.setFixedFloat(node, fc, Float.parseFloat(value));
        }

        if (INT32_NODES.contains(node)) {
            return this.setInt(node, fc, Integer.parseInt(value));
        }

        if (TIMESTAMP_NODES.contains(node)) {
            return this.setTime(node, fc, this.parseDate(value));
        }

        throw this.nodeTypeNotConfiguredException(node);
    }

    @Override
    public Fc getFunctionalConstraint(final String node) {
        return FC_BY_NODE.get(node);
    }
}
