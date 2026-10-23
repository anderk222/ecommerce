package com.anderk.ecommerce.lead.application.usecases;

import java.time.Instant;

import com.anderk.ecommerce.domain.error.NotFoundException;
import com.anderk.ecommerce.lead.application.command.ChangeLeadStatusCommand;
import com.anderk.ecommerce.lead.application.view.LeadView;
import com.anderk.ecommerce.lead.domain.model.Lead;
import com.anderk.ecommerce.lead.domain.model.LeadStatusHistory;
import com.anderk.ecommerce.lead.domain.model.enums.LeadStatus;
import com.anderk.ecommerce.lead.domain.policy.LeadStatusPolicy;
import com.anderk.ecommerce.lead.domain.port.LeadRepository;
import com.anderk.ecommerce.lead.domain.port.LeadStatusHistoryRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * The use case that changes the state of a lead (HU-22).
 */
@Transactional
public class ChangeLeadStatusUseCaseImpl implements ChangeLeadStatusUseCase {

    private final LeadRepository leadRepository;

    private final LeadStatusHistoryRepository statusHistoryRepository;

    private final LeadStatusPolicy statusPolicy;

    private final LeadViewMapper viewMapper;

    public ChangeLeadStatusUseCaseImpl(LeadRepository leadRepository,
                                       LeadStatusHistoryRepository statusHistoryRepository,
                                       LeadStatusPolicy statusPolicy,
                                       LeadViewMapper viewMapper) {
        this.leadRepository = leadRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.statusPolicy = statusPolicy;
        this.viewMapper = viewMapper;
    }

    @Override
    public LeadView changeStatus(Long id, ChangeLeadStatusCommand command) {
        Lead existing = this.leadRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe el lead " + id));

        // The rule lives in the backend: the frontend may not be smarter
        // (mission 7.8).
        this.statusPolicy.assertAllowed(existing.status(), command.status());

        Lead updated = existing.withStatus(
                command.status(),
                command.status() == LeadStatus.CLOSED ? command.outcome() : existing.outcome(),
                command.status() == LeadStatus.CLOSED && command.saleAmount() != null
                        ? command.saleAmount()
                        : existing.saleAmount());

        Lead saved = this.leadRepository.save(updated);

        this.statusHistoryRepository.save(new LeadStatusHistory(
                null, saved.id(), existing.status(), saved.status(), this.currentUser(), Instant.now()));

        return this.viewMapper.toView(saved);
    }

    private String currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "SISTEMA";
        }
        return authentication.getName();
    }
}
