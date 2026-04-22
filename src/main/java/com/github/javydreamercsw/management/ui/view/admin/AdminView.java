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
package com.github.javydreamercsw.management.ui.view.admin;

import static com.github.javydreamercsw.base.domain.account.RoleName.ADMIN_ROLE;

import com.github.javydreamercsw.base.ai.image.ImageCleanupService;
import com.github.javydreamercsw.base.service.ranking.RankingService;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.base.ui.service.NotificationService;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.ranking.TierRecalculationService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.AiSettingsView;
import com.github.javydreamercsw.management.ui.view.GameSettingsView;
import com.github.javydreamercsw.management.ui.view.account.AccountListView;
import com.github.javydreamercsw.management.ui.view.campaign.CampaignAbilityCardListView;
import com.github.javydreamercsw.management.ui.view.holiday.HolidayListView;
import com.github.javydreamercsw.management.ui.view.season.SeasonSettingsView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Route("admin")
@PageTitle("Admin")
@Menu(order = 11, icon = "vaadin:tools", title = "Admin")
@RolesAllowed(ADMIN_ROLE)
@Slf4j
public class AdminView extends VerticalLayout {

  private final RankingService rankingService;
  private final WrestlerRepository wrestlerRepository;
  private final ImageCleanupService imageCleanupService;
  private final WrestlerService wrestlerService;
  private final UniverseContextService universeContextService;
  private final NotificationService notificationService;

  @Autowired
  public AdminView(
      RankingService rankingService,
      WrestlerRepository wrestlerRepository,
      ImageCleanupService imageCleanupService,
      WrestlerService wrestlerService,
      UniverseContextService universeContextService,
      NotificationService notificationService) {
    this.rankingService = rankingService;
    this.wrestlerRepository = wrestlerRepository;
    this.imageCleanupService = imageCleanupService;
    this.wrestlerService = wrestlerService;
    this.universeContextService = universeContextService;
    this.notificationService = notificationService;
    initializeUI();
  }

  private void initializeUI() {
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.MEDIUM);
    setSizeFull();

    add(new ViewToolbar("Admin Tools"));

    Tabs tabs = createTabs();
    Div pages = createPages(tabs);

    add(tabs, pages);
    setFlexGrow(1, pages);
  }

  private Tabs createTabs() {
    return new Tabs(
        new Tab("Admin Tools"),
        new Tab("AI Settings"),
        new Tab("Game Settings"),
        new Tab("Holidays"),
        new Tab("Season Settings"),
        new Tab("Campaign Cards"),
        new Tab("Manage Accounts"));
  }

  private Div createPages(Tabs tabs) {
    Div pages = new Div();
    pages.setSizeFull();

    Map<Integer, Component> tabToPage =
        Map.of(
            0, createAdminToolsPage(),
            1, Instantiator.get(UI.getCurrent()).getOrCreate(AiSettingsView.class),
            2, Instantiator.get(UI.getCurrent()).getOrCreate(GameSettingsView.class),
            3, Instantiator.get(UI.getCurrent()).getOrCreate(HolidayListView.class),
            4, Instantiator.get(UI.getCurrent()).getOrCreate(SeasonSettingsView.class),
            5, Instantiator.get(UI.getCurrent()).getOrCreate(CampaignAbilityCardListView.class),
            6, createManageAccountsPage());

    tabToPage.values().forEach(p -> p.setVisible(false));
    tabToPage.get(0).setVisible(true);

    tabs.addSelectedChangeListener(
        event -> {
          tabToPage.values().forEach(p -> p.setVisible(false));
          tabToPage.get(tabs.getSelectedIndex()).setVisible(true);
        });

    tabToPage.values().forEach(pages::add);
    return pages;
  }

  private Component createAdminToolsPage() {
    VerticalLayout content = new VerticalLayout();
    content.setPadding(true);
    content.setSpacing(true);

    Button recalculateTiersButton = new Button("Recalculate Wrestler Tiers");
    recalculateTiersButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    recalculateTiersButton.addClickListener(
        event -> {
          try {
            if (rankingService instanceof TierRecalculationService trs) {
              trs.recalculateAllTiers();
            } else {
              // Fallback if not TierRecalculationService
              rankingService.recalculateRanking(
                  new java.util.ArrayList<>(wrestlerRepository.findAll()));
            }
            notificationService.showSuccess("All wrestler tiers recalculated successfully!");
          } catch (Exception e) {
            notificationService.showError("Error recalculating tiers: " + e.getMessage());
            log.error("Error during tier recalculation", e);
          }
        });

    Button manageAccountsButton = new Button("Manage Accounts and Roles");
    manageAccountsButton.addClickListener(event -> UI.getCurrent().navigate(AccountListView.class));

    Button cleanupImagesButton = new Button("Cleanup AI Generated Images");
    cleanupImagesButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
    cleanupImagesButton.addClickListener(
        event -> {
          try {
            int count = imageCleanupService.cleanupUnusedImages();
            notificationService.showSuccess(
                "AI image cleanup completed successfully! Deleted " + count + " images.");
          } catch (Exception e) {
            notificationService.showError("Error during image cleanup: " + e.getMessage());
            log.error("Error during image cleanup", e);
          }
        });

    Button resetConditionButton = new Button("Reset Wrestler Physical Condition");
    resetConditionButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
    resetConditionButton.addClickListener(
        event -> {
          try {
            wrestlerService.resetAllWearAndTear(universeContextService.getCurrentUniverseId());
            notificationService.showSuccess("All wrestlers reset to 100% physical condition!");
          } catch (Exception e) {
            notificationService.showError("Error resetting physical condition: " + e.getMessage());
            log.error("Error during physical condition reset", e);
          }
        });

    content.add(
        recalculateTiersButton, manageAccountsButton, cleanupImagesButton, resetConditionButton);
    return content;
  }

  private Component createManageAccountsPage() {
    return new Div(); // Placeholder
  }
}
