package com.anderk.ecommerce.catalog.adapter.out.persistence.catalog;

import java.util.ArrayList;
import java.util.List;

import com.anderk.ecommerce.catalog.adapter.out.persistence.CatalogItemMapper;
import com.anderk.ecommerce.catalog.domain.model.ItemType;
import com.anderk.ecommerce.catalog.domain.model.Specification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import org.hibernate.annotations.Type;

import io.hypersistence.utils.hibernate.type.json.JsonType;

/**
 * The persistence of a catalog item.
 *
 * <p>It lives in the adapter, not in the domain: the domain model is pure
 * Java and knows nothing about the database. This class is the only place
 * where the two shapes meet — {@link CatalogItemMapper} does the
 * translation.
 */
@Entity
@Table(name = "catalog_items")
@EntityListeners(AuditingEntityListener.class)
public class CatalogItemJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private ItemType type;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal price;

    /** {@code null} means the stock is not tracked. Only {@code 0} means out of stock. */
    @Column(name = "stock")
    private Integer stock;

    @Type(JsonType.class)
    @Column(name = "images", nullable = false, columnDefinition = "jsonb")
    private List<String> images = new ArrayList<>();

    @Type(JsonType.class)
    @Column(name = "specs", nullable = false, columnDefinition = "jsonb")
    private List<Specification> specifications = new ArrayList<>();

    /**
     * The name and the description, normalised: lower case and without accents.
     *
     * <p>The public search matches against this column, so that "cafe" finds
     * "Café" (HU-02) without depending on a PostgreSQL extension.
     */
    @Column(name = "search_text", nullable = false, columnDefinition = "text")
    private String searchText = "";

    @Column(name = "is_featured", nullable = false)
    private boolean featured;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "deleted_at")
    private java.time.Instant deletedAt;

    @org.springframework.data.annotation.CreatedDate
    @Column(name = "created_at", nullable = false)
    private java.time.Instant createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private java.time.Instant updatedAt;

    /**
     * The number of confirmed purchases, as a projection.
     *
     * <p>The order is the source of truth for "most bought"; this counter is
     * the projection the catalog reads, so the catalog never has to reach
     * into the tables of another feature.
     */
    @Column(name = "sales_count", nullable = false)
    private int salesCount;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ItemType getType() {
        return this.type;
    }

    public void setType(ItemType type) {
        this.type = type;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return this.slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public java.math.BigDecimal getPrice() {
        return this.price;
    }

    public void setPrice(java.math.BigDecimal price) {
        this.price = price;
    }

    public Integer getStock() {
        return this.stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public List<String> getImages() {
        return this.images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public List<Specification> getSpecifications() {
        return this.specifications;
    }

    public void setSpecifications(List<Specification> specifications) {
        this.specifications = specifications;
    }

    public String getSearchText() {
        return this.searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public boolean isFeatured() {
        return this.featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public java.time.Instant getDeletedAt() {
        return this.deletedAt;
    }

    public void setDeletedAt(java.time.Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public java.time.Instant getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(java.time.Instant createdAt) {
        this.createdAt = createdAt;
    }

    public java.time.Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public void setUpdatedAt(java.time.Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getSalesCount() {
        return this.salesCount;
    }

    public void setSalesCount(int salesCount) {
        this.salesCount = salesCount;
    }
}
