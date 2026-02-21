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
package com.github.javydreamercsw.management.ui.view.campaign;

import com.github.javydreamercsw.base.security.GeneralSecurityUtils;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.segment.promo.PromoHookDTO;
import com.github.javydreamercsw.management.dto.segment.promo.PromoOutcomeDTO;
import com.github.javydreamercsw.management.dto.segment.promo.SmartPromoResponseDTO;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.segment.SmartPromoService;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.github.javydreamercsw.management.ui.view.match.MatchView;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@Route(value = "campaign/promo", layout = MainLayout.class)
@PageTitle("Interactive Promo")
@PermitAll
@Slf4j
public class PromoView extends VerticalLayout implements HasUrlParameter<Long> {

  private final SmartPromoService smartPromoService;
  private final CampaignRepository campaignRepository;
  private final WrestlerRepository wrestlerRepository;
  private final SecurityUtils securityUtils;
  private final SegmentService segmentService;

  private Campaign currentCampaign;
  private Wrestler playerWrestler;
  private Wrestler opponent;
  private Long segmentId;

  private VerticalLayout narrativeContainer;
  private HorizontalLayout choicesContainer;
  private ProgressBar progressBar;

  @Autowired
  public PromoView(
      SmartPromoService smartPromoService,
      CampaignRepository campaignRepository,
      WrestlerRepository wrestlerRepository,
      SecurityUtils securityUtils,
      SegmentService segmentService) {
    this.smartPromoService = smartPromoService;
    this.campaignRepository = campaignRepository;
    this.wrestlerRepository = wrestlerRepository;
    this.securityUtils = securityUtils;
    this.segmentService = segmentService;

    setSpacing(true);
    setPadding(true);
    setAlignItems(FlexComponent.Alignment.CENTER);

    initUI();
  }

  @Override
  public void setParameter(BeforeEvent event, @OptionalParameter Long parameter) {
    if (parameter != null) {
      wrestlerRepository.findById(parameter).ifPresent(w -> opponent = w);
    }

    var queryParams = event.getLocation().getQueryParameters();
    if (queryParams.getParameters().containsKey("segment")) {
      this.segmentId = Long.valueOf(queryParams.getParameters().get("segment").getFirst());
    }

    if (queryParams.getParameters().containsKey("playerWrestler")) {
      Long pwId = Long.valueOf(queryParams.getParameters().get("playerWrestler").getFirst());
      wrestlerRepository.findById(pwId).ifPresent(w -> playerWrestler = w);
    }

    loadCampaign();
  }

  private void loadCampaign() {
    if (playerWrestler == null) {
      securityUtils
          .getAuthenticatedUser()
          .ifPresent(
              user -> {
                com.github.javydreamercsw.base.domain.account.Account account = user.getAccount();
                java.util.List<Wrestler> wrestlers = wrestlerRepository.findByAccount(account);
                playerWrestler =
                    wrestlers.stream()
                        .filter(w -> w.getId().equals(account.getActiveWrestlerId()))
                        .findFirst()
                        .orElse(wrestlers.isEmpty() ? null : wrestlers.get(0));
              });
    }

    if (playerWrestler != null) {
      campaignRepository.findActiveByWrestler(playerWrestler).ifPresent(c -> currentCampaign = c);
    }
  }

  private void initUI() {
    H2 title = new H2("Cutting a Promo");
    title.setId("promo-view-title");
    add(title);

    narrativeContainer = new VerticalLayout();
    narrativeContainer.setId("narrative-container");
    narrativeContainer.setWidthFull();
    narrativeContainer.setMaxWidth("800px");
    narrativeContainer.addClassNames(
        LumoUtility.Background.CONTRAST_5,
        LumoUtility.Padding.LARGE,
        LumoUtility.BorderRadius.LARGE,
        LumoUtility.BoxShadow.MEDIUM);

    choicesContainer = new HorizontalLayout();
    choicesContainer.setId("choices-container");
    choicesContainer.setSpacing(true);
    choicesContainer.setPadding(true);
    choicesContainer.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

    progressBar = new ProgressBar();
    progressBar.setId("promo-progress-bar");
    progressBar.setIndeterminate(true);
    progressBar.setVisible(false);
    progressBar.setWidth("300px");

    add(narrativeContainer, progressBar, choicesContainer);
  }

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    if (playerWrestler != null && narrativeContainer.getComponentCount() == 0) {
      startPromo();
    }
  }

  private void startPromo() {
    showLoading(true);
    narrativeContainer.removeAll();
    choicesContainer.removeAll();

    UI ui = UI.getCurrent();
    SecurityContext context = SecurityContextHolder.getContext();

    Runnable backgroundTask =
        () -> {
          log.info("Starting background task for promo initialization");
          GeneralSecurityUtils.runAsAdmin(
              () -> {
                try {
                  SmartPromoResponseDTO promoContext =
                      smartPromoService.generatePromoContext(playerWrestler, opponent);
                  log.info("Promo context generated successfully");
                  ui.access(
                      () -> {
                        try {
                          displayPromoContext(promoContext);
                          showLoading(false);
                          ui.push();
                          log.info("Promo UI updated with context");
                        } catch (Exception e) {
                          log.error("Failed to update UI with promo context", e);
                        }
                      });
                } catch (Exception e) {
                  log.error("Failed to start promo in background", e);
                  ui.access(
                      () -> {
                        Notification.show("Failed to connect to the Promo Director.")
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                        showLoading(false);
                        addBackButton();
                      });
                }
              });
        };

    new Thread(new DelegatingSecurityContextRunnable(backgroundTask, context)).start();
  }

  private void displayPromoContext(@NonNull SmartPromoResponseDTO context) {
    Paragraph p = new Paragraph(context.getOpener());
    p.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.LineHeight.MEDIUM);
    narrativeContainer.add(p);

    for (PromoHookDTO hook : context.getHooks()) {
      Button hookBtn = new Button(hook.getLabel());
      hookBtn.setId("promo-hook-" + hook.getLabel().replace(" ", "-").toLowerCase());
      hookBtn.setTooltipText(hook.getHook() + ": " + hook.getText());
      hookBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      hookBtn.addClickListener(e -> handleHookChoice(hook));
      choicesContainer.add(hookBtn);
    }
  }

  private void handleHookChoice(@NonNull PromoHookDTO hook) {
    log.info("Hook chosen: {}", hook.getLabel());
    showLoading(true);
    choicesContainer.removeAll();

    UI ui = UI.getCurrent();
    SecurityContext context = SecurityContextHolder.getContext();

    Runnable backgroundTask =
        () -> {
          log.info("Starting background task for hook processing: {}", hook.getLabel());
          GeneralSecurityUtils.runAsAdmin(
              () -> {
                try {
                  PromoOutcomeDTO outcome =
                      smartPromoService.processPromoHook(
                          playerWrestler, opponent, hook, currentCampaign);
                  log.info("Promo hook processed successfully. Success: {}", outcome.isSuccess());
                  ui.access(
                      () -> {
                        try {
                          displayOutcome(hook, outcome);
                          log.info("Promo UI updated with outcome");
                        } catch (Exception e) {
                          log.error("Failed to update UI with promo outcome", e);
                          Notification.show("Error displaying promo outcome.")
                              .addThemeVariants(NotificationVariant.LUMO_ERROR);
                        } finally {
                          showLoading(false);
                          ui.push();
                        }
                      });
                } catch (Exception e) {
                  log.error("Failed to process promo hook in background", e);
                  ui.access(
                      () -> {
                        Notification.show("Failed to resolve the promo: " + e.getMessage())
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                        showLoading(false);
                        addBackButton();
                        ui.push();
                      });
                }
              });
        };

    new Thread(new DelegatingSecurityContextRunnable(backgroundTask, context)).start();
  }

  private void displayOutcome(@NonNull PromoHookDTO hook, @NonNull PromoOutcomeDTO outcome) {
    log.info("Displaying outcome. Success: {}, SegmentID: {}", outcome.isSuccess(), segmentId);

    // Create a result layout to hold all outcome components atomically
    VerticalLayout resultLayout = new VerticalLayout();
    resultLayout.setPadding(false);
    resultLayout.setSpacing(true);

    Span chosenText = new Span("\"" + (hook.getText() != null ? hook.getText() : "") + "\"");
    chosenText.getStyle().set("font-style", "italic");
    chosenText.addClassNames(LumoUtility.TextColor.PRIMARY);
    resultLayout.add(chosenText);

    if (outcome.getRetort() != null && !outcome.getRetort().isBlank()) {
      Paragraph retort = new Paragraph(outcome.getRetort());
      retort.addClassNames(LumoUtility.FontWeight.BOLD);
      resultLayout.add(new Span("Opponent's retort:"), retort);
    }

    if (outcome.getCrowdReaction() != null && !outcome.getCrowdReaction().isBlank()) {
      Paragraph reaction = new Paragraph(outcome.getCrowdReaction());
      reaction.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
      resultLayout.add(new Span("Crowd Reaction:"), reaction);
    }

    String status = outcome.isSuccess() ? "SUCCESSFUL" : "FAILED";
    Span outcomeSpan = new Span("Promo " + status);
    outcomeSpan.setId("promo-outcome-status");
    outcomeSpan.addClassNames(
        LumoUtility.FontWeight.BOLD,
        outcome.isSuccess() ? LumoUtility.TextColor.SUCCESS : LumoUtility.TextColor.ERROR);
    resultLayout.add(outcomeSpan);

    // Add the complete result layout to the narrative container in one operation
    narrativeContainer.add(resultLayout);

    if (segmentId != null) {
      log.info("Updating segment {} with final narration", segmentId);
      // Save to segment narration
      segmentService
          .findById(segmentId)
          .ifPresentOrElse(
              s -> {
                s.setNarration(outcome.getFinalNarration());
                segmentService.updateSegment(s);
                log.info("Segment {} updated successfully", segmentId);
              },
              () -> log.warn("Segment {} not found for update", segmentId));
    }

    Button finishBtn =
        new Button(
            "Finish Promo",
            e -> {
              if (segmentId != null) {
                UI.getCurrent()
                    .navigate(
                        MatchView.class,
                        new com.vaadin.flow.router.RouteParameters(
                            "matchId", segmentId.toString()));
              } else {
                UI.getCurrent().navigate("campaign/actions");
              }
            });
    finishBtn.setId("finish-promo-button");
    finishBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    choicesContainer.add(finishBtn);
    log.info("Outcome display complete");
  }

  private void showLoading(boolean loading) {
    progressBar.setVisible(loading);
    choicesContainer.setVisible(!loading);
  }

  private void addBackButton() {
    Button backBtn =
        new Button("Back to Actions", e -> UI.getCurrent().navigate("campaign/actions"));
    choicesContainer.add(backBtn);
  }
}
