package com.anderk.ecommerce.lead.application.view;

import java.math.BigDecimal;
import java.util.List;

/**
 * The board of the pipeline (HU-23).
 *
 * @param columns one column per state, with the leads and the totals
 */
public record KanbanBoardView(List<KanbanColumnView> columns) {

    public KanbanBoardView {
        columns = columns == null ? List.of() : List.copyOf(columns);
    }

    /**
     * One column of the board.
     *
     * @param status the state of the pipeline
     * @param label the name of the state, for the interface
     * @param leads the leads of the column
     * @param count how many leads the column holds
     * @param total the sum of the amounts of the column
     */
    public record KanbanColumnView(
            String status,
            String label,
            List<LeadView> leads,
            long count,
            BigDecimal total) {
    }
}
