package com.example.Smarthome.protocol;

import com.example.Smarthome.model.Device;

import java.util.Map;

/**
 * Интерфейс адаптера для работы с различными протоколами связи
 */
public interface ProtocolAdapter {
    
    /**
     * Отправляет команду на устройство
     * @param device Устройство
     * @param command Команда
     * @param parameters Параметры команды
     * @return Успешность выполнения
     */
    boolean sendCommand(Device device, String command, Map<String, String> parameters);
    
    /**
     * Проверяет статус устройства
     * @param device Устройство
     * @return true если устройство в сети
     */
    boolean checkDeviceStatus(Device device);
    
    /**
     * Получает актуальные свойства устройства
     * @param device Устройство
     * @return Карта свойств устройства
     */
    Map<String, String> getDeviceProperties(Device device);
} 