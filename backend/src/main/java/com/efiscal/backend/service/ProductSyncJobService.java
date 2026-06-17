package com.efiscal.backend.service;

import com.efiscal.backend.model.ProductSyncJobEntity;
import com.efiscal.backend.repository.ProductRepository;
import com.efiscal.backend.repository.ProductSyncJobRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductSyncJobService {

    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_DONE = "DONE";
    public static final String STATUS_FAILED = "FAILED";

    public static final String SYNC_MODE_AUTO = "AUTO";
    public static final String SYNC_MODE_INCREMENTAL = "INCREMENTAL";
    public static final String SYNC_MODE_FULL = "FULL";
    public static final String SYNC_MODE_RESET_FULL = "RESET_FULL";

    public static final String SYNC_TYPE_FULL = "FULL";
    public static final String SYNC_TYPE_INCREMENTAL = "INCREMENTAL";
    public static final String SYNC_TYPE_RESET_FULL = "RESET_FULL";

    private static final long STALE_JOB_HOURS = 2L;

    private final ProductSyncJobRepository productSyncJobRepository;
    private final ProductRepository productRepository;

    public ProductSyncJobService(
        ProductSyncJobRepository productSyncJobRepository,
        ProductRepository productRepository
    ) {
        this.productSyncJobRepository = productSyncJobRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public void failStaleRunningJobs(Long orgId) {
        findRunningJob(orgId).ifPresent(job -> {
            OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC).minusHours(STALE_JOB_HOURS);
            if (job.getStartedAt().isBefore(cutoff)) {
                completeJob(job.getSyncJobId(), STATUS_FAILED, "Sync timed out (stale job)");
            }
        });
    }

    @Transactional(readOnly = true)
    public Optional<ProductSyncJobEntity> findRunningJob(Long orgId) {
        return productSyncJobRepository.findByOrgIdAndStatus(orgId, STATUS_RUNNING);
    }

    @Transactional(readOnly = true)
    public Optional<ProductSyncJobEntity> findLastCompletedFullJob(Long orgId) {
        return productSyncJobRepository.findTopByOrgIdAndSyncTypeInAndStatusOrderByStartedAtDesc(
            orgId,
            List.of(SYNC_TYPE_FULL, SYNC_TYPE_RESET_FULL),
            STATUS_DONE
        );
    }

    @Transactional
    public SyncStartDecision resolveSyncStart(Long orgId, String requestedMode) {
        failStaleRunningJobs(orgId);
        Optional<ProductSyncJobEntity> running = findRunningJob(orgId);
        if (running.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sync already in progress");
        }

        String mode = normalizeMode(requestedMode);
        long visibleCount = productRepository.countVisibleByOrgId(orgId);

        if (SYNC_MODE_RESET_FULL.equals(mode)) {
            return new SyncStartDecision(SYNC_TYPE_RESET_FULL, null, null);
        }
        if (SYNC_MODE_FULL.equals(mode)) {
            return new SyncStartDecision(SYNC_TYPE_FULL, null, null);
        }
        if (SYNC_MODE_INCREMENTAL.equals(mode)) {
            return resolveIncrementalStart(orgId);
        }

        // AUTO: force full when catalog is empty; use RESET_FULL if products are only hidden locally
        if (visibleCount == 0) {
            long hiddenCount = productRepository.countHiddenMerchantProByOrgId(orgId);
            String type = hiddenCount > 0 ? SYNC_TYPE_RESET_FULL : SYNC_TYPE_FULL;
            return new SyncStartDecision(type, null, null);
        }
        return resolveAutoStart(orgId);
    }

    private SyncStartDecision resolveAutoStart(Long orgId) {
        Optional<ProductSyncJobEntity> lastFull = findLastCompletedFullJob(orgId);
        if (lastFull.isEmpty()) {
            return new SyncStartDecision(SYNC_TYPE_FULL, null, null);
        }
        ProductSyncJobEntity job = lastFull.get();
        if (job.getSynced() >= job.getTotal() && job.getTotal() > 0 && job.getFinishedAt() != null) {
            OffsetDateTime filterFrom = job.getFinishedAt();
            LocalDate modifiedSince = filterFrom.atZoneSameInstant(ZoneOffset.UTC).toLocalDate().minusDays(1);
            return new SyncStartDecision(SYNC_TYPE_INCREMENTAL, filterFrom, modifiedSince);
        }
        return new SyncStartDecision(SYNC_TYPE_FULL, null, null);
    }

    private SyncStartDecision resolveIncrementalStart(Long orgId) {
        Optional<ProductSyncJobEntity> lastFull = findLastCompletedFullJob(orgId);
        if (lastFull.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Incremental sync requires a prior completed full sync"
            );
        }
        ProductSyncJobEntity job = lastFull.get();
        if (job.getFinishedAt() == null
                || job.getTotal() == 0
                || job.getSynced() < job.getTotal()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Incremental sync requires a prior completed full sync"
            );
        }
        OffsetDateTime filterFrom = job.getFinishedAt();
        LocalDate modifiedSince = filterFrom.atZoneSameInstant(ZoneOffset.UTC).toLocalDate().minusDays(1);
        return new SyncStartDecision(SYNC_TYPE_INCREMENTAL, filterFrom, modifiedSince);
    }

    private static String normalizeMode(String requestedMode) {
        if (requestedMode == null || requestedMode.isBlank()) {
            return SYNC_MODE_AUTO;
        }
        return requestedMode.trim().toUpperCase();
    }

    public static boolean isFullCatalogSync(String syncType) {
        return SYNC_TYPE_FULL.equals(syncType) || SYNC_TYPE_RESET_FULL.equals(syncType);
    }

    public static boolean isResetFullSync(String syncType) {
        return SYNC_TYPE_RESET_FULL.equals(syncType);
    }

    @Transactional
    public long startJob(Long orgId, String syncType, OffsetDateTime filterFrom) {
        ProductSyncJobEntity job = new ProductSyncJobEntity();
        job.setOrgId(orgId);
        job.setStatus(STATUS_RUNNING);
        job.setSyncType(syncType);
        job.setSynced(0);
        job.setTotal(0);
        job.setFilterFrom(filterFrom);
        job.setStartedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return productSyncJobRepository.save(job).getSyncJobId();
    }

    @Transactional
    public void updateProgress(long jobId, int synced, int total) {
        ProductSyncJobEntity job = requireJob(jobId);
        job.setSynced(synced);
        job.setTotal(total);
        productSyncJobRepository.save(job);
    }

    @Transactional
    public void completeJob(long jobId, String status, String errorMessage) {
        ProductSyncJobEntity job = requireJob(jobId);
        job.setStatus(status);
        job.setErrorMessage(errorMessage);
        job.setFinishedAt(OffsetDateTime.now(ZoneOffset.UTC));
        productSyncJobRepository.save(job);
    }

    @Transactional
    public SyncStatusDto getStatus(Long orgId) {
        failStaleRunningJobs(orgId);
        Optional<ProductSyncJobEntity> running = findRunningJob(orgId);
        if (running.isPresent()) {
            return toStatusDto(running.get(), true);
        }
        Optional<ProductSyncJobEntity> latest = productSyncJobRepository.findTopByOrgIdOrderByStartedAtDesc(orgId);
        if (latest.isPresent()) {
            return toStatusDto(latest.get(), false);
        }
        return new SyncStatusDto(false, null, null, null, 0, 0, null, null, null, null);
    }

    private ProductSyncJobEntity requireJob(long jobId) {
        return productSyncJobRepository.findById(jobId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sync job not found"));
    }

    private SyncStatusDto toStatusDto(ProductSyncJobEntity job, boolean running) {
        return new SyncStatusDto(
            running,
            job.getSyncJobId(),
            job.getSyncType(),
            job.getStatus(),
            job.getSynced(),
            job.getTotal(),
            job.getFilterFrom(),
            job.getStartedAt(),
            job.getFinishedAt(),
            job.getErrorMessage()
        );
    }

    public record SyncStartDecision(String syncType, OffsetDateTime filterFrom, LocalDate modifiedSince) {}

    public record SyncStatusDto(
        boolean running,
        Long syncJobId,
        String syncType,
        String status,
        int synced,
        int total,
        OffsetDateTime filterFrom,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        String errorMessage
    ) {}
}
