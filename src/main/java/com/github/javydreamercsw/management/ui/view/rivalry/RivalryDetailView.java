/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.ui.view.rivalry;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.feud.FeudScript;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeat;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeatStatus;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.feud.FeudScriptService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.feud.FeudScriptWizardDialog;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;

@Route("rivalry")
@PageTitle("Rivalry")
@PermitAll
public class RivalryDetailView extends Main implements HasUrlParameter<Long> {

  private final RivalryService rivalryService;
  private final WrestlerService wrestlerService;
  private final FeudScriptService feudScriptService;
  private final SegmentTypeService segmentTypeService;
  private final SegmentRuleService segmentRuleService;
  private final SecurityUtils securityUtils;

  private final VerticalLayout content = new VerticalLayout();
  private Long currentRivalryId;

  public RivalryDetailView(
      @NonNull final RivalryService rivalryService,
      @NonNull final WrestlerService wrestlerService,
      @NonNull final FeudScriptService feudScriptService,
      @NonNull final SegmentTypeService segmentTypeService,
      @NonNull final SegmentRuleService segmentRuleService,
      @NonNull final SecurityUtils securityUtils) {
    this.rivalryService = rivalryService;
    this.wrestlerService = wrestlerService;
    this.feudScriptService = feudScriptService;
    this.segmentTypeService = segmentTypeService;
    this.segmentRuleService = segmentRuleService;
    this.securityUtils = securityUtils;

    setSizeFull();
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.SMALL);

    content.setPadding(false);
    add(content);
  }

  @Override
  public void setParameter(BeforeEvent event, Long rivalryId) {
    this.currentRivalryId = rivalryId;
    reload();
  }

  private void reload() {
    content.removeAll();
    rivalryService
        .getRivalryByIdWithWrestlers(currentRivalryId)
        .ifPresentOrElse(this::buildView, () -> showNotFound(currentRivalryId));
  }

  private void buildView(Rivalry rivalry) {
    String wrestler1Name = rivalry.getWrestler1().getName();
    String wrestler2Name = rivalry.getWrestler2().getName();

    H2 title = new H2(wrestler1Name + " vs " + wrestler2Name);

    Span heatBadge = new Span("Heat: " + rivalry.getHeat());
    heatBadge.addClassNames(
        LumoUtility.Background.ERROR_10,
        LumoUtility.BorderRadius.LARGE,
        LumoUtility.Padding.Horizontal.SMALL);

    VerticalLayout info = new VerticalLayout(title, heatBadge);
    info.setPadding(false);
    info.setSpacing(false);

    if (rivalry.getStorylineNotes() != null && !rivalry.getStorylineNotes().isBlank()) {
      info.add(new Paragraph("Notes: " + rivalry.getStorylineNotes()));
    }
    if (rivalry.getStartedDate() != null) {
      info.add(new Span("Started: " + rivalry.getStartedDate()));
    }
    if (rivalry.getEndedDate() != null) {
      info.add(new Span("Ended: " + rivalry.getEndedDate()));
    }

    Button backButton = new Button("← Back to Rivalry List");
    backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    backButton.addClickListener(e -> UI.getCurrent().navigate(RivalryListView.class));

    HorizontalLayout actions = new HorizontalLayout(backButton);
    actions.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);

    if (securityUtils.canCreate()) {
      Button storyArcButton = new Button("Story Arc");
      storyArcButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      storyArcButton.addClickListener(e -> openWizard(rivalry));
      actions.add(storyArcButton);
    }

    content.add(info, actions);

    // Story arcs section
    List<FeudScript> scripts = feudScriptService.getScriptsWithBeatsForRivalry(rivalry);
    if (!scripts.isEmpty()) {
      H3 arcsHeader = new H3("Story Arcs");
      arcsHeader.addClassNames(LumoUtility.Margin.Top.MEDIUM);
      content.add(arcsHeader);
      scripts.forEach(script -> content.add(buildScriptCard(script)));
    }
  }

  private VerticalLayout buildScriptCard(FeudScript script) {
    VerticalLayout card = new VerticalLayout();
    card.setPadding(true);
    card.setSpacing(true);
    card.addClassNames(
        LumoUtility.Border.ALL, LumoUtility.BorderRadius.MEDIUM, LumoUtility.Margin.Bottom.SMALL);

    Span nameSpan = new Span(script.getName());
    nameSpan.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.SEMIBOLD);

    Span statusBadge = new Span(script.getStatus().name());
    statusBadge.getElement().getThemeList().add("badge");
    if ("COMPLETED".equals(script.getStatus().name())) {
      statusBadge.getElement().getThemeList().add("success");
    } else if ("CANCELLED".equals(script.getStatus().name())) {
      statusBadge.getElement().getThemeList().add("error");
    } else {
      statusBadge.getElement().getThemeList().add("contrast");
    }

    HorizontalLayout header = new HorizontalLayout(nameSpan, statusBadge);
    header.setAlignItems(FlexComponent.Alignment.CENTER);
    header.setFlexGrow(1, nameSpan);

    boolean editable =
        securityUtils.canCreate()
            && !"COMPLETED".equals(script.getStatus().name())
            && !"CANCELLED".equals(script.getStatus().name());

    if (editable) {
      Button editButton = new Button("Edit");
      editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
      editButton.addClickListener(e -> openEditDialog(script));

      Button cancelButton = new Button("Cancel Arc");
      cancelButton.addThemeVariants(
          ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
      cancelButton.addClickListener(e -> confirmCancelScript(script));

      header.add(editButton, cancelButton);
    }

    List<FeudScriptBeat> beats =
        script.getBeats().stream()
            .sorted(Comparator.comparing(FeudScriptBeat::getBeatOrder))
            .collect(Collectors.toList());

    if (beats.isEmpty()) {
      card.add(header, new Paragraph("No beats defined."));
      return card;
    }

    Grid<FeudScriptBeat> beatGrid = new Grid<>(FeudScriptBeat.class, false);
    beatGrid.addColumn(FeudScriptBeat::getBeatOrder).setHeader("#").setWidth("4em").setFlexGrow(0);
    beatGrid.addColumn(FeudScriptBeat::getSegmentType).setHeader("Match Type").setFlexGrow(1);
    beatGrid
        .addColumn(b -> b.getSegmentRule() != null ? b.getSegmentRule() : "—")
        .setHeader("Stipulation")
        .setFlexGrow(1);
    beatGrid.addColumn(b -> b.getWinnerControl().name()).setHeader("Winner Control").setFlexGrow(1);
    beatGrid
        .addColumn(b -> b.isCulmination() ? "★ Blowoff" : "")
        .setHeader("")
        .setWidth("6em")
        .setFlexGrow(0);
    beatGrid.addColumn(b -> b.getBeatStatus().name()).setHeader("Status").setFlexGrow(1);

    if (editable) {
      beatGrid
          .addComponentColumn(
              beat -> {
                if (beat.getBeatStatus() == FeudScriptBeatStatus.PENDING) {
                  Button removeBtn = new Button("✕");
                  removeBtn.addThemeVariants(
                      ButtonVariant.LUMO_SMALL,
                      ButtonVariant.LUMO_TERTIARY,
                      ButtonVariant.LUMO_ERROR);
                  removeBtn.addClickListener(e -> confirmRemoveBeat(script, beat));
                  return removeBtn;
                }
                return new Span();
              })
          .setWidth("4em")
          .setFlexGrow(0);
    }

    beatGrid.setItems(beats);
    beatGrid.setAllRowsVisible(true);

    card.add(header, beatGrid);
    return card;
  }

  private void openEditDialog(FeudScript script) {
    com.vaadin.flow.component.textfield.TextField nameField =
        new com.vaadin.flow.component.textfield.TextField("Arc Name");
    nameField.setValue(script.getName());
    nameField.setWidthFull();
    nameField.setRequired(true);

    com.vaadin.flow.component.textfield.IntegerField pleField =
        new com.vaadin.flow.component.textfield.IntegerField("Max PLE Appearances (1–3)");
    pleField.setValue(script.getMaxPleAppearances());
    pleField.setMin(1);
    pleField.setMax(3);
    pleField.setStepButtonsVisible(true);

    com.vaadin.flow.component.dialog.Dialog dialog = new com.vaadin.flow.component.dialog.Dialog();
    dialog.setHeaderTitle("Edit Story Arc");

    Button saveBtn =
        new Button(
            "Save",
            e -> {
              if (nameField.getValue().isBlank()) {
                nameField.setInvalid(true);
                return;
              }
              feudScriptService.updateScript(
                  script,
                  nameField.getValue(),
                  pleField.getValue() != null
                      ? pleField.getValue()
                      : script.getMaxPleAppearances());
              dialog.close();
              reload();
            });
    saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    Button cancelBtn = new Button("Cancel", e -> dialog.close());
    cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    dialog.add(new VerticalLayout(nameField, pleField));
    dialog.getFooter().add(cancelBtn, saveBtn);
    dialog.open();
  }

  private void confirmCancelScript(FeudScript script) {
    com.vaadin.flow.component.dialog.Dialog dialog = new com.vaadin.flow.component.dialog.Dialog();
    dialog.setHeaderTitle("Cancel Story Arc");
    dialog.add(
        new Paragraph(
            "Cancel arc \""
                + script.getName()
                + "\"? Completed beats are kept for reference, but no new beats can be added."));

    Button confirmBtn =
        new Button(
            "Cancel Arc",
            e -> {
              feudScriptService.cancelScript(script);
              dialog.close();
              reload();
            });
    confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

    Button backBtn = new Button("Keep Arc", e -> dialog.close());
    backBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    dialog.getFooter().add(backBtn, confirmBtn);
    dialog.open();
  }

  private void confirmRemoveBeat(FeudScript script, FeudScriptBeat beat) {
    com.vaadin.flow.component.dialog.Dialog dialog = new com.vaadin.flow.component.dialog.Dialog();
    dialog.setHeaderTitle("Remove Beat");
    dialog.add(
        new Paragraph(
            "Remove beat #"
                + beat.getBeatOrder()
                + " ("
                + beat.getSegmentType()
                + ")? Remaining beats will be renumbered."));

    Button confirmBtn =
        new Button(
            "Remove",
            e -> {
              feudScriptService.removeBeat(script, beat);
              dialog.close();
              reload();
            });
    confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

    Button cancelBtn = new Button("Keep Beat", e -> dialog.close());
    cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    dialog.getFooter().add(cancelBtn, confirmBtn);
    dialog.open();
  }

  private void openWizard(Rivalry rivalry) {
    List<Wrestler> allWrestlers =
        wrestlerService.getAllWrestlers().stream()
            .sorted(Comparator.comparing(Wrestler::getName))
            .collect(Collectors.toList());

    List<String> typeNames =
        segmentTypeService.findAll().stream()
            .map(SegmentType::getName)
            .sorted()
            .collect(Collectors.toList());

    List<String> ruleNames =
        segmentRuleService.findAll().stream()
            .map(SegmentRule::getName)
            .sorted()
            .collect(Collectors.toList());

    List<Wrestler> participants =
        List.of(
            wrestlerService.findById(rivalry.getWrestler1().getId()).orElseThrow(),
            wrestlerService.findById(rivalry.getWrestler2().getId()).orElseThrow());

    FeudScriptWizardDialog dialog =
        new FeudScriptWizardDialog(
            allWrestlers,
            typeNames,
            ruleNames,
            feudScriptService.getDefaultMaxPleAppearances(),
            feudScriptService,
            this::reload);
    dialog.preSelectWrestlers(participants);
    dialog.open();
  }

  private void showNotFound(Long id) {
    Notification.show("Rivalry #" + id + " not found", 3000, Notification.Position.MIDDLE)
        .addThemeVariants(NotificationVariant.LUMO_ERROR);
    UI.getCurrent().navigate(RivalryListView.class);
  }
}
