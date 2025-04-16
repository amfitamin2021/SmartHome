package com.example.Smarthome.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для передачи информации о доступных устройствах в ThingsBoard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableDeviceDto {
    
    private String id;
    private String name;
    private String type;
    private String token;
    private String manufacturer;
    private String model;
    private String firmwareVersion;
} 