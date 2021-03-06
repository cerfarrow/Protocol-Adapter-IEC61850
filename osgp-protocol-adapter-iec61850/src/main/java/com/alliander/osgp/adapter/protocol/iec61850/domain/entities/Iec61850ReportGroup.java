/**
 * Copyright 2017 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.alliander.osgp.adapter.protocol.iec61850.domain.entities;

import com.alliander.osgp.shared.domain.entities.AbstractEntity;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "iec61850_report_group")
public class Iec61850ReportGroup extends AbstractEntity {

    /**
     * Serial Version UID.
     */
    private static final long serialVersionUID = 182081847594060732L;

    @Column(unique = true, nullable = false, length = 255)
    private String name;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(name = "iec61850_report_report_group", joinColumns = @JoinColumn(name = "report_group_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "report_id", referencedColumnName = "id"))
    private final Set<Iec61850Report> iec61850Reports = new HashSet<>();

    public Iec61850ReportGroup() {
        // Default constructor
    }

    @Override
    public String toString() {
        return String.format("Iec61850ReportGroup[name=%s]", this.name);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Iec61850ReportGroup)) {
            return false;
        }

        final Iec61850ReportGroup report = (Iec61850ReportGroup) o;

        return Objects.equals(this.name, report.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.name);
    }

    public String getName() {
        return this.name;
    }

    public Set<Iec61850Report> getIec61850Reports() {
        return this.iec61850Reports;
    }
}
