package com.example.Smarthome.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * DTO для регистрации нового устройства или обновления существующего
 */
@Data
public class DeviceRegistrationRequest {
    private String name;
    private String type;
    private String category;    // Категория устройства (LIGHTING, SECURITY, CLIMATE, etc.)
    private String subType;     // Подтип устройства (SMART_BULB, CAMERA, THERMOSTAT, etc.)
    private String protocol;
    private String connectionParams;
    private String manufacturer;
    private String model;
    private String firmwareVersion;
    private String thingsboardToken;
    private String thingsboardId;    // ID устройства в ThingsBoard
    private UUID roomId;
    private UUID locationId;
    private Map<String, String> initialProperties = new HashMap<>();
    private Map<String, String> capabilities = new HashMap<>();
} 