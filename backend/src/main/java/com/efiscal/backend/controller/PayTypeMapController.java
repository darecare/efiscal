package com.efiscal.backend.controller;

import com.efiscal.backend.model.PayTypeMapEntity;
import com.efiscal.backend.repository.PayTypeMapRepository;
import com.efiscal.backend.security.AuthorizationService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/paytype-map")
public class PayTypeMapController {

    private final PayTypeMapRepository payTypeMapRepository;
    private final AuthorizationService authorizationService;

    public PayTypeMapController(PayTypeMapRepository payTypeMapRepository, AuthorizationService authorizationService) {
        this.payTypeMapRepository = payTypeMapRepository;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public ResponseEntity<List<PayTypeMapEntity>> list(@RequestParam Long clientId) {
        authorizationService.requireAction("ORGS_MANAGE");
        return ResponseEntity.ok(payTypeMapRepository.findByClientId(clientId));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody PayTypeMapRequest request) {
        authorizationService.requireAction("ORGS_MANAGE");
        if (payTypeMapRepository.findByClientIdAndPaymentMethodCode(
                request.clientId(), request.paymentMethodCode()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("Mapping already exists for this payment method code"));
        }
        PayTypeMapEntity entity = new PayTypeMapEntity();
        entity.setClientId(request.clientId());
        entity.setPaymentMethodCode(request.paymentMethodCode());
        entity.setPaymentType(request.paymentType());
        entity.setDescription(request.description());
        entity.setIsactive("Y");
        LocalDateTime now = LocalDateTime.now();
        entity.setCreated(now);
        entity.setUpdated(now);
        return ResponseEntity.status(HttpStatus.CREATED).body(payTypeMapRepository.save(entity));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody PayTypeMapRequest request) {
        authorizationService.requireAction("ORGS_MANAGE");
        return payTypeMapRepository.findById(id).map(entity -> {
            entity.setPaymentMethodCode(request.paymentMethodCode());
            entity.setPaymentType(request.paymentType());
            entity.setDescription(request.description());
            entity.setUpdated(LocalDateTime.now());
            return ResponseEntity.ok(payTypeMapRepository.save(entity));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        authorizationService.requireAction("ORGS_MANAGE");
        return payTypeMapRepository.findById(id).map(entity -> {
            entity.setIsactive("N");
            entity.setUpdated(LocalDateTime.now());
            payTypeMapRepository.save(entity);
            return ResponseEntity.noContent().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    public record PayTypeMapRequest(
            Long clientId,
            String paymentMethodCode,
            Integer paymentType,
            String description) {}

    public record ErrorResponse(String message) {}
}
