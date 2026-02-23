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

import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.show.segment.RingsideAction;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.ringside.RingsideActionDataService;
import com.github.javydreamercsw.management.service.ringside.RingsideActionService;
import com.github.javydreamercsw.management.service.team.TeamService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.theme.lumo.LumoUtility.FontSize;
import com.vaadin.flow.theme.lumo.LumoUtility.FontWeight;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import com.vaadin.flow.theme.lumo.LumoUtility.TextColor;
import java.util.List;
import java.util.function.BiConsumer;

public class RingsideActionComponent extends VerticalLayout {

  private final RingsideActionService ringsideActionService;
  private final RingsideActionDataService ringsideActionDataService;
  private final TeamService teamService;
  private final Segment segment;
  private final Wrestler playerWrestler;
  private final BiConsumer<RingsideAction, RingsideActionService.RingsideActionResult>
      onActionPerformed;

  private ProgressBar awarenessBar;
  private Span awarenessLabel;
  private VerticalLayout actionsLayout;

  public RingsideActionComponent(
      RingsideActionService ringsideActionService,
      RingsideActionDataService ringsideActionDataService,
      TeamService teamService,
      Segment segment,
      Wrestler playerWrestler,
      BiConsumer<RingsideAction, RingsideActionService.RingsideActionResult> onActionPerformed) {
    this.ringsideActionService = ringsideActionService;
    this.ringsideActionDataService = ringsideActionDataService;
    this.teamService = teamService;
    this.segment = segment;
    this.playerWrestler = playerWrestler;
    this.onActionPerformed = onActionPerformed;

    setPadding(true);
    setSpacing(true);
    setWidthFull();
    addClassName("ringside-action-component");

    buildUI();
  }

  private void buildUI() {
    // Awareness Meter
    VerticalLayout meterLayout = new VerticalLayout();
    meterLayout.setPadding(false);
    meterLayout.setSpacing(false);

    HorizontalLayout labelLayout = new HorizontalLayout();
    labelLayout.setWidthFull();
    labelLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);

    String refName = segment.getReferee() != null ? segment.getReferee().getName() : "Referee";
    Span title = new Span(refName + " Awareness");
    title.addClassNames(FontSize.SMALL, FontWeight.BOLD);

    awarenessLabel = new Span(segment.getRefereeAwarenessLevel() + "%");
    awarenessLabel.addClassNames(FontSize.SMALL);

    labelLayout.add(title, awarenessLabel);

    awarenessBar = new ProgressBar(0, 100, segment.getRefereeAwarenessLevel());
    updateProgressBarColor();

    meterLayout.add(labelLayout, awarenessBar);
    add(meterLayout);

    // Actions
    actionsLayout = new VerticalLayout();
    actionsLayout.setPadding(false);
    actionsLayout.setSpacing(true);

    if (hasSupportAtRingside()) {
      Span actionsTitle = new Span("Take Action");
      actionsTitle.addClassNames(FontSize.XSMALL, FontWeight.MEDIUM, Margin.Top.SMALL);
      actionsLayout.add(actionsTitle);

      HorizontalLayout buttons = new HorizontalLayout();
      buttons.setSpacing(true);

      List<RingsideAction> actions = ringsideActionDataService.findAllActions();
      for (RingsideAction action : actions) {
        Button btn = new Button(action.getName(), e -> performAction(action));
        btn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        btn.setTooltipText(action.getDescription());

        // Add color coding for alignment
        if (action.getAlignment() == AlignmentType.HEEL) {
          btn.addClassNames(TextColor.ERROR);
        } else if (action.getAlignment() == AlignmentType.FACE) {
          btn.addClassNames(TextColor.SUCCESS);
        }

        buttons.add(btn);
      }

      actionsLayout.add(buttons);
    } else {
      Span noSupport = new Span("No manager or faction members at ringside to assist.");
      noSupport.addClassNames(FontSize.XSMALL, TextColor.SECONDARY, Margin.Top.SMALL);
      actionsLayout.add(noSupport);
    }

    add(actionsLayout);
  }

  private boolean hasSupportAtRingside() {
    if (playerWrestler == null) return false;

    // Check for manager
    if (playerWrestler.getManager() != null) return true;

    // Check for faction members not in the match
    if (playerWrestler.getFaction() != null) {
      boolean otherFactionMembersExist =
          playerWrestler.getFaction().getMembers().stream()
              .anyMatch(m -> !segment.getWrestlers().contains(m));
      if (otherFactionMembersExist) return true;
    }

    // Check for team members not in the match
    boolean otherTeamMembersExist =
        teamService.getActiveTeamsByWrestler(playerWrestler).stream()
            .anyMatch(t -> !segment.getWrestlers().contains(t.getPartner(playerWrestler)));
    if (otherTeamMembersExist) return true;

    return false;
  }

  private void performAction(RingsideAction action) {
    RingsideActionService.RingsideActionResult result =
        ringsideActionService.performAction(segment, playerWrestler, playerWrestler, action);

    updateUI();

    Notification n = Notification.show(result.message());
    if (result.disqualified()) {
      n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    } else if (result.ejected()) {
      n.addThemeVariants(NotificationVariant.LUMO_WARNING);
    } else if (result.success()) {
      n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    if (onActionPerformed != null) {
      onActionPerformed.accept(action, result);
    }
  }

  public void updateUI() {
    int level = segment.getRefereeAwarenessLevel();
    awarenessBar.setValue(level);
    awarenessLabel.setText(level + "%");
    updateProgressBarColor();

    if (level >= 100) {
      actionsLayout.setEnabled(false);
    }
  }

  private void updateProgressBarColor() {
    int level = segment.getRefereeAwarenessLevel();
    String color = "var(--lumo-success-color)";
    if (level >= 80) {
      color = "var(--lumo-error-color)";
    } else if (level >= 50) {
      color = "var(--lumo-warning-color)";
    }
    awarenessBar.getElement().getStyle().set("--lumo-primary-color", color);
  }
}
