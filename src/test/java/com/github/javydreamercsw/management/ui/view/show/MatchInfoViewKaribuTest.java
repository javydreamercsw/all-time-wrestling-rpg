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
package com.github.javydreamercsw.management.ui.view.show;

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRulePlayGuide;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleVariantGuide;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.RouteParameters;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MatchInfoViewKaribuTest extends AbstractViewTest {

  private SegmentRuleService segmentRuleService;

  private SegmentRule ruleWithGuide;
  private SegmentRule ruleWithoutGuide;

  @BeforeEach
  void setUpRules() {
    segmentRuleService = mock(SegmentRuleService.class);

    SegmentRuleVariantGuide solo =
        new SegmentRuleVariantGuide(
            "Solo overview here",
            "Solo setup here",
            "Attacking instructions",
            "Defending instructions",
            "Win by pin",
            "NPC recovers slowly",
            null,
            null,
            null,
            null,
            null,
            null);
    SegmentRuleVariantGuide multiplayer =
        new SegmentRuleVariantGuide(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "Multiplayer concepts",
            "Gameplay changes",
            "Abilities",
            "Game ends when one team remains");
    SegmentRulePlayGuide guide = new SegmentRulePlayGuide(solo, multiplayer);

    ruleWithGuide = new SegmentRule();
    ruleWithGuide.setName("Cage");
    ruleWithGuide.setDescription("Steel cage match description.");
    ruleWithGuide.setGuide(guide);

    ruleWithoutGuide = new SegmentRule();
    ruleWithoutGuide.setName("Normal");
    ruleWithoutGuide.setDescription("Standard match.");
  }

  private MatchInfoView createView(final long ruleId, final SegmentRule rule) {
    when(segmentRuleService.findById(ruleId)).thenReturn(Optional.ofNullable(rule));
    MatchInfoView view = new MatchInfoView(segmentRuleService);
    UI.getCurrent().add(view);

    BeforeEnterEvent event = mock(BeforeEnterEvent.class);
    RouteParameters params = new RouteParameters("ruleId", String.valueOf(ruleId));
    when(event.getRouteParameters()).thenReturn(params);
    view.beforeEnter(event);
    return view;
  }

  @Test
  void fullGuide_rendersTitle() {
    createView(1L, ruleWithGuide);

    List<H2> headings = _find(H2.class);
    assertThat(headings).extracting(H2::getText).contains("Cage");
  }

  @Test
  void fullGuide_rendersSoloDetailsSection() {
    createView(2L, ruleWithGuide);

    List<Details> sections = _find(Details.class);
    assertThat(sections.stream().anyMatch(d -> "Solo Play".equals(d.getSummaryText()))).isTrue();
  }

  @Test
  void fullGuide_rendersMultiplayerDetailsSection() {
    createView(3L, ruleWithGuide);

    List<Details> sections = _find(Details.class);
    assertThat(sections.stream().anyMatch(d -> "Multiplayer".equals(d.getSummaryText()))).isTrue();
  }

  @Test
  void fullGuide_rendersSectionHeadings() {
    createView(4L, ruleWithGuide);

    List<H3> h3s = _find(H3.class);
    assertThat(h3s).extracting(H3::getText).contains("Overview", "Setup", "Win Condition");
  }

  @Test
  void soloSectionOpen_multiplayerSectionClosed_byDefault() {
    createView(5L, ruleWithGuide);

    List<Details> sections = _find(Details.class);
    Details soloSection =
        sections.stream()
            .filter(d -> "Solo Play".equals(d.getSummaryText()))
            .findFirst()
            .orElseThrow();
    Details multiSection =
        sections.stream()
            .filter(d -> "Multiplayer".equals(d.getSummaryText()))
            .findFirst()
            .orElseThrow();

    assertThat(soloSection.isOpened()).isTrue();
    assertThat(multiSection.isOpened()).isFalse();
  }

  @Test
  void nullRules_showsPlaceholderSpan() {
    createView(6L, ruleWithoutGuide);

    List<Span> spans = _find(Span.class);
    assertThat(spans.stream().anyMatch(s -> s.getText().contains("No gameplay rules documented")))
        .isTrue();
  }

  @Test
  void unknownRuleId_showsNotFoundSpan() {
    createView(99L, null);

    List<Span> spans = _find(Span.class);
    assertThat(spans.stream().anyMatch(s -> s.getText().contains("Match rule not found"))).isTrue();
  }
}
