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
package com.github.javydreamercsw.management.ui.component;

import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.ui.view.AbstractDocsE2ETest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Docs screenshot for the AI onboarding wizard (ATW-pq0s): captures the "Set up AI…" entry point
 * that appears at every AI dead end — here the Show Detail view's Summarize action on a fresh
 * install with no AI provider configured. The e2e profile registers a {@code @Primary} mock
 * narration service; the stubbed factory restores the real "no provider configured" shape.
 */
class AiOnboardingDocsE2ETest extends AbstractDocsE2ETest {

  @MockitoBean private SegmentNarrationServiceFactory segmentNarrationServiceFactory;

  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;
  @Autowired private UniverseRepository universeRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private SegmentRepository segmentRepository;
  @Autowired private ShowRepository showRepository;

  @Test
  void captureAiOnboardingDialog() {
    // Fresh-fixture shape: syncs seeded the type catalogs but no AI provider is configured,
    // so every AI action dead-ends into the onboarding wizard.
    when(segmentNarrationServiceFactory.getAvailableServicesInPriorityOrder())
        .thenReturn(List.of());
    ShowType showType = showTypeRepository.findByName("Weekly").orElseThrow();
    Universe universe = universeRepository.findAll().stream().findFirst().orElseThrow();

    Wrestler wrestler =
        wrestlerRepository
            .findByName("Apollo Vega")
            .orElseGet(
                () -> {
                  Wrestler w = new Wrestler();
                  w.setName("Apollo Vega");
                  return wrestlerRepository.saveAndFlush(w);
                });

    Show show = new Show();
    show.setName("AI Onboarding Screenshot Show");
    show.setDescription("Docs fixture for the AI onboarding wizard.");
    show.setType(showType);
    show.setUniverse(universe);
    show.setShowDate(LocalDate.now());
    show.setCreationDate(Instant.now());
    show = showRepository.saveAndFlush(show);

    SegmentType matchType = segmentTypeRepository.findByName("One on One").orElseThrow();
    Segment segment = new Segment();
    segment.setShow(show);
    segment.setSegmentType(matchType);
    segment.setSegmentDate(Instant.now());
    segment.addParticipant(wrestler);
    segment.setNarration("Apollo Vega wins a hard-fought opening contest in under ten minutes.");
    segment = segmentRepository.saveAndFlush(segment);

    navigateToAndWaitForElement(
        "show-detail/" + show.getId(),
        By.xpath("//*[contains(., 'AI Onboarding Screenshot Show')]"));

    // No AI provider configured → Summarize dead-ends into the notification that carries
    // the "Set up AI…" button opening the wizard.
    clickElement(
        waitForVaadinElement(
            driver,
            By.cssSelector("vaadin-button[id='generate-summary-button-" + segment.getId() + "']")));

    waitForVaadinElement(driver, By.xpath("//*[contains(text(), 'Set up AI')]"));

    documentFeature(
        "Admin",
        "AI Onboarding Wizard",
        """
        Fresh installs ship with no AI provider configured. Every AI action — match narration,\
         show summaries, segment planning — dead-ends into this entry point. "Set up AI…" opens\
         the wizard, which walks you through Google Gemini's free tier: it opens the API key\
         page in your browser, you paste the key, and the game verifies the provider is live.\
         The key is stored per universe, so each player configures their own.\
        """,
        "admin-ai-onboarding");
  }
}
