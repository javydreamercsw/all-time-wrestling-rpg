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
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRulePlayGuide;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleVariantGuide;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class MatchInfoDialog extends Dialog {

  public MatchInfoDialog(final SegmentRule rule) {
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

    content.add(new H2(rule.getName()));

    if (rule.getDescription() != null && !rule.getDescription().isBlank()) {
      content.add(new Paragraph(rule.getDescription()));
    }

    SegmentRulePlayGuide guide = rule.getRules();
    if (guide == null) {
      content.add(new Paragraph("No gameplay rules documented for this match type yet."));
    } else {
      if (guide.solo() != null) {
        Details soloSection = new Details("Solo Play", buildVariantContent(guide.solo()));
        soloSection.setOpened(true);
        content.add(soloSection);
      }
      if (guide.multiplayer() != null) {
        Details multiSection = new Details("Multiplayer", buildVariantContent(guide.multiplayer()));
        multiSection.setOpened(false);
        content.add(multiSection);
      }
    }

    add(content);
  }

  private VerticalLayout buildVariantContent(final SegmentRuleVariantGuide guide) {
    VerticalLayout layout = new VerticalLayout();
    layout.setPadding(false);
    layout.setSpacing(false);

    addSection(layout, "Overview", guide.overview());
    addSection(layout, "Setup", guide.setup());
    addSection(layout, "Attacking", guide.attacking());
    addSection(layout, "Defending", guide.defending());
    addSection(layout, "Win Condition", guide.winCondition());
    addSection(layout, "NPC Recovery", guide.npcRecovery());
    addSection(layout, "Top of Cage Struggle", guide.topOfCageStruggle());
    addSection(layout, "NPC Win Conditions", guide.npcWinConditions());
    addSection(layout, "Concepts", guide.concepts());
    addSection(layout, "Gameplay Changes", guide.gameplayChanges());
    addSection(layout, "Mode-Specific Abilities", guide.modeSpecificAbilities());
    addSection(layout, "Game End Conditions", guide.gameEndConditions());

    return layout;
  }

  private void addSection(final VerticalLayout parent, final String label, final String text) {
    if (text == null || text.isBlank()) {
      return;
    }
    parent.add(new H3(label));
    parent.add(new Paragraph(text));
  }
}
