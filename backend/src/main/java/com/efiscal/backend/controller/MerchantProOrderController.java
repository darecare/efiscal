package com.efiscal.backend.controller;

import com.efiscal.backend.service.MerchantProOrderService;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/merchantpro/orders")
public class MerchantProOrderController {

    private final MerchantProOrderService merchantProOrderService;

    public MerchantProOrderController(MerchantProOrderService merchantProOrderService) {
        this.merchantProOrderService = merchantProOrderService;
    }

    @GetMapping
    public Map<String, Object> fetchOrders(
        @RequestParam Long orgId,
        @RequestParam(required = false) String createdAfter,
        @RequestParam(required = false, defaultValue = "awaiting") String shippingStatus,
        @RequestParam(required = false, defaultValue = "0") int start,
        @RequestParam(required = false, defaultValue = "100") int limit
    ) {
        int effectiveLimit = Math.min(Math.max(limit, 1), 100);
        MerchantProOrderService.OrderFetchResult result =
            merchantProOrderService.fetchOrders(orgId, createdAfter, shippingStatus, start, effectiveLimit);
        return Map.of(
            "data", result.data(),
            "meta", Map.of("total", result.total(), "start", start, "limit", effectiveLimit)
        );
    }
}
