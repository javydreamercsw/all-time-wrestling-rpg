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
package com.github.javydreamercsw.management.ui.view.universe;

import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.export.CsvExportWriter;
import com.github.javydreamercsw.management.service.export.ExportCategory;
import com.github.javydreamercsw.management.service.export.ExportPayload;
import com.github.javydreamercsw.management.service.export.JsonExportWriter;
import com.github.javydreamercsw.management.service.export.UniverseExportService;
import com.github.javydreamercsw.management.service.export.WrestlerFilter;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import java.io.ByteArrayInputStream;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExportDataDialog extends Dialog {

  private static final String FORMAT_CSV = "CSV (ZIP)";
  private static final String FORMAT_JSON = "JSON";

  public ExportDataDialog(
      Universe universe,
      UniverseExportService exportService,
      CsvExportWriter csvWriter,
      JsonExportWriter jsonWriter,
      WrestlerStateRepository wrestlerStateRepository) {

    setHeaderTitle("Export Data — " + universe.getName());
    setWidth("480px");

    // --- Categories ---
    CheckboxGroup<ExportCategory> categoryGroup = new CheckboxGroup<>();
    categoryGroup.setLabel("Data to include");
    categoryGroup.setItems(ExportCategory.values());
    categoryGroup.setItemLabelGenerator(c -> formatLabel(c.name()));
    categoryGroup.select(ExportCategory.values());

    // --- Wrestler scope ---
    RadioButtonGroup<WrestlerFilter.Scope> scopeGroup = new RadioButtonGroup<>();
    scopeGroup.setLabel("Wrestlers");
    scopeGroup.setItems(WrestlerFilter.Scope.values());
    scopeGroup.setItemLabelGenerator(
        s ->
            switch (s) {
              case ALL -> "All wrestlers";
              case ACTIVE_ONLY -> "Active only";
              case MANUAL -> "Select manually";
            });
    scopeGroup.setValue(WrestlerFilter.Scope.ALL);

    MultiSelectComboBox<Wrestler> wrestlerPicker = new MultiSelectComboBox<>();
    wrestlerPicker.setPlaceholder("Choose wrestlers…");
    wrestlerPicker.setWidthFull();
    wrestlerPicker.setVisible(false);
    wrestlerPicker.setItemLabelGenerator(Wrestler::getName);

    scopeGroup.addValueChangeListener(
        e -> {
          boolean manual = e.getValue() == WrestlerFilter.Scope.MANUAL;
          wrestlerPicker.setVisible(manual);
          if (manual && wrestlerPicker.getListDataView().getItemCount() == 0) {
            wrestlerPicker.setItems(
                wrestlerStateRepository.findByUniverseId(universe.getId()).stream()
                    .map(WrestlerState::getWrestler)
                    .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                    .collect(Collectors.toList()));
          }
        });

    // --- Format ---
    RadioButtonGroup<String> formatGroup = new RadioButtonGroup<>();
    formatGroup.setLabel("Format");
    formatGroup.setItems(FORMAT_CSV, FORMAT_JSON);
    formatGroup.setValue(FORMAT_CSV);

    // --- Download button ---
    Button downloadBtn = new Button("Download");
    downloadBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    downloadBtn.setId("export-download-button");

    Anchor downloadAnchor =
        new Anchor(
            DownloadHandler.fromInputStream(
                event -> {
                  try {
                    Set<ExportCategory> categories = categoryGroup.getSelectedItems();
                    WrestlerFilter filter = buildFilter(scopeGroup.getValue(), wrestlerPicker);
                    ExportPayload payload = exportService.collect(universe, categories, filter);

                    if (FORMAT_CSV.equals(formatGroup.getValue())) {
                      byte[] data = csvWriter.write(payload, universe.getName());
                      return new DownloadResponse(
                          new ByteArrayInputStream(data),
                          universe.getName() + "-export.zip",
                          "application/zip",
                          data.length);
                    } else {
                      byte[] data = jsonWriter.write(payload, universe.getName());
                      return new DownloadResponse(
                          new ByteArrayInputStream(data),
                          universe.getName() + "-export.json",
                          "application/json",
                          data.length);
                    }
                  } catch (Exception ex) {
                    log.error("Export failed for universe {}", universe.getId(), ex);
                    return DownloadResponse.error(500);
                  }
                }),
            "");
    downloadAnchor.add(downloadBtn);

    // Disable download when no categories are selected
    categoryGroup.addValueChangeListener(e -> downloadBtn.setEnabled(!e.getValue().isEmpty()));

    VerticalLayout content =
        new VerticalLayout(
            new H4("Data to include"),
            categoryGroup,
            new H4("Wrestlers"),
            scopeGroup,
            wrestlerPicker,
            new H4("Format"),
            formatGroup);
    content.setPadding(false);
    content.setSpacing(false);
    content.getStyle().set("gap", "var(--lumo-space-s)");

    add(content);
    getFooter().add(downloadAnchor, new Button("Close", e -> close()));
  }

  private WrestlerFilter buildFilter(
      WrestlerFilter.Scope scope, MultiSelectComboBox<Wrestler> picker) {
    return switch (scope) {
      case ALL -> WrestlerFilter.all();
      case ACTIVE_ONLY -> WrestlerFilter.activeOnly();
      case MANUAL ->
          WrestlerFilter.manual(
              picker.getSelectedItems().stream().map(Wrestler::getId).collect(Collectors.toSet()));
    };
  }

  private String formatLabel(String enumName) {
    return enumName.replace('_', ' ').substring(0, 1).toUpperCase()
        + enumName.replace('_', ' ').substring(1).toLowerCase();
  }
}
