/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.service.show.planning.ProposedSegment;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;

public class EditSegmentDialog extends Dialog {

  /**
   * All reference data needed by the dialog, loaded in one async batch. Use {@link #load} to
   * pre-fetch before constructing the dialog on the UI thread.
   */
  public record PreloadedData(
      List<SegmentType> segmentTypes,
      List<SegmentRule> segmentRules,
      List<Npc> referees,
      List<Title> titles,
      List<Wrestler> activeWrestlers,
      Map<String, Wrestler> wrestlerByName) {

    public static PreloadedData load(
        final SegmentTypeRepository segmentTypeRepository,
        final SegmentRuleRepository segmentRuleRepository,
        final com.github.javydreamercsw.management.service.npc.NpcService npcService,
        final TitleService titleService,
        final WrestlerService wrestlerService,
        final Long universeId) {
      List<Wrestler> active = wrestlerService.findAllFiltered(null, null, universeId);
      Map<String, Wrestler> byName =
          wrestlerService.getAllWrestlers().stream()
              .collect(Collectors.toMap(Wrestler::getName, w -> w, (a, b) -> a));
      return new PreloadedData(
          segmentTypeRepository.findAll().stream()
              .sorted(Comparator.comparing(SegmentType::getName))
              .collect(Collectors.toList()),
          segmentRuleRepository.findAll().stream()
              .sorted(Comparator.comparing(SegmentRule::getName))
              .collect(Collectors.toList()),
          npcService.findAllByType("Referee").stream()
              .sorted(Comparator.comparing(Npc::getName))
              .collect(Collectors.toList()),
          titleService.findAll(),
          active,
          byName);
    }
  }

  @Getter private final ProposedSegment segment;
  private final PreloadedData data;
  private final WrestlerService wrestlerService;
  private final Long universeId;
  private final Runnable onSave;
  @Getter private final TextArea narrationArea;
  @Getter private final TextArea notesArea;
  @Getter private final MultiSelectComboBox<Wrestler> participantsCombo;

  private final ComboBox<Npc> refereeCombo;
  private final ComboBox<Gender> genderFilter;
  private final ComboBox<AlignmentType> alignmentFilter;

  @Getter private final MultiSelectComboBox<Title> titleMultiSelectComboBox;

  @Getter private final Button saveButton;
  private final Button cancelButton;
  @Getter private final ComboBox<SegmentType> segmentTypeCombo;
  private final MultiSelectComboBox<SegmentRule> rulesCombo;
  private final MultiSelectComboBox<Wrestler> winnersCombo;
  private final TextArea summaryArea;
  private final Checkbox isTitleSegmentCheckbox;
  private final com.vaadin.flow.component.html.Span synergyBonusLabel;

  /** Fast constructor: all reference data pre-loaded — zero DB queries at construction time. */
  public EditSegmentDialog(
      final ProposedSegment segment,
      final PreloadedData data,
      final WrestlerService wrestlerService,
      final Gender defaultGenderConstraint,
      final Long universeId,
      final Runnable onSave) {
    this.segment = segment;
    this.data = data;
    this.wrestlerService = wrestlerService;
    this.universeId = universeId;
    this.onSave = onSave;

    setHeaderTitle("Edit Segment");

    FormLayout formLayout = new FormLayout();
    formLayout.setResponsiveSteps(
        new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("500px", 2));

    synergyBonusLabel = new com.vaadin.flow.component.html.Span("Synergy Bonus: +0");
    synergyBonusLabel.addClassNames(
        com.vaadin.flow.theme.lumo.LumoUtility.FontSize.SMALL,
        com.vaadin.flow.theme.lumo.LumoUtility.TextColor.SUCCESS,
        com.vaadin.flow.theme.lumo.LumoUtility.FontWeight.BOLD);
    synergyBonusLabel.setId("edit-synergy-bonus-label");

    segmentTypeCombo = new ComboBox<>("Segment Type");
    segmentTypeCombo.setItems(data.segmentTypes());
    segmentTypeCombo.setItemLabelGenerator(SegmentType::getName);
    segmentTypeCombo.setWidthFull();
    segmentTypeCombo.setRequired(true);
    data.segmentTypes().stream()
        .filter(t -> t.getName().equals(segment.getType()))
        .findFirst()
        .ifPresent(segmentTypeCombo::setValue);
    segmentTypeCombo.setId("edit-segment-type-combo-box");

    refereeCombo = new ComboBox<>("Referee");
    refereeCombo.setItems(data.referees());
    refereeCombo.setItemLabelGenerator(Npc::getName);
    refereeCombo.setWidthFull();
    if (segment.getRefereeName() != null) {
      data.referees().stream()
          .filter(n -> segment.getRefereeName().equals(n.getName()))
          .findFirst()
          .ifPresent(refereeCombo::setValue);
    }
    refereeCombo.setId("edit-referee-combo-box");

    rulesCombo = new MultiSelectComboBox<>("Segment Rules");
    rulesCombo.setItems(data.segmentRules());
    rulesCombo.setItemLabelGenerator(SegmentRule::getName);
    rulesCombo.setWidthFull();
    if (segment.getRules() != null) {
      rulesCombo.setValue(
          segment.getRules().stream()
              .map(
                  name ->
                      data.segmentRules().stream()
                          .filter(r -> r.getName().equals(name))
                          .findFirst())
              .filter(Optional::isPresent)
              .map(Optional::get)
              .collect(Collectors.toSet()));
    }
    rulesCombo.setId("edit-segment-rules-combo-box");
    formLayout.setColspan(rulesCombo, 2);

    alignmentFilter = new ComboBox<>("Alignment Filter");
    alignmentFilter.setItems(AlignmentType.values());
    alignmentFilter.setClearButtonVisible(true);
    alignmentFilter.setPlaceholder("All alignments");
    alignmentFilter.setWidthFull();
    alignmentFilter.setId("edit-alignment-filter-combo-box");

    genderFilter = new ComboBox<>("Gender Filter");
    genderFilter.setItems(Gender.values());
    genderFilter.setClearButtonVisible(true);
    genderFilter.setPlaceholder("All genders");
    genderFilter.setWidthFull();
    genderFilter.setValue(defaultGenderConstraint);
    genderFilter.setId("edit-gender-filter-combo-box");

    participantsCombo = new MultiSelectComboBox<>("Participants");
    participantsCombo.setItemLabelGenerator(Wrestler::getName);
    participantsCombo.setWidthFull();
    participantsCombo.setRequired(true);
    participantsCombo.setId("edit-wrestlers-combo-box");

    Set<Wrestler> existingParticipants =
        segment.getParticipants().stream()
            .map(name -> Optional.ofNullable(data.wrestlerByName().get(name)))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());

    winnersCombo = new MultiSelectComboBox<>("Winners (Optional)");
    winnersCombo.setItemLabelGenerator(Wrestler::getName);
    winnersCombo.setWidthFull();
    winnersCombo.setId("edit-winners-combo-box");

    participantsCombo.addValueChangeListener(
        e -> {
          updateSynergyBonus(e.getValue());
          winnersCombo.setItems(
              e.getValue().stream()
                  .sorted(Comparator.comparing(Wrestler::getName))
                  .collect(Collectors.toList()));
          winnersCombo.setValue(
              winnersCombo.getValue().stream()
                  .filter(e.getValue()::contains)
                  .collect(Collectors.toSet()));
        });

    refreshParticipantsList(existingParticipants);

    alignmentFilter.addValueChangeListener(
        e -> refreshParticipantsList(participantsCombo.getValue()));
    genderFilter.addValueChangeListener(e -> refreshParticipantsList(participantsCombo.getValue()));

    participantsCombo.setValue(existingParticipants);

    if (segment.getWinners() != null) {
      winnersCombo.setValue(
          segment.getWinners().stream()
              .map(name -> Optional.ofNullable(data.wrestlerByName().get(name)))
              .filter(Optional::isPresent)
              .map(Optional::get)
              .collect(Collectors.toSet()));
    }

    updateSynergyBonus(existingParticipants);

    summaryArea = new TextArea("Summary");
    summaryArea.setWidthFull();
    summaryArea.setValue(segment.getSummary() != null ? segment.getSummary() : "");
    summaryArea.setId("edit-summary-text-area");
    formLayout.setColspan(summaryArea, 2);

    narrationArea = new TextArea("Narration");
    narrationArea.setWidthFull();
    narrationArea.setValue(segment.getNarration());
    narrationArea.setId("edit-narration-text-area");
    formLayout.setColspan(narrationArea, 2);

    notesArea = new TextArea("Match Feedback / Notes");
    notesArea.setWidthFull();
    notesArea.setValue(segment.getNotes() != null ? segment.getNotes() : "");
    notesArea.setId("edit-notes-text-area");
    notesArea.setPlaceholder("Provide specific instructions for the AI narration...");
    formLayout.setColspan(notesArea, 2);

    titleMultiSelectComboBox = new MultiSelectComboBox<>("Titles");
    titleMultiSelectComboBox.setItems(data.titles());
    titleMultiSelectComboBox.setItemLabelGenerator(Title::getName);
    titleMultiSelectComboBox.setWidthFull();
    titleMultiSelectComboBox.setId("edit-title-multi-select-combo-box");

    isTitleSegmentCheckbox = new Checkbox("Is Title Segment");
    isTitleSegmentCheckbox.setId("edit-is-title-segment-checkbox");
    isTitleSegmentCheckbox.addValueChangeListener(
        event -> {
          titleMultiSelectComboBox.setVisible(event.getValue());
          if (!event.getValue()) {
            titleMultiSelectComboBox.clear();
          }
        });
    isTitleSegmentCheckbox.setValue(segment.getIsTitleSegment());
    titleMultiSelectComboBox.setVisible(segment.getIsTitleSegment());
    if (segment.getIsTitleSegment()) {
      titleMultiSelectComboBox.setValue(segment.getTitles());
    }

    formLayout.add(
        segmentTypeCombo,
        rulesCombo,
        refereeCombo,
        alignmentFilter,
        genderFilter,
        participantsCombo,
        synergyBonusLabel,
        winnersCombo,
        isTitleSegmentCheckbox,
        titleMultiSelectComboBox,
        summaryArea,
        notesArea,
        narrationArea);

    saveButton = new Button("Save", e -> save());
    cancelButton = new Button("Cancel", e -> close());

    getFooter().add(cancelButton, saveButton);
    add(new VerticalLayout(formLayout));
  }

  /** Legacy constructor kept for test backward compatibility. Builds PreloadedData inline. */
  public EditSegmentDialog(
      final ProposedSegment segment,
      final WrestlerRepository wrestlerRepository,
      final WrestlerService wrestlerService,
      final TitleService titleService,
      final SegmentTypeRepository segmentTypeRepository,
      final SegmentRuleRepository segmentRuleRepository,
      final com.github.javydreamercsw.management.service.npc.NpcService npcService,
      final Gender defaultGenderConstraint,
      final Long universeId,
      final Runnable onSave) {
    this(
        segment,
        PreloadedData.load(
            segmentTypeRepository,
            segmentRuleRepository,
            npcService,
            titleService,
            wrestlerService,
            universeId),
        wrestlerService,
        defaultGenderConstraint,
        universeId,
        onSave);
  }

  private void updateSynergyBonus(final java.util.Collection<Wrestler> wrestlers) {
    int totalBonus = 0;
    Map<Long, Integer> factionCounts = new HashMap<>();
    Map<Long, Integer> factionAffinity = new HashMap<>();

    for (Wrestler w : wrestlers) {
      w.getDefaultState()
          .map(WrestlerState::getFaction)
          .ifPresent(
              faction -> {
                Long fid = faction.getId();
                factionCounts.put(fid, factionCounts.getOrDefault(fid, 0) + 1);
                factionAffinity.put(fid, faction.getAffinity());
              });
    }

    for (Map.Entry<Long, Integer> entry : factionCounts.entrySet()) {
      int count = entry.getValue();
      if (count > 1) {
        int affinity = factionAffinity.get(entry.getKey());
        totalBonus += (count - 1) * (affinity / 10);
      }
    }

    synergyBonusLabel.setText("Faction Synergy Bonus: +" + totalBonus + " weight");
    synergyBonusLabel.setVisible(totalBonus > 0);
  }

  private void refreshParticipantsList(final Set<Wrestler> selectedWrestlers) {
    AlignmentType alignment = alignmentFilter.getValue();
    Gender gender = genderFilter.getValue();

    if (alignment != null) {
      // Alignment filtering needs DB-backed service (alignment data not pre-loaded)
      participantsCombo.setItems(
          wrestlerService.findAllFiltered(alignment, gender, universeId, selectedWrestlers));
      return;
    }

    // In-memory filter from pre-loaded data — zero DB queries
    Set<Long> selectedIds =
        selectedWrestlers == null
            ? Set.of()
            : selectedWrestlers.stream().map(Wrestler::getId).collect(Collectors.toSet());

    List<Wrestler> filtered =
        data.activeWrestlers().stream()
            .filter(
                w -> selectedIds.contains(w.getId()) || (gender == null || w.getGender() == gender))
            .sorted(Comparator.comparing(Wrestler::getName))
            .collect(Collectors.toList());
    participantsCombo.setItems(filtered);
  }

  public void save() {
    segment.setType(segmentTypeCombo.getValue().getName());
    segment.setNarration(narrationArea.getValue());
    segment.setSummary(summaryArea.getValue());
    segment.setNotes(notesArea.getValue());
    segment.setParticipants(
        participantsCombo.getValue().stream().map(Wrestler::getName).collect(Collectors.toList()));
    segment.setWinners(
        winnersCombo.getValue().stream().map(Wrestler::getName).collect(Collectors.toList()));
    segment.setRules(
        rulesCombo.getValue().stream().map(SegmentRule::getName).collect(Collectors.toList()));
    segment.setIsTitleSegment(isTitleSegmentCheckbox.getValue());
    segment.setTitles(titleMultiSelectComboBox.getValue());
    if (refereeCombo.getValue() != null) {
      segment.setRefereeName(refereeCombo.getValue().getName());
    } else {
      segment.setRefereeName(null);
    }
    onSave.run();
    close();
  }
}
