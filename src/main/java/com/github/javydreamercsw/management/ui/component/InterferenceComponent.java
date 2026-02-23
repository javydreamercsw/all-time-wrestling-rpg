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

import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.interference.InterferenceService;
import com.github.javydreamercsw.management.service.interference.InterferenceType;
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
import java.util.function.Consumer;

public class InterferenceComponent extends VerticalLayout {

  private final InterferenceService interferenceService;
  private final Segment segment;
  private final Wrestler playerWrestler;
  private final Consumer<InterferenceService.InterferenceResult> onInterference;

  private ProgressBar awarenessBar;
  private Span awarenessLabel;
  private VerticalLayout actionsLayout;

  public InterferenceComponent(
      InterferenceService interferenceService,
      Segment segment,
      Wrestler playerWrestler,
      Consumer<InterferenceService.InterferenceResult> onInterference) {
    this.interferenceService = interferenceService;
    this.segment = segment;
    this.playerWrestler = playerWrestler;
    this.onInterference = onInterference;

    setPadding(true);
    setSpacing(true);
    setWidthFull();
    addClassName("interference-component");

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

    Span actionsTitle = new Span("Ringside Interference");
    actionsTitle.addClassNames(FontSize.XSMALL, FontWeight.MEDIUM, Margin.Top.SMALL);
    actionsLayout.add(actionsTitle);

    HorizontalLayout buttons = new HorizontalLayout();
    buttons.setSpacing(true);

    for (InterferenceType type : InterferenceType.values()) {
      Button btn = new Button(type.getDisplayName(), e -> performInterference(type));
      btn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
      btn.setTooltipText("Impact: +" + type.getBaseImpact() + ", Risk: " + type.getBaseRisk());
      buttons.add(btn);
    }

    actionsLayout.add(buttons);
    add(actionsLayout);

    // Only show actions if player is in the match or is a booker
    // For now, let's assume if this component is shown, the user has permission to interfere
  }

  private void performInterference(InterferenceType type) {
    // Determine interferer: for now, we'll just say "Faction Member" or use the manager if
    // available
    // In a real scenario, we'd pick a specific character at ringside
    InterferenceService.InterferenceResult result =
        interferenceService.attemptInterference(segment, playerWrestler, playerWrestler, type);

    updateUI();

    Notification n = Notification.show(result.message());
    if (result.disqualified()) {
      n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    } else if (result.ejected()) {
      n.addThemeVariants(NotificationVariant.LUMO_WARNING);
    } else if (result.success()) {
      n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    if (onInterference != null) {
      onInterference.accept(result);
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
