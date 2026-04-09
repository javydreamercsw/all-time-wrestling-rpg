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
package com.github.javydreamercsw.management.ui.view.admin;

import com.github.javydreamercsw.management.domain.relationship.RelationshipType;
import com.github.javydreamercsw.management.domain.relationship.WrestlerRelationship;
import com.github.javydreamercsw.management.domain.relationship.WrestlerRelationshipRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.relationship.WrestlerRelationshipService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.Comparator;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@SpringComponent
@UIScope
@Slf4j
public class WrestlerRelationshipManagementView extends VerticalLayout {

  private final WrestlerRelationshipService relationshipService;
  private final WrestlerRelationshipRepository relationshipRepository;
  private final WrestlerService wrestlerService;
  private final Grid<WrestlerRelationship> grid = new Grid<>(WrestlerRelationship.class, false);

  public WrestlerRelationshipManagementView(
      WrestlerRelationshipService relationshipService,
      WrestlerRelationshipRepository relationshipRepository,
      WrestlerService wrestlerService) {
    this.relationshipService = relationshipService;
    this.relationshipRepository = relationshipRepository;
    this.wrestlerService = wrestlerService;
    initializeUI();
  }

  private void initializeUI() {
    setSizeFull();
    setPadding(true);
    setSpacing(true);

    HorizontalLayout header = new HorizontalLayout();
    header.setWidthFull();
    header.setJustifyContentMode(JustifyContentMode.BETWEEN);
    header.setAlignItems(Alignment.CENTER);

    header.add(new H3("Wrestler Relationships"));

    Button addRelationshipButton = new Button("Add Relationship", new Icon(VaadinIcon.PLUS));
    addRelationshipButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    addRelationshipButton.addClickListener(e -> openRelationshipDialog(null));
    header.add(addRelationshipButton);

    add(header);

    grid.addColumn(r -> r.getWrestler1().getName()).setHeader("Wrestler 1").setSortable(true);
    grid.addColumn(r -> r.getType().getDisplayName()).setHeader("Type").setSortable(true);
    grid.addColumn(r -> r.getWrestler2().getName()).setHeader("Wrestler 2").setSortable(true);
    grid.addColumn(WrestlerRelationship::getLevel).setHeader("Level").setSortable(true);
    grid.addColumn(r -> r.getIsStoryline() ? "Yes" : "No").setHeader("Storyline");

    grid.addComponentColumn(this::createActionButtons).setHeader("Actions");

    grid.addClassNames(LumoUtility.Border.ALL, LumoUtility.BorderRadius.MEDIUM);
    refresh();

    add(grid);
  }

  private HorizontalLayout createActionButtons(WrestlerRelationship relationship) {
    Button editButton = new Button(new Icon(VaadinIcon.EDIT));
    editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    editButton.addClickListener(e -> openRelationshipDialog(relationship));

    Button deleteButton = new Button(new Icon(VaadinIcon.TRASH));
    deleteButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
    deleteButton.addClickListener(
        e -> {
          relationshipRepository.delete(relationship);
          refresh();
          Notification.show("Relationship deleted");
        });

    return new HorizontalLayout(editButton, deleteButton);
  }

  private void openRelationshipDialog(WrestlerRelationship relationship) {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle(relationship == null ? "Add Relationship" : "Edit Relationship");

    FormLayout formLayout = new FormLayout();

    ComboBox<Wrestler> w1Combo = new ComboBox<>("Wrestler 1");
    w1Combo.setItems(
        wrestlerService.findAll().stream()
            .sorted(Comparator.comparing(Wrestler::getName))
            .collect(Collectors.toList()));
    w1Combo.setItemLabelGenerator(Wrestler::getName);
    w1Combo.setRequired(true);

    ComboBox<Wrestler> w2Combo = new ComboBox<>("Wrestler 2");
    w2Combo.setItems(
        wrestlerService.findAll().stream()
            .sorted(Comparator.comparing(Wrestler::getName))
            .collect(Collectors.toList()));
    w2Combo.setItemLabelGenerator(Wrestler::getName);
    w2Combo.setRequired(true);

    ComboBox<RelationshipType> typeCombo = new ComboBox<>("Type");
    typeCombo.setItems(RelationshipType.values());
    typeCombo.setItemLabelGenerator(RelationshipType::getDisplayName);
    typeCombo.setRequired(true);

    IntegerField levelField = new IntegerField("Level (0-100)");
    levelField.setMin(0);
    levelField.setMax(100);
    levelField.setValue(50);

    Checkbox isStorylineCheckbox = new Checkbox("Storyline Relationship");

    TextArea notesArea = new TextArea("Notes");
    notesArea.setWidthFull();

    if (relationship != null) {
      w1Combo.setValue(relationship.getWrestler1());
      w2Combo.setValue(relationship.getWrestler2());
      typeCombo.setValue(relationship.getType());
      levelField.setValue(relationship.getLevel());
      isStorylineCheckbox.setValue(relationship.getIsStoryline());
      notesArea.setValue(relationship.getNotes() != null ? relationship.getNotes() : "");

      // Can't change wrestlers once created to avoid unique constraint issues easily
      w1Combo.setEnabled(false);
      w2Combo.setEnabled(false);
    }

    formLayout.add(w1Combo, w2Combo, typeCombo, levelField, isStorylineCheckbox, notesArea);
    formLayout.setColspan(notesArea, 2);

    Button saveButton =
        new Button(
            "Save",
            e -> {
              if (w1Combo.getValue() == null
                  || w2Combo.getValue() == null
                  || typeCombo.getValue() == null) {
                Notification.show("Please fill in all required fields");
                return;
              }
              try {
                relationshipService.createOrUpdateRelationship(
                    w1Combo.getValue().getId(),
                    w2Combo.getValue().getId(),
                    typeCombo.getValue(),
                    levelField.getValue(),
                    isStorylineCheckbox.getValue(),
                    notesArea.getValue());
                dialog.close();
                refresh();
                Notification.show("Relationship saved");
              } catch (Exception ex) {
                Notification.show("Error saving relationship: " + ex.getMessage());
              }
            });
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    Button cancelButton = new Button("Cancel", e -> dialog.close());

    dialog.getFooter().add(cancelButton, saveButton);
    dialog.add(formLayout);
    dialog.open();
  }

  public void refresh() {
    grid.setItems(relationshipRepository.findAllWithWrestlers());
  }
}
