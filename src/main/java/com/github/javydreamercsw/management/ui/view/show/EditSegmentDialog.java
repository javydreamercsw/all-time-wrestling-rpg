package com.github.javydreamercsw.management.ui.view.show;

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.show.planning.ProposedSegment;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import java.util.HashSet;
import java.util.stream.Collectors;
import org.vaadin.gatanaso.MultiselectComboBox;

public class EditSegmentDialog extends Dialog {

  private final ProposedSegment segment;
  private final WrestlerService wrestlerService;
  private final Runnable onSave;

  private MultiselectComboBox<Wrestler> participantsBox;
  private TextArea descriptionArea;
  private Button saveButton;

  public EditSegmentDialog(
      ProposedSegment segment, WrestlerService wrestlerService, Runnable onSave) {
    this.segment = segment;
    this.wrestlerService = wrestlerService;
    this.onSave = onSave;

    setHeaderTitle("Edit Segment");

    participantsBox = new MultiselectComboBox<>("Participants");
    participantsBox.setItems(wrestlerService.findAll());
    participantsBox.setItemLabelGenerator(Wrestler::getName);
    participantsBox.setValue(
        new HashSet<>(
            segment.getParticipants().stream()
                .map(wrestlerService::findByName)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .collect(Collectors.toSet())));

    descriptionArea = new TextArea("Description");
    descriptionArea.setValue(segment.getDescription());
    descriptionArea.setWidthFull();
    descriptionArea.setHeight("200px");

    saveButton = new Button("Save", e -> save());
    Button cancelButton = new Button("Cancel", e -> close());

    HorizontalLayout buttons = new HorizontalLayout(saveButton, cancelButton);

    VerticalLayout layout = new VerticalLayout(participantsBox, descriptionArea, buttons);
    add(layout);
  }

  private void save() {
    segment.setParticipants(
        participantsBox.getValue().stream().map(Wrestler::getName).collect(Collectors.toList()));
    segment.setDescription(descriptionArea.getValue());
    onSave.run();
    close();
  }

  public MultiselectComboBox<Wrestler> getParticipantsBox() {
    return participantsBox;
  }

  public TextArea getDescriptionArea() {
    return descriptionArea;
  }

  public Button getSaveButton() {
    return saveButton;
  }
}
