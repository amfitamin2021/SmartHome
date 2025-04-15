package com.example.Smarthome.service;

import com.example.Smarthome.model.Location;
import com.example.Smarthome.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService {
    
    private final LocationRepository locationRepository;
    
    public List<Location> getAllLocations() {
        return locationRepository.findAll();
    }
    
    public Optional<Location> getLocationById(UUID id) {
        return locationRepository.findById(id);
    }
    
    public Location saveLocation(Location location) {
        return locationRepository.save(location);
    }
    
    public void deleteLocation(UUID id) {
        locationRepository.deleteById(id);
    }
    
    public Location findByName(String name) {
        return locationRepository.findByName(name);
    }
} 