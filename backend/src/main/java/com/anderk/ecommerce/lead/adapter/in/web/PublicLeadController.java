package com.anderk.ecommerce.lead.adapter.in.web;

import java.util.List;
import java.util.Map;

import com.anderk.ecommerce.lead.application.LeadCaptureService;
import com.anderk.ecommerce.lead.domain.model.LeadSource;
import com.anderk.ecommerce.lead.domain.model.Province;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

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

    private final LeadCaptureService leadCaptureService;

    public PublicLeadController(LeadCaptureService leadCaptureService) {
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

        var lead = this.leadCaptureService.capture(new LeadCaptureService.CaptureLeadCommand(
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

    /**
     * The body of the request of information.
     *
     * @param name the name of the visitor
     * @param email the email of the visitor
     * @param phone the telephone of the visitor
     * @param province the province of Ecuador
     * @param consent whether the visitor agreed to be contacted
     * @param anonymousId the browser of the visitor
     * @param itemIds the items the visitor was interested in
     * @param message an optional message
     */
    public record InfoRequestRequest(
            @NotBlank String name,
            @NotBlank @Email String email,
            String phone,
            Province province,
            boolean consent,
            String anonymousId,
            List<Long> itemIds,
            String message) {
    }
}
