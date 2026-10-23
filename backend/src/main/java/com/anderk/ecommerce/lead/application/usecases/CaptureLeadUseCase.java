package com.anderk.ecommerce.lead.application.usecases;

import java.math.BigDecimal;
import java.util.List;

import com.anderk.ecommerce.domain.error.ErrorDetail;
import com.anderk.ecommerce.domain.error.ValidationException;
import com.anderk.ecommerce.domain.model.vo.Email;
import com.anderk.ecommerce.lead.domain.model.Lead;
import com.anderk.ecommerce.lead.domain.model.enums.LeadSource;
import com.anderk.ecommerce.lead.domain.model.Province;

/**
 * The port that captures a lead, wherever the contact comes from (HU-11).
 */
public interface CaptureLeadUseCase {

    /**
     * Captures the contact of a visitor.
     *
     * <p>
     * **Idempotent by email**: the lead is created, or the existing one is
     * updated and keeps **all** its origins. The activity the visitor had
     * before giving the contact is attached to the lead.
     *
     * @param command the contact of the visitor
     * @throws com.anderk.ecommerce.domain.error.ValidationException if the consent
     *                                                               is missing
     */
    Lead capture(CaptureLeadUseCaseCommand command);

    /**
     * The contact of a visitor.
     *
     * @param name        the name
     * @param email       the email, the identity of the lead
     * @param phone       the telephone
     * @param province    the province of Ecuador
     * @param consent     whether the visitor agreed to be contacted
     * @param source      where the contact came from
     * @param anonymousId the browser of the visitor, to attach its activity
     * @param itemIds     the items the visitor was interested in
     * @param message     an optional message from the visitor
     * @param saleAmount  the amount of the sale, when the capture comes from a
     *                    purchase; null when there is no sale yet
     */
    public record CaptureLeadUseCaseCommand(
            String name,
            Email email,
            String phone,
            Province province,
            boolean consent,
            LeadSource source,
            String anonymousId,
            List<Long> itemIds,
            String message,
            BigDecimal saleAmount) {

        public CaptureLeadUseCaseCommand {
            if (!consent) {
                throw new ValidationException("Se necesita el consentimiento para continuar",
                        ErrorDetail.of("consent", "es obligatorio"));
            }
        }

        /** A capture that does not come from a sale. */
        public CaptureLeadUseCaseCommand(String name, String email, String phone, Province province, boolean consent,
                LeadSource source, String anonymousId, List<Long> itemIds, String message) {
            this(name, new Email(email), phone, province, consent, source, anonymousId, itemIds, message, null);
        }
    }
}
