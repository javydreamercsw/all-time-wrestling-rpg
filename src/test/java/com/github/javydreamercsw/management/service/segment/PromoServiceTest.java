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
package com.github.javydreamercsw.management.service.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PromoServiceTest {

  private PromoService promoService;
  private SegmentNarrationService aiService;

  @BeforeEach
  void setUp() {
    SegmentNarrationServiceFactory aiFactory = mock(SegmentNarrationServiceFactory.class);
    aiService = mock(SegmentNarrationService.class);
    when(aiFactory.getBestAvailableService()).thenReturn(aiService);
    when(aiService.isAvailable()).thenReturn(true);
    promoService = new PromoService(aiFactory);
  }

  @Test
  void testGenerateRetort() {
    String playerText = "I'm going to crush you!";
    String expectedRetort = "You couldn't crush a grape!";
    Wrestler opponent = new Wrestler();
    opponent.setName("The Rock");
    opponent.setDescription("The Great One");

    WrestlerAlignment alignment =
        WrestlerAlignment.builder().alignmentType(AlignmentType.FACE).build();
    opponent.setAlignment(alignment);

    Show show = new Show();
    show.setName("Monday Night Raw");

    Segment segment = new Segment();
    segment.setShow(show);

    when(aiService.generateText(anyString())).thenReturn(expectedRetort);

    String result = promoService.generateRetort(playerText, segment, opponent);

    assertEquals(expectedRetort, result);

    ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

    verify(aiService).generateText(promptCaptor.capture());

    String prompt = promptCaptor.getValue();
    assert (prompt.contains("The Rock"));
    assert (prompt.contains("The Great One"));
    assert (prompt.contains("Monday Night Raw"));
    assert (prompt.contains(playerText));
  }
}
