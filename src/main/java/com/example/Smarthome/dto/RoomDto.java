package com.example.Smarthome.dto;

import lombok.Data;

import java.util.UUID;

/**
 * DTO для передачи информации о комнате через API
 */
@Data
public class RoomDto {
    private UUID id;
    private String name;
    private String floor;
    private Double area;
    private UUID locationId;
    private String locationName;
    private long deviceCount;
} 