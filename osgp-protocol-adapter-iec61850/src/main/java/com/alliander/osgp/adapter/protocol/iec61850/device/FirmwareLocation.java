/**
 * Copyright 2014-2016 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.alliander.osgp.adapter.protocol.iec61850.device;

import org.apache.commons.lang3.StringUtils;

/**
 * Value object containing information on firmware location.
 */
public class FirmwareLocation {

    private String protocol;
    private String domain;
    private int port;
    private String path;
    private String fileExtension;

    /**
     * Create a new instance of {@link FirmwareLocation}.
     *
     * @param protocol
     *            the protocol to be used in the firmware URL (e.g. http, https
     *            or ftp).
     * @param domain
     *            the domain on which the firmware is located.
     * @param port
     *            the port number to be used in the firmware URL.
     * @param path
     *            the path on which the firmware is located.
     * @param fileExtension
     *            the file extension of the firmware.
     */
    public FirmwareLocation(final String protocol, final String domain, final int port, final String path,
            final String fileExtension) {
        if (StringUtils.isBlank(domain)) {
            throw new IllegalArgumentException("Domain is empty or null.");
        }

        this.protocol = cleanUpProtocol(protocol);
        this.domain = cleanUpDomain(domain);
        this.port = port;
        this.path = cleanUpPath(path);
        this.fileExtension = cleanUpFileExtension(fileExtension);
    }

    /**
     * Gets the domain on which the firmware is located.
     *
     * @return the domain on which the firmware is located.
     */
    public String getDomain() {
        return this.protocol + "://" + this.domain + ":" + this.port;
    }

    /**
     * Gets the full path on which the firmware is located.
     *
     * @return the full path on which the firmware is located.
     */
    public String getFullPath(final String firmwareIdentification) {
        final StringBuilder pathBuilder = new StringBuilder();

        if (!StringUtils.isBlank(this.path)) {
            pathBuilder.append("/");
            pathBuilder.append(this.path);
        }

        pathBuilder.append("/");
        pathBuilder.append(firmwareIdentification);

        if (!StringUtils.isBlank(this.fileExtension)) {
            pathBuilder.append(".");
            pathBuilder.append(this.fileExtension);
        }

        return pathBuilder.toString();
    }

    private static String cleanUpProtocol(final String protocol) {
        String cleanProtocol = protocol;
        if (protocol.contains(":")) {
            cleanProtocol = protocol.substring(0, protocol.indexOf(':'));
        }
        return cleanProtocol.trim().toLowerCase();
    }

    private static String cleanUpDomain(final String domain) {
        String cleanDomain = domain;

        while (cleanDomain.endsWith("/")) {
            cleanDomain = cleanDomain.substring(0, cleanDomain.length() - 1);
        }

        if (cleanDomain.contains("://")) {
            cleanDomain = cleanDomain.substring(cleanDomain.lastIndexOf("://") + 3);
        }

        return cleanDomain;
    }

    private static String cleanUpPath(final String path) {
        String cleanPath = path;

        while (cleanPath.endsWith("/")) {
            cleanPath = cleanPath.substring(0, cleanPath.length() - 1);
        }

        while (cleanPath.startsWith("/")) {
            cleanPath = cleanPath.substring(1);
        }

        return cleanPath;
    }

    private static String cleanUpFileExtension(final String fileExtension) {
        String cleanFileExtension = fileExtension;

        if (cleanFileExtension.startsWith(".")) {
            cleanFileExtension = cleanFileExtension.substring(1);
        }

        return cleanFileExtension;
    }
}
