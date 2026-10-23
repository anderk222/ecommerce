package com.anderk.ecommerce.lead.adapter.in.web;

import java.time.Instant;

import com.anderk.ecommerce.domain.pagination.PageRequestDto;
import com.anderk.ecommerce.domain.pagination.PageResponse;
import com.anderk.ecommerce.lead.adapter.in.web.dto.ChangeLeadStatusRequest;
import com.anderk.ecommerce.lead.adapter.in.web.dto.CreateLeadRequest;
import com.anderk.ecommerce.lead.adapter.in.web.dto.NoteRequest;
import com.anderk.ecommerce.lead.adapter.in.web.dto.UpdateLeadRequest;
import com.anderk.ecommerce.lead.application.LeadCommandService;
import com.anderk.ecommerce.lead.application.LeadQueryService;
import com.anderk.ecommerce.lead.application.usecases.CalculateLeadKpiUseCase;
import com.anderk.ecommerce.lead.application.usecases.CalculateLeadKpiUseCase.CalculateLeadKpiUseCaseCommand;
import com.anderk.ecommerce.lead.application.usecases.CalculateLeadKpiUseCase.CalculateLeadKpiUseCaseResult;
import com.anderk.ecommerce.lead.application.usecases.result.KanbanBoardResult;
import com.anderk.ecommerce.lead.application.usecases.result.LeadDetailResult;
import com.anderk.ecommerce.lead.application.usecases.result.LeadNoteResult;
import com.anderk.ecommerce.lead.application.usecases.result.LeadResult;
import com.anderk.ecommerce.lead.domain.model.Province;
import com.anderk.ecommerce.lead.domain.model.enums.LeadSource;
import com.anderk.ecommerce.lead.domain.model.enums.LeadStatus;
import com.anderk.ecommerce.lead.domain.port.LeadSearchCriteria;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The CRM of the leads (HU-21 to HU-25).
 *
 * <p>The route is under `/api/admin`, so it needs the role `ADMIN`.
 */
@RestController
@RequestMapping("/api/admin/leads")
public class AdminLeadController {

    private final LeadQueryService leadQueryService;

    private final LeadCommandService leadCommandService;

    private final CalculateLeadKpiUseCase leadKpiService;

    public AdminLeadController(LeadQueryService leadQueryService,
                               LeadCommandService leadCommandService,
                               CalculateLeadKpiUseCase leadKpiService) {
        this.leadQueryService = leadQueryService;
        this.leadCommandService = leadCommandService;
        this.leadKpiService = leadKpiService;
    }

    /**
     * The listing of the leads (HU-21).
     *
     * @param q the free text: the name, the email or the telephone
     * @param status the state of the pipeline
     * @param province the province of Ecuador
     * @param source the origin
     * @param page the 0-based page number
     * @param size the page size
     */
    @GetMapping
    public PageResponse<LeadResult> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String source,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size) {

        return this.leadQueryService.search(criteriaOf(q, status, province, source, null, null),
                PageRequestDto.of(page, size));
    }

    /** The detail of a lead, with its timeline, its orders and its notes (HU-25). */
    @GetMapping("/{id}")
    public LeadDetailResult detail(@PathVariable Long id) {
        return this.leadQueryService.getDetail(id);
    }

    /** Creates a lead by hand, with the origin `MANUAL` (HU-21). */
    @PostMapping
    public ResponseEntity<LeadResult> create(@Valid @RequestBody CreateLeadRequest request) {
        LeadResult created = this.leadCommandService.create(new LeadCommandService.CreateLeadCommand(
                request.name(), request.email(), request.phone(), request.province(),
                request.saleAmount(), request.consent()));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** Edits a lead (HU-21). */
    @PutMapping("/{id}")
    public LeadResult update(@PathVariable Long id, @Valid @RequestBody UpdateLeadRequest request) {
        return this.leadCommandService.update(id, new LeadCommandService.UpdateLeadCommand(
                request.name(), request.phone(), request.province(), request.saleAmount(),
                request.consent()));
    }

    /** Deletes a lead (HU-21). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        this.leadCommandService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Changes the state of a lead (HU-22).
     *
     * <p>The backend validates the transition: {@code Nuevo → Cerrado} is
     * refused with a 409.
     */
    @PatchMapping("/{id}/status")
    public LeadResult changeStatus(@PathVariable Long id, @Valid @RequestBody ChangeLeadStatusRequest request) {
        return this.leadCommandService.changeStatus(id, new LeadCommandService.ChangeStatusCommand(
                request.status(), request.outcome(), request.saleAmount()));
    }

    /** Writes a note about a lead (HU-25). */
    @PostMapping("/{id}/notes")
    public ResponseEntity<LeadNoteResult> addNote(@PathVariable Long id,
                                                 @Valid @RequestBody NoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(this.leadCommandService.addNote(id, request.text()));
    }

    /** The board of the pipeline (HU-23). */
    @GetMapping("/kanban")
    public KanbanBoardResult kanban(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String source) {
        return this.leadQueryService.getKanban(criteriaOf(q, status, province, source, null, null));
    }

    /** The indicators of the sales (HU-24). */
    @GetMapping("/kpis")
    public CalculateLeadKpiUseCaseResult kpis(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        Instant fromInstant = parseInstant(from);
        Instant toInstant = parseInstant(to);
        return this.leadKpiService.calculate(new CalculateLeadKpiUseCaseCommand(fromInstant, toInstant));
    }


    // --- the helpers --------------------------------------------------------

    private LeadSearchCriteria criteriaOf(String q, String status, String province, String source,
                                          Instant from, Instant to) {
        return new LeadSearchCriteria(
                q,
                parseEnum(LeadStatus.class, status),
                parseEnum(Province.class, province),
                parseEnum(LeadSource.class, source),
                from,
                to);
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (java.time.format.DateTimeParseException exception) {
            return null;
        }
    }
}
