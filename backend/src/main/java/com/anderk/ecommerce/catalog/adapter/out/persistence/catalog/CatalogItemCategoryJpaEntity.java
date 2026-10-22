package com.anderk.ecommerce.catalog.adapter.out.persistence.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.anderk.ecommerce.catalog.adapter.out.persistence.category.CategoryJpaEntity;

import org.springframework.data.annotation.CreatedDate;

/**
 * The junction between an item and a category.
 *
 * <p>It is an entity of its own, not a plain {@code @ManyToMany} join table,
 * so that the link can carry its own timestamps and be written on its own.
 */
@Entity
@Table(name = "catalog_item_categories")
@EntityListeners(AuditingEntityListener.class)
public class CatalogItemCategoryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private CatalogItemJpaEntity item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoryJpaEntity category;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private java.time.Instant createdAt;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CatalogItemJpaEntity getItem() {
        return this.item;
    }

    public void setItem(CatalogItemJpaEntity item) {
        this.item = item;
    }

    public CategoryJpaEntity getCategory() {
        return this.category;
    }

    public void setCategory(CategoryJpaEntity category) {
        this.category = category;
    }

    public java.time.Instant getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(java.time.Instant createdAt) {
        this.createdAt = createdAt;
    }
}
