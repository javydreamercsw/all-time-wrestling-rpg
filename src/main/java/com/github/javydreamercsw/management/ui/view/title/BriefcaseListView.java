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
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.title.TitleOpportunity;
import com.github.javydreamercsw.management.domain.title.TitleOpportunityRepository;
import com.github.javydreamercsw.management.domain.title.TitleOpportunityStatus;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.title.TitleOpportunityService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Briefcase CRUD management view (ATW-jpki), following the TitleListView pattern: create, edit and
 * void (manual HELD → VOIDED cancel) for TitleOpportunities. CASHED_IN/EXPIRED rows are history —
 * they render read-only and the service rejects edits.
 */
@Route("briefcase-list")
@PageTitle("Briefcases")
@PermitAll
@Menu(order = 5, icon = "vaadin:briefcase", title = "Briefcases")
@Slf4j
public class BriefcaseListView extends Main {

  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private final TitleOpportunityService titleOpportunityService;
  private final TitleOpportunityRepository opportunityRepository;
  private final WrestlerRepository wrestlerRepository;
  private final SecurityUtils securityUtils;
  private final ImageStorageService imageStorageService;
  private final TextField searchField = new TextField();
  private final ComboBox<TitleOpportunityStatus> statusFilter = new ComboBox<>("Status");
  @Getter private final Grid<TitleOpportunity> grid = new Grid<>(TitleOpportunity.class, false);
  private List<TitleOpportunity> allCache = List.of();

  public BriefcaseListView(
      @NonNull TitleOpportunityService titleOpportunityService,
      @NonNull TitleOpportunityRepository opportunityRepository,
      @NonNull WrestlerRepository wrestlerRepository,
      @NonNull SecurityUtils securityUtils,
      @NonNull ImageStorageService imageStorageService) {
    this.titleOpportunityService = titleOpportunityService;
    this.opportunityRepository = opportunityRepository;
    this.wrestlerRepository = wrestlerRepository;
    this.securityUtils = securityUtils;
    this.imageStorageService = imageStorageService;

    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.SMALL,
        LumoUtility.Height.FULL,
        LumoUtility.Width.FULL);

    Button createButton = new Button("New Briefcase", new Icon(VaadinIcon.PLUS));
    createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    createButton.addClickListener(e -> openCreateDialog());
    createButton.setVisible(securityUtils.canCreate());
    createButton.setId("briefcase-new");

    statusFilter.setItems(TitleOpportunityStatus.values());
    statusFilter.setPlaceholder("All statuses");
    statusFilter.setClearButtonVisible(true);
    statusFilter.addValueChangeListener(e -> refreshGrid());

    searchField.setPlaceholder("Search briefcases...");
    searchField.setClearButtonVisible(true);
    searchField.setValueChangeMode(ValueChangeMode.LAZY);
    searchField.addValueChangeListener(e -> refreshGrid());
    searchField.setId("briefcase-search-field");

    add(
        new ViewToolbar(
            "Briefcase List", ViewToolbar.group(statusFilter, searchField, createButton)));

    setupGrid();
    grid.addClassNames(LumoUtility.Flex.GROW);
    Div gridWrapper = new Div(grid);
    gridWrapper.addClassName("grid-scroll-container");
    add(gridWrapper);
    refreshGrid();
  }

  private void setupGrid() {
    grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    // Same full-height rule as TitleListView (ATW-n97c): without it the grid collapses to its
    // ~135px default viewport and scrolls internally.
    grid.setSizeFull();
    grid.setMinWidth("950px");
    grid.addColumn(
            o -> o.getImageUrl() != null && !o.getImageUrl().isBlank() ? o.getImageUrl() : "")
        .setHeader("Image")
        .setAutoWidth(true)
        .setVisible(false); // rendered as thumbnail below when present
    grid.addComponentColumn(this::thumbnail).setHeader("").setAutoWidth(true);
    grid.addColumn(TitleOpportunity::getName).setHeader("Name").setSortable(true);
    grid.addColumn(o -> o.getWrestler() != null ? o.getWrestler().getName() : "—")
        .setHeader("Holder")
        .setSortable(true);
    grid.addColumn(o -> o.getGender() != null ? o.getGender().name() : "")
        .setHeader("Division")
        .setSortable(true);
    grid.addColumn(o -> o.getStatus().name()).setHeader("Status").setSortable(true);
    grid.addColumn(o -> o.getEarnedAt() != null ? DATE_FMT.format(o.getEarnedAt()) : "—")
        .setHeader("Earned")
        .setSortable(true);
    grid.addColumn(o -> o.getExpiryDate() != null ? DATE_FMT.format(o.getExpiryDate()) : "—")
        .setHeader("Cashable Until")
        .setSortable(true);
    grid.addComponentColumn(this::actions).setHeader("Actions").setAutoWidth(true);
  }

  private Component thumbnail(TitleOpportunity o) {
    if (o.getImageUrl() == null || o.getImageUrl().isBlank()) {
      Icon briefcase = new Icon(VaadinIcon.BRIEFCASE);
      briefcase.setColor("var(--lumo-secondary-text-color)");
      return briefcase;
    }
    Image image = new Image(o.getImageUrl(), "Briefcase Image");
    image.setHeight("48px");
    image.setWidth("48px");
    image.addClassNames(LumoUtility.BorderRadius.MEDIUM);
    return image;
  }

  private Component actions(TitleOpportunity o) {
    Button editButton = new Button("Edit", new Icon(VaadinIcon.EDIT));
    editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
    editButton.setVisible(securityUtils.canEdit() && o.isHeld());
    editButton.addClickListener(e -> openEditDialog(o));

    Button voidButton = new Button("Void", new Icon(VaadinIcon.CLOSE_CIRCLE));
    voidButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
    voidButton.setVisible(securityUtils.canEdit() && o.isHeld());
    voidButton.addClickListener(e -> voidCase(o));

    HorizontalLayout row = new HorizontalLayout(editButton, voidButton);
    row.setAlignItems(FlexComponent.Alignment.CENTER);
    row.setSpacing(true);
    return row;
  }

  private void openCreateDialog() {
    new BriefcaseFormDialog(
            null,
            titleOpportunityService,
            wrestlerRepository,
            null,
            imageStorageService,
            this::refreshGrid)
        .open();
  }

  private void openEditDialog(TitleOpportunity opportunity) {
    titleOpportunityService
        .findByIdWithDetails(opportunity.getId())
        .ifPresent(
            fresh ->
                new BriefcaseFormDialog(
                        fresh,
                        titleOpportunityService,
                        wrestlerRepository,
                        null,
                        imageStorageService,
                        this::refreshGrid)
                    .open());
  }

  private void voidCase(TitleOpportunity opportunity) {
    ConfirmDialog confirmDialog = new ConfirmDialog();
    confirmDialog.setHeader("Void Briefcase");
    confirmDialog.setText(
        "Void '"
            + opportunity.getName()
            + "'? The case is cancelled (HELD → VOIDED). History is never deleted.");
    confirmDialog.setConfirmText("Void");
    confirmDialog.setCancelable(true);
    confirmDialog.addConfirmListener(
        e -> {
          titleOpportunityService.adminVoid(opportunity.getId());
          refreshGrid();
        });
    confirmDialog.open();
  }

  public void refreshGrid() {
    allCache = titleOpportunityService.findAllWithDetails();
    String term = searchField.getValue();
    TitleOpportunityStatus status = statusFilter.getValue();
    List<TitleOpportunity> filtered =
        allCache.stream()
            .filter(o -> status == null || o.getStatus() == status)
            .filter(
                o ->
                    term == null
                        || term.isBlank()
                        || o.getName().toLowerCase().contains(term.toLowerCase())
                        || (o.getWrestler() != null
                            && o.getWrestler()
                                .getName()
                                .toLowerCase()
                                .contains(term.toLowerCase())))
            .toList();
    grid.setItems(filtered);
  }
}
