package com.anderk.ecommerce.lead.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;

import com.anderk.ecommerce.domain.error.NotFoundException;
import com.anderk.ecommerce.domain.error.ValidationException;
import com.anderk.ecommerce.lead.application.view.LeadView;
import com.anderk.ecommerce.lead.domain.model.Lead;
import com.anderk.ecommerce.lead.domain.model.LeadNote;
import com.anderk.ecommerce.lead.domain.model.LeadSource;
import com.anderk.ecommerce.lead.domain.model.LeadStatus;
import com.anderk.ecommerce.lead.domain.model.LeadStatusHistory;
import com.anderk.ecommerce.lead.domain.policy.LeadStatusPolicy;
import com.anderk.ecommerce.lead.domain.port.LeadNoteRepository;
import com.anderk.ecommerce.lead.domain.port.LeadRepository;
import com.anderk.ecommerce.lead.domain.port.LeadStatusHistoryRepository;
import com.anderk.ecommerce.shared.annotation.UseCase;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * The use cases that write the leads.
 */
@UseCase
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
    public LeadView create(CreateLeadCommand command) {
        if (command.email() == null || command.email().isBlank()) {
            throw new ValidationException("El correo del lead es obligatorio",
                    com.anderk.ecommerce.domain.error.ErrorDetail.of("email", "es obligatorio"));
        }
        String email = command.email().trim().toLowerCase(java.util.Locale.ROOT);
        if (this.leadRepository.findByEmail(email).isPresent()) {
            throw new com.anderk.ecommerce.domain.error.ConflictException(
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
    public LeadView update(Long id, UpdateLeadCommand command) {
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
    public LeadView changeStatus(Long id, ChangeStatusCommand command) {
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
    public LeadNoteView addNote(Long id, String text) {
        this.require(id);
        if (text == null || text.isBlank()) {
            throw new ValidationException("La nota no puede estar vacía",
                    com.anderk.ecommerce.domain.error.ErrorDetail.of("text", "es obligatorio"));
        }
        LeadNote note = this.noteRepository.save(new LeadNote(
                null, id, text.trim(), this.currentUser(), Instant.now()));
        return new LeadNoteView(note.id(), note.leadId(), note.body(), note.author(), note.createdAt());
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

    private LeadView toView(Lead lead) {
        return new LeadView(
                lead.id(),
                lead.name(),
                lead.email(),
                lead.phone(),
                lead.province(),
                lead.status(),
                java.util.List.copyOf(lead.sources()),
                lead.saleAmount(),
                lead.outcome(),
                lead.consent(),
                lead.createdAt(),
                lead.updatedAt());
    }
}
