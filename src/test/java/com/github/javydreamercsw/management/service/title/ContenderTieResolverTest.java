/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.service.title;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.inbox.InboxItemTarget;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.WellKnownSegmentType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.dto.ranking.RankedWrestlerDTO;
import com.github.javydreamercsw.management.event.ContenderTieDetectedEvent;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContenderTieResolverTest {

  @Mock private InboxService inboxService;
  @Mock private SegmentTypeRepository segmentTypeRepository;
  @Mock private GameSettingService gameSettingService;

  private ContenderTieResolver resolver;
  private Title title;
  private InboxItem inboxItem;

  @BeforeEach
  void setUp() {
    resolver =
        new ContenderTieResolver(
            inboxService,
            new InboxEventType("CONTENDER_TIE_DETECTED", "Contender Tie Detected"),
            segmentTypeRepository,
            gameSettingService);

    title = new Title();
    title.setId(1L);
    title.setName("World Title");

    inboxItem = new InboxItem();
    when(inboxService.createInboxItem(
            any(InboxEventType.class),
            anyString(),
            anyString(),
            any(InboxItem.Urgency.class),
            anyString(),
            any(InboxItemTarget.TargetType.class)))
        .thenReturn(inboxItem);

    when(gameSettingService.getContenderTieMinWrestlers()).thenReturn(3);
    when(gameSettingService.getContenderTieMatchType())
        .thenReturn(WellKnownSegmentType.FREE_FOR_ALL.getCode());

    SegmentType freeForAll = new SegmentType();
    freeForAll.setName("Free-for-All");
    freeForAll.setCode(WellKnownSegmentType.FREE_FOR_ALL.getCode());
    when(segmentTypeRepository.findByCode(WellKnownSegmentType.FREE_FOR_ALL.getCode()))
        .thenReturn(Optional.of(freeForAll));

    SegmentType oneOnOne = new SegmentType();
    oneOnOne.setName("One on One");
    oneOnOne.setCode(WellKnownSegmentType.ONE_ON_ONE.getCode());
    when(segmentTypeRepository.findByCode(WellKnownSegmentType.ONE_ON_ONE.getCode()))
        .thenReturn(Optional.of(oneOnOne));
  }

  private RankedWrestlerDTO ranked(final long id, final String name) {
    return RankedWrestlerDTO.builder().id(id).name(name).fans(1000L).build();
  }

  @Test
  void enoughTiedWrestlers_suggestsConfiguredMultiManMatch() {
    ContenderTieDetectedEvent event =
        new ContenderTieDetectedEvent(
            this, title, List.of(ranked(1L, "A"), ranked(2L, "B"), ranked(3L, "C")));

    resolver.onApplicationEvent(event);

    verify(inboxService)
        .createInboxItem(
            any(InboxEventType.class),
            eq("Contender Tie: World Title"),
            contains("Free-for-All"),
            eq(InboxItem.Urgency.ACTION_REQUIRED),
            eq("1"),
            eq(InboxItemTarget.TargetType.TITLE));
    verify(inboxService).save(inboxItem);
    assertThat(inboxItem.getActionType()).isEqualTo("NAVIGATE");
  }

  @Test
  void onlyTwoTied_suggestsOneOnOneInstead() {
    ContenderTieDetectedEvent event =
        new ContenderTieDetectedEvent(this, title, List.of(ranked(1L, "A"), ranked(2L, "B")));

    resolver.onApplicationEvent(event);

    verify(inboxService)
        .createInboxItem(
            any(InboxEventType.class),
            anyString(),
            contains("One on One"),
            any(InboxItem.Urgency.class),
            anyString(),
            any(InboxItemTarget.TargetType.class));
  }

  @Test
  void unknownMatchTypeCode_fallsBackToCodeInMessage() {
    when(gameSettingService.getContenderTieMatchType()).thenReturn("custom_type");
    when(segmentTypeRepository.findByCode("custom_type")).thenReturn(Optional.empty());

    ContenderTieDetectedEvent event =
        new ContenderTieDetectedEvent(
            this, title, List.of(ranked(1L, "A"), ranked(2L, "B"), ranked(3L, "C")));

    resolver.onApplicationEvent(event);

    verify(inboxService)
        .createInboxItem(
            any(InboxEventType.class),
            anyString(),
            contains("custom_type"),
            any(InboxItem.Urgency.class),
            anyString(),
            any(InboxItemTarget.TargetType.class));
  }

  @Test
  void messageListsAllTiedParticipants() {
    ContenderTieDetectedEvent event =
        new ContenderTieDetectedEvent(
            this, title, List.of(ranked(1L, "Alpha"), ranked(2L, "Beta"), ranked(3L, "Gamma")));

    resolver.onApplicationEvent(event);

    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
    verify(inboxService)
        .createInboxItem(
            any(InboxEventType.class),
            anyString(),
            messageCaptor.capture(),
            any(InboxItem.Urgency.class),
            anyString(),
            any(InboxItemTarget.TargetType.class));
    assertThat(messageCaptor.getValue()).contains("Alpha", "Beta", "Gamma");
  }
}
