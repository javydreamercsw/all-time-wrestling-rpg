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
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
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
  private final VerticalLayout connectionConfigStep;
  private final VerticalLayout dataSelectionStep; // New step for data selection
  private final VerticalLayout dataTransferProcessStep; // New step for data transfer process
  private final Button previousButton;
  private final Button nextButton;
  private final Button cancelButton;
  private final TextField hostField;
  private final IntegerField portField;
  private final TextField usernameField;
  private final PasswordField passwordField;
  private final Button testConnectionButton;
  private final NativeLabel statusLabel; // For connection status
  private final ProgressBar progressBar;
  private final Button rollbackButton;

  private final Binder<ConnectionParameters> binder;
  private final ConnectionParameters connectionParameters;

  private int currentStep = 0; // 0: Connection Config, 1: Data Selection, 2: Data Transfer Process

  public DataTransferView() {
    setId("data-transfer-wizard");
    setSizeFull();

    contentLayout = new VerticalLayout();
    contentLayout.setSizeFull();
    contentLayout.setPadding(false);
    contentLayout.setSpacing(false);

    // Connection Configuration Step
    connectionConfigStep = new VerticalLayout();
    connectionConfigStep.setId("connection-config-step");
    connectionConfigStep.setSizeFull();

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

    statusLabel = new NativeLabel();
    statusLabel.setId("status-label");

    connectionConfigStep.add(
        hostField, portField, usernameField, passwordField, testConnectionButton, statusLabel);

    // Data Selection Step
    dataSelectionStep = new VerticalLayout();
    dataSelectionStep.setId("data-selection-step");
    dataSelectionStep.setSizeFull();
    dataSelectionStep.add(new Div(new NativeLabel("Data Selection options will go here.")));
    Button startTransferButton = new Button("Start Transfer");
    startTransferButton.setId("start-transfer-button");
    dataSelectionStep.add(startTransferButton);
    dataSelectionStep.setVisible(false); // Hidden initially

    // Data Transfer Process Step
    dataTransferProcessStep = new VerticalLayout();
    dataTransferProcessStep.setId("data-transfer-process-step");
    dataTransferProcessStep.setSizeFull();
    // Placeholder for progress indicator
    dataTransferProcessStep.add(new Div(new NativeLabel("Data transfer in progress...")));
    progressBar = new ProgressBar();
    progressBar.setId("progress-indicator");
    progressBar.setIndeterminate(true); // Spinning indicator
    dataTransferProcessStep.add(progressBar);
    rollbackButton = new Button("Rollback");
    rollbackButton.setId("rollback-button");
    rollbackButton.setVisible(false); // Hidden initially
    dataTransferProcessStep.add(rollbackButton);
    dataTransferProcessStep.setVisible(false); // Hidden initially

    previousButton = new Button("Previous");
    previousButton.setId("previous-button");
    nextButton = new Button("Next");
    nextButton.setId("next-button");
    cancelButton = new Button("Cancel");
    cancelButton.setId("cancel-button");
    HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, previousButton, nextButton);
    buttonLayout.setWidthFull();
    buttonLayout.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);

    contentLayout.add(
        connectionConfigStep, dataSelectionStep, dataTransferProcessStep, buttonLayout);
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

    // Configure button visibility and click listeners
    previousButton.setEnabled(false); // First step has no previous

    nextButton.addClickListener(
        event -> {
          if (currentStep == 0) { // Connection Configuration Step
            try {
              binder.writeBean(connectionParameters);
              Notification.show(
                  "Validation successful. Host: "
                      + connectionParameters.getHost()
                      + ", Port: "
                      + connectionParameters.getPort());
              statusLabel.setText("Validation successful.");
              statusLabel.setVisible(true);

              // Advance to the next step (Data Selection)
              currentStep++;
              showStep(currentStep);
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
          } else if (currentStep == 1) { // Data Selection Step - Start transfer
            // Simulate data transfer process
            Notification.show("Starting data transfer...");
            statusLabel.setText("Data transfer in progress...");
            statusLabel.setVisible(true);

            // Disable next button during transfer
            nextButton.setEnabled(false);
            previousButton.setEnabled(false);

            // Simulate a long-running task
            new Thread(
                    () -> {
                      try {
                        Thread.sleep(3000); // Simulate 3 seconds of transfer
                        boolean simulateFailure = Boolean.getBoolean("simulateFailure");

                        getUI()
                            .ifPresent(
                                ui ->
                                    ui.access(
                                        () -> {
                                          if (simulateFailure) {
                                            Notification.show(
                                                "Data transfer failed. Please rollback.");
                                            statusLabel.setText("Data transfer failed.");
                                            statusLabel.setVisible(true);
                                            progressBar.setVisible(false);
                                            rollbackButton.setVisible(true);
                                            currentStep =
                                                2; // Ensure currentStep reflects the process step
                                            showStep(currentStep); // Make process step visible

                                          } else {
                                            Notification.show("Data transfer complete!");
                                            statusLabel.setText("Data transfer complete.");
                                            statusLabel.setVisible(true);
                                            // Advance to the next step (or a completion step)
                                            currentStep++;
                                            showStep(currentStep);
                                          }
                                        }));
                      } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        getUI()
                            .ifPresent(
                                ui ->
                                    ui.access(
                                        () -> {
                                          Notification.show(
                                              "Data transfer interrupted: " + ex.getMessage());
                                          statusLabel.setText("Data transfer interrupted.");
                                          statusLabel.setVisible(true);
                                          nextButton.setEnabled(true);
                                          previousButton.setEnabled(true);
                                          progressBar.setVisible(false);
                                          rollbackButton.setVisible(true);
                                        }));
                      }
                    })
                .start();
          }
        });

    rollbackButton.addClickListener(
        event -> {
          // Simulate rollback failure
          boolean simulateRollbackFailure = Boolean.getBoolean("simulateRollbackFailure");
          if (simulateRollbackFailure) {
            Notification.show("Rollback failed unexpectedly!");
            statusLabel.setText("Rollback failed.");
            // Keep on current step, perhaps show an error state
            // For now, just show the message and leave on the dataTransferProcessStep
          } else {
            Notification.show("Rolling back data transfer...");
            statusLabel.setText("Rolling back...");
            statusLabel.setVisible(true);
            // Reset to the first step (Connection Configuration)
            currentStep = 0;
            showStep(currentStep);
            Notification.show("Rollback complete.");
            statusLabel.setText("Rollback complete. Please reconfigure connection.");
          }
        });

    previousButton.addClickListener(
        event -> {
          currentStep--;
          showStep(currentStep);
        });

    cancelButton.addClickListener(
        event -> {
          Notification.show("Data transfer cancelled.");
          // In a real application, you would navigate away or close the wizard
          // For now, we'll just go back to the first step
          currentStep = 0;
          showStep(currentStep);
        });

    showStep(currentStep); // Initialize view to the first step
  }

  private void showStep(int stepIndex) {
    connectionConfigStep.setVisible(false);
    dataSelectionStep.setVisible(false);
    dataTransferProcessStep.setVisible(false);

    switch (stepIndex) {
      case 0:
        connectionConfigStep.setVisible(true);
        previousButton.setEnabled(false);
        nextButton.setEnabled(true);
        break;
      case 1:
        dataSelectionStep.setVisible(true);
        previousButton.setEnabled(true);
        nextButton.setEnabled(true);
        break;
      case 2:
        dataTransferProcessStep.setVisible(true);
        previousButton.setEnabled(true); // Allow going back from process step
        nextButton.setEnabled(false); // No next step after starting process
        break;
      default:
        // Handle invalid step, maybe show an error or go back to first step
        connectionConfigStep.setVisible(true);
        previousButton.setEnabled(false);
        nextButton.setEnabled(true);
        currentStep = 0;
    }
  }
}
