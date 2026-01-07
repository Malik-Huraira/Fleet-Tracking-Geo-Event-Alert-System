package com.geofleet.common.util;

import org.locationtech.jts.geom.*;

public class GeometryUtil {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private GeometryUtil() {
        // Utility class
    }

    public static Point createPoint(double longitude, double latitude) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
    }

    public static Polygon createPolygon(Coordinate[] coordinates) {
        return GEOMETRY_FACTORY.createPolygon(coordinates);
    }

    public static double calculateDistance(Point p1, Point p2) {
        // Haversine formula for distance in meters
        double lat1 = Math.toRadians(p1.getY());
        double lat2 = Math.toRadians(p2.getY());
        double lon1 = Math.toRadians(p1.getX());
        double lon2 = Math.toRadians(p2.getX());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return 6371000 * c; // Earth radius in meters
    }

    public static GeometryFactory getGeometryFactory() {
        return GEOMETRY_FACTORY;
    }
}
