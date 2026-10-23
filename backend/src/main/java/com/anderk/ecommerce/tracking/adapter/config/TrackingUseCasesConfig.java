package com.anderk.ecommerce.tracking.adapter.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.anderk.ecommerce.tracking.application.usecase.LinkVisitorToLeadUseCase;
import com.anderk.ecommerce.tracking.application.usecase.LinkVisitorToLeadUseCaseImpl;
import com.anderk.ecommerce.tracking.application.usecase.TrackActivityUseCase;
import com.anderk.ecommerce.tracking.application.usecase.TrackActivityUseCaseImpl;
import com.anderk.ecommerce.tracking.domain.port.ActivityEventRepository;
import com.anderk.ecommerce.tracking.domain.port.VisitorRepository;

@Configuration
public class TrackingUseCasesConfig {

    @Bean 
    TrackActivityUseCase trackActivityUseCase(
        @Autowired  ActivityEventRepository eventRepository,
        @Autowired VisitorRepository visitorRepository
    ){
        return new TrackActivityUseCaseImpl(eventRepository, visitorRepository);
    }

    @Bean 
    LinkVisitorToLeadUseCase linkVisitorToLeadUseCase(
        @Autowired  VisitorRepository visitorRepository,
        @Autowired  ActivityEventRepository eventRepository
    ){
        return new LinkVisitorToLeadUseCaseImpl(visitorRepository, eventRepository);
    }
}