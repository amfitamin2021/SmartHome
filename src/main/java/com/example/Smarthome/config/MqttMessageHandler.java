package com.example.Smarthome.config;

import com.example.Smarthome.model.ConnectionProtocol;
import com.example.Smarthome.model.Device;
import com.example.Smarthome.model.DeviceStatus;
import com.example.Smarthome.protocol.MqttProtocolAdapter;
import com.example.Smarthome.service.DeviceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Обработчик входящих MQTT сообщений от устройств
 */
@Component
@Profile("mqtt")
public class MqttMessageHandler implements MessageHandler {

    private static final Logger log = LoggerFactory.getLogger(MqttMessageHandler.class);
    
    @Autowired
    private DeviceService deviceService;
    
    @Autowired
    private MqttProtocolAdapter mqttProtocolAdapter;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // Паттерн для извлечения ID устройства из топика
    private final Pattern stateTopicPattern = Pattern.compile("smarthome/state/([^/]+)");
    private final Pattern discoveryTopicPattern = Pattern.compile("smarthome/discovery");
    
    @Override
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMessage(Message<?> message) throws MessagingException {
        try {
            String topic = (String) message.getHeaders().get("mqtt_receivedTopic");
            String payload = new String((byte[]) message.getPayload());
            
            log.debug("Получено MQTT сообщение: тема={}, содержимое={}", topic, payload);
            
            if (topic == null) {
                return;
            }
            
            // Обработка сообщений о состоянии устройств
            Matcher stateMatcher = stateTopicPattern.matcher(topic);
            if (stateMatcher.matches()) {
                String deviceId = stateMatcher.group(1);
                handleStateMessage(UUID.fromString(deviceId), payload);
                return;
            }
            
            // Обработка сообщений обнаружения
            Matcher discoveryMatcher = discoveryTopicPattern.matcher(topic);
            if (discoveryMatcher.matches()) {
                handleDiscoveryMessage(payload);
                return;
            }
            
            log.warn("Получено сообщение с неизвестной темой: {}", topic);
            
        } catch (Exception e) {
            log.error("Ошибка при обработке MQTT сообщения: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Обрабатывает сообщение о состоянии устройства
     */
    private void handleStateMessage(UUID deviceId, String payload) {
        try {
            // Парсим JSON со свойствами устройства
            Map<String, String> properties = objectMapper.readValue(payload, 
                    new TypeReference<Map<String, String>>() {});
            
            // Получаем устройство из базы данных
            Optional<Device> deviceOpt = deviceService.getDeviceById(deviceId);
            
            if (deviceOpt.isPresent()) {
                Device device = deviceOpt.get();
                
                // Обновляем статус и время последнего ответа
                device.setStatus(DeviceStatus.ONLINE);
                device.setLastSeen(LocalDateTime.now());
                
                // Обновляем свойства в кэше и в устройстве
                mqttProtocolAdapter.updateDeviceProperties(deviceId.toString(), properties);
                
                // Обновляем свойства устройства
                for (Map.Entry<String, String> entry : properties.entrySet()) {
                    device.getProperties().put(entry.getKey(), entry.getValue());
                }
                
                // Сохраняем обновленное устройство
                deviceService.saveDevice(device);
                
                log.debug("Обновлены свойства устройства {}: {}", device.getName(), properties);
            } else {
                log.warn("Получено сообщение от неизвестного устройства с ID: {}", deviceId);
            }
        } catch (JsonProcessingException e) {
            log.error("Ошибка при разборе JSON состояния устройства: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Обрабатывает сообщение обнаружения устройства (для автоматической регистрации)
     */
    private void handleDiscoveryMessage(String payload) {
        try {
            // Парсим JSON с информацией об устройстве
            Map<String, Object> discoveryInfo = objectMapper.readValue(payload, 
                    new TypeReference<Map<String, Object>>() {});
            
            String type = (String) discoveryInfo.get("type");
            String name = (String) discoveryInfo.get("name");
            String manufacturer = (String) discoveryInfo.get("manufacturer");
            String model = (String) discoveryInfo.get("model");
            String firmwareVersion = (String) discoveryInfo.get("firmware_version");
            
            // Получаем или создаем ID устройства
            String deviceIdStr = (String) discoveryInfo.get("device_id");
            UUID deviceId = (deviceIdStr != null) ? 
                    UUID.fromString(deviceIdStr) : UUID.randomUUID();
            
            // Проверяем, существует ли устройство
            Optional<Device> existingDevice = deviceService.getDeviceById(deviceId);
            
            if (existingDevice.isPresent()) {
                // Обновляем существующее устройство
                Device device = existingDevice.get();
                device.setStatus(DeviceStatus.ONLINE);
                device.setLastSeen(LocalDateTime.now());
                
                // Обновляем прошивку, если она изменилась
                if (firmwareVersion != null && !firmwareVersion.equals(device.getFirmwareVersion())) {
                    device.setFirmwareVersion(firmwareVersion);
                }
                
                deviceService.saveDevice(device);
                log.info("Обнаружено существующее устройство: {}", device.getName());
            } else {
                // Создаем новое устройство
                Device newDevice = new Device();
                newDevice.setId(deviceId);
                newDevice.setName(name);
                newDevice.setType(type);
                newDevice.setManufacturer(manufacturer);
                newDevice.setModel(model);
                newDevice.setFirmwareVersion(firmwareVersion);
                newDevice.setStatus(DeviceStatus.ONLINE);
                newDevice.setLastSeen(LocalDateTime.now());
                
                // По умолчанию используем MQTT протокол для новых устройств
                newDevice.setProtocol(ConnectionProtocol.MQTT);
                
                // Получаем возможности устройства
                if (discoveryInfo.containsKey("capabilities")) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> capabilities = (Map<String, String>) discoveryInfo.get("capabilities");
                    newDevice.getCapabilities().putAll(capabilities);
                }
                
                // Начальные свойства
                if (discoveryInfo.containsKey("properties")) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> initialProperties = (Map<String, String>) discoveryInfo.get("properties");
                    newDevice.getProperties().putAll(initialProperties);
                    mqttProtocolAdapter.updateDeviceProperties(deviceId.toString(), initialProperties);
                }
                
                deviceService.saveDevice(newDevice);
                log.info("Зарегистрировано новое устройство: {} ({})", name, type);
            }
            
        } catch (JsonProcessingException e) {
            log.error("Ошибка при разборе JSON сообщения обнаружения: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Ошибка при обработке сообщения обнаружения: {}", e.getMessage(), e);
        }
    }
} 