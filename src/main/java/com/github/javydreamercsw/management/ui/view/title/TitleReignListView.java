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

import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.theme.lumo.LumoUtility.Height;
import com.vaadin.flow.theme.lumo.LumoUtility.Width;
import jakarta.annotation.security.RolesAllowed;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Admin tool for directly inspecting and correcting {@link TitleReign} rows — e.g. removing reigns
 * left over from data-repair bugs (negative durations, orphaned entries) that the automated {@code
 * TitleReignRepairService} cannot safely resolve on its own.
 */
@Route("title-reign-list")
@PageTitle("Title Reigns")
@RolesAllowed("ADMIN")
@Menu(order = 16, icon = "vaadin:medal", title = "Title Reigns")
@Slf4j
public class TitleReignListView extends Main {

  private final TitleService titleService;
  private final TitleRepository titleRepository;
  private final WrestlerRepository wrestlerRepository;
  private final Grid<TitleReign> grid = new Grid<>(TitleReign.class, false);

  public TitleReignListView(
      @NonNull final TitleService titleService,
      @NonNull final TitleRepository titleRepository,
      @NonNull final WrestlerRepository wrestlerRepository) {
    this.titleService = titleService;
    this.titleRepository = titleRepository;
    this.wrestlerRepository = wrestlerRepository;

    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.SMALL,
        Height.FULL,
        Width.FULL);

    add(new ViewToolbar("Title Reigns"));

    setupGrid();
    grid.addClassNames(LumoUtility.Flex.GROW);
    add(grid);
    refreshGrid();
  }

  private void setupGrid() {
    grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    grid.addColumn(reign -> reign.getTitle() != null ? reign.getTitle().getName() : "")
        .setHeader("Title")
        .setSortable(true);
    grid.addColumn(
            reign ->
                reign.getChampions().stream()
                    .map(Wrestler::getName)
                    .collect(java.util.stream.Collectors.joining(" & ")))
        .setHeader("Champion(s)")
        .setSortable(true);
    grid.addColumn(TitleReign::getReignNumber).setHeader("Reign #").setSortable(true);
    grid.addColumn(reign -> formatInstant(reign.getStartDate()))
        .setHeader("Start Date")
        .setSortable(true);
    grid.addColumn(reign -> reign.getEndDate() != null ? formatInstant(reign.getEndDate()) : "—")
        .setHeader("End Date")
        .setSortable(true);
    grid.addColumn(
            reign -> {
              if (reign.getEndDate() == null) {
                return "—";
              }
              long days = reign.getReignLengthDays(reign.getEndDate());
              return String.valueOf(days);
            })
        .setHeader("Days")
        .setSortable(true);
    grid.addColumn(reign -> reign.getWonAtSegment() != null ? reign.getWonAtSegment().getId() : "—")
        .setHeader("Won At Segment");
    grid.addComponentColumn(
            reign -> {
              Button editButton = new Button("Edit", new Icon(VaadinIcon.EDIT));
              editButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
              editButton.addClickListener(e -> openEditDialog(reign).open());

              Button deleteButton = new Button("Delete", new Icon(VaadinIcon.TRASH));
              deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
              deleteButton.addClickListener(e -> deleteReign(reign));

              return new HorizontalLayout(editButton, deleteButton);
            })
        .setHeader("Actions");
  }

  private static String formatInstant(final Instant instant) {
    return instant == null ? "—" : instant.atZone(ZoneOffset.UTC).toLocalDate().toString();
  }

  public void refreshGrid() {
    List<TitleReign> reigns = titleService.getAllReigns();
    reigns.sort(
        Comparator.comparing((TitleReign r) -> r.getTitle() != null ? r.getTitle().getName() : "")
            .thenComparing(TitleReign::getStartDate));
    grid.setItems(reigns);
  }

  private Dialog openEditDialog(@NonNull final TitleReign reign) {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("Edit Title Reign");
    dialog.setWidth("500px");

    ComboBox<Title> titleField = new ComboBox<>("Title");
    titleField.setItems(titleRepository.findAll());
    titleField.setItemLabelGenerator(Title::getName);
    titleField.setValue(reign.getTitle());

    List<Wrestler> allWrestlers = wrestlerRepository.findAll();
    MultiSelectComboBox<Wrestler> championsField = new MultiSelectComboBox<>("Champion(s)");
    championsField.setItems(allWrestlers);
    championsField.setItemLabelGenerator(Wrestler::getName);
    championsField.setValue(new LinkedHashSet<>(reign.getChampions()));

    IntegerField reignNumberField = new IntegerField("Reign #");
    reignNumberField.setValue(reign.getReignNumber());
    reignNumberField.setMin(1);

    DatePicker startDateField = new DatePicker("Start Date");
    startDateField.setValue(toLocalDate(reign.getStartDate()));

    DatePicker endDateField = new DatePicker("End Date (blank = current reign)");
    endDateField.setValue(toLocalDate(reign.getEndDate()));
    endDateField.setClearButtonVisible(true);

    TextArea notesField = new TextArea("Notes");
    notesField.setValue(reign.getNotes() != null ? reign.getNotes() : "");
    notesField.setWidthFull();

    FormLayout formLayout = new FormLayout();
    formLayout.add(
        titleField, reignNumberField, championsField, startDateField, endDateField, notesField);

    Button saveButton = new Button("Save");
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    saveButton.addClickListener(
        e -> {
          try {
            reign.setTitle(titleField.getValue());
            reign.setChampions(new LinkedHashSet<>(championsField.getValue()));
            reign.setReignNumber(reignNumberField.getValue());
            reign.setStartDate(toInstant(startDateField.getValue()));
            reign.setEndDate(toInstant(endDateField.getValue()));
            reign.setNotes(notesField.getValue());
            titleService.saveReign(reign);
            refreshGrid();
            dialog.close();
            Notification.show("Title reign updated", 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
          } catch (Exception ex) {
            log.error("Error saving title reign", ex);
            Notification.show(
                    "Error saving title reign: " + ex.getMessage(),
                    5000,
                    Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
          }
        });

    Button cancelButton = new Button("Cancel", e -> dialog.close());

    dialog.add(formLayout);
    dialog.getFooter().add(cancelButton, saveButton);
    return dialog;
  }

  private static LocalDate toLocalDate(final Instant instant) {
    return instant == null ? null : instant.atZone(ZoneOffset.UTC).toLocalDate();
  }

  private static Instant toInstant(final LocalDate localDate) {
    return localDate == null ? null : localDate.atStartOfDay(ZoneOffset.UTC).toInstant();
  }

  private void deleteReign(@NonNull final TitleReign reign) {
    ConfirmDialog confirmDialog = new ConfirmDialog();
    confirmDialog.setHeader("Delete Title Reign");
    confirmDialog.setText(
        "Are you sure you want to delete this title reign? This cannot be undone.");
    confirmDialog.setCancelable(true);
    confirmDialog.setConfirmText("Delete");
    confirmDialog.setConfirmButtonTheme("error primary");

    confirmDialog.addConfirmListener(
        e -> {
          try {
            assert reign.getId() != null;
            titleService.deleteReign(reign.getId());
            refreshGrid();
            Notification.show("Title reign deleted", 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
          } catch (Exception ex) {
            log.error("Error deleting title reign", ex);
            Notification.show(
                    "Error deleting title reign: " + ex.getMessage(),
                    5000,
                    Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
          }
        });

    confirmDialog.open();
  }
}
