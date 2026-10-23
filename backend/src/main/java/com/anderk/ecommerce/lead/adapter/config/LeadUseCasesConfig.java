package com.anderk.ecommerce.lead.adapter.config;

import com.anderk.ecommerce.catalog.domain.port.CatalogRepository;
import com.anderk.ecommerce.lead.adapter.out.persistence.lead.JpaLeadRepositoryAdapter;
import com.anderk.ecommerce.lead.adapter.out.persistence.lead.LeadJpaRepository;
import com.anderk.ecommerce.lead.adapter.out.persistence.note.JpaLeadNoteRepositoryAdapter;
import com.anderk.ecommerce.lead.adapter.out.persistence.note.LeadNoteJpaRepository;
import com.anderk.ecommerce.lead.adapter.out.persistence.status_history.JpaLeadStatusHistoryRepositoryAdapter;
import com.anderk.ecommerce.lead.adapter.out.persistence.status_history.LeadStatusHistoryJpaRepository;
import com.anderk.ecommerce.lead.application.usecases.AddLeadNoteUseCase;
import com.anderk.ecommerce.lead.application.usecases.AddLeadNoteUseCaseImpl;
import com.anderk.ecommerce.lead.application.usecases.CalculateLeadKpiUseCase;
import com.anderk.ecommerce.lead.application.usecases.CalculateLeadKpiUseCaseImpl;
import com.anderk.ecommerce.lead.application.usecases.CaptureLeadUseCase;
import com.anderk.ecommerce.lead.application.usecases.CaptureLeadUseCaseImpl;
import com.anderk.ecommerce.lead.application.usecases.ChangeLeadStatusUseCase;
import com.anderk.ecommerce.lead.application.usecases.ChangeLeadStatusUseCaseImpl;
import com.anderk.ecommerce.lead.application.usecases.CreateLeadUseCase;
import com.anderk.ecommerce.lead.application.usecases.CreateLeadUseCaseImpl;
import com.anderk.ecommerce.lead.application.usecases.DeleteLeadUseCase;
import com.anderk.ecommerce.lead.application.usecases.DeleteLeadUseCaseImpl;
import com.anderk.ecommerce.lead.application.usecases.GetKanbanBoardUseCase;
import com.anderk.ecommerce.lead.application.usecases.GetKanbanBoardUseCaseImpl;
import com.anderk.ecommerce.lead.application.usecases.GetLeadDetailUseCase;
import com.anderk.ecommerce.lead.application.usecases.GetLeadDetailUseCaseImpl;
import com.anderk.ecommerce.lead.application.usecases.LeadViewMapper;
import com.anderk.ecommerce.lead.application.usecases.SearchLeadsUseCase;
import com.anderk.ecommerce.lead.application.usecases.SearchLeadsUseCaseImpl;
import com.anderk.ecommerce.lead.application.usecases.UpdateLeadUseCase;
import com.anderk.ecommerce.lead.application.usecases.UpdateLeadUseCaseImpl;
import com.anderk.ecommerce.lead.domain.policy.LeadStatusPolicy;
import com.anderk.ecommerce.lead.domain.port.LeadNoteRepository;
import com.anderk.ecommerce.lead.domain.port.LeadRepository;
import com.anderk.ecommerce.lead.domain.port.LeadStatusHistoryRepository;
import com.anderk.ecommerce.order.domain.port.OrderRepository;
import com.anderk.ecommerce.tracking.application.usecase.LinkVisitorToLeadUseCase;
import com.anderk.ecommerce.tracking.domain.port.ActivityEventRepository;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The explicit wiring of the {@code lead} hexagon.
 *
 * <p>Tech-stack §2.2.6 asks for one {@code @Configuration} per feature, so that
 * the graph is readable: the adapters are beans, and the use cases depend on
 * the ports, not on the adapters.
 */
@Configuration
public class LeadUseCasesConfig {

    // --- the adapters -------------------------------------------------------

    @Bean
    LeadRepository leadRepository(LeadJpaRepository leads) {
        return new JpaLeadRepositoryAdapter(leads);
    }

    @Bean
    LeadNoteRepository leadNoteRepository(LeadNoteJpaRepository notes) {
        return new JpaLeadNoteRepositoryAdapter(notes);
    }

    @Bean
    LeadStatusHistoryRepository leadStatusHistoryRepository(LeadStatusHistoryJpaRepository history) {
        return new JpaLeadStatusHistoryRepositoryAdapter(history);
    }

    @Bean
    LeadViewMapper leadViewMapper() {
        return new LeadViewMapper();
    }

    // --- the writes of the leads --------------------------------------------

    @Bean
    CreateLeadUseCase createLeadUseCase(LeadRepository leadRepository, LeadViewMapper viewMapper) {
        return new CreateLeadUseCaseImpl(leadRepository, viewMapper);
    }

    @Bean
    UpdateLeadUseCase updateLeadUseCase(LeadRepository leadRepository, LeadViewMapper viewMapper) {
        return new UpdateLeadUseCaseImpl(leadRepository, viewMapper);
    }

    @Bean
    ChangeLeadStatusUseCase changeLeadStatusUseCase(
            LeadRepository leadRepository,
            LeadStatusHistoryRepository statusHistoryRepository,
            LeadStatusPolicy statusPolicy,
            LeadViewMapper viewMapper) {
        return new ChangeLeadStatusUseCaseImpl(
                leadRepository, statusHistoryRepository, statusPolicy, viewMapper);
    }

    @Bean
    DeleteLeadUseCase deleteLeadUseCase(LeadRepository leadRepository) {
        return new DeleteLeadUseCaseImpl(leadRepository);
    }

    @Bean
    AddLeadNoteUseCase addLeadNoteUseCase(
            LeadRepository leadRepository,
            LeadNoteRepository noteRepository,
            LeadViewMapper viewMapper) {
        return new AddLeadNoteUseCaseImpl(leadRepository, noteRepository, viewMapper);
    }

    @Bean
    CaptureLeadUseCase captureLeadUseCase(
            LeadRepository leadRepository,
            LinkVisitorToLeadUseCase visitorLinker) {
        return new CaptureLeadUseCaseImpl(leadRepository, visitorLinker);
    }

    // --- the reads of the leads ---------------------------------------------

    @Bean
    SearchLeadsUseCase searchLeadsUseCase(LeadRepository leadRepository, LeadViewMapper viewMapper) {
        return new SearchLeadsUseCaseImpl(leadRepository, viewMapper);
    }

    @Bean
    GetLeadDetailUseCase getLeadDetailUseCase(
            LeadRepository leadRepository,
            LeadStatusHistoryRepository statusHistoryRepository,
            LeadNoteRepository noteRepository,
            ActivityEventRepository eventRepository,
            OrderRepository orderRepository,
            CatalogRepository catalogRepository,
            LeadViewMapper viewMapper) {
        return new GetLeadDetailUseCaseImpl(leadRepository, statusHistoryRepository, noteRepository,
                eventRepository, orderRepository, catalogRepository, viewMapper);
    }

    @Bean
    GetKanbanBoardUseCase getKanbanBoardUseCase(LeadRepository leadRepository,
                                                LeadViewMapper viewMapper) {
        return new GetKanbanBoardUseCaseImpl(leadRepository, viewMapper);
    }

    @Bean
    CalculateLeadKpiUseCase calculateLeadKpiUseCase(LeadRepository leadRepository) {
        return new CalculateLeadKpiUseCaseImpl(leadRepository);
    }
}
