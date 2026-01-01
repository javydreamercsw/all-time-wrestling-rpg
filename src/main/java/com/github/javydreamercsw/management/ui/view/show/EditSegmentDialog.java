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

import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.show.planning.ProposedSegment;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import java.util.Comparator;
import java.util.stream.Collectors;
import lombok.Getter;

public class EditSegmentDialog extends Dialog {

  @Getter private final ProposedSegment segment;
  private final WrestlerRepository wrestlerRepository;
  private final TitleService titleService;
  private final SegmentTypeRepository segmentTypeRepository;
  private final SegmentRuleRepository segmentRuleRepository;
  private final Runnable onSave;
  @Getter private final TextArea narrationArea;
  @Getter private final MultiSelectComboBox<Wrestler> participantsCombo;

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

  public EditSegmentDialog(
      ProposedSegment segment,
      WrestlerRepository wrestlerRepository,
      TitleService titleService,
      SegmentTypeRepository segmentTypeRepository,
      SegmentRuleRepository segmentRuleRepository,
      Runnable onSave) {
    this.segment = segment;
    this.wrestlerRepository = wrestlerRepository;
    this.titleService = titleService;
    this.segmentTypeRepository = segmentTypeRepository;
    this.segmentRuleRepository = segmentRuleRepository;
    this.onSave = onSave;

    setHeaderTitle("Edit Segment");

    // Form layout
    FormLayout formLayout = new FormLayout();
    formLayout.setResponsiveSteps(
        new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("500px", 2));

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

    participantsCombo = new MultiSelectComboBox<>("Participants");
    participantsCombo.setItems(wrestlerRepository.findAll());
    participantsCombo.setItemLabelGenerator(Wrestler::getName);
    participantsCombo.setValue(
        segment.getParticipants().stream()
            .map(wrestlerRepository::findByName)
            .filter(java.util.Optional::isPresent)
            .map(java.util.Optional::get)
            .collect(Collectors.toSet()));
    participantsCombo.setWidthFull();
    participantsCombo.setRequired(true);
    participantsCombo.setId("edit-wrestlers-combo-box");

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

    // Update winner options when wrestlers change
    participantsCombo.addValueChangeListener(
        e -> {
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

    summaryArea = new TextArea("Summary");
    summaryArea.setWidthFull();
    summaryArea.setValue(segment.getSummary() != null ? segment.getSummary() : "");
    summaryArea.setId("edit-summary-text-area");
    formLayout.setColspan(summaryArea, 2);

    narrationArea = new TextArea("Narration");
    narrationArea.setWidthFull();
    narrationArea.setValue(segment.getDescription());
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
        participantsCombo,
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

  public void save() {
    segment.setType(segmentTypeCombo.getValue().getName());
    segment.setDescription(narrationArea.getValue());
    segment.setSummary(summaryArea.getValue());
    segment.setParticipants(
        participantsCombo.getValue().stream().map(Wrestler::getName).collect(Collectors.toList()));
    segment.setWinners(
        winnersCombo.getValue().stream().map(Wrestler::getName).collect(Collectors.toList()));
    segment.setRules(
        rulesCombo.getValue().stream().map(SegmentRule::getName).collect(Collectors.toList()));
    segment.setIsTitleSegment(isTitleSegmentCheckbox.getValue());
    segment.setTitles(titleMultiSelectComboBox.getValue());
    onSave.run();
    close();
  }
}
