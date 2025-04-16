package com.example.Smarthome.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * DTO для передачи информации об устройстве через API
 */
@Data
public class DeviceDto {
    private UUID id;
    private String name;
    private String type;
    private String category;    // Категория устройства
    private String subType;     // Подтип устройства
    private String protocol;
    private String status;
    private String connectionParams;
    private LocalDateTime lastSeen;
    private Map<String, String> properties = new HashMap<>();
    private Map<String, String> capabilities = new HashMap<>();
    private String manufacturer;
    private String model;
    private String firmwareVersion;
    private UUID roomId;
    private String roomName;
    private UUID locationId;
    private String locationName;
    private String thingsboardToken;
    private String thingsboardId;   // ID устройства в ThingsBoard
    private Map<String, Object> attributes = new HashMap<>();
} 