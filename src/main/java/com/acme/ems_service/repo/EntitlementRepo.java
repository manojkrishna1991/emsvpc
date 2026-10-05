package com.acme.ems_service.repo;

import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface EntitlementRepo extends JpaRepository<com.acme.ems_service.model.Entitlement, UUID> {
    @Query(value = "SELECT f.code as code, e.value as value, f.type as type FROM entitlements e JOIN features f ON f.id=e.feature_id JOIN tenant_subscriptions ts ON ts.plan_id=e.plan_id WHERE ts.tenant_id=:tenantId AND f.code=:featureCode", nativeQuery = true)
    List<Map<String, Object>> findEntitlement(UUID tenantId, String featureCode);
}
