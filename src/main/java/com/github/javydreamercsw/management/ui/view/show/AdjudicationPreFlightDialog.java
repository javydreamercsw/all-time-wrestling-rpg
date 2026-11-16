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

import com.github.javydreamercsw.management.service.show.ShowService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * Pre-flight dialog for show adjudication (ATW-v0vh): lists what will be narrated, winnerless
 * warnings, and — new — segments involving player-controlled wrestlers, which are excluded by
 * default. The user must explicitly choose Include to adjudicate them.
 *
 * <p>A custom {@link Dialog} rather than {@code ConfirmDialog}: the Exclude/Include choice needs a
 * radio group plus a conditionally disabled confirm button, which ConfirmDialog's fixed-height
 * content flex area makes fragile (see the ListBox overflow workaround this replaces).
 */
public class AdjudicationPreFlightDialog extends Dialog {

  /** Callbacks into the owning view. */
  public interface Callbacks {
    /** Confirmed — start adjudication with the chosen player-segment handling. */
    void onConfirm(boolean includePlayerSegments);

    /** Cancelled — re-enable the trigger button. */
    void onCancel();
  }

  private final Button confirmButton = new Button("Adjudicate");
  private final Div content = new Div();
  private final Callbacks callbacks;
  private boolean includePlayerSegments = false;

  public AdjudicationPreFlightDialog(
      ShowService.AdjudicationPreFlight preFlight, Callbacks callbacks) {
    this.callbacks = callbacks;
    setHeaderTitle("Adjudicate Show");
    setModal(true);
    setCloseOnEsc(false);
    setCloseOnOutsideClick(false);

    buildContent(preFlight);

    // Error-toned confirm preserves the old ConfirmDialog semantics for winnerless warnings.
    confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    if (!preFlight.missingWinnerWarnings().isEmpty()) {
      confirmButton.setText("Adjudicate Anyway");
      confirmButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
    }
    confirmButton.addClickListener(
        e -> {
          close();
          callbacks.onConfirm(includePlayerSegments);
        });
    Button cancelButton =
        new Button(
            "Go Back",
            e -> {
              close();
              callbacks.onCancel();
            });
    getFooter().add(cancelButton, confirmButton);
  }

  private void buildContent(ShowService.AdjudicationPreFlight preFlight) {
    // Plain Div flow (not VerticalLayout): a fixed-height dialog content slot makes
    // a VerticalLayout child collapse and its items overlap (same ConfirmDialog lesson).
    if (!preFlight.toNarrate().isEmpty() && preFlight.narrationProviderAvailable()) {
      Paragraph narrateIntro =
          new Paragraph(
              "AI narration will be generated for "
                  + preFlight.toNarrate().size()
                  + " segment(s) before adjudication:");
      narrateIntro.addClassNames(LumoUtility.TextColor.SECONDARY);
      content.add(narrateIntro);
      ListBox<String> narrateList = new ListBox<>();
      narrateList.setItems(preFlight.toNarrate());
      narrateList.setMaxHeight("220px");
      narrateList.getElement().getStyle().set("overflowY", "auto");
      content.add(narrateList);
    } else if (!preFlight.toNarrate().isEmpty()) {
      // No provider: say what actually happens instead of promising narration (ATW-ktta).
      Paragraph noProviderNote =
          new Paragraph(
              "No AI narration provider is configured. "
                  + preFlight.toNarrate().size()
                  + " segment(s) will be adjudicated with their booked descriptions.");
      noProviderNote.addClassNames(LumoUtility.TextColor.SECONDARY);
      content.add(noProviderNote);
    }
    if (!preFlight.missingWinnerWarnings().isEmpty()) {
      Paragraph warningIntro =
          new Paragraph("These segments have no winner selected — they will be recorded as draws:");
      warningIntro.addClassNames(LumoUtility.TextColor.ERROR);
      content.add(warningIntro);
      ListBox<String> warningList = new ListBox<>();
      warningList.setItems(preFlight.missingWinnerWarnings());
      warningList.setMaxHeight("220px");
      warningList.getElement().getStyle().set("overflowY", "auto");
      content.add(warningList);
    }
    if (!preFlight.playerSegments().isEmpty()) {
      Paragraph playerIntro =
          new Paragraph(
              preFlight.playerSegments().size()
                  + " segment(s) involve player-controlled wrestlers. Exclude them by default — "
                  + "their outcomes are the player's story to tell:");
      playerIntro.addClassNames(LumoUtility.TextColor.BODY);
      content.add(playerIntro);
      ListBox<String> playerList = new ListBox<>();
      playerList.setItems(preFlight.playerSegments());
      playerList.setMaxHeight("160px");
      playerList.getElement().getStyle().set("overflowY", "auto");
      content.add(playerList);

      RadioButtonGroup<String> includeChoice = new RadioButtonGroup<>();
      includeChoice.setItems("Exclude (default)", "Include (their outcomes count)");
      includeChoice.setValue("Exclude (default)");
      Div nothingLeftWarning = new Div();
      nothingLeftWarning.setVisible(false);
      boolean nothingLeftWithoutPlayers =
          preFlight.pendingSegmentCount() == preFlight.playerSegments().size();
      includeChoice.addValueChangeListener(
          e -> {
            includePlayerSegments = "Include (their outcomes count)".equals(e.getValue());
            if (includePlayerSegments || !nothingLeftWithoutPlayers) {
              nothingLeftWarning.setVisible(false);
              confirmButton.setEnabled(true);
            } else {
              nothingLeftWarning.setText(
                  "Nothing to adjudicate — every pending segment involves player wrestlers.");
              nothingLeftWarning.addClassNames(LumoUtility.TextColor.ERROR);
              nothingLeftWarning.setVisible(true);
              confirmButton.setEnabled(false);
            }
          });
      content.add(includeChoice, nothingLeftWarning);
    }
    add(content);
  }
}
