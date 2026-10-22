package com.anderk.ecommerce.tracking.domain.port;

import java.util.List;

import com.anderk.ecommerce.tracking.domain.model.ActivityEvent;

/**
 * The outbound port of the activity events.
 */
public interface ActivityEventRepository {

    /** Inserts an event. */
    ActivityEvent save(ActivityEvent event);

    /** The events of a lead, the most recent first (HU-25). */
    List<ActivityEvent> findByLead(Long leadId, int limit);

    /** The events of a browser, the most recent first. */
    List<ActivityEvent> findByAnonymousId(String anonymousId, int limit);

    /**
     * Links the events of a browser that are not linked yet to a lead (HU-10).
     *
     * @return how many events were linked
     */
    int linkToLead(String anonymousId, Long leadId);
}
