package com.github.javydreamercsw.management.service.drama;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.drama.DramaEvent;
import com.github.javydreamercsw.management.domain.drama.DramaEventSeverity;
import com.github.javydreamercsw.management.domain.drama.DramaEventType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.transaction.annotation.Transactional;

@DisplayName("DramaEventService Integration Tests")
@Transactional
@EnabledIf("isNotionTokenAvailable")
class DramaEventServiceIT extends ManagementIntegrationTest {

  private Wrestler testWrestler1;
  private Wrestler testWrestler2;

  @BeforeEach
  void setUp() {
    // Create test wrestlers
    testWrestler1 = wrestlerRepository.save(createTestWrestler("Drama Test Wrestler 1"));
    testWrestler2 = wrestlerRepository.save(createTestWrestler("Drama Test Wrestler 2"));
  }

  @Test
  @DisplayName("Should create single-wrestler drama event successfully")
  void shouldCreateSingleWrestlerDramaEvent() {
    // Given
    String title = "Test Drama Event";
    String description = "This is a test drama event";
    DramaEventType eventType = DramaEventType.PERSONAL_ISSUE;
    DramaEventSeverity severity = DramaEventSeverity.NEUTRAL;

    // When
    Optional<DramaEvent> result =
        dramaEventService.createDramaEvent(
            testWrestler1.getId(), null, eventType, severity, title, description);

    // Then
    assertThat(result).isPresent();
    DramaEvent event = result.get();
    assertThat(event.getTitle()).isEqualTo(title);
    assertThat(event.getDescription()).isEqualTo(description);
    assertThat(event.getEventType()).isEqualTo(eventType);
    assertThat(event.getSeverity()).isEqualTo(severity);
    assertThat(event.getPrimaryWrestler()).isEqualTo(testWrestler1);
    assertThat(event.getSecondaryWrestler()).isNull();
    assertThat(event.getIsProcessed()).isFalse();
  }

  @Test
  @DisplayName("Should create multi-wrestler drama event successfully")
  void shouldCreateMultiWrestlerDramaEvent() {
    // Given
    String title = "Backstage Altercation";
    String description = "Two wrestlers got into a heated argument";
    DramaEventType eventType = DramaEventType.BACKSTAGE_INCIDENT;
    DramaEventSeverity severity = DramaEventSeverity.NEGATIVE;

    // When
    Optional<DramaEvent> result =
        dramaEventService.createDramaEvent(
            testWrestler1.getId(), testWrestler2.getId(), eventType, severity, title, description);

    // Then
    assertThat(result).isPresent();
    DramaEvent event = result.get();
    assertThat(event.getPrimaryWrestler()).isEqualTo(testWrestler1);
    assertThat(event.getSecondaryWrestler()).isEqualTo(testWrestler2);
    assertThat(event.isMultiWrestlerEvent()).isTrue();
    assertThat(event.getHeatImpact())
        .isNotNull(); // Should have heat impact for multi-wrestler negative event
  }

  @Test
  @DisplayName("Should generate random drama event successfully")
  void shouldGenerateRandomDramaEvent() {
    // When
    Optional<DramaEvent> result = dramaEventService.generateRandomDramaEvent(testWrestler1.getId());

    // Then
    assertThat(result).isPresent();
    DramaEvent event = result.get();
    assertThat(event.getPrimaryWrestler()).isEqualTo(testWrestler1);
    assertThat(event.getTitle()).isNotBlank();
    assertThat(event.getDescription()).isNotBlank();
    assertThat(event.getEventType()).isNotNull();
    assertThat(event.getSeverity()).isNotNull();
    assertThat(event.getIsProcessed()).isFalse();
  }

  @Test
  @DisplayName("Should process drama event and apply impacts")
  void shouldProcessDramaEventAndApplyImpacts() {
    // Given - Create a drama event with fan impact
    DramaEvent event = new DramaEvent();
    event.setPrimaryWrestler(testWrestler1);
    event.setEventType(DramaEventType.FAN_INTERACTION);
    event.setSeverity(DramaEventSeverity.POSITIVE);
    event.setTitle("Fan Meet Success");
    event.setDescription("Great fan interaction");
    event.setFanImpact(1000L); // Positive fan impact
    event.setIsProcessed(false);

    DramaEvent savedEvent = dramaEventRepository.save(event);
    Long originalFans = testWrestler1.getFans();

    // When
    dramaEventService.processEvent(savedEvent);

    // Then
    DramaEvent processedEvent = dramaEventRepository.findById(savedEvent.getId()).orElseThrow();
    assertThat(processedEvent.getIsProcessed()).isTrue();
    assertThat(processedEvent.getProcessedDate()).isNotNull();
    assertThat(processedEvent.getProcessingNotes()).isNotBlank();

    // Check fan impact was applied
    Wrestler updatedWrestler = wrestlerRepository.findById(testWrestler1.getId()).orElseThrow();
    assertThat(updatedWrestler.getFans()).isEqualTo(originalFans + 1000L);
  }

  @Test
  @DisplayName("Should process multiple unprocessed events")
  void shouldProcessMultipleUnprocessedEvents() {
    // Given - Create multiple unprocessed events
    for (int i = 0; i < 3; i++) {
      DramaEvent event = new DramaEvent();
      event.setPrimaryWrestler(testWrestler1);
      event.setEventType(DramaEventType.MEDIA_CONTROVERSY);
      event.setSeverity(DramaEventSeverity.NEUTRAL);
      event.setTitle("Test Event " + i);
      event.setDescription("Test description " + i);
      event.setIsProcessed(false);
      dramaEventRepository.save(event);
    }

    // When
    int processedCount = dramaEventService.processUnprocessedEvents();

    // Then
    assertThat(processedCount).isEqualTo(3);

    List<DramaEvent> unprocessedEvents =
        dramaEventRepository.findByIsProcessedFalseOrderByEventDateAsc();
    assertThat(unprocessedEvents).isEmpty();
  }

  @Test
  @DisplayName("Should retrieve events for wrestler")
  void shouldRetrieveEventsForWrestler() {
    // Given - Create events for wrestler
    dramaEventService.createDramaEvent(
        testWrestler1.getId(),
        null,
        DramaEventType.PERSONAL_ISSUE,
        DramaEventSeverity.NEUTRAL,
        "Event 1",
        "Description 1");

    dramaEventService.createDramaEvent(
        testWrestler1.getId(),
        testWrestler2.getId(),
        DramaEventType.BACKSTAGE_INCIDENT,
        DramaEventSeverity.NEGATIVE,
        "Event 2",
        "Description 2");

    // When
    List<DramaEvent> events = dramaEventService.getEventsForWrestler(testWrestler1.getId());

    // Then
    assertThat(events).hasSize(2);
    assertThat(events)
        .allMatch(
            event ->
                event.getPrimaryWrestler().equals(testWrestler1)
                    || event.getSecondaryWrestler().equals(testWrestler1));
  }

  @Test
  @DisplayName("Should retrieve events between two wrestlers")
  void shouldRetrieveEventsBetweenWrestlers() {
    // Given - Create events between wrestlers
    dramaEventService.createDramaEvent(
        testWrestler1.getId(),
        testWrestler2.getId(),
        DramaEventType.BETRAYAL,
        DramaEventSeverity.MAJOR,
        "Betrayal Event",
        "Major betrayal");

    // Create an event not involving both wrestlers
    Wrestler otherWrestler = createTestWrestler("Other Wrestler");
    dramaEventService.createDramaEvent(
        testWrestler1.getId(),
        otherWrestler.getId(),
        DramaEventType.ALLIANCE_FORMED,
        DramaEventSeverity.POSITIVE,
        "Alliance Event",
        "New alliance");

    // When
    List<DramaEvent> events =
        dramaEventService.getEventsBetweenWrestlers(testWrestler1.getId(), testWrestler2.getId());

    // Then
    assertThat(events).hasSize(1);
    DramaEvent event = events.get(0);
    assertThat(event.getEventType()).isEqualTo(DramaEventType.BETRAYAL);
    assertThat(
            (event.getPrimaryWrestler().equals(testWrestler1)
                    && event.getSecondaryWrestler().equals(testWrestler2))
                || (event.getPrimaryWrestler().equals(testWrestler2)
                    && event.getSecondaryWrestler().equals(testWrestler1)))
        .isTrue();
  }

  @Test
  @DisplayName("Should handle event impact calculations correctly")
  void shouldHandleEventImpactCalculationsCorrectly() {
    // Given
    DramaEventType fanAffectingType = DramaEventType.FAN_INTERACTION;
    DramaEventSeverity positiveSeverity = DramaEventSeverity.POSITIVE;

    // When
    Optional<DramaEvent> result =
        dramaEventService.createDramaEvent(
            testWrestler1.getId(),
            null,
            fanAffectingType,
            positiveSeverity,
            "Fan Event",
            "Positive fan interaction");

    // Then
    assertThat(result).isPresent();
    DramaEvent event = result.get();

    // Fan-affecting positive event should have positive fan impact
    assertThat(event.getFanImpact()).isNotNull();
    assertThat(event.getFanImpact()).isPositive();
    assertThat(event.hasPositiveImpact()).isTrue();
  }
}
