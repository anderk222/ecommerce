package com.anderk.ecommerce.lead.adapter.out.persistence.lead;

import static com.anderk.ecommerce.lead.adapter.out.persistence.lead.LeadAttributes.CREATED_AT;
import static com.anderk.ecommerce.lead.adapter.out.persistence.lead.LeadAttributes.ID;
import static com.anderk.ecommerce.lead.adapter.out.persistence.lead.LeadAttributes.NAME;
import static com.anderk.ecommerce.lead.adapter.out.persistence.lead.LeadAttributes.SALE_AMOUNT;
import static com.anderk.ecommerce.lead.adapter.out.persistence.lead.LeadAttributes.UPDATED_AT;

import java.util.ArrayList;
import java.util.List;

import com.anderk.ecommerce.lead.domain.search.LeadSort;

import org.springframework.data.domain.Sort;

/**
 * Turns a domain {@link LeadSort} into the {@link Sort} that the database
 * understands. It is the only place that knows both vocabularies.
 *
 * <p>An ordering is made of three parts, always in this order:
 * <ol>
 *   <li>the field the caller asked for, in the direction they asked for;
 *   <li>a second field for the cases where the first one often repeats (equal
 *       sale amounts);
 *   <li>the identifier, in the same direction as the first field. Without it
 *       two rows that tie could swap places between page 1 and page 2, and a
 *       lead would show up twice or never.
 * </ol>
 */
public final class LeadSorting {

    private LeadSorting() {
    }

    public static Sort of(LeadSort sort) {
        LeadSort effective = sort == null ? LeadSort.DEFAULT : sort;
        Sort.Direction direction = effective.direction() == LeadSort.Direction.ASC
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        List<Sort.Order> orders = new ArrayList<>();
        orders.add(new Sort.Order(direction, attributeOf(effective.field())));
        orders.addAll(thenBy(effective.field()));
        orders.add(new Sort.Order(direction, ID));
        return Sort.by(orders);
    }

    private static String attributeOf(LeadSort.Field field) {
        return switch (field) {
            case CREATED_AT -> CREATED_AT;
            case NAME -> NAME;
            case SALE_AMOUNT -> SALE_AMOUNT;
            case UPDATED_AT -> UPDATED_AT;
        };
    }

    /** The second ordering of the fields that are likely to tie. */
    private static List<Sort.Order> thenBy(LeadSort.Field field) {
        return switch (field) {
            case SALE_AMOUNT -> List.of(Sort.Order.asc(NAME));
            case CREATED_AT, NAME, UPDATED_AT -> List.of();
        };
    }
}
