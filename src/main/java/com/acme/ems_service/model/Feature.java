package com.acme.ems_service.model;
import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;
@Entity @Table(name="features") @Data
public class Feature {
  @Id @GeneratedValue private UUID id;
  @Column(unique=true) private String code;
  private String name;
  private String type; // BOOLEAN, NUMERIC, LIMIT
}