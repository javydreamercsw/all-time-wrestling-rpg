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
package com.github.javydreamercsw.base.ui.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;

/** View shown when a user tries to access a page they don't have permission for. */
@Route("access-denied")
@PageTitle("Access Denied")
@PermitAll
public class AccessDeniedView extends VerticalLayout {

  public AccessDeniedView() {
    setSizeFull();
    setAlignItems(Alignment.CENTER);
    setJustifyContentMode(JustifyContentMode.CENTER);

    H1 title = new H1("Access Denied");
    title.addClassNames(LumoUtility.TextColor.ERROR);

    Paragraph message =
        new Paragraph(
            "You don't have permission to access this page. Please contact your administrator if"
                + " you believe this is an error.");
    message.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.TextAlignment.CENTER);

    Button homeButton = new Button("Go to Home", VaadinIcon.HOME.create());
    homeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    homeButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("")));

    add(title, message, homeButton);
  }
}
