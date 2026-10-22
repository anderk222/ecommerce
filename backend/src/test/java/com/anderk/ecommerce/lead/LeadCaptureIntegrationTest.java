package com.anderk.ecommerce.lead;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.anderk.ecommerce.TestcontainersConfiguration;
import com.anderk.ecommerce.domain.error.ValidationException;
import com.anderk.ecommerce.lead.application.LeadCaptureService;
import com.anderk.ecommerce.lead.domain.model.LeadSource;
import com.anderk.ecommerce.lead.domain.model.Province;
import com.anderk.ecommerce.lead.domain.port.LeadRepository;
import com.anderk.ecommerce.tracking.application.ActivityTracker;
import com.anderk.ecommerce.tracking.domain.model.EventType;
import com.anderk.ecommerce.tracking.domain.port.ActivityEventRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * The capture of the leads (HU-10, HU-11, HU-12).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class LeadCaptureIntegrationTest {

    @Autowired
    private LeadCaptureService leadCaptureService;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private ActivityTracker activityTracker;

    @Autowired
    private ActivityEventRepository eventRepository;

    @Test
    @DisplayName("the same email from two devices yields one lead with both origins")
    void theSameEmailFromTwoDevicesYieldsOneLeadWithBothOrigins() {
        String anonymousId = "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee";

        var first = this.leadCaptureService.capture(new LeadCaptureService.CaptureLeadCommand(
                "Ana Pérez", "Ana@Mail.com", "0999999999", Province.PICHINCHA, true,
                LeadSource.INFO_REQUEST, anonymousId, List.of(12L), "Me interesa el router"));

        // The same person, from another browser and with another origin.
        var second = this.leadCaptureService.capture(new LeadCaptureService.CaptureLeadCommand(
                "Ana Pérez", "ana@mail.com", "0999999999", Province.PICHINCHA, true,
                LeadSource.CHECKOUT, "ffffffff-bbbb-4ccc-8ddd-ffffffffffff", List.of(1L), null));

        assertThat(first.id()).isEqualTo(second.id());
        assertThat(second.sources())
                .containsExactlyInAnyOrder(LeadSource.INFO_REQUEST, LeadSource.CHECKOUT);
        assertThat(this.leadRepository.findByEmail("ana@mail.com")).isPresent();
    }

    @Test
    @DisplayName("the activity before the contact is attached to the lead")
    void theActivityBeforeTheContactIsAttachedToTheLead() {
        String anonymousId = "12345678-bbbb-4ccc-8ddd-eeeeeeeeeeee";
        this.activityTracker.track(anonymousId, EventType.SEARCH, "CATALOG", null,
                java.util.Map.of("query", "internet hogar"));

        var lead = this.leadCaptureService.capture(new LeadCaptureService.CaptureLeadCommand(
                "Ana", "ana2@mail.com", null, Province.PICHINCHA, true,
                LeadSource.INFO_REQUEST, anonymousId, List.of(), null));

        var events = this.eventRepository.findByLead(lead.id(), 10);
        assertThat(events).isNotEmpty();
        assertThat(events.get(0).leadId()).isEqualTo(lead.id());
    }

    @Test
    @DisplayName("a capture that comes from a purchase records the amount of the sale")
    void aCaptureThatComesFromAPurchaseRecordsTheAmountOfTheSale() {
        var lead = this.leadCaptureService.capture(new LeadCaptureService.CaptureLeadCommand(
                "Ana", "ana4@mail.com", null, Province.PICHINCHA, true,
                LeadSource.CHECKOUT, null, List.of(1L), null,
                new java.math.BigDecimal("161.82")));

        assertThat(lead.saleAmount()).isEqualByComparingTo("161.82");
    }

    @Test
    @DisplayName("the consent is mandatory")
    void theConsentIsMandatory() {
        assertThatThrownBy(() -> this.leadCaptureService.capture(
                new LeadCaptureService.CaptureLeadCommand(
                        "Ana", "ana3@mail.com", null, Province.PICHINCHA, false,
                        LeadSource.INFO_REQUEST, null, List.of(), null)))
                .isInstanceOf(ValidationException.class);
    }
}
