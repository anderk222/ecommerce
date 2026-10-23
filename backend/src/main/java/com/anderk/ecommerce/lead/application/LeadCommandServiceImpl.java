package com.anderk.ecommerce.lead.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

import com.anderk.ecommerce.domain.error.ConflictException;
import com.anderk.ecommerce.domain.error.NotFoundException;
import com.anderk.ecommerce.domain.error.ValidationException;
import com.anderk.ecommerce.lead.application.usecases.result.LeadNoteResult;
import com.anderk.ecommerce.lead.application.usecases.result.LeadResult;
import com.anderk.ecommerce.lead.domain.model.Lead;
import com.anderk.ecommerce.lead.domain.model.LeadNote;
import com.anderk.ecommerce.lead.domain.model.enums.LeadSource;
import com.anderk.ecommerce.lead.domain.model.enums.LeadStatus;
import com.anderk.ecommerce.lead.domain.model.LeadStatusHistory;
import com.anderk.ecommerce.lead.domain.policy.LeadStatusPolicy;
import com.anderk.ecommerce.lead.domain.port.LeadNoteRepository;
import com.anderk.ecommerce.lead.domain.port.LeadRepository;
import com.anderk.ecommerce.lead.domain.port.LeadStatusHistoryRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * The use cases that write the leads.
 */
@Transactional
public class LeadCommandServiceImpl implements LeadCommandService {

    private final LeadRepository leadRepository;

    private final LeadStatusHistoryRepository statusHistoryRepository;

    private final LeadNoteRepository noteRepository;

    private final LeadStatusPolicy statusPolicy;

    public LeadCommandServiceImpl(LeadRepository leadRepository,
                                  LeadStatusHistoryRepository statusHistoryRepository,
                                  LeadNoteRepository noteRepository,
                                  LeadStatusPolicy statusPolicy) {
        this.leadRepository = leadRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.noteRepository = noteRepository;
        this.statusPolicy = statusPolicy;
    }

    @Override
    public LeadResult create(CreateLeadCommand command) {

        String email = command.email().trim().toLowerCase(java.util.Locale.ROOT);
        if (this.leadRepository.findByEmail(email).isPresent()) {
            throw new ConflictException(
                    "Ya existe un lead con el correo " + email);
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

        return toView(this.leadRepository.save(lead));
    }

    @Override
    public LeadResult update(Long id, UpdateLeadCommand command) {
        Lead existing = this.require(id);

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

        return toView(this.leadRepository.save(updated));
    }

    @Override
    public LeadResult changeStatus(Long id, ChangeStatusCommand command) {
        Lead existing = this.require(id);

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

        return toView(saved);
    }

    @Override
    public void delete(Long id) {
        this.require(id);
        this.leadRepository.deleteById(id);
    }

    @Override
    public LeadNoteResult addNote(Long id, String text) {
        this.require(id);
        if (text == null || text.isBlank()) {
            throw new ValidationException("La nota no puede estar vacía",
                    com.anderk.ecommerce.domain.error.ErrorDetail.of("text", "es obligatorio"));
        }
        LeadNote note = this.noteRepository.save(new LeadNote(
                null, id, text.trim(), this.currentUser(), Instant.now()));
        return new LeadNoteResult(note.id(), note.leadId(), note.body(), note.author(), note.createdAt());
    }

    private Lead require(Long id) {
        return this.leadRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe el lead " + id));
    }

    private String currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "SISTEMA";
        }
        return authentication.getName();
    }

    private LeadResult toView(Lead lead) {
        return new LeadResult(
                lead.id(),
                lead.name(),
                lead.email(),
                lead.phone(),
                lead.province(),
                lead.status(),
                List.copyOf(lead.sources()),
                lead.saleAmount(),
                lead.outcome(),
                lead.consent(),
                lead.createdAt(),
                lead.updatedAt());
    }
}
