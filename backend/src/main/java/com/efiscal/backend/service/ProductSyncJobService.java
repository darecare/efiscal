package com.efiscal.backend.service;

import com.efiscal.backend.model.ProductSyncJobEntity;
import com.efiscal.backend.repository.ProductSyncJobRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

    public static final String SYNC_TYPE_FULL = "FULL";
    public static final String SYNC_TYPE_INCREMENTAL = "INCREMENTAL";

    private static final long STALE_JOB_HOURS = 2L;

    private final ProductSyncJobRepository productSyncJobRepository;

    public ProductSyncJobService(ProductSyncJobRepository productSyncJobRepository) {
        this.productSyncJobRepository = productSyncJobRepository;
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
        return productSyncJobRepository.findTopByOrgIdAndSyncTypeAndStatusOrderByStartedAtDesc(
            orgId, SYNC_TYPE_FULL, STATUS_DONE);
    }

    @Transactional
    public SyncStartDecision resolveSyncStart(Long orgId) {
        failStaleRunningJobs(orgId);
        Optional<ProductSyncJobEntity> running = findRunningJob(orgId);
        if (running.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sync already in progress");
        }

        String syncType = SYNC_TYPE_FULL;
        OffsetDateTime filterFrom = null;
        LocalDate modifiedSince = null;

        Optional<ProductSyncJobEntity> lastFull = findLastCompletedFullJob(orgId);
        if (lastFull.isPresent()) {
            ProductSyncJobEntity job = lastFull.get();
            if (job.getSynced() >= job.getTotal() && job.getTotal() > 0 && job.getFinishedAt() != null) {
                syncType = SYNC_TYPE_INCREMENTAL;
                filterFrom = job.getFinishedAt();
                modifiedSince = filterFrom.atZoneSameInstant(ZoneOffset.UTC).toLocalDate().minusDays(1);
            }
        }

        return new SyncStartDecision(syncType, filterFrom, modifiedSince);
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
