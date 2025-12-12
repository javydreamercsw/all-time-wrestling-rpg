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
package com.github.javydreamercsw.management.ui.view.title;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

public class TitleFormDialog extends Dialog {

  private final Title title;
  private final Binder<Title> binder = new Binder<>(Title.class);

  public TitleFormDialog(
      TitleService titleService, WrestlerService wrestlerService, Title title, Runnable onSave) {
    this.title = title;

    TextField name = new TextField("Name");
    TextArea description = new TextArea("Description");
    ComboBox<WrestlerTier> tier = new ComboBox<>("Tier");
    tier.setItems(
        Arrays.stream(WrestlerTier.values())
            .sorted(Comparator.comparing(WrestlerTier::name))
            .collect(Collectors.toList()));
    ComboBox<Gender> gender = new ComboBox<>("Gender");
    gender.setItems(
        Arrays.stream(Gender.values())
            .sorted(Comparator.comparing(Gender::name))
            .collect(Collectors.toList()));
    Checkbox isActive = new Checkbox("Active");
    MultiSelectComboBox<Wrestler> champion = new MultiSelectComboBox<>("Champion(s)");
    champion.setItems(
        wrestlerService.findAll().stream()
            .sorted(Comparator.comparing(Wrestler::getName))
            .collect(Collectors.toList()));
    champion.setItemLabelGenerator(Wrestler::getName);

    binder.forField(name).asRequired().bind(Title::getName, Title::setName);
    binder.bind(description, Title::getDescription, Title::setDescription);
    binder.forField(tier).asRequired().bind(Title::getTier, Title::setTier);
    binder.bind(gender, Title::getGender, Title::setGender);
    binder.bind(isActive, Title::getIsActive, Title::setIsActive);

    if (title.getChampion() != null) {
      champion.setValue(title.getChampion());
    }

    FormLayout formLayout = new FormLayout(name, description, tier, gender, isActive, champion);
    add(formLayout);

    Button saveButton =
        new Button(
            "Save",
            event -> {
              if (binder.writeBeanIfValid(this.title)) {
                titleService.save(this.title);
                if (!new ArrayList<>(champion.getValue()).equals(this.title.getChampion())) {
                  if (champion.getValue().isEmpty()) {
                    assert this.title.getId() != null;
                    titleService.vacateTitle(this.title.getId());
                  } else {
                    titleService.awardTitleTo(this.title, new ArrayList<>(champion.getValue()));
                  }
                }
                onSave.run();
                close();
              }
            });
    Button cancelButton = new Button("Cancel", event -> close());
    getFooter().add(new HorizontalLayout(saveButton, cancelButton));

    binder.readBean(this.title);
  }
}
