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
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.theme.lumo.LumoUtility.FontSize;
import com.vaadin.flow.theme.lumo.LumoUtility.FontWeight;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import com.vaadin.flow.theme.lumo.LumoUtility.TextColor;
import java.util.List;
import java.util.function.BiConsumer;

public class RingsideActionComponent extends VerticalLayout {

  private final RingsideActionService ringsideActionService;
  private final RingsideActionDataService ringsideActionDataService;
  private final Segment segment;
  private final Wrestler playerWrestler;
  private final BiConsumer<RingsideAction, RingsideActionService.RingsideActionResult>
      onActionPerformed;

  private ProgressBar awarenessBar;
  private Span awarenessLabel;
  private VerticalLayout actionsLayout;
  private Select<Wrestler> targetWrestlerSelect;

  public RingsideActionComponent(
      RingsideActionService ringsideActionService,
      RingsideActionDataService ringsideActionDataService,
      Segment segment,
      Wrestler playerWrestler,
      BiConsumer<RingsideAction, RingsideActionService.RingsideActionResult> onActionPerformed) {
    this.ringsideActionService = ringsideActionService;
    this.ringsideActionDataService = ringsideActionDataService;
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

    if (playerWrestler == null) {
      // Admin/Booker mode - show target selector
      targetWrestlerSelect = new Select<>();
      targetWrestlerSelect.setLabel("Wrestler to Assist");
      List<Wrestler> supportedWrestlers =
          segment.getWrestlers().stream()
              .filter(w -> ringsideActionService.hasSupportAtRingside(segment, w))
              .toList();
      targetWrestlerSelect.setItems(supportedWrestlers);
      targetWrestlerSelect.setItemLabelGenerator(Wrestler::getName);
      targetWrestlerSelect.addValueChangeListener(e -> refreshActionButtons());
      if (!supportedWrestlers.isEmpty()) {
        targetWrestlerSelect.setValue(supportedWrestlers.get(0));
      }
      actionsLayout.add(targetWrestlerSelect);
    }

    refreshActionButtons();
    add(actionsLayout);
  }

  private void refreshActionButtons() {
    // Clear previous buttons but keep selector if it exists
    actionsLayout
        .getChildren()
        .filter(
            c ->
                c instanceof HorizontalLayout
                    || (c instanceof Span && !c.equals(targetWrestlerSelect)))
        .forEach(actionsLayout::remove);

    Wrestler activeTarget =
        playerWrestler != null ? playerWrestler : targetWrestlerSelect.getValue();

    if (activeTarget != null && ringsideActionService.hasSupportAtRingside(segment, activeTarget)) {
      Span actionsTitle = new Span("Take Action for " + activeTarget.getName());
      actionsTitle.addClassNames(FontSize.XSMALL, FontWeight.MEDIUM, Margin.Top.SMALL);
      actionsLayout.add(actionsTitle);

      HorizontalLayout buttons = new HorizontalLayout();
      buttons.setSpacing(true);

      List<RingsideAction> actions = ringsideActionDataService.findAllActions();

      // Filter by alignment: Heels can do anything, Faces/Neutral prefer non-Heel actions
      AlignmentType targetAlignment = getWrestlerAlignment(activeTarget);
      List<RingsideAction> suitableActions =
          actions.stream()
              .filter(
                  a -> {
                    if (targetAlignment == AlignmentType.HEEL) return true;
                    return a.getAlignment() != AlignmentType.HEEL;
                  })
              .toList();

      for (RingsideAction action : suitableActions) {
        Button btn = new Button(action.getName(), e -> performAction(activeTarget, action));
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
    } else if (playerWrestler != null) {
      Span noSupport = new Span("No manager or faction members at ringside to assist.");
      noSupport.addClassNames(FontSize.XSMALL, TextColor.SECONDARY, Margin.Top.SMALL);
      actionsLayout.add(noSupport);
    } else if (targetWrestlerSelect != null && targetWrestlerSelect.isEmpty()) {
      Span noSupport = new Span("No participants have ringside support available.");
      noSupport.addClassNames(FontSize.XSMALL, TextColor.SECONDARY, Margin.Top.SMALL);
      actionsLayout.add(noSupport);
    }
  }

  private AlignmentType getWrestlerAlignment(Wrestler wrestler) {
    if (wrestler != null && wrestler.getAlignment() != null) {
      return wrestler.getAlignment().getAlignmentType();
    }
    return AlignmentType.NEUTRAL;
  }

  private void performAction(Wrestler target, RingsideAction action) {
    Object supporter = ringsideActionService.getBestSupporter(segment, target);
    if (supporter == null) {
      Notification.show("No one is currently available at ringside to help " + target.getName())
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
      return;
    }

    RingsideActionService.RingsideActionResult result =
        ringsideActionService.performAction(segment, supporter, target, action);

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
