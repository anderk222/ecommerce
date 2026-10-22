package com.anderk.ecommerce.catalog.adapter.in.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.anderk.ecommerce.catalog.adapter.in.web.dto.CatalogItemAdminDetailResponse;
import com.anderk.ecommerce.catalog.adapter.in.web.dto.CatalogItemAdminResponse;
import com.anderk.ecommerce.catalog.adapter.in.web.dto.CatalogItemDetailResponse;
import com.anderk.ecommerce.catalog.application.CatalogCommandService;
import com.anderk.ecommerce.catalog.application.CatalogQueryService;
import com.anderk.ecommerce.catalog.application.command.ApplyDiscountCommand;
import com.anderk.ecommerce.catalog.application.command.CreateCatalogItemCommand;
import com.anderk.ecommerce.catalog.application.command.UpdateCatalogItemCommand;
import com.anderk.ecommerce.catalog.domain.model.ItemType;
import com.anderk.ecommerce.catalog.domain.model.Specification;
import com.anderk.ecommerce.catalog.domain.port.CatalogSearchCriteria;
import com.anderk.ecommerce.catalog.domain.port.CatalogSort;
import com.anderk.ecommerce.catalog.domain.port.ItemStateFilter;
import com.anderk.ecommerce.shared.pagination.PageRequestDto;
import com.anderk.ecommerce.shared.pagination.PageResponse;
import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The administration of the catalogue (HU-14 to HU-19).
 *
 * <p>The route is under `/api/admin`, so it needs the role `ADMIN`. The
 * changes of the catalogue are written by the use cases, not here.
 */
@RestController
@RequestMapping("/api/admin/catalog")
public class AdminCatalogController {

    private final CatalogQueryService catalogQueryService;

    private final CatalogCommandService catalogCommandService;

    public AdminCatalogController(CatalogQueryService catalogQueryService,
                                  CatalogCommandService catalogCommandService) {
        this.catalogQueryService = catalogQueryService;
        this.catalogCommandService = catalogCommandService;
    }

    /**
     * The unified listing of the administration (HU-14).
     *
     * @param type {@code PRODUCT}, {@code SERVICE}, or nothing for both
     * @param q the free text
     * @param category the slug of the category
     * @param sort the ordering
     * @param state {@code ALL}, {@code ACTIVE}, {@code INACTIVE} or {@code DELETED}
     * @param page the 0-based page number
     * @param size the page size
     */
    @GetMapping
    public PageResponse<CatalogItemAdminResponse> list(
            @RequestParam(required = false) String type,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "ALL") String state,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size) {

        CatalogSearchCriteria criteria = CatalogSearchCriteria.administration(
                q,
                parseType(type),
                blankToNull(category),
                parseSort(sort),
                parseState(state));

        return this.catalogQueryService
                .searchAdmin(criteria, PageRequestDto.of(page, size))
                .map(CatalogItemAdminResponse::of);
    }

    /** Reads an item for its edition (HU-16), whatever its state. */
    @GetMapping("/{id}")
    public CatalogItemAdminDetailResponse get(@PathVariable Long id) {
        return CatalogItemAdminDetailResponse.of(this.catalogQueryService.getAdminById(id));
    }

    /** Creates an item (HU-15). */
    @PostMapping
    public ResponseEntity<CatalogItemDetailResponse> create(
            @Valid @RequestBody CatalogItemRequest request) {
        CatalogItemDetailResponse created = CatalogApiMapper.toResponse(
                this.catalogCommandService.create(request.toCreateCommand()));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** Edits an item (HU-16). */
    @PutMapping("/{id}")
    public CatalogItemDetailResponse update(@PathVariable Long id,
                                            @Valid @RequestBody CatalogItemRequest request) {
        return CatalogApiMapper.toResponse(
                this.catalogCommandService.update(id, request.toUpdateCommand()));
    }

    /** Logically deletes an item (HU-18). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        this.catalogCommandService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Activates or deactivates an item (HU-17).
     *
     * @param active whether the item is published
     */
    @PatchMapping("/{id}/active")
    public ResponseEntity<Void> setActive(@PathVariable Long id,
                                           @RequestParam boolean active) {
        if (active) {
            this.catalogCommandService.reactivate(id);
        } else {
            this.catalogCommandService.deactivate(id);
        }
        return ResponseEntity.noContent().build();
    }

    /** Applies a discount to an item (HU-19). */
    @PutMapping("/{id}/discount")
    public ResponseEntity<Void> applyDiscount(@PathVariable Long id,
                                              @Valid @RequestBody DiscountRequest request) {
        this.catalogCommandService.applyDiscount(id, new ApplyDiscountCommand(
                request.percentage(), request.startsAt(), request.endsAt()));
        return ResponseEntity.noContent().build();
    }

    /** Revokes the discount in force of an item (HU-19). */
    @DeleteMapping("/{id}/discount")
    public ResponseEntity<Void> revokeDiscount(@PathVariable Long id) {
        this.catalogCommandService.revokeDiscount(id);
        return ResponseEntity.noContent().build();
    }

    // --- the body of the catalogue ------------------------------------------

    /**
     * The body of the creation and of the edition of an item.
     *
     * @param type {@code PRODUCT} or {@code SERVICE}
     * @param name the display name
     * @param description the long description
     * @param price the base price
     * @param stock the units available, null when the stock is not tracked
     * @param images the image URLs
     * @param specifications the specification table
     * @param categoryIds the categories the item belongs to
     * @param featured whether the item is featured
     */
    public record CatalogItemRequest(
            @NotNull ItemType type,
            @NotBlank String name,
            String description,
            @NotNull @DecimalMin("0.00") BigDecimal price,
            @Min(0) Integer stock,
            List<String> images,
            List<Specification> specifications,
            List<Long> categoryIds,
            boolean featured) {

        public CatalogItemRequest {
            images = images == null ? List.of() : List.copyOf(images);
            specifications = specifications == null ? List.of() : List.copyOf(specifications);
            categoryIds = categoryIds == null ? List.of() : List.copyOf(categoryIds);
            description = description == null ? "" : description;
        }

        CreateCatalogItemCommand toCreateCommand() {
            return new CreateCatalogItemCommand(this.type, this.name, this.description, this.price,
                    this.stock, this.images, this.specifications, this.categoryIds, this.featured);
        }

        UpdateCatalogItemCommand toUpdateCommand() {
            return new UpdateCatalogItemCommand(this.type, this.name, this.description, this.price,
                    this.stock, this.images, this.specifications, this.categoryIds, this.featured);
        }
    }

    /**
     * The body of the application of a discount.
     *
     * @param percentage the discount, from 0 to 100
     * @param startsAt when the discount starts
     * @param endsAt when the discount ends
     */
    public record DiscountRequest(
            @Min(0) int percentage,
            @JsonFormat(pattern = "YYYY-mm-dd")
            Instant startsAt,
            Instant endsAt) {
    }

    // --- the parsing of the parameters --------------------------------------

    private ItemType parseType(String type) {
        if (blankToNull(type) == null) {
            return null;
        }
        try {
            return ItemType.valueOf(type.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private CatalogSort parseSort(String sort) {
        if (blankToNull(sort) == null) {
            return CatalogSort.NEWEST;
        }
        try {
            return CatalogSort.valueOf(sort.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return CatalogSort.NEWEST;
        }
    }

    private ItemStateFilter parseState(String state) {
        if (blankToNull(state) == null) {
            return ItemStateFilter.ALL;
        }
        try {
            return ItemStateFilter.valueOf(state.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return ItemStateFilter.ALL;
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
