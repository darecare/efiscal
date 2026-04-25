package com.efiscal.backend.controller;

import com.efiscal.backend.service.DemoDataService;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchantpro/orders")
public class MerchantProOrderController {

    private final DemoDataService demoDataService;

    public MerchantProOrderController(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @PostMapping
    public List<DemoDataService.OrderView> fetchOrders(@RequestBody DemoDataService.OrderFilter filter) {
        return demoDataService.findOrders(filter);
    }
}
