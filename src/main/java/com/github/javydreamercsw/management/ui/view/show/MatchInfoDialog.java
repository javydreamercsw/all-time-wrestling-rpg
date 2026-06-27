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
package com.github.javydreamercsw.management.ui.view.show;

import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleVariantGuide;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.util.List;
import java.util.function.Function;

public class MatchInfoDialog extends Dialog {

  public MatchInfoDialog(final SegmentType type, final List<SegmentRule> rules) {
    setDraggable(true);
    setResizable(true);
    setWidth("600px");
    setMaxHeight("80vh");

    Button closeButton = new Button("Close", e -> close());
    closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    getFooter().add(closeButton);

    VerticalLayout content = new VerticalLayout();
    content.setPadding(false);
    content.setSpacing(true);

    if (type != null) {
      content.add(new H2(type.getName()));
      if (type.getDescription() != null && !type.getDescription().isBlank()) {
        content.add(new Paragraph(type.getDescription()));
      }
    }

    List<SegmentRule> rulesWithGuide =
        rules == null ? List.of() : rules.stream().filter(r -> r.getGuide() != null).toList();

    VerticalLayout soloContent =
        buildMergedVariant(
            type != null && type.getGuide() != null ? type.getGuide().solo() : null,
            rulesWithGuide,
            r -> r.getGuide().solo(),
            type);
    if (soloContent.getComponentCount() != 0) {
      Details soloSection = new Details("Solo Play", soloContent);
      soloSection.setOpened(true);
      content.add(soloSection);
    }

    VerticalLayout multiContent =
        buildMergedVariant(
            type != null && type.getGuide() != null ? type.getGuide().multiplayer() : null,
            rulesWithGuide,
            r -> r.getGuide().multiplayer(),
            type);
    if (multiContent.getComponentCount() != 0) {
      Details multiSection = new Details("Multiplayer", multiContent);
      multiSection.setOpened(false);
      content.add(multiSection);
    }

    if (soloContent.getComponentCount() == 0 && multiContent.getComponentCount() == 0) {
      content.add(new Paragraph("No gameplay guide documented for this segment yet."));
    }

    add(content);
  }

  private VerticalLayout buildMergedVariant(
      final SegmentRuleVariantGuide typeGuide,
      final List<SegmentRule> rulesWithGuide,
      final Function<SegmentRule, SegmentRuleVariantGuide> variantExtractor,
      final SegmentType type) {
    VerticalLayout layout = new VerticalLayout();
    layout.setPadding(false);
    layout.setSpacing(false);

    boolean hasTypeContent = typeGuide != null && hasAnyContent(typeGuide);
    boolean hasRuleContent =
        rulesWithGuide.stream().map(variantExtractor).anyMatch(g -> g != null && hasAnyContent(g));

    if (!hasTypeContent && !hasRuleContent) {
      return layout;
    }

    if (hasTypeContent) {
      if (hasRuleContent) {
        layout.add(new H4(type != null ? type.getName() + " (Base Rules)" : "Base Rules"));
      }
      addVariantSections(layout, typeGuide);
    }

    for (SegmentRule rule : rulesWithGuide) {
      SegmentRuleVariantGuide ruleVariant = variantExtractor.apply(rule);
      if (ruleVariant != null && hasAnyContent(ruleVariant)) {
        layout.add(new H4(rule.getName()));
        addVariantSections(layout, ruleVariant);
      }
    }

    return layout;
  }

  private boolean hasAnyContent(final SegmentRuleVariantGuide guide) {
    return isPresent(guide.overview())
        || isPresent(guide.setup())
        || isPresent(guide.attacking())
        || isPresent(guide.defending())
        || isPresent(guide.winCondition())
        || isPresent(guide.npcRecovery())
        || isPresent(guide.topOfCageStruggle())
        || isPresent(guide.npcWinConditions())
        || isPresent(guide.concepts())
        || isPresent(guide.gameplayChanges())
        || isPresent(guide.modeSpecificAbilities())
        || isPresent(guide.gameEndConditions());
  }

  private void addVariantSections(
      final VerticalLayout parent, final SegmentRuleVariantGuide guide) {
    addSection(parent, "Overview", guide.overview());
    addSection(parent, "Setup", guide.setup());
    addSection(parent, "Attacking", guide.attacking());
    addSection(parent, "Defending", guide.defending());
    addSection(parent, "Win Condition", guide.winCondition());
    addSection(parent, "NPC Recovery", guide.npcRecovery());
    addSection(parent, "Top of Cage Struggle", guide.topOfCageStruggle());
    addSection(parent, "NPC Win Conditions", guide.npcWinConditions());
    addSection(parent, "Concepts", guide.concepts());
    addSection(parent, "Gameplay Changes", guide.gameplayChanges());
    addSection(parent, "Mode-Specific Abilities", guide.modeSpecificAbilities());
    addSection(parent, "Game End Conditions", guide.gameEndConditions());
  }

  private void addSection(final VerticalLayout parent, final String label, final String text) {
    if (!isPresent(text)) {
      return;
    }
    parent.add(new H3(label));
    parent.add(new Paragraph(text));
  }

  private boolean isPresent(final String text) {
    return text != null && !text.isBlank();
  }
}
