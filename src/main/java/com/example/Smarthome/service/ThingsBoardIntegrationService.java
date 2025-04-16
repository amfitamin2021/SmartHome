package com.example.Smarthome.service;

import com.example.Smarthome.dto.AvailableDeviceDto;
import com.example.Smarthome.model.Device;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Сервис для интеграции с ThingsBoard
 * Позволяет создавать, обновлять и синхронизировать устройства с ThingsBoard
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ThingsBoardIntegrationService {

    private final RestTemplate restTemplate;
    private final DeviceRepository deviceRepository;
    
    @Value("${thingsboard.url}")
    private String thingsBoardUrl;
    
    @Value("${thingsboard.username}")
    private String thingsBoardUsername;
    
    @Value("${thingsboard.password}")
    private String thingsBoardPassword;
    
    private String accessToken;
    
    /**
     * Создает устройство в ThingsBoard
     * @param device Устройство для создания
     * @return true если устройство успешно создано
     */
    public boolean createDevice(Device device) {
        if (!ensureAuthenticated()) {
            log.error("Не удалось аутентифицироваться в ThingsBoard при создании устройства");
            return false;
        }
        
        try {
            String url = thingsBoardUrl + "/api/device";
            log.info("Создание устройства в ThingsBoard: {} по URL: {}", device.getName(), url);
            
            // Создаем запрос для ThingsBoard
            Map<String, Object> deviceRequest = new HashMap<>();
            deviceRequest.put("name", device.getName());
            deviceRequest.put("type", device.getType().toString());
            
            // Добавляем дополнительные атрибуты
            Map<String, Object> additionalInfo = new HashMap<>();
            additionalInfo.put("description", "Устройство из Smarthome: " + device.getName());
            additionalInfo.put("manufacturer", device.getManufacturer());
            additionalInfo.put("model", device.getModel());
            additionalInfo.put("firmwareVersion", device.getFirmwareVersion());
            deviceRequest.put("additionalInfo", additionalInfo);
            
            log.debug("Тело запроса: {}", deviceRequest);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Authorization", "Bearer " + accessToken);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(deviceRequest, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, 
                    HttpMethod.POST, 
                    entity, 
                    Map.class);
            
            log.info("Ответ от ThingsBoard при создании устройства. Код: {}", response.getStatusCode());
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Извлекаем ID устройства правильно
                Map<String, Object> responseBody = response.getBody();
                log.info("Ответ при создании устройства: {}", responseBody);
                
                // Получаем ID устройства из ответа
                String deviceId = null;
                if (responseBody.containsKey("id")) {
                    Map<String, String> idObject = (Map<String, String>) responseBody.get("id");
                    if (idObject != null && idObject.containsKey("id")) {
                        deviceId = idObject.get("id");
                    } else {
                        deviceId = responseBody.get("id").toString();
                    }
                }
                
                if (deviceId == null) {
                    log.error("Не удалось извлечь ID устройства из ответа: {}", responseBody);
                    return false;
                }
                
                log.info("Устройство {} создано в ThingsBoard с ID: {}", device.getName(), deviceId);
                
                // Сохраняем ThingsBoard UUID устройства
                device.setThingsboardDeviceId(deviceId);
                
                // Получаем токен доступа устройства
                String deviceToken = getDeviceCredentials(deviceId);
                if (deviceToken != null) {
                    device.setThingsboardToken(deviceToken);
                    log.info("Получен токен доступа для устройства {}: {}", device.getName(), deviceToken);
                    return true;
                } else {
                    log.error("Не удалось получить токен для созданного устройства {}", device.getName());
                }
            } else {
                log.error("Ответ от ThingsBoard не содержит данных или код ответа не успешный");
            }
            
            log.error("Ошибка при создании устройства {} в ThingsBoard. Код: {}, Тело: {}", 
                    device.getName(), response.getStatusCode(), response.getBody());
            return false;
        } catch (RestClientException e) {
            log.error("Ошибка при создании устройства {} в ThingsBoard: {}", 
                    device.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Обновляет устройство в ThingsBoard
     * @param device Устройство для обновления
     * @return true если устройство успешно обновлено
     */
    public boolean updateDevice(Device device) {
        if (!ensureAuthenticated() || device.getThingsboardToken() == null) {
            return false;
        }
        
        try {
            // Получаем ID устройства по токену
            String deviceId = getDeviceIdByToken(device.getThingsboardToken());
            if (deviceId == null) {
                log.error("Не удалось найти устройство в ThingsBoard по токену: {}", device.getThingsboardToken());
                return false;
            }
            
            // Сохраняем ThingsBoard Device ID
            if (device.getThingsboardDeviceId() == null || !device.getThingsboardDeviceId().equals(deviceId)) {
                device.setThingsboardDeviceId(deviceId);
            }
            
            String url = thingsBoardUrl + "/api/device/" + deviceId;
            
            // Создаем запрос для обновления
            Map<String, Object> deviceRequest = new HashMap<>();
            deviceRequest.put("name", device.getName());
            deviceRequest.put("type", device.getType().toString());
            deviceRequest.put("id", deviceId);
            
            // Обновляем атрибуты
            Map<String, Object> additionalInfo = new HashMap<>();
            additionalInfo.put("description", "Устройство из Smarthome: " + device.getName());
            additionalInfo.put("manufacturer", device.getManufacturer());
            additionalInfo.put("model", device.getModel());
            additionalInfo.put("firmwareVersion", device.getFirmwareVersion());
            deviceRequest.put("additionalInfo", additionalInfo);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Authorization", "Bearer " + accessToken);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(deviceRequest, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, 
                    HttpMethod.POST, 
                    entity, 
                    Map.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("Устройство {} обновлено в ThingsBoard", device.getName());
                
                // Обновляем атрибуты устройства
                boolean attributesUpdated = updateDeviceAttributesFromProperties(device);
                if (!attributesUpdated) {
                    log.warn("Не удалось обновить атрибуты устройства {} в ThingsBoard", device.getName());
                }
                
                // Отправляем телеметрию
                boolean telemetryUpdated = sendDeviceUpdate(device);
                if (!telemetryUpdated) {
                    log.warn("Не удалось отправить телеметрию устройства {} в ThingsBoard", device.getName());
                }
                
                return true;
            }
            
            log.error("Ошибка при обновлении устройства {} в ThingsBoard. Код: {}", 
                    device.getName(), response.getStatusCode());
            return false;
        } catch (RestClientException e) {
            log.error("Ошибка при обновлении устройства {} в ThingsBoard: {}", 
                    device.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Обновляет атрибуты устройства в ThingsBoard
     * @param device Устройство, атрибуты которого нужно обновить
     * @return true если атрибуты успешно обновлены
     */
    public boolean updateDeviceAttributes(Device device) {
        if (!ensureAuthenticated() || device.getThingsboardToken() == null) {
            return false;
        }
        
        try {
            String url = thingsBoardUrl + "/api/v1/" + device.getThingsboardToken() + "/attributes";
            
            // Создаем атрибуты устройства
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("manufacturer", device.getManufacturer());
            attributes.put("model", device.getModel());
            attributes.put("firmwareVersion", device.getFirmwareVersion());
            attributes.put("protocol", device.getProtocol().toString());
            
            // Добавляем capabilities в атрибуты
            if (device.getCapabilities() != null) {
                attributes.put("capabilities", device.getCapabilities());
            }
            
            // Отправляем атрибуты
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(attributes, headers);
            
            ResponseEntity<Void> response = restTemplate.exchange(
                    url, 
                    HttpMethod.POST, 
                    entity, 
                    Void.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("Атрибуты устройства {} обновлены в ThingsBoard", device.getName());
                return true;
            }
            
            log.error("Ошибка при обновлении атрибутов устройства {} в ThingsBoard. Код: {}", 
                    device.getName(), response.getStatusCode());
            return false;
        } catch (RestClientException e) {
            log.error("Ошибка при обновлении атрибутов устройства {} в ThingsBoard: {}", 
                    device.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Отправляет данные устройства в ThingsBoard
     * @param device Устройство, данные которого нужно отправить
     * @return true если данные успешно отправлены
     */
    public boolean sendDeviceUpdate(Device device) {
        if (device.getThingsboardToken() == null || device.getThingsboardToken().isEmpty()) {
            log.debug("Устройство {} не имеет токена ThingsBoard, пропускаем отправку", device.getName());
            return false;
        }
        
        try {
            String url = thingsBoardUrl + "/api/v1/" + device.getThingsboardToken() + "/telemetry";
            
            // Формируем телеметрию
            Map<String, Object> telemetry = new HashMap<>();
            
            // Добавляем только свойства телеметрии, исключая атрибуты
            for (Map.Entry<String, String> entry : device.getProperties().entrySet()) {
                String key = entry.getKey();
                // Исключаем атрибуты
                if (!key.startsWith("attr_")) {
                    // Для свойств телеметрии удаляем префикс tb_ при отправке
                    if (key.startsWith("tb_")) {
                        telemetry.put(key.substring(3), entry.getValue());
                    } else {
                        telemetry.put(key, entry.getValue());
                    }
                }
            }
            
            // Добавляем статус устройства
            telemetry.put("status", device.getStatus().toString());
            
            // Отправляем данные в ThingsBoard
            ResponseEntity<Void> response = restTemplate.postForEntity(url, telemetry, Void.class);
            
            log.debug("Данные устройства {} отправлены в ThingsBoard, ответ: {}", 
                    device.getName(), response.getStatusCode());
            
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            log.error("Ошибка при отправке данных устройства {} в ThingsBoard: {}", 
                    device.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Получает токен доступа для устройства
     * @param deviceId ID устройства
     * @return токен доступа или null в случае ошибки
     */
    private String getDeviceCredentials(String deviceId) {
        if (!ensureAuthenticated()) {
            log.error("Не удалось аутентифицироваться в ThingsBoard для получения токена устройства");
            return null;
        }
        
        try {
            // Формируем URL правильно, используя только ID устройства
            String url = thingsBoardUrl + "/api/device/" + deviceId + "/credentials";
            log.info("Запрашиваем токен устройства из ThingsBoard: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Authorization", "Bearer " + accessToken);
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, 
                    HttpMethod.GET, 
                    entity, 
                    Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Ответ при получении токена: {}", response.getBody());
                String token = (String) response.getBody().get("credentialsId");
                log.info("Получен токен устройства: {}", token);
                return token;
            }
            
            log.error("Ошибка при получении токена для устройства. Код: {}, Тело: {}", 
                     response.getStatusCode(), response.getBody());
            return null;
        } catch (RestClientException e) {
            log.error("Ошибка при получении токена доступа для устройства: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Метод для тестирования и получения существующего токена доступа
     * @param deviceName Имя устройства в ThingsBoard
     * @return токен доступа или null в случае ошибки
     */
    public String getTokenByDeviceName(String deviceName) {
        if (!ensureAuthenticated()) {
            log.error("Не удалось аутентифицироваться в ThingsBoard");
            return null;
        }
        
        try {
            // Сначала получим список всех устройств
            String url = thingsBoardUrl + "/api/tenant/devices?pageSize=100&page=0";
            log.info("Запрашиваем список устройств из ThingsBoard: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Authorization", "Bearer " + accessToken);
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, 
                    HttpMethod.GET, 
                    entity, 
                    Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Получен список устройств: {}", response.getBody());
                
                if (response.getBody().containsKey("data")) {
                    List<Map<String, Object>> devices = (List<Map<String, Object>>) response.getBody().get("data");
                    for (Map<String, Object> device : devices) {
                        String name = (String) device.get("name");
                        if (deviceName.equals(name)) {
                            // Нашли устройство, теперь получим его ID
                            Map<String, Object> idObj = (Map<String, Object>) device.get("id");
                            if (idObj != null && idObj.containsKey("id")) {
                                String deviceId = (String) idObj.get("id");
                                log.info("Найдено устройство с ID: {}", deviceId);
                                
                                // Получим токен
                                return getDeviceCredentials(deviceId);
                            }
                        }
                    }
                }
                
                log.error("Устройство с именем {} не найдено", deviceName);
            } else {
                log.error("Ошибка при получении списка устройств. Код: {}, Тело: {}", 
                         response.getStatusCode(), response.getBody());
            }
            
            return null;
        } catch (Exception e) {
            log.error("Ошибка при поиске устройства по имени: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Получает ID устройства по его токену
     * @param token токен устройства
     * @return ID устройства или null в случае ошибки
     */
    public String getDeviceIdByToken(String token) {
        if (!ensureAuthenticated()) {
            log.error("Не удалось аутентифицироваться в ThingsBoard");
            return null;
        }
        
        try {
            // Исправленный URL для получения учетных данных устройства
            String url = thingsBoardUrl + "/api/device/credentials?credentialsType=ACCESS_TOKEN&credentialsId=" + token;
            log.info("Запрашиваем ID устройства по токену из ThingsBoard: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Authorization", "Bearer " + accessToken);
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, 
                    HttpMethod.GET, 
                    entity, 
                    Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Ответ при получении ID устройства по токену: {}", response.getBody());
                
                if (response.getBody().containsKey("deviceId")) {
                    Map<String, Object> deviceIdObj = (Map<String, Object>) response.getBody().get("deviceId");
                    if (deviceIdObj != null && deviceIdObj.containsKey("id")) {
                        String deviceId = deviceIdObj.get("id").toString();
                        log.info("Получен ID устройства по токену: {}", deviceId);
                        return deviceId;
                    }
                }
                
                log.error("Не удалось извлечь ID устройства из ответа: {}", response.getBody());
            } else {
                // Попробуем использовать альтернативный метод - получить список устройств и найти по токену
                log.warn("Не удалось получить ID устройства через API учетных данных, пробуем получить через список устройств");
                return findDeviceIdByTokenFromDevicesList(token);
            }
            
            log.error("Ошибка при получении ID устройства по токену. Код: {}", response.getStatusCode());
            return null;
        } catch (RestClientException e) {
            log.error("Ошибка при получении ID устройства по токену: {}", e.getMessage(), e);
            // Попробуем альтернативный метод при ошибке
            log.warn("Пробуем получить ID устройства через список устройств");
            return findDeviceIdByTokenFromDevicesList(token);
        }
    }
    
    /**
     * Находит ID устройства по токену путем получения списка всех устройств
     * @param token токен устройства
     * @return ID устройства или null, если не найдено
     */
    private String findDeviceIdByTokenFromDevicesList(String token) {
        try {
            List<Map<String, Object>> devices = getAllDevicesFromThingsBoard();
            
            if (devices.isEmpty()) {
                log.error("Пустой список устройств из ThingsBoard");
                return null;
            }
            
            // Получаем учетные данные для каждого устройства по очереди
            for (Map<String, Object> device : devices) {
                if (device.containsKey("id")) {
                    Map<String, Object> idObj = (Map<String, Object>) device.get("id");
                    if (idObj != null && idObj.containsKey("id")) {
                        String deviceId = idObj.get("id").toString();
                        
                        // Получаем учетные данные устройства
                        String credentialsUrl = thingsBoardUrl + "/api/device/" + deviceId + "/credentials";
                        
                        HttpHeaders headers = new HttpHeaders();
                        headers.set("X-Authorization", "Bearer " + accessToken);
                        
                        HttpEntity<Void> entity = new HttpEntity<>(headers);
                        
                        ResponseEntity<Map> credResponse = restTemplate.exchange(
                                credentialsUrl, 
                                HttpMethod.GET, 
                                entity, 
                                Map.class);
                        
                        if (credResponse.getStatusCode().is2xxSuccessful() && credResponse.getBody() != null) {
                            if (credResponse.getBody().containsKey("credentialsId") && 
                                    token.equals(credResponse.getBody().get("credentialsId"))) {
                                log.info("Найден ID устройства по токену через список устройств: {}", deviceId);
                                return deviceId;
                            }
                        }
                    }
                }
            }
            
            log.error("Устройство с токеном {} не найдено в списке устройств", token);
            return null;
        } catch (Exception e) {
            log.error("Ошибка при поиске устройства по токену: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Выполняет аутентификацию в ThingsBoard
     * @return токен доступа или null в случае ошибки
     */
    public boolean ensureAuthenticated() {
        if (accessToken != null) {
            return true; // Предполагаем, что токен еще действителен
        }
        
        try {
            String url = thingsBoardUrl + "/api/auth/login";
            
            Map<String, String> loginRequest = new HashMap<>();
            loginRequest.put("username", thingsBoardUsername);
            loginRequest.put("password", thingsBoardPassword);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, loginRequest, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                accessToken = (String) response.getBody().get("token");
                log.debug("Успешная аутентификация в ThingsBoard");
                return true;
            }
            
            log.error("Ошибка аутентификации в ThingsBoard. Код: {}", response.getStatusCode());
            return false;
        } catch (RestClientException e) {
            log.error("Ошибка при аутентификации в ThingsBoard: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Планировщик для периодической отправки данных в ThingsBoard
     * Выполняется каждые 5 минут
     */
    @Scheduled(fixedRateString = "${thingsboard.sync.interval:300000}")
    public void syncDevicesWithThingsBoard() {
        log.debug("Запуск автоматической синхронизации устройств с ThingsBoard");
        
        // Получаем устройства с токенами ThingsBoard
        List<Device> devices = deviceRepository.findAllByThingsboardTokenIsNotNull();
        
        if (devices.isEmpty()) {
            log.debug("Нет устройств с токенами ThingsBoard для синхронизации");
            return;
        }
        
        log.info("Синхронизация {} устройств с ThingsBoard", devices.size());
        
        int successCount = 0;
        for (Device device : devices) {
            try {
                boolean success = sendDeviceUpdate(device);
                if (success) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("Ошибка при синхронизации устройства {} с ThingsBoard: {}", 
                        device.getName(), e.getMessage());
            }
        }
        
        log.info("Синхронизация завершена. Успешно: {}/{}", successCount, devices.size());
    }

    /**
     * Получает токен устройства по его ID в ThingsBoard
     * @param deviceId ID устройства в ThingsBoard
     * @return токен устройства или null в случае ошибки
     */
    public String getTokenByDeviceId(String deviceId) {
        if (!ensureAuthenticated()) {
            log.error("Не удалось аутентифицироваться в ThingsBoard");
            return null;
        }
        
        try {
            // Проверяем, что deviceId содержит только UUID часть, если нет - извлекаем
            if (deviceId.contains("entityType") && deviceId.contains("id")) {
                // Это объект в формате {entityType=DEVICE, id=XXX}
                log.debug("Пытаемся извлечь UUID из: {}", deviceId);
                int idIndex = deviceId.indexOf("id=");
                if (idIndex > 0) {
                    String tmp = deviceId.substring(idIndex + 3);
                    int endIndex = tmp.indexOf(",");
                    if (endIndex < 0) endIndex = tmp.indexOf("}");
                    if (endIndex > 0) {
                        deviceId = tmp.substring(0, endIndex).trim();
                        log.debug("Извлечен ID устройства: {}", deviceId);
                    }
                }
            }
            
            log.debug("Получение токена устройства по ID: {}", deviceId);
            return getDeviceCredentials(deviceId);
        } catch (Exception e) {
            log.error("Ошибка при получении токена устройства по ID: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Получает список всех устройств из ThingsBoard
     * @return список устройств или пустой список в случае ошибки
     */
    public List<Map<String, Object>> getAllDevicesFromThingsBoard() {
        if (!ensureAuthenticated()) {
            log.error("Не удалось аутентифицироваться в ThingsBoard");
            return List.of();
        }
        
        try {
            // Получаем список всех устройств из ThingsBoard
            String url = thingsBoardUrl + "/api/tenant/devices?pageSize=100&page=0";
            log.info("Запрашиваем список устройств из ThingsBoard: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Authorization", "Bearer " + accessToken);
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, 
                    HttpMethod.GET, 
                    entity, 
                    Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                if (response.getBody().containsKey("data")) {
                    List<Map<String, Object>> devices = (List<Map<String, Object>>) response.getBody().get("data");
                    log.info("Получено {} устройств из ThingsBoard", devices.size());
                    return devices;
                }
            }
            
            log.error("Ошибка при получении списка устройств из ThingsBoard. Код: {}", response.getStatusCode());
            return List.of();
        } catch (Exception e) {
            log.error("Ошибка при получении списка устройств из ThingsBoard: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Получает текущий JWT токен авторизации для взаимодействия с ThingsBoard
     * @return текущий токен авторизации или null, если не авторизован
     */
    public String getAccessToken() {
        ensureAuthenticated();
        return accessToken;
    }

    /**
     * Обновляет атрибуты устройства в ThingsBoard на основе свойств с префиксами
     * @param device Устройство, атрибуты которого нужно обновить
     * @return true если атрибуты успешно обновлены
     */
    public boolean updateDeviceAttributesFromProperties(Device device) {
        if (device.getThingsboardToken() == null || device.getThingsboardToken().isEmpty()) {
            log.debug("Устройство {} не имеет токена ThingsBoard, пропускаем обновление атрибутов", device.getName());
            return false;
        }
        
        try {
            // Обновляем клиентские атрибуты
            Map<String, Object> clientAttributes = new HashMap<>();
            for (Map.Entry<String, String> entry : device.getProperties().entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("attr_client_")) {
                    clientAttributes.put(key.substring("attr_client_".length()), entry.getValue());
                }
            }
            
            if (!clientAttributes.isEmpty()) {
                String clientAttrUrl = thingsBoardUrl + "/api/v1/" + device.getThingsboardToken() + "/attributes";
                log.debug("Отправляем клиентские атрибуты устройства {} в ThingsBoard: {}", device.getName(), clientAttributes);
                
                ResponseEntity<Void> clientResponse = restTemplate.postForEntity(clientAttrUrl, clientAttributes, Void.class);
                
                if (!clientResponse.getStatusCode().is2xxSuccessful()) {
                    log.error("Ошибка при обновлении клиентских атрибутов устройства {} в ThingsBoard. Код: {}", 
                            device.getName(), clientResponse.getStatusCode());
                    return false;
                }
            }
            
            // Обновляем shared и server атрибуты через серверный API
            Map<String, Object> sharedAttributes = new HashMap<>();
            Map<String, Object> serverAttributes = new HashMap<>();
            
            for (Map.Entry<String, String> entry : device.getProperties().entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("attr_shared_")) {
                    sharedAttributes.put(key.substring("attr_shared_".length()), entry.getValue());
                } else if (key.startsWith("attr_server_")) {
                    serverAttributes.put(key.substring("attr_server_".length()), entry.getValue());
                }
            }
            
            boolean success = true;
            
            // Для обновления shared и server атрибутов требуется аутентификация
            if ((!sharedAttributes.isEmpty() || !serverAttributes.isEmpty()) && ensureAuthenticated()) {
                // Получаем ThingsBoard Device ID
                String deviceId;
                if (device.getThingsboardDeviceId() != null && !device.getThingsboardDeviceId().isEmpty()) {
                    deviceId = device.getThingsboardDeviceId();
                } else {
                    deviceId = getDeviceIdByToken(device.getThingsboardToken());
                    if (deviceId != null) {
                        device.setThingsboardDeviceId(deviceId);
                    } else {
                        log.error("Не удалось получить ThingsBoard ID для устройства {}", device.getName());
                        return false;
                    }
                }
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("X-Authorization", "Bearer " + accessToken);
                
                // Обновляем shared атрибуты
                if (!sharedAttributes.isEmpty()) {
                    String sharedAttrUrl = thingsBoardUrl + "/api/plugins/telemetry/DEVICE/" + deviceId + "/SHARED_SCOPE";
                    log.debug("Отправляем shared атрибуты устройства {} в ThingsBoard: {}", device.getName(), sharedAttributes);
                    
                    HttpEntity<Map<String, Object>> sharedEntity = new HttpEntity<>(sharedAttributes, headers);
                    
                    ResponseEntity<Void> sharedResponse = restTemplate.exchange(
                            sharedAttrUrl,
                            HttpMethod.POST,
                            sharedEntity,
                            Void.class);
                    
                    if (!sharedResponse.getStatusCode().is2xxSuccessful()) {
                        log.error("Ошибка при обновлении shared атрибутов устройства {} в ThingsBoard. Код: {}", 
                                device.getName(), sharedResponse.getStatusCode());
                        success = false;
                    }
                }
                
                // Обновляем server атрибуты
                if (!serverAttributes.isEmpty()) {
                    String serverAttrUrl = thingsBoardUrl + "/api/plugins/telemetry/DEVICE/" + deviceId + "/SERVER_SCOPE";
                    log.debug("Отправляем server атрибуты устройства {} в ThingsBoard: {}", device.getName(), serverAttributes);
                    
                    HttpEntity<Map<String, Object>> serverEntity = new HttpEntity<>(serverAttributes, headers);
                    
                    ResponseEntity<Void> serverResponse = restTemplate.exchange(
                            serverAttrUrl,
                            HttpMethod.POST,
                            serverEntity,
                            Void.class);
                    
                    if (!serverResponse.getStatusCode().is2xxSuccessful()) {
                        log.error("Ошибка при обновлении server атрибутов устройства {} в ThingsBoard. Код: {}", 
                                device.getName(), serverResponse.getStatusCode());
                        success = false;
                    }
                }
            }
            
            return success;
        } catch (RestClientException e) {
            log.error("Ошибка при обновлении атрибутов устройства {} в ThingsBoard: {}", 
                    device.getName(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Обновляет серверные атрибуты устройства в ThingsBoard
     * @param device Устройство
     * @param attributes Атрибуты для обновления
     * @return true если атрибуты успешно обновлены
     */
    public boolean updateServerAttributes(Device device, Map<String, String> attributes) {
        if (!ensureAuthenticated() || device.getThingsboardToken() == null) {
            return false;
        }
        
        try {
            // Получаем ID устройства по токену
            String deviceId = getDeviceIdByToken(device.getThingsboardToken());
            if (deviceId == null) {
                log.error("Не удалось найти устройство в ThingsBoard по токену: {}", device.getThingsboardToken());
                return false;
            }
            
            // Сохраняем ThingsBoard Device ID
            if (device.getThingsboardDeviceId() == null || !device.getThingsboardDeviceId().equals(deviceId)) {
                device.setThingsboardDeviceId(deviceId);
            }
            
            // Используем API для сущностей сервера
            String url = thingsBoardUrl + "/api/plugins/telemetry/DEVICE/" + deviceId + "/attributes/SERVER_SCOPE";
            log.info("Отправка серверных атрибутов на URL: {}", url);
            log.info("Атрибуты для обновления: {}", attributes);
            
            // В ThingsBoard сервере ожидаются атрибуты в виде JSON-объекта
            // Преобразуем строковые значения в соответствующие типы данных
            Map<String, Object> processedAttributes = new HashMap<>();
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                
                // Пытаемся преобразовать строковые значения в типизированные
                if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                    processedAttributes.put(key, Boolean.parseBoolean(value));
                } else if (value.matches("^\\d+$")) {
                    processedAttributes.put(key, Integer.parseInt(value));
                } else if (value.matches("^\\d+\\.\\d+$")) {
                    processedAttributes.put(key, Double.parseDouble(value));
                } else {
                    processedAttributes.put(key, value);
                }
            }
            
            log.info("Обработанные атрибуты: {}", processedAttributes);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Authorization", "Bearer " + accessToken);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(processedAttributes, headers);
            
            ResponseEntity<Void> response = restTemplate.exchange(
                    url, 
                    HttpMethod.POST, 
                    entity, 
                    Void.class);
            
            log.info("Ответ от ThingsBoard: статус {}", response.getStatusCode());
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Серверные атрибуты устройства {} обновлены в ThingsBoard", device.getName());
                return true;
            }
            
            log.error("Ошибка при обновлении серверных атрибутов устройства {} в ThingsBoard. Код: {}", 
                    device.getName(), response.getStatusCode());
            return false;
        } catch (RestClientException e) {
            log.error("Ошибка при обновлении серверных атрибутов устройства {} в ThingsBoard: {}", 
                    device.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Обновляет клиентские атрибуты устройства в ThingsBoard
     * @param device Устройство
     * @param attributes Атрибуты для обновления
     * @return true если атрибуты успешно обновлены
     */
    public boolean updateClientAttributes(Device device, Map<String, String> attributes) {
        if (device.getThingsboardToken() == null) {
            return false;
        }
        
        try {
            String url = thingsBoardUrl + "/api/v1/" + device.getThingsboardToken() + "/attributes";
            
            ResponseEntity<Void> response = restTemplate.postForEntity(url, attributes, Void.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("Клиентские атрибуты устройства {} обновлены в ThingsBoard", device.getName());
                return true;
            }
            
            log.error("Ошибка при обновлении клиентских атрибутов устройства {} в ThingsBoard. Код: {}", 
                    device.getName(), response.getStatusCode());
            return false;
        } catch (RestClientException e) {
            log.error("Ошибка при обновлении клиентских атрибутов устройства {} в ThingsBoard: {}", 
                    device.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Обновляет shared атрибуты устройства в ThingsBoard
     * @param device Устройство
     * @param attributes Атрибуты для обновления
     * @return true если атрибуты успешно обновлены
     */
    public boolean updateSharedAttributes(Device device, Map<String, String> attributes) {
        if (!ensureAuthenticated() || device.getThingsboardToken() == null) {
            return false;
        }
        
        try {
            // Получаем ID устройства по токену
            String deviceId = getDeviceIdByToken(device.getThingsboardToken());
            if (deviceId == null) {
                log.error("Не удалось найти устройство в ThingsBoard по токену: {}", device.getThingsboardToken());
                return false;
            }
            
            // Сохраняем ThingsBoard Device ID
            if (device.getThingsboardDeviceId() == null || !device.getThingsboardDeviceId().equals(deviceId)) {
                device.setThingsboardDeviceId(deviceId);
            }
            
            String url = thingsBoardUrl + "/api/plugins/telemetry/DEVICE/" + deviceId + "/SHARED_SCOPE";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Authorization", "Bearer " + accessToken);
            
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(attributes, headers);
            
            ResponseEntity<Void> response = restTemplate.exchange(
                    url, 
                    HttpMethod.POST, 
                    entity, 
                    Void.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("Shared атрибуты устройства {} обновлены в ThingsBoard", device.getName());
                return true;
            }
            
            log.error("Ошибка при обновлении shared атрибутов устройства {} в ThingsBoard. Код: {}", 
                    device.getName(), response.getStatusCode());
            return false;
        } catch (RestClientException e) {
            log.error("Ошибка при обновлении shared атрибутов устройства {} в ThingsBoard: {}", 
                    device.getName(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Получает список доступных устройств из ThingsBoard
     * @return список доступных устройств
     */
    public List<AvailableDeviceDto> getAvailableDevices() {
        List<Map<String, Object>> thingsBoardDevices = getAllDevicesFromThingsBoard();
        List<AvailableDeviceDto> availableDevices = new ArrayList<>();
        
        for (Map<String, Object> device : thingsBoardDevices) {
            try {
                String deviceId = extractDeviceId(device);
                if (deviceId == null) {
                    log.error("Не удалось извлечь ID устройства из ответа ThingsBoard");
                    continue;
                }
                
                // Получаем токен устройства
                String token = getDeviceCredentials(deviceId);
                
                // Извлекаем информацию об устройстве
                String name = (String) device.getOrDefault("name", "");
                String type = (String) device.getOrDefault("type", "");
                
                // Получаем дополнительные атрибуты, если они есть
                String manufacturer = "";
                String model = "";
                String firmwareVersion = "";
                
                Map<String, Object> additionalInfo = (Map<String, Object>) device.get("additionalInfo");
                if (additionalInfo != null) {
                    manufacturer = (String) additionalInfo.getOrDefault("manufacturer", "");
                    model = (String) additionalInfo.getOrDefault("model", "");
                    firmwareVersion = (String) additionalInfo.getOrDefault("firmwareVersion", "");
                }
                
                // Создаем DTO устройства
                AvailableDeviceDto deviceDto = AvailableDeviceDto.builder()
                    .id(deviceId)
                    .name(name)
                    .type(type)
                    .token(token)
                    .manufacturer(manufacturer)
                    .model(model)
                    .firmwareVersion(firmwareVersion)
                    .build();
                
                availableDevices.add(deviceDto);
            } catch (Exception e) {
                log.error("Ошибка при обработке устройства из ThingsBoard: {}", e.getMessage(), e);
            }
        }
        
        return availableDevices;
    }
    
    /**
     * Извлекает ID устройства из JSON ответа ThingsBoard
     * @param device данные устройства из ThingsBoard
     * @return ID устройства или null в случае ошибки
     */
    private String extractDeviceId(Map<String, Object> device) {
        if (device.containsKey("id")) {
            Map<String, Object> idObj = (Map<String, Object>) device.get("id");
            if (idObj != null && idObj.containsKey("id")) {
                return (String) idObj.get("id");
            }
        }
        return null;
    }

    /**
     * Отправляет данные телеметрии устройства в ThingsBoard
     * @param device устройство
     * @param telemetryData данные телеметрии в формате ключ-значение
     * @return true если телеметрия успешно отправлена
     */
    public boolean sendTelemetry(Device device, Map<String, Object> telemetryData) {
        if (!ensureAuthenticated() || 
            device.getThingsboardToken() == null || 
            device.getThingsboardDeviceId() == null) {
            log.error("Невозможно отправить телеметрию: нет авторизации или отсутствуют данные устройства");
            return false;
        }
        
        try {
            String deviceToken = device.getThingsboardToken();
            log.info("Отправка телеметрии для устройства {} с токеном {}", device.getName(), deviceToken);
            
            // Создаем JSON запрос для отправки телеметрии
            String telemetryUrl = thingsBoardUrl + "/api/v1/" + deviceToken + "/telemetry";
            log.info("URL для отправки телеметрии: {}", telemetryUrl);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(telemetryData, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    telemetryUrl,
                    HttpMethod.POST,
                    entity,
                    String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Телеметрия успешно отправлена для устройства {}", device.getName());
                return true;
            } else {
                log.error("Ошибка при отправке телеметрии. Код: {}, Ответ: {}", 
                          response.getStatusCode(), response.getBody());
                return false;
            }
        } catch (Exception e) {
            log.error("Ошибка при отправке телеметрии для устройства {}: {}", 
                      device.getName(), e.getMessage(), e);
            return false;
        }
    }
} 