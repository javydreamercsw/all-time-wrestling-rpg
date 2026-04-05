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
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
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
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;

public class EditSegmentDialog extends Dialog {

  @Getter private final ProposedSegment segment;
  private final WrestlerRepository wrestlerRepository;
  private final WrestlerService wrestlerService;
  private final TitleService titleService;
  private final SegmentTypeRepository segmentTypeRepository;
  private final SegmentRuleRepository segmentRuleRepository;
  private final com.github.javydreamercsw.management.service.npc.NpcService npcService;
  private final Runnable onSave;
  @Getter private final TextArea narrationArea;
  @Getter private final MultiSelectComboBox<Wrestler> participantsCombo;

  private final ComboBox<com.github.javydreamercsw.management.domain.npc.Npc> refereeCombo;
  private final ComboBox<Gender> genderFilter;
  private final ComboBox<AlignmentType> alignmentFilter;

  @Getter
  private final MultiSelectComboBox<Title>
      titleMultiSelectComboBox; // MultiSelectComboBox for titles

  @Getter private final Button saveButton;
  private final Button cancelButton;
  @Getter private final ComboBox<SegmentType> segmentTypeCombo;
  private final MultiSelectComboBox<SegmentRule> rulesCombo;
  private final MultiSelectComboBox<Wrestler> winnersCombo;
  private final TextArea summaryArea;
  private final Checkbox isTitleSegmentCheckbox;
  private final com.vaadin.flow.component.html.Span synergyBonusLabel;

  public EditSegmentDialog(
      ProposedSegment segment,
      WrestlerRepository wrestlerRepository,
      WrestlerService wrestlerService,
      TitleService titleService,
      SegmentTypeRepository segmentTypeRepository,
      SegmentRuleRepository segmentRuleRepository,
      com.github.javydreamercsw.management.service.npc.NpcService npcService,
      Gender defaultGenderConstraint,
      Runnable onSave) {
    this.segment = segment;
    this.wrestlerRepository = wrestlerRepository;
    this.wrestlerService = wrestlerService;
    this.titleService = titleService;
    this.segmentTypeRepository = segmentTypeRepository;
    this.segmentRuleRepository = segmentRuleRepository;
    this.npcService = npcService;
    this.onSave = onSave;

    setHeaderTitle("Edit Segment");

    // Form layout
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
    segmentTypeCombo.setItems(
        segmentTypeRepository.findAll().stream()
            .sorted(Comparator.comparing(SegmentType::getName))
            .collect(Collectors.toList()));
    segmentTypeCombo.setItemLabelGenerator(SegmentType::getName);
    segmentTypeCombo.setWidthFull();
    segmentTypeCombo.setRequired(true);
    segmentTypeRepository.findByName(segment.getType()).ifPresent(segmentTypeCombo::setValue);
    segmentTypeCombo.setId("edit-segment-type-combo-box");

    refereeCombo = new ComboBox<>("Referee");
    refereeCombo.setItems(npcService.findAllByType("Referee"));
    refereeCombo.setItemLabelGenerator(
        com.github.javydreamercsw.management.domain.npc.Npc::getName);
    refereeCombo.setWidthFull();
    if (segment.getRefereeName() != null) {
      npcService.findByName(segment.getRefereeName());
      refereeCombo.setValue(npcService.findByName(segment.getRefereeName()));
    }
    refereeCombo.setId("edit-referee-combo-box");

    rulesCombo = new MultiSelectComboBox<>("Segment Rules");
    rulesCombo.setItems(
        segmentRuleRepository.findAll().stream()
            .sorted(Comparator.comparing(SegmentRule::getName))
            .collect(Collectors.toList()));
    rulesCombo.setItemLabelGenerator(SegmentRule::getName);
    rulesCombo.setWidthFull();
    // Pre-select existing rules
    if (segment.getRules() != null) {
      rulesCombo.setValue(
          segment.getRules().stream()
              .map(segmentRuleRepository::findByName)
              .filter(java.util.Optional::isPresent)
              .map(java.util.Optional::get)
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

    // Pre-select existing participants
    java.util.Set<Wrestler> existingParticipants =
        segment.getParticipants().stream()
            .map(wrestlerRepository::findByName)
            .filter(java.util.Optional::isPresent)
            .map(java.util.Optional::get)
            .collect(Collectors.toSet());

    refreshParticipantsList(existingParticipants);

    // Filter logic
    alignmentFilter.addValueChangeListener(
        e -> refreshParticipantsList(participantsCombo.getValue()));
    genderFilter.addValueChangeListener(e -> refreshParticipantsList(participantsCombo.getValue()));

    participantsCombo.setValue(existingParticipants);

    winnersCombo = new MultiSelectComboBox<>("Winners (Optional)");
    winnersCombo.setItemLabelGenerator(Wrestler::getName);
    winnersCombo.setWidthFull();
    winnersCombo.setId("edit-winners-combo-box");
    // Pre-select existing winners if any
    if (segment.getWinners() != null) {
      winnersCombo.setValue(
          segment.getWinners().stream()
              .map(wrestlerRepository::findByName)
              .filter(java.util.Optional::isPresent)
              .map(java.util.Optional::get)
              .collect(Collectors.toSet()));
    }

    participantsCombo.addValueChangeListener(
        e -> {
          updateSynergyBonus(e.getValue());
          winnersCombo.setItems(
              e.getValue().stream()
                  .sorted(Comparator.comparing(Wrestler::getName))
                  .collect(Collectors.toList()));
          // Clear winners if selected participants no longer include them
          winnersCombo.setValue(
              winnersCombo.getValue().stream()
                  .filter(e.getValue()::contains)
                  .collect(Collectors.toSet()));
        });

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

    titleMultiSelectComboBox = new MultiSelectComboBox<>("Titles");
    titleMultiSelectComboBox.setItems(titleService.findAll());
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
        narrationArea);

    saveButton = new Button("Save", e -> save());
    cancelButton = new Button("Cancel", e -> close());

    getFooter().add(cancelButton, saveButton);
    add(new VerticalLayout(formLayout));
  }

  private void updateSynergyBonus(java.util.Collection<Wrestler> wrestlers) {
    int totalBonus = 0;
    java.util.Map<Long, Integer> factionCounts = new java.util.HashMap<>();
    java.util.Map<Long, Integer> factionAffinity = new java.util.HashMap<>();

    for (Wrestler w : wrestlers) {
      if (w.getFaction() != null) {
        Long fid = w.getFaction().getId();
        factionCounts.put(fid, factionCounts.getOrDefault(fid, 0) + 1);
        factionAffinity.put(fid, w.getFaction().getAffinity());
      }
    }

    for (java.util.Map.Entry<Long, Integer> entry : factionCounts.entrySet()) {
      int count = entry.getValue();
      if (count > 1) {
        int affinity = factionAffinity.get(entry.getKey());
        totalBonus += (count - 1) * (affinity / 10);
      }
    }

    synergyBonusLabel.setText("Faction Synergy Bonus: +" + totalBonus + " weight");
    synergyBonusLabel.setVisible(totalBonus > 0);
  }

  private void refreshParticipantsList(java.util.Set<Wrestler> selectedWrestlers) {

    AlignmentType alignment = alignmentFilter.getValue();

    Gender gender = genderFilter.getValue();

    List<Wrestler> filteredWrestlers =
        wrestlerService.findAllFiltered(alignment, gender, selectedWrestlers);

    participantsCombo.setItems(filteredWrestlers);
  }

  public void save() {
    segment.setType(segmentTypeCombo.getValue().getName());
    segment.setNarration(narrationArea.getValue());
    segment.setSummary(summaryArea.getValue());
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
