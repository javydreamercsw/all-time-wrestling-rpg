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
import com.github.javydreamercsw.management.domain.feud.FeudParticipant;
import com.github.javydreamercsw.management.domain.feud.FeudScript;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeat;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeatStatus;
import com.github.javydreamercsw.management.domain.feud.FeudScriptWinnerControl;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.title.Title;
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
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * One story arc as a card: status badge, progress header (beats resolved, PLE appearances vs cap)
 * and the beat grid (planned winner, title stakes, target show, actions). Shared by the rivalry
 * detail view and the story-arcs list view so both surfaces render identically.
 */
public class FeudScriptCard extends VerticalLayout {

  /** Services the card's dialogs and actions need; bundle keeps the constructor short. */
  public record EditorServices(
      FeudScriptService feudScriptService,
      SegmentTypeService segmentTypeService,
      SegmentRuleService segmentRuleService,
      FeudBeatAssistantService feudBeatAssistantService,
      SecurityUtils securityUtils,
      WrestlerService wrestlerService,
      ShowService showService,
      TitleService titleService) {

    public static EditorServices of(
        FeudScriptService feudScriptService,
        SegmentTypeService segmentTypeService,
        SegmentRuleService segmentRuleService,
        FeudBeatAssistantService feudBeatAssistantService,
        SecurityUtils securityUtils,
        WrestlerService wrestlerService,
        ShowService showService,
        TitleService titleService) {
      return new EditorServices(
          feudScriptService,
          segmentTypeService,
          segmentRuleService,
          feudBeatAssistantService,
          securityUtils,
          wrestlerService,
          showService,
          titleService);
    }
  }

  private final FeudScript script;
  private final List<Wrestler> participants;
  private final EditorServices services;
  private final Runnable reload;

  public FeudScriptCard(
      FeudScript script, List<Wrestler> participants, EditorServices services, Runnable reload) {
    this.script = script;
    this.participants = participants;
    this.services = services;
    this.reload = reload;

    setPadding(true);
    setSpacing(true);
    addClassNames(
        LumoUtility.Border.ALL, LumoUtility.BorderRadius.MEDIUM, LumoUtility.Margin.Bottom.SMALL);

    Span nameSpan = new Span(script.getName());
    nameSpan.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.SEMIBOLD);

    Span statusBadge = new Span(script.getStatus().name());
    statusBadge.getElement().getThemeList().add("badge");
    if ("COMPLETED".equals(script.getStatus().name())) {
      statusBadge.getElement().getThemeList().add("success");
    } else if ("CANCELLED".equals(script.getStatus().name())) {
      statusBadge.getElement().getThemeList().add("error");
    } else {
      statusBadge.getElement().getThemeList().add("contrast");
    }

    HorizontalLayout header = new HorizontalLayout(nameSpan, statusBadge);
    header.setAlignItems(FlexComponent.Alignment.CENTER);
    header.setFlexGrow(1, nameSpan);

    long doneBeats =
        script.getBeats().stream()
            .filter(
                b ->
                    b.getBeatStatus() == FeudScriptBeatStatus.COMPLETED
                        || b.getBeatStatus() == FeudScriptBeatStatus.SKIPPED)
            .count();
    Span progressSpan =
        new Span(
            doneBeats
                + "/"
                + script.getBeats().size()
                + " beats · "
                + script.countPleBeats()
                + "/"
                + script.getMaxPleAppearances()
                + " PLEs");
    progressSpan.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
    header.add(progressSpan);

    boolean editable =
        services.securityUtils().canCreate()
            && !"COMPLETED".equals(script.getStatus().name())
            && !"CANCELLED".equals(script.getStatus().name());

    if (editable) {
      Button editButton = new Button("Edit");
      editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
      editButton.addClickListener(e -> openEditDialog());

      Button cancelButton = new Button("Cancel Arc");
      cancelButton.addThemeVariants(
          ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
      cancelButton.addClickListener(e -> confirmCancelScript());

      Button addBeatButton = new Button("+ Add Beat");
      addBeatButton.addThemeVariants(
          ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
      addBeatButton.addClickListener(e -> openAddBeatDialog());

      header.add(addBeatButton, editButton, cancelButton);
    }

    List<FeudScriptBeat> beats =
        script.getBeats().stream()
            .sorted(Comparator.comparing(FeudScriptBeat::getBeatOrder))
            .collect(Collectors.toList());

    if (beats.isEmpty()) {
      add(header, new Paragraph("No beats defined."));
      return;
    }

    Grid<FeudScriptBeat> beatGrid = new Grid<>(FeudScriptBeat.class, false);
    beatGrid.addColumn(FeudScriptBeat::getBeatOrder).setHeader("#").setWidth("4em").setFlexGrow(0);
    beatGrid.addColumn(FeudScriptBeat::getSegmentType).setHeader("Match Type").setFlexGrow(1);
    beatGrid
        .addColumn(b -> b.getSegmentRule() != null ? b.getSegmentRule() : "—")
        .setHeader("Stipulation")
        .setFlexGrow(1);
    beatGrid
        .addColumn(
            b ->
                b.getExternalParticipants().isEmpty()
                    ? "—"
                    : b.getExternalParticipants().stream()
                        .map(
                            p ->
                                p.getWrestler().getName()
                                    + " ("
                                    + p.getRole().getDisplayName()
                                    + ")")
                        .collect(Collectors.joining(", ")))
        .setHeader("External")
        .setFlexGrow(1);
    beatGrid.addColumn(b -> b.getWinnerControl().name()).setHeader("Winner Control").setFlexGrow(1);
    beatGrid
        .addColumn(
            b ->
                b.getPlannedWinner() != null
                    ? b.getPlannedWinner().getName()
                    : b.getWinnerControl() == FeudScriptWinnerControl.AI_PICKS ? "AI" : "—")
        .setHeader("Planned Winner")
        .setFlexGrow(1);
    beatGrid
        .addColumn(
            b -> {
              List<String> stakes = new java.util.ArrayList<>();
              if (b.isTitleStakes()) {
                b.getTitles().stream().map(Title::getName).forEach(n -> stakes.add("★ " + n));
              }
              if (b.getContenderTitle() != null) {
                stakes.add("#1C " + b.getContenderTitle().getName());
              }
              return stakes.isEmpty() ? "—" : String.join(", ", stakes);
            })
        .setHeader("Title Stakes")
        .setFlexGrow(1);
    beatGrid
        .addColumn(
            b ->
                b.getTargetShow() == null
                    ? "—"
                    : b.getTargetShow().getName()
                        + (b.getTargetShow().isPremiumLiveEvent() ? " ★" : ""))
        .setHeader("Target Show")
        .setFlexGrow(1);
    beatGrid
        .addColumn(b -> b.isCulmination() ? "★ Blowoff" : "")
        .setHeader("")
        .setWidth("6em")
        .setFlexGrow(0);
    beatGrid.addColumn(b -> b.getBeatStatus().name()).setHeader("Status").setFlexGrow(1);

    if (editable) {
      beatGrid
          .addComponentColumn(
              beat -> {
                if (beat.getBeatStatus() == FeudScriptBeatStatus.PENDING) {
                  HorizontalLayout actions = new HorizontalLayout();
                  actions.setSpacing(false);
                  actions.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

                  Button editBtn = new Button("✎");
                  editBtn.addThemeVariants(
                      ButtonVariant.LUMO_SMALL,
                      ButtonVariant.LUMO_TERTIARY,
                      ButtonVariant.LUMO_CONTRAST);
                  editBtn.addClickListener(e -> openEditBeatDialog(beat));
                  actions.add(editBtn);

                  Button skipBtn = new Button("Skip");
                  skipBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
                  skipBtn.addClickListener(e -> confirmSkipBeat(beat));
                  actions.add(skipBtn);

                  Button removeBtn = new Button("✕");
                  removeBtn.addThemeVariants(
                      ButtonVariant.LUMO_SMALL,
                      ButtonVariant.LUMO_TERTIARY,
                      ButtonVariant.LUMO_ERROR);
                  removeBtn.addClickListener(e -> confirmRemoveBeat(beat));
                  actions.add(removeBtn);
                  return actions;
                }
                return new Span();
              })
          .setWidth("9em")
          .setFlexGrow(0);
    }

    beatGrid.setItems(beats);
    beatGrid.setAllRowsVisible(true);

    add(header, beatGrid);
  }

  /** The arc's wrestlers (rivalry pair or active feud members) for editor contexts. */
  public static List<Wrestler> participantsOf(FeudScript script) {
    if (script.getRivalry() != null) {
      return List.of(script.getRivalry().getWrestler1(), script.getRivalry().getWrestler2());
    }
    if (script.getFeud() != null) {
      return script.getFeud().getParticipants().stream()
          .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
          .map(FeudParticipant::getWrestler)
          .collect(Collectors.toList());
    }
    return List.of();
  }

  private void openAddBeatDialog() {
    AddBeatDialog dialog =
        new AddBeatDialog(
            script,
            participants,
            typeNames(),
            ruleNames(),
            services.feudScriptService(),
            externalCandidates(),
            services.feudBeatAssistantService(),
            services.showService().getUpcomingShows(20),
            services.titleService().getActiveTitles(),
            reload);
    dialog.open();
  }

  private void openEditBeatDialog(FeudScriptBeat beat) {
    EditBeatDialog dialog =
        new EditBeatDialog(
            script,
            beat,
            participants,
            typeNames(),
            ruleNames(),
            services.feudScriptService(),
            externalCandidates(),
            services.feudBeatAssistantService(),
            services.showService().getUpcomingShows(20),
            services.titleService().getActiveTitles(),
            reload);
    dialog.open();
  }

  private List<String> typeNames() {
    return services.segmentTypeService().findAll().stream()
        .map(SegmentType::getName)
        .sorted()
        .collect(Collectors.toList());
  }

  private List<String> ruleNames() {
    return services.segmentRuleService().findAll().stream()
        .map(SegmentRule::getName)
        .sorted()
        .collect(Collectors.toList());
  }

  /** Roster eligible for external participation: active/universe-filtered, any wrestler. */
  private List<Wrestler> externalCandidates() {
    return services.wrestlerService().findAllFiltered(null, null, null, null, null);
  }

  private void openEditDialog() {
    com.vaadin.flow.component.textfield.TextField nameField =
        new com.vaadin.flow.component.textfield.TextField("Arc Name");
    nameField.setValue(script.getName());
    nameField.setWidthFull();
    nameField.setRequired(true);

    com.vaadin.flow.component.textfield.IntegerField pleField =
        new com.vaadin.flow.component.textfield.IntegerField("Max PLE Appearances (1–3)");
    pleField.setValue(script.getMaxPleAppearances());
    pleField.setMin(1);
    pleField.setMax(3);
    pleField.setStepButtonsVisible(true);

    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("Edit Story Arc");

    Button saveBtn =
        new Button(
            "Save",
            e -> {
              if (nameField.getValue().isBlank()) {
                nameField.setInvalid(true);
                return;
              }
              services
                  .feudScriptService()
                  .updateScript(
                      script,
                      nameField.getValue(),
                      pleField.getValue() != null
                          ? pleField.getValue()
                          : script.getMaxPleAppearances());
              dialog.close();
              reload.run();
            });
    saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    Button cancelBtn = new Button("Cancel", e -> dialog.close());
    cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    dialog.add(new VerticalLayout(nameField, pleField));
    dialog.getFooter().add(cancelBtn, saveBtn);
    dialog.open();
  }

  private void confirmCancelScript() {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("Cancel Story Arc");
    dialog.add(
        new Paragraph(
            "Cancel arc \""
                + script.getName()
                + "\"? Completed beats are kept for reference, but no new beats can be added."));

    Button confirmBtn =
        new Button(
            "Cancel Arc",
            e -> {
              services.feudScriptService().cancelScript(script);
              dialog.close();
              reload.run();
            });
    confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

    Button backBtn = new Button("Keep Arc", e -> dialog.close());
    backBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    dialog.getFooter().add(backBtn, confirmBtn);
    dialog.open();
  }

  private void confirmRemoveBeat(FeudScriptBeat beat) {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("Remove Beat");
    dialog.add(
        new Paragraph(
            "Remove beat #"
                + beat.getBeatOrder()
                + " ("
                + beat.getSegmentType()
                + ")? Remaining beats will be renumbered."));

    Button confirmBtn =
        new Button(
            "Remove",
            e -> {
              services.feudScriptService().removeBeat(script, beat);
              dialog.close();
              reload.run();
            });
    confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

    Button cancelBtn = new Button("Keep Beat", e -> dialog.close());
    cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    dialog.getFooter().add(cancelBtn, confirmBtn);
    dialog.open();
  }

  private void confirmSkipBeat(FeudScriptBeat beat) {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("Skip Beat");
    dialog.add(
        new Paragraph(
            "Skip beat #"
                + beat.getBeatOrder()
                + " ("
                + beat.getSegmentType()
                + ")? It stays in the arc for reference but will never be booked."
                + (beat.getReservation() != null
                    ? " Its PLE slot reservation will be cancelled."
                    : "")));

    Button confirmBtn =
        new Button(
            "Skip Beat",
            e -> {
              services.feudScriptService().skipBeat(script, beat);
              dialog.close();
              reload.run();
            });
    confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

    Button keepBtn = new Button("Keep Beat", e -> dialog.close());
    keepBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    dialog.getFooter().add(keepBtn, confirmBtn);
    dialog.open();
  }
}
