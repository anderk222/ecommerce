package com.anderk.ecommerce.lead.application.usecases;

import java.time.Instant;

import com.anderk.ecommerce.domain.error.NotFoundException;
import com.anderk.ecommerce.lead.application.command.UpdateLeadCommand;
import com.anderk.ecommerce.lead.application.view.LeadView;
import com.anderk.ecommerce.lead.domain.model.Lead;
import com.anderk.ecommerce.lead.domain.port.LeadRepository;

import org.springframework.transaction.annotation.Transactional;

/**
 * The use case that edits the data of a lead (HU-21).
 */
@Transactional
public class UpdateLeadUseCaseImpl implements UpdateLeadUseCase {

    private final LeadRepository leadRepository;

    private final LeadViewMapper viewMapper;

    public UpdateLeadUseCaseImpl(LeadRepository leadRepository, LeadViewMapper viewMapper) {
        this.leadRepository = leadRepository;
        this.viewMapper = viewMapper;
    }

    @Override
    public LeadView update(Long id, UpdateLeadCommand command) {
        Lead existing = this.leadRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe el lead " + id));

        Lead updated = new Lead(
                existing.id(),
                command.name() == null || command.name().isBlank() ? existing.name() : command.name(),
                existing.email(),
                command.phone(),
                command.province() == null ? existing.province() : command.province(),
                existing.status(),
                existing.sources(),
                command.saleAmount() == null ? existing.saleAmount() : command.saleAmount(),
                existing.outcome(),
                existing.consent() || command.consent(),
                existing.createdAt(),
                Instant.now());

        return this.viewMapper.toView(this.leadRepository.save(updated));
    }
}
