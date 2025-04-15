package com.example.Smarthome.service;

import com.example.Smarthome.model.Device;
import com.example.Smarthome.model.ConnectionProtocol;
import com.example.Smarthome.model.DeviceStatus;
import com.example.Smarthome.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * Сервис для синхронизации данных из ThingsBoard
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ThingsBoardSyncService {

    private final DeviceRepository deviceRepository;
    private final ThingsBoardIntegrationService thingsBoardService;
    private final DeviceService deviceService;
    private final RestTemplate restTemplate;
    
    @Value("${thingsboard.url}")
    private String thingsBoardUrl;
    
    /**
     * Периодически синхронизирует данные устройств из ThingsBoard
     * Выполняется каждый час
     */
    @Scheduled(fixedRateString = "${thingsboard.pull.interval:3600000}")
    public void syncFromThingsBoard() {
        log.info("Запуск синхронизации устройств из ThingsBoard");
        
        // Получаем все устройства с токенами ThingsBoard
        List<Device> devices = deviceRepository.findAllByThingsboardTokenIsNotNull();
        
        if (devices.isEmpty()) {
            log.debug("Нет устройств с токенами ThingsBoard для синхронизации");
            return;
        }
        
        log.info("Синхронизация {} устройств из ThingsBoard", devices.size());
        
        int updatedCount = 0;
        for (Device device : devices) {
            try {
                boolean updated = syncDeviceFromThingsBoard(device);
                if (updated) {
                    updatedCount++;
                }
            } catch (Exception e) {
                log.error("Ошибка при синхронизации устройства {} из ThingsBoard: {}", 
                        device.getName(), e.getMessage(), e);
            }
        }
        
        log.info("Синхронизация из ThingsBoard завершена. Обновлено: {}/{}", updatedCount, devices.size());
    }
    
    /**
     * Синхронизирует все данные устройств из ThingsBoard
     * Включая атрибуты и телеметрию
     * Выполняется каждые 5 минут
     */
    @Scheduled(fixedRateString = "${thingsboard.sync.telemetry.interval:300000}")
    public void syncTelemetryFromThingsBoard() {
        log.info("Запуск синхронизации телеметрии устройств из ThingsBoard");
        
        // Получаем все устройства с токенами ThingsBoard
        List<Device> devices = deviceRepository.findAllByThingsboardTokenIsNotNull();
        
        if (devices.isEmpty()) {
            log.debug("Нет устройств с токенами ThingsBoard для синхронизации телеметрии");
            return;
        }
        
        log.info("Синхронизация телеметрии {} устройств из ThingsBoard", devices.size());
        
        int updatedCount = 0;
        for (Device device : devices) {
            try {
                boolean updated = syncDeviceTelemetryFromThingsBoard(device);
                if (updated) {
                    updatedCount++;
                }
            } catch (Exception e) {
                log.error("Ошибка при синхронизации телеметрии устройства {} из ThingsBoard: {}", 
                        device.getName(), e.getMessage(), e);
            }
        }
        
        log.info("Синхронизация телеметрии из ThingsBoard завершена. Обновлено: {}/{}", updatedCount, devices.size());
    }
    
    /**
     * Синхронизирует данные одного устройства из ThingsBoard
     * @param device Устройство для синхронизации
     * @return true если устройство было обновлено
     */
    private boolean syncDeviceFromThingsBoard(Device device) {
        if (device.getThingsboardToken() == null || device.getThingsboardToken().isEmpty()) {
            log.debug("Устройство {} не имеет токена ThingsBoard, пропускаем синхронизацию", device.getName());
            return false;
        }
        
        try {
            boolean updated = false;
            
            // 1. Получаем клиентские и shared атрибуты устройства через публичный API
            String url = thingsBoardUrl + "/api/v1/" + device.getThingsboardToken() + "/attributes";
            log.debug("Запрашиваем атрибуты устройства {} из ThingsBoard: {}", device.getName(), url);
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            // Создаем наборы для отслеживания атрибутов
            Set<String> clientAttrKeys = new HashSet<>();
            Set<String> sharedAttrKeys = new HashSet<>();
            Set<String> serverAttrKeys = new HashSet<>();
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.debug("Получены атрибуты устройства {} из ThingsBoard: {}", device.getName(), response.getBody());
                
                // Обрабатываем клиентские атрибуты
                if (response.getBody().containsKey("client")) {
                    Map<String, Object> clientAttributes = (Map<String, Object>) response.getBody().get("client");
                    
                    // Собираем текущие ключи клиентских атрибутов
                    clientAttrKeys.addAll(clientAttributes.keySet());
                    
                    // Обрабатываем основные атрибуты, которые напрямую влияют на поля объекта
                    if (clientAttributes.containsKey("name") && 
                            !clientAttributes.get("name").toString().equals(device.getName())) {
                        device.setName(clientAttributes.get("name").toString());
                        updated = true;
                    }
                    
                    if (clientAttributes.containsKey("manufacturer") && 
                            !clientAttributes.get("manufacturer").toString().equals(device.getManufacturer())) {
                        device.setManufacturer(clientAttributes.get("manufacturer").toString());
                        updated = true;
                    }
                    
                    if (clientAttributes.containsKey("model") && 
                            !clientAttributes.get("model").toString().equals(device.getModel())) {
                        device.setModel(clientAttributes.get("model").toString());
                        updated = true;
                    }
                    
                    if (clientAttributes.containsKey("firmwareVersion") && 
                            !clientAttributes.get("firmwareVersion").toString().equals(device.getFirmwareVersion())) {
                        device.setFirmwareVersion(clientAttributes.get("firmwareVersion").toString());
                        updated = true;
                    }
                    
                    // Обрабатываем все остальные клиентские атрибуты
                    for (Map.Entry<String, Object> entry : clientAttributes.entrySet()) {
                        String key = entry.getKey();
                        // Пропускаем основные атрибуты, которые уже обработаны
                        if (key.equals("name") || key.equals("manufacturer") || 
                            key.equals("model") || key.equals("firmwareVersion")) {
                            continue;
                        }
                        
                        // Обрабатываем остальные клиентские атрибуты с префиксом
                        Object value = entry.getValue();
                        String stringValue = value != null ? value.toString() : null;
                        String propKey = "attr_client_" + key;
                        
                        // Сравниваем и обновляем при необходимости
                        String currentValue = device.getProperties().get(propKey);
                        if (currentValue == null || !currentValue.equals(stringValue)) {
                            device.getProperties().put(propKey, stringValue);
                            updated = true;
                            log.info("Обновлен client-атрибут {} устройства {}: {} -> {}", 
                                    key, device.getName(), currentValue, stringValue);
                        }
                    }
                }
                
                // Обрабатываем shared атрибуты
                if (response.getBody().containsKey("shared")) {
                    Map<String, Object> sharedAttributes = (Map<String, Object>) response.getBody().get("shared");
                    
                    // Собираем текущие ключи shared атрибутов
                    sharedAttrKeys.addAll(sharedAttributes.keySet());
                    
                    // Обрабатываем все shared атрибуты
                    for (Map.Entry<String, Object> entry : sharedAttributes.entrySet()) {
                        String key = entry.getKey();
                        Object value = entry.getValue();
                        String stringValue = value != null ? value.toString() : null;
                        String propKey = "attr_shared_" + key;
                        
                        // Сравниваем и обновляем при необходимости
                        String currentValue = device.getProperties().get(propKey);
                        if (currentValue == null || !currentValue.equals(stringValue)) {
                            device.getProperties().put(propKey, stringValue);
                            updated = true;
                            log.info("Обновлен shared-атрибут {} устройства {}: {} -> {}", 
                                    key, device.getName(), currentValue, stringValue);
                        }
                    }
                }
            } else {
                log.warn("Не удалось получить атрибуты устройства {} из ThingsBoard. Код: {}", 
                        device.getName(), response.getStatusCode());
            }
            
            // 2. Получаем серверные атрибуты через серверный API с аутентификацией
            if (thingsBoardService.ensureAuthenticated()) {
                // Нам нужен ThingsBoard Device ID
                String deviceId;
                if (device.getThingsboardDeviceId() != null && !device.getThingsboardDeviceId().isEmpty()) {
                    deviceId = device.getThingsboardDeviceId();
                } else {
                    deviceId = thingsBoardService.getDeviceIdByToken(device.getThingsboardToken());
                    if (deviceId != null) {
                        device.setThingsboardDeviceId(deviceId);
                        updated = true;
                    } else {
                        log.error("Не удалось получить ThingsBoard ID для устройства {}", device.getName());
                    }
                }
                
                if (deviceId != null) {
                    // Формируем запрос для получения серверных атрибутов
                    String serverAttrUrl = thingsBoardUrl + "/api/plugins/telemetry/DEVICE/" + deviceId + "/values/attributes/SERVER_SCOPE";
                    log.debug("Запрашиваем серверные атрибуты устройства {} из ThingsBoard: {}", device.getName(), serverAttrUrl);
                    
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("X-Authorization", "Bearer " + thingsBoardService.getAccessToken());
                    
                    HttpEntity<Void> entity = new HttpEntity<>(headers);
                    
                    try {
                        ResponseEntity<List> serverAttrResponse = restTemplate.exchange(
                                serverAttrUrl,
                                HttpMethod.GET,
                                entity,
                                List.class);
                        
                        if (serverAttrResponse.getStatusCode().is2xxSuccessful() && 
                                serverAttrResponse.getBody() != null && 
                                !serverAttrResponse.getBody().isEmpty()) {
                            
                            List<Map<String, Object>> serverAttrs = serverAttrResponse.getBody();
                            log.debug("Получены серверные атрибуты устройства {}: {}", device.getName(), serverAttrs);
                            
                            // Обрабатываем серверные атрибуты
                            for (Map<String, Object> attr : serverAttrs) {
                                if (attr.containsKey("key") && attr.containsKey("value")) {
                                    String key = attr.get("key").toString();
                                    Object value = attr.get("value");
                                    serverAttrKeys.add(key); // Добавляем ключ в набор для отслеживания
                                    
                                    String stringValue = value != null ? value.toString() : null;
                                    String propKey = "attr_server_" + key;
                                    
                                    // Сравниваем и обновляем при необходимости
                                    String currentValue = device.getProperties().get(propKey);
                                    if (currentValue == null || !currentValue.equals(stringValue)) {
                                        device.getProperties().put(propKey, stringValue);
                                        updated = true;
                                        log.info("Обновлен server-атрибут {} устройства {}: {} -> {}", 
                                                key, device.getName(), currentValue, stringValue);
                                    }
                                }
                            }
                        } else {
                            log.debug("Нет серверных атрибутов для устройства {} в ThingsBoard", device.getName());
                        }
                    } catch (Exception e) {
                        log.warn("Ошибка при получении серверных атрибутов устройства {}: {}", 
                                device.getName(), e.getMessage());
                    }
                }
            } else {
                log.warn("Не удалось аутентифицироваться для получения серверных атрибутов устройства {}", device.getName());
            }
            
            // Находим атрибуты, которые были удалены в ThingsBoard
            Set<String> keysToRemove = new HashSet<>();
            for (String key : device.getProperties().keySet()) {
                // Проверяем только атрибуты, не трогаем другие свойства
                if (key.startsWith("attr_client_")) {
                    String attrKey = key.substring("attr_client_".length());
                    if (!clientAttrKeys.contains(attrKey)) {
                        keysToRemove.add(key);
                    }
                } else if (key.startsWith("attr_shared_")) {
                    String attrKey = key.substring("attr_shared_".length());
                    if (!sharedAttrKeys.contains(attrKey)) {
                        keysToRemove.add(key);
                    }
                } else if (key.startsWith("attr_server_")) {
                    String attrKey = key.substring("attr_server_".length());
                    if (!serverAttrKeys.contains(attrKey)) {
                        keysToRemove.add(key);
                    }
                }
            }
            
            // Удаляем отсутствующие атрибуты
            for (String key : keysToRemove) {
                device.getProperties().remove(key);
                updated = true;
                log.info("Удален отсутствующий атрибут {} устройства {}", 
                        key, device.getName());
            }
            
            // Если есть изменения, сохраняем устройство
            if (updated) {
                deviceRepository.save(device);
                log.info("Устройство {} обновлено на основе данных из ThingsBoard", device.getName());
                return true;
            }
            
            return false;
        } catch (RestClientException e) {
            log.error("Ошибка при синхронизации устройства {} из ThingsBoard: {}", 
                    device.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Синхронизирует телеметрию одного устройства из ThingsBoard
     * @param device Устройство для синхронизации
     * @return true если телеметрия была обновлена
     */
    private boolean syncDeviceTelemetryFromThingsBoard(Device device) {
        if (device.getThingsboardToken() == null || device.getThingsboardToken().isEmpty()) {
            log.debug("Устройство {} не имеет токена ThingsBoard, пропускаем синхронизацию телеметрии", device.getName());
            return false;
        }
        
        boolean updated = false;
        
        try {
            // 1. Получаем свежие атрибуты устройства 
            // Используем существующий метод для синхронизации атрибутов
            boolean attributesUpdated = syncDeviceFromThingsBoard(device);
            if (attributesUpdated) {
                updated = true;
                log.info("Успешно обновлены атрибуты устройства {}", device.getName());
            }
            
            // 2. Для получения телеметрии используем API v2, который требует аутентификации
            // Сначала проверяем, что мы авторизованы
            if (!thingsBoardService.ensureAuthenticated()) {
                log.error("Не удалось аутентифицироваться в ThingsBoard для получения телеметрии");
                return updated; // возвращаем updated, т.к. атрибуты могли обновиться
            }
            
            // Получаем deviceId от ThingsBoard
            String deviceId;
            
            // Если у устройства уже сохранен ThingsBoard ID
            if (device.getThingsboardDeviceId() != null && !device.getThingsboardDeviceId().isEmpty()) {
                deviceId = device.getThingsboardDeviceId();
            } else {
                // Если нет, получаем ID устройства по токену
                deviceId = thingsBoardService.getDeviceIdByToken(device.getThingsboardToken());
                if (deviceId == null) {
                    log.error("Не удалось получить ID устройства по токену: {}", device.getThingsboardToken());
                    return updated; // возвращаем updated, т.к. атрибуты могли обновиться
                }
                // Сохраняем ID для будущих запросов
                device.setThingsboardDeviceId(deviceId);
                deviceRepository.save(device);
                updated = true; // ID обновлен
            }
            
            // Используем API v2 для получения последних значений телеметрии
            String telemetryUrl = thingsBoardUrl + "/api/plugins/telemetry/DEVICE/" + deviceId + "/values/timeseries";
            log.info("Запрашиваем телеметрию устройства {} через API v2: {}", device.getName(), telemetryUrl);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Authorization", "Bearer " + thingsBoardService.getAccessToken());
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> telemetryResponse = restTemplate.exchange(
                    telemetryUrl,
                    HttpMethod.GET,
                    entity,
                    Map.class);
            
            log.info("Ответ на запрос телеметрии для {} (код {}): {}", 
                    device.getName(), telemetryResponse.getStatusCode(), telemetryResponse.getBody());
            
            if (telemetryResponse.getStatusCode().is2xxSuccessful() && 
                    telemetryResponse.getBody() != null && 
                    !telemetryResponse.getBody().isEmpty()) {
                
                // Обрабатываем ответ с телеметрией в новом формате ThingsBoard v4
                Map<String, Object> telemetryData = telemetryResponse.getBody();
                log.info("Полученные данные телеметрии для {}: {}", device.getName(), telemetryData);
                
                boolean telemetryUpdated = false;
                
                // Сохраняем текущие ключи телеметрии
                Set<String> currentKeys = new HashSet<>(telemetryData.keySet());
                
                // Создаем копию существующих свойств для поиска удаленных полей
                Set<String> keysToRemove = new HashSet<>();
                for (String key : device.getProperties().keySet()) {
                    // Проверяем только поля телеметрии, не трогаем другие свойства
                    if (key.startsWith("tb_") && !currentKeys.contains(key.substring(3))) {
                        keysToRemove.add(key);
                    }
                }
                
                // Удаляем отсутствующие поля телеметрии
                for (String key : keysToRemove) {
                    device.getProperties().remove(key);
                    telemetryUpdated = true;
                    log.info("Удалено отсутствующее свойство телеметрии {} устройства {}", 
                            key, device.getName());
                }
                
                // Проходим по каждому ключу в телеметрии и обновляем свойства устройства
                for (Map.Entry<String, Object> entry : telemetryData.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    
                    // Извлекаем значение из различных форматов телеметрии
                    String stringValue = extractValueFromTelemetry(value);
                    if (stringValue != null) {
                        // Сравниваем с текущим значением
                        // Используем префикс tb_ для различения полей телеметрии от других свойств
                        String propKey = "tb_" + key;
                        String currentValue = device.getProperties().get(propKey);
                        if (currentValue == null || !currentValue.equals(stringValue)) {
                            device.getProperties().put(propKey, stringValue);
                            telemetryUpdated = true;
                            log.info("Обновлена телеметрия {} устройства {}: {} -> {}", 
                                    key, device.getName(), currentValue, stringValue);
                        }
                    }
                }
                
                // Если были обновления, сохраняем устройство
                if (telemetryUpdated) {
                    deviceRepository.save(device);
                    log.info("Сохранены обновления телеметрии для устройства {}", device.getName());
                    return true;
                }
            } else {
                log.warn("Нет данных телеметрии для устройства {} из ThingsBoard. Код: {}, Тело: {}", 
                        device.getName(), telemetryResponse.getStatusCode(), telemetryResponse.getBody());
                
                // Пробуем получить конкретные ключи телеметрии
                updated = testAndUpdateTelemetryPost(device, "status", updated);
                updated = testAndUpdateTelemetryPost(device, "temperature", updated);
                updated = testAndUpdateTelemetryPost(device, "humidity", updated);
                updated = testAndUpdateTelemetryPost(device, "power", updated);
                updated = testAndUpdateTelemetryPost(device, "brightness", updated);
                updated = testAndUpdateTelemetryPost(device, "color", updated);
                updated = testAndUpdateTelemetryPost(device, "startTs", updated);
                updated = testAndUpdateTelemetryPost(device, "endTs", updated);
                updated = testAndUpdateTelemetryPost(device, "keys", updated);
                
                // Добавляем специфичные ключи для устройства
                updated = testAndUpdateTelemetryPost(device, "ewfwe", updated);
                
                // Если были обновления, сохраняем устройство
                if (updated) {
                    deviceRepository.save(device);
                    log.info("Сохранены обновления отдельных ключей телеметрии для устройства {}", device.getName());
                    return true;
                }
            }
            
            return updated;
        } catch (Exception e) {
            log.error("Ошибка при синхронизации данных устройства {} из ThingsBoard: {}", 
                    device.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Пробует получить и обновить конкретный ключ телеметрии для устройства
     * @param device Устройство
     * @param key Ключ телеметрии
     * @param currentlyUpdated Текущий статус обновления
     * @return true если произошло обновление
     */
    private boolean testAndUpdateTelemetryPost(Device device, String key, boolean currentlyUpdated) {
        try {
            // Для получения телеметрии используем API v2, который требует аутентификации
            if (!thingsBoardService.ensureAuthenticated()) {
                log.error("Не удалось аутентифицироваться в ThingsBoard для получения телеметрии");
                return false;
            }
            
            // Получаем deviceId от ThingsBoard
            String deviceId;
            
            // Если у устройства уже сохранен ThingsBoard ID
            if (device.getThingsboardDeviceId() != null && !device.getThingsboardDeviceId().isEmpty()) {
                deviceId = device.getThingsboardDeviceId();
            } else {
                // Если нет, получаем ID устройства по токену
                deviceId = thingsBoardService.getDeviceIdByToken(device.getThingsboardToken());
                if (deviceId == null) {
                    log.error("Не удалось получить ID устройства по токену: {}", device.getThingsboardToken());
                    return false;
                }
                // Сохраняем ID для будущих запросов
                device.setThingsboardDeviceId(deviceId);
                deviceRepository.save(device);
            }
            
            // Используем API v2 для получения последних значений телеметрии по конкретному ключу
            String specificUrl = thingsBoardUrl + "/api/plugins/telemetry/DEVICE/" + deviceId + "/values/timeseries?keys=" + key;
            log.info("Пробуем получить телеметрию {} для устройства {} через API v2: {}", key, device.getName(), specificUrl);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Authorization", "Bearer " + thingsBoardService.getAccessToken());
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    specificUrl,
                    HttpMethod.GET,
                    entity,
                    Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && 
                    response.getBody() != null && 
                    !response.getBody().isEmpty() && 
                    response.getBody().containsKey(key)) {
                
                Object value = response.getBody().get(key);
                log.info("Получено значение для ключа {}: {}", key, value);
                
                String stringValue = extractValueFromTelemetry(value);
                if (stringValue != null) {
                    // Используем префикс tb_ для различения полей телеметрии от других свойств
                    String propKey = "tb_" + key;
                    String currentValue = device.getProperties().get(propKey);
                    if (currentValue == null || !currentValue.equals(stringValue)) {
                        device.getProperties().put(propKey, stringValue);
                        log.info("Обновлена конкретная телеметрия {} устройства {}: {} -> {}", 
                                key, device.getName(), currentValue, stringValue);
                        return true;
                    }
                }
            }
            
            return false;
        } catch (Exception e) {
            log.debug("Не удалось получить телеметрию {} для устройства {}: {}", 
                    key, device.getName(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Вспомогательный метод для извлечения значения телеметрии из разных форматов данных
     */
    private String extractValueFromTelemetry(Object telemetryValue) {
        try {
            if (telemetryValue == null) {
                return null;
            }
            
            // Случай 1: прямое значение
            if (!(telemetryValue instanceof Map) && !(telemetryValue instanceof List)) {
                return telemetryValue.toString();
            }
            
            // Случай 2: список значений с timestamp
            if (telemetryValue instanceof List) {
                List<Object> values = (List<Object>) telemetryValue;
                if (!values.isEmpty()) {
                    Object firstValue = values.get(0);
                    if (firstValue instanceof Map) {
                        Map<String, Object> valueMap = (Map<String, Object>) firstValue;
                        if (valueMap.containsKey("value")) {
                            Object value = valueMap.get("value");
                            return value != null ? value.toString() : null;
                        } else if (valueMap.containsKey("ts") && valueMap.containsKey("value")) {
                            // Формат с временной меткой и значением
                            Object value = valueMap.get("value");
                            return value != null ? value.toString() : null;
                        }
                    } else {
                        // Если это просто список значений
                        return firstValue != null ? firstValue.toString() : null;
                    }
                }
            }
            
            // Случай 3: Значение внутри Map
            if (telemetryValue instanceof Map) {
                Map<String, Object> valueMap = (Map<String, Object>) telemetryValue;
                
                // В ThingsBoard v4 телеметрия в формате структуры "key": [{"ts": timestamp, "value": value}]
                for (Map.Entry<String, Object> entry : valueMap.entrySet()) {
                    Object entryValue = entry.getValue();
                    if (entryValue instanceof List && !((List<?>) entryValue).isEmpty()) {
                        List<Object> valueList = (List<Object>) entryValue;
                        Object firstValue = valueList.get(0);
                        
                        if (firstValue instanceof Map) {
                            Map<String, Object> firstValueMap = (Map<String, Object>) firstValue;
                            if (firstValueMap.containsKey("value")) {
                                return firstValueMap.get("value") != null ? 
                                       firstValueMap.get("value").toString() : null;
                            }
                        } else {
                            return firstValue != null ? firstValue.toString() : null;
                        }
                    }
                }
                
                if (valueMap.containsKey("value")) {
                    Object value = valueMap.get("value");
                    return value != null ? value.toString() : null;
                } else if (!valueMap.isEmpty()) {
                    // Если это какой-то другой формат Map, попробуем использовать первое значение
                    for (Object value : valueMap.values()) {
                        if (value != null) {
                            return value.toString();
                        }
                    }
                }
            }
            
            // Если не смогли извлечь значение, возвращаем строковое представление
            return telemetryValue.toString();
            
        } catch (Exception e) {
            log.warn("Ошибка при извлечении значения из телеметрии: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Принудительная синхронизация телеметрии всех устройств
     * Может быть вызвана из контроллера
     */
    public void forceSyncTelemetry() {
        syncTelemetryFromThingsBoard();
    }
    
    /**
     * Принудительная синхронизация всех данных устройства
     * @param deviceId ID устройства
     * @return true если синхронизация прошла успешно
     */
    public boolean forceSyncDevice(String deviceId) {
        Device device = deviceService.getDeviceById(java.util.UUID.fromString(deviceId)).orElse(null);
        if (device == null) {
            log.warn("Устройство с ID {} не найдено", deviceId);
            return false;
        }
        
        boolean attrUpdated = syncDeviceFromThingsBoard(device);
        boolean telemetryUpdated = syncDeviceTelemetryFromThingsBoard(device);
        
        return attrUpdated || telemetryUpdated;
    }
    
    /**
     * Импортирует устройства из ThingsBoard в локальную базу данных
     * @return количество импортированных устройств
     */
    public int importDevicesFromThingsBoard() {
        log.info("Запуск импорта устройств из ThingsBoard");
        
        // Получаем список всех устройств из ThingsBoard
        List<Map<String, Object>> tbDevices = thingsBoardService.getAllDevicesFromThingsBoard();
        
        if (tbDevices.isEmpty()) {
            log.warn("Нет устройств для импорта из ThingsBoard");
            return 0;
        }
        
        int importedCount = 0;
        
        for (Map<String, Object> tbDevice : tbDevices) {
            try {
                // Извлекаем основные данные устройства
                String name = tbDevice.get("name").toString();
                String type = tbDevice.get("type").toString();
                
                // Извлекаем ID устройства для получения токена
                String tbDeviceId = null;
                if (tbDevice.containsKey("id")) {
                    Map<String, Object> idObject = (Map<String, Object>) tbDevice.get("id");
                    if (idObject != null && idObject.containsKey("id")) {
                        tbDeviceId = idObject.get("id").toString();
                    }
                }
                
                if (tbDeviceId == null) {
                    log.warn("Невозможно получить ID устройства {} из ThingsBoard", name);
                    continue;
                }
                
                // Проверяем, есть ли уже такое устройство с таким токеном
                String token = thingsBoardService.getTokenByDeviceId(tbDeviceId);
                if (token == null) {
                    log.warn("Невозможно получить токен для устройства {} из ThingsBoard", name);
                    continue;
                }
                
                // Проверяем, существует ли устройство с таким токеном
                if (deviceRepository.findByThingsboardToken(token).isPresent()) {
                    log.debug("Устройство с токеном {} уже существует в базе", token);
                    continue;
                }
                
                // Создаем новое устройство
                Device device = new Device();
                device.setName(name);
                device.setType(type);
                device.setThingsboardToken(token);
                device.setThingsboardDeviceId(tbDeviceId); // Сохраняем ThingsBoard UUID
                device.setProtocol(ConnectionProtocol.VIRTUAL);
                device.setStatus(DeviceStatus.ONLINE);
                device.setLastSeen(LocalDateTime.now());
                
                // Дополнительные данные из ThingsBoard
                if (tbDevice.containsKey("label")) {
                    device.updateAttribute("label", tbDevice.get("label"));
                }
                
                // Получаем дополнительные атрибуты
                device = deviceRepository.save(device);
                
                // Синхронизируем атрибуты и телеметрию
                syncDeviceFromThingsBoard(device);
                syncDeviceTelemetryFromThingsBoard(device);
                
                importedCount++;
                log.info("Импортировано устройство: {}", name);
            } catch (Exception e) {
                log.error("Ошибка при импорте устройства из ThingsBoard: {}", e.getMessage(), e);
            }
        }
        
        log.info("Импорт устройств из ThingsBoard завершен. Импортировано: {}/{}", importedCount, tbDevices.size());
        return importedCount;
    }

    /**
     * Возвращает сервис устройств
     * @return DeviceService
     */
    public DeviceService getDeviceService() {
        return deviceService;
    }
} 