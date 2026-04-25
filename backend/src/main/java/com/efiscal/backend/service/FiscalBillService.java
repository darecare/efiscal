package com.efiscal.backend.service;

import com.efiscal.backend.model.FiscalBillEntity;
import com.efiscal.backend.model.FiscalBillIdempotencyKeyEntity;
import com.efiscal.backend.model.FiscalBillStatus;
import com.efiscal.backend.repository.FiscalBillIdempotencyKeyRepository;
import com.efiscal.backend.repository.FiscalBillRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FiscalBillService {

    private final FiscalBillRepository fiscalBillRepository;
    private final FiscalBillIdempotencyKeyRepository idempotencyKeyRepository;

    public FiscalBillService(
        FiscalBillRepository fiscalBillRepository,
        FiscalBillIdempotencyKeyRepository idempotencyKeyRepository
    ) {
        this.fiscalBillRepository = fiscalBillRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
    }

    @Transactional
    public FiscalBillCreateResult createFiscalBill(String idempotencyKey, FiscalBillCreateRequest request) {
        Optional<FiscalBillIdempotencyKeyEntity> existingKey = idempotencyKeyRepository.findById(idempotencyKey);
        if (existingKey.isPresent()) {
            return FiscalBillCreateResult.ofAlreadyExists(toView(existingKey.get().getFiscalBill()));
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        FiscalBillStatus status = resolveInitialFiscalStatus(request.orderId());
        String fiscalDocumentId = UUID.randomUUID().toString();

        FiscalBillEntity entity = new FiscalBillEntity();
        entity.setFiscalDocumentId(fiscalDocumentId);
        entity.setOrderId(request.orderId());
        entity.setStatus(status);
        entity.setProviderReference(status == FiscalBillStatus.SUCCESS ? "SUF-" + request.orderId() + "-" + now.toEpochSecond() : null);
        entity.setLastError(status == FiscalBillStatus.FAILED ? "Provider timeout. Retry is allowed." : null);
        entity.setAttemptCount(1);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        fiscalBillRepository.save(entity);

        FiscalBillIdempotencyKeyEntity keyEntity = new FiscalBillIdempotencyKeyEntity();
        keyEntity.setIdempotencyKey(idempotencyKey);
        keyEntity.setFiscalBill(entity);
        keyEntity.setCreatedAt(now);
        idempotencyKeyRepository.save(keyEntity);

        return FiscalBillCreateResult.ofCreated(toView(entity));
    }

    @Transactional(readOnly = true)
    public FiscalBillView findFiscalBillById(String fiscalBillId) {
        return fiscalBillRepository.findById(fiscalBillId)
            .map(this::toView)
            .orElse(null);
    }

    @Transactional
    public FiscalBillRetryResult retryFiscalBill(String fiscalBillId, String idempotencyKey) {
        Optional<FiscalBillIdempotencyKeyEntity> existingRetryKey = idempotencyKeyRepository.findById(idempotencyKey);
        if (existingRetryKey.isPresent()) {
            String existingBillId = existingRetryKey.get().getFiscalBill().getFiscalDocumentId();
            if (!existingBillId.equals(fiscalBillId)) {
                return FiscalBillRetryResult.ofIdempotencyConflict();
            }
        }

        FiscalBillEntity entity = fiscalBillRepository.findById(fiscalBillId).orElse(null);
        if (entity == null) {
            return FiscalBillRetryResult.ofNotFound();
        }
        if (entity.getStatus() != FiscalBillStatus.FAILED) {
            return FiscalBillRetryResult.ofNotRetryable(toView(entity));
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        entity.setStatus(FiscalBillStatus.RETRYING);
        entity.setLastError(null);
        entity.setAttemptCount(entity.getAttemptCount() + 1);
        entity.setUpdatedAt(now);
        fiscalBillRepository.save(entity);

        if (existingRetryKey.isEmpty()) {
            FiscalBillIdempotencyKeyEntity keyEntity = new FiscalBillIdempotencyKeyEntity();
            keyEntity.setIdempotencyKey(idempotencyKey);
            keyEntity.setFiscalBill(entity);
            keyEntity.setCreatedAt(now);
            idempotencyKeyRepository.save(keyEntity);
        }

        return FiscalBillRetryResult.ofRetried(toView(entity));
    }

    private FiscalBillView toView(FiscalBillEntity entity) {
        return new FiscalBillView(
            entity.getFiscalDocumentId(),
            entity.getOrderId(),
            entity.getStatus().name(),
            entity.getProviderReference(),
            entity.getLastError(),
            entity.getAttemptCount(),
            entity.getCreatedAt().toString(),
            entity.getUpdatedAt().toString());
    }

    private FiscalBillStatus resolveInitialFiscalStatus(String orderId) {
        return "2".equals(orderId) ? FiscalBillStatus.FAILED : FiscalBillStatus.SUCCESS;
    }

    public record FiscalBillCreateRequest(
        String orderId,
        CustomerPayload customer,
        List<FiscalItemPayload> items,
        String currency,
        String paymentMethod
    ) {
    }

    public record CustomerPayload(String name) {
    }

    public record FiscalItemPayload(
        String sku,
        String name,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal taxRate
    ) {
    }

    public record FiscalBillCreateResult(FiscalBillView fiscalBill, boolean created, boolean idempotencyConflict) {
        public static FiscalBillCreateResult ofCreated(FiscalBillView fiscalBill) {
            return new FiscalBillCreateResult(fiscalBill, true, false);
        }

        public static FiscalBillCreateResult ofAlreadyExists(FiscalBillView fiscalBill) {
            return new FiscalBillCreateResult(fiscalBill, false, false);
        }
    }

    public record FiscalBillRetryResult(
        FiscalBillView fiscalBill,
        boolean retried,
        boolean notFound,
        boolean notRetryable,
        boolean idempotencyConflict
    ) {
        public static FiscalBillRetryResult ofRetried(FiscalBillView fiscalBill) {
            return new FiscalBillRetryResult(fiscalBill, true, false, false, false);
        }

        public static FiscalBillRetryResult ofNotFound() {
            return new FiscalBillRetryResult(null, false, true, false, false);
        }

        public static FiscalBillRetryResult ofNotRetryable(FiscalBillView fiscalBill) {
            return new FiscalBillRetryResult(fiscalBill, false, false, true, false);
        }

        public static FiscalBillRetryResult ofIdempotencyConflict() {
            return new FiscalBillRetryResult(null, false, false, false, true);
        }
    }

    public record FiscalBillView(
        String fiscalDocumentId,
        String orderId,
        String status,
        String providerReference,
        String lastError,
        int attemptCount,
        String createdAt,
        String updatedAt
    ) {
    }
}
