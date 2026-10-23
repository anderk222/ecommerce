package com.anderk.ecommerce.lead.adapter.out.persistence.lead;

import static com.anderk.ecommerce.lead.adapter.out.persistence.lead.LeadAttributes.CREATED_AT;
import static com.anderk.ecommerce.lead.adapter.out.persistence.lead.LeadAttributes.EMAIL;
import static com.anderk.ecommerce.lead.adapter.out.persistence.lead.LeadAttributes.NAME;
import static com.anderk.ecommerce.lead.adapter.out.persistence.lead.LeadAttributes.PHONE;
import static com.anderk.ecommerce.lead.adapter.out.persistence.lead.LeadAttributes.PROVINCE;
import static com.anderk.ecommerce.lead.adapter.out.persistence.lead.LeadAttributes.SOURCES;
import static com.anderk.ecommerce.lead.adapter.out.persistence.lead.LeadAttributes.STATUS;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.anderk.ecommerce.lead.domain.model.Province;
import com.anderk.ecommerce.lead.domain.model.enums.LeadSource;
import com.anderk.ecommerce.lead.domain.model.enums.LeadStatus;
import com.anderk.ecommerce.lead.domain.search.LeadSearchCriteria;

import jakarta.persistence.criteria.Expression;

import org.springframework.data.jpa.domain.Specification;

/**
 * The filters of the lead search, one small {@link Specification} each.
 *
 * <p>{@link #from} reads a {@link LeadSearchCriteria} and joins, with AND, only
 * the filters the criteria actually carry. The query is built when it is run,
 * so there is no JPQL with {@code :param is null or ...} to keep in sync with
 * the criteria.
 */
public final class LeadSpecifications {

    /** The character that escapes a wildcard inside the free text. */
    private static final char LIKE_ESCAPE = '\\';

    private LeadSpecifications() {
    }

    /** The specification of a whole search. */
    public static Specification<LeadJpaEntity> from(LeadSearchCriteria criteria) {
        List<Specification<LeadJpaEntity>> filters = new ArrayList<>();

        if (criteria.hasQuery()) {
            filters.add(containsText(criteria.query()));
        }
        if (criteria.status() != null) {
            filters.add(hasStatus(criteria.status()));
        }
        if (criteria.province() != null) {
            filters.add(inProvince(criteria.province()));
        }
        if (criteria.source() != null) {
            filters.add(hasSource(criteria.source()));
        }
        if (criteria.createdFrom() != null) {
            filters.add(createdFrom(criteria.createdFrom()));
        }
        if (criteria.createdTo() != null) {
            filters.add(createdUntil(criteria.createdTo()));
        }

        return Specification.allOf(filters);
    }

    // --- the filters --------------------------------------------------------

    /**
     * The leads whose name, email or phone contain the text, ignoring case.
     *
     * <p>Wildcards typed by the caller are escaped: a {@code %} is a literal
     * percentage, not "anything".
     */
    static Specification<LeadJpaEntity> containsText(String text) {
        String pattern = "%" + escapeLike(text.toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> {
            Expression<String> phone = cb.coalesce(root.get(PHONE), "");
            return cb.or(
                    cb.like(cb.lower(root.get(NAME)), pattern, LIKE_ESCAPE),
                    cb.like(cb.lower(root.get(EMAIL)), pattern, LIKE_ESCAPE),
                    cb.like(cb.lower(phone), pattern, LIKE_ESCAPE));
        };
    }

    /** The leads in one state of the pipeline. */
    static Specification<LeadJpaEntity> hasStatus(LeadStatus status) {
        return (root, query, cb) -> cb.equal(root.get(STATUS), status);
    }

    /** The leads of one province. */
    static Specification<LeadJpaEntity> inProvince(Province province) {
        return (root, query, cb) -> cb.equal(root.get(PROVINCE), province);
    }

    /** The leads that carry that origin among their comma-separated sources. */
    static Specification<LeadJpaEntity> hasSource(LeadSource source) {
        return (root, query, cb) -> cb.like(root.get(SOURCES), "%" + source.name() + "%");
    }

    /** The leads created at or after that instant. */
    static Specification<LeadJpaEntity> createdFrom(Instant from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get(CREATED_AT), from);
    }

    /** The leads created at or before that instant. */
    static Specification<LeadJpaEntity> createdUntil(Instant to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get(CREATED_AT), to);
    }

    /**
     * The text with every {@code LIKE} wildcard turned into a literal.
     *
     * <p>The backslash goes first: escaping it after the others would escape
     * the escapes.
     */
    private static String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
