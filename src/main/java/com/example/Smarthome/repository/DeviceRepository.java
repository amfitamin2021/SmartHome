package com.example.Smarthome.repository;

import com.example.Smarthome.model.ConnectionProtocol;
import com.example.Smarthome.model.Device;
import com.example.Smarthome.model.DeviceStatus;
import com.example.Smarthome.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {
    
    List<Device> findByStatus(DeviceStatus status);
    
    List<Device> findByProtocol(ConnectionProtocol protocol);
    
    List<Device> findByRoom(Room room);
    
    List<Device> findByType(String type);
    
    /**
     * Находит все устройства, которые имеют установленный токен ThingsBoard
     * @return список устройств с ненулевым токеном ThingsBoard
     */
    @Query("SELECT d FROM Device d WHERE d.thingsboardToken IS NOT NULL AND d.thingsboardToken <> ''")
    List<Device> findAllByThingsboardTokenIsNotNull();
    
    /**
     * Находит устройство по его токену ThingsBoard
     * @param token токен устройства в ThingsBoard
     * @return Optional с устройством, если найдено
     */
    Optional<Device> findByThingsboardToken(String token);
} 