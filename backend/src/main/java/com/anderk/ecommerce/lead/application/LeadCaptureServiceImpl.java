package com.anderk.ecommerce.lead.application;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.Locale;

import com.anderk.ecommerce.domain.error.ValidationException;
import com.anderk.ecommerce.lead.domain.model.Lead;
import com.anderk.ecommerce.lead.domain.port.LeadRepository;
import com.anderk.ecommerce.shared.annotation.UseCase;
import com.anderk.ecommerce.tracking.application.VisitorLinker;

import org.springframework.transaction.annotation.Transactional;

/**
 * The use case that captures the contact of a visitor.
 *
 * <p>Every form of the system ends here: the request of information, the
 * subscription of the stock, the purchase and the creation by hand. There is
 * **one** rule — the email is the identity, and the origins are never lost —
 * and it lives in one place (HU-11).
 */
@UseCase
@Transactional
public class LeadCaptureServiceImpl implements LeadCaptureService {

    private final LeadRepository leadRepository;

    private final VisitorLinker visitorLinker;

    public LeadCaptureServiceImpl(LeadRepository leadRepository, VisitorLinker visitorLinker) {
        this.leadRepository = leadRepository;
        this.visitorLinker = visitorLinker;
    }

    @Override
    public Lead capture(CaptureLeadCommand command) {
        if (!command.consent()) {
            throw new ValidationException("Se necesita el consentimiento para continuar",
                    com.anderk.ecommerce.domain.error.ErrorDetail.of("consent", "es obligatorio"));
        }
        if (command.email() == null || command.email().isBlank()) {
            throw new ValidationException("El correo es obligatorio",
                    com.anderk.ecommerce.domain.error.ErrorDetail.of("email", "es obligatorio"));
        }

        String email = command.email().trim().toLowerCase(Locale.ROOT);

        java.math.BigDecimal amount = command.saleAmount() == null
                ? BigDecimal.ZERO
                : command.saleAmount();

        Lead lead = this.leadRepository.findByEmail(email)
                .map(existing -> existing
                        .withContact(command.name(), command.phone(), command.province(), command.consent())
                        .withSource(command.source())
                        .withSaleAmount(amount))
                .orElseGet(() -> new Lead(
                        null,
                        command.name() == null ? "" : command.name().trim(),
                        email,
                        command.phone(),
                        command.province(),
                        com.anderk.ecommerce.lead.domain.model.LeadStatus.NEW,
                        java.util.EnumSet.of(command.source()),
                        amount,
                        null,
                        command.consent(),
                        Instant.now(),
                        Instant.now()));

        Lead saved = this.leadRepository.save(lead);

        // The activity the visitor had before giving the contact is now part
        // of the history of the lead (HU-10).
        if (command.anonymousId() != null && !command.anonymousId().isBlank()) {
            this.visitorLinker.link(command.anonymousId(), saved.id());
        }

        return saved;
    }
}
