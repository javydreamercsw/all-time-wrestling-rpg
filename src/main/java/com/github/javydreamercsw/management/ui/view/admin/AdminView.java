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

import com.github.javydreamercsw.base.service.ranking.RankingService;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.ui.view.AiSettingsView;
import com.github.javydreamercsw.management.ui.view.GameSettingsView;
import com.github.javydreamercsw.management.ui.view.campaign.CampaignAbilityCardListView;
import com.github.javydreamercsw.management.ui.view.holiday.HolidayListView;
import com.github.javydreamercsw.management.ui.view.season.SeasonSettingsView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
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

@Route("admin")
@PageTitle("Admin")
@Menu(order = 11, icon = "vaadin:tools", title = "Admin")
@RolesAllowed(ADMIN_ROLE)
@Slf4j
public class AdminView extends VerticalLayout {

  private final RankingService rankingService;
  private final WrestlerRepository wrestlerRepository;

  public AdminView(RankingService rankingService, WrestlerRepository wrestlerRepository) {
    this.rankingService = rankingService;
    this.wrestlerRepository = wrestlerRepository;
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
        new Tab("Campaign Cards"));
  }

  private Div createPages(Tabs tabs) {
    Instantiator instantiator = VaadinService.getCurrent().getInstantiator();
    VerticalLayout adminToolsPage = createAdminToolsPage();
    adminToolsPage.setSizeFull();

    AiSettingsView aiSettingsView = instantiator.getOrCreate(AiSettingsView.class);
    GameSettingsView gameSettingsView = instantiator.getOrCreate(GameSettingsView.class);
    HolidayListView holidayListView = instantiator.getOrCreate(HolidayListView.class);
    SeasonSettingsView seasonSettingsView = instantiator.getOrCreate(SeasonSettingsView.class);
    CampaignAbilityCardListView campaignAbilityCardListView =
        instantiator.getOrCreate(CampaignAbilityCardListView.class);

    Div pages =
        new Div(
            adminToolsPage,
            aiSettingsView,
            gameSettingsView,
            holidayListView,
            seasonSettingsView,
            campaignAbilityCardListView);
    pages.setSizeFull();

    Map<Tab, Component> tabsToPages =
        Map.of(
            tabs.getTabAt(0), adminToolsPage,
            tabs.getTabAt(1), aiSettingsView,
            tabs.getTabAt(2), gameSettingsView,
            tabs.getTabAt(3), holidayListView,
            tabs.getTabAt(4), seasonSettingsView,
            tabs.getTabAt(5), campaignAbilityCardListView);

    tabs.addSelectedChangeListener(
        event -> {
          tabsToPages.values().forEach(page -> page.setVisible(false));
          Component selectedPage = tabsToPages.get(tabs.getSelectedTab());
          selectedPage.setVisible(true);
        });

    // Hide all pages except the first one
    tabsToPages.values().stream().skip(1).forEach(page -> page.setVisible(false));

    // Show the first page initially.
    tabsToPages.get(tabs.getTabAt(0)).setVisible(true);

    return pages;
  }

  private VerticalLayout createAdminToolsPage() {
    VerticalLayout content = new VerticalLayout();
    content.addClassNames(
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Border.ALL,
        LumoUtility.BorderRadius.MEDIUM,
        LumoUtility.Background.CONTRAST_5);

    Button recalculateTiersButton = new Button("Recalculate Wrestler Tiers");
    recalculateTiersButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    recalculateTiersButton.addClickListener(
        event -> {
          try {
            rankingService.recalculateRanking(
                new java.util.ArrayList<>(wrestlerRepository.findAll()));
            Notification.show(
                    "Wrestler tiers recalculated successfully!",
                    3000,
                    Notification.Position.TOP_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            log.info("Manual tier recalculation triggered and completed successfully.");
          } catch (Exception e) {
            Notification.show(
                    "Error during tier recalculation: " + e.getMessage(),
                    5000,
                    Notification.Position.TOP_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            log.error("Error during manual tier recalculation", e);
          }
        });

    Button manageAccountsButton = new Button("Manage Accounts");
    manageAccountsButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    manageAccountsButton.addClickListener(event -> UI.getCurrent().navigate("/account-list"));

    content.add(recalculateTiersButton, manageAccountsButton);
    return content;
  }
}
