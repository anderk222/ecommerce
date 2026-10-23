package com.anderk.ecommerce.lead.application.usecases;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import com.anderk.ecommerce.lead.domain.model.enums.LeadStatus;

/**
 * The inbound port of the indicators of the sales
 */
public interface CalculateLeadKpiUseCase {

    /**
     * The indicators of the sales in a range of dates.
     *
     * @param range the range of dates; null means "everything"
     */
    CalculateLeadKpiUseCaseResult calculate(CalculateLeadKpiUseCaseCommand command);

    /**
     * A range of dates.
     *
     * @param from the beginning, null means "from the beginning"
     * @param to   the end, null means "until now"
     */
    public record CalculateLeadKpiUseCaseCommand(Instant from, Instant to) {

        public CalculateLeadKpiUseCaseCommand {
            if (from != null && to != null && from.isAfter(to)) {
                throw new IllegalArgumentException("from must not be after to");
            }
        }

    }

    /**
     * The indicators of the sales.
     *
     * @param closedSales     the sum of the leads closed as won
     * @param wonCount        how many leads were closed as won
     * @param lostCount       how many leads were closed as lost
     * @param totalCount      how many leads there are
     * @param conversionRate  the ratio of won over the total
     * @param potentialAmount the sum of the amounts of the leads in management
     * @param byStatus        how many leads there are in each state
     */
    public record CalculateLeadKpiUseCaseResult(
            BigDecimal closedSales,
            long wonCount,
            long lostCount,
            long totalCount,
            double conversionRate,
            BigDecimal potentialAmount,
            Map<LeadStatus, Long> byStatus) {

    }
}
