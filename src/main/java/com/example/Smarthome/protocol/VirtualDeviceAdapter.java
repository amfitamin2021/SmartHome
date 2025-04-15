package com.example.Smarthome.protocol;

import com.example.Smarthome.model.Device;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Адаптер для работы с виртуальными (эмулируемыми) устройствами
 * Используется для тестирования и демонстрации без реальных физических устройств
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VirtualDeviceAdapter implements ProtocolAdapter {
    
    private final ObjectMapper objectMapper;
    private final Random random = new Random();
    
    // Кэш состояний виртуальных устройств
    private final Map<String, Map<String, String>> deviceStateCache = new ConcurrentHashMap<>();
    private final Map<String, Long> lastUpdateTime = new ConcurrentHashMap<>();
    
    @Override
    public boolean sendCommand(Device device, String command, Map<String, String> parameters) {
        String deviceId = device.getId().toString();
        log.debug("Отправлена команда на виртуальное устройство {}: {} с параметрами {}", 
                device.getName(), command, parameters);
        
        // Получаем текущее состояние устройства
        Map<String, String> state = deviceStateCache.computeIfAbsent(deviceId, k -> new HashMap<>());
        
        // Эмулируем обработку команд
        switch (command) {
            case "setState" -> {
                // Обработка команды установки состояния с несколькими параметрами
                // Копируем все параметры в состояние устройства
                state.putAll(parameters);
                log.debug("Обновлено состояние устройства {}: {}", device.getName(), parameters);
                
                // Обновляем свойства в самом устройстве
                device.getProperties().putAll(parameters);
            }
            case "power" -> {
                if (parameters.containsKey("state")) {
                    state.put("power", parameters.get("state"));
                }
            }
            case "brightness" -> {
                if (parameters.containsKey("level")) {
                    state.put("brightness", parameters.get("level"));
                }
            }
            case "color" -> {
                if (parameters.containsKey("rgb")) {
                    state.put("color", parameters.get("rgb"));
                }
            }
            case "temperature" -> {
                if (parameters.containsKey("value")) {
                    state.put("temperature", parameters.get("value"));
                }
            }
            // Другие команды...
            default -> log.warn("Неизвестная команда {} для устройства {}", command, device.getName());
        }
        
        // Обновляем время последнего обновления
        lastUpdateTime.put(deviceId, System.currentTimeMillis());
        
        // Эмулируем вероятность ошибки для более реалистичного поведения
        return random.nextDouble() > 0.05; // 5% вероятность ошибки
    }
    
    @Override
    public boolean checkDeviceStatus(Device device) {
        String deviceId = device.getId().toString();
        
        // Эмулируем онлайн-статус с некоторой долей случайности
        if (!lastUpdateTime.containsKey(deviceId)) {
            // Если устройство ещё не имело обновлений, инициализируем его
            initializeVirtualDevice(device);
            return true;
        }
        
        // Симулируем некоторую вероятность отключения
        if (random.nextDouble() < 0.01) { // 1% вероятность быть оффлайн
            return false;
        }
        
        return true;
    }
    
    @Override
    public Map<String, String> getDeviceProperties(Device device) {
        String deviceId = device.getId().toString();
        
        // Если устройство ещё не инициализировано, делаем это
        if (!deviceStateCache.containsKey(deviceId)) {
            initializeVirtualDevice(device);
        }
        
        // Периодически обновляем состояние для симуляции изменений
        // (например, для датчиков температуры, влажности и т.д.)
        updateSensorValues(device);
        
        return deviceStateCache.getOrDefault(deviceId, new HashMap<>());
    }
    
    /**
     * Инициализирует начальное состояние виртуального устройства в зависимости от его типа
     */
    private void initializeVirtualDevice(Device device) {
        String deviceId = device.getId().toString();
        Map<String, String> state = new HashMap<>();
        
        // В зависимости от типа устройства задаем начальные параметры
        switch (device.getType()) {
            case "light" -> {
                state.put("power", "off");
                state.put("brightness", "0");
                state.put("color", "FFFFFF");
            }
            case "thermostat" -> {
                state.put("power", "on");
                state.put("mode", "heat");
                state.put("temperature", "21.5");
                state.put("target_temperature", "22.0");
            }
            case "sensor" -> {
                state.put("temperature", String.format("%.1f", 20 + random.nextDouble() * 5));
                state.put("humidity", String.format("%.1f", 40 + random.nextDouble() * 20));
                state.put("battery", String.valueOf(70 + random.nextInt(30)));
            }
            case "switch" -> {
                state.put("state", "off");
            }
            default -> {
                state.put("status", "unknown");
            }
        }
        
        deviceStateCache.put(deviceId, state);
        lastUpdateTime.put(deviceId, System.currentTimeMillis());
    }
    
    /**
     * Обновляет значения датчиков для более реалистичной симуляции
     */
    private void updateSensorValues(Device device) {
        String deviceId = device.getId().toString();
        
        if (!deviceStateCache.containsKey(deviceId)) {
            return;
        }
        
        Map<String, String> state = deviceStateCache.get(deviceId);
        long lastUpdate = lastUpdateTime.getOrDefault(deviceId, 0L);
        
        // Обновляем только каждые 30 секунд для экономии ресурсов
        if (System.currentTimeMillis() - lastUpdate < 30000) {
            return;
        }
        
        // Обновляем значения в зависимости от типа устройства
        if ("sensor".equals(device.getType())) {
            // Немного изменяем температуру
            if (state.containsKey("temperature")) {
                double currentTemp = Double.parseDouble(state.get("temperature"));
                double newTemp = currentTemp + (random.nextDouble() - 0.5) * 0.5; // +/- 0.5 градуса максимум
                state.put("temperature", String.format("%.1f", newTemp));
            }
            
            // Немного изменяем влажность
            if (state.containsKey("humidity")) {
                double currentHumidity = Double.parseDouble(state.get("humidity"));
                double newHumidity = currentHumidity + (random.nextDouble() - 0.5) * 2.0; // +/- 2% максимум
                newHumidity = Math.max(0, Math.min(100, newHumidity)); // ограничиваем 0-100%
                state.put("humidity", String.format("%.1f", newHumidity));
            }
            
            // Уменьшаем заряд батареи со временем
            if (state.containsKey("battery")) {
                int currentBattery = Integer.parseInt(state.get("battery"));
                int newBattery = Math.max(0, currentBattery - random.nextInt(2));
                state.put("battery", String.valueOf(newBattery));
            }
        } else if ("thermostat".equals(device.getType())) {
            // Симулируем изменение температуры в зависимости от целевой температуры
            if (state.containsKey("temperature") && state.containsKey("target_temperature")) {
                double currentTemp = Double.parseDouble(state.get("temperature"));
                double targetTemp = Double.parseDouble(state.get("target_temperature"));
                
                // Медленно приближаем текущую температуру к целевой
                double diff = targetTemp - currentTemp;
                double adjustment = diff * 0.1 + (random.nextDouble() - 0.5) * 0.2;
                double newTemp = currentTemp + adjustment;
                
                state.put("temperature", String.format("%.1f", newTemp));
            }
        }
        
        // Обновляем время последнего обновления
        lastUpdateTime.put(deviceId, System.currentTimeMillis());
    }
} 