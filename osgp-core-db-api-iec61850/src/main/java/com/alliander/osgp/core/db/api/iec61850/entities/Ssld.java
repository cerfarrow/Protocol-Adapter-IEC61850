/**
 * Copyright 2014-2016 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.alliander.osgp.core.db.api.iec61850.entities;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.PrimaryKeyJoinColumn;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alliander.osgp.core.db.api.iec61850valueobjects.RelayType;

/**
 * Copy of the platform Ssld class
 */
@Entity
@PrimaryKeyJoinColumn(name = "id")
public class Ssld extends Device {

    private static final Logger LOGGER = LoggerFactory.getLogger(Ssld.class);

    /**
     * Serial Version UID.
     */
    private static final long serialVersionUID = 1492214916633445439L;

    /**
     * Device type indicator for PSLD
     */
    public static final String PSLD_TYPE = "PSLD";

    /**
     * Device type indicator for SSLD
     */
    public static final String SSLD_TYPE = "SSLD";

    @LazyCollection(LazyCollectionOption.FALSE)
    @ElementCollection()
    @CollectionTable(name = "device_output_setting", joinColumns = @JoinColumn(name = "device_id"))
    private List<DeviceOutputSetting> outputSettings = new ArrayList<>();

    public Ssld() {
        // Default constructor.
    }

    public Ssld(final String deviceIdentification) {
        this.deviceIdentification = deviceIdentification;
    }

    public Ssld(final String deviceIdentification, final String deviceType, final InetAddress networkAddress,
            final boolean activated, final boolean hasSchedule) {
        this.deviceIdentification = deviceIdentification;
    }

    public Ssld(final String deviceIdentification, final String alias, final String containerCity,
            final String containerPostalCode, final String containerStreet, final String containerNumber,
            final String containerMunicipality, final Float latitude, final Float longitude) {
        this.deviceIdentification = deviceIdentification;
    }

    public List<DeviceOutputSetting> getOutputSettings() {
        if (this.outputSettings == null || this.outputSettings.isEmpty()) {
            return Collections.unmodifiableList(this.createDefaultConfiguration());
        }

        return Collections.unmodifiableList(this.outputSettings);
    }

    public void updateOutputSettings(final List<DeviceOutputSetting> outputSettings) {
        this.outputSettings = outputSettings;
    }

    public List<DeviceOutputSetting> receiveOutputSettings() {
        return this.outputSettings;
    }

    /*
     * Create default configuration for a device (based on type).
     * 
     * @return default configuration
     */
    private List<DeviceOutputSetting> createDefaultConfiguration() {
        final List<DeviceOutputSetting> defaultConfiguration = new ArrayList<>();

        if (this.deviceType == null) {
            LOGGER.warn("DeviceType is null, using empty list of DeviceOutputSetting");
            return defaultConfiguration;
        }

        if (this.deviceType.equalsIgnoreCase(SSLD_TYPE)) {
            defaultConfiguration.add(new DeviceOutputSetting(1, 1, RelayType.TARIFF, ""));
            defaultConfiguration.add(new DeviceOutputSetting(2, 2, RelayType.LIGHT, ""));
            defaultConfiguration.add(new DeviceOutputSetting(3, 3, RelayType.LIGHT, ""));
            defaultConfiguration.add(new DeviceOutputSetting(4, 4, RelayType.LIGHT, ""));

            LOGGER.warn("DeviceType is SSLD, returning default list of DeviceOutputSetting: 1 TARIFF, 2 & 3 & 4 LIGHT");
            return defaultConfiguration;
        }

        if (this.deviceType.equalsIgnoreCase(PSLD_TYPE)) {
            defaultConfiguration.add(new DeviceOutputSetting(1, 1, RelayType.LIGHT, ""));
            LOGGER.warn("DeviceType is PSLD, returning default list of DeviceOutputSetting: 1 LIGHT");
            return defaultConfiguration;
        }

        return defaultConfiguration;
    }
}
