package com.anderk.ecommerce.lead.application.usecases;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.anderk.ecommerce.catalog.domain.port.CatalogRepository;
import com.anderk.ecommerce.domain.error.NotFoundException;
import com.anderk.ecommerce.lead.application.view.LeadDetailView;
import com.anderk.ecommerce.lead.domain.model.Lead;
import com.anderk.ecommerce.lead.domain.port.LeadNoteRepository;
import com.anderk.ecommerce.lead.domain.port.LeadRepository;
import com.anderk.ecommerce.lead.domain.port.LeadStatusHistoryRepository;
import com.anderk.ecommerce.order.domain.port.OrderRepository;
import com.anderk.ecommerce.tracking.domain.port.ActivityEventRepository;

import org.springframework.transaction.annotation.Transactional;

/**
 * The use case that reads the detail of a lead (HU-25).
 */
@Transactional(readOnly = true)
public class GetLeadDetailUseCaseImpl implements GetLeadDetailUseCase {

    private final LeadRepository leadRepository;

    private final LeadStatusHistoryRepository statusHistoryRepository;

    private final LeadNoteRepository noteRepository;

    private final ActivityEventRepository eventRepository;

    private final OrderRepository orderRepository;

    private final CatalogRepository catalogRepository;

    private final LeadViewMapper viewMapper;

    public GetLeadDetailUseCaseImpl(LeadRepository leadRepository,
                                    LeadStatusHistoryRepository statusHistoryRepository,
                                    LeadNoteRepository noteRepository,
                                    ActivityEventRepository eventRepository,
                                    OrderRepository orderRepository,
                                    CatalogRepository catalogRepository,
                                    LeadViewMapper viewMapper) {
        this.leadRepository = leadRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.noteRepository = noteRepository;
        this.eventRepository = eventRepository;
        this.orderRepository = orderRepository;
        this.catalogRepository = catalogRepository;
        this.viewMapper = viewMapper;
    }

    @Override
    public LeadDetailView getDetail(Long id) {
        Lead lead = this.leadRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe el lead " + id));

        // The read of the timeline names the products (P35): one lookup per
        // distinct catalog item of the timeline, never two.
        Map<Long, String> itemNames = new HashMap<>();
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
                this.viewMapper.toView(lead),
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
}
