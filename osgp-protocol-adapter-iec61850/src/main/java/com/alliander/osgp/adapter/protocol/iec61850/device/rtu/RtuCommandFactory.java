/**
 * Copyright 2014-2016 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.alliander.osgp.adapter.protocol.iec61850.device.rtu;

import com.alliander.osgp.dto.valueobjects.microgrids.MeasurementFilterDto;

public interface RtuCommandFactory {

    RtuCommand getCommand(final MeasurementFilterDto filter);

    RtuCommand getCommand(final String node);
}
