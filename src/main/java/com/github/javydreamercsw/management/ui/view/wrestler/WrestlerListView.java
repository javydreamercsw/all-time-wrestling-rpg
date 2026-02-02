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

import com.github.javydreamercsw.base.ai.image.ImageGenerationServiceFactory;
import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.service.account.AccountService;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.component.WrestlerActionMenu;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;

@Route("wrestler-list")
@PageTitle("Wrestler List")
@Menu(order = 0, icon = "vaadin:user", title = "Wrestler List")
@PermitAll // When security is enabled, allow all authenticated users
public class WrestlerListView extends Main {

  private final WrestlerService wrestlerService;
  private final InjuryService injuryService;
  private final NpcService npcService;
  private final WrestlerRepository wrestlerRepository;
  private final AccountService accountService;
  private final SecurityUtils securityUtils;
  private final CampaignService campaignService;
  private final ImageGenerationServiceFactory imageGenerationServiceFactory;
  private final ImageStorageService imageStorageService;
  final Grid<Wrestler> wrestlerGrid;

  public WrestlerListView(
      @NonNull WrestlerService wrestlerService,
      @NonNull InjuryService injuryService,
      @NonNull NpcService npcService,
      @NonNull WrestlerRepository wrestlerRepository,
      @NonNull @Qualifier("baseAccountService") AccountService accountService,
      @NonNull SecurityUtils securityUtils,
      @NonNull CampaignService campaignService,
      @NonNull ImageGenerationServiceFactory imageGenerationServiceFactory,
      @NonNull ImageStorageService imageStorageService) {
    this.wrestlerService = wrestlerService;
    this.injuryService = injuryService;
    this.npcService = npcService;
    this.wrestlerRepository = wrestlerRepository;
    this.accountService = accountService;
    this.securityUtils = securityUtils;
    this.campaignService = campaignService;
    this.imageGenerationServiceFactory = imageGenerationServiceFactory;
    this.imageStorageService = imageStorageService;
    wrestlerGrid = new Grid<>();
    reloadGrid();

    Set<Long> injuredWrestlerIds =
        injuryService.getWrestlersWithActiveInjuries().stream()
            .map(Wrestler::getId)
            .collect(Collectors.toSet());

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
              return nameLayout;
            })
        .setHeader("Name")
        .setComparator(Comparator.comparing(Wrestler::getName))
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
    wrestlerGrid.addColumn(Wrestler::getFans).setHeader("Fans").setSortable(true);
    wrestlerGrid.addColumn(Wrestler::getBumps).setHeader("Bumps").setSortable(true);
    wrestlerGrid.addColumn(Wrestler::getCreationDate).setHeader("Creation Date");
    wrestlerGrid
        .addComponentColumn(
            wrestler -> {
              WrestlerActionMenu wrestlerActionMenu =
                  new WrestlerActionMenu(
                      wrestler,
                      wrestlerService,
                      injuryService,
                      npcService,
                      campaignService,
                      this::reloadGrid,
                      false,
                      securityUtils,
                      accountService,
                      imageGenerationServiceFactory,
                      imageStorageService);
              wrestlerActionMenu.setId("action-menu-" + wrestler.getId());
              return wrestlerActionMenu;
            })
        .setHeader("Actions")
        .setFlexGrow(1)
        .setWidth("200px");
    wrestlerGrid.setSizeFull();

    setSizeFull();
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.SMALL);

    Button createButton = createWrestlerButton();
    if (securityUtils.canCreate()) {
      add(new ViewToolbar("Wrestler List", createButton));
    } else {
      add(new ViewToolbar("Wrestler List"));
    }
    add(wrestlerGrid);
  }

  private Button createWrestlerButton() {
    Button button =
        new Button(
            "Create Wrestler",
            e -> {
              WrestlerDialog dialog =
                  new WrestlerDialog(
                      wrestlerService, accountService, npcService, this::reloadGrid, securityUtils);
              dialog.open();
            });
    button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    button.setId("create-wrestler-button");
    button.setVisible(securityUtils.canCreate());
    return button;
  }

  private void reloadGrid() {
    if (securityUtils.isAdmin() || securityUtils.isBooker()) {
      wrestlerGrid.setItems(wrestlerService.findAllIncludingInactive());
    } else {
      securityUtils
          .getAuthenticatedUser()
          .ifPresent(
              user -> {
                wrestlerGrid.setItems(wrestlerService.findAllByAccount(user.getAccount()));
              });
    }
  }
}
