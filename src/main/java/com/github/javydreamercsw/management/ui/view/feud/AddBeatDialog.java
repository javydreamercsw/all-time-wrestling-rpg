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

import com.github.javydreamercsw.management.domain.feud.FeudScript;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeat;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.feud.FeudBeatAssistantService;
import com.github.javydreamercsw.management.service.feud.FeudScriptService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dialog for appending a beat to an existing story arc. The beat is saved through the same {@link
 * FeudScriptService#addBeat(FeudScript, FeudScriptBeat)} path the creation wizard uses, so PLE cap
 * validation and reservation handling apply identically.
 */
public class AddBeatDialog extends Dialog {

  private final FeudScriptService feudScriptService;
  private final FeudScript script;
  private final Runnable onComplete;

  private final BeatEditor beatEditor;

  public AddBeatDialog(
      FeudScript script,
      List<Wrestler> participants,
      List<String> segmentTypeNames,
      List<String> segmentRuleNames,
      FeudScriptService feudScriptService,
      Runnable onComplete) {
    this(
        script,
        participants,
        segmentTypeNames,
        segmentRuleNames,
        feudScriptService,
        List.of(),
        null,
        List.of(),
        List.of(),
        onComplete);
  }

  /**
   * Full dialog: {@code externalCandidates} (active roster minus the feud's wrestlers) enables
   * external opponent/extras selection; {@code opponentAssistant} enables the AI suggestion button
   * when non-null; {@code upcomingShows}/{@code activeTitles} power the target-show picker and the
   * title-stakes section.
   */
  public AddBeatDialog(
      FeudScript script,
      List<Wrestler> participants,
      List<String> segmentTypeNames,
      List<String> segmentRuleNames,
      FeudScriptService feudScriptService,
      List<Wrestler> externalCandidates,
      FeudBeatAssistantService opponentAssistant,
      List<Show> upcomingShows,
      List<Title> activeTitles,
      Runnable onComplete) {
    this.script = script;
    this.feudScriptService = feudScriptService;
    this.onComplete = onComplete;

    setHeaderTitle("Add Beat — " + script.getName());
    setWidth("min(900px, 90vw)");
    setCloseOnEsc(true);
    setCloseOnOutsideClick(false);

    beatEditor =
        new BeatEditor(
            new BeatEditor.BeatEditorContext(
                sortParticipants(participants),
                externalCandidates != null ? externalCandidates : List.of(),
                upcomingShows != null ? upcomingShows : List.of(),
                activeTitles != null ? activeTitles : List.of(),
                segmentTypeNames,
                segmentRuleNames,
                false,
                null,
                opponentAssistant));
    beatEditor.bindScriptContext(script, participants);

    Button saveBtn =
        new Button(
            "Add Beat",
            e -> {
              if (!beatEditor.isValid()) {
                beatEditor.showValidationError();
                return;
              }
              saveBeat();
            });
    saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    Button cancelBtn = new Button("Cancel", e -> close());
    cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    VerticalLayout body = new VerticalLayout(beatEditor);
    body.setPadding(false);
    body.setSpacing(true);

    getFooter().add(cancelBtn, saveBtn);
    add(body);
  }

  private List<Wrestler> sortParticipants(List<Wrestler> participants) {
    return participants.stream()
        .sorted(Comparator.comparing(Wrestler::getName))
        .collect(Collectors.toList());
  }

  private void saveBeat() {
    FeudScriptBeat beat = beatEditor.toBeat();
    try {
      feudScriptService.addBeat(script, beat);
    } catch (Exception ex) {
      Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.BOTTOM_END)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
      return;
    }
    Notification.show("Beat added to arc!", 3000, Notification.Position.BOTTOM_END)
        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    close();
    if (onComplete != null) {
      onComplete.run();
    }
  }

  /** Participant list helper for rivalry-driven arcs. */
  public static List<Wrestler> participantsOf(Rivalry rivalry) {
    List<Wrestler> participants = new ArrayList<>();
    if (rivalry != null) {
      participants.add(rivalry.getWrestler1());
      participants.add(rivalry.getWrestler2());
    }
    return participants;
  }
}
