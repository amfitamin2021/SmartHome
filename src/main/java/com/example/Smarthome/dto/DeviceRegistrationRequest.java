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
    private String protocol;
    private String connectionParams;
    private String manufacturer;
    private String model;
    private String firmwareVersion;
    private String thingsboardToken;
    private UUID roomId;
    private UUID locationId;
    private Map<String, String> initialProperties = new HashMap<>();
    private Map<String, String> capabilities = new HashMap<>();
} 