package com.anderk.ecommerce.catalog.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import com.anderk.ecommerce.TestcontainersConfiguration;
import com.anderk.ecommerce.catalog.domain.model.CatalogItem;
import com.anderk.ecommerce.catalog.domain.model.Category;
import com.anderk.ecommerce.catalog.domain.model.ItemType;
import com.anderk.ecommerce.catalog.domain.model.Specification;
import com.anderk.ecommerce.catalog.domain.port.CatalogRepository;
import com.anderk.ecommerce.catalog.domain.port.CategoryRepository;
import com.anderk.ecommerce.catalog.domain.vo.Money;
import com.anderk.ecommerce.catalog.domain.vo.Slug;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * The persistence of the catalog against a real PostgreSQL.
 *
 * <p>It runs against the real engine — not H2 — because the `jsonb` columns
 * behave differently on anything else.
 *
 * <p>The tests are transactional and leave nothing behind.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CatalogPersistenceIntegrationTest {

    @Autowired
    private CatalogRepository catalogRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    @DisplayName("an item survives the round trip through PostgreSQL, jsonb included")
    void anItemSurvivesTheRoundTripThroughPostgres() {
        Category category = this.categoryRepository.save(new Category(null, "Pruebas", Slug.of("Pruebas Integración")));
        this.entityManager.flush();

        CatalogItem saved = this.catalogRepository.save(new CatalogItem(
                null, ItemType.PRODUCT, "Ítem de prueba", Slug.of("Ítem de prueba"),
                "Una descripción con acentos: tecnología, café, fiable.",
                Money.of(new java.math.BigDecimal("19.90")), 4,
                List.of("https://example.test/one.png", "https://example.test/two.png"),
                List.of(new Specification("Color", "Azul"), new Specification("Peso", "1 kg")),
                List.of(category),
                true, true, null, Instant.now()));

        this.entityManager.flush();
        this.entityManager.clear();

        CatalogItem reloaded = this.catalogRepository.findById(saved.id()).orElseThrow();

        assertThat(reloaded.name()).isEqualTo("Ítem de prueba");
        assertThat(reloaded.slug().value()).isEqualTo("item-de-prueba");
        assertThat(reloaded.images()).hasSize(2);
        assertThat(reloaded.specifications()).containsExactly(
                new Specification("Color", "Azul"), new Specification("Peso", "1 kg"));
        assertThat(reloaded.categories()).extracting(Category::slug).extracting(Slug::value)
                .containsExactly("pruebas-integracion");
        assertThat(reloaded.stock()).isEqualTo(4);
        assertThat(reloaded.featured()).isTrue();
    }

    @Test
    @Transactional
    @DisplayName("the specifications land in the column as a jsonb document")
    void theSpecificationsLandInTheColumnAsAJsonbDocument() {
        Category category = this.categoryRepository.save(new Category(null, "JSON", Slug.of("JSON Integración")));
        this.entityManager.flush();

        this.catalogRepository.save(new CatalogItem(
                null, ItemType.PRODUCT, "Ítem con jsonb", Slug.of("Ítem con jsonb"),
                "Comprobación del documento JSON.", Money.of(new java.math.BigDecimal("5.00")), 1,
                List.of("https://example.test/one.png"),
                List.of(new Specification("Material", "Algodón"), new Specification("Talla", "M")),
                List.of(category),
                false, true, null, Instant.now()));
        this.entityManager.flush();

        Object stored = this.entityManager.createNativeQuery(
                        "select specs from catalog_items where slug = :slug")
                .setParameter("slug", "item-con-jsonb")
                .getSingleResult();

        assertThat(stored.toString())
                .contains("Material")
                .contains("Algodón")
                .contains("Talla")
                .contains("M");
    }

    @Test
    @Transactional
    @DisplayName("a null stock means the stock is not tracked")
    void aNullStockMeansTheStockIsNotTracked() {
        Category category = this.categoryRepository.save(new Category(null, "Servicios", Slug.of("Servicios Integración")));
        this.entityManager.flush();

        CatalogItem saved = this.catalogRepository.save(new CatalogItem(
                null, ItemType.SERVICE, "Servicio de prueba", Slug.of("Servicio de prueba"),
                "Un servicio no controla stock.",
                Money.of(new java.math.BigDecimal("10.00")), null,
                List.of(), List.of(), List.of(category),
                false, true, null, Instant.now()));

        this.entityManager.flush();
        this.entityManager.clear();

        CatalogItem reloaded = this.catalogRepository.findById(saved.id()).orElseThrow();

        assertThat(reloaded.stock()).isNull();
        assertThat(reloaded.isOutOfStock()).isFalse();
        assertThat(reloaded.tracksStock()).isFalse();
    }

}
