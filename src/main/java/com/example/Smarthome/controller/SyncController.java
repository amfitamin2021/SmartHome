package com.example.Smarthome.controller;

import com.example.Smarthome.model.Device;
import com.example.Smarthome.service.ThingsBoardSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Контроллер для управления синхронизацией с ThingsBoard
 */
@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
@Slf4j
public class SyncController {

    private final ThingsBoardSyncService thingsBoardSyncService;
    
    /**
     * Запускает принудительную синхронизацию телеметрии всех устройств
     */
    @PostMapping("/telemetry")
    public ResponseEntity<Map<String, Object>> syncTelemetry() {
        log.info("Запуск принудительной синхронизации телеметрии");
        thingsBoardSyncService.forceSyncTelemetry();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Синхронизация телеметрии запущена");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Запускает принудительную синхронизацию всех данных конкретного устройства
     */
    @PostMapping("/device/{deviceId}")
    public ResponseEntity<Map<String, Object>> syncDevice(@PathVariable String deviceId) {
        log.info("Запуск принудительной синхронизации устройства с ID: {}", deviceId);
        boolean success = thingsBoardSyncService.forceSyncDevice(deviceId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        if (success) {
            response.put("message", "Синхронизация устройства выполнена успешно");
        } else {
            response.put("message", "Ошибка синхронизации устройства");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Запускает импорт устройств из ThingsBoard
     */
    @PostMapping("/import-devices")
    public ResponseEntity<Map<String, Object>> importDevices() {
        log.info("Запуск импорта устройств из ThingsBoard");
        int importedCount = thingsBoardSyncService.importDevicesFromThingsBoard();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Импорт устройств завершен");
        response.put("importedCount", importedCount);
        
        return ResponseEntity.ok(response);
    }

    /* Временно закомментировано, так как метод еще не реализован в сервисе
    /**
     * Проверка подключения к ThingsBoard
     * @return результаты теста подключения
     */
    /*
    @GetMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection() {
        boolean result = thingsBoardSyncService.testThingsBoardConnection();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", result);
        response.put("message", result ? 
                "Подключение к ThingsBoard работает" : 
                "Проблема с подключением к ThingsBoard");
        
        return ResponseEntity.ok(response);
    }
    */

    /**
     * Тестирует получение телеметрии из ThingsBoard для конкретного устройства
     * @param deviceId ID устройства
     * @return результаты теста
     */
    @GetMapping("/test-telemetry/{deviceId}")
    public ResponseEntity<Map<String, Object>> testTelemetry(@PathVariable String deviceId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Получаем устройство по ID
            java.util.Optional<Device> deviceOpt = thingsBoardSyncService.getDeviceService().getDeviceById(java.util.UUID.fromString(deviceId));
            
            if (!deviceOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "Устройство не найдено");
                return ResponseEntity.ok(response);
            }
            
            Device device = deviceOpt.get();
            
            if (device.getThingsboardToken() == null || device.getThingsboardToken().isEmpty()) {
                response.put("success", false);
                response.put("message", "У устройства нет токена ThingsBoard");
                return ResponseEntity.ok(response);
            }
            
            // Текущие свойства устройства перед синхронизацией
            response.put("deviceInfo", Map.of(
                "id", device.getId().toString(),
                "name", device.getName(),
                "token", device.getThingsboardToken(),
                "properties", new HashMap<>(device.getProperties())
            ));
            
            // Запускаем синхронизацию для этого устройства
            boolean result = thingsBoardSyncService.forceSyncDevice(deviceId);
            response.put("success", result);
            
            if (result) {
                response.put("message", "Телеметрия устройства успешно синхронизирована");
                
                // Получаем обновленные данные устройства после синхронизации
                Device updatedDevice = thingsBoardSyncService.getDeviceService().getDeviceById(java.util.UUID.fromString(deviceId)).get();
                
                response.put("updatedProperties", new HashMap<>(updatedDevice.getProperties()));
            } else {
                response.put("message", "Не удалось синхронизировать телеметрию устройства");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Ошибка при тестировании телеметрии: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
} 