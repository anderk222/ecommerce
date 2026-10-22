package com.anderk.ecommerce.lead.application;

import java.math.BigDecimal;
import java.time.Instant;

import com.anderk.ecommerce.lead.domain.model.LeadStatus;

/**
 * The inbound port of the indicators of the sales (HU-24).
 */
public interface LeadKpiService {

    /**
     * The indicators of the sales in a range of dates.
     *
     * @param range the range of dates; null means "everything"
     */
    LeadKpis calculate(DateRange range);

    /**
     * A range of dates.
     *
     * @param from the beginning, null means "from the beginning"
     * @param to the end, null means "until now"
     */
    record DateRange(Instant from, Instant to) {
    }

    /**
     * The indicators of the sales.
     *
     * @param closedSales the sum of the leads closed as won
     * @param wonCount how many leads were closed as won
     * @param lostCount how many leads were closed as lost
     * @param totalCount how many leads there are
     * @param conversionRate the ratio of won over the total
     * @param potentialAmount the sum of the amounts of the leads in management
     * @param byStatus how many leads there are in each state
     */
    record LeadKpis(
            BigDecimal closedSales,
            long wonCount,
            long lostCount,
            long totalCount,
            double conversionRate,
            BigDecimal potentialAmount,
            java.util.Map<LeadStatus, Long> byStatus) {
    }
}
