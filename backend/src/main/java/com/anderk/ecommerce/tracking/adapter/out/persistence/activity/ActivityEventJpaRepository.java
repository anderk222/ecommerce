package com.anderk.ecommerce.tracking.adapter.out.persistence.activity;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityEventJpaRepository extends JpaRepository<ActivityEventJpaEntity, Long> {

    List<ActivityEventJpaEntity> findByLeadIdOrderByCreatedAtDesc(Long leadId, org.springframework.data.domain.Pageable pageable);

    List<ActivityEventJpaEntity> findByAnonymousIdOrderByCreatedAtDesc(java.util.UUID anonymousId, org.springframework.data.domain.Pageable pageable);

    /**
     * Links the events of a browser that are not linked yet.
     *
     * <p>It is the only place where a direct update is allowed: the table is
     * not audited, and the change is exactly what HU-10 asks for.
     */
    @Modifying
    @Query("update ActivityEventJpaEntity e set e.leadId = :leadId "
            + "where e.anonymousId = :anonymousId and e.leadId is null")
    int linkToLead(@Param("anonymousId") java.util.UUID anonymousId, @Param("leadId") Long leadId);
}
