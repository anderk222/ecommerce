package com.anderk.ecommerce.lead.application.usecases;

import com.anderk.ecommerce.domain.error.NotFoundException;
import com.anderk.ecommerce.lead.domain.port.LeadRepository;

import org.springframework.transaction.annotation.Transactional;

/**
 * The use case that deletes a lead (HU-21).
 */
@Transactional
public class DeleteLeadUseCaseImpl implements DeleteLeadUseCase {

    private final LeadRepository leadRepository;

    public DeleteLeadUseCaseImpl(LeadRepository leadRepository) {
        this.leadRepository = leadRepository;
    }

    @Override
    public void delete(Long id) {
        if (this.leadRepository.findById(id).isEmpty()) {
            throw new NotFoundException("No existe el lead " + id);
        }
        this.leadRepository.deleteById(id);
    }
}
