package com.efiscal.backend.service;

import com.efiscal.backend.model.TaxCategoryEntity;
import com.efiscal.backend.model.TaxEntity;
import com.efiscal.backend.repository.TaxCategoryRepository;
import com.efiscal.backend.repository.TaxRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TaxService {

    private final TaxRepository taxRepository;
    private final TaxCategoryRepository taxCategoryRepository;

    public TaxService(TaxRepository taxRepository, TaxCategoryRepository taxCategoryRepository) {
        this.taxRepository = taxRepository;
        this.taxCategoryRepository = taxCategoryRepository;
    }

    @Transactional(readOnly = true)
    public List<TaxDto> listTaxes() {
        return taxRepository.findAllByDeletedAtIsNull().stream().map(this::toDto).toList();
    }

    @Transactional
    public TaxDto createTax(TaxRequest req) {
        validateRequest(req, true);

        TaxCategoryEntity taxCategory = taxCategoryRepository.findById(req.taxCategoryId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tax category not found"));

        TaxEntity entity = new TaxEntity();
        entity.setTaxCategory(taxCategory);
        entity.setLabel(req.label().trim().toUpperCase());
        entity.setRate(req.rate());
        entity.setActive(req.isActive() != null ? req.isActive() : true);
        entity.setEfiscalTaxname(req.efiscalTaxname());
        entity.setEfiscalAdvanceprefix(req.efiscalAdvanceprefix());
        entity.setEfiscalAdvancename(req.efiscalAdvancename());

        return toDto(taxRepository.save(entity));
    }

    @Transactional
    public TaxDto updateTax(Long taxId, TaxRequest req) {
        TaxEntity entity = taxRepository.findById(taxId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tax not found"));

        validateRequest(req, false);

        if (req.taxCategoryId() != null) {
            TaxCategoryEntity taxCategory = taxCategoryRepository.findById(req.taxCategoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tax category not found"));
            entity.setTaxCategory(taxCategory);
        }
        if (req.label() != null) entity.setLabel(req.label().trim().toUpperCase());
        if (req.rate() != null) entity.setRate(req.rate());
        if (req.isActive() != null) entity.setActive(req.isActive());
        if (req.efiscalTaxname() != null) entity.setEfiscalTaxname(req.efiscalTaxname());
        if (req.efiscalAdvanceprefix() != null) entity.setEfiscalAdvanceprefix(req.efiscalAdvanceprefix());
        if (req.efiscalAdvancename() != null) entity.setEfiscalAdvancename(req.efiscalAdvancename());

        return toDto(taxRepository.save(entity));
    }

    private void validateRequest(TaxRequest req, boolean isCreate) {
        if (isCreate && req.taxCategoryId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "taxCategoryId is required");
        }
        if (isCreate && (req.label() == null || req.label().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "label is required");
        }
        if (req.label() != null && req.label().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "label cannot be blank");
        }
        if (isCreate && req.rate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rate is required");
        }
        if (req.rate() != null && req.rate().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rate must be >= 0");
        }
    }

    private TaxDto toDto(TaxEntity entity) {
        return new TaxDto(
            entity.getTaxId(),
            entity.getTaxCategory().getTaxCategoryId(),
            entity.getTaxCategory().getName(),
            entity.getLabel(),
            entity.getRate(),
            entity.isActive(),
            entity.getEfiscalTaxname(),
            entity.getEfiscalAdvanceprefix(),
            entity.getEfiscalAdvancename(),
            entity.getCreatedAt()
        );
    }

    public record TaxDto(
        Long taxId,
        Long taxCategoryId,
        String taxCategoryName,
        String label,
        BigDecimal rate,
        boolean isActive,
        String efiscalTaxname,
        String efiscalAdvanceprefix,
        String efiscalAdvancename,
        OffsetDateTime createdAt
    ) {}

    public record TaxRequest(
        Long taxCategoryId,
        String label,
        BigDecimal rate,
        Boolean isActive,
        String efiscalTaxname,
        String efiscalAdvanceprefix,
        String efiscalAdvancename
    ) {}
}
