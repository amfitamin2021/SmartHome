package com.example.Smarthome.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "devices")
@Data
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;
    private String type;
    
    // Новые поля для категории и подтипа устройства
    private String category;
    private String subType;
    
    @Enumerated(EnumType.STRING)
    private ConnectionProtocol protocol;
    
    @Enumerated(EnumType.STRING)
    private DeviceStatus status = DeviceStatus.OFFLINE;
    
    private String connectionParams;
    private LocalDateTime lastSeen;
    
    // Динамические свойства устройства
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "device_properties", joinColumns = @JoinColumn(name = "device_id"))
    @MapKeyColumn(name = "property_name")
    @Column(name = "property_value")
    private Map<String, String> properties = new HashMap<>();
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "device_capabilities", joinColumns = @JoinColumn(name = "device_id"))
    @MapKeyColumn(name = "capability_name")
    @Column(name = "capability_value")
    private Map<String, String> capabilities = new HashMap<>();
    
    // Метаданные устройства
    private String manufacturer;
    private String model;
    @Column(name = "firmware_version")
    private String firmwareVersion;
    
    @Column(name = "thingsboard_token")
    private String thingsboardToken;
    private String thingsboardDeviceId; // UUID устройства в ThingsBoard
    
    // Связанные сущности
    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location location;
    
    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;
    
    // Дополнительные произвольные атрибуты
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "device_attributes", joinColumns = @JoinColumn(name = "device_id"))
    @MapKeyColumn(name = "attribute_name")
    @Column(name = "attribute_value")
    private Map<String, String> attributes = new HashMap<>();
    
    /**
     * Получает карту произвольных атрибутов устройства
     * @return Карта атрибутов
     */
    public Map<String, Object> getAttributes() {
        // Преобразуем Map<String, String> в Map<String, Object>
        Map<String, Object> result = new HashMap<>();
        attributes.forEach((key, value) -> result.put(key, value));
        return result;
    }
    
    /**
     * Обновляет значение произвольного атрибута
     * @param key Ключ атрибута
     * @param value Значение атрибута
     */
    public void updateAttribute(String key, Object value) {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        attributes.put(key, value != null ? value.toString() : null);
    }
} 