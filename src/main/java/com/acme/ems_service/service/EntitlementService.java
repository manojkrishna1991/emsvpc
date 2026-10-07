package com.acme.ems_service.service;
import com.acme.ems_service.dto.EntitlementRequestDto;
import com.acme.ems_service.model.Entitlement;
import com.acme.ems_service.repo.EntitlementRepo;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import java.util.*;
@Service 
@RequiredArgsConstructor
public class EntitlementService {
  private final EntitlementRepo repo;
  public Map<String,Object> check(UUID tenantId, String featureCode) {
    var rows = repo.findEntitlement(tenantId, featureCode);
    if(rows.isEmpty()) return Map.of("tenantId",tenantId,"feature",featureCode,"allowed",false,"reason","not entitled");
    var row = rows.get(0);
    return Map.of("tenantId",tenantId,"feature",row.get("code"),"allowed",true,"value",row.get("value"),"type",row.get("type"));
  }
  public EntitlementRequestDto saveEntitlement(EntitlementRequestDto entitlementRequestDto) {
    Entitlement entitlement = new Entitlement();
    entitlement.setValue(new ObjectMapper().writeValueAsString(entitlementRequestDto));
    Entitlement savedEntitlement = repo.save(entitlement);
    return entitlementRequestDto.toEntitlementRequestDto(savedEntitlement);
  }
}
