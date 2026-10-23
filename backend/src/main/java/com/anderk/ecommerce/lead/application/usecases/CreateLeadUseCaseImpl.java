package com.anderk.ecommerce.lead.application.usecases;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Locale;

import com.anderk.ecommerce.domain.error.ConflictException;
import com.anderk.ecommerce.lead.application.command.CreateLeadCommand;
import com.anderk.ecommerce.lead.application.view.LeadView;
import com.anderk.ecommerce.lead.domain.model.Lead;
import com.anderk.ecommerce.lead.domain.model.enums.LeadSource;
import com.anderk.ecommerce.lead.domain.model.enums.LeadStatus;
import com.anderk.ecommerce.lead.domain.port.LeadRepository;

import org.springframework.transaction.annotation.Transactional;

/**
 * The use case that creates a lead by hand (HU-21).
 */
@Transactional
public class CreateLeadUseCaseImpl implements CreateLeadUseCase {

    private final LeadRepository leadRepository;

    private final LeadViewMapper viewMapper;

    public CreateLeadUseCaseImpl(LeadRepository leadRepository, LeadViewMapper viewMapper) {
        this.leadRepository = leadRepository;
        this.viewMapper = viewMapper;
    }

    @Override
    public LeadView create(CreateLeadCommand command) {
        String email = command.email().trim().toLowerCase(Locale.ROOT);
        if (this.leadRepository.findByEmail(email).isPresent()) {
            throw new ConflictException("Ya existe un lead con el correo " + email);
        }

        Lead lead = new Lead(
                null,
                command.name(),
                email,
                command.phone(),
                command.province(),
                LeadStatus.NEW,
                EnumSet.of(LeadSource.MANUAL),
                command.saleAmount() == null ? BigDecimal.ZERO : command.saleAmount(),
                null,
                command.consent(),
                Instant.now(),
                Instant.now());

        return this.viewMapper.toView(this.leadRepository.save(lead));
    }
}
