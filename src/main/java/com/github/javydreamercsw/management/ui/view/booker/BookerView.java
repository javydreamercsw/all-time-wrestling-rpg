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
package com.github.javydreamercsw.management.ui.view.booker;

import static com.github.javydreamercsw.management.domain.account.RoleName.ADMIN_ROLE;
import static com.github.javydreamercsw.management.domain.account.RoleName.BOOKER_ROLE;

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
 * View for BOOKER role to manage their content. This view will provide access to booking-related
 * functionality without needing full entity management access.
 */
@Route(value = "booker", layout = MainLayout.class)
@PageTitle("Booker Dashboard | ATW RPG")
@RolesAllowed({ADMIN_ROLE, BOOKER_ROLE})
public class BookerView extends VerticalLayout {

  public BookerView() {
    addClassName(LumoUtility.Padding.MEDIUM);
    add(new ViewToolbar("Booker Dashboard"));

    H2 title = new H2("Booker Dashboard");
    title.addClassNames(LumoUtility.Margin.Top.NONE);

    Paragraph description =
        new Paragraph(
            "This is your booker workspace. Here you'll be able to manage shows, wrestlers, and"
                + " booking-related content.");
    description.addClassNames(LumoUtility.TextColor.SECONDARY);

    Div placeholder = new Div();
    placeholder.setText("🎬 Booker-specific functionality will be added here in Phase 4.");
    placeholder.addClassNames(
        LumoUtility.Padding.LARGE,
        LumoUtility.Background.CONTRAST_5,
        LumoUtility.BorderRadius.MEDIUM,
        LumoUtility.TextAlignment.CENTER);

    Paragraph futureFeatures =
        new Paragraph(
            "Planned features: Quick access to shows, wrestler management, match booking, and show"
                + " planning.");
    futureFeatures.addClassNames(
        LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL, LumoUtility.Margin.Top.MEDIUM);

    add(title, description, placeholder, futureFeatures);
  }
}
