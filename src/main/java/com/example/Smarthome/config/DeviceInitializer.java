package com.example.Smarthome.config;

import com.example.Smarthome.model.ConnectionProtocol;
import com.example.Smarthome.model.Device;
import com.example.Smarthome.model.DeviceStatus;
import com.example.Smarthome.model.Location;
import com.example.Smarthome.model.Room;
import com.example.Smarthome.repository.DeviceRepository;
import com.example.Smarthome.repository.LocationRepository;
import com.example.Smarthome.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Компонент для инициализации демонстрационных данных при запуске приложения
 * Запускается только если активен профиль "demo"
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("demo") // Добавлена аннотация, чтобы инициализатор запускался только при активном профиле "demo"
public class DeviceInitializer implements CommandLineRunner {

    private final DeviceRepository deviceRepository;
    private final LocationRepository locationRepository;
    private final RoomRepository roomRepository;

    @Override
    public void run(String... args) {
        // Проверяем, есть ли уже какие-то устройства
        if (deviceRepository.count() > 0) {
            log.info("База данных уже содержит устройства, пропускаем инициализацию");
            return;
        }

        log.info("Инициализация демонстрационных данных...");
        
        // Создаем тестовую локацию
        Location home = new Location();
        home.setName("Мой дом");
        home.setAddress("г. Москва, ул. Примерная, д. 123");
        home.setLatitude(55.755826);
        home.setLongitude(37.6173);
        locationRepository.save(home);
        
        // Создаем комнаты
        Room livingRoom = new Room();
        livingRoom.setName("Гостиная");
        livingRoom.setFloor("1");
        livingRoom.setArea(30.0);
        livingRoom.setLocation(home);
        roomRepository.save(livingRoom);
        
        Room kitchen = new Room();
        kitchen.setName("Кухня");
        kitchen.setFloor("1");
        kitchen.setArea(15.0);
        kitchen.setLocation(home);
        roomRepository.save(kitchen);
        
        Room bedroom = new Room();
        bedroom.setName("Спальня");
        bedroom.setFloor("2");
        bedroom.setArea(20.0);
        bedroom.setLocation(home);
        roomRepository.save(bedroom);
        
        Room bathroom = new Room();
        bathroom.setName("Ванная");
        bathroom.setFloor("2");
        bathroom.setArea(8.0);
        bathroom.setLocation(home);
        roomRepository.save(bathroom);
        
        // Создаем виртуальные устройства
        createLightDevice(livingRoom, "Люстра в гостиной", "Philips", "Hue White");
        createLightDevice(livingRoom, "Торшер у дивана", "Xiaomi", "Mi LED Smart Bulb");
        createLightDevice(kitchen, "Освещение на кухне", "IKEA", "TRÅDFRI");
        createLightDevice(bedroom, "Ночник в спальне", "TP-Link", "Kasa Smart");
        
        createThermostatDevice(livingRoom, "Термостат в гостиной", "Nest", "Learning Thermostat");
        createThermostatDevice(bedroom, "Термостат в спальне", "Ecobee", "SmartThermostat");
        
        createSensorDevice(livingRoom, "Датчик движения", "Aqara", "Motion Sensor");
        createSensorDevice(kitchen, "Датчик температуры на кухне", "Sonoff", "SNZB-02");
        createSensorDevice(bathroom, "Датчик влажности в ванной", "Eve", "Room");
        
        createSwitchDevice(livingRoom, "Умная розетка ТВ", "TP-Link", "Kasa Smart Plug");
        createSwitchDevice(kitchen, "Умная розетка холодильника", "Sonoff", "S31");
        
        log.info("Демонстрационные данные успешно инициализированы");
    }
    
    /**
     * Создает виртуальное устройство освещения
     */
    private void createLightDevice(Room room, String name, String manufacturer, String model) {
        Device device = new Device();
        device.setName(name);
        device.setType("light");
        device.setProtocol(ConnectionProtocol.VIRTUAL);
        device.setStatus(DeviceStatus.ONLINE);
        device.setLastSeen(LocalDateTime.now());
        device.setManufacturer(manufacturer);
        device.setModel(model);
        device.setFirmwareVersion("1.0.0");
        device.setRoom(room);
        device.setLocation(room.getLocation());
        
        // Добавляем возможности устройства
        device.getCapabilities().put("power", "on,off");
        device.getCapabilities().put("brightness", "0-100");
        device.getCapabilities().put("color", "rgb");
        
        // Начальные свойства
        device.getProperties().put("power", "off");
        device.getProperties().put("brightness", "0");
        device.getProperties().put("color", "FFFFFF");
        
        deviceRepository.save(device);
    }
    
    /**
     * Создает виртуальное устройство термостата
     */
    private void createThermostatDevice(Room room, String name, String manufacturer, String model) {
        Device device = new Device();
        device.setName(name);
        device.setType("thermostat");
        device.setProtocol(ConnectionProtocol.VIRTUAL);
        device.setStatus(DeviceStatus.ONLINE);
        device.setLastSeen(LocalDateTime.now());
        device.setManufacturer(manufacturer);
        device.setModel(model);
        device.setFirmwareVersion("2.1.0");
        device.setRoom(room);
        device.setLocation(room.getLocation());
        
        // Добавляем возможности устройства
        device.getCapabilities().put("power", "on,off");
        device.getCapabilities().put("mode", "heat,cool,auto,off");
        device.getCapabilities().put("temperature", "celsius");
        device.getCapabilities().put("target_temperature", "16-30");
        
        // Начальные свойства
        device.getProperties().put("power", "on");
        device.getProperties().put("mode", "heat");
        device.getProperties().put("temperature", "21.5");
        device.getProperties().put("target_temperature", "22.0");
        
        deviceRepository.save(device);
    }
    
    /**
     * Создает виртуальное устройство датчика
     */
    private void createSensorDevice(Room room, String name, String manufacturer, String model) {
        Device device = new Device();
        device.setName(name);
        device.setType("sensor");
        device.setProtocol(ConnectionProtocol.VIRTUAL);
        device.setStatus(DeviceStatus.ONLINE);
        device.setLastSeen(LocalDateTime.now());
        device.setManufacturer(manufacturer);
        device.setModel(model);
        device.setFirmwareVersion("1.2.3");
        device.setRoom(room);
        device.setLocation(room.getLocation());
        
        // Добавляем возможности устройства
        if (name.contains("движения")) {
            device.getCapabilities().put("motion", "detected,clear");
            device.getProperties().put("motion", "clear");
        }
        
        if (name.contains("температуры")) {
            device.getCapabilities().put("temperature", "celsius");
            device.getProperties().put("temperature", String.format("%.1f", 20 + Math.random() * 5));
        }
        
        if (name.contains("влажности")) {
            device.getCapabilities().put("humidity", "percent");
            device.getProperties().put("humidity", String.format("%.1f", 40 + Math.random() * 20));
        }
        
        // Общие свойства для всех датчиков
        device.getCapabilities().put("battery", "percent");
        device.getProperties().put("battery", String.valueOf(70 + (int)(Math.random() * 30)));
        
        deviceRepository.save(device);
    }
    
    /**
     * Создает виртуальное устройство умной розетки
     */
    private void createSwitchDevice(Room room, String name, String manufacturer, String model) {
        Device device = new Device();
        device.setName(name);
        device.setType("switch");
        device.setProtocol(ConnectionProtocol.VIRTUAL);
        device.setStatus(DeviceStatus.ONLINE);
        device.setLastSeen(LocalDateTime.now());
        device.setManufacturer(manufacturer);
        device.setModel(model);
        device.setFirmwareVersion("3.0.1");
        device.setRoom(room);
        device.setLocation(room.getLocation());
        
        // Добавляем возможности устройства
        device.getCapabilities().put("state", "on,off");
        device.getCapabilities().put("power_consumption", "watt");
        
        // Начальные свойства
        device.getProperties().put("state", "off");
        device.getProperties().put("power_consumption", "0");
        
        deviceRepository.save(device);
    }
} 