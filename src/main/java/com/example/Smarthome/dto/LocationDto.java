package com.example.Smarthome.dto;

import lombok.Data;

import java.util.UUID;

/**
 * DTO для передачи информации о локации через API
 */
@Data
public class LocationDto {
    private UUID id;
    private String name;
    private String address;
    private Double latitude;
    private Double longitude;
    private long roomCount;
    private long deviceCount;
} 