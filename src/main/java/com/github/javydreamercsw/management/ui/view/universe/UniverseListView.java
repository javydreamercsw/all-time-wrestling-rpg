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

import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.Universe.UniverseType;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.export.CsvExportWriter;
import com.github.javydreamercsw.management.service.export.ImageExportService;
import com.github.javydreamercsw.management.service.export.JsonExportWriter;
import com.github.javydreamercsw.management.service.export.UniverseExportService;
import com.github.javydreamercsw.management.service.universe.UniverseMembershipService;
import com.github.javydreamercsw.management.service.universe.UniverseService;
import com.github.javydreamercsw.management.service.universe.UniverseSettingsService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.theme.lumo.LumoUtility.Height;
import com.vaadin.flow.theme.lumo.LumoUtility.Width;
import jakarta.annotation.security.RolesAllowed;
import java.io.ByteArrayInputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Route("universe-list")
@PageTitle("Universes")
@RolesAllowed("ADMIN")
@Menu(order = 0, icon = "vaadin:globe", title = "Universes")
@Slf4j
public class UniverseListView extends Main {

  private static final DateTimeFormatter DATE_FMT =
      DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault());

  private final UniverseService universeService;
  private final UniverseMembershipService membershipService;
  private final AccountService accountService;
  private final UniverseSettingsService settingsService;
  private final WrestlerRepository wrestlerRepository;
  private final UniverseExportService exportService;
  private final CsvExportWriter csvWriter;
  private final JsonExportWriter jsonWriter;
  private final WrestlerStateRepository wrestlerStateRepository;
  private final ImageExportService imageExportService;
  public final Grid<Universe> grid = new Grid<>(Universe.class, false);

  public UniverseListView(
      final UniverseService universeService,
      final UniverseMembershipService membershipService,
      final AccountService accountService,
      final UniverseSettingsService settingsService,
      final WrestlerRepository wrestlerRepository,
      final UniverseExportService exportService,
      final CsvExportWriter csvWriter,
      final JsonExportWriter jsonWriter,
      final WrestlerStateRepository wrestlerStateRepository,
      final ImageExportService imageExportService) {
    this.universeService = universeService;
    this.membershipService = membershipService;
    this.accountService = accountService;
    this.settingsService = settingsService;
    this.wrestlerRepository = wrestlerRepository;
    this.exportService = exportService;
    this.csvWriter = csvWriter;
    this.jsonWriter = jsonWriter;
    this.wrestlerStateRepository = wrestlerStateRepository;
    this.imageExportService = imageExportService;

    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.SMALL,
        Height.FULL,
        Width.FULL);

    Button createButton = new Button("Create Universe", new Icon(VaadinIcon.PLUS));
    createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    createButton.addClickListener(e -> openCreateDialog().open());

    int imageCount = 0;
    try {
      imageCount = imageExportService.countImages();
    } catch (Exception ex) {
      log.warn("Could not count images for button label", ex);
    }
    String exportLabel = imageCount > 0 ? "Export Images (" + imageCount + ")" : "Export Images";
    Button exportImagesBtn = new Button(exportLabel, new Icon(VaadinIcon.PICTURE));
    exportImagesBtn.setId("export-images-button");
    final int finalImageCount = imageCount;
    Anchor exportImagesAnchor =
        new Anchor(
            DownloadHandler.fromInputStream(
                event -> {
                  try {
                    byte[] data = imageExportService.exportImages();
                    return new DownloadResponse(
                        new ByteArrayInputStream(data),
                        "images-export.zip",
                        "application/zip",
                        data.length);
                  } catch (Exception ex) {
                    log.error("Image export failed", ex);
                    return DownloadResponse.error(500);
                  }
                }),
            "");
    exportImagesBtn.addClickListener(
        e ->
            Notification.show(
                "Downloading " + finalImageCount + " image(s)",
                3000,
                Notification.Position.BOTTOM_END));
    exportImagesAnchor.add(exportImagesBtn);

    add(new ViewToolbar("Universe List", ViewToolbar.group(exportImagesAnchor, createButton)));

    setupGrid();
    grid.addClassNames(LumoUtility.Flex.GROW);
    add(grid);
    refreshGrid();
  }

  private void setupGrid() {
    grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    grid.addColumn(Universe::getName).setHeader("Name").setSortable(true);
    grid.addColumn(u -> formatType(u.getType())).setHeader("Type").setSortable(true);
    grid.addColumn(u -> u.getCreationDate() != null ? DATE_FMT.format(u.getCreationDate()) : "")
        .setHeader("Created")
        .setSortable(true);

    grid.addComponentColumn(
            universe -> {
              Button editButton = new Button("Edit", new Icon(VaadinIcon.EDIT));
              editButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
              editButton.addClickListener(e -> openEditDialog(universe).open());

              Button exportButton = new Button("Export Data", new Icon(VaadinIcon.DOWNLOAD));
              exportButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
              exportButton.addClickListener(
                  e ->
                      new ExportDataDialog(
                              universe,
                              exportService,
                              csvWriter,
                              jsonWriter,
                              wrestlerStateRepository)
                          .open());

              Button deleteButton = new Button("Delete", new Icon(VaadinIcon.TRASH));
              deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
              deleteButton.addClickListener(e -> confirmDelete(universe));

              return new HorizontalLayout(editButton, exportButton, deleteButton);
            })
        .setHeader("Actions");
  }

  public void refreshGrid() {
    grid.setItems(universeService.findAll());
  }

  UniverseFormDialog openCreateDialog() {
    Universe newUniverse = Universe.builder().type(UniverseType.GLOBAL).build();
    UniverseFormDialog dialog =
        new UniverseFormDialog(universeService, newUniverse, this::refreshGrid);
    dialog.setHeaderTitle("Create Universe");
    return dialog;
  }

  UniverseFormDialog openEditDialog(final Universe universe) {
    UniverseFormDialog dialog =
        new UniverseFormDialog(
            universeService,
            membershipService,
            accountService,
            settingsService,
            wrestlerRepository,
            universe,
            this::refreshGrid);
    dialog.setHeaderTitle("Edit Universe: " + universe.getName());
    return dialog;
  }

  private void confirmDelete(final Universe universe) {
    List<String> blockers = universeService.getDeletionBlockers(universe);

    if (!blockers.isEmpty()) {
      showDeletionBlockedDialog(universe, blockers);
      return;
    }

    ConfirmDialog confirm = new ConfirmDialog();
    confirm.setHeader("Delete Universe");
    confirm.setText(
        "Are you sure you want to delete universe '"
            + universe.getName()
            + "'? This cannot be undone.");
    confirm.setCancelable(true);
    confirm.setConfirmText("Delete");
    confirm.setConfirmButtonTheme("error primary");

    confirm.addConfirmListener(
        e -> {
          try {
            assert universe.getId() != null;
            universeService.delete(universe.getId());
            refreshGrid();
            Notification.show("Universe deleted.", 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
          } catch (Exception ex) {
            log.error("Error deleting universe", ex);
            Notification.show(
                    "Error deleting universe: " + ex.getMessage(),
                    5000,
                    Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
          }
        });

    confirm.open();
  }

  private void showDeletionBlockedDialog(final Universe universe, final List<String> blockers) {
    Dialog warning = new Dialog();
    warning.setHeaderTitle("Cannot Delete Universe");

    Paragraph intro =
        new Paragraph(
            "Universe '"
                + universe.getName()
                + "' cannot be deleted because it is still referenced by:");

    UnorderedList list = new UnorderedList();
    blockers.stream().map(b -> new com.vaadin.flow.component.html.ListItem(b)).forEach(list::add);

    Paragraph outro =
        new Paragraph("Remove or reassign these entities before deleting this universe.");

    warning.add(intro, list, outro);
    warning.getFooter().add(new Button("Close", e -> warning.close()));
    warning.open();
  }

  private static String formatType(final UniverseType type) {
    if (type == null) {
      return "";
    }
    return switch (type) {
      case GLOBAL -> "Global";
      case LEAGUE -> "League";
      case CAMPAIGN -> "Campaign";
    };
  }
}
