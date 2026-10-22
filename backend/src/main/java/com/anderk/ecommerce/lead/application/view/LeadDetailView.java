package com.anderk.ecommerce.lead.application.view;

import java.util.List;

import com.anderk.ecommerce.lead.domain.model.LeadNote;
import com.anderk.ecommerce.lead.domain.model.LeadStatusHistory;
import com.anderk.ecommerce.order.domain.model.Order;

/**
 * The detail of a lead: its data, its timeline, its orders and its notes
 * (HU-25).
 *
 * @param lead the lead
 * @param timeline the activity of the visitor, the most recent first
 * @param orders the purchases of the lead, the most recent first
 * @param notes the notes of the team, the most recent first
 * @param statusHistory the changes of state, the most recent first
 */
public record LeadDetailView(
        LeadView lead,
        List<TimelineEntry> timeline,
        List<Order> orders,
        List<LeadNote> notes,
        List<LeadStatusHistory> statusHistory) {

    public LeadDetailView {
        timeline = timeline == null ? List.of() : List.copyOf(timeline);
        orders = orders == null ? List.of() : List.copyOf(orders);
        notes = notes == null ? List.of() : List.copyOf(notes);
        statusHistory = statusHistory == null ? List.of() : List.copyOf(statusHistory);
    }

    /**
     * One entry of the timeline of the lead.
     *
     * @param type what the visitor did
     * @param entityType the kind of entity the action is about
     * @param entityId the identifier of that entity
     * @param productName the product the action is about, null when it is
     *        not about a product or the item cannot be named (P35)
     * @param metadata the details of the action
     * @param createdAt when it happened
     */
    public record TimelineEntry(
            String type,
            String entityType,
            Long entityId,
            String productName,
            java.util.Map<String, Object> metadata,
            java.time.Instant createdAt) {
    }
}
