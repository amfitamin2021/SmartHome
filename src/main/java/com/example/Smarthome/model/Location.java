package com.example.Smarthome.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "locations")
@Data
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;
    private String address;
    
    // Геолокация
    private Double latitude;
    private Double longitude;
    
    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Room> rooms = new ArrayList<>();
    
    @OneToMany(mappedBy = "location")
    private List<Device> devices = new ArrayList<>();
} 