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
package com.github.javydreamercsw.management.ui.view.challenge;

import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.dto.challenge.ChallengeDTO;
import com.github.javydreamercsw.management.service.challenge.ChallengeService;
import com.github.javydreamercsw.management.service.expansion.ExpansionService;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility.Background;
import com.vaadin.flow.theme.lumo.LumoUtility.Border;
import com.vaadin.flow.theme.lumo.LumoUtility.BorderRadius;
import com.vaadin.flow.theme.lumo.LumoUtility.BoxShadow;
import com.vaadin.flow.theme.lumo.LumoUtility.Display;
import com.vaadin.flow.theme.lumo.LumoUtility.FlexDirection;
import com.vaadin.flow.theme.lumo.LumoUtility.FlexWrap;
import com.vaadin.flow.theme.lumo.LumoUtility.FontSize;
import com.vaadin.flow.theme.lumo.LumoUtility.FontWeight;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;
import com.vaadin.flow.theme.lumo.LumoUtility.TextColor;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "challenges", layout = MainLayout.class)
@PageTitle("Weekly Challenges")
@Menu(order = 15, icon = "vaadin:trophy", title = "Weekly Challenges")
@PermitAll
public class ChallengeListView extends VerticalLayout {

  private final ChallengeService challengeService;
  private final ExpansionService expansionService;
  private final SegmentRuleRepository segmentRuleRepository;

  private FlexLayout cardsLayout;
  private Checkbox officialOnlyFilter;
  private Checkbox myExpansionsFilter;

  @Autowired
  public ChallengeListView(
      final ChallengeService challengeService,
      final ExpansionService expansionService,
      final SegmentRuleRepository segmentRuleRepository) {
    this.challengeService = challengeService;
    this.expansionService = expansionService;
    this.segmentRuleRepository = segmentRuleRepository;

    setPadding(true);
    setSpacing(true);

    add(new H2("Weekly Challenges"));

    HorizontalLayout filters = new HorizontalLayout();
    filters.setAlignItems(Alignment.CENTER);

    officialOnlyFilter = new Checkbox("Official only", true);
    officialOnlyFilter.addValueChangeListener(e -> refreshCards());

    myExpansionsFilter = new Checkbox("My expansions only", false);
    myExpansionsFilter.addValueChangeListener(e -> refreshCards());

    filters.add(officialOnlyFilter, myExpansionsFilter);
    add(filters);

    cardsLayout = new FlexLayout();
    cardsLayout.addClassNames(Display.FLEX, FlexWrap.WRAP, Gap.MEDIUM);
    add(cardsLayout);

    refreshCards();
  }

  private void refreshCards() {
    cardsLayout.removeAll();

    List<ChallengeDTO> all = challengeService.getActiveChallenges();
    Set<String> enabledCodes = Set.copyOf(expansionService.getEnabledExpansionCodes());

    all.stream()
        .filter(c -> !officialOnlyFilter.getValue() || !"CUSTOM".equals(c.getExpansionCode()))
        .filter(
            c ->
                !myExpansionsFilter.getValue()
                    || c.getRequiredExpansions().isEmpty()
                    || enabledCodes.containsAll(c.getRequiredExpansions()))
        .forEach(c -> cardsLayout.add(buildCard(c)));
  }

  private Div buildCard(final ChallengeDTO challenge) {
    Div card = new Div();
    card.addClassNames(
        Display.FLEX,
        FlexDirection.COLUMN,
        Padding.MEDIUM,
        Background.BASE,
        Border.ALL,
        BorderRadius.MEDIUM,
        BoxShadow.SMALL);
    card.setWidth("320px");

    FlexLayout badges = new FlexLayout();
    badges.addClassNames(FlexWrap.WRAP, Gap.SMALL, Margin.Bottom.SMALL);

    if (challenge.getWeekNumber() != null) {
      Span week = new Span("Week " + challenge.getWeekNumber());
      week.getElement().getThemeList().add("badge contrast");
      badges.add(week);
    }

    if (challenge.getDifficulty() != null) {
      Span diff = new Span(challenge.getDifficulty().name());
      diff.getElement().getThemeList().add(difficultyTheme(challenge));
      badges.add(diff);
    }

    boolean isCustom = "CUSTOM".equals(challenge.getExpansionCode());
    Span sourceBadge = new Span(isCustom ? "Custom" : "Official");
    sourceBadge.getElement().getThemeList().add(isCustom ? "badge" : "badge success");
    badges.add(sourceBadge);

    if (!challenge.getRequiredExpansions().isEmpty()) {
      Span expBadge = new Span("Requires: " + String.join(", ", challenge.getRequiredExpansions()));
      expBadge.getElement().getThemeList().add("badge error");
      badges.add(expBadge);
    }

    card.add(badges);

    H3 title = new H3(challenge.getTitle());
    title.addClassNames(Margin.Top.NONE, Margin.Bottom.XSMALL, FontSize.MEDIUM);
    card.add(title);

    if (challenge.getProductLine() != null) {
      Span product = new Span(challenge.getProductLine());
      product.addClassNames(FontSize.XSMALL, TextColor.SECONDARY, Margin.Bottom.SMALL);
      card.add(product);
    }

    Paragraph objective = new Paragraph(challenge.getObjective());
    objective.addClassNames(FontSize.SMALL, Margin.Bottom.MEDIUM);
    card.add(objective);

    Button viewBtn = new Button("View Challenge", e -> openDetailDialog(challenge));
    viewBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
    card.add(viewBtn);

    return card;
  }

  private void openDetailDialog(final ChallengeDTO challenge) {
    Dialog dialog = new Dialog();
    dialog.setWidth("640px");
    dialog.setMaxWidth("95vw");

    VerticalLayout content = new VerticalLayout();
    content.setPadding(false);
    content.setSpacing(false);

    HorizontalLayout titleRow = new HorizontalLayout();
    titleRow.setAlignItems(Alignment.CENTER);
    titleRow.setWidthFull();
    titleRow.addClassName(Margin.Bottom.MEDIUM);

    H2 title = new H2(challenge.getTitle());
    title.addClassNames(Margin.NONE);
    titleRow.add(title);
    titleRow.setFlexGrow(1, title);

    Button closeBtn = new Button("✕", e -> dialog.close());
    closeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    titleRow.add(closeBtn);

    content.add(titleRow);

    if (challenge.getImageUrl() != null && !challenge.getImageUrl().isBlank()) {
      Image img = new Image(challenge.getImageUrl(), challenge.getTitle());
      img.setMaxWidth("100%");
      img.addClassName(Margin.Bottom.MEDIUM);
      content.add(img);
    }

    if (challenge.getFlavorText() != null && !challenge.getFlavorText().isBlank()) {
      Paragraph flavor = new Paragraph(challenge.getFlavorText());
      flavor.addClassNames(FontSize.SMALL, TextColor.SECONDARY, Margin.Bottom.MEDIUM);
      content.add(flavor);
    }

    addSection(content, "Objective", List.of(challenge.getObjective()), false);

    if (challenge.getSetupInstructions() != null && !challenge.getSetupInstructions().isBlank()) {
      addSection(content, "Setup", List.of(challenge.getSetupInstructions()), false);
    }

    if (challenge.getMatchType() != null) {
      H4 matchHeader = new H4("Match Type");
      matchHeader.addClassNames(Margin.Top.MEDIUM, Margin.Bottom.XSMALL, FontWeight.BOLD);
      content.add(matchHeader);

      segmentRuleRepository
          .findByName(challenge.getMatchType())
          .ifPresentOrElse(
              rule -> {
                Anchor link = new Anchor("match-info/" + rule.getId(), challenge.getMatchType());
                link.addClassNames(FontSize.SMALL, Margin.Bottom.XSMALL);
                link.getElement().addEventListener("click", e -> dialog.close());
                content.add(link);
              },
              () -> {
                Paragraph p = new Paragraph(challenge.getMatchType());
                p.addClassNames(FontSize.SMALL, Margin.Top.NONE, Margin.Bottom.XSMALL);
                content.add(p);
              });
    }

    if (!challenge.getConditions().isEmpty()) {
      addSection(content, "Conditions", challenge.getConditions(), true);
    }

    if (!challenge.getModifiers().isEmpty()) {
      addSection(content, "Modifiers", challenge.getModifiers(), true);
    }

    if (challenge.getNotes() != null && !challenge.getNotes().isBlank()) {
      Paragraph notes = new Paragraph("📌 " + challenge.getNotes());
      notes.addClassNames(FontSize.XSMALL, TextColor.SECONDARY, Margin.Top.MEDIUM);
      content.add(notes);
    }

    dialog.add(content);
    dialog.open();
  }

  private void addSection(
      final VerticalLayout parent,
      final String heading,
      final List<String> items,
      final boolean asList) {
    H4 h = new H4(heading);
    h.addClassNames(Margin.Top.MEDIUM, Margin.Bottom.XSMALL, FontWeight.BOLD);
    parent.add(h);

    if (asList && items.size() > 1) {
      UnorderedList ul = new UnorderedList();
      items.forEach(item -> ul.add(new ListItem(item)));
      parent.add(ul);
    } else {
      items.forEach(
          item -> {
            Paragraph p = new Paragraph(item);
            p.addClassNames(FontSize.SMALL, Margin.Top.NONE, Margin.Bottom.XSMALL);
            parent.add(p);
          });
    }
  }

  private String difficultyTheme(final ChallengeDTO challenge) {
    if (challenge.getDifficulty() == null) return "badge contrast";
    return switch (challenge.getDifficulty()) {
      case ENTRY, EASY -> "badge success";
      case MEDIUM -> "badge";
      case HARD, LEGENDARY -> "badge error";
    };
  }
}
