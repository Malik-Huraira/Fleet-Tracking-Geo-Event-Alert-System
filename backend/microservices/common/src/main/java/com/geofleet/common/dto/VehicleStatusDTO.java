package com.geofleet.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleStatusDTO {
    @JsonProperty("vehicleId")
    private String vehicleId;

    @JsonProperty("latitude")
    private Double latitude;

    @JsonProperty("longitude")
    private Double longitude;

    @JsonProperty("speed")
    private Double speed;

    @JsonProperty("heading")
    private Double heading;

    @JsonProperty("status")
    private String status;

    @JsonProperty("lastUpdate")
    private LocalDateTime lastUpdate;

    @JsonProperty("region")
    private String region = "San Francisco";
}
