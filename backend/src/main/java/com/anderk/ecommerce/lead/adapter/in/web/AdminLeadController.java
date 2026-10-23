package com.anderk.ecommerce.lead.adapter.in.web;

import java.time.Instant;

import com.anderk.ecommerce.domain.pagination.PageResponse;
import com.anderk.ecommerce.lead.adapter.in.web.dto.ChangeLeadStatusRequest;
import com.anderk.ecommerce.lead.adapter.in.web.dto.CreateLeadRequest;
import com.anderk.ecommerce.lead.adapter.in.web.dto.LeadSearchRequest;
import com.anderk.ecommerce.lead.adapter.in.web.dto.NoteRequest;
import com.anderk.ecommerce.lead.adapter.in.web.dto.UpdateLeadRequest;
import com.anderk.ecommerce.lead.application.command.AddLeadNoteCommand;
import com.anderk.ecommerce.lead.application.command.ChangeLeadStatusCommand;
import com.anderk.ecommerce.lead.application.command.CreateLeadCommand;
import com.anderk.ecommerce.lead.application.command.UpdateLeadCommand;
import com.anderk.ecommerce.lead.application.usecases.AddLeadNoteUseCase;
import com.anderk.ecommerce.lead.application.usecases.CalculateLeadKpiUseCase;
import com.anderk.ecommerce.lead.application.usecases.CalculateLeadKpiUseCase.CalculateLeadKpiUseCaseCommand;
import com.anderk.ecommerce.lead.application.usecases.CalculateLeadKpiUseCase.CalculateLeadKpiUseCaseResult;
import com.anderk.ecommerce.lead.application.usecases.ChangeLeadStatusUseCase;
import com.anderk.ecommerce.lead.application.usecases.CreateLeadUseCase;
import com.anderk.ecommerce.lead.application.usecases.DeleteLeadUseCase;
import com.anderk.ecommerce.lead.application.usecases.GetKanbanBoardUseCase;
import com.anderk.ecommerce.lead.application.usecases.GetLeadDetailUseCase;
import com.anderk.ecommerce.lead.application.usecases.SearchLeadsUseCase;
import com.anderk.ecommerce.lead.application.usecases.UpdateLeadUseCase;
import com.anderk.ecommerce.lead.application.view.KanbanBoardView;
import com.anderk.ecommerce.lead.application.view.LeadDetailView;
import com.anderk.ecommerce.lead.application.view.LeadNoteView;
import com.anderk.ecommerce.lead.application.view.LeadView;

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
 * <p>The route is under `/api/admin`, so it needs the role `ADMIN`. The changes
 * of the leads are written by the use cases, not here.
 */
@RestController
@RequestMapping("/api/admin/leads")
public class AdminLeadController {

    private final SearchLeadsUseCase searchLeadsUseCase;

    private final GetLeadDetailUseCase getLeadDetailUseCase;

    private final GetKanbanBoardUseCase getKanbanBoardUseCase;

    private final CreateLeadUseCase createLeadUseCase;

    private final UpdateLeadUseCase updateLeadUseCase;

    private final ChangeLeadStatusUseCase changeLeadStatusUseCase;

    private final DeleteLeadUseCase deleteLeadUseCase;

    private final AddLeadNoteUseCase addLeadNoteUseCase;

    private final CalculateLeadKpiUseCase leadKpiService;

    public AdminLeadController(SearchLeadsUseCase searchLeadsUseCase,
                               GetLeadDetailUseCase getLeadDetailUseCase,
                               GetKanbanBoardUseCase getKanbanBoardUseCase,
                               CreateLeadUseCase createLeadUseCase,
                               UpdateLeadUseCase updateLeadUseCase,
                               ChangeLeadStatusUseCase changeLeadStatusUseCase,
                               DeleteLeadUseCase deleteLeadUseCase,
                               AddLeadNoteUseCase addLeadNoteUseCase,
                               CalculateLeadKpiUseCase leadKpiService) {
        this.searchLeadsUseCase = searchLeadsUseCase;
        this.getLeadDetailUseCase = getLeadDetailUseCase;
        this.getKanbanBoardUseCase = getKanbanBoardUseCase;
        this.createLeadUseCase = createLeadUseCase;
        this.updateLeadUseCase = updateLeadUseCase;
        this.changeLeadStatusUseCase = changeLeadStatusUseCase;
        this.deleteLeadUseCase = deleteLeadUseCase;
        this.addLeadNoteUseCase = addLeadNoteUseCase;
        this.leadKpiService = leadKpiService;
    }

    /**
     * The listing of the leads (HU-21). The parameters are described in
     * {@link LeadSearchRequest}.
     */
    @GetMapping
    public PageResponse<LeadView> list(LeadSearchRequest request) {
        return this.searchLeadsUseCase.search(request.toCriteria(), request.toPageRequest());
    }

    /** The detail of a lead, with its timeline, its orders and its notes (HU-25). */
    @GetMapping("/{id}")
    public LeadDetailView detail(@PathVariable Long id) {
        return this.getLeadDetailUseCase.getDetail(id);
    }

    /** Creates a lead by hand, with the origin `MANUAL` (HU-21). */
    @PostMapping
    public ResponseEntity<LeadView> create(@Valid @RequestBody CreateLeadRequest request) {
        LeadView created = this.createLeadUseCase.create(new CreateLeadCommand(
                request.name(), request.email(), request.phone(), request.province(),
                request.saleAmount(), request.consent()));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** Edits a lead (HU-21). */
    @PutMapping("/{id}")
    public LeadView update(@PathVariable Long id, @Valid @RequestBody UpdateLeadRequest request) {
        return this.updateLeadUseCase.update(id, new UpdateLeadCommand(
                request.name(), request.phone(), request.province(), request.saleAmount(),
                request.consent()));
    }

    /** Deletes a lead (HU-21). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        this.deleteLeadUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Changes the state of a lead (HU-22).
     *
     * <p>The backend validates the transition: {@code Nuevo → Cerrado} is
     * refused with a 409.
     */
    @PatchMapping("/{id}/status")
    public LeadView changeStatus(@PathVariable Long id,
                                 @Valid @RequestBody ChangeLeadStatusRequest request) {
        return this.changeLeadStatusUseCase.changeStatus(id, new ChangeLeadStatusCommand(
                request.status(), request.outcome(), request.saleAmount()));
    }

    /** Writes a note about a lead (HU-25). */
    @PostMapping("/{id}/notes")
    public ResponseEntity<LeadNoteView> addNote(@PathVariable Long id,
                                                @Valid @RequestBody NoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(this.addLeadNoteUseCase.addNote(id, new AddLeadNoteCommand(request.text())));
    }

    /** The board of the pipeline (HU-23). */
    @GetMapping("/kanban")
    public KanbanBoardView kanban(LeadSearchRequest request) {
        return this.getKanbanBoardUseCase.getKanban(request.toCriteria());
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
