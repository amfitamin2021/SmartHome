package com.example.Smarthome.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "rooms")
@Data
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;
    private String floor;
    private Double area;
    
    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location location;
    
    @OneToMany(mappedBy = "room")
    private List<Device> devices = new ArrayList<>();
} 