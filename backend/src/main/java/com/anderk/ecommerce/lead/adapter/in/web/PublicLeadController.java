package com.anderk.ecommerce.lead.adapter.in.web;

import java.util.Map;

import com.anderk.ecommerce.lead.domain.model.enums.LeadSource;
import com.anderk.ecommerce.lead.adapter.in.web.dto.InfoRequestRequest;
import com.anderk.ecommerce.lead.application.usecases.CaptureLeadUseCase;
import com.anderk.ecommerce.lead.application.usecases.CaptureLeadUseCase.CaptureLeadUseCaseCommand;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The forms of the public site that capture a lead (HU-12).
 */
@RestController
@RequestMapping("/api/public")
public class PublicLeadController {

    private final CaptureLeadUseCase leadCaptureService;

    public PublicLeadController(CaptureLeadUseCase leadCaptureService) {
        this.leadCaptureService = leadCaptureService;
    }

    /**
     * The request of more information (HU-12).
     *
     * <p>It creates or updates the lead, with the origin `INFO_REQUEST`, and
     * records the items the visitor was interested in.
     */
    @PostMapping("/info-requests")
    public ResponseEntity<Map<String, Object>> infoRequest(
            @Valid @RequestBody InfoRequestRequest request) {

        var lead = this.leadCaptureService.capture(new CaptureLeadUseCaseCommand(
                request.name(),
                request.email(),
                request.phone(),
                request.province(),
                request.consent(),
                LeadSource.INFO_REQUEST,
                request.anonymousId(),
                request.itemIds(),
                request.message()));

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "leadId", lead.id(),
                "name", lead.name(),
                "email", lead.email(),
                "status", lead.status().name()));
    }
}
