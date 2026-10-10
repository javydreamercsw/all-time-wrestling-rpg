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
package com.github.javydreamercsw.management.ui.view.title;

import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.ui.component.ImageUploadComponent;
import com.github.javydreamercsw.management.domain.title.TitleOpportunity;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.title.TitleOpportunityService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

/**
 * Create/edit dialog for briefcases (ATW-jpki), matching the TitleFormDialog pattern: fields for
 * name/holder/gender/dates plus the shared image upload. Create mode builds a HELD case; edit mode
 * only opens for HELD rows (the service rejects otherwise).
 */
@Slf4j
public class BriefcaseFormDialog extends Dialog {

  private final TitleOpportunityService titleOpportunityService;
  private final WrestlerRepository wrestlerRepository;
  private final ImageStorageService imageStorageService;
  private final boolean createMode;

  private final TextField name = new TextField("Name");
  private final ComboBox<Wrestler> holder = new ComboBox<>("Holder");
  private final ComboBox<Gender> gender = new ComboBox<>("Division");
  private final DatePicker earnedAt = new DatePicker("Earned");
  private final DatePicker expiryDate = new DatePicker("Cashable Until");
  private final TextField imageUrl = new TextField("Image URL");
  private final Image previewImage = new Image();
  private final Span statusNote = new Span();
  private TitleOpportunity opportunity;

  public BriefcaseFormDialog(
      @Nullable TitleOpportunity existing,
      TitleOpportunityService titleOpportunityService,
      WrestlerRepository wrestlerRepository,
      UniverseRepository universeRepository,
      ImageStorageService imageStorageService,
      Runnable onSave) {
    this.opportunity = existing != null ? existing : new TitleOpportunity();
    this.titleOpportunityService = titleOpportunityService;
    this.wrestlerRepository = wrestlerRepository;
    this.imageStorageService = imageStorageService;
    this.createMode = existing == null;

    setHeaderTitle(createMode ? "New Briefcase" : "Edit Briefcase: " + opportunity.getName());
    setWidth("560px");

    statusNote.getStyle().set("color", "var(--lumo-secondary-text-color)");
    statusNote.setText(
        createMode
            ? "Creates a HELD case — one HELD briefcase per wrestler."
            : "Only HELD cases are editable; CASHED_IN/EXPIRED rows are history.");

    List<Wrestler> activeWrestlers = wrestlerRepository.findAllByActiveTrue();
    holder.setItems(activeWrestlers);
    holder.setItemLabelGenerator(Wrestler::getName);
    holder.setAllowCustomValue(false);
    holder.setRequired(true);

    gender.setItems(Gender.values());
    gender.setAllowCustomValue(false);

    name.setRequired(true);

    FormLayout form = new FormLayout(name, holder, gender, earnedAt, expiryDate, imageUrl);
    form.setColspan(name, 2);
    form.setColspan(holder, 2);

    // Image upload — the TitleFormDialog pattern.
    previewImage.setAlt("Briefcase Preview");
    previewImage.setHeight("100px");
    previewImage.setVisible(false);
    ImageUploadComponent imageUpload =
        new ImageUploadComponent(
            imageStorageService,
            url -> {
              imageUrl.setValue(url);
              updatePreviewImage(url);
            });
    imageUpload.setUploadButtonText("Upload Image");
    HorizontalLayout imageLayout = new HorizontalLayout(previewImage, imageUpload);
    imageLayout.setAlignItems(FlexComponent.Alignment.BASELINE);

    // Seed values.
    name.setValue(opportunity.getName() != null ? opportunity.getName() : "");
    if (opportunity.getWrestler() != null) {
      holder.setValue(opportunity.getWrestler());
    }
    if (opportunity.getGender() != null) {
      gender.setValue(opportunity.getGender());
    }
    if (opportunity.getEarnedAt() != null) {
      earnedAt.setValue(opportunity.getEarnedAt());
    }
    if (opportunity.getExpiryDate() != null) {
      expiryDate.setValue(opportunity.getExpiryDate());
    }
    if (opportunity.getImageUrl() != null) {
      imageUrl.setValue(opportunity.getImageUrl());
      updatePreviewImage(opportunity.getImageUrl());
    }
    if (!createMode) {
      earnedAt.setRequiredIndicatorVisible(false);
    }

    Button cancel = new Button("Cancel", e -> close());
    Button save = new Button("Save", e -> save(onSave));
    save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    save.setId("briefcase-save");

    HorizontalLayout footer = new HorizontalLayout(cancel, save);
    footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
    footer.setWidthFull();

    add(statusNote, form, imageLayout, footer);
  }

  private void save(Runnable onSave) {
    try {
      if (createMode) {
        Wrestler chosen = holder.getValue();
        if (chosen == null || name.getValue().isBlank()) {
          Notification.show("Name and holder are required.", 3000, Notification.Position.MIDDLE);
          return;
        }
        Universe universe = chosen.getDefaultState().map(s -> s.getUniverse()).orElse(null);
        titleOpportunityService.adminCreate(
            name.getValue().trim(),
            chosen,
            universe,
            gender.getValue(),
            earnedAt.getValue() != null ? earnedAt.getValue() : LocalDate.now(),
            expiryDate.getValue(),
            imageUrl.getValue().isEmpty() ? null : imageUrl.getValue());
      } else {
        titleOpportunityService.adminUpdate(
            opportunity.getId(),
            name.getValue().trim(),
            expiryDate.getValue(),
            imageUrl.getValue().isEmpty() ? null : imageUrl.getValue(),
            earnedAt.getValue());
        // Holder and division are editable on a HELD case (ATW-jpki): reassignment keeps the
        // one-HELD invariant inside the service; division no-op when unchanged.
        if (holder.getValue() != null
            && !holder.getValue().getId().equals(opportunity.getWrestler().getId())) {
          titleOpportunityService.adminUpdateHolder(opportunity.getId(), holder.getValue().getId());
        }
        titleOpportunityService.adminUpdateDivision(
            opportunity.getId(), gender.getValue() != null ? gender.getValue() : null);
      }
      close();
      onSave.run();
      Notification.show("Briefcase saved.", 3000, Notification.Position.BOTTOM_CENTER)
          .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    } catch (Exception ex) {
      log.error("Error saving briefcase", ex);
      Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
  }

  private void updatePreviewImage(String url) {
    if (url != null && !url.isBlank()) {
      previewImage.setSrc(url);
      previewImage.setVisible(true);
    } else {
      previewImage.setVisible(false);
    }
  }
}
