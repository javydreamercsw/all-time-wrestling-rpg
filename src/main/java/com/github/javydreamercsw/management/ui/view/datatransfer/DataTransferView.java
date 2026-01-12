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
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
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
  private final TextField hostField;
  private final IntegerField portField;
  private final TextField usernameField;
  private final PasswordField passwordField;
  private final Button testConnectionButton;
  private final Label statusLabel;

  private final Binder<ConnectionParameters> binder;
  private final ConnectionParameters connectionParameters;

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

    hostField = new TextField("Database Host");
    hostField.setId("host-field");
    hostField.setValue("localhost");
    hostField.setRequiredIndicatorVisible(true);

    portField = new IntegerField("Port");
    portField.setId("port-field");
    portField.setValue(3306); // Default MySQL port
    portField.setRequiredIndicatorVisible(true);

    portField.setErrorMessage("Port must be a number");

    usernameField = new TextField("Username");
    usernameField.setId("username-field");
    usernameField.setRequiredIndicatorVisible(true);

    passwordField = new PasswordField("Password");
    passwordField.setId("password-field");
    passwordField.setRequiredIndicatorVisible(true);

    testConnectionButton = new Button("Test Connection");
    testConnectionButton.setId("test-connection-button");

    statusLabel = new Label();
    statusLabel.setId("status-label");

    stepContent.add(
        hostField, portField, usernameField, passwordField, testConnectionButton, statusLabel);

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

    // Initialize Binder
    binder = new Binder<>(ConnectionParameters.class);
    connectionParameters = new ConnectionParameters();

    binder
        .forField(hostField)
        .asRequired("Host cannot be empty")
        .bind(ConnectionParameters::getHost, ConnectionParameters::setHost);

    binder
        .forField(portField)
        .asRequired("Port cannot be empty")
        .withValidator(
            port -> port != null && port >= 0 && port <= 65535, "Port must be between 0 and 65535")
        .bind(ConnectionParameters::getPort, ConnectionParameters::setPort);

    binder
        .forField(usernameField)
        .asRequired("Username cannot be empty")
        .bind(ConnectionParameters::getUsername, ConnectionParameters::setUsername);

    binder
        .forField(passwordField)
        .asRequired("Password cannot be empty")
        .bind(ConnectionParameters::getPassword, ConnectionParameters::setPassword);

    // Set initial values to binder
    binder.readBean(connectionParameters);

    nextButton.addClickListener(
        event -> {
          try {
            binder.writeBean(connectionParameters);
            Notification.show(
                "Validation successful. Host: "
                    + connectionParameters.getHost()
                    + ", Port: "
                    + connectionParameters.getPort());
            statusLabel.setText("Validation successful.");
            statusLabel.setVisible(true);
            // In a real wizard, here you would advance to the next step
          } catch (ValidationException e) {
            Notification.show("Validation failed: " + e.getMessage());
            statusLabel.setText("Validation failed.");
            statusLabel.setVisible(true);
            // Explicitly mark fields as invalid and set error messages
            e.getFieldValidationErrors()
                .forEach(
                    error -> {
                      // Assuming the component is a HasValidation (e.g., TextField, IntegerField,
                      // PasswordField)
                      if (error.getField() instanceof TextField) {
                        ((TextField) error.getField()).setInvalid(true);
                        ((TextField) error.getField())
                            .setErrorMessage(error.getMessage().orElse(""));
                      } else if (error.getField() instanceof IntegerField) {
                        ((IntegerField) error.getField()).setInvalid(true);
                        ((IntegerField) error.getField())
                            .setErrorMessage(error.getMessage().orElse(""));
                      } else if (error.getField() instanceof PasswordField) {
                        ((PasswordField) error.getField()).setInvalid(true);
                        ((PasswordField) error.getField())
                            .setErrorMessage(error.getMessage().orElse(""));
                      }
                    });
          }
        });
  }
}
