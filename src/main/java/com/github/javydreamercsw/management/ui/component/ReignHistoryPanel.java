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
package com.github.javydreamercsw.management.ui.component;

import com.github.javydreamercsw.management.dto.ranking.TitleReignDTO;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.theme.lumo.LumoUtility.*;
import java.util.List;

/**
 * The "detailed" half of the championship history: reign cards rendered with the same lazy
 * windowing as {@link HistoryTimelineComponent} — the newest {@link #INITIAL_VISIBLE_REIGNS} cards
 * initially, older ones revealed in batches through the shared trailing control (ATW-3a0t). The
 * window tracks its own position so the timeline above and the cards below expand in lockstep.
 */
public class ReignHistoryPanel extends Composite<Div> {

  /** Mirrors {@link HistoryTimelineComponent#INITIAL_VISIBLE_REIGNS}. */
  public static final int INITIAL_VISIBLE_REIGNS = HistoryTimelineComponent.INITIAL_VISIBLE_REIGNS;

  /** Mirrors {@link HistoryTimelineComponent#EXPAND_BATCH}. */
  public static final int EXPAND_BATCH = HistoryTimelineComponent.EXPAND_BATCH;

  private final List<TitleReignDTO> history;
  private final Div cardsContainer;
  private final Button expandControl;
  private int visibleCount;

  public ReignHistoryPanel(final List<TitleReignDTO> history) {
    this.history = history;
    getContent().addClassNames(Display.FLEX, FlexDirection.COLUMN, Gap.SMALL);

    cardsContainer = new Div();
    cardsContainer.addClassNames(Display.FLEX, FlexWrap.WRAP, Gap.SMALL);
    getContent().add(cardsContainer);

    expandControl = new Button();
    expandControl.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    expandControl.addClickListener(
        event -> {
          visibleCount = Math.min(visibleCount + EXPAND_BATCH, history.size());
          renderWindow();
        });

    this.visibleCount = Math.min(INITIAL_VISIBLE_REIGNS, history.size());
    renderWindow();
  }

  private void renderWindow() {
    cardsContainer.removeAll();
    // Newest first: index 0 is the current reign, cards render newest → oldest.
    for (int i = 0; i < visibleCount; i++) {
      cardsContainer.add(new ReignCardComponent(history.get(i)));
    }

    int hidden = history.size() - visibleCount;
    if (hidden > 0) {
      expandControl.setText("%d older reigns — show older…".formatted(hidden));
      if (expandControl.getParent().isEmpty()) {
        getContent().add(expandControl);
      }
    } else {
      expandControl.removeFromParent();
    }
  }
}
