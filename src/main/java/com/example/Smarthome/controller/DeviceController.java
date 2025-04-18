package com.example.Smarthome.controller;

import com.example.Smarthome.dto.AvailableDeviceDto;
import com.example.Smarthome.dto.DeviceCommandRequest;
import com.example.Smarthome.dto.DeviceDto;
import com.example.Smarthome.dto.DeviceRegistrationRequest;
import com.example.Smarthome.model.ConnectionProtocol;
import com.example.Smarthome.model.Device;
import com.example.Smarthome.model.DeviceStatus;
import com.example.Smarthome.model.Location;
import com.example.Smarthome.model.Room;
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
        log.info("Получен запрос на регистрацию устройства: {}", request);
        
        try {
            Device device = new Device();
            device.setName(request.getName());
            device.setType(request.getType());
            
            // Установка категории и подтипа устройства
            device.setCategory(request.getCategory());
            device.setSubType(request.getSubType());
            
            // Для обратной совместимости, если subType указан, но type не указан
            if (device.getType() == null && device.getSubType() != null) {
                device.setType(device.getSubType());
            }
            
            device.setProtocol(ConnectionProtocol.valueOf(request.getProtocol()));
            device.setConnectionParams(request.getConnectionParams());
            device.setManufacturer(request.getManufacturer());
            device.setModel(request.getModel());
            device.setFirmwareVersion(request.getFirmwareVersion());
            device.setThingsboardToken(request.getThingsboardToken());
            
            // Устанавливаем ID устройства из ThingsBoard, если он указан
            if (request.getThingsboardId() != null && !request.getThingsboardId().isEmpty()) {
                device.setThingsboardDeviceId(request.getThingsboardId());
                log.info("Установлен идентификатор устройства ThingsBoard: {}", request.getThingsboardId());
            }
            
            device.setStatus(DeviceStatus.OFFLINE);
            
            // Устанавливаем связь с локацией
            if (request.getLocationId() != null) {
                // Получаем локацию из репозитория
                Location location = locationService.getLocationById(request.getLocationId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                                "Локация с ID " + request.getLocationId() + " не найдена"));
                device.setLocation(location);
            }
            
            // Если указан ID комнаты, устанавливаем её
            if (request.getRoomId() != null) {
                Room room = null;
                try {
                    room = deviceService.getRoomById(request.getRoomId());
                } catch (Exception e) {
                    log.warn("Не удалось найти комнату с ID: {}", request.getRoomId());
                }
                device.setRoom(room);
            }
            
            // Если это виртуальное устройство, сразу устанавливаем статус ONLINE
            if (device.getProtocol() == ConnectionProtocol.VIRTUAL) {
                device.setStatus(DeviceStatus.ONLINE);
                device.setLastSeen(LocalDateTime.now());
            }
            
            // Установка начальных свойств в зависимости от категории и типа устройства
            if (request.getInitialProperties() != null && !request.getInitialProperties().isEmpty()) {
                device.getProperties().putAll(request.getInitialProperties());
            } else {
                // Автоматическое добавление базовых свойств в зависимости от категории и типа
                enrichDevicePropertiesByCategoryAndType(device);
            }
            
            // Установка возможностей устройства
            if (request.getCapabilities() != null && !request.getCapabilities().isEmpty()) {
                device.getCapabilities().putAll(request.getCapabilities());
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
                
                // Если это датчик влажности, отправляем начальные значения телеметрии
                if ("CLIMATE".equals(savedDevice.getCategory()) && "HUMIDITY_SENSOR".equals(savedDevice.getSubType())) {
                    try {
                        // Создаем данные телеметрии для инициализации
                        Map<String, Object> telemetryData = new HashMap<>();
                        telemetryData.put("humidity", 45); // Начальное значение влажности
                        telemetryData.put("battery", 98);  // Начальное значение батареи
                        
                        // Отправляем телеметрию в ThingsBoard
                        thingsBoardService.sendTelemetry(savedDevice, telemetryData);
                        
                        log.info("Отправлены начальные данные телеметрии для датчика влажности: {}", telemetryData);
                    } catch (Exception e) {
                        log.error("Ошибка при отправке начальной телеметрии для датчика влажности: {}", e.getMessage(), e);
                    }
                }
            }
            
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDto(savedDevice));
        } catch (Exception e) {
            log.error("Ошибка при регистрации устройства: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Ошибка при регистрации устройства: " + e.getMessage());
        }
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
        
        // Обновление категории и подтипа устройства
        if (request.getCategory() != null) {
            device.setCategory(request.getCategory());
        }
        
        if (request.getSubType() != null) {
            device.setSubType(request.getSubType());
        }
        
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
        
        // Специальная обработка для команды updateTelemetry
        if ("updateTelemetry".equals(request.getCommand())) {
            Map<String, String> parameters = request.getParameters();
            Map<String, Object> telemetryData = new HashMap<>();
            
            // Преобразуем параметры для телеметрии (без префиксов tb_)
            // Телеметрия - это данные, которые хранятся отдельно от атрибутов в ThingsBoard
            if (parameters.containsKey("humidity")) {
                telemetryData.put("humidity", Integer.parseInt(parameters.get("humidity")));
            }
            
            if (parameters.containsKey("battery")) {
                telemetryData.put("battery", Integer.parseInt(parameters.get("battery")));
            }
            
            // Создаем final копию объекта device
            final Device finalDevice = device;
            
            // Сохраняем свойства устройства локально
            parameters.forEach((key, value) -> {
                if (key.startsWith("tb_") || !key.contains("humidity") || !key.contains("battery")) {
                    finalDevice.getProperties().put(key, value);
                }
            });
            
            // Сохраняем устройство
            deviceService.saveDevice(finalDevice);
            
            // Отправляем телеметрию в ThingsBoard, если есть данные
            if (!telemetryData.isEmpty() && finalDevice.getThingsboardToken() != null) {
                boolean success = thingsBoardService.sendTelemetry(finalDevice, telemetryData);
                
                if (!success) {
                    log.warn("Не удалось отправить телеметрию в ThingsBoard для устройства {}", finalDevice.getName());
                }
            }
            
            // Возвращаем ответ
            Map<String, Object> response = Map.of(
                    "success", true,
                    "device_id", id.toString(),
                    "command", request.getCommand(),
                    "parameters", request.getParameters(),
                    "properties", finalDevice.getProperties()
            );
            
            return ResponseEntity.ok(response);
        }
        
        // Стандартная обработка для других команд
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
     * Получение списка доступных устройств из ThingsBoard
     */
    @GetMapping("/available-devices")
    public ResponseEntity<List<AvailableDeviceDto>> getAvailableDevices() {
        List<AvailableDeviceDto> devices = thingsBoardService.getAvailableDevices();
        return ResponseEntity.ok(devices);
    }

    /**
     * Конвертация модели устройства в DTO
     */
    private DeviceDto convertToDto(Device device) {
        DeviceDto dto = new DeviceDto();
        dto.setId(device.getId());
        dto.setName(device.getName());
        dto.setType(device.getType());
        
        // Добавляем категорию и подтип устройства
        dto.setCategory(device.getCategory());
        dto.setSubType(device.getSubType());
        
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
        dto.setThingsboardId(device.getThingsboardDeviceId());
        
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
    
    /**
     * Метод для автоматического добавления базовых свойств устройству в зависимости от категории и типа
     */
    private void enrichDevicePropertiesByCategoryAndType(Device device) {
        String category = device.getCategory();
        String subType = device.getSubType();
        
        if (category == null || subType == null) {
            return;
        }
        
        // Генерируем уникальный ID для устройства
        String uniqueId = subType.toLowerCase() + "_" + UUID.randomUUID().toString().substring(0, 8);
        device.getProperties().put("device_unique_id", uniqueId);
        
        switch(category) {
            case "LIGHTING":
                if ("SMART_BULB".equals(subType)) {
                    device.getProperties().put("attr_server_active", "true");
                    device.getProperties().put("tb_power", "off");
                    device.getProperties().put("tb_brightness", "80");
                    device.getProperties().put("tb_color", "FFFFFF");
                    
                    device.getCapabilities().put("toggle", "true");
                    device.getCapabilities().put("brightness", "true");
                    device.getCapabilities().put("color", "true");
                } else if ("LED_STRIP".equals(subType)) {
                    device.getProperties().put("attr_server_active", "true");
                    device.getProperties().put("tb_power", "off");
                    device.getProperties().put("tb_brightness", "80");
                    device.getProperties().put("tb_color", "FFFFFF");
                    device.getProperties().put("tb_effect", "none");
                    
                    device.getCapabilities().put("toggle", "true");
                    device.getCapabilities().put("brightness", "true");
                    device.getCapabilities().put("color", "true");
                    device.getCapabilities().put("effects", "true");
                }
                break;
                
            case "CLIMATE":
                if ("THERMOSTAT".equals(subType)) {
                    device.getProperties().put("attr_server_active", "true");
                    device.getProperties().put("tb_power", "off");
                    device.getProperties().put("tb_temperature", "22");
                    device.getProperties().put("tb_target_temperature", "22");
                    device.getProperties().put("tb_mode", "auto");
                    
                    device.getCapabilities().put("toggle", "true");
                    device.getCapabilities().put("temperature", "true");
                    device.getCapabilities().put("target_temperature", "true");
                    device.getCapabilities().put("mode", "true");
                } else if ("TEMPERATURE_SENSOR".equals(subType)) {
                    device.getProperties().put("attr_server_active", "true");
                    device.getProperties().put("tb_temperature", "22");
                    
                    device.getCapabilities().put("temperature", "true");
                } else if ("HUMIDITY_SENSOR".equals(subType)) {
                    device.getProperties().put("attr_server_active", "true");
                    device.getProperties().put("tb_humidity", "45");
                    device.getProperties().put("tb_temperature", "22");
                    device.getProperties().put("tb_battery", "98");
                    device.getProperties().put("tb_last_updated", LocalDateTime.now().toString());
                    
                    device.getCapabilities().put("humidity", "true");
                    device.getCapabilities().put("temperature", "true");
                    device.getCapabilities().put("battery", "true");
                }
                break;
                
            case "SECURITY":
                if ("CAMERA".equals(subType)) {
                    device.getProperties().put("attr_server_active", "true");
                    device.getProperties().put("tb_power", "off");
                    device.getProperties().put("tb_recording", "off");
                    device.getProperties().put("tb_motion_detection", "on");
                    
                    device.getCapabilities().put("toggle", "true");
                    device.getCapabilities().put("recording", "true");
                    device.getCapabilities().put("motion_detection", "true");
                } else if ("SMART_LOCK".equals(subType)) {
                    device.getProperties().put("attr_server_active", "true");
                    device.getProperties().put("tb_locked", "true");
                    device.getProperties().put("tb_battery", "100");
                    
                    device.getCapabilities().put("lock", "true");
                    device.getCapabilities().put("battery", "true");
                }
                break;
                
            case "APPLIANCES":
                if ("TV".equals(subType)) {
                    device.getProperties().put("attr_server_active", "true");
                    device.getProperties().put("tb_power", "off");
                    device.getProperties().put("tb_volume", "50");
                    device.getProperties().put("tb_channel", "1");
                    
                    device.getCapabilities().put("toggle", "true");
                    device.getCapabilities().put("volume", "true");
                    device.getCapabilities().put("channel", "true");
                } else if ("VACUUM".equals(subType)) {
                    device.getProperties().put("attr_server_active", "true");
                    device.getProperties().put("tb_power", "off");
                    device.getProperties().put("tb_mode", "auto");
                    device.getProperties().put("tb_battery", "100");
                    
                    device.getCapabilities().put("toggle", "true");
                    device.getCapabilities().put("mode", "true");
                    device.getCapabilities().put("battery", "true");
                }
                break;
                
            default:
                // Для всех остальных типов устройств устанавливаем базовые свойства
                device.getProperties().put("attr_server_active", "true");
                device.getProperties().put("tb_power", "off");
        }
    }
} 