package com.efiscal.backend.repository;

import com.efiscal.backend.service.report.FiscalReportModels.ProductsSoldRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class FiscalBillReportRepository {

    private static final String PRODUCTS_SOLD_BASE = """
            FROM fiscalbillline l
            JOIN fiscalbill b ON b.fiscalbill_id = l.fiscalbill_id
            WHERE b.org_id = :orgId
              AND b.status = 'SUCCESS'
              AND b.efiscal_invoicetype = 0
              AND b.efiscal_sdcdatetime IS NOT NULL
              AND b.efiscal_sdcdatetime >= :fromInclusive
              AND b.efiscal_sdcdatetime < :toExclusive
            """;

    private static final String PRODUCTS_SOLD_SELECT = """
            SELECT coalesce(nullif(trim(l.product_id), ''), nullif(trim(l.sku), ''), nullif(trim(l.gtin), ''), l.name) AS product_key,
                   max(l.name) AS name,
                   max(l.sku) AS sku,
                   max(l.gtin) AS gtin,
                   max(l.product_id) AS product_id,
                   sum(CASE WHEN b.efiscal_transactiontype = 0 THEN l.quantity ELSE 0 END) AS qty_sold,
                   sum(CASE WHEN b.efiscal_transactiontype = 1 THEN l.quantity ELSE 0 END) AS qty_refunded,
                   sum(CASE WHEN b.efiscal_transactiontype = 0 THEN l.quantity ELSE -l.quantity END) AS qty_net,
                   sum(CASE WHEN b.efiscal_transactiontype = 0 THEN l.total_amount ELSE 0 END) AS amount_sold,
                   sum(CASE WHEN b.efiscal_transactiontype = 1 THEN l.total_amount ELSE 0 END) AS amount_refunded,
                   sum(CASE WHEN b.efiscal_transactiontype = 0 THEN l.total_amount ELSE -l.total_amount END) AS amount_net,
                   count(DISTINCT b.fiscalbill_id) AS bill_count
            """ + PRODUCTS_SOLD_BASE + """
            GROUP BY 1
            ORDER BY max(l.name)
            """;

    private static final String PRODUCTS_SOLD_COUNT = """
            SELECT count(*) FROM (
              SELECT 1
            """ + PRODUCTS_SOLD_BASE + """
              GROUP BY coalesce(nullif(trim(l.product_id), ''), nullif(trim(l.sku), ''), nullif(trim(l.gtin), ''), l.name)
            ) grouped
            """;

    @PersistenceContext
    private EntityManager entityManager;

    public long countProductsSoldNetNormal(Long orgId, String fromInclusive, String toExclusive) {
        Query query = entityManager.createNativeQuery(PRODUCTS_SOLD_COUNT);
        bindRange(query, orgId, fromInclusive, toExclusive);
        Object result = query.getSingleResult();
        if (result instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(result));
    }

    @SuppressWarnings("unchecked")
    public List<ProductsSoldRow> findProductsSoldNetNormal(
            Long orgId, String fromInclusive, String toExclusive, int offset, int limit) {
        Query query = entityManager.createNativeQuery(PRODUCTS_SOLD_SELECT);
        bindRange(query, orgId, fromInclusive, toExclusive);
        if (offset > 0) {
            query.setFirstResult(offset);
        }
        if (limit > 0) {
            query.setMaxResults(limit);
        }
        List<Object[]> rows = query.getResultList();
        List<ProductsSoldRow> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(mapProductsSoldRow(row));
        }
        return result;
    }

    public List<ProductsSoldRow> findAllProductsSoldNetNormal(
            Long orgId, String fromInclusive, String toExclusive) {
        return findProductsSoldNetNormal(orgId, fromInclusive, toExclusive, 0, 0);
    }

    private static void bindRange(Query query, Long orgId, String fromInclusive, String toExclusive) {
        query.setParameter("orgId", orgId);
        query.setParameter("fromInclusive", fromInclusive);
        query.setParameter("toExclusive", toExclusive);
    }

    private static ProductsSoldRow mapProductsSoldRow(Object[] row) {
        return new ProductsSoldRow(
                asString(row[0]),
                asString(row[1]),
                asString(row[2]),
                asString(row[3]),
                asString(row[4]),
                asDecimal(row[5]),
                asDecimal(row[6]),
                asDecimal(row[7]),
                asDecimal(row[8]),
                asDecimal(row[9]),
                asDecimal(row[10]),
                asLong(row[11])
        );
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static BigDecimal asDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return new BigDecimal(String.valueOf(value));
    }

    private static long asLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
