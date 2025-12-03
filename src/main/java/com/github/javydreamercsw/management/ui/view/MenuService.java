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
package com.github.javydreamercsw.management.ui.view;

import com.vaadin.flow.component.icon.VaadinIcon;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MenuService {

  public List<MenuItem> getMenuItems() {
    List<MenuItem> menuItems = new ArrayList<>();

    // Manually define the menu structure
    MenuItem dashboards = new MenuItem("Dashboards", VaadinIcon.DASHBOARD, null);
    dashboards.addChild(new MenuItem("Inbox", VaadinIcon.INBOX, "inbox"));
    dashboards.addChild(new MenuItem("Show Calendar", VaadinIcon.CALENDAR, "show-calendar"));
    dashboards.addChild(new MenuItem("Wrestler Rankings", VaadinIcon.STAR, "wrestler-rankings"));
    dashboards.addChild(
        new MenuItem("Championship Rankings", VaadinIcon.TROPHY, "championship-rankings"));

    MenuItem entities = new MenuItem("Entities", VaadinIcon.DATABASE, null);
    entities.addChild(new MenuItem("Faction Rivalries", VaadinIcon.GROUP, "faction-rivalry-list"));
    entities.addChild(new MenuItem("Factions", VaadinIcon.GROUP, "faction-list"));
    entities.addChild(new MenuItem("Injury Types", VaadinIcon.PLUS_CIRCLE, "injury-types"));
    entities.addChild(new MenuItem("NPCs", VaadinIcon.USERS, "npc-list"));
    entities.addChild(new MenuItem("Rivalries", VaadinIcon.FIRE, "rivalry-list"));
    entities.addChild(new MenuItem("Seasons", VaadinIcon.CALENDAR_CLOCK, "season-list"));
    entities.addChild(new MenuItem("Segment Rules", VaadinIcon.LIST_OL, "segment-rule-list"));
    entities.addChild(new MenuItem("Segment Types", VaadinIcon.PUZZLE_PIECE, "segment-type-list"));
    entities.addChild(
        new MenuItem("Show Templates", VaadinIcon.CLIPBOARD_TEXT, "show-template-list"));
    entities.addChild(new MenuItem("Shows", VaadinIcon.CALENDAR_O, "show-list"));
    entities.addChild(new MenuItem("Teams", VaadinIcon.USERS, "teams"));
    entities.addChild(new MenuItem("Titles", VaadinIcon.TROPHY, "title-list"));
    entities.addChild(new MenuItem("Wrestlers", VaadinIcon.USER, "wrestler-list"));

    MenuItem contentGeneration = new MenuItem("Content Generation", VaadinIcon.AUTOMATION, null);
    contentGeneration.addChild(new MenuItem("Show Planning", VaadinIcon.CALENDAR, "show-planning"));

    MenuItem cardGame = new MenuItem("Card Game", VaadinIcon.RECORDS, null);
    cardGame.addChild(new MenuItem("Cards", VaadinIcon.CREDIT_CARD, "card-list"));
    // Removed Card Sets
    cardGame.addChild(new MenuItem("Decks", VaadinIcon.RECORDS, "deck-list"));

    MenuItem configuration = new MenuItem("Configuration", VaadinIcon.COG, null);
    configuration.addChild(new MenuItem("Sync Dashboard", VaadinIcon.REFRESH, "notion-sync"));

    menuItems.add(dashboards);
    menuItems.add(entities);
    menuItems.add(contentGeneration);
    menuItems.add(cardGame);
    menuItems.add(configuration);

    // Sort top-level menu items
    menuItems.sort(Comparator.comparing(MenuItem::getTitle));

    // Recursively sort sub-menus
    menuItems.forEach(this::sortSubMenus);

    return menuItems;
  }

  private void sortSubMenus(MenuItem menuItem) {
    if (!menuItem.getChildren().isEmpty()) {
      menuItem.getChildren().sort(Comparator.comparing(MenuItem::getTitle));
      menuItem.getChildren().forEach(this::sortSubMenus);
    }
  }
}
