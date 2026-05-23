package com.efiscal.backend.controller;

import com.efiscal.backend.model.OrgEntity;
import com.efiscal.backend.repository.OrgRepository;
import com.efiscal.backend.security.AuthorizationService;
import com.efiscal.backend.service.MerchantProOrderService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/merchantpro/orders")
public class MerchantProOrderController {

    private final MerchantProOrderService merchantProOrderService;
    private final AuthorizationService authorizationService;
    private final OrgRepository orgRepository;

    public MerchantProOrderController(
        MerchantProOrderService merchantProOrderService,
        AuthorizationService authorizationService,
        OrgRepository orgRepository
    ) {
        this.merchantProOrderService = merchantProOrderService;
        this.authorizationService = authorizationService;
        this.orgRepository = orgRepository;
    }

    @GetMapping
    public Map<String, Object> fetchOrders(
        @RequestParam Long orgId,
        @RequestParam(required = false) String createdAfter,
        @RequestParam(required = false, defaultValue = "awaiting") String shippingStatus,
        @RequestParam(required = false, defaultValue = "0") int start,
        @RequestParam(required = false, defaultValue = "100") int limit
    ) {
        authorizationService.requireAction("MERCHANTPRO_FETCH_ORDERS");
        authorizationService.requireOrgAccess(orgId);

        int effectiveLimit = Math.min(Math.max(limit, 1), 100);
        MerchantProOrderService.OrderFetchResult result =
            merchantProOrderService.fetchOrders(orgId, createdAfter, shippingStatus, start, effectiveLimit);
        return Map.of(
            "data", result.data(),
            "meta", Map.of("total", result.total(), "start", start, "limit", effectiveLimit)
        );
    }
}
