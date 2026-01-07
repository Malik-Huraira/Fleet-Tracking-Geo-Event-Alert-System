package com.geofleet.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertEventDTO {
    @JsonProperty("vehicleId")
    private String vehicleId;

    @JsonProperty("alertType")
    private String alertType;

    @JsonProperty("message")
    private String message;

    // Keep both field names for compatibility
    @JsonProperty("latitude")
    private Double latitude;

    @JsonProperty("longitude")
    private Double longitude;

    // Also expose as lat/lng for frontend compatibility
    @JsonProperty("lat")
    public Double getLat() {
        return latitude;
    }

    @JsonProperty("lng")
    public Double getLng() {
        return longitude;
    }

    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime timestamp;

    @JsonProperty("severity")
    private String severity;
}
