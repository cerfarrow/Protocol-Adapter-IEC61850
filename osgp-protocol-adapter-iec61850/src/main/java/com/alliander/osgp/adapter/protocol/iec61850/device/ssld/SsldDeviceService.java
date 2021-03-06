/**
 * Copyright 2014-2016 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.alliander.osgp.adapter.protocol.iec61850.device.ssld;

import javax.jms.JMSException;

import com.alliander.osgp.adapter.protocol.iec61850.device.DeviceMessageStatus;
import com.alliander.osgp.adapter.protocol.iec61850.device.DeviceRequest;
import com.alliander.osgp.adapter.protocol.iec61850.device.DeviceResponseHandler;
import com.alliander.osgp.adapter.protocol.iec61850.device.ssld.requests.GetPowerUsageHistoryDeviceRequest;
import com.alliander.osgp.adapter.protocol.iec61850.device.ssld.requests.SetConfigurationDeviceRequest;
import com.alliander.osgp.adapter.protocol.iec61850.device.ssld.requests.SetEventNotificationsDeviceRequest;
import com.alliander.osgp.adapter.protocol.iec61850.device.ssld.requests.SetLightDeviceRequest;
import com.alliander.osgp.adapter.protocol.iec61850.device.ssld.requests.SetScheduleDeviceRequest;
import com.alliander.osgp.adapter.protocol.iec61850.device.ssld.requests.SetTransitionDeviceRequest;
import com.alliander.osgp.adapter.protocol.iec61850.device.ssld.requests.UpdateDeviceSslCertificationDeviceRequest;
import com.alliander.osgp.adapter.protocol.iec61850.device.ssld.requests.UpdateFirmwareDeviceRequest;
import com.alliander.osgp.adapter.protocol.iec61850.device.ssld.responses.GetConfigurationDeviceResponse;
import com.alliander.osgp.adapter.protocol.iec61850.device.ssld.responses.GetFirmwareVersionDeviceResponse;
import com.alliander.osgp.adapter.protocol.iec61850.device.ssld.responses.GetPowerUsageHistoryDeviceResponse;
import com.alliander.osgp.adapter.protocol.iec61850.device.ssld.responses.GetStatusDeviceResponse;
import com.alliander.osgp.dto.valueobjects.EventNotificationTypeDto;

public interface SsldDeviceService {

    /**
     * Reads the {@link DeviceStatus} from the device.
     *
     * Returns a {@link GetStatusDeviceResponse} via the deviceResponseHandler's
     * callback.
     */
    void getStatus(DeviceRequest deviceRequest, final DeviceResponseHandler deviceResponseHandler) throws JMSException;

    /**
     * Reads the {@link PowerUsageData} from the device.
     *
     * Returns a {@link GetPowerUsageHistoryDeviceResponse} via the
     * deviceResponseHandler's callback.
     */
    void getPowerUsageHistory(GetPowerUsageHistoryDeviceRequest deviceRequest,
            DeviceResponseHandler deviceResponseHandler) throws JMSException;

    /**
     * Switches the given light relays on or off, depending on the given
     * {@link LightValue} list.
     *
     * Returns a {@link DeviceMessageStatus} via the deviceResponseHandler's
     * callback.
     */
    void setLight(SetLightDeviceRequest deviceRequest, DeviceResponseHandler deviceResponseHandler) throws JMSException;

    /**
     * Writes all the given {@link Configuration} data to the device. Ignores
     * all null values.
     *
     * Returns a {@link DeviceMessageStatus} via the deviceResponseHandler's
     * callback.
     */
    void setConfiguration(SetConfigurationDeviceRequest deviceRequest, DeviceResponseHandler deviceResponseHandler)
            throws JMSException;

    /**
     * Reads {@link Configuration} data from the device.
     *
     * Returns a {@link GetConfigurationDeviceResponse} via the
     * deviceResponseHandler's callback.
     */
    void getConfiguration(DeviceRequest deviceRequest, DeviceResponseHandler deviceResponseHandler) throws JMSException;

    /**
     * Signals the Device to reboot.
     *
     * Returns a {@link DeviceMessageStatus} via the deviceResponseHandler's
     * callback.
     */
    void setReboot(DeviceRequest deviceRequest, DeviceResponseHandler deviceResponseHandler) throws JMSException;

    /**
     * Runs a self-test by turning all light relays on or off, depending on
     * StartOfTest, then checking to see it they are all on/off.
     *
     * Returns a {@link DeviceMessageStatus} via the deviceResponseHandler's
     * callback.
     */
    void runSelfTest(DeviceRequest deviceRequest, DeviceResponseHandler deviceResponseHandler, boolean startOfTest)
            throws JMSException;

    /**
     * Writes the list {@link Schedule} entries to the device.
     *
     * Returns a {@link DeviceMessageStatus} via the deviceResponseHandler's
     * callback.
     */
    void setSchedule(SetScheduleDeviceRequest deviceRequest, DeviceResponseHandler deviceResponseHandler)
            throws JMSException;

    /**
     * Reads both the version of the functional and the security firmware.
     *
     * Returns a {@link GetFirmwareVersionDeviceResponse} via the
     * deviceResponseHandler's callback.
     */
    void getFirmwareVersion(DeviceRequest deviceRequest, DeviceResponseHandler deviceResponseHandler)
            throws JMSException;

    /**
     * Writes the {@link TransitionType} to the device.
     *
     * Returns a {@link DeviceMessageStatus} via the deviceResponseHandler's
     * callback.
     */
    void setTransition(SetTransitionDeviceRequest deviceRequest, DeviceResponseHandler deviceResponseHandler)
            throws JMSException;

    /**
     * Writes the download URL of the new firmware and the time it has to start
     * downloading to the Device.
     *
     * Returns a {@link DeviceMessageStatus} via the deviceResponseHandler's
     * callback.
     */
    void updateFirmware(UpdateFirmwareDeviceRequest deviceRequest, DeviceResponseHandler deviceResponseHandler)
            throws JMSException;

    /**
     * Writes the download URL of the new SSL certificate and the time it has to
     * start downloading to the Device.
     *
     * Returns a {@link DeviceMessageStatus} via the deviceResponseHandler's
     * callback.
     */
    void updateDeviceSslCertification(UpdateDeviceSslCertificationDeviceRequest deviceRequest,
            DeviceResponseHandler deviceResponseHandler) throws JMSException;

    /**
     * Writes the {@link EventNotificationTypeDto} list to the device.
     *
     * Returns a {@link DeviceMessageStatus} via the deviceResponseHandler's
     * callback.
     */
    void setEventNotifications(SetEventNotificationsDeviceRequest deviceRequest,
            DeviceResponseHandler deviceResponseHandler) throws JMSException;

}
