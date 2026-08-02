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

import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.security.CustomUserDetails;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.component.ImageUploadComponent;
import com.github.javydreamercsw.management.domain.challenge.AccountChallengeCompletion;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.dto.challenge.ChallengeDTO;
import com.github.javydreamercsw.management.service.challenge.ChallengeCompletionService;
import com.github.javydreamercsw.management.service.challenge.ChallengeService;
import com.github.javydreamercsw.management.service.challenge.ChallengeUpdateService;
import com.github.javydreamercsw.management.service.expansion.ExpansionService;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.github.javydreamercsw.management.ui.view.show.MatchInfoDialog;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "challenges", layout = MainLayout.class)
@PageTitle("Weekly Challenges")
@Menu(order = 15, icon = "vaadin:trophy", title = "Weekly Challenges")
@PermitAll
public class ChallengeListView extends VerticalLayout {

  private static final DateTimeFormatter DATE_FMT =
      DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a");

  private final ChallengeService challengeService;
  private final ChallengeUpdateService updateService;
  private final ExpansionService expansionService;
  private final SegmentRuleRepository segmentRuleRepository;
  private final ChallengeCompletionService completionService;
  private final ImageStorageService imageStorageService;
  private final SecurityUtils securityUtils;

  private FlexLayout cardsLayout;
  private Checkbox officialOnlyFilter;
  private ComboBox<String> seasonFilter;

  @Autowired
  public ChallengeListView(
      final ChallengeService challengeService,
      final ChallengeUpdateService updateService,
      final ExpansionService expansionService,
      final SegmentRuleRepository segmentRuleRepository,
      final ChallengeCompletionService completionService,
      final ImageStorageService imageStorageService,
      final SecurityUtils securityUtils) {
    this.challengeService = challengeService;
    this.updateService = updateService;
    this.expansionService = expansionService;
    this.segmentRuleRepository = segmentRuleRepository;
    this.completionService = completionService;
    this.imageStorageService = imageStorageService;
    this.securityUtils = securityUtils;

    setPadding(true);
    setSpacing(true);

    add(new H2("Weekly Challenges"));

    HorizontalLayout filters = new HorizontalLayout();
    filters.setAlignItems(Alignment.CENTER);
    filters.setFlexGrow(0, filters);

    officialOnlyFilter = new Checkbox("Official only", false);
    officialOnlyFilter.addValueChangeListener(e -> refreshCards());

    List<String> seasons =
        challengeService.getAllChallenges().stream()
            .map(ChallengeDTO::getSeason)
            .filter(s -> s != null && !s.isBlank())
            .distinct()
            .sorted()
            .toList();

    seasonFilter = new ComboBox<>("Season");
    seasonFilter.setItems(seasons);
    seasonFilter.setPlaceholder("All seasons");
    seasonFilter.setClearButtonVisible(true);
    seasonFilter.addValueChangeListener(e -> refreshCards());

    filters.add(officialOnlyFilter, seasonFilter);

    if (securityUtils.isAdmin()) {
      Button updateBtn = new Button("Check for Updates", new Icon(VaadinIcon.REFRESH));
      updateBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_CONTRAST);
      updateBtn.addClickListener(e -> runUpdateCheck(updateBtn));
      filters.add(updateBtn);
    }

    add(filters);

    cardsLayout = new FlexLayout();
    cardsLayout.addClassNames(Display.FLEX, FlexWrap.WRAP, Gap.MEDIUM);
    add(cardsLayout);

    refreshCards();
  }

  private void runUpdateCheck(final Button btn) {
    btn.setEnabled(false);
    UI ui = UI.getCurrent();
    CompletableFuture.supplyAsync(updateService::checkAndApply)
        .thenAccept(
            result ->
                ui.access(
                    () -> {
                      Notification n = Notification.show(result.message());
                      n.addThemeVariants(
                          result.downloaded() > 0
                              ? NotificationVariant.LUMO_SUCCESS
                              : NotificationVariant.LUMO_PRIMARY);
                      btn.setEnabled(true);
                      if (result.downloaded() > 0) {
                        refreshCards();
                      }
                    }));
  }

  private Optional<Account> currentAccount() {
    return securityUtils.getAuthenticatedUser().map(CustomUserDetails::getAccount);
  }

  private void refreshCards() {
    List<String> seasons =
        challengeService.getAllChallenges().stream()
            .map(ChallengeDTO::getSeason)
            .filter(s -> s != null && !s.isBlank())
            .distinct()
            .sorted()
            .toList();
    seasonFilter.setItems(seasons);

    cardsLayout.removeAll();

    List<ChallengeDTO> all = challengeService.getActiveChallenges();
    Set<String> enabledCodes = Set.copyOf(expansionService.getEnabledExpansionCodes());
    String selectedSeason = seasonFilter.getValue();

    all.stream()
        .filter(c -> !officialOnlyFilter.getValue() || !"CUSTOM".equals(c.getExpansionCode()))
        .filter(
            c ->
                selectedSeason == null
                    || selectedSeason.isBlank()
                    || selectedSeason.equals(c.getSeason()))
        .forEach(c -> cardsLayout.add(buildCard(c, enabledCodes)));
  }

  private Div buildCard(final ChallengeDTO challenge, final Set<String> enabledCodes) {
    boolean isLocked =
        !challenge.getRequiredExpansions().isEmpty()
            && !enabledCodes.containsAll(challenge.getRequiredExpansions());

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

    if (isLocked) {
      Span lock = new Span("🔒 Locked");
      lock.getElement().getThemeList().add("badge contrast");
      badges.add(lock);
    }

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

    if (!isLocked && !challenge.getRequiredExpansions().isEmpty()) {
      Span expBadge = new Span("Requires: " + String.join(", ", challenge.getRequiredExpansions()));
      expBadge.getElement().getThemeList().add("badge success");
      badges.add(expBadge);
    }

    if (!isLocked) {
      currentAccount()
          .filter(account -> completionService.isCompleted(account, challenge.getId()))
          .ifPresent(
              account -> {
                Span done = new Span("Completed ✓");
                done.getElement().getThemeList().add("badge success");
                badges.add(done);
              });
    }

    card.add(badges);

    H3 title = new H3(challenge.getTitle());
    title.addClassNames(Margin.Top.NONE, Margin.Bottom.XSMALL, FontSize.MEDIUM);
    if (isLocked) {
      title.addClassName(TextColor.SECONDARY);
    }
    card.add(title);

    if (challenge.getProductLine() != null) {
      Span product = new Span(challenge.getProductLine());
      product.addClassNames(FontSize.XSMALL, TextColor.SECONDARY, Margin.Bottom.SMALL);
      card.add(product);
    }

    Paragraph objective = new Paragraph(challenge.getObjective());
    objective.addClassNames(FontSize.SMALL, Margin.Bottom.MEDIUM);
    if (isLocked) {
      objective.addClassName(TextColor.SECONDARY);
    }
    card.add(objective);

    if (isLocked) {
      List<String> missing =
          challenge.getRequiredExpansions().stream()
              .filter(code -> !enabledCodes.contains(code))
              .toList();
      String expansionList =
          String.join(", ", missing) + " expansion" + (missing.size() > 1 ? "s" : "");

      Paragraph lockMsg = new Paragraph("Requires " + expansionList + ".");
      lockMsg.addClassNames(FontSize.XSMALL, TextColor.SECONDARY, Margin.Bottom.SMALL);
      card.add(lockMsg);

      if (securityUtils.isAdmin()) {
        HorizontalLayout enableButtons = new HorizontalLayout();
        enableButtons.setSpacing(true);
        enableButtons.addClassName(Margin.Bottom.SMALL);
        for (String code : missing) {
          Button enableBtn = new Button("Enable " + code, new Icon(VaadinIcon.UNLOCK));
          enableBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
          enableBtn.addClickListener(
              e -> {
                expansionService.setExpansionEnabled(code, true);
                refreshCards();
              });
          enableButtons.add(enableBtn);
        }
        card.add(enableButtons);
      } else {
        String tooltip =
            "Requires the "
                + expansionList
                + ". Contact your administrator to enable it for this universe.";
        Button lockedBtn = new Button("🔒 Locked");
        lockedBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        lockedBtn.setEnabled(false);
        lockedBtn.setTooltipText(tooltip);
        card.add(lockedBtn);
      }
    } else {
      Button viewBtn = new Button("View Challenge", e -> openDetailDialog(challenge));
      viewBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
      card.add(viewBtn);
    }

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
                Button rulesBtn =
                    new Button(
                        challenge.getMatchType(),
                        new Icon(VaadinIcon.INFO_CIRCLE_O),
                        e -> new MatchInfoDialog(null, List.of(rule)).open());
                rulesBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
                content.add(rulesBtn);
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

    currentAccount()
        .ifPresent(account -> addCompletionSection(content, dialog, challenge, account));

    dialog.add(content);
    dialog.open();
  }

  private void addCompletionSection(
      final VerticalLayout content,
      final Dialog dialog,
      final ChallengeDTO challenge,
      final Account account) {
    Div divider = new Div();
    divider.getStyle().set("border-top", "1px solid var(--lumo-contrast-10pct)");
    divider.addClassNames(Margin.Vertical.MEDIUM);
    content.add(divider);

    H4 header = new H4("Your Completion");
    header.addClassNames(Margin.Top.NONE, Margin.Bottom.SMALL, FontWeight.BOLD);
    content.add(header);

    Optional<AccountChallengeCompletion> existing =
        completionService.find(account, challenge.getId());

    AccountChallengeCompletion existing0 = existing.orElse(null);
    boolean alreadyCompleted =
        existing0 != null
            && existing0.getStatus()
                == com.github.javydreamercsw.management.domain.challenge.ChallengeCompletionStatus
                    .COMPLETED;

    if (alreadyCompleted) {
      Span completedOn =
          new Span(
              "Completed on: "
                  + (existing0.getCompletedAt() != null
                      ? existing0.getCompletedAt().format(DATE_FMT)
                      : "—"));
      completedOn.addClassNames(FontSize.SMALL, FontWeight.BOLD, TextColor.SUCCESS);
      content.add(completedOn);
    }

    TextArea notesField = new TextArea("Notes");
    notesField.setPlaceholder("How did it go? Any tips for other players?");
    notesField.setWidthFull();
    notesField.setMaxHeight("120px");
    if (existing0 != null && existing0.getPlayerNotes() != null) {
      notesField.setValue(existing0.getPlayerNotes());
    }
    content.add(notesField);

    String[] proofUrl = {existing0 != null ? existing0.getProofImageUrl() : null};

    if (proofUrl[0] != null && !proofUrl[0].isBlank()) {
      Span proofLabel = new Span("Current proof photo:");
      proofLabel.addClassNames(FontSize.SMALL, TextColor.SECONDARY);
      content.add(proofLabel);

      Image currentProof = new Image(proofUrl[0], "Completion proof");
      currentProof.setMaxWidth("100%");
      currentProof.addClassName(Margin.Top.XSMALL);
      content.add(currentProof);

      Button removePhoto = new Button("Remove photo", new Icon(VaadinIcon.TRASH));
      removePhoto.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
      removePhoto.addClassName(Margin.Bottom.SMALL);
      removePhoto.addClickListener(
          e -> {
            proofUrl[0] = null;
            content.remove(proofLabel);
            content.remove(currentProof);
            content.remove(removePhoto);
          });
      content.add(removePhoto);
    }

    Span uploadLabel =
        new Span(alreadyCompleted ? "Replace proof photo (optional):" : "Proof photo (optional):");
    uploadLabel.addClassNames(FontSize.SMALL, TextColor.SECONDARY);
    content.add(uploadLabel);

    ImageUploadComponent uploadComponent =
        new ImageUploadComponent(imageStorageService, url -> proofUrl[0] = url);
    content.add(uploadComponent);

    Button actionBtn =
        alreadyCompleted
            ? new Button("Save Changes", new Icon(VaadinIcon.CHECK))
            : new Button("Mark as Complete", new Icon(VaadinIcon.CHECK_CIRCLE));
    actionBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
    actionBtn.addClassName(Margin.Top.SMALL);
    actionBtn.addClickListener(
        e -> {
          completionService.markComplete(
              account, challenge.getId(), notesField.getValue(), proofUrl[0]);
          String msg = alreadyCompleted ? "Changes saved!" : "Challenge marked as complete!";
          Notification.show(msg).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
          dialog.close();
          refreshCards();
        });
    content.add(actionBtn);
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
    if (challenge.getDifficulty() == null) {
      return "badge contrast";
    }
    return switch (challenge.getDifficulty()) {
      case ENTRY, EASY -> "badge success";
      case MEDIUM -> "badge";
      case HARD, LEGENDARY -> "badge error";
    };
  }
}
