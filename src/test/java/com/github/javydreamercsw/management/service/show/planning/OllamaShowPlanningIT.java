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
package com.github.javydreamercsw.management.service.show.planning;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.base.ai.ollama.OllamaSegmentNarrationService;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.service.HolidayService;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningContextDTO;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningRosterEntryDTO;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

/**
 * Real-LLM integration test for AI show planning against a LOCAL Ollama instance (spike prototype
 * for ATW-hgmt; production version tracked by ATW-hndr).
 *
 * <p><b>Opt-in only.</b> The whole class is skipped unless {@code OLLAMA_BASE_URL} is set, so
 * regular {@code -Pintegration-test} runs (developer machines and the per-PR CI job) are
 * unaffected. To run locally:
 *
 * <pre>
 *   ollama serve &amp;
 *   ollama pull llama3.2:1b
 *   OLLAMA_BASE_URL=http://localhost:11434 mvn -Pintegration-test verify \
 *       -Dit.test=OllamaShowPlanningIT -Dsurefire.skip=true
 * </pre>
 *
 * <p><b>Assertion strategy:</b> the LLM output is nondeterministic, so assertions are shape-based
 * only — the response must parse into segments (enforced inside {@link ShowPlanningAiService}), and
 * every proposed participant must come from the roster that was supplied in the prompt. Since the
 * supplied roster is all-female (mirroring what {@code ShowPlanningService} produces for a women's
 * show template), any hallucinated or male participant fails the roster-membership check. No
 * assertion ever inspects narration text.
 */
@Slf4j
@Tag("ollama")
@ActiveProfiles("ollama") // merged with parent "test" profile; activates the Ollama provider bean
@EnabledIfEnvironmentVariable(named = "OLLAMA_BASE_URL", matches = ".+")
class OllamaShowPlanningIT extends ManagementIntegrationTest {

  @Autowired private OllamaSegmentNarrationService ollamaService;
  @Autowired private HolidayService holidayService;

  /**
   * ShowPlanningAiService wired to a factory containing ONLY the Ollama provider. This sidesteps
   * MockSegmentNarrationService, which is {@code @Primary} under the "test" profile and would
   * otherwise win the factory's priority ordering. (ATW-hndr: alternatively add an "Ollama"
   * ProviderPriority above Mock in SegmentNarrationServiceFactory and use the context bean.)
   */
  private ShowPlanningAiService aiService;

  private static final List<String> FEMALE_ROSTER =
      List.of("Trish Stratus", "Lita", "Chyna", "Molly Holly", "Jazz", "Victoria");

  @BeforeEach
  void setUpAiService() {
    aiService =
        new ShowPlanningAiService(
            new SegmentNarrationServiceFactory(List.of(ollamaService)),
            objectMapper,
            segmentTypeService,
            segmentRuleService,
            holidayService);
  }

  @Test
  void plansWomensShowUsingOnlyProvidedRoster() {
    ShowPlanningContextDTO context = womensShowContext();

    ProposedShow show = aiService.planShow(context);

    // Shape assertion 1: the response parsed into a JSON array of segments (parse failures throw
    // ShowPlanningException inside planShow) and produced at least one segment.
    assertThat(show.getSegments()).as("AI should propose at least one segment").isNotEmpty();

    // Shape assertion 2: every participant references a roster name — the roster given to the
    // prompt was all-female, so this is the intergender booking check.
    Set<String> allowedNames =
        FEMALE_ROSTER.stream().map(n -> n.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());

    for (ProposedSegment segment : show.getSegments()) {
      assertThat(segment.getType()).as("segment type must be present").isNotBlank();
      if (segment.getTeams() == null) {
        continue; // promos without explicit teams are acceptable
      }
      List<String> participants =
          segment.getTeams().stream().flatMap(List::stream).collect(Collectors.toList());
      assertThat(participants)
          .as("all participants must come from the provided all-female roster: %s", participants)
          .allSatisfy(name -> assertThat(allowedNames).contains(name.toLowerCase(Locale.ROOT)));
    }
  }

  /**
   * Builds the planning context the way ShowPlanningService would for a show whose template has
   * {@code genderConstraint == FEMALE}: rivalries, titles, and roster already filtered to female
   * wrestlers. Built directly as a DTO to keep the prototype independent of Show/Template
   * persistence plumbing — ATW-hndr can switch to persisting a Show and calling
   * showPlanningService.getShowPlanningContext(show) for full end-to-end coverage.
   */
  private ShowPlanningContextDTO womensShowContext() {
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();

    ShowTemplate template = new ShowTemplate();
    template.setShowName("Women's Weekly");
    template.setDescription("All-women weekly show");
    template.setExpectedMatches(3);
    template.setExpectedPromos(1);
    template.setGenderConstraint(Gender.FEMALE);
    context.setShowTemplate(template);

    context.setShowDate(Instant.now());
    context.setPremiumLiveEvent(false);
    context.setRecentSegments(List.of());
    context.setCurrentRivalries(List.of());
    context.setChampionships(List.of());
    context.setFactions(List.of());
    context.setFullRoster(
        FEMALE_ROSTER.stream().map(this::femaleRosterEntry).collect(Collectors.toList()));
    return context;
  }

  private ShowPlanningRosterEntryDTO femaleRosterEntry(final String name) {
    ShowPlanningRosterEntryDTO entry = new ShowPlanningRosterEntryDTO();
    entry.setId((long) (FEMALE_ROSTER.indexOf(name) + 1));
    entry.setName(name);
    entry.setGender(Gender.FEMALE.name());
    entry.setTier(WrestlerTier.MIDCARDER);
    entry.setFans(10_000L);
    entry.setAlignment("FACE");
    entry.setInjured(false);
    return entry;
  }
}
