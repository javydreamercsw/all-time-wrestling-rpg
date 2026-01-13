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
  private IntegerField targetPort;
  private TextField targetUser;
  private PasswordField targetPassword;
  private Button testConnectionButton;
  private Button transferButton;
  private ProgressBar progressBar;
  private Span statusLabel;

  @Autowired
  public DataTransferView(DataMigrationService migrationService) {
    this.migrationService = migrationService;
    initializeUI();
  }

  private void initializeUI() {
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.MEDIUM);
    add(new ViewToolbar("Data Transfer Management"));
    add(createConfigurationSection());
    add(createControlSection());
    add(createProgressSection());
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
    sourceDbType.setReadOnly(true); // Only H2 is supported as source for now

    targetDbType = new ComboBox<>("Target Database Type");
    targetDbType.setItems("MySQL");
    targetDbType.setValue("MySQL");
    targetHost = new TextField("Target Host");
    targetPort = new IntegerField("Target Port");
    targetUser = new TextField("Target User");
    targetPassword = new PasswordField("Target Password");
    configurationSection.add(
        title,
        new HorizontalLayout(sourceDbType, targetDbType),
        new HorizontalLayout(targetHost, targetPort),
        new HorizontalLayout(targetUser, targetPassword));
    return configurationSection;
  }

  private HorizontalLayout createControlSection() {
    HorizontalLayout controlSection = new HorizontalLayout();
    controlSection.setSpacing(true);
    controlSection.setAlignItems(FlexComponent.Alignment.BASELINE);
    testConnectionButton = new Button("Test Connection");
    testConnectionButton.addClickListener(event -> testConnection());
    transferButton = new Button("Transfer Data");
    transferButton.addClickListener(event -> transferData());
    controlSection.add(testConnectionButton, transferButton);
    return controlSection;
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
    progressBar.setIndeterminate(true);
    progressBar.setVisible(false);
    statusLabel = new Span();
    progressSection.add(title, progressBar, statusLabel);
    return progressSection;
  }

  private void testConnection() {
    DatabaseManager targetManager =
        DatabaseManagerFactory.getDatabaseManager(
            targetDbType.getValue(),
            targetHost.getValue(),
            targetPort.getValue(),
            targetUser.getValue(),
            targetPassword.getValue());
    try {
      targetManager.testConnection();
      Notification.show("Connection successful!", 3000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    } catch (SQLException e) {
      log.error("Failed to connect to the database", e);
      Notification.show("Connection failed: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
  }

  private void transferData() {
    progressBar.setVisible(true);
    statusLabel.setText("Starting data transfer...");
    transferButton.setEnabled(false);
    testConnectionButton.setEnabled(false);

    CompletableFuture.runAsync(
            () -> {
              try {
                migrationService.migrateData(
                    sourceDbType.getValue(),
                    targetDbType.getValue(),
                    targetHost.getValue(),
                    targetPort.getValue(),
                    targetUser.getValue(),
                    targetPassword.getValue());
                getUI()
                    .ifPresent(
                        ui ->
                            ui.access(
                                () -> {
                                  progressBar.setVisible(false);
                                  statusLabel.setText("Data transfer completed successfully.");
                                  transferButton.setEnabled(true);
                                  testConnectionButton.setEnabled(true);
                                  Notification.show(
                                          "Data transfer completed!",
                                          3000,
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
                                  transferButton.setEnabled(true);
                                  testConnectionButton.setEnabled(true);
                                  Notification.show(
                                          "Data transfer failed: " + e.getMessage(),
                                          5000,
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
                                transferButton.setEnabled(true);
                                testConnectionButton.setEnabled(true);
                                Notification.show(
                                        "An unexpected error occurred: " + ex.getMessage(),
                                        5000,
                                        Notification.Position.MIDDLE)
                                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                              }));
              return null;
            });
  }
}
