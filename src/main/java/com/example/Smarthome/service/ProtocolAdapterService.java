package com.example.Smarthome.service;

import com.example.Smarthome.model.ConnectionProtocol;
import com.example.Smarthome.model.Device;
import com.example.Smarthome.protocol.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ProtocolAdapterService {

    private final Map<ConnectionProtocol, ProtocolAdapter> protocolAdapters = new HashMap<>();
    private final VirtualDeviceAdapter virtualDeviceAdapter;
    
    // Конструктор с профилями для разных конфигураций
    @Autowired
    public ProtocolAdapterService(VirtualDeviceAdapter virtualDeviceAdapter, 
                                  List<ProtocolAdapter> adapters) {
        this.virtualDeviceAdapter = virtualDeviceAdapter;
        
        // Регистрируем адаптеры, найденные в контексте Spring
        for (ProtocolAdapter adapter : adapters) {
            if (adapter instanceof MqttProtocolAdapter) {
                protocolAdapters.put(ConnectionProtocol.MQTT, adapter);
                log.info("Зарегистрирован MQTT адаптер протокола");
            } else if (adapter instanceof DummyProtocolAdapter) {
                // Заглушка может обрабатывать MQTT запросы при отсутствии настоящего адаптера
                if (!protocolAdapters.containsKey(ConnectionProtocol.MQTT)) {
                    protocolAdapters.put(ConnectionProtocol.MQTT, adapter);
                    log.info("Зарегистрирован заглушка-адаптер для MQTT протокола");
                }
            }
        }
        
        // Регистрируем виртуальный адаптер
        protocolAdapters.put(ConnectionProtocol.VIRTUAL, virtualDeviceAdapter);
        log.info("Зарегистрирован виртуальный адаптер протокола");
    }
    
    /**
     * Отправляет команду на устройство через соответствующий протокол
     * @param device Устройство
     * @param command Команда
     * @param parameters Параметры команды
     * @return Успешность выполнения
     */
    public boolean sendCommand(Device device, String command, Map<String, String> parameters) {
        ProtocolAdapter adapter = getAdapterForDevice(device);
        if (adapter != null) {
            try {
                return adapter.sendCommand(device, command, parameters);
            } catch (Exception e) {
                log.error("Ошибка при отправке команды {} на устройство {}: {}", 
                        command, device.getName(), e.getMessage(), e);
                return false;
            }
        }
        log.warn("Не найден адаптер для протокола {} устройства {}", 
                device.getProtocol(), device.getName());
        return false;
    }
    
    /**
     * Проверяет статус устройства через соответствующий протокол
     * @param device Устройство
     * @return Онлайн ли устройство
     */
    public boolean checkDeviceStatus(Device device) {
        ProtocolAdapter adapter = getAdapterForDevice(device);
        if (adapter != null) {
            try {
                return adapter.checkDeviceStatus(device);
            } catch (Exception e) {
                log.error("Ошибка при проверке статуса устройства {}: {}", 
                        device.getName(), e.getMessage(), e);
                return false;
            }
        }
        return false;
    }
    
    /**
     * Получает текущие свойства устройства через соответствующий протокол
     * @param device Устройство
     * @return Карта свойств устройства
     */
    public Map<String, String> getDeviceProperties(Device device) {
        ProtocolAdapter adapter = getAdapterForDevice(device);
        if (adapter != null) {
            try {
                return adapter.getDeviceProperties(device);
            } catch (Exception e) {
                log.error("Ошибка при получении свойств устройства {}: {}", 
                        device.getName(), e.getMessage(), e);
            }
        }
        return new HashMap<>();
    }
    
    /**
     * Определяет соответствующий адаптер протокола для устройства
     * @param device Устройство
     * @return Подходящий адаптер или null
     */
    private ProtocolAdapter getAdapterForDevice(Device device) {
        ConnectionProtocol protocol = device.getProtocol();
        if (protocol == null) {
            return null;
        }
        
        return protocolAdapters.get(protocol);
    }
} 