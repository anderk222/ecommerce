package com.anderk.ecommerce.catalog.domain.search;

/**
 * The orderings the catalog offers (HU-03 and HU-04).
 *
 * <p>Each ordering says <em>what</em> it sorts by and <em>in which direction</em>,
 * in the words of the domain. How a {@link Field} becomes a column is the
 * business of the persistence adapter, not of this enum.
 */
public enum CatalogSort {

    /** The newest first — the default. */
    NEWEST(Field.CREATED_AT, Direction.DESC),

    /** The oldest first. */
    OLDEST(Field.CREATED_AT, Direction.ASC),

    /** Cheapest first. */
    PRICE_ASC(Field.PRICE, Direction.ASC),

    /** Most expensive first. */
    PRICE_DESC(Field.PRICE, Direction.DESC),

    /** Alphabetically, A to Z. */
    NAME_ASC(Field.NAME, Direction.ASC),

    /** Alphabetically, Z to A. */
    NAME_DESC(Field.NAME, Direction.DESC),

    /** The most bought first. */
    BEST_SELLING(Field.SALES, Direction.DESC);

    /** The ordering used when the caller does not ask for one. */
    public static final CatalogSort DEFAULT = NEWEST;

    /** What an item can be sorted by. */
    public enum Field {
        CREATED_AT,
        PRICE,
        NAME,
        SALES
    }

    public enum Direction {
        ASC,
        DESC
    }

    private final Field field;

    private final Direction direction;

    CatalogSort(Field field, Direction direction) {
        this.field = field;
        this.direction = direction;
    }

    public Field field() {
        return this.field;
    }

    public Direction direction() {
        return this.direction;
    }
}
