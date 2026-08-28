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
package com.github.javydreamercsw.management.event.inbox;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.feud.FeudParticipant;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeud;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.event.FeudResolvedEvent;
import com.github.javydreamercsw.management.service.title.ContenderSelectionService;
import com.github.javydreamercsw.management.service.title.TitleService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FeudResolvedContenderListenerTest {

  @Mock private TitleService titleService;
  @Mock private ContenderSelectionService contenderSelectionService;

  @InjectMocks private FeudResolvedContenderListener listener;

  private Wrestler participant;
  private Wrestler outsider;
  private MultiWrestlerFeud feud;

  @BeforeEach
  void setUp() {
    participant = new Wrestler();
    participant.setId(1L);
    participant.setName("Feud Member");

    outsider = new Wrestler();
    outsider.setId(2L);
    outsider.setName("Uninvolved");

    feud = new MultiWrestlerFeud();
    feud.setName("Big Feud");
    FeudParticipant fp = new FeudParticipant();
    fp.setWrestler(participant);
    fp.setIsActive(true);
    feud.getParticipants().add(fp);
  }

  private Title titleWithChallenger(final long id, final Wrestler challenger) {
    Title title = new Title();
    title.setId(id);
    title.setName("Title " + id);
    title.addChallenger(challenger);
    return title;
  }

  @Test
  void feudResolved_rotatesContenderForTitlesWithParticipantChallengers() {
    Title affected = titleWithChallenger(1L, participant);
    Title unaffected = titleWithChallenger(2L, outsider);
    when(titleService.getActiveTitles()).thenReturn(List.of(affected, unaffected));

    listener.onApplicationEvent(new FeudResolvedEvent(this, feud));

    verify(contenderSelectionService).autoSelectNextContender(affected);
    verify(contenderSelectionService, never()).autoSelectNextContender(unaffected);
  }

  @Test
  void feudResolved_noParticipants_doesNothing() {
    MultiWrestlerFeud emptyFeud = new MultiWrestlerFeud();
    emptyFeud.setName("Empty Feud");

    listener.onApplicationEvent(new FeudResolvedEvent(this, emptyFeud));

    verifyNoInteractions(titleService, contenderSelectionService);
  }
}
