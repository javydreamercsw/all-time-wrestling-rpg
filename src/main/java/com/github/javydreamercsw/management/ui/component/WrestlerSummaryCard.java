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
    this(wrestler, wrestlerService, isPlayer, 0);
  }

  public WrestlerSummaryCard(
      @NonNull Wrestler wrestler,
      @NonNull WrestlerService wrestlerService,
      boolean isPlayer,
      int additionalPenalty) {
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
              // Always show effective HP if we have penalties or bonuses, even if not the player's
              // card directly (e.g. opponent)
              boolean hasCampaign =
                  wrestler.getAlignment() != null
                      && wrestler.getAlignment().getCampaign() != null
                      && wrestler.getAlignment().getCampaign().getState() != null;

              int effectiveHp =
                  Math.max(1, wrestler.getEffectiveStartingHealth() - additionalPenalty);

              VerticalLayout mods = new VerticalLayout();
              mods.setPadding(false);
              mods.setSpacing(false);
              mods.addClassNames(Margin.Top.SMALL, Padding.Top.SMALL, Border.TOP);

              StringBuilder hpTooltip = new StringBuilder();
              hpTooltip.append("Base Health: ").append(wrestler.getStartingHealth());

              int campaignBonus = 0;
              int campaignPenalty = 0;
              if (wrestler.getAlignment() != null
                  && wrestler.getAlignment().getCampaign() != null
                  && wrestler.getAlignment().getCampaign().getState() != null) {
                campaignBonus =
                    wrestler.getAlignment().getCampaign().getState().getCampaignHealthBonus();
                campaignPenalty =
                    wrestler.getAlignment().getCampaign().getState().getHealthPenalty();
                if (campaignBonus > 0)
                  hpTooltip.append("\nCampaign Bonus: +").append(campaignBonus);
                if (campaignPenalty > 0)
                  hpTooltip.append("\nCampaign Penalty: -").append(campaignPenalty);
              }

              if (wrestlerWithInjuries.getBumps() > 0) {
                hpTooltip.append("\nBump Penalty: -").append(wrestlerWithInjuries.getBumps());
              }

              int injuryPenalty = wrestler.getTotalInjuryPenalty();
              if (injuryPenalty > 0) {
                hpTooltip.append("\nInjury Penalty: -").append(injuryPenalty);
              }

              if (additionalPenalty > 0) {
                hpTooltip.append("\nOpponent Penalty: -").append(additionalPenalty);
              }

              String hpText = "❤️ Effective HP: " + effectiveHp;
              Span hp = new Span(hpText);
              if (wrestlerWithInjuries.getBumps() > 0) {
                Span bumpIndicator =
                    new Span(" (📉 -" + wrestlerWithInjuries.getBumps() + " bumps)");
                bumpIndicator.addClassNames(TextColor.ERROR, FontSize.XSMALL, Margin.Left.XSMALL);
                hp.add(bumpIndicator);
              }
              Tooltip.forComponent(hp).setText(hpTooltip.toString());

              Span stam =
                  new Span("⚡ Effective Stamina: " + wrestler.getEffectiveStartingStamina());

              hp.addClassNames(FontSize.XSMALL, FontWeight.BOLD);
              stam.addClassNames(FontSize.XSMALL, FontWeight.BOLD);

              mods.add(hp, stam);

              // Campaign Attributes (Only show if they have them set, defaults are 1)
              HorizontalLayout attrs = new HorizontalLayout();
              attrs.setSpacing(true);
              attrs.addClassNames(FontSize.XSMALL, Margin.Vertical.XSMALL);
              attrs.add(new Span("DRV: " + wrestler.getDrive()));
              attrs.add(new Span("RES: " + wrestler.getResilience()));
              attrs.add(new Span("CHA: " + wrestler.getCharisma()));
              attrs.add(new Span("BRL: " + wrestler.getBrawl()));
              mods.add(attrs);

              if (hasCampaign) {
                CampaignState state = wrestler.getAlignment().getCampaign().getState();
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
              }
              getContent().add(mods);

              if (!wrestlerWithInjuries.getInjuries().isEmpty()) {
                VerticalLayout activeInjuries = new VerticalLayout();
                activeInjuries.setPadding(false);
                activeInjuries.setSpacing(false);

                VerticalLayout healedInjuries = new VerticalLayout();
                healedInjuries.setPadding(false);
                healedInjuries.setSpacing(false);

                wrestlerWithInjuries
                    .getInjuries()
                    .forEach(
                        injury -> {
                          Span i = new Span("🩹 " + injury.getDisplayString());
                          i.addClassNames(FontSize.XSMALL);
                          if (injury.isCurrentlyActive()) {
                            i.addClassNames(TextColor.ERROR);
                            activeInjuries.add(i);
                          } else {
                            i.addClassNames(TextColor.SECONDARY);
                            healedInjuries.add(i);
                          }
                        });

                if (activeInjuries.getComponentCount() > 0) {
                  getContent().add(activeInjuries);
                }

                if (healedInjuries.getComponentCount() > 0) {
                  com.vaadin.flow.component.details.Details healedDetails =
                      new com.vaadin.flow.component.details.Details(
                          "Healed Injuries", healedInjuries);
                  healedDetails.addThemeVariants(
                      com.vaadin.flow.component.details.DetailsVariant.REVERSE,
                      com.vaadin.flow.component.details.DetailsVariant.SMALL);
                  healedDetails.setOpened(false);
                  healedDetails.addClassNames(FontSize.XSMALL, TextColor.SECONDARY);
                  getContent().add(healedDetails);
                }
              }
            });
  }
}
