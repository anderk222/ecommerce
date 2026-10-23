package com.anderk.ecommerce.catalog.adapter.in.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.anderk.ecommerce.catalog.adapter.in.web.dto.catalog.AdminCatalogSearchRequest;
import com.anderk.ecommerce.catalog.adapter.in.web.dto.catalog.CatalogItemAdminDetailResponse;
import com.anderk.ecommerce.catalog.adapter.in.web.dto.catalog.CatalogItemAdminResponse;
import com.anderk.ecommerce.catalog.adapter.in.web.dto.catalog.CatalogItemDetailResponse;
import com.anderk.ecommerce.catalog.application.command.ApplyDiscountCommand;
import com.anderk.ecommerce.catalog.application.command.CreateCatalogItemCommand;
import com.anderk.ecommerce.catalog.application.command.UpdateCatalogItemCommand;
import com.anderk.ecommerce.catalog.application.usecases.ApplyDiscountUseCase;
import com.anderk.ecommerce.catalog.application.usecases.CreateCatalogItemUseCase;
import com.anderk.ecommerce.catalog.application.usecases.DeactivateCatalogItemUseCase;
import com.anderk.ecommerce.catalog.application.usecases.DeleteCatalogItemUseCase;
import com.anderk.ecommerce.catalog.application.usecases.GetAdminCatalogItemUseCase;
import com.anderk.ecommerce.catalog.application.usecases.ReactivateCatalogItemUseCase;
import com.anderk.ecommerce.catalog.application.usecases.RevokeDiscountUseCase;
import com.anderk.ecommerce.catalog.application.usecases.SearchAdminCatalogUseCase;
import com.anderk.ecommerce.catalog.application.usecases.UpdateCatalogItemUseCase;
import com.anderk.ecommerce.catalog.domain.model.ItemType;
import com.anderk.ecommerce.catalog.domain.model.Specification;
import com.anderk.ecommerce.domain.pagination.PageResponse;
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

    private final SearchAdminCatalogUseCase searchAdminCatalogUseCase;

    private final GetAdminCatalogItemUseCase getAdminCatalogItemUseCase;

    private final CreateCatalogItemUseCase createCatalogItemUseCase;

    private final UpdateCatalogItemUseCase updateCatalogItemUseCase;

    private final DeactivateCatalogItemUseCase deactivateCatalogItemUseCase;

    private final ReactivateCatalogItemUseCase reactivateCatalogItemUseCase;

    private final DeleteCatalogItemUseCase deleteCatalogItemUseCase;

    private final ApplyDiscountUseCase applyDiscountUseCase;

    private final RevokeDiscountUseCase revokeDiscountUseCase;

    public AdminCatalogController(SearchAdminCatalogUseCase searchAdminCatalogUseCase,
                                  GetAdminCatalogItemUseCase getAdminCatalogItemUseCase,
                                  CreateCatalogItemUseCase createCatalogItemUseCase,
                                  UpdateCatalogItemUseCase updateCatalogItemUseCase,
                                  DeactivateCatalogItemUseCase deactivateCatalogItemUseCase,
                                  ReactivateCatalogItemUseCase reactivateCatalogItemUseCase,
                                  DeleteCatalogItemUseCase deleteCatalogItemUseCase,
                                  ApplyDiscountUseCase applyDiscountUseCase,
                                  RevokeDiscountUseCase revokeDiscountUseCase) {
        this.searchAdminCatalogUseCase = searchAdminCatalogUseCase;
        this.getAdminCatalogItemUseCase = getAdminCatalogItemUseCase;
        this.createCatalogItemUseCase = createCatalogItemUseCase;
        this.updateCatalogItemUseCase = updateCatalogItemUseCase;
        this.deactivateCatalogItemUseCase = deactivateCatalogItemUseCase;
        this.reactivateCatalogItemUseCase = reactivateCatalogItemUseCase;
        this.deleteCatalogItemUseCase = deleteCatalogItemUseCase;
        this.applyDiscountUseCase = applyDiscountUseCase;
        this.revokeDiscountUseCase = revokeDiscountUseCase;
    }

    /**
     * The unified listing of the administration (HU-14). The parameters are
     * described in {@link AdminCatalogSearchRequest}.
     */
    @GetMapping
    public PageResponse<CatalogItemAdminResponse> list(AdminCatalogSearchRequest request) {
        return this.searchAdminCatalogUseCase
                .search(request.toCriteria(), request.toPageRequest())
                .map(CatalogItemAdminResponse::of);
    }

    /** Reads an item for its edition (HU-16), whatever its state. */
    @GetMapping("/{id}")
    public CatalogItemAdminDetailResponse get(@PathVariable Long id) {
        return CatalogItemAdminDetailResponse.of(this.getAdminCatalogItemUseCase.getById(id));
    }

    /** Creates an item (HU-15). */
    @PostMapping
    public ResponseEntity<CatalogItemDetailResponse> create(
            @Valid @RequestBody CatalogItemRequest request) {
        CatalogItemDetailResponse created = CatalogApiMapper.toResponse(
                this.createCatalogItemUseCase.create(request.toCreateCommand()));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** Edits an item (HU-16). */
    @PutMapping("/{id}")
    public CatalogItemDetailResponse update(@PathVariable Long id,
                                            @Valid @RequestBody CatalogItemRequest request) {
        return CatalogApiMapper.toResponse(
                this.updateCatalogItemUseCase.update(id, request.toUpdateCommand()));
    }

    /** Logically deletes an item (HU-18). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        this.deleteCatalogItemUseCase.delete(id);
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
            this.reactivateCatalogItemUseCase.reactivate(id);
        } else {
            this.deactivateCatalogItemUseCase.deactivate(id);
        }
        return ResponseEntity.noContent().build();
    }

    /** Applies a discount to an item (HU-19). */
    @PutMapping("/{id}/discount")
    public ResponseEntity<Void> applyDiscount(@PathVariable Long id,
                                              @Valid @RequestBody DiscountRequest request) {
        this.applyDiscountUseCase.apply(id, new ApplyDiscountCommand(
                request.percentage(), request.startsAt(), request.endsAt()));
        return ResponseEntity.noContent().build();
    }

    /** Revokes the discount in force of an item (HU-19). */
    @DeleteMapping("/{id}/discount")
    public ResponseEntity<Void> revokeDiscount(@PathVariable Long id) {
        this.revokeDiscountUseCase.revoke(id);
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
}
