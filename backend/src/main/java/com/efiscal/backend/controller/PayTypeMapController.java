package com.efiscal.backend.controller;

import com.efiscal.backend.model.PayTypeMapEntity;
import com.efiscal.backend.repository.PayTypeMapRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for payment type mapping configuration (spec 4.1.6).
 * Maps external payment method codes (e.g. MerchantPro payment_method_code)
 * to fiscal payment type integers per client.
 */
@RestController
@RequestMapping("/api/v1/paytype-map")
public class PayTypeMapController {

    private final PayTypeMapRepository payTypeMapRepository;

    public PayTypeMapController(PayTypeMapRepository payTypeMapRepository) {
        this.payTypeMapRepository = payTypeMapRepository;
    }

    @GetMapping
    public ResponseEntity<List<PayTypeMapEntity>> list(@RequestParam Long clientId) {
        return ResponseEntity.ok(payTypeMapRepository.findByClientId(clientId));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody PayTypeMapRequest request) {
        // Prevent duplicate per client + code
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
