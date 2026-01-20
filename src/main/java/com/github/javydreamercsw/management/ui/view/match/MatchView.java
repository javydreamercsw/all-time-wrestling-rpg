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
package com.github.javydreamercsw.management.ui.view.match;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerStats;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "match/:matchId", layout = MainLayout.class)
@PageTitle("Match | ATW RPG")
@PermitAll
@Slf4j
public class MatchView extends VerticalLayout implements BeforeEnterObserver {

  private final SegmentService segmentService;
  private final WrestlerService wrestlerService;
  private final SecurityUtils securityUtils;
  private final CampaignService campaignService;
  private final CampaignRepository campaignRepository;

  private Segment segment;
  private TextArea narrationArea;
  private MultiSelectComboBox<Wrestler> winnersComboBox;

  @Autowired
  public MatchView(
      SegmentService segmentService,
      WrestlerService wrestlerService,
      SecurityUtils securityUtils,
      CampaignService campaignService,
      CampaignRepository campaignRepository) {
    this.segmentService = segmentService;
    this.wrestlerService = wrestlerService;
    this.securityUtils = securityUtils;
    this.campaignService = campaignService;
    this.campaignRepository = campaignRepository;
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    try {
      removeAll();
      add(new H2("Match Details"));
      String matchId = event.getRouteParameters().get("matchId").orElse(null);
      if (matchId != null) {
        Optional<Segment> foundSegment = segmentService.findByIdWithShow(Long.valueOf(matchId));
        if (foundSegment.isPresent()) {
          segment = foundSegment.get();
          buildView();
        } else {
          add(new H2("Match not found."));
        }
      }
    } catch (Exception e) {
      log.error("Error in MatchView.beforeEnter", e);
      add(new H2("Error displaying match details."));
    }
  }

  private void buildView() {
    setId("match-view-" + segment.getId());
    Wrestler playerWrestler = securityUtils.getAuthenticatedUser().get().getWrestler();

    H3 showName = new H3("Show: " + segment.getShow().getName());
    showName.setId("show-name");
    add(showName);

    // Check if this is a campaign match to show a back button
    securityUtils
        .getAuthenticatedUser()
        .ifPresent(
            user -> {
              campaignRepository
                  .findActiveByWrestler(user.getWrestler())
                  .ifPresent(
                      campaign -> {
                        if (campaign.getState().getCurrentMatch() != null
                            && campaign
                                .getState()
                                .getCurrentMatch()
                                .getId()
                                .equals(segment.getId())) {
                          Button backToCampaignBtn =
                              new Button(
                                  "Back to Campaign", e -> UI.getCurrent().navigate("campaign"));
                          backToCampaignBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                          backToCampaignBtn.setId("back-to-campaign-button");
                          addComponentAsFirst(backToCampaignBtn);
                        }
                      });
            });

    Paragraph matchType = new Paragraph("Match Type: " + segment.getSegmentType().getName());
    matchType.setId("match-type");
    add(matchType);

    add(new H3("Participants"));
    List<Wrestler> opponents =
        segment.getWrestlers().stream().filter(w -> !w.equals(playerWrestler)).toList();

    add(new Paragraph("Your Wrestler: " + playerWrestler.getName()));
    add(createPlayerSummary(playerWrestler));
    add(
        new Paragraph(
            "Opponents: "
                + opponents.stream().map(Wrestler::getName).collect(Collectors.joining(", "))));

    opponents.forEach(
        opponent -> {
          add(createOpponentSummary(opponent));
        });

    add(new H3("Match Rules"));
    if (segment.getSegmentRules().isEmpty()) {
      add(new Paragraph("No special rules."));
    } else {
      segment.getSegmentRules().forEach(rule -> add(new Paragraph("- " + rule.getName())));
    }

    add(new H3("Titles on the Line"));
    if (segment.getTitles().isEmpty()) {
      add(new Paragraph("No titles on the line."));
    } else {
      segment.getTitles().forEach(title -> add(new Paragraph("- " + title.getName())));
    }

    add(new H3("Match Winners"));
    winnersComboBox = new MultiSelectComboBox<>();
    winnersComboBox.setItems(segment.getWrestlers());
    winnersComboBox.setItemLabelGenerator(Wrestler::getName);
    winnersComboBox.setValue(new HashSet<>(segment.getWinners()));
    winnersComboBox.setId("winners-combobox");
    add(winnersComboBox);

    Button saveWinnersButton = new Button("Save Winners", event -> saveWinners());
    saveWinnersButton.setId("save-winners-button");
    add(saveWinnersButton);

    add(new H3("Match Narration"));
    narrationArea = new TextArea();
    narrationArea.setWidthFull();
    narrationArea.setValue(segment.getNarration() == null ? "" : segment.getNarration());
    narrationArea.setId("narration-area");
    add(narrationArea);

    Button saveButton = new Button("Save Narration", event -> saveNarration());
    saveButton.setId("save-narration-button");
    add(saveButton);
  }

  private void saveWinners() {
    List<Wrestler> winners = new ArrayList<>(winnersComboBox.getValue());
    segment.setWinners(winners);
    segmentService.updateSegment(segment);
    Notification.show("Winners saved!");

    // Campaign Integration
    securityUtils
        .getAuthenticatedUser()
        .ifPresent(
            user -> {
              campaignRepository
                  .findActiveByWrestler(user.getWrestler())
                  .ifPresent(
                      campaign -> {
                        if (campaign.getState().getCurrentMatch() != null
                            && campaign
                                .getState()
                                .getCurrentMatch()
                                .getId()
                                .equals(segment.getId())) {
                          boolean won = winners.contains(user.getWrestler());
                          campaignService.processMatchResult(campaign, won);

                          campaignRepository.save(campaign);

                          Notification n =
                              Notification.show("Campaign Progress Updated! Continuing Story...");
                          n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                          UI.getCurrent().navigate("campaign/narrative");
                        }
                      });
            });
  }

  private void saveNarration() {
    segment.setNarration(narrationArea.getValue());
    segmentService.updateSegment(segment);
    Notification.show("Narration saved!");
  }

  private VerticalLayout createOpponentSummary(Wrestler opponent) {
    VerticalLayout summary = new VerticalLayout();
    summary.add(new H3("Opponent: " + opponent.getName()));

    Optional<WrestlerStats> stats = wrestlerService.getWrestlerStats(opponent.getId());
    if (stats.isPresent()) {
      WrestlerStats wrestlerStats = stats.get();
      summary.add(new Paragraph("Wins: " + wrestlerStats.getWins()));
      summary.add(new Paragraph("Losses: " + wrestlerStats.getLosses()));
    }

    Wrestler opponentWithInjuries = wrestlerService.findByIdWithInjuries(opponent.getId()).get();

    summary.add(new Paragraph("Bumps: " + opponentWithInjuries.getBumps()));
    if (opponentWithInjuries.getInjuries().isEmpty()) {
      summary.add(new Paragraph("No current injuries."));
    } else {
      opponentWithInjuries
          .getInjuries()
          .forEach(
              injury -> {
                summary.add(new Paragraph("- " + injury.getDisplayString()));
              });
    }

    return summary;
  }

  private VerticalLayout createPlayerSummary(Wrestler player) {
    VerticalLayout summary = new VerticalLayout();
    summary.add(new H3("Your Summary: " + player.getName()));

    Optional<WrestlerStats> stats = wrestlerService.getWrestlerStats(player.getId());
    if (stats.isPresent()) {
      WrestlerStats wrestlerStats = stats.get();
      summary.add(new Paragraph("Wins: " + wrestlerStats.getWins()));
      summary.add(new Paragraph("Losses: " + wrestlerStats.getLosses()));
    }

    Wrestler playerWithInjuries = wrestlerService.findByIdWithInjuries(player.getId()).get();

    summary.add(new Paragraph("Bumps: ".concat(String.valueOf(playerWithInjuries.getBumps()))));
    if (playerWithInjuries.getInjuries().isEmpty()) {
      summary.add(new Paragraph("No current injuries."));
    } else {
      playerWithInjuries
          .getInjuries()
          .forEach(
              injury -> {
                summary.add(new Paragraph("- " + injury.getDisplayString()));
              });
    }

    return summary;
  }
}
