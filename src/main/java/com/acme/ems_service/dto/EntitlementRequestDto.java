package com.acme.ems_service.dto;

import java.util.List;

import com.acme.ems_service.model.Entitlement;

import lombok.Getter;
import lombok.Setter;
import tools.jackson.databind.ObjectMapper;
@Getter 
@Setter 
public class EntitlementRequestDto {
    private String shippingID;
    private String startDate;
    private String endDate;
    private String orderNumber;
    private String source;
    private String contractNumber;
    private List<AccountDTO> accounts;

    public EntitlementRequestDto toEntitlementRequestDto(Entitlement entitlement) {
        EntitlementRequestDto dto = new ObjectMapper().readValue(entitlement.getValue().toString(), EntitlementRequestDto.class);
        return dto;
    }
}
