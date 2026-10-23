package com.anderk.ecommerce.lead.application.usecases;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import com.anderk.ecommerce.lead.domain.model.Lead;
import com.anderk.ecommerce.lead.domain.model.enums.LeadOutcome;
import com.anderk.ecommerce.lead.domain.model.enums.LeadStatus;
import com.anderk.ecommerce.lead.domain.port.LeadSearchCriteria;
import com.anderk.ecommerce.lead.domain.port.LeadSearchRepository;

import org.springframework.transaction.annotation.Transactional;

/**
 * The indicators of the sales (HU-24).
 */
@Transactional(readOnly = true)
public class CalculateLeadKpiUseCaseImpl implements CalculateLeadKpiUseCase {

    private final LeadSearchRepository leadSearchRepository;

    public CalculateLeadKpiUseCaseImpl(LeadSearchRepository leadSearchRepository) {
        this.leadSearchRepository = leadSearchRepository;
    }

    @Override
    public CalculateLeadKpiUseCaseResult calculate(CalculateLeadKpiUseCaseCommand command) {
        List<Lead> leads = this.leadSearchRepository.findAll(new LeadSearchCriteria(
                null, null, null, null,
                command == null ? null : command.from(),
                command == null ? null : command.to()));

        BigDecimal closedSales = BigDecimal.ZERO;
        BigDecimal potential = BigDecimal.ZERO;
        long won = 0;
        long lost = 0;
        Map<LeadStatus, Long> byStatus = new EnumMap<>(LeadStatus.class);
        for (LeadStatus status : LeadStatus.values()) {
            byStatus.put(status, 0L);
        }

        for (Lead lead : leads) {
            byStatus.merge(lead.status(), 1L, Long::sum);
            if (lead.status() == LeadStatus.CLOSED) {
                if (lead.outcome() == LeadOutcome.WON) {
                    won++;
                    closedSales = closedSales.add(
                            lead.saleAmount() == null ? BigDecimal.ZERO : lead.saleAmount());
                } else {
                    lost++;
                }
            } else if (lead.status() == LeadStatus.IN_PROGRESS) {
                potential = potential.add(
                        lead.saleAmount() == null ? BigDecimal.ZERO : lead.saleAmount());
            }
        }

        long total = leads.size();
        double conversion = total == 0 ? 0d : (double) won / total;

        return new CalculateLeadKpiUseCaseResult(closedSales, won, lost, total, conversion, potential, byStatus);
    }
}
