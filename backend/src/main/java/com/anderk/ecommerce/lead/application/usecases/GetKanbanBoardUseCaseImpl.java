package com.anderk.ecommerce.lead.application.usecases;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.anderk.ecommerce.lead.application.view.KanbanBoardView;
import com.anderk.ecommerce.lead.application.view.LeadView;
import com.anderk.ecommerce.lead.domain.model.Lead;
import com.anderk.ecommerce.lead.domain.model.enums.LeadStatus;
import com.anderk.ecommerce.lead.domain.port.LeadRepository;
import com.anderk.ecommerce.lead.domain.search.LeadSearchCriteria;

import org.springframework.transaction.annotation.Transactional;

/**
 * The use case that reads the board of the pipeline (HU-23).
 */
@Transactional(readOnly = true)
public class GetKanbanBoardUseCaseImpl implements GetKanbanBoardUseCase {

    private final LeadRepository leadRepository;

    private final LeadViewMapper viewMapper;

    public GetKanbanBoardUseCaseImpl(LeadRepository leadRepository, LeadViewMapper viewMapper) {
        this.leadRepository = leadRepository;
        this.viewMapper = viewMapper;
    }

    @Override
    public KanbanBoardView getKanban(LeadSearchCriteria criteria) {
        List<Lead> leads = this.leadRepository.findAll(criteria);

        Map<LeadStatus, List<LeadView>> byStatus = new EnumMap<>(LeadStatus.class);
        for (LeadStatus status : LeadStatus.values()) {
            byStatus.put(status, new ArrayList<>());
        }
        leads.forEach(lead -> byStatus.get(lead.status()).add(this.viewMapper.toView(lead)));

        List<KanbanBoardView.KanbanColumnView> columns = new ArrayList<>();
        for (LeadStatus status : LeadStatus.values()) {
            List<LeadView> column = byStatus.get(status);
            BigDecimal total = column.stream()
                    .map(LeadView::saleAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            columns.add(new KanbanBoardView.KanbanColumnView(
                    status.name(), label(status), column, column.size(), total));
        }

        return new KanbanBoardView(columns);
    }

    private String label(LeadStatus status) {
        return switch (status) {
            case NEW -> "Nuevo";
            case IN_PROGRESS -> "En gestión";
            case CLOSED -> "Cerrado";
        };
    }
}
