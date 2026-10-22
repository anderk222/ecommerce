package com.anderk.ecommerce.catalog.domain.model;

import java.io.Serializable;

/**
 * One row of the specification table of an item.
 *
 * <p>It is {@link Serializable} because the JPA specification requires the
 * attributes of an entity to be serializable — the column is a {@code jsonb}
 * and the value is cloned when the entity is written.
 *
 * @param name the label, for instance "Material"
 * @param value the value, for instance "Algodón"
 */
public record Specification(String name, String value) implements Serializable {
}
