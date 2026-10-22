package com.anderk.ecommerce.tracking.application;

/**
 * The port that links the activity of a browser with a lead (HU-10).
 */
public interface VisitorLinker {

    /**
     * Links the browser with the lead, and its events that are not linked yet.
     *
     * @param anonymousId the identifier of the browser
     * @param leadId the lead the visitor became
     * @return how many events were linked
     */
    int link(String anonymousId, Long leadId);
}
