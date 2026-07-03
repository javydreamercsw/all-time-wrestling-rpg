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
package com.github.javydreamercsw.management.ui.view.wrestler;

import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.service.account.AccountService;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.campaign.AlignmentService;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.service.expansion.ExpansionService;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.injury.InjuryTypeService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.universe.UniverseSettingsService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.component.WrestlerActionMenu;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;

@Route("wrestler-list")
@PageTitle("Wrestler List")
@Menu(order = 0, icon = "vaadin:user", title = "Wrestler List")
@Slf4j
@PermitAll // When security is enabled, allow all authenticated users
public class WrestlerListView extends Main {

  private final WrestlerService wrestlerService;
  private final InjuryService injuryService;
  private final InjuryTypeService injuryTypeService;
  private final NpcService npcService;
  private final ExpansionService expansionService;
  private final UniverseSettingsService universeSettingsService;
  private final AccountService accountService;
  private final SecurityUtils securityUtils;
  private final ImageStorageService imageStorageService;
  private final UniverseContextService universeContextService;
  private final WrestlerStateRepository wrestlerStateRepository;
  private final AlignmentService alignmentService;
  private Set<Long> injuredWrestlerIds;
  private Map<Long, WrestlerState> statesByWrestlerId = new HashMap<>();
  private Map<Long, WrestlerAlignment> alignmentsByWrestlerId = new HashMap<>();
  final Grid<Wrestler> wrestlerGrid;

  public WrestlerListView(
      @NonNull final WrestlerService wrestlerService,
      @NonNull final InjuryService injuryService,
      @NonNull final InjuryTypeService injuryTypeService,
      @NonNull final NpcService npcService,
      @NonNull final ExpansionService expansionService,
      @NonNull final UniverseSettingsService universeSettingsService,
      @NonNull @Qualifier("baseAccountService") final AccountService accountService,
      @NonNull final SecurityUtils securityUtils,
      @NonNull final CampaignService campaignService,
      @NonNull final ImageStorageService imageStorageService,
      @NonNull final UniverseContextService universeContextService,
      @NonNull final WrestlerStateRepository wrestlerStateRepository,
      @NonNull final AlignmentService alignmentService) {
    this.wrestlerService = wrestlerService;
    this.injuryService = injuryService;
    this.injuryTypeService = injuryTypeService;
    this.npcService = npcService;
    this.expansionService = expansionService;
    this.universeSettingsService = universeSettingsService;
    this.accountService = accountService;
    this.securityUtils = securityUtils;
    this.imageStorageService = imageStorageService;
    this.universeContextService = universeContextService;
    this.wrestlerStateRepository = wrestlerStateRepository;
    this.alignmentService = alignmentService;
    wrestlerGrid = new Grid<>();
    reloadGrid();

    Grid.Column<Wrestler> nameColumn =
        wrestlerGrid
            .addComponentColumn(
                wrestler -> {
                  HorizontalLayout nameLayout = new HorizontalLayout();
                  nameLayout.setAlignItems(FlexComponent.Alignment.CENTER);
                  if (wrestler.getActive()) {
                    Icon activeIcon = new Icon(VaadinIcon.CHECK);
                    activeIcon.setColor("green");
                    activeIcon.getStyle().set("margin-right", "5px");
                    nameLayout.add(activeIcon);
                  } else {
                    Icon inactiveIcon = new Icon(VaadinIcon.MINUS_CIRCLE);
                    inactiveIcon.setColor("red");
                    inactiveIcon.getStyle().set("margin-right", "5px");
                    nameLayout.add(inactiveIcon);
                  }
                  if (injuredWrestlerIds.contains(wrestler.getId())) {
                    Icon injuryIcon = new Icon(VaadinIcon.AMBULANCE);
                    injuryIcon.setColor("red");
                    injuryIcon.getStyle().set("margin-right", "5px");
                    nameLayout.add(injuryIcon);
                  }
                  if (wrestler.getAccount() != null) {
                    Icon userIcon = new Icon(VaadinIcon.USER);
                    userIcon.setColor("blue");
                    userIcon.getStyle().set("margin-right", "5px");
                    nameLayout.add(userIcon);
                  }
                  nameLayout.add(new Span(wrestler.getName()));
                  WrestlerAlignment alignment = alignmentsByWrestlerId.get(wrestler.getId());
                  if (alignment != null) {
                    Span badge = new Span(alignment.getAlignmentType().name());
                    badge.getStyle().set("font-size", "var(--lumo-font-size-xs)");
                    badge.getStyle().set("padding", "0 4px");
                    badge.getStyle().set("border-radius", "4px");
                    badge.getStyle().set("font-weight", "bold");
                    if (alignment.getAlignmentType() == AlignmentType.FACE) {
                      badge.getStyle().set("background-color", "#c8e6c9");
                      badge.getStyle().set("color", "#1b5e20");
                    } else if (alignment.getAlignmentType() == AlignmentType.HEEL) {
                      badge.getStyle().set("background-color", "#ffcdd2");
                      badge.getStyle().set("color", "#b71c1c");
                    } else {
                      badge.getStyle().set("background-color", "#e0e0e0");
                      badge.getStyle().set("color", "#424242");
                    }
                    nameLayout.add(badge);
                  }
                  return nameLayout;
                })
            .setHeader("Name")
            .setComparator(Comparator.comparing(Wrestler::getName))
            .setSortProperty("name")
            .setSortable(true);
    wrestlerGrid.addColumn(Wrestler::getGender).setHeader("Gender").setSortable(true);
    wrestlerGrid.addColumn(Wrestler::getDeckSize).setHeader("Deck Size").setSortable(true);
    wrestlerGrid
        .addColumn(Wrestler::getStartingHealth)
        .setHeader("Starting Health")
        .setSortable(true);
    wrestlerGrid.addColumn(Wrestler::getLowHealth).setHeader("Low Health").setSortable(true);
    wrestlerGrid
        .addColumn(Wrestler::getStartingStamina)
        .setHeader("Starting Stamina")
        .setSortable(true);
    wrestlerGrid.addColumn(Wrestler::getLowStamina).setHeader("Low Stamina").setSortable(true);

    wrestlerGrid
        .addColumn(
            wrestler -> {
              WrestlerState state = statesByWrestlerId.get(wrestler.getId());
              return state != null ? state.getFans() : 0L;
            })
        .setHeader("Fans")
        .setSortable(true);

    wrestlerGrid
        .addColumn(
            wrestler -> {
              WrestlerState state = statesByWrestlerId.get(wrestler.getId());
              return state != null ? state.getBumps() : 0;
            })
        .setHeader("Bumps")
        .setSortable(true);

    wrestlerGrid
        .addColumn(
            wrestler -> {
              WrestlerState state = statesByWrestlerId.get(wrestler.getId());
              if (state == null || state.getManager() == null) {
                return "";
              }
              String expansionCode = state.getManager().getExpansionCode();
              Set<String> enabled =
                  universeContextService
                      .getCurrentUniverse()
                      .map(universeSettingsService::getEnabledExpansionCodesForUniverse)
                      .orElseGet(() -> new HashSet<>(expansionService.getEnabledExpansionCodes()));
              if (!enabled.contains(expansionCode)) {
                return "";
              }
              return state.getManager().getName();
            })
        .setHeader("Manager")
        .setSortable(true);

    wrestlerGrid.addColumn(Wrestler::getCreationDate).setHeader("Creation Date");

    // Default sorting by Name
    wrestlerGrid.sort(GridSortOrder.asc(nameColumn).build());

    wrestlerGrid
        .addComponentColumn(
            wrestler -> {
              WrestlerActionMenu wrestlerActionMenu =
                  new WrestlerActionMenu(
                      wrestler,
                      wrestlerService,
                      injuryService,
                      injuryTypeService,
                      npcService,
                      campaignService,
                      wrestlerStateRepository,
                      this::reloadGrid,
                      false,
                      securityUtils,
                      accountService,
                      imageStorageService,
                      universeContextService,
                      alignmentService);
              wrestlerActionMenu.setId("action-menu-" + wrestler.getId());
              return wrestlerActionMenu;
            })
        .setHeader("Actions")
        .setFlexGrow(1)
        .setWidth("200px");
    wrestlerGrid.setSizeFull();
    wrestlerGrid.setMinWidth("900px");
    wrestlerGrid.setId("wrestler-list-grid");

    setSizeFull();
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.SMALL);

    com.vaadin.flow.component.html.Div gridWrapper =
        new com.vaadin.flow.component.html.Div(wrestlerGrid);
    gridWrapper.addClassName("grid-scroll-container");

    Button createButton = createWrestlerButton();
    if (securityUtils.canCreate()) {
      add(new ViewToolbar("Wrestler List", createButton));
    } else {
      add(new ViewToolbar("Wrestler List"));
    }
    add(gridWrapper);
  }

  private Button createWrestlerButton() {
    Button button =
        new Button(
            "Create Wrestler",
            e -> {
              WrestlerDialog dialog =
                  new WrestlerDialog(
                      wrestlerService,
                      accountService,
                      npcService,
                      imageStorageService,
                      wrestlerStateRepository,
                      this::reloadGrid,
                      securityUtils,
                      universeContextService,
                      alignmentService);
              dialog.open();
            });
    button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    button.setId("create-wrestler-button");
    button.setVisible(securityUtils.canCreate());
    return button;
  }

  private void reloadGrid() {
    Long universeId = universeContextService.getCurrentUniverseId();
    try {
      statesByWrestlerId = wrestlerService.getStateMapByUniverseId(universeId);
    } catch (Exception e) {
      log.warn("Could not preload wrestler states for universe {}: {}", universeId, e.getMessage());
      statesByWrestlerId = new HashMap<>();
    }
    try {
      alignmentsByWrestlerId = alignmentService.getAlignmentMapByUniverseId(universeId);
    } catch (Exception e) {
      log.warn(
          "Could not preload wrestler alignments for universe {}: {}", universeId, e.getMessage());
      alignmentsByWrestlerId = new HashMap<>();
    }
    injuredWrestlerIds =
        injuryService.getWrestlersWithActiveInjuries(universeId).stream()
            .map(Wrestler::getId)
            .collect(Collectors.toSet());

    Set<String> enabledCodes =
        universeContextService
            .getCurrentUniverse()
            .map(universeSettingsService::getEnabledExpansionCodesForUniverse)
            .orElseGet(() -> new HashSet<>(expansionService.getEnabledExpansionCodes()));
    Set<Long> excludedIds =
        universeContextService
            .getCurrentUniverse()
            .map(
                u ->
                    universeSettingsService.getExcludedWrestlers(u).stream()
                        .map(Wrestler::getId)
                        .collect(Collectors.toSet()))
            .orElseGet(java.util.Collections::emptySet);

    if (securityUtils.isAdmin() || securityUtils.isBooker()) {
      wrestlerGrid.setItems(
          query ->
              wrestlerService
                  .findPageFiltered(
                      enabledCodes, excludedIds, VaadinSpringDataHelpers.toSpringPageRequest(query))
                  .stream(),
          query -> (int) wrestlerService.countFiltered(enabledCodes, excludedIds));
    } else {
      securityUtils
          .getAuthenticatedUser()
          .ifPresent(
              user -> {
                wrestlerGrid.setItems(
                    wrestlerService.findAllByAccount(user.getAccount()).stream()
                        .filter(w -> enabledCodes.contains(w.getExpansionCode()))
                        .filter(w -> !excludedIds.contains(w.getId()))
                        .toList());
              });
    }
  }
}
