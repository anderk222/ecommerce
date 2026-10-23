package com.anderk.ecommerce.lead.application.usecases;

/**
 * The use case that deletes a lead (HU-21).
 */
public interface DeleteLeadUseCase {

    /**
     * Deletes a lead.
     *
     * @throws com.anderk.ecommerce.domain.error.NotFoundException if the lead does not exist
     */
    void delete(Long id);
}
