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

import com.github.javydreamercsw.management.event.ShowAdjudicationProgressEvent;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * Floating non-blocking adjudication progress indicator (ATW-v0vh): a fixed-position bottom-right
 * card showing determinate per-segment progress while narration/adjudication runs, and the
 * partial-failure outcome (with a Retry failed action) when it finishes. A plain positioned Div,
 * not a Dialog — the user must be able to keep working while the run proceeds server-side.
 *
 * <p>The owning view keeps one instance, wires it to {@code AdjudicationProgressBroadcaster} events
 * while attached, and calls the {@code on*} methods from {@code ui.access()} blocks only. Click
 * listeners are registered once in the constructor and route through swappable action fields, so
 * repeated runs never stack listeners.
 */
public class AdjudicationProgressOverlay extends Div {
  private final VerticalLayout content = new VerticalLayout();
  private final Span titleLabel = new Span();
  private final ProgressBar progressBar = new ProgressBar();
  private final Span detailLabel = new Span();
  private final HorizontalLayout actions = new HorizontalLayout();
  private final Button retryButton = new Button("Retry failed", new Icon(VaadinIcon.REFRESH));
  private final Button dismissButton = new Button("Dismiss");
  private final Button hideButton = new Button(new Icon(VaadinIcon.CLOSE_SMALL));

  private Runnable retryAction;
  private Runnable dismissAction;

  public AdjudicationProgressOverlay() {
    addClassNames(
        LumoUtility.Background.BASE,
        LumoUtility.Border.ALL,
        LumoUtility.BorderColor.CONTRAST_20,
        LumoUtility.BorderRadius.MEDIUM,
        LumoUtility.BoxShadow.LARGE,
        LumoUtility.Padding.MEDIUM);
    getStyle()
        .setPosition(Style.Position.FIXED)
        .setBottom("1rem")
        .setRight("1rem")
        .setZIndex(500)
        .setMinWidth("320px")
        .setMaxWidth("420px");
    setVisible(false);

    titleLabel.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.BOLD);
    progressBar.setWidthFull();
    detailLabel.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.SECONDARY);

    hideButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
    hideButton.addClickListener(e -> hide());

    HorizontalLayout header = new HorizontalLayout(titleLabel, hideButton);
    header.setWidthFull();
    header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
    header.setAlignItems(FlexComponent.Alignment.CENTER);
    header.setSpacing(false);

    actions.setSpacing(true);
    retryButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
    dismissButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
    actions.add(retryButton, dismissButton);
    actions.setVisible(false);

    content.setSpacing(true);
    content.setPadding(false);
    content.add(header, progressBar, detailLabel, actions);
    add(content);

    retryButton.addClickListener(
        e -> {
          if (retryAction != null && retryButton.isEnabled()) {
            retryButton.setEnabled(false);
            retryAction.run();
          }
        });
    dismissButton.addClickListener(
        e -> {
          if (dismissAction != null) {
            dismissAction.run();
          }
        });
  }

  /** Running state: determinate progress for the given phase counts. */
  public void onProgress(ShowAdjudicationProgressEvent event) {
    titleLabel.setText(titleFor(event));
    if (event.getTotal() > 0) {
      progressBar.setIndeterminate(false);
      progressBar.setValue(Math.min(1.0, event.getCompleted() / (double) event.getTotal()));
    } else {
      progressBar.setIndeterminate(true);
    }
    detailLabel.setText(
        event.getCompleted() + " of " + event.getTotal() + " — " + event.getCurrentLabel());
    actions.setVisible(false);
    retryButton.setVisible(true);
    titleLabel.removeClassNames(LumoUtility.TextColor.ERROR);
    show();
  }

  /**
   * Terminal state for a finished run. With failures the overlay stays with a Retry action; without
   * them it shows a brief completion message then closes.
   */
  public void onCompleted(
      ShowService.AdjudicationResult result, Runnable retryAction, Runnable dismissAction) {
    this.retryAction = retryAction;
    this.dismissAction = dismissAction;
    retryButton.setEnabled(true);
    retryButton.setVisible(!result.failedSegmentIds().isEmpty());
    if (result.failedSegmentIds().isEmpty()) {
      titleLabel.setText("Adjudication complete");
      titleLabel.removeClassNames(LumoUtility.TextColor.ERROR);
      progressBar.setIndeterminate(false);
      progressBar.setValue(1.0);
      detailLabel.setText(
          result.narratedCount()
              + " narration(s), "
              + result.adjudicatedCount()
              + " segment(s) adjudicated");
      actions.setVisible(false);
      show();
      return;
    }
    titleLabel.setText(
        "Narration finished: "
            + result.narratedCount()
            + " succeeded — "
            + result.failedSegmentIds().size()
            + " failed");
    titleLabel.addClassNames(LumoUtility.TextColor.ERROR);
    progressBar.setIndeterminate(false);
    progressBar.setValue(1.0);
    detailLabel.setText("Failed segments kept their booked descriptions.");
    actions.setVisible(true);
    show();
  }

  /** Terminal failure of the whole run (exception from the async call). */
  public void onRunFailed(String message, Runnable dismissAction) {
    this.dismissAction = dismissAction;
    titleLabel.setText("Adjudication failed");
    titleLabel.addClassNames(LumoUtility.TextColor.ERROR);
    progressBar.setIndeterminate(false);
    progressBar.setValue(0.0);
    detailLabel.setText(message != null ? message : "Unexpected error");
    retryButton.setVisible(false);
    actions.setVisible(true);
    show();
  }

  public void hide() {
    setVisible(false);
  }

  /** Shows the overlay; idempotent. */
  public void show() {
    setVisible(true);
  }

  private String titleFor(ShowAdjudicationProgressEvent event) {
    return switch (event.getPhase()) {
      case NARRATION -> "Narrating…";
      case ADJUDICATION -> "Adjudicating…";
      case FINALIZING -> "Finalizing…";
      case RETRY -> "Retrying narration…";
    };
  }
}
