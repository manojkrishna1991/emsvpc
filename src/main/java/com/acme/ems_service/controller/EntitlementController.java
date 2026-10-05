package com.acme.ems_service.controller;

import com.acme.ems_service.service.EntitlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/v1") 
@RequiredArgsConstructor
public class EntitlementController {
  private final EntitlementService service;
  @GetMapping("/tenants/{tenantId}/features/{featureCode}")
  public Map<String,Object> check(@PathVariable UUID tenantId, @PathVariable String featureCode){ return service.check(tenantId, featureCode); }
  @GetMapping("/health") public Map<String,String> health(){ return Map.of("status","UP"); }
}