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

import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.Binder;
import java.util.Comparator;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.springframework.data.domain.Pageable;

public class EditShowDetailsDialog extends Dialog {

  private final ShowService showService;
  private final ShowTypeService showTypeService;
  private final SeasonService seasonService;
  private final ShowTemplateService showTemplateService;
  private final Show show;
  private final Binder<Show> binder = new Binder<>(Show.class);

  private final TextArea descriptionField = new TextArea("Description");
  private final ComboBox<ShowType> typeField = new ComboBox<>("Show Type");
  private final ComboBox<Season> seasonField = new ComboBox<>("Season");
  private final ComboBox<ShowTemplate> templateField = new ComboBox<>("Template");
  private final DatePicker showDateField = new DatePicker("Show Date");

  public EditShowDetailsDialog(
      @NonNull ShowService showService,
      @NonNull ShowTypeService showTypeService,
      @NonNull SeasonService seasonService,
      @NonNull ShowTemplateService showTemplateService,
      @NonNull Show show) {
    this.showService = showService;
    this.showTypeService = showTypeService;
    this.seasonService = seasonService;
    this.showTemplateService = showTemplateService;
    this.show = show;

    setHeaderTitle("Edit Show Details");
    setModal(true);
    setResizable(false);
    setCloseOnEsc(true);
    setCloseOnOutsideClick(false);

    // Configure form fields
    descriptionField.setValue(show.getDescription() != null ? show.getDescription() : "");
    descriptionField.setWidthFull();

    typeField.setItems(
        showTypeService.findAll().stream()
            .sorted(Comparator.comparing(ShowType::getName))
            .collect(Collectors.toList()));
    typeField.setItemLabelGenerator(ShowType::getName);
    typeField.setValue(show.getType());
    typeField.setRequired(true);
    typeField.setWidthFull();

    seasonField.setItems(
        seasonService.getAllSeasons(Pageable.unpaged()).getContent().stream()
            .sorted(Comparator.comparing(Season::getName))
            .collect(Collectors.toList()));
    seasonField.setItemLabelGenerator(Season::getName);
    seasonField.setValue(show.getSeason());
    seasonField.setClearButtonVisible(true);
    seasonField.setWidthFull();

    templateField.setItems(
        showTemplateService.findAll().stream()
            .sorted(Comparator.comparing(ShowTemplate::getName))
            .collect(Collectors.toList()));
    templateField.setItemLabelGenerator(ShowTemplate::getName);
    templateField.setValue(show.getTemplate());
    templateField.setClearButtonVisible(true);
    templateField.setWidthFull();

    showDateField.setValue(show.getShowDate());
    showDateField.setClearButtonVisible(true);
    showDateField.setWidthFull();

    // Bind fields
    binder.bind(descriptionField, Show::getDescription, Show::setDescription);
    binder.bind(typeField, Show::getType, Show::setType);
    binder.bind(seasonField, Show::getSeason, Show::setSeason);
    binder.bind(templateField, Show::getTemplate, Show::setTemplate);
    binder.bind(showDateField, Show::getShowDate, Show::setShowDate);

    // Buttons
    Button saveButton = new Button("Save", e -> saveShowDetails());
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    Button cancelButton = new Button("Cancel", e -> close());

    HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
    buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
    buttonLayout.setWidthFull();

    FormLayout formLayout =
        new FormLayout(descriptionField, typeField, seasonField, templateField, showDateField);
    formLayout.setWidthFull();

    VerticalLayout dialogLayout = new VerticalLayout(formLayout, buttonLayout);
    dialogLayout.setSpacing(true);
    dialogLayout.setPadding(false);
    dialogLayout.setWidth("600px");

    add(dialogLayout);
  }

  private void saveShowDetails() {
    if (binder.isValid()) {
      try {
        showService.updateShow(
            show.getId(),
            show.getName(), // Name is handled by EditShowNameDialog
            descriptionField.getValue(),
            typeField.getValue().getId(),
            showDateField.getValue(),
            seasonField.getValue() != null ? seasonField.getValue().getId() : null,
            templateField.getValue() != null ? templateField.getValue().getId() : null);
        Notification.show(
                "Show details updated successfully!", 3000, Notification.Position.BOTTOM_END)
            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        close();
      } catch (Exception e) {
        Notification.show(
                "Error updating show details: " + e.getMessage(),
                5000,
                Notification.Position.MIDDLE)
            .addThemeVariants(NotificationVariant.LUMO_ERROR);
      }
    } else {
      Notification.show(
              "Please correct the errors in the form.", 3000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
  }
}
