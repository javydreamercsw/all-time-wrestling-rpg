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
package com.github.javydreamercsw.management.ui.view.rivalry;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.feud.FeudScriptService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.feud.FeudScriptWizardDialog;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;

@Route("rivalry")
@PageTitle("Rivalry")
@PermitAll
public class RivalryDetailView extends Main implements HasUrlParameter<Long> {

  private final RivalryService rivalryService;
  private final WrestlerService wrestlerService;
  private final FeudScriptService feudScriptService;
  private final SegmentTypeService segmentTypeService;
  private final SegmentRuleService segmentRuleService;
  private final SecurityUtils securityUtils;

  private final VerticalLayout content = new VerticalLayout();

  public RivalryDetailView(
      @NonNull final RivalryService rivalryService,
      @NonNull final WrestlerService wrestlerService,
      @NonNull final FeudScriptService feudScriptService,
      @NonNull final SegmentTypeService segmentTypeService,
      @NonNull final SegmentRuleService segmentRuleService,
      @NonNull final SecurityUtils securityUtils) {
    this.rivalryService = rivalryService;
    this.wrestlerService = wrestlerService;
    this.feudScriptService = feudScriptService;
    this.segmentTypeService = segmentTypeService;
    this.segmentRuleService = segmentRuleService;
    this.securityUtils = securityUtils;

    setSizeFull();
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.SMALL);

    content.setPadding(false);
    add(content);
  }

  @Override
  public void setParameter(BeforeEvent event, Long rivalryId) {
    content.removeAll();
    rivalryService
        .getRivalryByIdWithWrestlers(rivalryId)
        .ifPresentOrElse(this::buildView, () -> showNotFound(rivalryId));
  }

  private void buildView(Rivalry rivalry) {
    String wrestler1Name = rivalry.getWrestler1().getName();
    String wrestler2Name = rivalry.getWrestler2().getName();

    H2 title = new H2(wrestler1Name + " vs " + wrestler2Name);

    Span heatBadge = new Span("Heat: " + rivalry.getHeat());
    heatBadge.addClassNames(
        LumoUtility.Background.ERROR_10,
        LumoUtility.BorderRadius.LARGE,
        LumoUtility.Padding.Horizontal.SMALL);

    VerticalLayout info = new VerticalLayout(title, heatBadge);
    info.setPadding(false);
    info.setSpacing(false);

    if (rivalry.getStorylineNotes() != null && !rivalry.getStorylineNotes().isBlank()) {
      info.add(new Paragraph("Notes: " + rivalry.getStorylineNotes()));
    }
    if (rivalry.getStartedDate() != null) {
      info.add(new Span("Started: " + rivalry.getStartedDate()));
    }
    if (rivalry.getEndedDate() != null) {
      info.add(new Span("Ended: " + rivalry.getEndedDate()));
    }

    Button backButton = new Button("← Back to Rivalry List");
    backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    backButton.addClickListener(e -> UI.getCurrent().navigate(RivalryListView.class));

    HorizontalLayout actions = new HorizontalLayout(backButton);
    actions.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);

    if (securityUtils.canCreate()) {
      Button storyArcButton = new Button("Story Arc");
      storyArcButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      storyArcButton.addClickListener(e -> openWizard(rivalry));
      actions.add(storyArcButton);
    }

    content.add(info, actions);
  }

  private void openWizard(Rivalry rivalry) {
    List<Wrestler> allWrestlers =
        wrestlerService.getAllWrestlers().stream()
            .sorted(Comparator.comparing(Wrestler::getName))
            .collect(Collectors.toList());

    List<String> typeNames =
        segmentTypeService.findAll().stream()
            .map(SegmentType::getName)
            .sorted()
            .collect(Collectors.toList());

    List<String> ruleNames =
        segmentRuleService.findAll().stream()
            .map(SegmentRule::getName)
            .sorted()
            .collect(Collectors.toList());

    List<Wrestler> participants =
        List.of(
            wrestlerService.findById(rivalry.getWrestler1().getId()).orElseThrow(),
            wrestlerService.findById(rivalry.getWrestler2().getId()).orElseThrow());

    FeudScriptWizardDialog dialog =
        new FeudScriptWizardDialog(
            allWrestlers,
            typeNames,
            ruleNames,
            feudScriptService.getDefaultMaxPleAppearances(),
            feudScriptService,
            null);
    dialog.preSelectWrestlers(participants);
    dialog.open();
  }

  private void showNotFound(Long id) {
    Notification.show("Rivalry #" + id + " not found", 3000, Notification.Position.MIDDLE)
        .addThemeVariants(NotificationVariant.LUMO_ERROR);
    UI.getCurrent().navigate(RivalryListView.class);
  }
}
