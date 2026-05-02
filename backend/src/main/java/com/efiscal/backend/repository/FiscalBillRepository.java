package com.efiscal.backend.repository;

import com.efiscal.backend.model.FiscalBillEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FiscalBillRepository extends JpaRepository<FiscalBillEntity, String> {

    List<FiscalBillEntity> findByOrgIdOrderByCreatedDesc(Long orgId);

    /** All fiscal bills for a given external order id. */
    List<FiscalBillEntity> findByOrderId(String orderId);

    /** Find the most recent fiscal bill for an order with specific invoice/transaction type. */
    @Query("SELECT fb FROM FiscalBillEntity fb WHERE fb.orderId = :orderId " +
           "AND fb.efiscalInvoicetype = :invoiceType AND fb.efiscalTransactiontype = :transactionType " +
           "ORDER BY fb.created DESC")
    List<FiscalBillEntity> findByOrderIdAndInvoiceTypeAndTransactionType(
            @Param("orderId") String orderId,
            @Param("invoiceType") Integer invoiceType,
            @Param("transactionType") Integer transactionType);

    /** Check if a fiscal bill already exists for a given order + invoiceType + transactionType. */
    default Optional<FiscalBillEntity> findLatestByOrderAndType(String orderId, Integer invoiceType, Integer transactionType) {
        List<FiscalBillEntity> list = findByOrderIdAndInvoiceTypeAndTransactionType(orderId, invoiceType, transactionType);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
