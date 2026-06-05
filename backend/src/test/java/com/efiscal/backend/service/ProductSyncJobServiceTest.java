package com.efiscal.backend.service;

import com.efiscal.backend.model.ProductSyncJobEntity;
import com.efiscal.backend.repository.ProductSyncJobRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductSyncJobServiceTest {

    private static final Long ORG_ID = 1L;

    @Mock
    private ProductSyncJobRepository productSyncJobRepository;

    private ProductSyncJobService productSyncJobService;

    @BeforeEach
    void setUp() {
        productSyncJobService = new ProductSyncJobService(productSyncJobRepository);
    }

    @Test
    void resolveSyncStart_returnsFullWhenNoPriorFullJob() {
        when(productSyncJobRepository.findByOrgIdAndStatus(ORG_ID, ProductSyncJobService.STATUS_RUNNING))
            .thenReturn(Optional.empty());
        when(productSyncJobRepository.findTopByOrgIdAndSyncTypeAndStatusOrderByStartedAtDesc(
            ORG_ID, ProductSyncJobService.SYNC_TYPE_FULL, ProductSyncJobService.STATUS_DONE))
            .thenReturn(Optional.empty());

        ProductSyncJobService.SyncStartDecision decision = productSyncJobService.resolveSyncStart(ORG_ID);

        assertEquals(ProductSyncJobService.SYNC_TYPE_FULL, decision.syncType());
        assertNull(decision.filterFrom());
        assertNull(decision.modifiedSince());
    }

    @Test
    void resolveSyncStart_returnsIncrementalAfterCompletedFullSync() {
        when(productSyncJobRepository.findByOrgIdAndStatus(ORG_ID, ProductSyncJobService.STATUS_RUNNING))
            .thenReturn(Optional.empty());

        OffsetDateTime finishedAt = OffsetDateTime.of(2026, 6, 4, 12, 0, 0, 0, ZoneOffset.UTC);
        ProductSyncJobEntity lastFull = job(10L, ProductSyncJobService.SYNC_TYPE_FULL, ProductSyncJobService.STATUS_DONE);
        lastFull.setSynced(120);
        lastFull.setTotal(120);
        lastFull.setFinishedAt(finishedAt);

        when(productSyncJobRepository.findTopByOrgIdAndSyncTypeAndStatusOrderByStartedAtDesc(
            ORG_ID, ProductSyncJobService.SYNC_TYPE_FULL, ProductSyncJobService.STATUS_DONE))
            .thenReturn(Optional.of(lastFull));

        ProductSyncJobService.SyncStartDecision decision = productSyncJobService.resolveSyncStart(ORG_ID);

        assertEquals(ProductSyncJobService.SYNC_TYPE_INCREMENTAL, decision.syncType());
        assertEquals(finishedAt, decision.filterFrom());
        assertEquals(LocalDate.of(2026, 6, 3), decision.modifiedSince());
    }

    @Test
    void resolveSyncStart_returnsFullWhenLastFullSyncIncomplete() {
        when(productSyncJobRepository.findByOrgIdAndStatus(ORG_ID, ProductSyncJobService.STATUS_RUNNING))
            .thenReturn(Optional.empty());

        ProductSyncJobEntity lastFull = job(11L, ProductSyncJobService.SYNC_TYPE_FULL, ProductSyncJobService.STATUS_DONE);
        lastFull.setSynced(50);
        lastFull.setTotal(120);
        lastFull.setFinishedAt(OffsetDateTime.now(ZoneOffset.UTC));

        when(productSyncJobRepository.findTopByOrgIdAndSyncTypeAndStatusOrderByStartedAtDesc(
            ORG_ID, ProductSyncJobService.SYNC_TYPE_FULL, ProductSyncJobService.STATUS_DONE))
            .thenReturn(Optional.of(lastFull));

        ProductSyncJobService.SyncStartDecision decision = productSyncJobService.resolveSyncStart(ORG_ID);

        assertEquals(ProductSyncJobService.SYNC_TYPE_FULL, decision.syncType());
    }

    @Test
    void resolveSyncStart_throwsConflictWhenRunningJobExists() {
        ProductSyncJobEntity running = job(12L, ProductSyncJobService.SYNC_TYPE_FULL, ProductSyncJobService.STATUS_RUNNING);
        when(productSyncJobRepository.findByOrgIdAndStatus(ORG_ID, ProductSyncJobService.STATUS_RUNNING))
            .thenReturn(Optional.of(running));

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> productSyncJobService.resolveSyncStart(ORG_ID)
        );

        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void failStaleRunningJobs_marksOldRunningJobAsFailed() {
        ProductSyncJobEntity stale = job(13L, ProductSyncJobService.SYNC_TYPE_FULL, ProductSyncJobService.STATUS_RUNNING);
        stale.setStartedAt(OffsetDateTime.now(ZoneOffset.UTC).minusHours(3));

        when(productSyncJobRepository.findByOrgIdAndStatus(ORG_ID, ProductSyncJobService.STATUS_RUNNING))
            .thenReturn(Optional.of(stale));
        when(productSyncJobRepository.findById(13L)).thenReturn(Optional.of(stale));

        productSyncJobService.failStaleRunningJobs(ORG_ID);

        ArgumentCaptor<ProductSyncJobEntity> captor = ArgumentCaptor.forClass(ProductSyncJobEntity.class);
        verify(productSyncJobRepository).save(captor.capture());
        assertEquals(ProductSyncJobService.STATUS_FAILED, captor.getValue().getStatus());
        assertEquals("Sync timed out (stale job)", captor.getValue().getErrorMessage());
    }

    @Test
    void failStaleRunningJobs_leavesFreshRunningJobUntouched() {
        ProductSyncJobEntity fresh = job(14L, ProductSyncJobService.SYNC_TYPE_FULL, ProductSyncJobService.STATUS_RUNNING);
        fresh.setStartedAt(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(30));

        when(productSyncJobRepository.findByOrgIdAndStatus(ORG_ID, ProductSyncJobService.STATUS_RUNNING))
            .thenReturn(Optional.of(fresh));

        productSyncJobService.failStaleRunningJobs(ORG_ID);

        verify(productSyncJobRepository, never()).save(any());
    }

    @Test
    void getStatus_returnsIdleWhenNoJobs() {
        when(productSyncJobRepository.findByOrgIdAndStatus(ORG_ID, ProductSyncJobService.STATUS_RUNNING))
            .thenReturn(Optional.empty());
        when(productSyncJobRepository.findTopByOrgIdOrderByStartedAtDesc(ORG_ID))
            .thenReturn(Optional.empty());

        ProductSyncJobService.SyncStatusDto status = productSyncJobService.getStatus(ORG_ID);

        assertFalse(status.running());
        assertEquals(0, status.synced());
        assertEquals(0, status.total());
    }

    @Test
    void getStatus_cleansStaleJobBeforeReturningLatest() {
        ProductSyncJobEntity stale = job(15L, ProductSyncJobService.SYNC_TYPE_FULL, ProductSyncJobService.STATUS_RUNNING);
        stale.setStartedAt(OffsetDateTime.now(ZoneOffset.UTC).minusHours(3));
        stale.setSynced(10);
        stale.setTotal(100);

        ProductSyncJobEntity failed = job(15L, ProductSyncJobService.SYNC_TYPE_FULL, ProductSyncJobService.STATUS_FAILED);
        failed.setStartedAt(stale.getStartedAt());
        failed.setSynced(10);
        failed.setTotal(100);
        failed.setErrorMessage("Sync timed out (stale job)");
        failed.setFinishedAt(OffsetDateTime.now(ZoneOffset.UTC));

        when(productSyncJobRepository.findByOrgIdAndStatus(eq(ORG_ID), eq(ProductSyncJobService.STATUS_RUNNING)))
            .thenReturn(Optional.of(stale))
            .thenReturn(Optional.empty());
        when(productSyncJobRepository.findById(15L)).thenReturn(Optional.of(stale));
        when(productSyncJobRepository.findTopByOrgIdOrderByStartedAtDesc(ORG_ID))
            .thenReturn(Optional.of(failed));

        ProductSyncJobService.SyncStatusDto status = productSyncJobService.getStatus(ORG_ID);

        verify(productSyncJobRepository, times(2)).findByOrgIdAndStatus(ORG_ID, ProductSyncJobService.STATUS_RUNNING);
        assertFalse(status.running());
        assertEquals(ProductSyncJobService.STATUS_FAILED, status.status());
    }

    private static ProductSyncJobEntity job(long id, String syncType, String status) {
        ProductSyncJobEntity job = new ProductSyncJobEntity();
        job.setSyncJobId(id);
        job.setOrgId(ORG_ID);
        job.setSyncType(syncType);
        job.setStatus(status);
        job.setStartedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return job;
    }
}
