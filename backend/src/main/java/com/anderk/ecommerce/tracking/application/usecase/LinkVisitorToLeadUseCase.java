package com.anderk.ecommerce.tracking.application.usecase;

/**
 * The port that links the activity of a browser with a lead (HU-10).
 */
public interface LinkVisitorToLeadUseCase {

    /**
     * Links the browser with the lead, and its events that are not linked yet.
     *
     * @param anonymousId the identifier of the browser
     * @param leadId the lead the visitor became
     * @return how many events were linked
     */
    LinkResult link(LinkVisitorToLeadUseCaseCommand command);

    public record LinkVisitorToLeadUseCaseCommand (String anonymousId, Long leadId) {
        public LinkVisitorToLeadUseCaseCommand{ }
    }

    public record LinkResult(int eventsLinked) { }
}