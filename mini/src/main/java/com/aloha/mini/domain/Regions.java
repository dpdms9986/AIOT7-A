package com.aloha.mini.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter 
@Setter
@AllArgsConstructor 
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "regions")
public class Regions {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long regionId;

    @Column(nullable = false, unique = true, length = 100)
    private String regionName;

    @Column(nullable = false)
    private Double centerLat;

    @Column(nullable = false)
    private Double centerLng;
}
