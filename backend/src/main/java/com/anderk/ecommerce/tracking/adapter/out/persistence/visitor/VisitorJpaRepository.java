package com.anderk.ecommerce.tracking.adapter.out.persistence.visitor;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitorJpaRepository extends JpaRepository<VisitorJpaEntity, java.util.UUID> {
}
