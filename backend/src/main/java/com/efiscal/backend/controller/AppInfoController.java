package com.efiscal.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/app-info")
public class AppInfoController {

    private final String manufacturer;
    private final String serialNumber;
    private final String softwareVersion;

    public AppInfoController(
        @Value("${app.manufacturer}") String manufacturer,
        @Value("${app.serial-number}") String serialNumber,
        @Value("${app.software-version}") String softwareVersion
    ) {
        this.manufacturer = manufacturer;
        this.serialNumber = serialNumber;
        this.softwareVersion = softwareVersion;
    }

    @GetMapping
    public AppInfoResponse getAppInfo() {
        return new AppInfoResponse(manufacturer, serialNumber, softwareVersion);
    }

    public record AppInfoResponse(
        String manufacturer,
        String serialNumber,
        String softwareVersion
    ) {}
}