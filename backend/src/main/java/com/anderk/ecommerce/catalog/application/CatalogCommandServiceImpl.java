package com.anderk.ecommerce.catalog.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.anderk.ecommerce.catalog.application.command.ApplyDiscountCommand;
import com.anderk.ecommerce.catalog.application.command.CreateCatalogItemCommand;
import com.anderk.ecommerce.catalog.application.command.UpdateCatalogItemCommand;
import com.anderk.ecommerce.catalog.application.view.CatalogItemDetail;
import com.anderk.ecommerce.catalog.application.view.PriceView;
import com.anderk.ecommerce.catalog.application.view.SpecificationView;
import com.anderk.ecommerce.catalog.domain.event.StockReplenishedEvent;
import com.anderk.ecommerce.catalog.domain.model.CatalogItem;
import com.anderk.ecommerce.catalog.domain.model.Category;
import com.anderk.ecommerce.catalog.domain.model.Discount;
import com.anderk.ecommerce.catalog.domain.policy.PricingPolicy;
import com.anderk.ecommerce.catalog.domain.policy.StockPolicy;
import com.anderk.ecommerce.catalog.domain.port.CatalogRepository;
import com.anderk.ecommerce.catalog.domain.port.CategoryRepository;
import com.anderk.ecommerce.catalog.domain.port.DiscountRepository;
import com.anderk.ecommerce.catalog.domain.port.DomainEventPublisher;
import com.anderk.ecommerce.catalog.domain.vo.Money;
import com.anderk.ecommerce.catalog.domain.vo.Slug;
import com.anderk.ecommerce.domain.error.ConflictException;
import com.anderk.ecommerce.domain.error.NotFoundException;
import com.anderk.ecommerce.domain.error.ValidationException;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

/**
 * The use cases that write the catalog.
 *
 * <p>Every mutation goes through the entity, so the change of the stock is
 * written in the same transaction that discounts it. Nothing here knows
 * about HTTP.
 */
@Transactional
public class CatalogCommandServiceImpl implements CatalogCommandService {

    private final CatalogRepository catalogRepository;

    private final CategoryRepository categoryRepository;

    private final DiscountRepository discountRepository;

    private final PricingPolicy pricingPolicy;

    private final DomainEventPublisher eventPublisher;

    public CatalogCommandServiceImpl(CatalogRepository catalogRepository,
                                      CategoryRepository categoryRepository,
                                      DiscountRepository discountRepository,
                                      PricingPolicy pricingPolicy,
                                      DomainEventPublisher eventPublisher) {
        this.catalogRepository = catalogRepository;
        this.categoryRepository = categoryRepository;
        this.discountRepository = discountRepository;
        this.pricingPolicy = pricingPolicy;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @CacheEvict(cacheNames = {"catalog-home", "catalog-detail", "catalog-listings"}, allEntries = true)
    public CatalogItemDetail create(CreateCatalogItemCommand command) {
        validate(command.price(), command.stock());

        CatalogItem item = new CatalogItem(
                null,
                command.type(),
                command.name().trim(),
                uniqueSlug(command.name()),
                command.description(),
                Money.of(command.price()),
                command.stock(),
                command.images(),
                command.specifications(),
                resolveCategories(command.categoryIds()),
                command.featured(),
                true,
                null,
                Instant.now());

        CatalogItem saved = this.catalogRepository.save(item);
        return toDetail(saved, null);
    }

    @Override
    @CacheEvict(cacheNames = {"catalog-home", "catalog-detail", "catalog-listings"}, allEntries = true)
    public CatalogItemDetail update(Long id, UpdateCatalogItemCommand command) {
        validate(command.price(), command.stock());

        CatalogItem existing = this.catalogRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe el ítem " + id));

        CatalogItem updated = new CatalogItem(
                existing.id(),
                command.type(),
                command.name().trim(),
                existing.slug(),
                command.description(),
                Money.of(command.price()),
                command.stock(),
                command.images(),
                command.specifications(),
                resolveCategories(command.categoryIds()),
                command.featured(),
                existing.active(),
                existing.deletedAt(),
                existing.createdAt());

        CatalogItem saved = this.catalogRepository.save(updated);

        // The stock went from zero to a number above zero: the pending
        // subscriptions of the item have to be notified (HU-08).
        if (isReplenished(existing.stock(), saved.stock())) {
            this.eventPublisher.publish(new StockReplenishedEvent(
                    saved.id(), existing.stock() == null ? 0 : existing.stock(), saved.stock()));
        }

        return toDetail(saved, null);
    }

    /** Whether the stock went from zero to a number above zero. */
    private boolean isReplenished(Integer previousStock, Integer newStock) {
        return newStock != null && newStock > 0 && (previousStock == null || previousStock == 0);
    }

    @Override
    @CacheEvict(cacheNames = {"catalog-home", "catalog-detail", "catalog-listings"}, allEntries = true)
    public void deactivate(Long id) {
        CatalogItem item = this.require(id);
        this.catalogRepository.save(item.deactivated());
    }

    @Override
    @CacheEvict(cacheNames = {"catalog-home", "catalog-detail", "catalog-listings"}, allEntries = true)
    public void reactivate(Long id) {
        CatalogItem item = this.require(id);
        this.catalogRepository.save(item.reactivated());
    }

    @Override
    @CacheEvict(cacheNames = {"catalog-home", "catalog-detail", "catalog-listings"}, allEntries = true)
    public void delete(Long id) {
        CatalogItem item = this.require(id);
        this.catalogRepository.save(item.softDeleted(Instant.now()));
    }

    @Override
    @CacheEvict(cacheNames = {"catalog-home", "catalog-detail", "catalog-listings"}, allEntries = true)
    public void applyDiscount(Long id, ApplyDiscountCommand command) {
        CatalogItem item = this.require(id);

        // One current discount per item (mission 7.7): the new one replaces
        // the previous one, which is revoked the same way.
        this.discountRepository.findEffectiveByItem(item.id(), Instant.now())
                .ifPresent(previous -> this.discountRepository.save(
                        new Discount(previous.id(), previous.itemId(), previous.percentage(),
                                previous.startsAt(), previous.endsAt(), Instant.now())));

        this.discountRepository.save(new Discount(
                null, item.id(), command.percentage(), command.startsAt(), command.endsAt(), null));
    }

    @Override
    @CacheEvict(cacheNames = {"catalog-home", "catalog-detail", "catalog-listings"}, allEntries = true)
    public void revokeDiscount(Long id) {
        CatalogItem item = this.require(id);
        Discount discount = this.discountRepository.findEffectiveByItem(item.id(), Instant.now())
                .orElseThrow(() -> new ConflictException("El ítem " + id + " no tiene ningún descuento vigente"));

        this.discountRepository.save(new Discount(discount.id(), discount.itemId(), discount.percentage(),
                discount.startsAt(), discount.endsAt(), Instant.now()));
    }

    // --- helpers ------------------------------------------------------------

    private CatalogItem require(Long id) {
        return this.catalogRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe el ítem " + id));
    }

    private void validate(BigDecimal price, Integer stock) {
        if (price == null || price.signum() < 0) {
            throw new ValidationException("El precio no puede ser negativo",
                    com.anderk.ecommerce.domain.error.ErrorDetail.of("price", "no puede ser negativo"));
        }
        if (stock != null && stock < 0) {
            throw new ValidationException("El stock no puede ser negativo",
                    com.anderk.ecommerce.domain.error.ErrorDetail.of("stock", "no puede ser negativo"));
        }
    }

    /** A slug that is unique, derived from the name of the item. */
    private Slug uniqueSlug(String name) {
        Slug candidate = Slug.of(name);
        String value = candidate.value();
        int suffix = 2;
        while (this.catalogRepository.existsBySlug(value)) {
            value = candidate.value() + "-" + suffix;
            suffix++;
        }
        return new Slug(value);
    }

    private List<Category> resolveCategories(List<Long> categoryIds) {
        return categoryIds.stream()
                .map(categoryId -> this.categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new NotFoundException("No existe la categoría " + categoryId)))
                .toList();
    }

    private CatalogItemDetail toDetail(CatalogItem item, Discount discount) {
        var breakdown = this.pricingPolicy.calculate(item.price().amount(), discount, Instant.now());
        PriceView price = new PriceView(breakdown.basePrice(), breakdown.finalPrice(),
                breakdown.discountPercentage(), item.price().currency());

        return new CatalogItemDetail(
                item.id(),
                item.slug().value(),
                item.name(),
                item.type(),
                item.description(),
                price,
                item.images(),
                item.specifications().stream()
                        .map(specification -> new SpecificationView(specification.name(), specification.value()))
                        .toList(),
                item.categories().stream().map(Category::name).toList(),
                item.featured(),
                item.isPurchasable(),
                item.stock(),
                new com.anderk.ecommerce.catalog.domain.policy.StockPolicy()
                        .stateOf(item.stock(), StockPolicy.DEFAULT_LOW_STOCK_THRESHOLD));
    }
}
