/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.acme.schooltimetabling.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.optaplanner.core.api.domain.lookup.PlanningId;

@Entity
public class Room {

    @PlanningId
    @Id
    @GeneratedValue
    private Long id;

    private String name;

    private Integer xLocation;
    private Integer yLocation;

    // No-arg constructor required for Hibernate
    public Room() {
    }

    public Room(String name) {
        this.name = name.trim();
    }

    public Room(String name, Integer xLocation, Integer yLocation) {
        this.name = name.trim();
        this.xLocation = xLocation;
        this.yLocation = yLocation;
    }

    public Room(long id, String name) {
        this(name);
        this.id = id;
    }

    @Override
    public String toString() {
        return name;
    }

    // ************************************************************************
    // Getters and setters
    // ************************************************************************

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getXLocation() {
        if(xLocation == null) return 0;
        return xLocation;
    }

    public void setXLocation(Integer xLocation) {
        this.xLocation = xLocation;
    }

    public Integer getYLocation() {
        if(yLocation == null) return 0;
        return yLocation;
    }

    public void setYLocation(Integer yLocation) {
        this.yLocation = yLocation;
    }

    public Long getDistance(Room room) {
        return Math.round(Math.sqrt( 
            Math.pow( room.getXLocation() - this.getXLocation() , 2 ) + 
            Math.pow( room.getYLocation() - this.getYLocation() , 2) ));
    } 
}
