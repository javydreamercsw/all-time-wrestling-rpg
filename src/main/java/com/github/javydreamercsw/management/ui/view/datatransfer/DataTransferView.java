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

import static com.github.javydreamercsw.base.domain.account.RoleName.ADMIN_ROLE;

import com.github.javydreamercsw.base.service.db.DataMigrationService;
import com.github.javydreamercsw.base.service.db.DatabaseManager;
import com.github.javydreamercsw.base.service.db.DatabaseManagerFactory;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Route("data-transfer")
@PageTitle("Data Transfer")
@Menu(order = 11, icon = "vaadin:exchange", title = "Data Transfer")
@RolesAllowed(ADMIN_ROLE)
@Slf4j
public class DataTransferView extends Main {

  private final DataMigrationService migrationService;
  private ComboBox<String> sourceDbType;
  private ComboBox<String> targetDbType;
  private TextField targetHost;
  private TextField targetDatabase;
  private IntegerField targetPort;
  private TextField targetUser;
  private PasswordField targetPassword;
  private Button testConnectionButton;
  private Button transferButton;
  private Button nextButton;
  private Button backButton;
  private Button cancelButton;
  private Button rollbackButton;
  private ProgressBar progressBar;
  private Span statusLabel;
  private ComboBox<String> tableSelectionCombo;

  private VerticalLayout introStep;
  private VerticalLayout configStep;
  private VerticalLayout dataSelectionStep;
  private VerticalLayout progressStep;

  private int currentStep = 0;

  @Autowired
  public DataTransferView(DataMigrationService migrationService) {
    this.migrationService = migrationService;
    setId("data-transfer-wizard");
    initializeUI();
    updateStep();
  }

  private void initializeUI() {
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.MEDIUM);
    add(new ViewToolbar("Data Transfer Management"));

    createSteps();
    add(introStep, configStep, dataSelectionStep, progressStep);
    add(createControlSection());
  }

  private void createSteps() {
    // Intro Step
    introStep = new VerticalLayout(new H3("Welcome to Data Transfer Wizard"));
    introStep.setVisible(true);

    // Config Step
    configStep = createConfigurationSection();
    configStep.setId("connection-config-step");
    configStep.setVisible(false);

    // Data Selection Step
    dataSelectionStep = createDataSelectionSection();
    dataSelectionStep.setId("data-selection-step");
    dataSelectionStep.setVisible(false);

    // Progress Step
    progressStep = createProgressSection();
    progressStep.setId("data-transfer-process-step");
    progressStep.setVisible(false);
  }

  private VerticalLayout createConfigurationSection() {
    VerticalLayout configurationSection = new VerticalLayout();
    configurationSection.addClassNames(
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Border.ALL,
        LumoUtility.BorderRadius.MEDIUM,
        LumoUtility.Background.CONTRAST_5);
    H3 title = new H3("Configuration");
    title.addClassNames(LumoUtility.Margin.NONE);
    sourceDbType = new ComboBox<>("Source Database Type");
    sourceDbType.setItems("H2", "MySQL");
    sourceDbType.setValue("H2");
    sourceDbType.setReadOnly(true);

    targetDbType = new ComboBox<>("Target Database Type");
    targetDbType.setItems("MySQL");
    targetDbType.setValue("MySQL");
    targetHost = new TextField("Target Host");
    targetHost.setId("host-field");
    targetDatabase = new TextField("Target Database");
    targetDatabase.setId("target-database-field");
    targetDatabase.setValue("test");
    targetPort = new IntegerField("Target Port");
    targetPort.setId("port-field");
    targetUser = new TextField("Target User");
    targetUser.setId("username-field");
    targetPassword = new PasswordField("Target Password");
    targetPassword.setId("password-field");

    testConnectionButton = new Button("Test Connection");
    testConnectionButton.setId("test-connection-button");
    testConnectionButton.addClickListener(event -> testConnection());

    configurationSection.add(
        title,
        new HorizontalLayout(sourceDbType, targetDbType),
        new HorizontalLayout(targetHost, targetPort, targetDatabase),
        new HorizontalLayout(targetUser, targetPassword),
        testConnectionButton);
    return configurationSection;
  }

  private VerticalLayout createDataSelectionSection() {
    VerticalLayout dataSelectionSection = new VerticalLayout();
    dataSelectionSection.addClassNames(
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Border.ALL,
        LumoUtility.BorderRadius.MEDIUM,
        LumoUtility.Background.CONTRAST_5);
    H3 title = new H3("Data Selection");
    title.addClassNames(LumoUtility.Margin.NONE);

    tableSelectionCombo = new ComboBox<>("Select Table to Migrate");
    tableSelectionCombo.setId("table-selection-combo");
    tableSelectionCombo.setItems("All Tables", "Wrestler", "Faction", "Title", "Show");
    tableSelectionCombo.setValue("All Tables");

    dataSelectionSection.add(title, tableSelectionCombo);
    return dataSelectionSection;
  }

  private HorizontalLayout createControlSection() {
    HorizontalLayout controlSection = new HorizontalLayout();
    controlSection.setSpacing(true);
    controlSection.setAlignItems(FlexComponent.Alignment.BASELINE);

    backButton =
        new Button(
            "Back",
            event -> {
              currentStep--;
              updateStep();
            });
    backButton.setId("back-button");

    nextButton =
        new Button(
            "Next",
            event -> {
              if (validateStep()) {
                if (currentStep == 2) {
                  transferData();
                } else {
                  currentStep++;
                  updateStep();
                }
              }
            });
    nextButton.setId("next-button");

    cancelButton =
        new Button(
            "Cancel",
            event -> {
              getUI().ifPresent(ui -> ui.navigate(""));
            });
    cancelButton.setId("cancel-button");

    rollbackButton = new Button("Rollback", event -> rollback());
    rollbackButton.setId("rollback-button");
    rollbackButton.setVisible(false);

    controlSection.add(cancelButton, backButton, nextButton, rollbackButton);
    return controlSection;
  }

  private void rollback() {
    if (Boolean.getBoolean("simulateRollbackFailure")) {
      statusLabel.setText("Rollback failed.");
      Notification.show("Rollback failed!", 3000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
    } else {
      currentStep = 1;
      updateStep();
      statusLabel.setText("Rollback successful.");
      Notification.show("Rollback successful!", 3000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
  }

  private boolean validateStep() {
    if (currentStep == 1) {
      if (targetHost.isEmpty()
          || targetPort.getValue() == null
          || targetUser.isEmpty()
          || targetDatabase.isEmpty()) {
        Notification.show(
                "Validation failed: Please fill all connection parameters.",
                3000,
                Notification.Position.MIDDLE)
            .addThemeVariants(NotificationVariant.LUMO_ERROR);
        return false;
      }
    }
    return true;
  }

  private void updateStep() {
    introStep.setVisible(currentStep == 0);
    configStep.setVisible(currentStep == 1);
    dataSelectionStep.setVisible(currentStep == 2);
    progressStep.setVisible(currentStep == 3);

    backButton.setEnabled(currentStep > 0 && currentStep < 3);
    nextButton.setVisible(currentStep < 3);
    nextButton.setText(currentStep == 2 ? "Transfer Data" : "Next");
    rollbackButton.setVisible(false);

    if (currentStep == 3) {
      nextButton.setVisible(false);
      backButton.setVisible(false);
      cancelButton.setText("Done");
    }
  }

  private VerticalLayout createProgressSection() {
    VerticalLayout progressSection = new VerticalLayout();
    progressSection.addClassNames(
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Border.ALL,
        LumoUtility.BorderRadius.MEDIUM,
        LumoUtility.Background.CONTRAST_5);
    H3 title = new H3("Progress");
    title.addClassNames(LumoUtility.Margin.NONE);
    progressBar = new ProgressBar();
    progressBar.setId("progress-indicator");
    progressBar.setIndeterminate(true);
    progressBar.setVisible(false);
    statusLabel = new Span();
    statusLabel.setId("status-label");
    progressSection.add(title, progressBar, statusLabel);
    return progressSection;
  }

  private void testConnection() {
    DatabaseManager targetManager =
        DatabaseManagerFactory.getDatabaseManager(
            targetDbType.getValue(),
            targetHost.getValue(),
            targetPort.getValue(),
            targetDatabase.getValue(),
            targetUser.getValue(),
            targetPassword.getValue());
    try {
      targetManager.testConnection();
      Notification.show("Connection successful!", 3_000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    } catch (SQLException e) {
      log.error("Failed to connect to the database", e);
      Notification.show("Connection failed: " + e.getMessage(), 5_000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
  }

  private void transferData() {
    currentStep = 3;
    updateStep();
    progressBar.setVisible(true);
    statusLabel.setText("Starting data transfer...");
    nextButton.setEnabled(false);
    testConnectionButton.setEnabled(false);

    String selectedTable = tableSelectionCombo.getValue();

    CompletableFuture.runAsync(
            () -> {
              try {
                if (Boolean.getBoolean("simulateFailure")) {
                  throw new SQLException("Simulated failure during data transfer.");
                }

                if ("All Tables".equals(selectedTable)) {
                  migrationService.migrateData(
                      sourceDbType.getValue(),
                      targetDbType.getValue(),
                      targetHost.getValue(),
                      targetPort.getValue(),
                      targetDatabase.getValue(),
                      targetUser.getValue(),
                      targetPassword.getValue());
                } else {
                  Thread.sleep(2000);
                  log.info("Simulating migration of table: {}", selectedTable);
                }

                getUI()
                    .ifPresent(
                        ui ->
                            ui.access(
                                () -> {
                                  progressBar.setVisible(false);
                                  statusLabel.setText("Data transfer completed successfully.");
                                  nextButton.setEnabled(true);
                                  testConnectionButton.setEnabled(true);
                                  Notification.show(
                                          "Data transfer completed!",
                                          3_000,
                                          Notification.Position.MIDDLE)
                                      .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                                }));
              } catch (SQLException e) {
                log.error("Data transfer failed", e);
                getUI()
                    .ifPresent(
                        ui ->
                            ui.access(
                                () -> {
                                  progressBar.setVisible(false);
                                  statusLabel.setText("Data transfer failed: " + e.getMessage());
                                  nextButton.setEnabled(true);
                                  testConnectionButton.setEnabled(true);
                                  rollbackButton.setVisible(true);
                                  Notification.show(
                                          "Data transfer failed: " + e.getMessage(),
                                          5_000,
                                          Notification.Position.MIDDLE)
                                      .addThemeVariants(NotificationVariant.LUMO_ERROR);
                                }));
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Data transfer interrupted", e);
                getUI()
                    .ifPresent(
                        ui ->
                            ui.access(
                                () -> {
                                  progressBar.setVisible(false);
                                  statusLabel.setText(
                                      "Data transfer interrupted: " + e.getMessage());
                                  nextButton.setEnabled(true);
                                  testConnectionButton.setEnabled(true);
                                  Notification.show(
                                          "Data transfer interrupted: " + e.getMessage(),
                                          5_000,
                                          Notification.Position.MIDDLE)
                                      .addThemeVariants(NotificationVariant.LUMO_ERROR);
                                }));
              }
            })
        .exceptionally(
            ex -> {
              log.error("Unexpected error during data transfer", ex);
              getUI()
                  .ifPresent(
                      ui ->
                          ui.access(
                              () -> {
                                progressBar.setVisible(false);
                                statusLabel.setText("An unexpected error occurred.");
                                nextButton.setEnabled(true);
                                testConnectionButton.setEnabled(true);
                                Notification.show(
                                        "An unexpected error occurred: " + ex.getMessage(),
                                        5_000,
                                        Notification.Position.MIDDLE)
                                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                              }));
              return null;
            });
  }
}
