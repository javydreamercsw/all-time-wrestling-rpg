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
package com.github.javydreamercsw.management.ui.view.player;

import static com.github.javydreamercsw.management.domain.account.RoleName.ADMIN_ROLE;
import static com.github.javydreamercsw.management.domain.account.RoleName.BOOKER_ROLE;
import static com.github.javydreamercsw.management.domain.account.RoleName.PLAYER_ROLE;

import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;

/**
 * View for PLAYER role to manage their own content. This view will provide access to player-owned
 * wrestlers and matches.
 */
@Route(value = "player", layout = MainLayout.class)
@PageTitle("Player Dashboard | ATW RPG")
@RolesAllowed({ADMIN_ROLE, BOOKER_ROLE, PLAYER_ROLE})
public class PlayerView extends VerticalLayout {

  public PlayerView() {
    addClassName(LumoUtility.Padding.MEDIUM);
    add(new ViewToolbar("Player Dashboard"));

    H2 title = new H2("Player Dashboard");
    title.addClassNames(LumoUtility.Margin.Top.NONE);

    Paragraph description =
        new Paragraph(
            "This is your player workspace. Here you'll be able to manage your own wrestlers and"
                + " view your match history.");
    description.addClassNames(LumoUtility.TextColor.SECONDARY);

    Div placeholder = new Div();
    placeholder.setText("🎮 Player-specific functionality will be added here in Phase 4.");
    placeholder.addClassNames(
        LumoUtility.Padding.LARGE,
        LumoUtility.Background.CONTRAST_5,
        LumoUtility.BorderRadius.MEDIUM,
        LumoUtility.TextAlignment.CENTER);

    Paragraph futureFeatures =
        new Paragraph(
            "Planned features: My Wrestlers, My Matches, My Stats, and Profile Management.");
    futureFeatures.addClassNames(
        LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL, LumoUtility.Margin.Top.MEDIUM);

    add(title, description, placeholder, futureFeatures);
  }
}
