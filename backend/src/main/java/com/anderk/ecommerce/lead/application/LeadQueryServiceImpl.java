package com.anderk.ecommerce.lead.application;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.anderk.ecommerce.catalog.domain.port.CatalogRepository;
import com.anderk.ecommerce.domain.error.NotFoundException;
import com.anderk.ecommerce.lead.application.view.KanbanBoard;
import com.anderk.ecommerce.lead.application.view.LeadDetailView;
import com.anderk.ecommerce.lead.application.view.LeadView;
import com.anderk.ecommerce.lead.domain.model.Lead;
import com.anderk.ecommerce.lead.domain.model.LeadStatus;
import com.anderk.ecommerce.lead.domain.port.LeadNoteRepository;
import com.anderk.ecommerce.shared.annotation.UseCase;
import com.anderk.ecommerce.lead.domain.port.LeadRepository;
import com.anderk.ecommerce.lead.domain.port.LeadSearchCriteria;
import com.anderk.ecommerce.lead.domain.port.LeadSearchRepository;
import com.anderk.ecommerce.lead.domain.port.LeadStatusHistoryRepository;
import com.anderk.ecommerce.order.domain.port.OrderRepository;
import com.anderk.ecommerce.shared.pagination.DomainPage;
import com.anderk.ecommerce.shared.pagination.PageRequestDto;
import com.anderk.ecommerce.shared.pagination.PageResponse;
import com.anderk.ecommerce.tracking.domain.port.ActivityEventRepository;

import org.springframework.transaction.annotation.Transactional;

/**
 * The use cases that read the leads.
 */
@UseCase
@Transactional(readOnly = true)
public class LeadQueryServiceImpl implements LeadQueryService {

    private final LeadRepository leadRepository;

    private final LeadSearchRepository leadSearchRepository;

    private final LeadStatusHistoryRepository statusHistoryRepository;

    private final LeadNoteRepository noteRepository;

    private final ActivityEventRepository eventRepository;

    private final OrderRepository orderRepository;

    private final CatalogRepository catalogRepository;

    public LeadQueryServiceImpl(LeadRepository leadRepository,
                                LeadSearchRepository leadSearchRepository,
                                LeadStatusHistoryRepository statusHistoryRepository,
                                LeadNoteRepository noteRepository,
                                ActivityEventRepository eventRepository,
                                OrderRepository orderRepository,
                                CatalogRepository catalogRepository) {
        this.leadRepository = leadRepository;
        this.leadSearchRepository = leadSearchRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.noteRepository = noteRepository;
        this.eventRepository = eventRepository;
        this.orderRepository = orderRepository;
        this.catalogRepository = catalogRepository;
    }

    @Override
    public PageResponse<LeadView> search(LeadSearchCriteria criteria, PageRequestDto page) {
        DomainPage<Lead> found = this.leadSearchRepository.search(criteria, page);
        List<LeadView> content = found.content().stream().map(this::toView).toList();
        return PageResponse.of(content, found.page(), found.size(), found.totalElements());
    }

    @Override
    public LeadDetailView getDetail(Long id) {
        Lead lead = this.leadRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe el lead " + id));

        // The read of the timeline names the products (P35): one lookup per
        // distinct catalog item of the timeline, never two.
        Map<Long, String> itemNames = new java.util.HashMap<>();
        List<LeadDetailView.TimelineEntry> timeline = this.eventRepository.findByLead(id, 200).stream()
                .map(event -> new LeadDetailView.TimelineEntry(
                        event.eventType().name(),
                        event.entityType(),
                        event.entityId(),
                        this.productNameOf(event.entityType(), event.entityId(), event.metadata(), itemNames),
                        event.metadata(),
                        event.createdAt()))
                .toList();

        return new LeadDetailView(
                toView(lead),
                timeline,
                this.orderRepository.findByLead(id, 50),
                this.noteRepository.findByLead(id),
                this.statusHistoryRepository.findByLead(id));
    }

    /**
     * The name of the product an event of the timeline is about (P35).
     *
     * <p>The metadata the tracker recorded wins; the catalog is the source of
     * truth for every {@code CATALOG_ITEM} event that it did not name — the
     * house deletes logically, so even a deleted item is still readable. A
     * miss yields {@code null} and the panel falls back to the generic words;
     * the timeline never throws because an item disappeared.
     *
     * @param itemNames the names already resolved in this read, shared between
     *                  the events of the same item
     */
    private String productNameOf(String entityType, Long entityId, Map<String, Object> metadata,
                                 Map<Long, String> itemNames) {
        if (!"CATALOG_ITEM".equals(entityType) || entityId == null) {
            return null;
        }
        if (metadata != null && metadata.get("name") instanceof String name && !name.isBlank()) {
            return name;
        }
        return itemNames.computeIfAbsent(entityId, itemId -> this.catalogRepository
                .findById(itemId)
                .map(item -> item.name())
                .orElse(null));
    }

    @Override
    public KanbanBoard getKanban(LeadSearchCriteria criteria) {
        List<Lead> leads = this.leadSearchRepository.findAll(criteria);

        Map<LeadStatus, List<LeadView>> byStatus = new EnumMap<>(LeadStatus.class);
        for (LeadStatus status : LeadStatus.values()) {
            byStatus.put(status, new ArrayList<>());
        }
        leads.forEach(lead -> byStatus.get(lead.status()).add(toView(lead)));

        List<KanbanBoard.KanbanColumn> columns = new ArrayList<>();
        for (LeadStatus status : LeadStatus.values()) {
            List<LeadView> column = byStatus.get(status);
            java.math.BigDecimal total = column.stream()
                    .map(LeadView::saleAmount)
                    .filter(java.util.Objects::nonNull)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            columns.add(new KanbanBoard.KanbanColumn(status.name(), label(status), column, column.size(), total));
        }

        return new KanbanBoard(columns);
    }

    private String label(LeadStatus status) {
        return switch (status) {
            case NEW -> "Nuevo";
            case IN_PROGRESS -> "En gestión";
            case CLOSED -> "Cerrado";
        };
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
