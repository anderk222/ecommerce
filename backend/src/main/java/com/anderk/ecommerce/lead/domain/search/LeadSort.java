package com.anderk.ecommerce.lead.domain.search;

/**
 * The orderings the listing of the leads offers (HU-21).
 *
 * <p>Each ordering says <em>what</em> it sorts by and <em>in which direction</em>,
 * in the words of the domain. How a {@link Field} becomes a column is the
 * business of the persistence adapter, not of this enum.
 */
public enum LeadSort {

    /** The newest first — the default. */
    NEWEST(Field.CREATED_AT, Direction.DESC),

    /** The oldest first. */
    OLDEST(Field.CREATED_AT, Direction.ASC),

    /** Alphabetically by name, A to Z. */
    NAME_ASC(Field.NAME, Direction.ASC),

    /** Alphabetically by name, Z to A. */
    NAME_DESC(Field.NAME, Direction.DESC),

    /** The biggest sale first. */
    SALE_AMOUNT_DESC(Field.SALE_AMOUNT, Direction.DESC),

    /** The most recently touched first. */
    UPDATED_DESC(Field.UPDATED_AT, Direction.DESC);

    /** The ordering used when the caller does not ask for one. */
    public static final LeadSort DEFAULT = NEWEST;

    /** What a lead can be sorted by. */
    public enum Field {
        CREATED_AT,
        NAME,
        SALE_AMOUNT,
        UPDATED_AT
    }

    public enum Direction {
        ASC,
        DESC
    }

    private final Field field;

    private final Direction direction;

    LeadSort(Field field, Direction direction) {
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
