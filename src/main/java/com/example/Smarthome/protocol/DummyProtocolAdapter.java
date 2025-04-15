package com.example.Smarthome.protocol;

import com.example.Smarthome.model.Device;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Заглушка адаптера протокола, используется когда MQTT недоступен
 */
@Component
@Slf4j
@Profile("!mqtt")
public class DummyProtocolAdapter implements ProtocolAdapter {
    
    // Кэш для хранения состояний устройств
    private final Map<String, Map<String, String>> deviceStates = new ConcurrentHashMap<>();
    
    @Override
    public boolean sendCommand(Device device, String command, Map<String, String> parameters) {
        log.info("Имитация отправки команды на устройство {}: команда={}, параметры={}", 
                device.getName(), command, parameters);
        
        // Для примера, можно симулировать сохранение состояния устройства
        String deviceId = device.getId().toString();
        Map<String, String> state = deviceStates.getOrDefault(deviceId, new HashMap<>());
        
        // Обновляем состояние устройства на основе команды
        if ("setState".equals(command)) {
            state.putAll(parameters);
        } else if ("setValue".equals(command) && parameters.containsKey("property") && parameters.containsKey("value")) {
            state.put(parameters.get("property"), parameters.get("value"));
        }
        
        deviceStates.put(deviceId, state);
        return true;
    }
    
    @Override
    public boolean checkDeviceStatus(Device device) {
        log.info("Имитация проверки статуса устройства {}", device.getName());
        // Всегда возвращаем true для демонстрации
        return true;
    }
    
    @Override
    public Map<String, String> getDeviceProperties(Device device) {
        log.info("Имитация получения свойств устройства {}", device.getName());
        String deviceId = device.getId().toString();
        
        // Возвращаем кэшированные свойства или создаем демо-свойства
        return deviceStates.computeIfAbsent(deviceId, k -> {
            Map<String, String> demoProps = new HashMap<>();
            // Создаем демо-свойства в зависимости от типа устройства
            switch(device.getType()) {
                case "light":
                    demoProps.put("state", "off");
                    demoProps.put("brightness", "50");
                    demoProps.put("color", "warm");
                    break;
                case "thermostat":
                    demoProps.put("temperature", "21.5");
                    demoProps.put("mode", "auto");
                    demoProps.put("target", "22.0");
                    break;
                case "sensor":
                    demoProps.put("temperature", "23.2");
                    demoProps.put("humidity", "45");
                    demoProps.put("battery", "87");
                    break;
                default:
                    demoProps.put("status", "ready");
                    break;
            }
            return demoProps;
        });
    }
} 