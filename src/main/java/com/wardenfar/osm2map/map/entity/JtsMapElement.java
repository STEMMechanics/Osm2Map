package com.wardenfar.osm2map.map.entity;

import org.locationtech.jts.geom.Geometry;

public class JtsMapElement {

    public long id;
    public String type;
    public Geometry geometry;

    public JtsMapElement(long id, String type, Geometry geometry) {
        this.id = id;
        this.type = type;
        this.geometry = geometry;
    }
}
