package com.example.Smarthome.controller;

import com.example.Smarthome.dto.LocationDto;
import com.example.Smarthome.dto.RoomDto;
import com.example.Smarthome.model.Device;
import com.example.Smarthome.model.Location;
import com.example.Smarthome.model.Room;
import com.example.Smarthome.repository.DeviceRepository;
import com.example.Smarthome.repository.RoomRepository;
import com.example.Smarthome.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
@Slf4j
public class LocationController {

    private final LocationService locationService;
    private final RoomRepository roomRepository;
    private final DeviceRepository deviceRepository;

    /**
     * Получение списка всех локаций
     */
    @GetMapping
    public ResponseEntity<List<LocationDto>> getAllLocations() {
        List<Location> locations = locationService.getAllLocations();
        List<LocationDto> locationDtos = locations.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(locationDtos);
    }

    /**
     * Получение конкретной локации по ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<LocationDto> getLocation(@PathVariable UUID id) {
        Location location = locationService.getLocationById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Локация с ID " + id + " не найдена"));
        return ResponseEntity.ok(convertToDto(location));
    }

    /**
     * Создание новой локации
     */
    @PostMapping
    public ResponseEntity<LocationDto> createLocation(@RequestBody LocationDto locationDto) {
        Location location = new Location();
        location.setName(locationDto.getName());
        location.setAddress(locationDto.getAddress());
        location.setLatitude(locationDto.getLatitude());
        location.setLongitude(locationDto.getLongitude());
        
        Location savedLocation = locationService.saveLocation(location);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDto(savedLocation));
    }

    /**
     * Обновление локации
     */
    @PutMapping("/{id}")
    public ResponseEntity<LocationDto> updateLocation(
            @PathVariable UUID id, 
            @RequestBody LocationDto locationDto) {
        
        Location location = locationService.getLocationById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Локация с ID " + id + " не найдена"));
        
        location.setName(locationDto.getName());
        location.setAddress(locationDto.getAddress());
        location.setLatitude(locationDto.getLatitude());
        location.setLongitude(locationDto.getLongitude());
        
        Location updatedLocation = locationService.saveLocation(location);
        return ResponseEntity.ok(convertToDto(updatedLocation));
    }

    /**
     * Удаление локации
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLocation(@PathVariable UUID id) {
        if (!locationService.getLocationById(id).isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                    "Локация с ID " + id + " не найдена");
        }
        
        // Перед удалением получаем все устройства этой локации и сбрасываем ссылку на локацию
        List<Device> devices = deviceRepository.findAll().stream()
                .filter(device -> device.getLocation() != null && device.getLocation().getId().equals(id))
                .collect(Collectors.toList());
        
        for (Device device : devices) {
            device.setLocation(null);
            deviceRepository.save(device);
        }
        
        locationService.deleteLocation(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Получение всех комнат локации
     */
    @GetMapping("/{locationId}/rooms")
    public ResponseEntity<List<RoomDto>> getRooms(@PathVariable UUID locationId) {
        Location location = locationService.getLocationById(locationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Локация с ID " + locationId + " не найдена"));
        
        List<Room> rooms = roomRepository.findByLocation(location);
        List<RoomDto> roomDtos = rooms.stream()
                .map(this::convertToRoomDto)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(roomDtos);
    }

    /**
     * Добавление комнаты в локацию
     */
    @PostMapping("/{locationId}/rooms")
    public ResponseEntity<RoomDto> addRoom(
            @PathVariable UUID locationId,
            @RequestBody RoomDto roomDto) {
        
        Location location = locationService.getLocationById(locationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Локация с ID " + locationId + " не найдена"));
        
        Room room = new Room();
        room.setName(roomDto.getName());
        room.setFloor(roomDto.getFloor());
        room.setArea(roomDto.getArea());
        room.setLocation(location);
        
        Room savedRoom = roomRepository.save(room);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToRoomDto(savedRoom));
    }

    /**
     * Обновление комнаты
     */
    @PutMapping("/{locationId}/rooms/{roomId}")
    public ResponseEntity<RoomDto> updateRoom(
            @PathVariable UUID locationId,
            @PathVariable UUID roomId,
            @RequestBody RoomDto roomDto) {
        
        Location location = locationService.getLocationById(locationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Локация с ID " + locationId + " не найдена"));
        
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Комната с ID " + roomId + " не найдена"));
        
        if (!room.getLocation().getId().equals(locationId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Комната не принадлежит указанной локации");
        }
        
        room.setName(roomDto.getName());
        room.setFloor(roomDto.getFloor());
        room.setArea(roomDto.getArea());
        
        Room updatedRoom = roomRepository.save(room);
        return ResponseEntity.ok(convertToRoomDto(updatedRoom));
    }

    /**
     * Удаление комнаты
     */
    @DeleteMapping("/{locationId}/rooms/{roomId}")
    public ResponseEntity<Void> deleteRoom(
            @PathVariable UUID locationId,
            @PathVariable UUID roomId) {
        
        if (!locationService.getLocationById(locationId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                    "Локация с ID " + locationId + " не найдена");
        }
        
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Комната с ID " + roomId + " не найдена"));
        
        if (!room.getLocation().getId().equals(locationId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Комната не принадлежит указанной локации");
        }
        
        // Сбрасываем ссылки на эту комнату у всех устройств
        List<Device> devices = deviceRepository.findByRoom(room);
        for (Device device : devices) {
            device.setRoom(null);
            deviceRepository.save(device);
        }
        
        roomRepository.deleteById(roomId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Конвертация модели локации в DTO
     */
    private LocationDto convertToDto(Location location) {
        LocationDto dto = new LocationDto();
        dto.setId(location.getId());
        dto.setName(location.getName());
        dto.setAddress(location.getAddress());
        dto.setLatitude(location.getLatitude());
        dto.setLongitude(location.getLongitude());
        
        // Считаем количество устройств в локации
        long deviceCount = location.getDevices().size();
        dto.setDeviceCount(deviceCount);
        
        // Считаем количество комнат
        long roomCount = location.getRooms().size();
        dto.setRoomCount(roomCount);
        
        return dto;
    }

    /**
     * Конвертация модели комнаты в DTO
     */
    private RoomDto convertToRoomDto(Room room) {
        RoomDto dto = new RoomDto();
        dto.setId(room.getId());
        dto.setName(room.getName());
        dto.setFloor(room.getFloor());
        dto.setArea(room.getArea());
        
        if (room.getLocation() != null) {
            dto.setLocationId(room.getLocation().getId());
            dto.setLocationName(room.getLocation().getName());
        }
        
        // Считаем количество устройств в комнате
        long deviceCount = room.getDevices().size();
        dto.setDeviceCount(deviceCount);
        
        return dto;
    }
} 