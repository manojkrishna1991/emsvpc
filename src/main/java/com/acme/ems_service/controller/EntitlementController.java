package com.acme.ems_service.controller;

import com.acme.ems_service.dto.EntitlementRequestDto;
import com.acme.ems_service.service.EntitlementService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EntitlementController {
  private final EntitlementService service;

  @GetMapping("/tenants/{tenantId}/features/{featureCode}")
  public Map<String, Object> check(@PathVariable UUID tenantId, @PathVariable String featureCode) {
    return service.check(tenantId, featureCode);
  }

  @PostMapping("/entitlements")
  public ResponseEntity<EntitlementRequestDto> saveEntitlement(@RequestBody EntitlementRequestDto entitlementRequestDto) {
    entitlementRequestDto = service.saveEntitlement(entitlementRequestDto);
    return ResponseEntity.ok(entitlementRequestDto);
  }

  @GetMapping("/health")
  public Map<String, String> health() {
    return Map.of("status", "UP");
  }
}