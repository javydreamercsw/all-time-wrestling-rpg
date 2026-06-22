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
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.ranking.TierRecalculationService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.AiSettingsView;
import com.github.javydreamercsw.management.ui.view.GameSettingsView;
import com.github.javydreamercsw.management.ui.view.campaign.CampaignAbilityCardListView;
import com.github.javydreamercsw.management.ui.view.campaign.StatusCardListView;
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
import com.vaadin.flow.server.VaadinService;
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
  private final WrestlerStateRepository wrestlerStateRepository;
  private final ImageCleanupService imageCleanupService;
  private final WrestlerService wrestlerService;
  private final UniverseContextService universeContextService;
  private final NotificationService notificationService;

  @Autowired
  public AdminView(
      final RankingService rankingService,
      final WrestlerStateRepository wrestlerStateRepository,
      final ImageCleanupService imageCleanupService,
      final WrestlerService wrestlerService,
      final UniverseContextService universeContextService,
      final NotificationService notificationService) {
    this.rankingService = rankingService;
    this.wrestlerStateRepository = wrestlerStateRepository;
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
        new Tab("Status Cards"),
        new Tab("Expansion Management"),
        new Tab("Wrestler Relationships"));
  }

  private Div createPages(final Tabs tabs) {
    Instantiator instantiator = VaadinService.getCurrent().getInstantiator();
    VerticalLayout adminToolsPage = createAdminToolsPage();
    adminToolsPage.setSizeFull();

    AiSettingsView aiSettingsView = instantiator.getOrCreate(AiSettingsView.class);
    GameSettingsView gameSettingsView = instantiator.getOrCreate(GameSettingsView.class);
    HolidayListView holidayListView = instantiator.getOrCreate(HolidayListView.class);
    SeasonSettingsView seasonSettingsView = instantiator.getOrCreate(SeasonSettingsView.class);
    CampaignAbilityCardListView campaignAbilityCardListView =
        instantiator.getOrCreate(CampaignAbilityCardListView.class);
    StatusCardListView statusCardListView = instantiator.getOrCreate(StatusCardListView.class);
    ExpansionManagementView expansionManagementView =
        instantiator.getOrCreate(ExpansionManagementView.class);
    WrestlerRelationshipManagementView relationshipManagementView =
        instantiator.getOrCreate(WrestlerRelationshipManagementView.class);

    Div pages =
        new Div(
            adminToolsPage,
            aiSettingsView,
            gameSettingsView,
            holidayListView,
            seasonSettingsView,
            campaignAbilityCardListView,
            statusCardListView,
            expansionManagementView,
            relationshipManagementView);
    pages.setSizeFull();

    Map<Tab, Component> tabsToPages =
        Map.of(
            tabs.getTabAt(0), adminToolsPage,
            tabs.getTabAt(1), aiSettingsView,
            tabs.getTabAt(2), gameSettingsView,
            tabs.getTabAt(3), holidayListView,
            tabs.getTabAt(4), seasonSettingsView,
            tabs.getTabAt(5), campaignAbilityCardListView,
            tabs.getTabAt(6), statusCardListView,
            tabs.getTabAt(7), expansionManagementView,
            tabs.getTabAt(8), relationshipManagementView);

    tabsToPages.values().forEach(p -> p.setVisible(false));
    adminToolsPage.setVisible(true);

    tabs.addSelectedChangeListener(
        event -> {
          tabsToPages.values().forEach(page -> page.setVisible(false));
          Tab selectedTab = tabs.getSelectedTab();
          Component selectedPage = tabsToPages.get(selectedTab);
          if (selectedPage != null) {
            selectedPage.setVisible(true);
            if (selectedPage instanceof ExpansionManagementView emv) {
              emv.refresh();
            }
            if (selectedPage instanceof WrestlerRelationshipManagementView rmv) {
              rmv.refresh();
            }
            if (selectedPage instanceof StatusCardListView sclv) {
              sclv.refresh();
            }
            if (selectedPage instanceof CampaignAbilityCardListView caclv) {
              caclv.refresh();
            }
          }
        });

    return pages;
  }

  private VerticalLayout createAdminToolsPage() {
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
                  new java.util.ArrayList<>(wrestlerStateRepository.findAll()));
            }
            notificationService.showSuccess("All wrestler tiers recalculated successfully!");
          } catch (Exception e) {
            notificationService.showError("Error recalculating tiers: " + e.getMessage());
            log.error("Error during tier recalculation", e);
          }
        });

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

    Button observabilityButton = new Button("System Observability Dashboard");
    observabilityButton.setId("observability-dashboard");
    observabilityButton.addClickListener(
        event -> UI.getCurrent().navigate(SystemObservabilityView.class));

    content.add(
        recalculateTiersButton, cleanupImagesButton, resetConditionButton, observabilityButton);
    return content;
  }
}
