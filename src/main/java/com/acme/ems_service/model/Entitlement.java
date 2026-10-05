package com.acme.ems_service.model;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.UUID;
@Entity @Table(name="entitlements") @Data
public class Entitlement {
  @Id @GeneratedValue private UUID id;
  @Column(name="plan_id") private UUID planId;
  @Column(name="feature_id") private UUID featureId;
  @JdbcTypeCode(SqlTypes.JSON) private Object value;
}