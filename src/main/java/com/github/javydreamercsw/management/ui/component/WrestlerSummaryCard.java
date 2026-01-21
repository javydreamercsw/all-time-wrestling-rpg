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

import com.github.javydreamercsw.base.domain.wrestler.WrestlerStats;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.theme.lumo.LumoUtility.*;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/** A reusable card component for displaying wrestler summaries in matches and lists. */
@Slf4j
public class WrestlerSummaryCard extends Composite<VerticalLayout> {

  public WrestlerSummaryCard(
      @NonNull Wrestler wrestler, @NonNull WrestlerService wrestlerService, boolean isPlayer) {
    getContent().setPadding(true);
    getContent().setSpacing(false);

    if (isPlayer) {
      getContent()
          .addClassNames(
              Background.PRIMARY_10,
              Border.ALL,
              BorderColor.PRIMARY,
              BorderRadius.MEDIUM,
              Margin.Bottom.MEDIUM);
    } else {
      getContent()
          .addClassNames(
              Background.CONTRAST_5, BorderRadius.MEDIUM, Margin.Bottom.SMALL, Padding.SMALL);
    }

    H4 name = new H4(wrestler.getName() + (isPlayer ? " (YOU)" : ""));
    name.addClassNames(Margin.NONE, TextColor.PRIMARY);
    getContent().add(name);

    if (wrestler.getId() == null) {
      log.warn("Wrestler {} has NO ID!", wrestler.getName());
      return;
    }

    HorizontalLayout statsRow = new HorizontalLayout();
    statsRow.setSpacing(true);
    statsRow.addClassNames(FontSize.SMALL);

    try {
      Optional<WrestlerStats> stats = wrestlerService.getWrestlerStats(wrestler.getId());
      if (stats.isPresent()) {
        WrestlerStats wrestlerStats = stats.get();
        statsRow.add(new Span("Wins: " + wrestlerStats.getWins()));
        statsRow.add(new Span("Losses: " + wrestlerStats.getLosses()));
      }
    } catch (Exception e) {
      log.error("Error getting stats for wrestler: " + wrestler.getId(), e);
    }
    getContent().add(statsRow);

    wrestlerService
        .findByIdWithInjuries(wrestler.getId())
        .ifPresent(
            wrestlerWithInjuries -> {
              Span bumps = new Span("Bumps: " + wrestlerWithInjuries.getBumps());
              bumps.addClassNames(FontSize.SMALL, FontWeight.MEDIUM);
              getContent().add(bumps);

              // Campaign Modifiers
              if (wrestler.getAlignment() != null
                  && wrestler.getAlignment().getCampaign() != null
                  && wrestler.getAlignment().getCampaign().getState() != null) {
                CampaignState state = wrestler.getAlignment().getCampaign().getState();

                VerticalLayout mods = new VerticalLayout();
                mods.setPadding(false);
                mods.setSpacing(false);
                mods.addClassNames(Margin.Top.SMALL, Padding.Top.SMALL, Border.TOP);

                Span hp = new Span("❤️ Effective HP: " + wrestler.getEffectiveStartingHealth());

                Span stam =
                    new Span("⚡ Effective Stamina: " + wrestler.getEffectiveStartingStamina());

                hp.addClassNames(FontSize.XSMALL, FontWeight.BOLD);

                stam.addClassNames(FontSize.XSMALL, FontWeight.BOLD);

                mods.add(hp, stam);

                // Campaign Attributes

                HorizontalLayout attrs = new HorizontalLayout();

                attrs.setSpacing(true);

                attrs.addClassNames(FontSize.XSMALL, Margin.Vertical.XSMALL);

                attrs.add(new Span("DRV: " + wrestler.getDrive()));

                attrs.add(new Span("RES: " + wrestler.getResilience()));

                attrs.add(new Span("CHA: " + wrestler.getCharisma()));

                attrs.add(new Span("BRL: " + wrestler.getBrawl()));

                mods.add(attrs);

                if (!state.getUpgrades().isEmpty()) {
                  state
                      .getUpgrades()
                      .forEach(
                          upgrade -> {
                            Span p = new Span("✨ " + upgrade.getName());
                            p.addClassNames(FontSize.XSMALL, TextColor.SUCCESS, FontWeight.BOLD);
                            mods.add(p);
                          });
                }

                if (!state.getActiveCards().isEmpty()) {
                  mods.add(new Span("Cards:"));
                  HorizontalLayout cardsLayout = new HorizontalLayout();
                  cardsLayout.addClassNames(FlexWrap.WRAP, Gap.XSMALL);
                  state
                      .getActiveCards()
                      .forEach(
                          card -> {
                            Span c = new Span(card.getName());
                            c.addClassNames(
                                FontSize.XSMALL,
                                Background.CONTRAST_10,
                                Padding.Horizontal.XSMALL,
                                BorderRadius.SMALL);
                            Tooltip.forComponent(c).setText(card.getDescription());
                            cardsLayout.add(c);
                          });
                  mods.add(cardsLayout);
                }
                getContent().add(mods);
              }

              if (!wrestlerWithInjuries.getInjuries().isEmpty()) {
                VerticalLayout injuryList = new VerticalLayout();
                injuryList.setPadding(false);
                injuryList.setSpacing(false);
                wrestlerWithInjuries
                    .getInjuries()
                    .forEach(
                        injury -> {
                          Span i = new Span("🩹 " + injury.getDisplayString());
                          i.addClassNames(FontSize.XSMALL, TextColor.ERROR);
                          injuryList.add(i);
                        });
                getContent().add(injuryList);
              }
            });
  }
}
