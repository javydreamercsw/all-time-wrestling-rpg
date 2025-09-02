package com.github.javydreamercsw.management.ui.view;

import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.team.TeamStatus;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.TeamDTO;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.team.TeamService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.StringLengthValidator;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Dialog for creating and editing teams. Provides form fields for team name, description,
 * wrestlers, faction, and status.
 */
@Component
@Slf4j
public class TeamFormDialog extends Dialog {

  private final TeamService teamService;
  private final WrestlerService wrestlerService;
  private final FactionService factionService;

  // Form fields
  private final TextField nameField;
  private final TextArea descriptionField;
  private final ComboBox<Wrestler> wrestler1Field;
  private final ComboBox<Wrestler> wrestler2Field;
  private final ComboBox<Faction> factionField;
  private final ComboBox<TeamStatus> statusField;

  // Form components
  private final FormLayout formLayout;
  private final Button saveButton;
  private final Button cancelButton;
  private final Binder<TeamDTO> binder;

  // Current team being edited (null for new team)
  private TeamDTO currentTeam;

  @Autowired
  public TeamFormDialog(
      TeamService teamService, WrestlerService wrestlerService, FactionService factionService) {
    this.teamService = teamService;
    this.wrestlerService = wrestlerService;
    this.factionService = factionService;

    // Initialize form fields
    this.nameField = new TextField("Team Name");
    this.descriptionField = new TextArea("Description");
    this.wrestler1Field = new ComboBox<>("First Wrestler");
    this.wrestler2Field = new ComboBox<>("Second Wrestler");
    this.factionField = new ComboBox<>("Faction");
    this.statusField = new ComboBox<>("Status");

    // Initialize form components
    this.formLayout = new FormLayout();
    this.saveButton = new Button("Save");
    this.cancelButton = new Button("Cancel");
    this.binder = new Binder<>(TeamDTO.class);

    configureDialog();
    configureForm();
    configureButtons();
    configureBinder();
  }

  private void configureDialog() {
    setModal(true);
    setDraggable(true);
    setResizable(true);
    setWidth("600px");
    setHeight("500px");
  }

  private void configureForm() {
    // Configure name field
    nameField.setRequired(true);
    nameField.setMaxLength(255);
    nameField.setWidthFull();

    // Configure description field
    descriptionField.setMaxLength(255);
    descriptionField.setWidthFull();
    descriptionField.setHeight("100px");

    // Configure wrestler fields
    wrestler1Field.setRequired(true);
    wrestler1Field.setItemLabelGenerator(Wrestler::getName);
    wrestler1Field.setWidthFull();

    wrestler2Field.setRequired(true);
    wrestler2Field.setItemLabelGenerator(Wrestler::getName);
    wrestler2Field.setWidthFull();

    // Configure faction field
    factionField.setItemLabelGenerator(Faction::getName);
    factionField.setWidthFull();

    // Configure status field
    statusField.setItems(TeamStatus.values());
    statusField.setItemLabelGenerator(TeamStatus::getDisplayName);
    statusField.setWidthFull();

    // Add validation to prevent same wrestler selection
    wrestler1Field.addValueChangeListener(e -> validateWrestlerSelection());
    wrestler2Field.addValueChangeListener(e -> validateWrestlerSelection());

    // Layout form fields
    formLayout.add(
        nameField, descriptionField, wrestler1Field, wrestler2Field, factionField, statusField);
    formLayout.setResponsiveSteps(
        new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("500px", 2));
    formLayout.setColspan(nameField, 2);
    formLayout.setColspan(descriptionField, 2);
  }

  private void configureButtons() {
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    saveButton.addClickListener(e -> saveTeam());

    cancelButton.addClickListener(e -> close());

    HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
    buttonLayout.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);
    buttonLayout.setWidthFull();

    add(formLayout, buttonLayout);
  }

  private void configureBinder() {
    // Bind name field
    binder
        .forField(nameField)
        .withValidator(
            new StringLengthValidator("Team name must be between 1 and 255 characters", 1, 255))
        .bind(TeamDTO::getName, TeamDTO::setName);

    // Bind description field (no length limit for Notion sync compatibility)
    binder.forField(descriptionField).bind(TeamDTO::getDescription, TeamDTO::setDescription);

    // Bind wrestler fields
    binder
        .forField(wrestler1Field)
        .withValidator(wrestler -> wrestler != null, "First wrestler is required")
        .bind(
            dto -> wrestlerService.getWrestlerById(dto.getWrestler1Id()).orElse(null),
            (dto, wrestler) -> {
              if (wrestler != null) {
                dto.setWrestler1Id(wrestler.getId());
                dto.setWrestler1Name(wrestler.getName());
              }
            });

    binder
        .forField(wrestler2Field)
        .withValidator(wrestler -> wrestler != null, "Second wrestler is required")
        .bind(
            dto -> wrestlerService.getWrestlerById(dto.getWrestler2Id()).orElse(null),
            (dto, wrestler) -> {
              if (wrestler != null) {
                dto.setWrestler2Id(wrestler.getId());
                dto.setWrestler2Name(wrestler.getName());
              }
            });

    // Bind faction field
    binder
        .forField(factionField)
        .bind(
            dto ->
                dto.getFactionId() != null
                    ? factionService.getFactionById(dto.getFactionId()).orElse(null)
                    : null,
            (dto, faction) -> {
              if (faction != null) {
                dto.setFactionId(faction.getId());
                dto.setFactionName(faction.getName());
              } else {
                dto.setFactionId(null);
                dto.setFactionName(null);
              }
            });

    // Bind status field
    binder.forField(statusField).bind(TeamDTO::getStatus, TeamDTO::setStatus);
  }

  private void validateWrestlerSelection() {
    Wrestler wrestler1 = wrestler1Field.getValue();
    Wrestler wrestler2 = wrestler2Field.getValue();

    if (wrestler1 != null && wrestler2 != null && wrestler1.equals(wrestler2)) {
      showErrorNotification("Both wrestlers cannot be the same person");
      wrestler2Field.setValue(null);
    }
  }

  private void loadComboBoxData() {
    // Load wrestlers
    wrestler1Field.setItems(wrestlerService.getAllWrestlers());
    wrestler2Field.setItems(wrestlerService.getAllWrestlers());

    // Load factions
    factionField.setItems(factionService.getAllFactions());
  }

  public void setTeam(TeamDTO team) {
    this.currentTeam = team;

    // Load combo box data
    loadComboBoxData();

    if (team == null) {
      // New team
      setHeaderTitle("Create New Team");
      TeamDTO newTeam = new TeamDTO();
      newTeam.setStatus(TeamStatus.ACTIVE);
      binder.setBean(newTeam);
      statusField.setVisible(false); // Hide status for new teams
    } else {
      // Edit existing team
      setHeaderTitle("Edit Team: " + team.getName());
      binder.setBean(team);
      statusField.setVisible(true); // Show status for existing teams
    }
  }

  private void saveTeam() {
    try {
      TeamDTO teamToSave = binder.getBean();
      binder.writeBean(teamToSave);

      // Validate wrestler selection
      if (teamToSave.getWrestler1Id() == null || teamToSave.getWrestler2Id() == null) {
        showErrorNotification("Both wrestlers must be selected");
        return;
      }

      if (teamToSave.getWrestler1Id().equals(teamToSave.getWrestler2Id())) {
        showErrorNotification("Both wrestlers cannot be the same person");
        return;
      }

      Optional<com.github.javydreamercsw.management.domain.team.Team> result;

      if (currentTeam == null || currentTeam.getId() == null) {
        // Create new team
        result =
            teamService.createTeam(
                teamToSave.getName(),
                teamToSave.getDescription(),
                teamToSave.getWrestler1Id(),
                teamToSave.getWrestler2Id(),
                teamToSave.getFactionId());
      } else {
        // Update existing team
        result =
            teamService.updateTeam(
                currentTeam.getId(),
                teamToSave.getName(),
                teamToSave.getDescription(),
                teamToSave.getStatus(),
                teamToSave.getFactionId());
      }

      if (result.isPresent()) {
        showSuccessNotification(
            currentTeam == null ? "Team created successfully" : "Team updated successfully");
        close();
      } else {
        showErrorNotification(
            "Failed to save team. Please check for duplicate names or other conflicts.");
      }

    } catch (ValidationException e) {
      showErrorNotification("Please fix the validation errors and try again");
    } catch (Exception e) {
      log.error("Error saving team", e);
      showErrorNotification("An error occurred while saving the team: " + e.getMessage());
    }
  }

  private void showSuccessNotification(String message) {
    Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
    notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
  }

  private void showErrorNotification(String message) {
    Notification notification = Notification.show(message, 5000, Notification.Position.TOP_CENTER);
    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
  }
}
