/**
 * Copyright 2017 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.alliander.osgp.adapter.protocol.iec61850.domain.repositories;

import com.alliander.osgp.adapter.protocol.iec61850.domain.entities.Iec61850DeviceReportGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Iec61850DeviceReportGroupRepository extends JpaRepository<Iec61850DeviceReportGroup, Long> {

    List<Iec61850DeviceReportGroup> findByDeviceIdentificationAndEnabled(String deviceIdentification, boolean enabled);

    Iec61850DeviceReportGroup findByDeviceIdentificationAndReportDataSet(String deviceIdentification, String reportDataSet);
}
