package com.github.javydreamercsw.management.ui.view.sync;

import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.service.sync.EntityDependencyAnalyzer;
import com.github.javydreamercsw.management.service.sync.NotionSyncScheduler;
import com.github.javydreamercsw.management.service.sync.NotionSyncService;
import com.github.javydreamercsw.management.service.sync.NotionSyncService.SyncResult;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker.SyncProgress;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * UI view for managing Notion synchronization operations. Provides controls to trigger sync
 * operations with real-time progress updates.
 */
@Route("notion-sync")
@PageTitle("Notion Sync")
@Menu(order = 10, icon = "vaadin:refresh", title = "Notion Sync")
@PermitAll
@ConditionalOnProperty(name = "notion.sync.enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class NotionSyncView extends Main {

  private final NotionSyncService notionSyncService;
  private final NotionSyncScheduler notionSyncScheduler;
  private final NotionSyncProperties syncProperties;
  private final SyncProgressTracker progressTracker;
  private final EntityDependencyAnalyzer dependencyAnalyzer;

  // UI Components
  private Button syncAllButton;
  private Button syncShowsButton;
  private Button syncWrestlersButton;
  private Button syncFactionsButton;
  private Button syncTeamsButton;
  private Button syncTemplatesButton;
  private ProgressBar progressBar;
  private Span statusLabel;
  private Span lastSyncLabel;
  private VerticalLayout progressContainer;
  private VerticalLayout logContainer;
  private Details configDetails;

  // Background task management
  private ScheduledExecutorService uiUpdateExecutor;
  private volatile boolean syncInProgress = false;

  public NotionSyncView(
      NotionSyncService notionSyncService,
      NotionSyncScheduler notionSyncScheduler,
      NotionSyncProperties syncProperties,
      SyncProgressTracker progressTracker,
      EntityDependencyAnalyzer dependencyAnalyzer) {
    this.notionSyncService = notionSyncService;
    this.notionSyncScheduler = notionSyncScheduler;
    this.syncProperties = syncProperties;
    this.progressTracker = progressTracker;
    this.dependencyAnalyzer = dependencyAnalyzer;

    initializeUI();
  }

  private void initializeUI() {
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.MEDIUM);

    // Header
    add(new ViewToolbar("Notion Sync Management"));

    // Status section
    add(createStatusSection());

    // Control buttons section
    add(createControlSection());

    // Progress section
    add(createProgressSection());

    // Configuration section
    add(createConfigurationSection());

    // Log section
    add(createLogSection());

    // Initialize status
    updateSyncStatus();
  }

  private VerticalLayout createStatusSection() {
    VerticalLayout statusSection = new VerticalLayout();
    statusSection.addClassNames(
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Border.ALL,
        LumoUtility.BorderRadius.MEDIUM,
        LumoUtility.Background.CONTRAST_5);

    H3 statusTitle = new H3("Sync Status");
    statusTitle.addClassNames(LumoUtility.Margin.NONE);

    statusLabel = new Span("Ready");
    statusLabel.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.TextColor.SUCCESS);

    lastSyncLabel = new Span("Never");
    lastSyncLabel.addClassNames(LumoUtility.TextColor.SECONDARY);

    HorizontalLayout statusInfo = new HorizontalLayout();
    statusInfo.addClassNames(LumoUtility.Gap.LARGE);
    statusInfo.add(
        new Div(new Span("Status: "), statusLabel),
        new Div(new Span("Last Sync: "), lastSyncLabel));

    statusSection.add(statusTitle, statusInfo);
    return statusSection;
  }

  private HorizontalLayout createControlSection() {
    HorizontalLayout controlSection = new HorizontalLayout();
    controlSection.addClassNames(LumoUtility.Gap.MEDIUM);

    syncAllButton = new Button("Sync All Entities", VaadinIcon.REFRESH.create());
    syncAllButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    syncAllButton.addClickListener(e -> triggerFullSync());

    // Individual entity sync buttons
    syncShowsButton = new Button("Sync Shows", VaadinIcon.CALENDAR.create());
    syncShowsButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
    syncShowsButton.addClickListener(e -> triggerEntitySync("shows"));

    syncWrestlersButton = new Button("Sync Wrestlers", VaadinIcon.USER.create());
    syncWrestlersButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
    syncWrestlersButton.addClickListener(e -> triggerEntitySync("wrestlers"));

    syncFactionsButton = new Button("Sync Factions", VaadinIcon.GROUP.create());
    syncFactionsButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
    syncFactionsButton.addClickListener(e -> triggerEntitySync("factions"));

    syncTeamsButton = new Button("Sync Teams", VaadinIcon.USERS.create());
    syncTeamsButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
    syncTeamsButton.addClickListener(e -> triggerEntitySync("teams"));

    syncTemplatesButton = new Button("Sync Templates", VaadinIcon.FILE_TEXT.create());
    syncTemplatesButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
    syncTemplatesButton.addClickListener(e -> triggerEntitySync("templates"));

    Button statusButton = new Button("Check Status", VaadinIcon.INFO_CIRCLE.create());
    statusButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    statusButton.addClickListener(e -> updateSyncStatus());

    controlSection.add(
        syncAllButton,
        syncShowsButton,
        syncWrestlersButton,
        syncFactionsButton,
        syncTeamsButton,
        syncTemplatesButton,
        statusButton);
    return controlSection;
  }

  private VerticalLayout createProgressSection() {
    progressContainer = new VerticalLayout();
    progressContainer.addClassNames(
        LumoUtility.Padding.MEDIUM, LumoUtility.Border.ALL, LumoUtility.BorderRadius.MEDIUM);
    progressContainer.setVisible(false);

    H4 progressTitle = new H4("Sync Progress");
    progressTitle.addClassNames(LumoUtility.Margin.NONE);

    progressBar = new ProgressBar();
    progressBar.setIndeterminate(true);
    progressBar.setWidth("100%");

    Span progressText = new Span("Initializing sync...");
    progressText.addClassNames(LumoUtility.TextColor.SECONDARY);

    progressContainer.add(progressTitle, progressBar, progressText);
    return progressContainer;
  }

  private Details createConfigurationSection() {
    configDetails = new Details();
    configDetails.setSummaryText("Configuration Details");
    configDetails.addClassNames(LumoUtility.Width.FULL);

    VerticalLayout configContent = new VerticalLayout();
    configContent.addClassNames(LumoUtility.Gap.SMALL);

    // Configuration info
    configContent.add(
        createConfigItem("Sync Enabled", String.valueOf(syncProperties.isEnabled())),
        createConfigItem("Scheduler Enabled", String.valueOf(syncProperties.isSchedulerEnabled())),
        createConfigItem(
            "Sync Interval", formatInterval(syncProperties.getScheduler().getInterval())),
        createConfigItem("Entities", String.join(", ", dependencyAnalyzer.getAutomaticSyncOrder())),
        createConfigItem("Backup Enabled", String.valueOf(syncProperties.isBackupEnabled())));

    if (syncProperties.isBackupEnabled()) {
      configContent.add(
          createConfigItem("Backup Directory", syncProperties.getBackup().getDirectory()),
          createConfigItem(
              "Max Backup Files", String.valueOf(syncProperties.getBackup().getMaxFiles())));
    }

    configDetails.add(configContent);
    return configDetails;
  }

  private HorizontalLayout createConfigItem(String label, String value) {
    HorizontalLayout item = new HorizontalLayout();
    item.addClassNames(LumoUtility.Gap.MEDIUM);

    Span labelSpan = new Span(label + ":");
    labelSpan.addClassNames(LumoUtility.FontWeight.SEMIBOLD);
    labelSpan.setWidth("200px");

    Span valueSpan = new Span(value);
    valueSpan.addClassNames(LumoUtility.TextColor.SECONDARY);

    item.add(labelSpan, valueSpan);
    return item;
  }

  private VerticalLayout createLogSection() {
    VerticalLayout logSection = new VerticalLayout();
    logSection.addClassNames(
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Border.ALL,
        LumoUtility.BorderRadius.MEDIUM,
        LumoUtility.Background.CONTRAST_5);

    H4 logTitle = new H4("Sync Log");
    logTitle.addClassNames(LumoUtility.Margin.NONE);

    logContainer = new VerticalLayout();
    logContainer.addClassNames(LumoUtility.Gap.XSMALL, LumoUtility.Overflow.AUTO);
    logContainer.setMaxHeight("300px");

    addLogEntry("System initialized", "INFO");

    logSection.add(logTitle, logContainer);
    return logSection;
  }

  private void triggerFullSync() {
    if (syncInProgress) {
      showNotification("Sync already in progress", NotificationVariant.LUMO_CONTRAST);
      return;
    }

    startSyncOperation(
        "Syncing all entities...",
        () -> {
          try {
            List<SyncResult> results = notionSyncScheduler.triggerManualSync();
            return new SyncOperationResult(
                true,
                results.size(),
                results.stream().mapToInt(SyncResult::getSyncedCount).sum(),
                "All entities synced successfully");
          } catch (Exception e) {
            log.error("Full sync failed", e);
            return new SyncOperationResult(false, 0, 0, "Sync failed: " + e.getMessage());
          }
        });
  }

  private void triggerEntitySync(String entityName) {
    if (syncInProgress) {
      showNotification("Sync already in progress", NotificationVariant.LUMO_CONTRAST);
      return;
    }

    String operationId = entityName + "-sync-" + System.currentTimeMillis();
    String displayName = entityName.substring(0, 1).toUpperCase() + entityName.substring(1);

    startSyncOperationWithProgress(
        "Syncing " + displayName.toLowerCase() + "...",
        operationId,
        () -> {
          try {
            SyncResult result = notionSyncScheduler.syncEntity(entityName, operationId);
            return new SyncOperationResult(
                result.isSuccess(),
                1,
                result.getSyncedCount(),
                result.isSuccess()
                    ? displayName + " synced successfully"
                    : result.getErrorMessage());
          } catch (Exception e) {
            log.error("{} sync failed", displayName, e);
            progressTracker.failOperation(
                operationId, displayName + " sync failed: " + e.getMessage());
            return new SyncOperationResult(
                false, 0, 0, displayName + " sync failed: " + e.getMessage());
          }
        });
  }

  private void triggerShowsSync() {
    triggerEntitySync("shows");
  }

  private void startSyncOperation(String operationName, SyncOperation operation) {
    syncInProgress = true;
    updateButtonStates();
    showProgressSection(operationName);
    addLogEntry("Started: " + operationName, "INFO");

    CompletableFuture.supplyAsync(operation::execute)
        .whenComplete(
            (result, throwable) -> {
              getUI()
                  .ifPresent(
                      ui ->
                          ui.access(
                              () -> {
                                syncInProgress = false;
                                hideProgressSection();
                                updateButtonStates();

                                if (throwable != null) {
                                  handleSyncError(throwable);
                                } else {
                                  handleSyncResult(result);
                                }

                                updateLastSyncTime();
                              }));
            });
  }

  private void startSyncOperationWithProgress(
      String operationName, String operationId, SyncOperation operation) {
    syncInProgress = true;
    updateButtonStates();
    showProgressSection(operationName);
    addLogEntry("Started: " + operationName, "INFO");

    // Register as progress listener for this operation
    SyncProgressTracker.SyncProgressListener progressListener =
        new SyncProgressTracker.SyncProgressListener() {
          @Override
          public void onProgressUpdated(SyncProgress progress) {
            if (progress.getOperationId().equals(operationId)) {
              getUI().ifPresent(ui -> ui.access(() -> updateProgressDisplay(progress)));
            }
          }

          @Override
          public void onOperationCompleted(SyncProgress progress) {
            if (progress.getOperationId().equals(operationId)) {
              getUI()
                  .ifPresent(
                      ui ->
                          ui.access(
                              () -> {
                                syncInProgress = false;
                                hideProgressSection();
                                updateButtonStates();

                                if (progress.isSuccess()) {
                                  handleSyncResult(
                                      new SyncOperationResult(
                                          true,
                                          1,
                                          progress.getItemsProcessed(),
                                          progress.getResultMessage()));
                                } else {
                                  handleSyncError(
                                      new RuntimeException(progress.getResultMessage()));
                                }

                                updateLastSyncTime();
                                progressTracker.removeProgressListener(this);
                              }));
            }
          }

          @Override
          public void onLogMessage(String logOperationId, String message, String level) {
            if (logOperationId.equals(operationId)) {
              getUI().ifPresent(ui -> ui.access(() -> addLogEntry(message, level)));
            }
          }
        };

    progressTracker.addProgressListener(progressListener);

    CompletableFuture.supplyAsync(operation::execute)
        .whenComplete(
            (result, throwable) -> {
              if (throwable != null) {
                getUI()
                    .ifPresent(
                        ui ->
                            ui.access(
                                () -> {
                                  syncInProgress = false;
                                  hideProgressSection();
                                  updateButtonStates();
                                  handleSyncError(throwable);
                                  updateLastSyncTime();
                                  progressTracker.removeProgressListener(progressListener);
                                }));
              }
              // Success case is handled by the progress listener
            });
  }

  @FunctionalInterface
  private interface SyncOperation {
    SyncOperationResult execute();
  }

  private static class SyncOperationResult {
    final boolean success;
    final int entitiesProcessed;
    final int itemsSynced;
    final String message;

    SyncOperationResult(boolean success, int entitiesProcessed, int itemsSynced, String message) {
      this.success = success;
      this.entitiesProcessed = entitiesProcessed;
      this.itemsSynced = itemsSynced;
      this.message = message;
    }
  }

  private void showProgressSection(String operationName) {
    progressContainer.setVisible(true);
    statusLabel.setText("Syncing...");
    statusLabel.removeClassNames(LumoUtility.TextColor.SUCCESS, LumoUtility.TextColor.ERROR);
    statusLabel.addClassNames(LumoUtility.TextColor.PRIMARY);
  }

  private void updateProgressDisplay(SyncProgress progress) {
    // Update progress bar
    if (progress.getTotalSteps() > 0) {
      progressBar.setIndeterminate(false);
      progressBar.setValue(progress.getProgressPercentage());
    } else {
      progressBar.setIndeterminate(true);
    }

    // Update progress text
    String progressText =
        String.format(
            "%s (%d/%d)",
            progress.getCurrentStepDescription(),
            progress.getCurrentStep(),
            progress.getTotalSteps());

    // Find and update the progress text span
    progressContainer
        .getChildren()
        .filter(component -> component instanceof Span)
        .map(component -> (Span) component)
        .filter(span -> span.getClassNames().contains(LumoUtility.TextColor.SECONDARY))
        .findFirst()
        .ifPresent(span -> span.setText(progressText));

    // Add log entry for significant progress updates
    if (progress.getCurrentStep() > 0) {
      addLogEntry(String.format("Progress: %s", progress.getCurrentStepDescription()), "INFO");
    }
  }

  private void hideProgressSection() {
    progressContainer.setVisible(false);
  }

  private void updateButtonStates() {
    boolean enabled = !syncInProgress;
    syncAllButton.setEnabled(enabled);
    syncShowsButton.setEnabled(enabled);
    syncWrestlersButton.setEnabled(enabled);
    syncFactionsButton.setEnabled(enabled);
    syncTeamsButton.setEnabled(enabled);
    syncTemplatesButton.setEnabled(enabled);
  }

  private void handleSyncResult(SyncOperationResult result) {
    if (result.success) {
      statusLabel.setText("Completed");
      statusLabel.removeClassNames(LumoUtility.TextColor.PRIMARY, LumoUtility.TextColor.ERROR);
      statusLabel.addClassNames(LumoUtility.TextColor.SUCCESS);

      String message = String.format("✅ %s (%d items synced)", result.message, result.itemsSynced);
      addLogEntry(message, "SUCCESS");
      showNotification(message, NotificationVariant.LUMO_SUCCESS);
    } else {
      handleSyncError(new RuntimeException(result.message));
    }
  }

  private void handleSyncError(Throwable error) {
    statusLabel.setText("Error");
    statusLabel.removeClassNames(LumoUtility.TextColor.PRIMARY, LumoUtility.TextColor.SUCCESS);
    statusLabel.addClassNames(LumoUtility.TextColor.ERROR);

    String errorMessage = "❌ Sync failed: " + error.getMessage();
    addLogEntry(errorMessage, "ERROR");
    showNotification("Sync failed: " + error.getMessage(), NotificationVariant.LUMO_ERROR);
  }

  private void updateSyncStatus() {
    if (!syncInProgress) {
      statusLabel.setText("Ready");
      statusLabel.removeClassNames(LumoUtility.TextColor.PRIMARY, LumoUtility.TextColor.ERROR);
      statusLabel.addClassNames(LumoUtility.TextColor.SUCCESS);
    }
    addLogEntry("Status updated", "INFO");
  }

  private void updateLastSyncTime() {
    String timestamp =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm:ss"));
    lastSyncLabel.setText(timestamp);
  }

  private void addLogEntry(String message, String level) {
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

    HorizontalLayout logEntry = new HorizontalLayout();
    logEntry.addClassNames(LumoUtility.Gap.SMALL, LumoUtility.AlignItems.CENTER);

    Span timeSpan = new Span(timestamp);
    timeSpan.addClassNames(LumoUtility.TextColor.TERTIARY, LumoUtility.FontSize.SMALL);

    Icon levelIcon =
        switch (level) {
          case "SUCCESS" -> VaadinIcon.CHECK_CIRCLE.create();
          case "ERROR" -> VaadinIcon.EXCLAMATION_CIRCLE.create();
          case "WARN" -> VaadinIcon.WARNING.create();
          default -> VaadinIcon.INFO_CIRCLE.create();
        };

    levelIcon.addClassNames(
        level.equals("SUCCESS")
            ? LumoUtility.TextColor.SUCCESS
            : level.equals("ERROR")
                ? LumoUtility.TextColor.ERROR
                : level.equals("WARN")
                    ? LumoUtility.TextColor.WARNING
                    : LumoUtility.TextColor.PRIMARY);

    Span messageSpan = new Span(message);
    messageSpan.addClassNames(LumoUtility.FontSize.SMALL);

    logEntry.add(timeSpan, levelIcon, messageSpan);
    logContainer.addComponentAsFirst(logEntry);

    // Keep only last 20 log entries
    while (logContainer.getComponentCount() > 20) {
      logContainer.remove(logContainer.getComponentAt(logContainer.getComponentCount() - 1));
    }
  }

  private void showNotification(String message, NotificationVariant variant) {
    Notification notification = Notification.show(message, 5000, Notification.Position.TOP_END);
    notification.addThemeVariants(variant);
  }

  private String formatInterval(long milliseconds) {
    long seconds = milliseconds / 1000;
    long minutes = seconds / 60;
    long hours = minutes / 60;

    if (hours > 0) {
      return hours + " hour" + (hours != 1 ? "s" : "");
    } else if (minutes > 0) {
      return minutes + " minute" + (minutes != 1 ? "s" : "");
    } else {
      return seconds + " second" + (seconds != 1 ? "s" : "");
    }
  }

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    // Initialize UI update executor for periodic status updates
    uiUpdateExecutor = new ScheduledThreadPoolExecutor(1);

    // Update status every 30 seconds
    uiUpdateExecutor.scheduleAtFixedRate(
        () -> {
          if (getUI().isPresent() && !syncInProgress) {
            getUI().get().access(this::updateSyncStatus);
          }
        },
        30,
        30,
        TimeUnit.SECONDS);
  }

  @Override
  protected void onDetach(DetachEvent detachEvent) {
    super.onDetach(detachEvent);
    if (uiUpdateExecutor != null) {
      uiUpdateExecutor.shutdown();
    }
  }
}
