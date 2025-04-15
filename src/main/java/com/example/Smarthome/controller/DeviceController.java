package com.example.Smarthome.controller;

import com.example.Smarthome.dto.DeviceCommandRequest;
import com.example.Smarthome.dto.DeviceDto;
import com.example.Smarthome.dto.DeviceRegistrationRequest;
import com.example.Smarthome.model.ConnectionProtocol;
import com.example.Smarthome.model.Device;
import com.example.Smarthome.model.DeviceStatus;
import com.example.Smarthome.model.Location;
import com.example.Smarthome.service.DeviceService;
import com.example.Smarthome.service.LocationService;
import com.example.Smarthome.service.ProtocolAdapterService;
import com.example.Smarthome.service.ThingsBoardIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.HashMap;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@Slf4j
public class DeviceController {

    private final DeviceService deviceService;
    private final ProtocolAdapterService protocolAdapterService;
    private final ThingsBoardIntegrationService thingsBoardService;
    private final LocationService locationService;

    /**
     * Получение списка всех устройств
     */
    @GetMapping
    public ResponseEntity<List<DeviceDto>> getAllDevices() {
        List<Device> devices = deviceService.getAllDevices();
        List<DeviceDto> deviceDtos = devices.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(deviceDtos);
    }

    /**
     * Получение информации об устройстве по ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<DeviceDto> getDevice(@PathVariable UUID id) {
        Device device = deviceService.getDeviceById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Устройство с ID " + id + " не найдено"));
        
        // Получаем актуальные свойства
        Map<String, String> currentProperties = protocolAdapterService.getDeviceProperties(device);
        if (!currentProperties.isEmpty()) {
            // Обновляем свойства в устройстве
            for (Map.Entry<String, String> entry : currentProperties.entrySet()) {
                device.getProperties().put(entry.getKey(), entry.getValue());
            }
            deviceService.saveDevice(device);
        }
        
        return ResponseEntity.ok(convertToDto(device));
    }

    /**
     * Регистрация нового устройства
     */
    @PostMapping
    public ResponseEntity<DeviceDto> registerDevice(@RequestBody DeviceRegistrationRequest request) {
        Device device = new Device();
        device.setName(request.getName());
        device.setType(request.getType());
        device.setProtocol(ConnectionProtocol.valueOf(request.getProtocol()));
        device.setConnectionParams(request.getConnectionParams());
        device.setManufacturer(request.getManufacturer());
        device.setModel(request.getModel());
        device.setFirmwareVersion(request.getFirmwareVersion());
        device.setThingsboardToken(request.getThingsboardToken());
        device.setStatus(DeviceStatus.OFFLINE);
        
        // Устанавливаем связь с локацией
        if (request.getLocationId() != null) {
            // Получаем локацию из репозитория
            Location location = locationService.getLocationById(request.getLocationId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                            "Локация с ID " + request.getLocationId() + " не найдена"));
            device.setLocation(location);
        }
        
        // Если это виртуальное устройство, сразу устанавливаем статус ONLINE
        if (device.getProtocol() == ConnectionProtocol.VIRTUAL) {
            device.setStatus(DeviceStatus.ONLINE);
            device.setLastSeen(LocalDateTime.now());
        }
        
        // Сначала сохраняем устройство
        Device savedDevice = deviceService.saveDevice(device);
        
        // Создаем устройство в ThingsBoard, если не указан токен
        if (savedDevice.getThingsboardToken() == null || savedDevice.getThingsboardToken().isEmpty()) {
            boolean tbCreated = thingsBoardService.createDevice(savedDevice);
            
            if (tbCreated) {
                // Обновляем устройство с новым токеном
                savedDevice = deviceService.saveDevice(savedDevice);
                log.info("Устройство {} создано в ThingsBoard", savedDevice.getName());
            } else {
                log.warn("Не удалось создать устройство {} в ThingsBoard", savedDevice.getName());
            }
        } else {
            // Обновляем атрибуты в ThingsBoard
            thingsBoardService.updateDeviceAttributes(savedDevice);
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDto(savedDevice));
    }

    /**
     * Обновление информации устройства
     */
    @PutMapping("/{id}")
    public ResponseEntity<DeviceDto> updateDevice(@PathVariable UUID id, 
                                                  @RequestBody DeviceRegistrationRequest request) {
        Device device = deviceService.getDeviceById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Устройство с ID " + id + " не найдено"));

        device.setName(request.getName());
        device.setType(request.getType());
        device.setProtocol(ConnectionProtocol.valueOf(request.getProtocol()));
        device.setConnectionParams(request.getConnectionParams());
        device.setManufacturer(request.getManufacturer());
        device.setModel(request.getModel());
        device.setFirmwareVersion(request.getFirmwareVersion());
        
        // Обновляем связь с локацией
        if (request.getLocationId() != null) {
            Location location = locationService.getLocationById(request.getLocationId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                            "Локация с ID " + request.getLocationId() + " не найдена"));
            device.setLocation(location);
        }
        
        // Если токен был обновлен, обновляем его
        if (request.getThingsboardToken() != null) {
            device.setThingsboardToken(request.getThingsboardToken());
        }
        
        Device updatedDevice = deviceService.saveDevice(device);
        
        // Обновляем устройство в ThingsBoard
        if (updatedDevice.getThingsboardToken() != null && !updatedDevice.getThingsboardToken().isEmpty()) {
            boolean tbUpdated = thingsBoardService.updateDevice(updatedDevice);
            if (tbUpdated) {
                log.info("Устройство {} обновлено в ThingsBoard", updatedDevice.getName());
            } else {
                log.warn("Не удалось обновить устройство {} в ThingsBoard", updatedDevice.getName());
            }
        }
        
        return ResponseEntity.ok(convertToDto(updatedDevice));
    }

    /**
     * Удаление устройства
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable UUID id) {
        if (!deviceService.getDeviceById(id).isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                    "Устройство с ID " + id + " не найдено");
        }
        
        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Отправка команды на устройство
     */
    @PostMapping("/{id}/command")
    public ResponseEntity<Map<String, Object>> sendCommand(@PathVariable UUID id, 
                                                        @RequestBody DeviceCommandRequest request) {
        Device device = deviceService.getDeviceById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Устройство с ID " + id + " не найдено"));
        
        // Для виртуальных устройств всегда устанавливаем статус ONLINE перед выполнением команды
        if (device.getProtocol() == ConnectionProtocol.VIRTUAL) {
            device.setStatus(DeviceStatus.ONLINE);
            device.setLastSeen(LocalDateTime.now());
            deviceService.saveDevice(device);
        }
        // Для других протоколов проверяем, что устройство онлайн
        else if (device.getStatus() != DeviceStatus.ONLINE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, 
                    "Устройство " + device.getName() + " находится в состоянии OFFLINE");
        }
        
        // Разделяем параметры на атрибуты и обычные свойства устройства
        Map<String, String> deviceParameters = new HashMap<>();
        Map<String, String> serverAttributes = new HashMap<>();
        Map<String, String> clientAttributes = new HashMap<>();
        Map<String, String> sharedAttributes = new HashMap<>();
        
        request.getParameters().forEach((key, value) -> {
            if (key.startsWith("attr_server_")) {
                // Серверные атрибуты
                serverAttributes.put(key.substring("attr_server_".length()), value);
            } else if (key.startsWith("attr_client_")) {
                // Клиентские атрибуты
                clientAttributes.put(key.substring("attr_client_".length()), value);
            } else if (key.startsWith("attr_shared_")) {
                // Shared атрибуты
                sharedAttributes.put(key.substring("attr_shared_".length()), value);
            } else {
                // Обычные свойства устройства
                deviceParameters.put(key, value);
            }
        });
        
        // Если есть атрибуты для обновления в ThingsBoard
        if (!serverAttributes.isEmpty() || !clientAttributes.isEmpty() || !sharedAttributes.isEmpty()) {
            if (device.getThingsboardToken() != null && !device.getThingsboardToken().isEmpty()) {
                // Обновляем атрибуты в ThingsBoard
                if (!serverAttributes.isEmpty()) {
                    // Сохраняем серверные атрибуты в объекте устройства
                    for (Map.Entry<String, String> entry : serverAttributes.entrySet()) {
                        device.getProperties().put("attr_server_" + entry.getKey(), entry.getValue());
                    }
                    
                    boolean updated = thingsBoardService.updateServerAttributes(device, serverAttributes);
                    if (!updated) {
                        log.warn("Не удалось обновить серверные атрибуты устройства {} в ThingsBoard", device.getName());
                    } else {
                        log.debug("Обновлены серверные атрибуты устройства {} в ThingsBoard: {}", device.getName(), serverAttributes);
                    }
                }
                
                if (!clientAttributes.isEmpty()) {
                    // Сохраняем клиентские атрибуты в объекте устройства
                    for (Map.Entry<String, String> entry : clientAttributes.entrySet()) {
                        device.getProperties().put("attr_client_" + entry.getKey(), entry.getValue());
                    }
                    
                    boolean updated = thingsBoardService.updateClientAttributes(device, clientAttributes);
                    if (!updated) {
                        log.warn("Не удалось обновить клиентские атрибуты устройства {} в ThingsBoard", device.getName());
                    } else {
                        log.debug("Обновлены клиентские атрибуты устройства {} в ThingsBoard: {}", device.getName(), clientAttributes);
                    }
                }
                
                if (!sharedAttributes.isEmpty()) {
                    // Сохраняем shared атрибуты в объекте устройства
                    for (Map.Entry<String, String> entry : sharedAttributes.entrySet()) {
                        device.getProperties().put("attr_shared_" + entry.getKey(), entry.getValue());
                    }
                    
                    boolean updated = thingsBoardService.updateSharedAttributes(device, sharedAttributes);
                    if (!updated) {
                        log.warn("Не удалось обновить shared атрибуты устройства {} в ThingsBoard", device.getName());
                    } else {
                        log.debug("Обновлены shared атрибуты устройства {} в ThingsBoard: {}", device.getName(), sharedAttributes);
                    }
                }
                
                // Сохраняем обновленное устройство
                deviceService.saveDevice(device);
            } else {
                log.warn("Устройство {} не имеет токена ThingsBoard, атрибуты не были обновлены", device.getName());
            }
        }
        
        // Отправляем только обычные параметры на устройство
        boolean success = true;
        if (!deviceParameters.isEmpty()) {
            success = deviceService.sendCommandToDevice(id, request.getCommand(), deviceParameters);
            
            if (!success) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                        "Ошибка при отправке команды на устройство");
            }
            
            // Для команды setState сразу обновляем свойства устройства в базе данных
            if ("setState".equals(request.getCommand())) {
                deviceParameters.forEach((key, value) -> 
                    deviceService.updateDeviceProperty(id, key, value));
                
                // Получаем обновленное устройство
                device = deviceService.getDeviceById(id).orElseThrow();
            }
        }
        
        // Возвращаем обновленные свойства устройства
        Map<String, String> updatedProperties = device.getProperties();
        
        Map<String, Object> response = Map.of(
                "success", success,
                "device_id", id.toString(),
                "command", request.getCommand(),
                "parameters", request.getParameters(),
                "properties", updatedProperties
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Синхронизация устройства с ThingsBoard
     */
    @PostMapping("/{id}/sync-thingsboard")
    public ResponseEntity<Map<String, Object>> syncThingsBoard(@PathVariable UUID id) {
        Device device = deviceService.getDeviceById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Устройство с ID " + id + " не найдено"));
        
        boolean success;
        
        // Если токен не установлен, пытаемся создать устройство
        if (device.getThingsboardToken() == null || device.getThingsboardToken().isEmpty()) {
            success = thingsBoardService.createDevice(device);
            if (success) {
                // Сохраняем устройство с новым токеном
                deviceService.saveDevice(device);
                log.info("Устройство {} создано в ThingsBoard", device.getName());
            }
        } else {
            // Обновляем существующее устройство
            success = thingsBoardService.updateDevice(device);
            if (success) {
                // И отправляем телеметрию
                thingsBoardService.sendDeviceUpdate(device);
            }
        }
        
        if (!success) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Ошибка при синхронизации с ThingsBoard");
        }
        
        Map<String, Object> response = Map.of(
                "success", true,
                "device_id", id.toString(),
                "message", "Устройство успешно синхронизировано с ThingsBoard"
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Тестовый метод для получения токена устройства из ThingsBoard по имени
     */
    @GetMapping("/test-thingsboard/{deviceName}")
    public ResponseEntity<Map<String, Object>> testThingsBoard(@PathVariable String deviceName) {
        log.info("Тестирование получения токена для устройства: {}", deviceName);
        
        String token = thingsBoardService.getTokenByDeviceName(deviceName);
        
        if (token != null) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "device_name", deviceName,
                "token", token
            ));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "device_name", deviceName,
                "message", "Не удалось получить токен устройства"
            ));
        }
    }

    /**
     * Конвертация модели устройства в DTO
     */
    private DeviceDto convertToDto(Device device) {
        DeviceDto dto = new DeviceDto();
        dto.setId(device.getId());
        dto.setName(device.getName());
        dto.setType(device.getType());
        dto.setProtocol(device.getProtocol().name());
        dto.setStatus(device.getStatus().name());
        dto.setConnectionParams(device.getConnectionParams());
        dto.setLastSeen(device.getLastSeen());
        dto.setProperties(device.getProperties());
        dto.setCapabilities(device.getCapabilities());
        dto.setManufacturer(device.getManufacturer());
        dto.setModel(device.getModel());
        dto.setFirmwareVersion(device.getFirmwareVersion());
        dto.setThingsboardToken(device.getThingsboardToken());
        
        // Добавляем атрибуты
        dto.setAttributes(device.getAttributes());
        
        if (device.getRoom() != null) {
            dto.setRoomId(device.getRoom().getId());
            dto.setRoomName(device.getRoom().getName());
        }
        
        if (device.getLocation() != null) {
            dto.setLocationId(device.getLocation().getId());
            dto.setLocationName(device.getLocation().getName());
        }
        
        return dto;
    }
} 