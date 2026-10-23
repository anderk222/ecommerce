package com.anderk.ecommerce.lead.application.usecases;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.EnumSet;

import com.anderk.ecommerce.lead.domain.model.Lead;
import com.anderk.ecommerce.lead.domain.port.LeadRepository;
import com.anderk.ecommerce.tracking.application.usecase.LinkVisitorToLeadUseCase;
import com.anderk.ecommerce.tracking.application.usecase.LinkVisitorToLeadUseCase.LinkVisitorToLeadUseCaseCommand;
import com.anderk.ecommerce.lead.domain.model.enums.LeadStatus;

import org.springframework.transaction.annotation.Transactional;

/**
 * The use case that captures the contact of a visitor.
 *
 * <p>Every form of the system ends here: the request of information, the
 * subscription of the stock, the purchase and the creation by hand. There is
 * **one** rule — the email is the identity, and the origins are never lost —
 * and it lives in one place (HU-11).
 */
@Transactional
public class CaptureLeadUseCaseImpl implements CaptureLeadUseCase {

    private final LeadRepository leadRepository;

    private final LinkVisitorToLeadUseCase visitorLinker;

    public CaptureLeadUseCaseImpl(LeadRepository leadRepository, LinkVisitorToLeadUseCase visitorLinker) {
        this.leadRepository = leadRepository;
        this.visitorLinker = visitorLinker;
    }

    @Override
    public Lead capture(CaptureLeadUseCaseCommand command) {

        String email = command.email().toString().trim().toLowerCase(Locale.ROOT);

        BigDecimal amount = command.saleAmount() == null
                ? BigDecimal.ZERO
                : command.saleAmount();

        Lead lead = this.leadRepository.findByEmail(email)
                .map(existing -> existing
                        .withContact(command.name(), command.phone(), command.province(), command.consent())
                        .withSource(command.source())
                        .withSaleAmount(amount))
                .orElseGet(() -> new Lead(null,command.name() == null ? "" : command.name().trim(),
                        email, command.phone(), command.province(), LeadStatus.NEW, EnumSet.of(command.source()),
                        amount, null, command.consent(), Instant.now(), Instant.now()));

        Lead saved = this.leadRepository.save(lead);

        // The activity the visitor had before giving the contact is now part
        if (command.anonymousId() != null && !command.anonymousId().isBlank()) {
            this.visitorLinker.link(new LinkVisitorToLeadUseCaseCommand(command.anonymousId(), saved.id()));
        }

        return saved;
    }
}
