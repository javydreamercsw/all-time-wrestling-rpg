/*
* Copyright (C) 2025 Software Consulting Dreams LLC
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
package com.github.javydreamercsw.management.service.sync.entity.notion.outgoing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.AdjudicationStatus;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.SegmentStatus;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.sync.entity.notion.SegmentNotionSyncService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import notion.api.v1.NotionClient;
import notion.api.v1.model.pages.Page;
import notion.api.v1.request.pages.CreatePageRequest;
import notion.api.v1.request.pages.UpdatePageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class SegmentNotionSyncServiceIT extends ManagementIntegrationTest {

  @Autowired private SegmentRepository segmentRepository;
  @Autowired private ShowRepository showRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;
  @Autowired private SegmentRuleRepository segmentRuleRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private FactionRepository factionRepository;
  @Autowired private SegmentNotionSyncService segmentNotionSyncService;

  @MockitoBean private NotionHandler notionHandler;

  @Mock private NotionClient notionClient;
  @Mock private Page newPage;

  @Captor private ArgumentCaptor<CreatePageRequest> createPageRequestCaptor;
  @Captor private ArgumentCaptor<UpdatePageRequest> updatePageRequestCaptor;

  @BeforeEach
  public void setup() {
    clearAllRepositories();
  }

  @Test
  void testSyncToNotion() {
    when(notionHandler.createNotionClient()).thenReturn(java.util.Optional.of(notionClient));

    String newPageId = UUID.randomUUID().toString();
    when(newPage.getId()).thenReturn(newPageId);

    when(notionClient.createPage(any(CreatePageRequest.class))).thenReturn(newPage);
    when(notionClient.updatePage(any(UpdatePageRequest.class))).thenReturn(newPage);
    when(notionHandler.getDatabaseId("Segments")).thenReturn("test-db-id");
    when(notionHandler.executeWithRetry(any()))
        .thenAnswer(
            (Answer<Page>)
                invocation -> {
                  java.util.function.Supplier<Page> supplier = invocation.getArgument(0);
                  return supplier.get();
                });

    // Create a Show Type
    ShowType showType = new ShowType();
    showType.setName("Weekly Show");
    showType.setDescription("A weekly show type."); // Added description
    showType.setExternalId(UUID.randomUUID().toString()); // Simulate external ID from prior sync
    showTypeRepository.save(showType);

    // Create a Show
    Show show = new Show();
    show.setName("Test Show " + UUID.randomUUID());
    show.setDescription("A test show."); // Added description
    show.setShowDate(LocalDate.now()); // Corrected to LocalDate
    show.setType(showType); // Set the show type
    show.setExternalId(UUID.randomUUID().toString()); // Simulate external ID from prior sync
    showRepository.save(show);

    // Create a Segment Type
    SegmentType segmentType = new SegmentType();
    segmentType.setName("Match");
    segmentType.setExternalId(UUID.randomUUID().toString()); // Simulate external ID from prior sync
    segmentTypeRepository.save(segmentType);

    // Create a Segment Rule
    SegmentRule segmentRule = new SegmentRule();
    segmentRule.setName("No DQ");
    segmentRule.setExternalId(UUID.randomUUID().toString()); // Simulate external ID from prior sync
    segmentRuleRepository.save(segmentRule);

    // Create a Faction (for Wrestler)
    Faction faction = new Faction();
    faction.setName("Test Faction " + UUID.randomUUID());
    factionRepository.save(faction);

    // Create a Wrestler
    Wrestler wrestler = new Wrestler();
    wrestler.setName("Test Wrestler " + UUID.randomUUID());
    wrestler.setStartingStamina(16);
    wrestler.setFans(1000L);
    wrestler.setBumps(1);
    wrestler.setGender(Gender.MALE);
    wrestler.setLowStamina(2);
    wrestler.setStartingHealth(15);
    wrestler.setLowHealth(4);
    wrestler.setDeckSize(15);
    wrestler.setTier(WrestlerTier.MIDCARDER);
    wrestler.setCreationDate(Instant.now());
    wrestler.setFaction(faction);
    wrestler.setExternalId(UUID.randomUUID().toString()); // Simulate external ID from prior sync
    wrestlerRepository.save(wrestler);

    // Create a new Segment
    Segment segment = new Segment();
    segment.setShow(show);
    segment.setSegmentType(segmentType);
    segment.setSegmentDate(Instant.now()); // Reverted reflection hack
    segment.setStatus(SegmentStatus.BOOKED);
    segment.setAdjudicationStatus(AdjudicationStatus.PENDING);
    segment.addSegmentRule(segmentRule);
    segment.setNarration("This is a test narration.");
    segment.setSummary("This is a test summary.");
    segment.setIsTitleSegment(false);
    segment.setIsNpcGenerated(false);
    segment.setSegmentOrder(1);
    segment.setMainEvent(true);
    segment.addParticipant(wrestler); // Add wrestler as a participant
    segmentRepository.save(segment);

    // Sync to Notion for the first time
    segmentNotionSyncService.syncToNotion("test-op-1");

    // Verify that the externalId and lastSync fields are updated
    assertNotNull(segment.getId());
    Segment updatedSegment = segmentRepository.findById(segment.getId()).get();
    assertNotNull(updatedSegment.getExternalId());
    assertEquals(newPageId, updatedSegment.getExternalId());
    assertNotNull(updatedSegment.getLastSync());

    // Verify properties sent to Notion
    Mockito.verify(notionClient).createPage(createPageRequestCaptor.capture());
    CreatePageRequest capturedRequest = createPageRequestCaptor.getValue();
    assertEquals(
        segment.getSegmentType().getName() + " - " + segment.getShow().getName(),
        capturedRequest.getProperties().get("Name").getTitle().get(0).getText().getContent());
    assertEquals(
        segment.getShow().getExternalId(),
        capturedRequest.getProperties().get("Shows").getRelation().get(0).getId());
    assertEquals(
        segment.getSegmentType().getExternalId(),
        capturedRequest.getProperties().get("Segment Type").getRelation().get(0).getId());
    assertEquals(
        segment.getSegmentDate().truncatedTo(java.time.temporal.ChronoUnit.MILLIS),
        Instant.parse(capturedRequest.getProperties().get("Date").getDate().getStart())
            .truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
    assertEquals(
        segment.getSegmentRules().get(0).getExternalId(),
        capturedRequest.getProperties().get("Rules").getRelation().get(0).getId());
    assertEquals(
        segment.getNarration(),
        capturedRequest
            .getProperties()
            .get("Description")
            .getRichText()
            .get(0)
            .getText()
            .getContent());
    assertEquals(
        segment.getSummary(),
        capturedRequest.getProperties().get("Summary").getRichText().get(0).getText().getContent());
    assertEquals(
        segment.getWrestlers().get(0).getExternalId(),
        capturedRequest.getProperties().get("Participants").getRelation().get(0).getId());

    // Sync to Notion again
    updatedSegment.setNarration("Updated narration " + UUID.randomUUID());
    updatedSegment.setSummary("Updated summary " + UUID.randomUUID());
    updatedSegment.setSegmentOrder(2);
    updatedSegment.setMainEvent(false);
    segmentRepository.save(updatedSegment);
    segmentNotionSyncService.syncToNotion("test-op-2");
    Segment updatedSegment2 = segmentRepository.findById(segment.getId()).get();
    assertTrue(updatedSegment2.getLastSync().isAfter(updatedSegment.getLastSync()));

    // Verify updated properties sent to Notion
    Mockito.verify(notionClient).updatePage(updatePageRequestCaptor.capture());
    UpdatePageRequest capturedUpdateRequest = updatePageRequestCaptor.getValue();
    assertEquals(
        updatedSegment2.getNarration(),
        capturedUpdateRequest
            .getProperties()
            .get("Description")
            .getRichText()
            .get(0)
            .getText()
            .getContent());
    assertEquals(
        updatedSegment2.getSummary(),
        capturedUpdateRequest
            .getProperties()
            .get("Summary")
            .getRichText()
            .get(0)
            .getText()
            .getContent());
  }
}
