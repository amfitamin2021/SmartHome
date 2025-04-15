package com.example.Smarthome.repository;

import com.example.Smarthome.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LocationRepository extends JpaRepository<Location, UUID> {
    Location findByName(String name);
} 