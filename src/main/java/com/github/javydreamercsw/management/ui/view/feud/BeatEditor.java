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

import com.github.javydreamercsw.base.security.GeneralSecurityUtils;
import com.github.javydreamercsw.management.domain.feud.FeudBeatParticipantRole;
import com.github.javydreamercsw.management.domain.feud.FeudScript;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeat;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeatStatus;
import com.github.javydreamercsw.management.domain.feud.FeudScriptWinnerControl;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.feud.AiSuggestedOpponentDTO;
import com.github.javydreamercsw.management.service.feud.FeudBeatAssistantService;
import com.github.javydreamercsw.management.ui.component.TeamRowsEditor;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextArea;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Editor for a single {@link FeudScriptBeat}: match type, stipulation, winner control, culmination
 * flag, story notes, target show, title stakes (title match / #1 contender), external participants
 * and an optional custom team layout. Shared by the arc creation wizard and the add/edit beat
 * dialogs so all entry points stay in sync.
 *
 * <p>Besides the feud's own wrestlers, a beat can involve external participants — an optional
 * surprise opponent and run-in extras, placed on the opposing team at planning time — or, with a
 * custom team layout, any arrangement of arc wrestlers and externals (tag matches, feud partners
 * facing each other, stables vs stables). When an AI assistant is bound, an "AI Suggest Opponent"
 * button fills the opponent field from the eligible roster; the booker confirms or overrides it.
 */
public class BeatEditor extends VerticalLayout {

  private static final String BOOKER_PICKS = "Booker Picks";
  private static final String AI_PICKS = "AI Picks";
  private static final String SYSTEM_ROLL = "System Roll";
  private static final DateTimeFormatter SHOW_DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

  /** Reference data + wiring for a beat editor, shared by wizard and add/edit dialogs. */
  public record BeatEditorContext(
      List<Wrestler> participants,
      List<Wrestler> externalCandidates,
      List<Show> upcomingShows,
      List<Title> activeTitles,
      List<String> segmentTypeNames,
      List<String> segmentRuleNames,
      boolean removable,
      Consumer<BeatEditor> removeHandler,
      FeudBeatAssistantService assistant) {}

  private final BeatEditorContext context;

  private final ComboBox<String> segmentTypeCombo;
  private final ComboBox<String> segmentRuleCombo;
  private final RadioButtonGroup<String> winnerControlRadio;
  private final ComboBox<Wrestler> plannedWinnerCombo;
  private final Checkbox culminationCheck;
  private final TextArea notesField;

  // External quick-path (mutually exclusive with the custom team layout)
  private final ComboBox<Wrestler> opponentCombo;
  private final MultiSelectComboBox<Wrestler> extrasMulti;
  private final HorizontalLayout externalRow;
  private final Span rationaleLabel;
  private final Button suggestOpponentButton;

  // Custom team layout
  private final Checkbox customTeamsCheck;
  private final VerticalLayout teamRowsArea;
  private TeamRowsEditor teamRows;

  // Title stakes
  private final Checkbox titleMatchCheck;
  private final MultiSelectComboBox<Title> titlesMulti;
  private final Checkbox contenderMatchCheck;
  private final ComboBox<Title> contenderTitleCombo;

  // Target show
  private final ComboBox<Show> targetShowCombo;
  private final Span pleCapHint;

  private FeudBeatAssistantService opponentAssistant;
  private FeudScript script;
  private List<Wrestler> feudParticipants = List.of();

  /**
   * Legacy constructor without external candidates, title stakes, target shows or an AI assistant.
   *
   * @deprecated use {@link #BeatEditor(BeatEditorContext)}
   */
  @Deprecated
  public BeatEditor(
      List<Wrestler> participants,
      List<String> segmentTypeNames,
      List<String> segmentRuleNames,
      boolean removable,
      Consumer<BeatEditor> removeHandler) {
    this(
        new BeatEditorContext(
            participants,
            List.of(),
            List.of(),
            List.of(),
            segmentTypeNames,
            segmentRuleNames,
            removable,
            removeHandler,
            null));
  }

  /**
   * Legacy constructor with external candidates and the AI assistant; no title stakes or target
   * shows.
   *
   * @deprecated use {@link #BeatEditor(BeatEditorContext)}
   */
  @Deprecated
  public BeatEditor(
      List<Wrestler> participants,
      List<String> segmentTypeNames,
      List<String> segmentRuleNames,
      List<Wrestler> externalCandidates,
      boolean removable,
      Consumer<BeatEditor> removeHandler,
      FeudBeatAssistantService opponentAssistant) {
    this(
        new BeatEditorContext(
            participants,
            externalCandidates,
            List.of(),
            List.of(),
            segmentTypeNames,
            segmentRuleNames,
            removable,
            removeHandler,
            opponentAssistant));
  }

  /** Full editor fed from a {@link BeatEditorContext} built by the owning dialog. */
  public BeatEditor(BeatEditorContext context) {
    this.context = context;
    List<Wrestler> participants = context.participants();
    List<Wrestler> externalCandidates = context.externalCandidates();
    this.opponentAssistant = context.assistant();

    segmentTypeCombo = new ComboBox<>("Match Type");
    segmentTypeCombo.setItems(context.segmentTypeNames());
    segmentTypeCombo.setRequired(true);
    segmentTypeCombo.setWidth("250px");

    segmentRuleCombo = new ComboBox<>("Stipulation");
    segmentRuleCombo.setItems(context.segmentRuleNames());
    segmentRuleCombo.setPlaceholder("None");
    segmentRuleCombo.setWidth("220px");
    segmentRuleCombo.setClearButtonVisible(true);

    winnerControlRadio = new RadioButtonGroup<>("Winner");
    winnerControlRadio.setItems(BOOKER_PICKS, AI_PICKS, SYSTEM_ROLL);
    winnerControlRadio.setValue(AI_PICKS);

    plannedWinnerCombo = new ComboBox<>("Planned Winner");
    plannedWinnerCombo.setItems(participants);
    plannedWinnerCombo.setItemLabelGenerator(Wrestler::getName);
    plannedWinnerCombo.setVisible(false);
    plannedWinnerCombo.setWidth("180px");

    winnerControlRadio.addValueChangeListener(
        e -> plannedWinnerCombo.setVisible(BOOKER_PICKS.equals(e.getValue())));

    culminationCheck = new Checkbox("Culmination / Blowoff");
    notesField = new TextArea("Story Notes");
    notesField.setPlaceholder("Context for the AI narrator…");
    notesField.setWidthFull();
    notesField.setMaxHeight("120px");

    // External participants (wrestlers outside the feud). They land on the opposing team.
    List<Wrestler> candidates =
        externalCandidates != null
            ? externalCandidates.stream()
                .filter(w -> participants.stream().noneMatch(p -> p.getId().equals(w.getId())))
                .toList()
            : List.of();
    opponentCombo = new ComboBox<>("External Opponent");
    opponentCombo.setItems(candidates);
    opponentCombo.setItemLabelGenerator(Wrestler::getName);
    opponentCombo.setPlaceholder("None");
    opponentCombo.setClearButtonVisible(true);
    opponentCombo.setWidth("200px");
    opponentCombo.setVisible(!candidates.isEmpty());

    extrasMulti = new MultiSelectComboBox<>("External Extras");
    extrasMulti.setItems(candidates);
    extrasMulti.setItemLabelGenerator(Wrestler::getName);
    extrasMulti.setPlaceholder("None");
    extrasMulti.setWidth("220px");
    extrasMulti.setVisible(!candidates.isEmpty());

    // Keep the two external widgets disjoint.
    opponentCombo.addValueChangeListener(
        e -> {
          Wrestler opponent = e.getValue();
          if (opponent != null && extrasMulti.getValue().contains(opponent)) {
            var remaining = new HashSet<>(extrasMulti.getValue());
            remaining.remove(opponent);
            extrasMulti.setValue(remaining);
          }
        });

    rationaleLabel = new Span();
    rationaleLabel.setVisible(false);
    rationaleLabel.getStyle().set("color", "var(--lumo-secondary-text-color)");
    rationaleLabel.getStyle().set("font-style", "italic");

    suggestOpponentButton = new Button("✨ AI Suggest Opponent");
    suggestOpponentButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    suggestOpponentButton.setVisible(opponentAssistant != null && !candidates.isEmpty());
    suggestOpponentButton.addClickListener(e -> suggestOpponent());

    externalRow = new HorizontalLayout(suggestOpponentButton, opponentCombo, extrasMulti);
    externalRow.setAlignItems(FlexComponent.Alignment.END);
    externalRow.setWidthFull();
    externalRow.setVisible(!candidates.isEmpty());

    // Custom team layout: any arrangement of arc wrestlers and externals.
    customTeamsCheck = new Checkbox("Custom team layout");
    customTeamsCheck.setHelperText(
        "Split the arc's wrestlers and externals across teams yourself (tag matches, partners "
            + "facing each other). Off: arc wrestlers vs the externals below.");
    teamRowsArea = new VerticalLayout();
    teamRowsArea.setPadding(false);
    teamRowsArea.setSpacing(false);
    teamRowsArea.setWidthFull();
    teamRowsArea.setVisible(false);
    customTeamsCheck.addValueChangeListener(
        e -> {
          boolean custom = Boolean.TRUE.equals(e.getValue());
          teamRowsArea.setVisible(custom);
          externalRow.setVisible(!custom && !candidates.isEmpty());
          if (custom) {
            if (teamRows == null) {
              teamRows =
                  new TeamRowsEditor(participantsAndCandidates(participants, candidates), () -> {});
              teamRowsArea.add(teamRows);
            }
            // Pre-seed team 1 with the arc's own wrestlers; the booker arranges from there.
            if (teamRows.getTeams().isEmpty()) {
              teamRows.addTeamRow(participants);
              teamRows.addTeamRow();
            }
            opponentCombo.clear();
            extrasMulti.clear();
          }
        });

    // Title stakes: title match and/or #1 contender designation.
    titleMatchCheck = new Checkbox("Title on the line");
    titlesMulti = new MultiSelectComboBox<>("Titles");
    titlesMulti.setItems(context.activeTitles() != null ? context.activeTitles() : List.of());
    titlesMulti.setItemLabelGenerator(BeatEditor::titleWithHolders);
    titlesMulti.setPlaceholder("Select title(s)…");
    titlesMulti.setClearButtonVisible(true);
    titlesMulti.setWidthFull();
    titlesMulti.setVisible(false);
    titleMatchCheck.addValueChangeListener(
        e -> {
          titlesMulti.setVisible(Boolean.TRUE.equals(e.getValue()));
          if (!Boolean.TRUE.equals(e.getValue())) {
            titlesMulti.clear();
          }
        });

    contenderMatchCheck = new Checkbox("Winner becomes #1 contender for…");
    contenderTitleCombo = new ComboBox<>("Contender Title");
    contenderTitleCombo.setItems(
        context.activeTitles() != null ? context.activeTitles() : List.of());
    contenderTitleCombo.setItemLabelGenerator(BeatEditor::titleWithHolders);
    contenderTitleCombo.setPlaceholder("None");
    contenderTitleCombo.setClearButtonVisible(true);
    contenderTitleCombo.setWidthFull();
    contenderTitleCombo.setVisible(false);
    contenderMatchCheck.addValueChangeListener(
        e -> {
          contenderTitleCombo.setVisible(Boolean.TRUE.equals(e.getValue()));
          if (!Boolean.TRUE.equals(e.getValue())) {
            contenderTitleCombo.clear();
          }
        });
    contenderTitleCombo.addValueChangeListener(
        e -> {
          Title picked = e.getValue();
          if (picked != null
              && titleMatchCheck.getValue()
              && titlesMulti.getValue().contains(picked)) {
            // Same title cannot be both defended and a contender slot in one beat.
            var remaining = new HashSet<>(titlesMulti.getValue());
            remaining.remove(picked);
            titlesMulti.setValue(remaining);
          }
        });

    // Target show (optional): upcoming shows, PLEs flagged; the PLE appearance cap is enforced
    // server-side at add/edit time.
    targetShowCombo = new ComboBox<>("Target Show (optional)");
    targetShowCombo.setItems(context.upcomingShows() != null ? context.upcomingShows() : List.of());
    targetShowCombo.setItemLabelGenerator(BeatEditor::showLabel);
    targetShowCombo.setPlaceholder("Unassigned (AI planning picks)");
    targetShowCombo.setClearButtonVisible(true);
    targetShowCombo.setWidthFull();

    pleCapHint = new Span();
    pleCapHint.setVisible(false);
    pleCapHint.getStyle().set("color", "var(--lumo-secondary-text-color)");
    pleCapHint.getStyle().set("font-size", "var(--lumo-font-size-s)");
    targetShowCombo.addValueChangeListener(e -> refreshPleCapHint());

    HorizontalLayout topRow =
        new HorizontalLayout(
            segmentTypeCombo, segmentRuleCombo, winnerControlRadio, plannedWinnerCombo);
    topRow.setAlignItems(FlexComponent.Alignment.END);
    topRow.setWidthFull();

    VerticalLayout externalArea =
        new VerticalLayout(customTeamsCheck, teamRowsArea, externalRow, rationaleLabel);
    externalArea.setPadding(false);
    externalArea.setSpacing(false);
    externalArea.setWidthFull();

    HorizontalLayout titleRow = new HorizontalLayout(titleMatchCheck, titlesMulti);
    titleRow.setAlignItems(FlexComponent.Alignment.END);
    titleRow.setWidthFull();

    HorizontalLayout contenderRow = new HorizontalLayout(contenderMatchCheck, contenderTitleCombo);
    contenderRow.setAlignItems(FlexComponent.Alignment.END);
    contenderRow.setWidthFull();

    HorizontalLayout showRow = new HorizontalLayout(targetShowCombo, pleCapHint);
    showRow.setAlignItems(FlexComponent.Alignment.END);
    showRow.setWidthFull();

    HorizontalLayout optionRow = new HorizontalLayout(culminationCheck);
    optionRow.setAlignItems(FlexComponent.Alignment.CENTER);
    if (context.removable()) {
      Button removeBtn =
          new Button(
              "Remove",
              ev -> {
                if (context.removeHandler() != null) {
                  context.removeHandler().accept(this);
                }
              });
      removeBtn.addThemeVariants(
          ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
      optionRow.add(removeBtn);
    }

    setPadding(true);
    setSpacing(false);
    getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
    getStyle().set("border-radius", "var(--lumo-border-radius-m)");
    getStyle().set("margin-bottom", "var(--lumo-space-s)");

    add(topRow, optionRow, externalArea, titleRow, contenderRow, showRow, notesField);
  }

  private static List<Wrestler> participantsAndCandidates(
      List<Wrestler> participants, List<Wrestler> candidates) {
    List<Wrestler> all = new ArrayList<>(participants);
    for (Wrestler candidate : candidates) {
      if (all.stream().noneMatch(p -> p.getId().equals(candidate.getId()))) {
        all.add(candidate);
      }
    }
    return all;
  }

  /** Label helper: show name, date and a ★ PLE marker for premium live events. */
  private static String showLabel(Show show) {
    String date =
        show.getShowDate() != null ? SHOW_DATE_FMT.format(show.getShowDate()) : "unscheduled";
    return show.getName() + " — " + date + (show.isPremiumLiveEvent() ? " ★ PLE" : "");
  }

  /** Title label with current champion(s), mirroring the title views. */
  private static String titleWithHolders(Title title) {
    String holders =
        title.getChampion() == null || title.getChampion().isEmpty()
            ? "vacant"
            : title.getChampion().stream()
                .map(Wrestler::getName)
                .reduce((a, b) -> a + " & " + b)
                .orElse("vacant");
    return title.getName() + " — held by " + holders;
  }

  private void refreshPleCapHint() {
    if (script == null) {
      pleCapHint.setVisible(false);
      return;
    }
    long used = script.countPleBeats();
    Show picked = targetShowCombo.getValue();
    boolean pickedIsPle = picked != null && picked.isPremiumLiveEvent();
    pleCapHint.setText("PLE beats: " + used + " / " + script.getMaxPleAppearances());
    pleCapHint
        .getStyle()
        .set(
            "color",
            used >= script.getMaxPleAppearances() && pickedIsPle
                ? "var(--lumo-error-color)"
                : "var(--lumo-secondary-text-color)");
    pleCapHint.setVisible(true);
  }

  /**
   * Binds the owning arc and its feud wrestlers, required for the AI opponent suggestion and the
   * PLE-cap hint. Called by the owning dialog after construction.
   */
  public void bindScriptContext(FeudScript script, List<Wrestler> feudParticipants) {
    this.script = script;
    this.feudParticipants = feudParticipants != null ? feudParticipants : List.of();
    refreshPleCapHint();
  }

  /**
   * Pre-fills the editor from an existing (pending) beat — match type, stipulation, winner control,
   * planned winner, culmination, notes, target show, title stakes and participants. The persisted
   * participants are only selectable if they appear in this editor's candidate list (roster may
   * have changed since the beat was created); anything missing is re-added to the candidate set so
   * an edit never silently drops a persisted participant.
   *
   * @param beat the pending beat to load
   * @param additionalCandidates persisted participants of the beat, so they remain selectable
   */
  public void setBeat(FeudScriptBeat beat, List<Wrestler> additionalCandidates) {
    segmentTypeCombo.setValue(beat.getSegmentType());
    if (beat.getSegmentRule() != null) {
      segmentRuleCombo.setValue(beat.getSegmentRule());
    }
    winnerControlRadio.setValue(toWinnerControlLabel(beat.getWinnerControl()));
    if (beat.getPlannedWinner() != null) {
      plannedWinnerCombo.setValue(beat.getPlannedWinner());
    }
    culminationCheck.setValue(beat.isCulmination());
    notesField.setValue(beat.getNotes() != null ? beat.getNotes() : "");

    List<Wrestler> selectable =
        new ArrayList<>(opponentCombo.getListDataView().getItems().toList());
    if (additionalCandidates != null) {
      for (Wrestler candidate : additionalCandidates) {
        if (selectable.stream().noneMatch(w -> w.getId().equals(candidate.getId()))) {
          selectable.add(candidate);
        }
      }
      opponentCombo.setItems(selectable);
      extrasMulti.setItems(selectable);
    }

    if (beat.hasCustomTeams()) {
      customTeamsCheck.setValue(true);
      if (teamRows == null) {
        teamRows =
            new TeamRowsEditor(participantsAndCandidates(feudParticipants, selectable), () -> {});
        teamRowsArea.add(teamRows);
      } else {
        teamRows.clear();
      }
      for (Map.Entry<Integer, List<Wrestler>> entry : beat.getExplicitTeamLayout().entrySet()) {
        teamRows.addTeamRow(entry.getValue());
      }
      opponentCombo.clear();
      extrasMulti.clear();
    } else {
      Wrestler opponent =
          beat.getExternalOpponents().isEmpty() ? null : beat.getExternalOpponents().get(0);
      if (opponent != null) {
        opponentCombo.setValue(opponent);
      }
      extrasMulti.setValue(new HashSet<>(beat.getExternalExtras()));
    }

    if (beat.isTitleStakes()) {
      titleMatchCheck.setValue(true);
      Set<Title> selectableTitles =
          new HashSet<>(titlesMulti.getListDataView().getItems().toList());
      selectableTitles.addAll(beat.getTitles());
      titlesMulti.setItems(new ArrayList<>(selectableTitles));
      titlesMulti.setValue(beat.getTitles());
    }
    if (beat.getContenderTitle() != null) {
      contenderMatchCheck.setValue(true);
      Set<Title> selectableTitles =
          new HashSet<>(contenderTitleCombo.getListDataView().getItems().toList());
      selectableTitles.add(beat.getContenderTitle());
      contenderTitleCombo.setItems(new ArrayList<>(selectableTitles));
      contenderTitleCombo.setValue(beat.getContenderTitle());
    }
    if (beat.getTargetShow() != null) {
      List<Show> shows = new ArrayList<>(targetShowCombo.getListDataView().getItems().toList());
      if (shows.stream().noneMatch(s -> s.getId().equals(beat.getTargetShow().getId()))) {
        shows.add(beat.getTargetShow());
        targetShowCombo.setItems(shows);
      }
      targetShowCombo.setValue(beat.getTargetShow());
    }
    refreshPleCapHint();
  }

  private String toWinnerControlLabel(FeudScriptWinnerControl control) {
    return switch (control) {
      case BOOKER_PICKS -> BOOKER_PICKS;
      case SYSTEM_ROLL -> SYSTEM_ROLL;
      default -> AI_PICKS;
    };
  }

  private void suggestOpponent() {
    if (opponentAssistant == null || script == null) {
      return;
    }
    suggestOpponentButton.setEnabled(false);
    suggestOpponentButton.setText("AI thinking…");
    UI ui = UI.getCurrent();
    if (ui == null) {
      // No UI context (unit tests) — synchronous call.
      try {
        applySuggestion(
            opponentAssistant.suggestOpponent(
                script,
                feudParticipants,
                segmentTypeCombo.getValue(),
                segmentRuleCombo.getValue(),
                notesField.getValue()));
      } catch (Exception ex) {
        // Mirror the async error path: keep manual selection, report, restore button.
        Notification.show(
                "AI Suggest Opponent failed: " + rootMessage(ex),
                5000,
                Notification.Position.BOTTOM_END)
            .addThemeVariants(NotificationVariant.LUMO_ERROR);
      } finally {
        suggestOpponentButton.setEnabled(true);
        suggestOpponentButton.setText("✨ AI Suggest Opponent");
      }
      return;
    }
    GeneralSecurityUtils.runAsAdminAsync(
            () ->
                opponentAssistant.suggestOpponent(
                    script,
                    feudParticipants,
                    segmentTypeCombo.getValue(),
                    segmentRuleCombo.getValue(),
                    notesField.getValue()))
        .thenAccept(
            dto ->
                ui.access(
                    () -> {
                      applySuggestion(dto);
                      suggestOpponentButton.setEnabled(true);
                      suggestOpponentButton.setText("✨ AI Suggest Opponent");
                    }))
        .exceptionally(
            ex -> {
              Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
              ui.access(
                  () -> {
                    Notification.show(
                            "AI Suggest Opponent failed: " + rootMessage(cause),
                            5000,
                            Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    suggestOpponentButton.setEnabled(true);
                    suggestOpponentButton.setText("✨ AI Suggest Opponent");
                  });
              return null;
            });
  }

  private void applySuggestion(AiSuggestedOpponentDTO dto) {
    if (dto == null) {
      return;
    }
    Wrestler match = findCandidate(dto.getWrestlerId());
    if (match != null) {
      opponentCombo.setValue(match);
      rationaleLabel.setText("AI: " + dto.getRationale());
      rationaleLabel.setVisible(true);
    }
  }

  private Wrestler findCandidate(Long wrestlerId) {
    return opponentCombo
        .getListDataView()
        .getItems()
        .filter(w -> w.getId().equals(wrestlerId))
        .findFirst()
        .orElse(null);
  }

  private String rootMessage(Throwable t) {
    return t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
  }

  /** Builds a {@link FeudScriptBeat} from the current field values. */
  public FeudScriptBeat toBeat() {
    FeudScriptBeat beat = new FeudScriptBeat();
    beat.setSegmentType(segmentTypeCombo.getValue());
    beat.setSegmentRule(segmentRuleCombo.getValue());
    beat.setWinnerControl(toWinnerControl(winnerControlRadio.getValue()));
    if (BOOKER_PICKS.equals(winnerControlRadio.getValue())) {
      beat.setPlannedWinner(plannedWinnerCombo.getValue());
    }
    beat.setCulmination(culminationCheck.getValue());
    beat.setNotes(notesField.getValue());
    beat.setBeatStatus(FeudScriptBeatStatus.PENDING);
    beat.setTargetShow(targetShowCombo.getValue());

    if (Boolean.TRUE.equals(titleMatchCheck.getValue())) {
      beat.setTitleStakes(true);
      beat.setTitles(titlesMulti.getValue());
    }
    if (contenderMatchCheck.getValue()) {
      beat.setContenderTitle(contenderTitleCombo.getValue());
    }

    if (Boolean.TRUE.equals(customTeamsCheck.getValue()) && teamRows != null) {
      // Explicit layout: every participant row carries its team number. Arc wrestlers are
      // FEUD_MEMBER rows; anyone else in a custom layout is an OPPONENT.
      for (Map.Entry<Integer, List<Wrestler>> entry : teamRows.getTeams().entrySet()) {
        for (Wrestler wrestler : entry.getValue()) {
          FeudBeatParticipantRole role =
              onArc(wrestler)
                  ? FeudBeatParticipantRole.FEUD_MEMBER
                  : FeudBeatParticipantRole.OPPONENT;
          beat.addExternalParticipant(wrestler, role, entry.getKey());
        }
      }
    } else {
      if (opponentCombo.getValue() != null) {
        beat.addExternalParticipant(opponentCombo.getValue(), FeudBeatParticipantRole.OPPONENT);
      }
      for (Wrestler extra : extrasMulti.getValue()) {
        beat.addExternalParticipant(extra, FeudBeatParticipantRole.EXTRA);
      }
    }
    return beat;
  }

  /** True when the wrestler is one of the arc's own participants. */
  private boolean onArc(Wrestler wrestler) {
    return feudParticipants.stream().anyMatch(p -> p.getId().equals(wrestler.getId()));
  }

  /** True when a match type is selected, the externals stay disjoint and team/title rules hold. */
  public boolean isValid() {
    if (segmentTypeCombo.getValue() == null || segmentTypeCombo.getValue().isBlank()) {
      return false;
    }
    Wrestler opponent = opponentCombo.getValue();
    if (opponent != null && extrasMulti.getValue().contains(opponent)) {
      return false;
    }
    if (Boolean.TRUE.equals(customTeamsCheck.getValue())) {
      return teamRows != null && teamRows.isValid();
    }
    if (titleMatchCheck.getValue() && titlesMulti.getValue().isEmpty()) {
      return false;
    }
    return !contenderMatchCheck.getValue() || contenderTitleCombo.getValue() != null;
  }

  /** Marks every offending field invalid with a message. */
  public void showValidationError() {
    if (segmentTypeCombo.getValue() == null || segmentTypeCombo.getValue().isBlank()) {
      segmentTypeCombo.setInvalid(true);
      segmentTypeCombo.setErrorMessage("Match type is required");
    }
    if (Boolean.TRUE.equals(customTeamsCheck.getValue())
        && teamRows != null
        && !teamRows.isValid()) {
      Span error = new Span("Custom layout needs 2+ non-empty teams and no wrestler on two teams");
      error.getStyle().set("color", "var(--lumo-error-color)");
      teamRowsArea.addComponentAsFirst(error);
    }
    if (titleMatchCheck.getValue() && titlesMulti.getValue().isEmpty()) {
      titlesMulti.setInvalid(true);
      titlesMulti.setErrorMessage("Select at least one title when it's on the line");
    }
    if (contenderMatchCheck.getValue() && contenderTitleCombo.getValue() == null) {
      contenderTitleCombo.setInvalid(true);
      contenderTitleCombo.setErrorMessage("Pick the title the winner becomes #1 contender for");
    }
  }

  private FeudScriptWinnerControl toWinnerControl(String label) {
    return switch (label) {
      case BOOKER_PICKS -> FeudScriptWinnerControl.BOOKER_PICKS;
      case SYSTEM_ROLL -> FeudScriptWinnerControl.SYSTEM_ROLL;
      default -> FeudScriptWinnerControl.AI_PICKS;
    };
  }

  /** Externals selected so far (opponent + extras), for tests and owning dialogs. */
  public List<Wrestler> getSelectedExternals() {
    List<Wrestler> all = new ArrayList<>();
    if (opponentCombo.getValue() != null) {
      all.add(opponentCombo.getValue());
    }
    all.addAll(extrasMulti.getValue());
    return all;
  }
}
