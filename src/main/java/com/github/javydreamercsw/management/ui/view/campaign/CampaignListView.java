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
package com.github.javydreamercsw.management.ui.view.campaign;

import static com.github.javydreamercsw.base.domain.account.RoleName.ADMIN_ROLE;

import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import java.time.format.DateTimeFormatter;
import lombok.NonNull;

@Route(value = "campaign-list", layout = MainLayout.class)
@PageTitle("Campaigns")
@RolesAllowed(ADMIN_ROLE)
public class CampaignListView extends Main {

  private final CampaignRepository campaignRepository;
  private final UniverseContextService universeContextService;
  private final UniverseRepository universeRepository;
  final Grid<Campaign> grid = new Grid<>(Campaign.class, false);

  public CampaignListView(
      @NonNull final CampaignRepository campaignRepository,
      @NonNull final UniverseContextService universeContextService,
      @NonNull final UniverseRepository universeRepository) {
    this.campaignRepository = campaignRepository;
    this.universeContextService = universeContextService;
    this.universeRepository = universeRepository;

    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.SMALL);
    setSizeFull();

    setupGrid();
    add(new ViewToolbar("Campaigns"));
    add(grid);
    refreshGrid();
  }

  private void setupGrid() {
    grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    grid.setSizeFull();

    grid.addColumn(c -> c.getWrestler().getName())
        .setHeader("Wrestler")
        .setSortable(true)
        .setFlexGrow(2);

    grid.addColumn(c -> c.getStatus() != null ? c.getStatus().name() : "")
        .setHeader("Status")
        .setSortable(true)
        .setFlexGrow(1);

    grid.addColumn(
            c ->
                c.getStartedAt() != null
                    ? c.getStartedAt().format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                    : "—")
        .setHeader("Started")
        .setSortable(true)
        .setFlexGrow(1);

    grid.addColumn(
            c ->
                c.getEndedAt() != null
                    ? c.getEndedAt().format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                    : "—")
        .setHeader("Ended")
        .setSortable(true)
        .setFlexGrow(1);

    grid.addComponentColumn(
            campaign -> {
              Button viewBtn = new Button("Dashboard", new Icon(VaadinIcon.DASHBOARD));
              viewBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
              viewBtn.setId("campaign-dashboard-btn-" + campaign.getId());
              viewBtn.addClickListener(
                  e -> getUI().ifPresent(ui -> ui.navigate(CampaignDashboardView.class)));
              return viewBtn;
            })
        .setHeader("Actions")
        .setFlexGrow(0)
        .setWidth("130px");
  }

  void refreshGrid() {
    Long universeId = universeContextService.getCurrentUniverseId();
    universeRepository
        .findById(universeId)
        .ifPresent(u -> grid.setItems(campaignRepository.findByUniverse(u)));
  }
}
