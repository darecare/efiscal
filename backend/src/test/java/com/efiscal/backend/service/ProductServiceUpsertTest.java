package com.efiscal.backend.service;

import com.efiscal.backend.model.ProductEntity;
import com.efiscal.backend.repository.OrgRepository;
import com.efiscal.backend.repository.ProductRepository;
import com.efiscal.backend.service.MerchantProProductService.MerchantProProductRow;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.efiscal.backend.model.ProductEntity.SOURCE_TYPE_MERCHANTPRO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceUpsertTest {

    private static final Long ORG_ID = 1L;
    private static final long CLIENT_ID = 10L;

    @Mock
    private ProductRepository productRepository;
    @Mock
    private OrgRepository orgRepository;
    @Mock
    private MerchantProProductService merchantProProductService;
    @Mock
    private ProductSyncJobService productSyncJobService;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        ProductService delegate = new ProductService(
            productRepository,
            orgRepository,
            merchantProProductService,
            productSyncJobService,
            new ObjectMapper(),
            null
        );
        productService = new ProductService(
            productRepository,
            orgRepository,
            merchantProProductService,
            productSyncJobService,
            new ObjectMapper(),
            delegate
        );
    }

    @Test
    void upsertPage_updatesHiddenSyncedProductWithoutUnhidingOnFullSync() {
        ProductEntity hidden = syncedProduct(100L, 501L);
        hidden.setHiddenAt(OffsetDateTime.now(ZoneOffset.UTC));

        when(productRepository.findByOrgIdAndMpProductIdIncludingHidden(ORG_ID, 501L))
            .thenReturn(Optional.of(hidden));
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        MerchantProProductRow row = new MerchantProProductRow(501L, "Widget", "SKU-1", "EAN-1", new BigDecimal("9.99"));
        ProductService.UpsertPageResult result = productService.upsertPage(
            ORG_ID,
            CLIENT_ID,
            List.of(row),
            ProductSyncJobService.SYNC_TYPE_FULL
        );

        assertEquals(1, result.count());
        assertEquals(List.of(501L), result.seenMpProductIds());

        ArgumentCaptor<ProductEntity> captor = ArgumentCaptor.forClass(ProductEntity.class);
        verify(productRepository).save(captor.capture());
        ProductEntity saved = captor.getValue();
        assertEquals(100L, saved.getProductId());
        assertEquals("Widget", saved.getName());
        assertNotNull(saved.getHiddenAt());
    }

    @Test
    void upsertPage_unhidesSyncedProductOnResetFull() {
        ProductEntity hidden = syncedProduct(101L, 502L);
        hidden.setHiddenAt(OffsetDateTime.now(ZoneOffset.UTC));

        when(productRepository.findByOrgIdAndMpProductIdIncludingHidden(ORG_ID, 502L))
            .thenReturn(Optional.of(hidden));
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        MerchantProProductRow row = new MerchantProProductRow(502L, "Gadget", "SKU-2", "EAN-2", new BigDecimal("19.99"));
        productService.upsertPage(
            ORG_ID,
            CLIENT_ID,
            List.of(row),
            ProductSyncJobService.SYNC_TYPE_RESET_FULL
        );

        ArgumentCaptor<ProductEntity> captor = ArgumentCaptor.forClass(ProductEntity.class);
        verify(productRepository).save(captor.capture());
        assertNull(captor.getValue().getHiddenAt());
    }

    @Test
    void upsertPage_doesNotResurrectDeletedManualProductWithMatchingSku() {
        when(productRepository.findByOrgIdAndMpProductIdIncludingHidden(ORG_ID, 503L))
            .thenReturn(Optional.empty());
        when(productRepository.findMerchantProByOrgIdAndSkuIncludingHidden(ORG_ID, "SKU-MANUAL"))
            .thenReturn(Optional.empty());
        when(productRepository.findManualByOrgIdAndSku(ORG_ID, "SKU-MANUAL"))
            .thenReturn(Optional.empty());
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        MerchantProProductRow row = new MerchantProProductRow(503L, "Shop Item", "SKU-MANUAL", null, new BigDecimal("5.00"));
        productService.upsertPage(
            ORG_ID,
            CLIENT_ID,
            List.of(row),
            ProductSyncJobService.SYNC_TYPE_FULL
        );

        ArgumentCaptor<ProductEntity> captor = ArgumentCaptor.forClass(ProductEntity.class);
        verify(productRepository).save(captor.capture());
        ProductEntity saved = captor.getValue();
        assertEquals(SOURCE_TYPE_MERCHANTPRO, saved.getSourceType());
        assertNull(saved.getProductId());
    }

    @Test
    void upsertPage_skipsWhenManualProductHasMatchingSku() {
        when(productRepository.findByOrgIdAndMpProductIdIncludingHidden(ORG_ID, 504L))
            .thenReturn(Optional.empty());
        when(productRepository.findMerchantProByOrgIdAndSkuIncludingHidden(ORG_ID, "SKU-MANUAL"))
            .thenReturn(Optional.empty());
        when(productRepository.findManualByOrgIdAndSku(ORG_ID, "SKU-MANUAL"))
            .thenReturn(Optional.of(manualProduct(200L, "SKU-MANUAL")));

        MerchantProProductRow row = new MerchantProProductRow(504L, "Shop Item", "SKU-MANUAL", null, new BigDecimal("5.00"));
        ProductService.UpsertPageResult result = productService.upsertPage(
            ORG_ID,
            CLIENT_ID,
            List.of(row),
            ProductSyncJobService.SYNC_TYPE_FULL
        );

        assertEquals(1, result.count());
        assertEquals(List.of(), result.seenMpProductIds());
        verify(productRepository, never()).save(any(ProductEntity.class));
    }

    private static ProductEntity manualProduct(long productId, String sku) {
        ProductEntity entity = new ProductEntity();
        entity.setProductId(productId);
        entity.setOrgId(ORG_ID);
        entity.setClientId(CLIENT_ID);
        entity.setSourceType("MANUAL");
        entity.setName("Manual");
        entity.setSku(sku);
        return entity;
    }

    private static ProductEntity syncedProduct(long productId, long mpProductId) {
        ProductEntity entity = new ProductEntity();
        entity.setProductId(productId);
        entity.setOrgId(ORG_ID);
        entity.setClientId(CLIENT_ID);
        entity.setMpProductId(mpProductId);
        entity.setSourceType(SOURCE_TYPE_MERCHANTPRO);
        entity.setName("Old");
        entity.setSku("OLD-SKU");
        return entity;
    }
}
