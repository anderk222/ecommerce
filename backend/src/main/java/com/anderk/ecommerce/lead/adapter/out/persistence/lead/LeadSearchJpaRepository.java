package com.anderk.ecommerce.lead.adapter.out.persistence.lead;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.anderk.ecommerce.lead.domain.model.Province;
import com.anderk.ecommerce.lead.domain.model.enums.LeadStatus;

public interface LeadSearchJpaRepository extends JpaRepository<LeadJpaEntity, Long> {

    /**
     * The leads that match the criteria.
     *
     * <p>Every filter is optional; a null parameter means "do not filter by
     * this".
     */
    @Query("""
            select l from LeadJpaEntity l
            where (:queryPattern is null
                   or lower(l.name) like :queryPattern
                   or lower(l.email) like :queryPattern
                   or lower(coalesce(l.phone, '')) like :queryPattern)
              and (:status is null or l.status = :status)
              and (:province is null or l.province = :province)
              and (:sourcePattern is null or l.sources like :sourcePattern)
            """)
    List<LeadJpaEntity> search(@Param("queryPattern") String queryPattern,
                                @Param("status") LeadStatus status,
                                @Param("province") Province province,
                                @Param("sourcePattern") String sourcePattern);

    /**
     * The leads of a range of dates.
     *
     * <p>It is a query of its own: a null date has no type in PostgreSQL, so
     * the range cannot be an optional filter of the query above.
     */
    @Query("""
            select l from LeadJpaEntity l
            where l.createdAt >= :createdFrom and l.createdAt <= :createdTo
              and (:queryPattern is null
                   or lower(l.name) like :queryPattern
                   or lower(l.email) like :queryPattern
                   or lower(coalesce(l.phone, '')) like :queryPattern)
              and (:status is null or l.status = :status)
              and (:province is null or l.province = :province)
              and (:sourcePattern is null or l.sources like :sourcePattern)
            """)
    List<LeadJpaEntity> searchInRange(@Param("queryPattern") String queryPattern,
                                       @Param("status") LeadStatus status,
                                       @Param("province") Province province,
                                       @Param("sourcePattern") String sourcePattern,
                                       @Param("createdFrom") java.time.Instant createdFrom,
                                       @Param("createdTo") java.time.Instant createdTo);
}
