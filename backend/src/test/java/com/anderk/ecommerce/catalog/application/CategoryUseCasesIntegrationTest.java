package com.anderk.ecommerce.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.anderk.ecommerce.TestcontainersConfiguration;
import com.anderk.ecommerce.catalog.application.usecases.CreateCategoryUseCase;
import com.anderk.ecommerce.catalog.application.usecases.DeleteCategoryUseCase;
import com.anderk.ecommerce.catalog.application.usecases.UpdateCategoryUseCase;
import com.anderk.ecommerce.catalog.domain.model.CatalogItem;
import com.anderk.ecommerce.catalog.domain.model.Category;
import com.anderk.ecommerce.catalog.domain.model.ItemType;
import com.anderk.ecommerce.catalog.domain.port.CatalogRepository;
import com.anderk.ecommerce.catalog.domain.port.CategoryRepository;
import com.anderk.ecommerce.catalog.domain.vo.Money;
import com.anderk.ecommerce.catalog.domain.vo.Slug;
import com.anderk.ecommerce.domain.error.ConflictException;
import com.anderk.ecommerce.domain.error.NotFoundException;
import com.anderk.ecommerce.domain.error.ValidationException;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * The administration of the categories (HU-20).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class CategoryUseCasesIntegrationTest {

    @Autowired
    private CreateCategoryUseCase createCategoryUseCase;

    @Autowired
    private UpdateCategoryUseCase updateCategoryUseCase;

    @Autowired
    private DeleteCategoryUseCase deleteCategoryUseCase;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CatalogRepository catalogRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("the slug is generated from the name and it is unique")
    void theSlugIsGeneratedFromTheNameAndItIsUnique() {
        var first = this.createCategoryUseCase.create("Hogar y jardín");
        var second = this.createCategoryUseCase.create("Hogar y jardín");

        assertThat(first.slug()).isEqualTo("hogar-y-jardin");
        assertThat(second.slug()).isEqualTo("hogar-y-jardin-2");
    }

    @Test
    @DisplayName("a category without a name is refused")
    void aCategoryWithoutANameIsRefused() {
        assertThatThrownBy(() -> this.createCategoryUseCase.create("  "))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("a category can be renamed, and its slug does not change")
    void aCategoryCanBeRenamedAndItsSlugDoesNotChange() {
        var created = this.createCategoryUseCase.create("Papelería de prueba");

        var renamed = this.updateCategoryUseCase.update(created.id(), "Papelería y útiles");

        assertThat(renamed.name()).isEqualTo("Papelería y útiles");
        assertThat(renamed.slug()).isEqualTo("papeleria-de-prueba");
    }

    @Test
    @DisplayName("a category that holds items cannot be deleted")
    void aCategoryThatHoldsItemsCannotBeDeleted() {
        Category category = this.categoryRepository.save(new Category(null, "Con ítems", Slug.of("con-items")));

        // The category holds an item: the deletion has to be refused.
        this.catalogRepository.save(new CatalogItem(
                null, ItemType.PRODUCT, "Ítem con categoría", Slug.of("Ítem con categoría"),
                "Un ítem que pertenece a la categoría.", Money.of(new java.math.BigDecimal("10.00")), 1,
                java.util.List.of(), java.util.List.of(), java.util.List.of(category),
                false, true, null, java.time.Instant.now()));
        this.entityManager.flush();

        assertThatThrownBy(() -> this.deleteCategoryUseCase.delete(category.id()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ítems asignados");
    }

    @Test
    @DisplayName("a category without items can be deleted")
    void aCategoryWithoutItemsCanBeDeleted() {
        var created = this.createCategoryUseCase.create("Efímera");

        this.deleteCategoryUseCase.delete(created.id());

        assertThat(this.categoryRepository.findById(created.id())).isEmpty();
    }

    @Test
    @DisplayName("a category that does not exist answers 404")
    void aCategoryThatDoesNotExistAnswers404() {
        assertThatThrownBy(() -> this.deleteCategoryUseCase.delete(999_999L))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> this.updateCategoryUseCase.update(999_999L, "Nada"))
                .isInstanceOf(NotFoundException.class);
    }
}
