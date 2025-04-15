package com.example.Smarthome.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO для отправки команды на устройство
 */
@Data
public class DeviceCommandRequest {
    private String command;
    private Map<String, String> parameters = new HashMap<>();
} 