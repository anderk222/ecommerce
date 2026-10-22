package com.anderk.ecommerce.lead.application;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.anderk.ecommerce.lead.domain.model.Lead;
import com.anderk.ecommerce.lead.domain.model.LeadOutcome;
import com.anderk.ecommerce.lead.domain.model.LeadStatus;
import com.anderk.ecommerce.lead.domain.port.LeadSearchCriteria;
import com.anderk.ecommerce.lead.domain.port.LeadSearchRepository;
import com.anderk.ecommerce.shared.annotation.UseCase;

import org.springframework.transaction.annotation.Transactional;

/**
 * The indicators of the sales (HU-24).
 */
@UseCase
@Transactional(readOnly = true)
public class LeadKpiServiceImpl implements LeadKpiService {

    private final LeadSearchRepository leadSearchRepository;

    public LeadKpiServiceImpl(LeadSearchRepository leadSearchRepository) {
        this.leadSearchRepository = leadSearchRepository;
    }

    @Override
    public LeadKpis calculate(DateRange range) {
        List<Lead> leads = this.leadSearchRepository.findAll(new LeadSearchCriteria(
                null, null, null, null,
                range == null ? null : range.from(),
                range == null ? null : range.to()));

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

        return new LeadKpis(closedSales, won, lost, total, conversion, potential, byStatus);
    }
}
