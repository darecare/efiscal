package com.efiscal.backend.service;

import com.efiscal.backend.model.TaxCategoryEntity;
import com.efiscal.backend.repository.TaxCategoryRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TaxCategoryService {

    private final TaxCategoryRepository taxCategoryRepository;

    public TaxCategoryService(TaxCategoryRepository taxCategoryRepository) {
        this.taxCategoryRepository = taxCategoryRepository;
    }

    @Transactional(readOnly = true)
    public List<TaxCategoryDto> listTaxCategories() {
        return taxCategoryRepository.findAllByDeletedAtIsNull().stream().map(this::toDto).toList();
    }

    @Transactional
    public TaxCategoryDto createTaxCategory(TaxCategoryRequest req) {
        if (req.name() == null || req.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category name is required");
        }

        TaxCategoryEntity entity = new TaxCategoryEntity();
        entity.setName(req.name().trim());
        entity.setTaxcategoryCode(req.taxcategoryCode());
        entity.setActive(req.isActive() != null ? req.isActive() : true);
        return toDto(taxCategoryRepository.save(entity));
    }

    @Transactional
    public TaxCategoryDto updateTaxCategory(Long taxCategoryId, TaxCategoryRequest req) {
        TaxCategoryEntity entity = taxCategoryRepository.findById(taxCategoryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tax category not found"));

        if (req.name() != null && req.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category name cannot be blank");
        }

        if (req.name() != null) entity.setName(req.name().trim());
        if (req.taxcategoryCode() != null) entity.setTaxcategoryCode(req.taxcategoryCode());
        if (req.isActive() != null) entity.setActive(req.isActive());

        return toDto(taxCategoryRepository.save(entity));
    }

    private TaxCategoryDto toDto(TaxCategoryEntity entity) {
        return new TaxCategoryDto(
            entity.getTaxCategoryId(),
            entity.getName(),
            entity.getTaxcategoryCode(),
            entity.isActive(),
            entity.getCreatedAt()
        );
    }

    public record TaxCategoryDto(
        Long taxCategoryId,
        String name,
        String taxcategoryCode,
        boolean isActive,
        OffsetDateTime createdAt
    ) {}

    public record TaxCategoryRequest(
        String name,
        String taxcategoryCode,
        Boolean isActive
    ) {}
}
