package com.anderk.ecommerce.lead.adapter.out.persistence.note;

import java.util.List;

import com.anderk.ecommerce.lead.domain.model.LeadNote;
import com.anderk.ecommerce.lead.domain.port.LeadNoteRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The output adapter of the notes.
 */
@Component
@Transactional
public class JpaLeadNoteRepositoryAdapter implements LeadNoteRepository {

    private final LeadNoteJpaRepository notes;

    public JpaLeadNoteRepositoryAdapter(LeadNoteJpaRepository notes) {
        this.notes = notes;
    }

    @Override
    public LeadNote save(LeadNote note) {
        LeadNoteJpaEntity entity = new LeadNoteJpaEntity();
        entity.setId(note.id());
        entity.setLeadId(note.leadId());
        entity.setBody(note.body());
        entity.setAuthor(note.author());
        entity.setCreatedAt(note.createdAt());
        return toDomain(this.notes.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeadNote> findByLead(Long leadId) {
        return this.notes.findByLeadIdOrderByCreatedAtDesc(leadId).stream()
                .map(JpaLeadNoteRepositoryAdapter::toDomain)
                .toList();
    }

    private static LeadNote toDomain(LeadNoteJpaEntity entity) {
        return new LeadNote(entity.getId(), entity.getLeadId(), entity.getBody(),
                entity.getAuthor(), entity.getCreatedAt());
    }
}
