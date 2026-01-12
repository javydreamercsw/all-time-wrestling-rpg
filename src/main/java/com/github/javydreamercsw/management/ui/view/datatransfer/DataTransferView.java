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
package com.github.javydreamercsw.management.ui.view.datatransfer;

import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@PageTitle("Data Transfer")
@Route(value = "data-transfer", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class DataTransferView extends Div {

  private final VerticalLayout contentLayout;
  private final VerticalLayout stepContent;
  private final Button previousButton;
  private final Button nextButton;
  private final Button cancelButton;

  public DataTransferView() {
    setId("data-transfer-wizard");
    setSizeFull();

    contentLayout = new VerticalLayout();
    contentLayout.setSizeFull();
    contentLayout.setPadding(false);
    contentLayout.setSpacing(false);

    stepContent = new VerticalLayout();
    stepContent.setId("connection-config-step");
    stepContent.setSizeFull();

    TextField hostField = new TextField("Database Host");
    hostField.setId("host-field");
    hostField.setValue("localhost");

    IntegerField portField = new IntegerField("Port");
    portField.setId("port-field");
    portField.setValue(3306); // Default MySQL port

    TextField usernameField = new TextField("Username");
    usernameField.setId("username-field");

    PasswordField passwordField = new PasswordField("Password");
    passwordField.setId("password-field");

    Button testConnectionButton = new Button("Test Connection");
    testConnectionButton.setId("test-connection-button");

    stepContent.add(hostField, portField, usernameField, passwordField, testConnectionButton);

    previousButton = new Button("Previous");
    previousButton.setId("previous-button");
    nextButton = new Button("Next");
    nextButton.setId("next-button");
    cancelButton = new Button("Cancel");
    cancelButton.setId("cancel-button");
    HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, previousButton, nextButton);
    buttonLayout.setWidthFull();
    buttonLayout.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);

    contentLayout.add(stepContent, buttonLayout);
    add(contentLayout);
  }
}
