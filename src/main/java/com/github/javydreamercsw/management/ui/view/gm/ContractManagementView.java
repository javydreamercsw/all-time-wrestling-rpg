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
package com.github.javydreamercsw.management.ui.view.gm;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.league.League;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerContract;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerContractRepository;
import com.github.javydreamercsw.management.service.league.LeagueService;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Route(value = "contracts", layout = MainLayout.class)
@PageTitle("Contract Management")
@RolesAllowed({"ADMIN", "BOOKER"})
@Slf4j
public class ContractManagementView extends VerticalLayout {

  private final LeagueService leagueService;
  private final WrestlerContractRepository contractRepository;
  private final SecurityUtils securityUtils;

  private final ComboBox<League> leagueSelector = new ComboBox<>("Select League");
  private final Grid<WrestlerContract> contractGrid = new Grid<>(WrestlerContract.class, false);

  public ContractManagementView(
      LeagueService leagueService,
      WrestlerContractRepository contractRepository,
      SecurityUtils securityUtils) {
    this.leagueService = leagueService;
    this.contractRepository = contractRepository;
    this.securityUtils = securityUtils;

    initializeUI();
  }

  private void initializeUI() {
    setSizeFull();
    setPadding(true);

    add(new H2("Contract Management"));

    leagueSelector.setItemLabelGenerator(League::getName);
    leagueSelector.addValueChangeListener(e -> refreshGrid(e.getValue()));
    leagueSelector.setWidth("300px");
    add(leagueSelector);

    configureContractGrid();
    add(contractGrid);

    loadLeagues();
  }

  private void configureContractGrid() {
    contractGrid.addColumn(c -> c.getWrestler().getName()).setHeader("Wrestler").setSortable(true);
    contractGrid.addColumn(WrestlerContract::getSalaryPerShow).setHeader("Salary per Show");
    contractGrid.addColumn(WrestlerContract::getDurationWeeks).setHeader("Duration (Weeks)");
    contractGrid.addColumn(c -> c.getIsInitialDraft() ? "Draft" : "Free Agent").setHeader("Type");
    contractGrid.addColumn(c -> c.getIsActive() ? "Active" : "Expired").setHeader("Status");

    contractGrid.addClassNames(LumoUtility.Border.ALL, LumoUtility.BorderRadius.MEDIUM);
  }

  private void loadLeagues() {
    securityUtils
        .getAuthenticatedUser()
        .ifPresent(
            user -> {
              List<League> leagues = leagueService.getLeaguesForUser(user.getAccount());
              leagueSelector.setItems(leagues);
              if (!leagues.isEmpty()) {
                leagueSelector.setValue(leagues.get(0));
              }
            });
  }

  private void refreshGrid(League league) {
    if (league == null) {
      contractGrid.setItems(List.of());
      return;
    }
    contractGrid.setItems(contractRepository.findByLeagueAndIsActiveTrue(league));
  }
}
