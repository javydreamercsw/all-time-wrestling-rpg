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
package com.github.javydreamercsw.management.ui.view.feud;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.feud.FeudScript;
import com.github.javydreamercsw.management.domain.feud.FeudScriptStatus;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.feud.FeudBeatAssistantService;
import com.github.javydreamercsw.management.service.feud.FeudScriptService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

/** Unit tests for the story-arcs list view (the only surface for feud-linked arcs). */
class FeudScriptListViewTest extends AbstractViewTest {

  @Mock private FeudScriptService feudScriptService;
  @Mock private WrestlerService wrestlerService;
  @Mock private SegmentTypeService segmentTypeService;
  @Mock private SegmentRuleService segmentRuleService;
  @Mock private FeudBeatAssistantService feudBeatAssistantService;
  @Mock private SecurityUtils securityUtils;
  @Mock private ShowService showService;
  @Mock private TitleService titleService;

  private Wrestler wrestler1;
  private Wrestler wrestler2;

  @BeforeEach
  void setup() {
    wrestler1 = wrestler(1L, "Adam Axe");
    wrestler2 = wrestler(2L, "Bob Boulder");

    when(securityUtils.canCreate()).thenReturn(true);
    when(wrestlerService.getAllWrestlers()).thenReturn(List.of(wrestler1, wrestler2));
    when(wrestlerService.findAllFiltered(any(), any(), any(), any(), any())).thenReturn(List.of());
    when(segmentTypeService.findAll()).thenReturn(List.of());
    when(segmentRuleService.findAll()).thenReturn(List.of());
    when(feudScriptService.getDefaultMaxPleAppearances()).thenReturn(2);
    when(showService.getUpcomingShows(any(Integer.class))).thenReturn(List.of());
    when(titleService.getActiveTitles()).thenReturn(List.of());
  }

  private static Wrestler wrestler(Long id, String name) {
    Wrestler wrestler = new Wrestler();
    wrestler.setId(id);
    wrestler.setName(name);
    return wrestler;
  }

  private FeudScriptListView newView(List<FeudScript> scripts) {
    when(feudScriptService.getAllScriptsWithBeats()).thenReturn(scripts);
    return new FeudScriptListView(
        feudScriptService,
        wrestlerService,
        segmentTypeService,
        segmentRuleService,
        feudBeatAssistantService,
        securityUtils,
        showService,
        titleService);
  }

  /** All text held by span-like components anywhere in the tree. */
  private static List<String> componentTexts(Component root) {
    List<String> texts = new ArrayList<>();
    collectTexts(root, texts);
    return texts;
  }

  private static void collectTexts(Component c, List<String> texts) {
    if (c instanceof com.vaadin.flow.component.html.Span span && span.getText() != null) {
      texts.add(span.getText());
    }
    if (c instanceof com.vaadin.flow.component.html.Paragraph p && p.getText() != null) {
      texts.add(p.getText());
    }
    c.getChildren().forEach(child -> collectTexts(child, texts));
  }

  @Test
  @DisplayName("Empty state shows the create hint")
  void emptyState_showsCreateHint() {
    FeudScriptListView view = newView(List.of());
    assertTrue(
        componentTexts(view)
            .contains("No story arcs yet — create one from a rivalry or the button above."));
  }

  @Test
  @DisplayName("Each saved arc renders as a card with its name")
  void savedArcs_renderAsCards() {
    FeudScript script = new FeudScript();
    script.setName("My Arcade");
    script.setStatus(FeudScriptStatus.ACTIVE);
    script.setMaxPleAppearances(2);
    Rivalry rivalry = new Rivalry();
    rivalry.setWrestler1(wrestler1);
    rivalry.setWrestler2(wrestler2);
    script.setRivalry(rivalry);

    FeudScriptListView view = newView(List.of(script));
    assertTrue(componentTexts(view).contains("My Arcade"));
  }

  @Test
  @DisplayName("New Story Arc button is visible for users who can create")
  void newArcButton_visibleWhenCanCreate() {
    FeudScriptListView view = newView(List.of());
    assertTrue(_get(view, com.vaadin.flow.component.button.Button.class).isVisible());
  }

  @Test
  @DisplayName("New Story Arc button is hidden for users who cannot create")
  void newArcButton_hiddenWhenCannotCreate() {
    when(securityUtils.canCreate()).thenReturn(false);
    FeudScriptListView view = newView(List.of());
    List<Button> buttons =
        view.getChildren()
            .flatMap(c -> c.getChildren())
            .filter(Button.class::isInstance)
            .map(c -> (Button) c)
            .toList();
    assertTrue(buttons.stream().noneMatch(Button::isVisible));
  }
}
