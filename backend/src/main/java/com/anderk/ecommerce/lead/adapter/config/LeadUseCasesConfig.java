package com.anderk.ecommerce.lead.adapter.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.anderk.ecommerce.catalog.domain.port.CatalogRepository;
import com.anderk.ecommerce.lead.application.LeadCommandService;
import com.anderk.ecommerce.lead.application.LeadCommandServiceImpl;
import com.anderk.ecommerce.lead.application.LeadQueryService;
import com.anderk.ecommerce.lead.application.LeadQueryServiceImpl;
import com.anderk.ecommerce.lead.application.usecases.CalculateLeadKpiUseCase;
import com.anderk.ecommerce.lead.application.usecases.CalculateLeadKpiUseCaseImpl;
import com.anderk.ecommerce.lead.application.usecases.CaptureLeadUseCase;
import com.anderk.ecommerce.lead.application.usecases.CaptureLeadUseCaseImpl;
import com.anderk.ecommerce.lead.domain.policy.LeadStatusPolicy;
import com.anderk.ecommerce.lead.domain.port.LeadNoteRepository;
import com.anderk.ecommerce.lead.domain.port.LeadRepository;
import com.anderk.ecommerce.lead.domain.port.LeadSearchRepository;
import com.anderk.ecommerce.lead.domain.port.LeadStatusHistoryRepository;
import com.anderk.ecommerce.order.domain.port.OrderRepository;
import com.anderk.ecommerce.tracking.application.usecase.LinkVisitorToLeadUseCase;
import com.anderk.ecommerce.tracking.domain.port.ActivityEventRepository;

@Configuration
public class LeadUseCasesConfig {

    @Bean
    LeadCommandService leadCommandService(
            @Autowired LeadRepository leadRepository,
            @Autowired LeadStatusHistoryRepository statusHistoryRepository,
            @Autowired LeadNoteRepository noteRepository,
            @Autowired LeadStatusPolicy statusPolicy) {
        return new LeadCommandServiceImpl(leadRepository, statusHistoryRepository, noteRepository, statusPolicy);
    }

    @Bean
    CalculateLeadKpiUseCase calculateLeadKpiUseCase(@Autowired LeadSearchRepository leadSearchRepository) {
        return new CalculateLeadKpiUseCaseImpl(leadSearchRepository);
    }

    @Bean
    CaptureLeadUseCase captureLeadUseCase(
            @Autowired LeadRepository leadRepository, @Autowired LinkVisitorToLeadUseCase visitorLinker) {
        return new CaptureLeadUseCaseImpl(leadRepository, visitorLinker);
    }

    @Bean
    LeadQueryService leadQueryService(
            LeadRepository leadRepository,
            LeadSearchRepository leadSearchRepository,
            LeadStatusHistoryRepository statusHistoryRepository,
            LeadNoteRepository noteRepository,
            ActivityEventRepository eventRepository,
            OrderRepository orderRepository,
            CatalogRepository catalogRepository) {
        return new LeadQueryServiceImpl(leadRepository, leadSearchRepository, statusHistoryRepository,
                noteRepository, eventRepository, orderRepository, catalogRepository);
    }
}