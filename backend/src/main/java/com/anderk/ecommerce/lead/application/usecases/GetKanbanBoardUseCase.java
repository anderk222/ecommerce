package com.anderk.ecommerce.lead.application.usecases;

import com.anderk.ecommerce.lead.application.view.KanbanBoardView;
import com.anderk.ecommerce.lead.domain.search.LeadSearchCriteria;

/**
 * The use case that reads the board of the pipeline (HU-23).
 */
public interface GetKanbanBoardUseCase {

    /** One column per state, with the leads and the totals. */
    KanbanBoardView getKanban(LeadSearchCriteria criteria);
}
