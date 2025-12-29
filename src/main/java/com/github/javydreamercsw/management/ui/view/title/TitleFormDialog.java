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
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.ranking.TierRecalculationService;
import com.github.javydreamercsw.management.service.title.TitleService;
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
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;

public class TitleFormDialog extends Dialog {

  private final Title title;
  private final Binder<Title> binder = new Binder<>(Title.class);
  private final WrestlerRepository wrestlerRepository;
  private final TextField name;
  private final TextArea description;
  private final ComboBox<WrestlerTier> tier;
  private final ComboBox<Gender> gender;
  private final MultiSelectComboBox<Wrestler> champion;

  public TitleFormDialog(
      @NonNull TitleService titleService,
      @NonNull WrestlerRepository wrestlerRepository,
      @NonNull TierRecalculationService tierRecalculationService,
      @NonNull Title title,
      @NonNull Runnable onSave,
      @NonNull SecurityUtils securityUtils) {
    this.title = title;
    this.wrestlerRepository = wrestlerRepository;
    name = new TextField("Name");
    name.setReadOnly(!securityUtils.canEdit());
    description = new TextArea("Description");
    description.setReadOnly(!securityUtils.canEdit());
    tier = new ComboBox<>("Tier");
    tier.setItems(
        Arrays.stream(WrestlerTier.values())
            .sorted(Comparator.comparing(WrestlerTier::name))
            .collect(Collectors.toList()));
    tier.setReadOnly(!securityUtils.canEdit());
    gender = new ComboBox<>("Gender");
    gender.setItems(
        Arrays.stream(Gender.values())
            .sorted(Comparator.comparing(Gender::name))
            .collect(Collectors.toList()));
    gender.setReadOnly(!securityUtils.canEdit());
    ComboBox<ChampionshipType> championshipType = new ComboBox<>("Championship Type");
    championshipType.setItems(
        Arrays.stream(ChampionshipType.values())
            .sorted(Comparator.comparing(ChampionshipType::name))
            .collect(Collectors.toList()));
    championshipType.setReadOnly(!securityUtils.canEdit());
    Checkbox isActive = new Checkbox("Active");
    isActive.setReadOnly(!securityUtils.canEdit());
    champion = new MultiSelectComboBox<>("Champion(s)");
    champion.setItemLabelGenerator(Wrestler::getName);
    champion.setReadOnly(!securityUtils.canEdit());

    binder.forField(name).asRequired().bind(Title::getName, Title::setName);
    binder.bind(description, Title::getDescription, Title::setDescription);
    binder.forField(tier).asRequired().bind(Title::getTier, Title::setTier);
    binder.bind(gender, Title::getGender, Title::setGender);
    binder
        .forField(championshipType)
        .asRequired()
        .bind(Title::getChampionshipType, Title::setChampionshipType);
    binder.bind(isActive, Title::getIsActive, Title::setIsActive);

    Runnable populateChampions =
        () -> {
          WrestlerTier currentTier = tier.getValue();
          if (currentTier != null) {
            // Create a temporary title object to avoid binder timing issues in listeners.
            Title tempTitle = new Title();
            tempTitle.setTier(currentTier);
            tempTitle.setGender(gender.getValue());

            var selected = champion.getValue();
            List<Wrestler> eligible =
                wrestlerRepository.findAll().stream()
                    .peek(tierRecalculationService::recalculateTier)
                    .filter(w -> titleService.isWrestlerEligible(w, tempTitle))
                    .sorted(Comparator.comparing(Wrestler::getName))
                    .collect(Collectors.toList());
            champion.setItems(eligible);
            champion.setValue(selected);
          }
        };

    tier.addValueChangeListener(event -> populateChampions.run());
    gender.addValueChangeListener(event -> populateChampions.run());

    FormLayout formLayout =
        new FormLayout(name, description, tier, gender, championshipType, isActive, champion);
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
    saveButton.setVisible(securityUtils.canEdit());
    Button cancelButton = new Button("Cancel", event -> close());
    getFooter().add(new HorizontalLayout(saveButton, cancelButton));

    binder.readBean(this.title);

    // Initial population for existing titles.
    if (this.title.getTier() != null) {
      populateChampions.run();
    }

    if (title.getChampion() != null) {
      champion.setValue(title.getChampion());
    }
  }
}
