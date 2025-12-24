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

import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.show.planning.ProposedSegment;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import java.util.stream.Collectors;

public class EditSegmentDialog extends Dialog {

  private final ProposedSegment segment;
  private final WrestlerService wrestlerService;
  private final WrestlerRepository wrestlerRepository;
  private final TitleService titleService; // Injected TitleService
  private final Runnable onSave;

  private final TextArea descriptionArea;
  private final CheckboxGroup<Wrestler> participantsCheckboxGroup;
  private final MultiSelectComboBox<Title>
      titleMultiSelectComboBox; // MultiSelectComboBox for titles
  private final Button saveButton;
  private final Button cancelButton;

  public EditSegmentDialog(
      ProposedSegment segment,
      WrestlerService wrestlerService,
      WrestlerRepository wrestlerRepository,
      TitleService titleService,
      Runnable onSave) {
    this.segment = segment;
    this.wrestlerService = wrestlerService;
    this.wrestlerRepository = wrestlerRepository;
    this.titleService = titleService;
    this.onSave = onSave;

    setHeaderTitle("Edit Segment");

    descriptionArea = new TextArea("Description");
    descriptionArea.setValue(segment.getDescription());
    descriptionArea.setWidthFull();

    participantsCheckboxGroup = new CheckboxGroup<>("Participants");
    participantsCheckboxGroup.setItems(wrestlerRepository.findAll());
    participantsCheckboxGroup.setItemLabelGenerator(Wrestler::getName);
    participantsCheckboxGroup.setValue(
        segment.getParticipants().stream()
            .map(wrestlerRepository::findByName)
            .filter(java.util.Optional::isPresent)
            .map(java.util.Optional::get)
            .collect(Collectors.toSet()));

    // MultiSelectComboBox for titles
    titleMultiSelectComboBox = new MultiSelectComboBox<>("Titles");
    titleMultiSelectComboBox.setItems(titleService.findAll()); // Populate with available titles
    titleMultiSelectComboBox.setItemLabelGenerator(Title::getName);
    titleMultiSelectComboBox.setWidthFull();
    // Set initial selection if the segment is a title match
    if (segment.getIsTitleSegment()) {
      titleMultiSelectComboBox.setValue(segment.getTitles());
      titleMultiSelectComboBox.setVisible(true);
    } else {
      titleMultiSelectComboBox.setVisible(false);
    }

    saveButton = new Button("Save", e -> save());
    cancelButton = new Button("Cancel", e -> close());

    getFooter().add(cancelButton, saveButton);
    add(new VerticalLayout(descriptionArea, participantsCheckboxGroup, titleMultiSelectComboBox));
  }

  public void save() {
    segment.setParticipants(
        participantsCheckboxGroup.getValue().stream()
            .map(Wrestler::getName)
            .collect(Collectors.toList()));
    segment.setDescription(descriptionArea.getValue());
    // Update segment's titles
    segment.setTitles(titleMultiSelectComboBox.getValue());
    onSave.run();
    close();
  }

  public TextArea getDescriptionArea() {
    return descriptionArea;
  }

  public CheckboxGroup<Wrestler> getParticipantsCheckboxGroup() {
    return participantsCheckboxGroup;
  }

  public MultiSelectComboBox<Title> getTitleMultiSelectComboBox() {
    return titleMultiSelectComboBox;
  }

  public ProposedSegment getSegment() {
    return segment;
  }

  public Button getSaveButton() {
    return saveButton;
  }
}
