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
package com.github.javydreamercsw.management.ui.view.show;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.service.show.planning.ProposedSegment;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;

public class EditSegmentDialog extends Dialog {

  // ==================== NESTED TYPES ====================

  /**
   * All reference data needed by the dialog, loaded in one async batch. Use {@link #load} to
   * pre-fetch before constructing the dialog on the UI thread.
   */
  public record PreloadedData(
      List<SegmentType> segmentTypes,
      List<SegmentRule> segmentRules,
      List<Npc> referees,
      List<Title> titles,
      List<Wrestler> activeWrestlers,
      Map<String, Wrestler> wrestlerByName) {

    public static PreloadedData load(
        final SegmentTypeRepository segmentTypeRepository,
        final SegmentRuleRepository segmentRuleRepository,
        final com.github.javydreamercsw.management.service.npc.NpcService npcService,
        final TitleService titleService,
        final WrestlerService wrestlerService,
        final Long universeId) {
      List<Wrestler> active = wrestlerService.findAllFiltered(null, null, universeId);
      Map<String, Wrestler> byName =
          wrestlerService.getAllWrestlers().stream()
              .collect(Collectors.toMap(Wrestler::getName, w -> w, (a, b) -> a));
      return new PreloadedData(
          segmentTypeRepository.findAll().stream()
              .sorted(Comparator.comparing(SegmentType::getName))
              .collect(Collectors.toList()),
          segmentRuleRepository.findAll().stream()
              .sorted(Comparator.comparing(SegmentRule::getName))
              .collect(Collectors.toList()),
          npcService.findAllByType("Referee").stream()
              .sorted(Comparator.comparing(Npc::getName))
              .collect(Collectors.toList()),
          titleService.findAll(),
          active,
          byName);
    }
  }

  /** Normalized initial values for the dialog, independent of source type. */
  public record SegmentDialogData(
      String typeName,
      Map<Integer, List<Wrestler>> teams,
      List<Wrestler> winners,
      Set<SegmentRule> segmentRules,
      Npc referee,
      String narration,
      String summary,
      String notes,
      boolean isTitleSegment,
      Set<Title> titles) {

    /** Build from a planning DTO using pre-loaded wrestler map for name resolution. */
    public static SegmentDialogData from(final ProposedSegment proposed, final PreloadedData data) {
      Map<Integer, List<Wrestler>> teams = new LinkedHashMap<>();
      if (proposed.getTeams() != null && !proposed.getTeams().isEmpty()) {
        for (int i = 0; i < proposed.getTeams().size(); i++) {
          List<Wrestler> team =
              proposed.getTeams().get(i).stream()
                  .map(name -> data.wrestlerByName().get(name))
                  .filter(java.util.Objects::nonNull)
                  .collect(Collectors.toList());
          teams.put(i + 1, team);
        }
      } else if (proposed.getParticipants() != null) {
        List<Wrestler> all =
            proposed.getParticipants().stream()
                .map(name -> data.wrestlerByName().get(name))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        for (int i = 0; i < all.size(); i++) {
          teams.put(i + 1, new ArrayList<>(List.of(all.get(i))));
        }
      }
      if (teams.isEmpty()) {
        teams.put(1, new ArrayList<>());
        teams.put(2, new ArrayList<>());
      }

      List<Wrestler> winners =
          proposed.getWinners() == null
              ? List.of()
              : proposed.getWinners().stream()
                  .map(name -> data.wrestlerByName().get(name))
                  .filter(java.util.Objects::nonNull)
                  .collect(Collectors.toList());

      Set<SegmentRule> rules =
          proposed.getRules() == null
              ? new HashSet<>()
              : proposed.getRules().stream()
                  .map(
                      name ->
                          data.segmentRules().stream()
                              .filter(r -> r.getName().equals(name))
                              .findFirst())
                  .filter(Optional::isPresent)
                  .map(Optional::get)
                  .collect(Collectors.toSet());

      Npc referee =
          proposed.getRefereeName() == null
              ? null
              : data.referees().stream()
                  .filter(n -> proposed.getRefereeName().equals(n.getName()))
                  .findFirst()
                  .orElse(null);

      return new SegmentDialogData(
          proposed.getType(),
          teams,
          winners,
          rules,
          referee,
          proposed.getNarration() != null ? proposed.getNarration() : "",
          proposed.getSummary() != null ? proposed.getSummary() : "",
          proposed.getNotes() != null ? proposed.getNotes() : "",
          Boolean.TRUE.equals(proposed.getIsTitleSegment()),
          proposed.getTitles() != null ? proposed.getTitles() : new HashSet<>());
    }

    /** Build from a persisted Segment entity. */
    public static SegmentDialogData from(final Segment segment) {
      Map<Integer, List<Wrestler>> teams = segment.getWrestlersByTeam();
      if (teams.isEmpty()) {
        for (Wrestler w : segment.getWrestlers()) {
          int next = teams.size() + 1;
          teams.put(next, new ArrayList<>(List.of(w)));
        }
      }
      if (teams.isEmpty()) {
        teams.put(1, new ArrayList<>());
        teams.put(2, new ArrayList<>());
      }

      return new SegmentDialogData(
          segment.getSegmentType() != null ? segment.getSegmentType().getName() : null,
          teams,
          segment.getWinners(),
          segment.getSegmentRules(),
          segment.getReferee(),
          segment.getNarration() != null ? segment.getNarration() : "",
          segment.getSummary() != null ? segment.getSummary() : "",
          segment.getNotes() != null ? segment.getNotes() : "",
          Boolean.TRUE.equals(segment.getIsTitleSegment()),
          segment.getTitles() != null ? segment.getTitles() : new HashSet<>());
    }
  }

  /** Data produced by the dialog when the user clicks Save. */
  public record SegmentSaveData(
      SegmentType segmentType,
      Map<Integer, List<Wrestler>> teams,
      Set<Wrestler> winners,
      Set<SegmentRule> rules,
      Npc referee,
      String narration,
      String summary,
      String notes,
      boolean isTitleSegment,
      Set<Title> titles) {}

  @FunctionalInterface
  public interface SaveCallback {
    void onSave(SegmentSaveData data);
  }

  // ==================== FIELDS ====================

  /** Kept for legacy test constructor compatibility. */
  @Getter private ProposedSegment segment;

  private final PreloadedData data;
  private final WrestlerService wrestlerService;
  private final Long universeId;

  @Getter private final TextArea narrationArea;
  @Getter private final TextArea notesArea;
  @Getter private final MultiSelectComboBox<Wrestler> participantsCombo;
  @Getter private final MultiSelectComboBox<Title> titleMultiSelectComboBox;
  @Getter private final Button saveButton;
  @Getter private final ComboBox<SegmentType> segmentTypeCombo;

  private final ComboBox<Npc> refereeCombo;
  private final ComboBox<Gender> genderFilter;
  private final ComboBox<AlignmentType> alignmentFilter;
  private final MultiSelectComboBox<SegmentRule> rulesCombo;
  private final MultiSelectComboBox<Wrestler> winnersCombo;
  private final TextArea summaryArea;
  private final Checkbox isTitleSegmentCheckbox;
  private final com.vaadin.flow.component.html.Span synergyBonusLabel;
  private final List<MultiSelectComboBox<Wrestler>> teamCombos = new ArrayList<>();
  private final VerticalLayout teamsLayout = new VerticalLayout();

  // ==================== MAIN CONSTRUCTOR ====================

  public EditSegmentDialog(
      final PreloadedData data,
      final SegmentDialogData initial,
      final WrestlerService wrestlerService,
      final Gender defaultGenderConstraint,
      final Long universeId,
      final SaveCallback onSave) {
    this.data = data;
    this.wrestlerService = wrestlerService;
    this.universeId = universeId;

    setHeaderTitle("Edit Segment");

    FormLayout formLayout = new FormLayout();
    formLayout.setResponsiveSteps(
        new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("500px", 2));

    synergyBonusLabel = new com.vaadin.flow.component.html.Span("Synergy Bonus: +0");
    synergyBonusLabel.addClassNames(
        com.vaadin.flow.theme.lumo.LumoUtility.FontSize.SMALL,
        com.vaadin.flow.theme.lumo.LumoUtility.TextColor.SUCCESS,
        com.vaadin.flow.theme.lumo.LumoUtility.FontWeight.BOLD);
    synergyBonusLabel.setId("edit-synergy-bonus-label");

    segmentTypeCombo = new ComboBox<>("Segment Type");
    segmentTypeCombo.setItems(data.segmentTypes());
    segmentTypeCombo.setItemLabelGenerator(SegmentType::getName);
    segmentTypeCombo.setWidthFull();
    segmentTypeCombo.setRequired(true);
    if (initial.typeName() != null) {
      data.segmentTypes().stream()
          .filter(t -> t.getName().equals(initial.typeName()))
          .findFirst()
          .ifPresent(segmentTypeCombo::setValue);
    }
    segmentTypeCombo.setId("edit-segment-type-combo-box");

    refereeCombo = new ComboBox<>("Referee");
    refereeCombo.setItems(data.referees());
    refereeCombo.setItemLabelGenerator(Npc::getName);
    refereeCombo.setWidthFull();
    refereeCombo.setValue(initial.referee());
    refereeCombo.setId("edit-referee-combo-box");

    rulesCombo = new MultiSelectComboBox<>("Segment Rules");
    rulesCombo.setItems(data.segmentRules());
    rulesCombo.setItemLabelGenerator(SegmentRule::getName);
    rulesCombo.setWidthFull();
    rulesCombo.setValue(initial.segmentRules());
    rulesCombo.setId("edit-segment-rules-combo-box");
    formLayout.setColspan(rulesCombo, 2);

    alignmentFilter = new ComboBox<>("Alignment Filter");
    alignmentFilter.setItems(AlignmentType.values());
    alignmentFilter.setClearButtonVisible(true);
    alignmentFilter.setPlaceholder("All alignments");
    alignmentFilter.setWidthFull();
    alignmentFilter.setId("edit-alignment-filter-combo-box");

    genderFilter = new ComboBox<>("Gender Filter");
    genderFilter.setItems(Gender.values());
    genderFilter.setClearButtonVisible(true);
    genderFilter.setPlaceholder("All genders");
    genderFilter.setWidthFull();
    genderFilter.setValue(defaultGenderConstraint);
    genderFilter.setId("edit-gender-filter-combo-box");

    // Winners combo — defined before teams so team lambdas can capture it
    winnersCombo = new MultiSelectComboBox<>("Winners (Optional)");
    winnersCombo.setItemLabelGenerator(Wrestler::getName);
    winnersCombo.setWidthFull();
    winnersCombo.setId("edit-winners-combo-box");

    // Dummy field to satisfy @Getter contract (teams replace participantsCombo functionally)
    participantsCombo = new MultiSelectComboBox<>();
    participantsCombo.setVisible(false);

    Runnable refreshWinners =
        () -> {
          Set<Wrestler> allSelected =
              teamCombos.stream().flatMap(c -> c.getValue().stream()).collect(Collectors.toSet());
          Set<Wrestler> currentWinners = new HashSet<>(winnersCombo.getValue());
          winnersCombo.setItems(
              allSelected.stream()
                  .sorted(Comparator.comparing(Wrestler::getName))
                  .collect(Collectors.toList()));
          winnersCombo.setValue(
              currentWinners.stream().filter(allSelected::contains).collect(Collectors.toSet()));
          updateSynergyBonus(allSelected);
        };

    java.util.function.Consumer<Set<Wrestler>> addTeamRow =
        initialWrestlers -> {
          int teamNum = teamCombos.size() + 1;
          MultiSelectComboBox<Wrestler> teamCombo = new MultiSelectComboBox<>("Team " + teamNum);
          teamCombo.setItemLabelGenerator(Wrestler::getName);
          teamCombo.setWidthFull();
          teamCombo.setItems(getFilteredWrestlers(initialWrestlers));
          if (!initialWrestlers.isEmpty()) {
            teamCombo.setValue(initialWrestlers);
          }
          teamCombo.addValueChangeListener(e -> refreshWinners.run());
          teamCombos.add(teamCombo);

          Button removeTeamButton = new Button(new Icon(VaadinIcon.MINUS));
          removeTeamButton.addThemeVariants(
              ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);
          removeTeamButton.setTooltipText("Remove Team");
          HorizontalLayout teamRow = new HorizontalLayout(teamCombo, removeTeamButton);
          teamRow.setFlexGrow(1, teamCombo);
          teamRow.setAlignItems(HorizontalLayout.Alignment.END);
          teamRow.setWidthFull();
          removeTeamButton.addClickListener(
              e -> {
                teamsLayout.remove(teamRow);
                teamCombos.remove(teamCombo);
                for (int i = 0; i < teamCombos.size(); i++) {
                  teamCombos.get(i).setLabel("Team " + (i + 1));
                }
                refreshWinners.run();
              });
          teamsLayout.add(teamRow);
          refreshWinners.run();
        };

    // Populate teams from initial data
    initial.teams().forEach((teamNum, wrestlers) -> addTeamRow.accept(new HashSet<>(wrestlers)));

    alignmentFilter.addValueChangeListener(
        e -> {
          for (MultiSelectComboBox<Wrestler> combo : teamCombos) {
            Set<Wrestler> current = combo.getValue();
            combo.setItems(getFilteredWrestlers(current));
            combo.setValue(current);
          }
        });
    genderFilter.addValueChangeListener(
        e -> {
          for (MultiSelectComboBox<Wrestler> combo : teamCombos) {
            Set<Wrestler> current = combo.getValue();
            combo.setItems(getFilteredWrestlers(current));
            combo.setValue(current);
          }
        });

    Button addTeamButton = new Button("Add Team", new Icon(VaadinIcon.PLUS));
    addTeamButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    addTeamButton.setId("edit-add-team-button");
    addTeamButton.addClickListener(e -> addTeamRow.accept(new HashSet<>()));

    teamsLayout.setSpacing(true);
    teamsLayout.setPadding(false);
    VerticalLayout teamsSection = new VerticalLayout(teamsLayout, addTeamButton);
    teamsSection.setSpacing(false);
    teamsSection.setPadding(false);
    formLayout.setColspan(teamsSection, 2);

    winnersCombo.setValue(new HashSet<>(initial.winners()));

    summaryArea = new TextArea("Summary");
    summaryArea.setWidthFull();
    summaryArea.setValue(initial.summary());
    summaryArea.setId("edit-summary-text-area");
    formLayout.setColspan(summaryArea, 2);

    narrationArea = new TextArea("Narration");
    narrationArea.setWidthFull();
    narrationArea.setValue(initial.narration());
    narrationArea.setId("edit-narration-text-area");
    formLayout.setColspan(narrationArea, 2);

    notesArea = new TextArea("Match Feedback / Notes");
    notesArea.setWidthFull();
    notesArea.setValue(initial.notes());
    notesArea.setId("edit-notes-text-area");
    notesArea.setPlaceholder("Provide specific instructions for the AI narration...");
    formLayout.setColspan(notesArea, 2);

    titleMultiSelectComboBox = new MultiSelectComboBox<>("Titles");
    titleMultiSelectComboBox.setItems(data.titles());
    titleMultiSelectComboBox.setItemLabelGenerator(Title::getName);
    titleMultiSelectComboBox.setWidthFull();
    titleMultiSelectComboBox.setId("edit-title-multi-select-combo-box");

    isTitleSegmentCheckbox = new Checkbox("Is Title Segment");
    isTitleSegmentCheckbox.setId("edit-is-title-segment-checkbox");
    isTitleSegmentCheckbox.addValueChangeListener(
        event -> {
          titleMultiSelectComboBox.setVisible(event.getValue());
          if (!event.getValue()) {
            titleMultiSelectComboBox.clear();
          }
        });
    isTitleSegmentCheckbox.setValue(initial.isTitleSegment());
    titleMultiSelectComboBox.setVisible(initial.isTitleSegment());
    if (initial.isTitleSegment()) {
      titleMultiSelectComboBox.setValue(initial.titles());
    }

    formLayout.add(
        segmentTypeCombo,
        rulesCombo,
        refereeCombo,
        alignmentFilter,
        genderFilter,
        teamsSection,
        synergyBonusLabel,
        winnersCombo,
        isTitleSegmentCheckbox,
        titleMultiSelectComboBox,
        summaryArea,
        notesArea,
        narrationArea);

    saveButton =
        new Button(
            "Save",
            e -> {
              Map<Integer, List<Wrestler>> teamMap = new LinkedHashMap<>();
              for (int i = 0; i < teamCombos.size(); i++) {
                teamMap.put(i + 1, new ArrayList<>(teamCombos.get(i).getValue()));
              }
              onSave.onSave(
                  new SegmentSaveData(
                      segmentTypeCombo.getValue(),
                      teamMap,
                      new HashSet<>(winnersCombo.getValue()),
                      new HashSet<>(rulesCombo.getValue()),
                      refereeCombo.getValue(),
                      narrationArea.getValue(),
                      summaryArea.getValue(),
                      notesArea.getValue(),
                      isTitleSegmentCheckbox.getValue(),
                      titleMultiSelectComboBox.getValue()));
            });

    saveButton.setId("edit-segment-save-button");
    Button cancelButton = new Button("Cancel", e -> close());
    getFooter().add(cancelButton, saveButton);
    add(new VerticalLayout(formLayout));
  }

  // ==================== CONVENIENCE CONSTRUCTORS ====================

  /** Fast constructor for ShowPlanningView: accepts pre-loaded data and a ProposedSegment. */
  public EditSegmentDialog(
      final ProposedSegment segment,
      final PreloadedData data,
      final WrestlerService wrestlerService,
      final Gender defaultGenderConstraint,
      final Long universeId,
      final Runnable onSave) {
    this(
        data,
        SegmentDialogData.from(segment, data),
        wrestlerService,
        defaultGenderConstraint,
        universeId,
        saveData -> {
          segment.setType(saveData.segmentType().getName());
          segment.setNarration(saveData.narration());
          segment.setSummary(saveData.summary());
          segment.setNotes(saveData.notes());
          List<List<String>> teams =
              saveData.teams().values().stream()
                  .map(
                      wrestlers ->
                          wrestlers.stream().map(Wrestler::getName).collect(Collectors.toList()))
                  .collect(Collectors.toList());
          segment.setTeams(teams);
          segment.setParticipants(
              saveData.teams().values().stream()
                  .flatMap(List::stream)
                  .map(Wrestler::getName)
                  .collect(Collectors.toList()));
          segment.setWinners(
              saveData.winners().stream().map(Wrestler::getName).collect(Collectors.toList()));
          segment.setRules(
              saveData.rules().stream().map(SegmentRule::getName).collect(Collectors.toList()));
          segment.setRefereeName(saveData.referee() != null ? saveData.referee().getName() : null);
          segment.setIsTitleSegment(saveData.isTitleSegment());
          segment.setTitles(saveData.titles());
          onSave.run();
        });
    this.segment = segment;
  }

  /** Legacy constructor kept for test backward compatibility. Builds PreloadedData inline. */
  public EditSegmentDialog(
      final ProposedSegment segment,
      final WrestlerRepository wrestlerRepository,
      final WrestlerService wrestlerService,
      final TitleService titleService,
      final SegmentTypeRepository segmentTypeRepository,
      final SegmentRuleRepository segmentRuleRepository,
      final com.github.javydreamercsw.management.service.npc.NpcService npcService,
      final Gender defaultGenderConstraint,
      final Long universeId,
      final Runnable onSave) {
    this(
        segment,
        PreloadedData.load(
            segmentTypeRepository,
            segmentRuleRepository,
            npcService,
            titleService,
            wrestlerService,
            universeId),
        wrestlerService,
        defaultGenderConstraint,
        universeId,
        onSave);
  }

  // ==================== PRIVATE HELPERS ====================

  private List<Wrestler> getFilteredWrestlers(final Set<Wrestler> forceInclude) {
    AlignmentType alignment = alignmentFilter.getValue();
    Gender gender = genderFilter.getValue();

    if (alignment != null) {
      return wrestlerService.findAllFiltered(alignment, gender, universeId, forceInclude);
    }

    Set<Long> includeIds =
        forceInclude == null
            ? Set.of()
            : forceInclude.stream().map(Wrestler::getId).collect(Collectors.toSet());

    return data.activeWrestlers().stream()
        .filter(w -> includeIds.contains(w.getId()) || (gender == null || w.getGender() == gender))
        .sorted(Comparator.comparing(Wrestler::getName))
        .collect(Collectors.toList());
  }

  private void updateSynergyBonus(final java.util.Collection<Wrestler> wrestlers) {
    int totalBonus = 0;
    Map<Long, Integer> factionCounts = new HashMap<>();
    Map<Long, Integer> factionAffinity = new HashMap<>();

    for (Wrestler w : wrestlers) {
      if (!org.hibernate.Hibernate.isInitialized(w.getWrestlerStates())) {
        continue;
      }
      w.getDefaultState()
          .map(WrestlerState::getFaction)
          .ifPresent(
              faction -> {
                Long fid = faction.getId();
                factionCounts.put(fid, factionCounts.getOrDefault(fid, 0) + 1);
                factionAffinity.put(fid, faction.getAffinity());
              });
    }

    for (Map.Entry<Long, Integer> entry : factionCounts.entrySet()) {
      int count = entry.getValue();
      if (count > 1) {
        int affinity = factionAffinity.get(entry.getKey());
        totalBonus += (count - 1) * (affinity / 10);
      }
    }

    synergyBonusLabel.setText("Faction Synergy Bonus: +" + totalBonus + " weight");
    synergyBonusLabel.setVisible(totalBonus > 0);
  }

  public void save() {
    saveButton.click();
  }
}
