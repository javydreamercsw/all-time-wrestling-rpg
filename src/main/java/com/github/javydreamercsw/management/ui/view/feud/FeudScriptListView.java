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

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.feud.FeudScript;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.feud.FeudBeatAssistantService;
import com.github.javydreamercsw.management.service.feud.FeudScriptService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;

/**
 * Every story arc — rivalry-linked and multi-wrestler feud-linked — in one place. Feud-linked arcs
 * (3+ wrestler arcs) have no rivalry detail page of their own, so this route is their only
 * management surface.
 */
@Route("story-arcs")
@PageTitle("Story Arcs")
@Menu(order = 2, icon = "vaadin:book", title = "Story Arcs")
@PermitAll
public class FeudScriptListView extends Main {

  private final FeudScriptService feudScriptService;
  private final WrestlerService wrestlerService;
  private final SegmentTypeService segmentTypeService;
  private final SegmentRuleService segmentRuleService;
  private final FeudBeatAssistantService feudBeatAssistantService;
  private final SecurityUtils securityUtils;
  private final ShowService showService;
  private final TitleService titleService;

  private final VerticalLayout content = new VerticalLayout();

  public FeudScriptListView(
      @NonNull final FeudScriptService feudScriptService,
      @NonNull final WrestlerService wrestlerService,
      @NonNull final SegmentTypeService segmentTypeService,
      @NonNull final SegmentRuleService segmentRuleService,
      @NonNull final FeudBeatAssistantService feudBeatAssistantService,
      @NonNull final SecurityUtils securityUtils,
      @NonNull final ShowService showService,
      @NonNull final TitleService titleService) {
    this.feudScriptService = feudScriptService;
    this.wrestlerService = wrestlerService;
    this.segmentTypeService = segmentTypeService;
    this.segmentRuleService = segmentRuleService;
    this.feudBeatAssistantService = feudBeatAssistantService;
    this.securityUtils = securityUtils;
    this.showService = showService;
    this.titleService = titleService;

    setSizeFull();
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.SMALL);

    Button newArcButton = new Button("New Story Arc", e -> openWizard());
    newArcButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    newArcButton.setVisible(securityUtils.canCreate());

    content.setPadding(false);
    add(new H2("Story Arcs"), newArcButton, content);
    reload();
  }

  private void openWizard() {
    List<Wrestler> allWrestlers =
        wrestlerService.getAllWrestlers().stream()
            .sorted(Comparator.comparing(Wrestler::getName))
            .collect(Collectors.toList());
    FeudScriptWizardDialog dialog =
        new FeudScriptWizardDialog(
            allWrestlers,
            segmentTypeService.findAll().stream()
                .map(t -> t.getName())
                .sorted()
                .collect(Collectors.toList()),
            segmentRuleService.findAll().stream()
                .map(r -> r.getName())
                .sorted()
                .collect(Collectors.toList()),
            feudScriptService.getDefaultMaxPleAppearances(),
            feudScriptService,
            feudBeatAssistantService,
            showService.getUpcomingShows(20),
            titleService.getActiveTitles(),
            this::reload);
    dialog.open();
  }

  private void reload() {
    content.removeAll();
    List<FeudScript> scripts = feudScriptService.getAllScriptsWithBeats();
    if (scripts.isEmpty()) {
      content.add(new Span("No story arcs yet — create one from a rivalry or the button above."));
      return;
    }
    FeudScriptCard.EditorServices services =
        new FeudScriptCard.EditorServices(
            feudScriptService,
            segmentTypeService,
            segmentRuleService,
            feudBeatAssistantService,
            securityUtils,
            wrestlerService,
            showService,
            titleService);
    scripts.forEach(
        script ->
            content.add(
                new FeudScriptCard(
                    script, FeudScriptCard.participantsOf(script), services, this::reload)));
  }
}
