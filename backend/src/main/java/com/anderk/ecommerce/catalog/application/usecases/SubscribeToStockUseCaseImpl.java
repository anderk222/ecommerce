package com.anderk.ecommerce.catalog.application.usecases;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import com.anderk.ecommerce.catalog.application.command.SubscribeToStockCommand;
import com.anderk.ecommerce.catalog.domain.model.StockSubscription;
import com.anderk.ecommerce.catalog.domain.port.StockSubscriptionRepository;
import com.anderk.ecommerce.lead.application.usecases.CaptureLeadUseCase;
import com.anderk.ecommerce.lead.domain.model.Province;
import com.anderk.ecommerce.lead.domain.model.enums.LeadSource;

import org.springframework.transaction.annotation.Transactional;

/**
 * The use case that subscribes a visitor to the notice that an item comes
 * back (HU-08).
 */
@Transactional
public class SubscribeToStockUseCaseImpl implements SubscribeToStockUseCase {

    private final StockSubscriptionRepository subscriptionRepository;

    private final CaptureLeadUseCase leadCaptureService;

    public SubscribeToStockUseCaseImpl(StockSubscriptionRepository subscriptionRepository,
                                       CaptureLeadUseCase leadCaptureService) {
        this.subscriptionRepository = subscriptionRepository;
        this.leadCaptureService = leadCaptureService;
    }

    @Override
    public void subscribe(SubscribeToStockCommand command) {
        var lead = this.leadCaptureService.capture(new CaptureLeadUseCase.CaptureLeadUseCaseCommand(
                command.name(),
                command.email(),
                null,
                parseProvince(command.province()),
                command.consent(),
                LeadSource.STOCK_NOTIFY,
                command.anonymousId(),
                command.itemIds(),
                null));

        for (Long itemId : itemIds(command)) {
            // One subscription per lead and item: the second request updates
            // the first one.
            StockSubscription existing = this.subscriptionRepository.findByLeadAndItem(lead.id(), itemId)
                    .orElse(null);
            this.subscriptionRepository.save(new StockSubscription(
                    existing == null ? null : existing.id(),
                    lead.id(),
                    itemId,
                    existing == null ? null : existing.notifiedAt(),
                    existing == null ? Instant.now() : existing.createdAt()));
        }
    }

    private List<Long> itemIds(SubscribeToStockCommand command) {
        if (command.itemId() != null) {
            return List.of(command.itemId());
        }
        return command.itemIds() == null ? List.of() : command.itemIds();
    }

    private Province parseProvince(String province) {
        if (province == null || province.isBlank()) {
            return null;
        }
        try {
            return Province.valueOf(province.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
