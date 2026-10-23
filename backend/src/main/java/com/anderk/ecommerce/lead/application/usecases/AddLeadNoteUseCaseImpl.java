package com.anderk.ecommerce.lead.application.usecases;

import java.time.Instant;

import com.anderk.ecommerce.domain.error.ErrorDetail;
import com.anderk.ecommerce.domain.error.NotFoundException;
import com.anderk.ecommerce.domain.error.ValidationException;
import com.anderk.ecommerce.lead.application.command.AddLeadNoteCommand;
import com.anderk.ecommerce.lead.application.view.LeadNoteView;
import com.anderk.ecommerce.lead.domain.model.LeadNote;
import com.anderk.ecommerce.lead.domain.port.LeadNoteRepository;
import com.anderk.ecommerce.lead.domain.port.LeadRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * The use case that writes a note about a lead (HU-25).
 */
@Transactional
public class AddLeadNoteUseCaseImpl implements AddLeadNoteUseCase {

    private final LeadRepository leadRepository;

    private final LeadNoteRepository noteRepository;

    private final LeadViewMapper viewMapper;

    public AddLeadNoteUseCaseImpl(LeadRepository leadRepository,
                                  LeadNoteRepository noteRepository,
                                  LeadViewMapper viewMapper) {
        this.leadRepository = leadRepository;
        this.noteRepository = noteRepository;
        this.viewMapper = viewMapper;
    }

    @Override
    public LeadNoteView addNote(Long id, AddLeadNoteCommand command) {
        if (this.leadRepository.findById(id).isEmpty()) {
            throw new NotFoundException("No existe el lead " + id);
        }
        String text = command.text();
        if (text == null || text.isBlank()) {
            throw new ValidationException("La nota no puede estar vacía",
                    ErrorDetail.of("text", "es obligatorio"));
        }
        LeadNote note = this.noteRepository.save(new LeadNote(
                null, id, text.trim(), this.currentUser(), Instant.now()));
        return this.viewMapper.toView(note);
    }

    private String currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "SISTEMA";
        }
        return authentication.getName();
    }
}
