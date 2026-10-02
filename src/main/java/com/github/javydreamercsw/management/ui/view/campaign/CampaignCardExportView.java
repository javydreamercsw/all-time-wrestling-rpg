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

import static com.github.javydreamercsw.base.domain.account.RoleName.ADMIN_ROLE;

import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.show.segment.rule.BumpAddition;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRulePlayGuide;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import com.github.javydreamercsw.management.dto.campaign.StaticEncounterDTO;
import com.github.javydreamercsw.management.service.campaign.CampaignChapterService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowFacade;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Admin-only view that renders game content as printable poker-size cards (63.5×88.9mm) in the
 * browser. Two categories: campaign chapters (encounter cards) and custom content — everything
 * tagged with the CUSTOM expansion (segment types, segment rules, NPCs, titles). Use the Print
 * button or Ctrl+P / Cmd+P — the Vaadin shell is suppressed automatically via @media print.
 */
@Route(value = "campaign-card-export", layout = MainLayout.class)
@RolesAllowed(ADMIN_ROLE)
@Slf4j
public class CampaignCardExportView extends VerticalLayout implements HasDynamicTitle {

  private static final String STYLE_ID = "campaign-card-export-styles";
  private static final String DEFAULT_TITLE = "Card Export";
  private static final String CUSTOM_EXPANSION = "CUSTOM";

  private final CampaignChapterService chapterService;
  private final SegmentTypeService segmentTypeService;
  private final SegmentRuleService segmentRuleService;
  private final NpcService npcService;
  private final TitleService titleService;
  private String currentTitle = DEFAULT_TITLE;
  private final Div printTitle = new Div();
  private final ComboBox<ExportCategory> categorySelect = new ComboBox<>("Category");
  private final Div detailSlot = new Div();
  private final ComboBox<CampaignChapterDTO> chapterSelect = new ComboBox<>("Campaign Chapter");
  private final ComboBox<CustomKind> customKindSelect = new ComboBox<>("Custom Content Kind");
  private final Div cardGrid = new Div();
  private final Button printBtn =
      new Button("Print Cards", VaadinIcon.PRINT.create(), e -> print());

  /** Top-level export categories offered by the toolbar. */
  enum ExportCategory {
    CAMPAIGN_CHAPTERS("Campaign Chapters"),
    CUSTOM_CONTENT("Custom Content");

    private final String label;

    ExportCategory(final String label) {
      this.label = label;
    }

    String label() {
      return label;
    }
  }

  /** The second-level selector for the Custom Content category. */
  enum CustomKind {
    SEGMENT_TYPES("Segment Types"),
    SEGMENT_RULES("Segment Rules"),
    NPCS("NPCs"),
    TITLES("Titles");

    private final String label;

    CustomKind(final String label) {
      this.label = label;
    }

    String label() {
      return label;
    }
  }

  public CampaignCardExportView(
      final CampaignChapterService chapterService,
      final ShowFacade showFacade,
      final TitleService titleService) {
    this.chapterService = chapterService;
    this.segmentTypeService = showFacade.getSegmentTypeService();
    this.segmentRuleService = showFacade.getSegmentRuleService();
    this.npcService = showFacade.getNpcService();
    this.titleService = titleService;
    setPadding(true);
    setSpacing(true);
    buildUi();
  }

  @Override
  public String getPageTitle() {
    return currentTitle;
  }

  @Override
  protected void onAttach(AttachEvent event) {
    super.onAttach(event);
    UI.getCurrent()
        .getPage()
        .executeJs(
            "if (!document.getElementById($0)) {"
                + "  const s = document.createElement('style');"
                + "  s.id = $0;"
                + "  s.textContent = $1;"
                + "  document.head.appendChild(s);"
                + "}",
            STYLE_ID,
            cardCss());
  }

  private void print() {
    UI.getCurrent().getPage().executeJs("window.print()");
  }

  private void buildUi() {
    add(new H2("Card Export"));
    add(
        new Paragraph(
            "Cards are rendered at poker size (63.5 × 88.9 mm). "
                + "Use the Print button or Ctrl+P / Cmd+P to send to your printer."));

    List<CampaignChapterDTO> chapters =
        chapterService.getAllChapters().stream()
            .filter(CampaignChapterDTO::hasStaticEncounters)
            .sorted(Comparator.comparing(CampaignChapterDTO::getTitle))
            .toList();

    chapterSelect.setItems(chapters);
    chapterSelect.setItemLabelGenerator(
        c -> c.getTitle() + " (" + c.getStaticEncounters().size() + " cards)");
    chapterSelect.setWidth("420px");
    chapterSelect.setPlaceholder("Choose a chapter…");
    chapterSelect.addValueChangeListener(e -> renderCards(e.getValue()));

    customKindSelect.setItems(CustomKind.values());
    customKindSelect.setItemLabelGenerator(CustomKind::label);
    customKindSelect.setWidth("280px");
    customKindSelect.setPlaceholder("Choose what to print…");
    customKindSelect.addValueChangeListener(e -> renderCustomCards(e.getValue()));

    categorySelect.setItems(ExportCategory.values());
    categorySelect.setItemLabelGenerator(ExportCategory::label);
    categorySelect.setWidth("230px");
    categorySelect.setPlaceholder("Choose a category…");
    categorySelect.addValueChangeListener(e -> swapDetailSelector(e.getValue()));

    printBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    printBtn.setEnabled(false);

    detailSlot.setWidth("440px");
    HorizontalLayout toolbar = new HorizontalLayout(categorySelect, detailSlot, printBtn);
    toolbar.setAlignItems(Alignment.END);

    cardGrid.setId("campaign-card-print-area");
    cardGrid.addClassName("campaign-card-grid");

    printTitle.setId("campaign-card-print-title");

    add(toolbar, cardGrid);

    // Default category: campaign chapters. Default to extreme_campaign, fallback to first
    // available.
    chapters.stream()
        .filter(c -> "extreme_campaign".equals(c.getId()))
        .findFirst()
        .ifPresentOrElse(
            chapterSelect::setValue,
            () -> {
              if (!chapters.isEmpty()) {
                chapterSelect.setValue(chapters.get(0));
              }
            });
    categorySelect.setValue(ExportCategory.CAMPAIGN_CHAPTERS);
  }

  private void swapDetailSelector(ExportCategory category) {
    detailSlot.removeAll();
    if (category == ExportCategory.CUSTOM_CONTENT) {
      detailSlot.add(customKindSelect);
      renderCustomCards(customKindSelect.getValue());
    } else {
      detailSlot.add(chapterSelect);
      renderCards(chapterSelect.getValue());
    }
  }

  // ── Card rendering ────────────────────────────────────────────────────────

  private void renderCards(CampaignChapterDTO chapter) {
    cardGrid.removeAll();
    printBtn.setEnabled(chapter != null);
    if (chapter == null) {
      currentTitle = DEFAULT_TITLE;
      UI.getCurrent().getPage().setTitle(currentTitle);
      return;
    }
    currentTitle = chapter.getTitle() + " — Card Export";
    UI.getCurrent().getPage().setTitle(currentTitle);
    printTitle.setText(chapter.getTitle());
    cardGrid.add(printTitle);
    cardGrid.add(buildChapterCard(chapter));
    List<StaticEncounterDTO> encounters = chapter.getStaticEncounters();
    for (int i = 0; i < encounters.size(); i++) {
      cardGrid.add(buildEncounterCard(encounters.get(i), i + 1, encounters.size()));
    }
  }

  // ── Custom content rendering (expansionCode == CUSTOM) ────────────────────

  private void renderCustomCards(CustomKind kind) {
    cardGrid.removeAll();
    if (kind == null) {
      printBtn.setEnabled(false);
      currentTitle = DEFAULT_TITLE;
      UI.getCurrent().getPage().setTitle(currentTitle);
      return;
    }
    currentTitle = kind.label() + " — Card Export";
    UI.getCurrent().getPage().setTitle(currentTitle);
    printTitle.setText(kind.label());
    cardGrid.add(printTitle);
    switch (kind) {
      case SEGMENT_TYPES -> {
        List<SegmentType> types = customSegmentTypes();
        if (types.isEmpty()) {
          printBtn.setEnabled(false);
          cardGrid.add(emptyState("No custom segment types defined yet."));
          return;
        }
        types.forEach(t -> cardGrid.add(buildSegmentTypeCard(t)));
      }
      case SEGMENT_RULES -> {
        List<SegmentRule> rules = customSegmentRules();
        if (rules.isEmpty()) {
          printBtn.setEnabled(false);
          cardGrid.add(emptyState("No custom segment rules defined yet."));
          return;
        }
        rules.forEach(r -> cardGrid.add(buildSegmentRuleCard(r)));
      }
      case NPCS -> {
        List<Npc> npcs = customNpcs();
        if (npcs.isEmpty()) {
          printBtn.setEnabled(false);
          cardGrid.add(emptyState("No custom NPCs defined yet."));
          return;
        }
        npcs.forEach(n -> cardGrid.add(buildNpcCard(n)));
      }
      case TITLES -> {
        List<Title> titles = customTitles();
        if (titles.isEmpty()) {
          printBtn.setEnabled(false);
          cardGrid.add(emptyState("No custom titles defined yet."));
          return;
        }
        titles.forEach(t -> cardGrid.add(buildTitleCard(t)));
      }
    }
    printBtn.setEnabled(true);
  }

  private Component emptyState(String message) {
    Paragraph p = new Paragraph(message);
    p.addClassName("empty-state");
    return p;
  }

  private List<SegmentType> customSegmentTypes() {
    return segmentTypeService.findAllByExpansionCode(CUSTOM_EXPANSION);
  }

  private List<SegmentRule> customSegmentRules() {
    return segmentRuleService.findAllByExpansionCode(CUSTOM_EXPANSION);
  }

  private List<Npc> customNpcs() {
    // Unfiltered on purpose: custom content is the user's own creation and should be
    // printable regardless of expansion toggles or active flags.
    return npcService.findAllUnfiltered().stream()
        .filter(n -> CUSTOM_EXPANSION.equals(n.getExpansionCode()))
        .toList();
  }

  private List<Title> customTitles() {
    return titleService.findAllByExpansionCode(CUSTOM_EXPANSION);
  }

  static Div buildSegmentTypeCard(SegmentType type) {
    Div header = cardHeader("segment-type-header", "SEGMENT TYPE", type.getName());
    Div body = new Div();
    body.addClassName("card-body");
    addNarrative(body, type.getDescription());
    addGuideBlock(body, type.getGuide());
    addExpansionFooter(body, type.getExpansionCode());
    Div card = new Div();
    card.addClassName("campaign-card");
    card.add(header, body);
    return card;
  }

  static Div buildSegmentRuleCard(SegmentRule rule) {
    Div header = cardHeader("segment-rule-header", "SEGMENT RULE", rule.getName());
    List<String> badges = new ArrayList<>();
    if (Boolean.TRUE.equals(rule.getNoDq())) {
      badges.add("NO DQ");
    }
    if (Boolean.TRUE.equals(rule.getRequiresHighHeat())) {
      badges.add("HIGH HEAT");
    }
    if (!rule.isAllowsRefereeStopage()) {
      badges.add("NO REF STOPPAGE");
    }
    if (rule.getBumpAddition() != null && rule.getBumpAddition() != BumpAddition.NONE) {
      badges.add("BUMPS: " + rule.getBumpAddition().name());
    }
    if (!badges.isEmpty()) {
      Div badgeRow = new Div();
      badgeRow.addClassName("rule-badges");
      badges.forEach(
          b -> {
            Span badge = new Span(b);
            badge.addClassName("rule-badge");
            badgeRow.add(badge);
          });
      header.add(badgeRow);
    }

    Div body = new Div();
    body.addClassName("card-body");
    addNarrative(body, rule.getDescription());
    addGuideBlock(body, rule.getGuide());
    addExpansionFooter(body, rule.getExpansionCode());
    Div card = new Div();
    card.addClassName("campaign-card");
    card.add(header, body);
    return card;
  }

  static Div buildNpcCard(Npc npc) {
    Div header = cardHeader("npc-header", "NPC · " + npc.getNpcType(), npc.getName());
    Span align = new Span(npc.getAlignment() + " · " + npc.getGender());
    align.addClassName("npc-meta");
    header.add(align);

    Div body = new Div();
    body.addClassName("card-body");
    if (npc.getImageUrl() != null && !npc.getImageUrl().isBlank()) {
      Image img = new Image(npc.getImageUrl(), npc.getName());
      img.addClassName("npc-image");
      body.add(img);
    }
    addNarrative(body, npc.getDescription());
    addExpansionFooter(body, npc.getExpansionCode());
    Div card = new Div();
    card.addClassName("campaign-card");
    card.add(header, body);
    return card;
  }

  static Div buildTitleCard(Title title) {
    Div header = cardHeader("title-header", "CHAMPIONSHIP", title.getName());
    if (title.getTier() != null) {
      Span tier = new Span(title.getTier().name());
      tier.addClassName("wrestler-tag");
      header.add(tier);
    }

    Div body = new Div();
    body.addClassName("card-body");
    addNarrative(body, title.getDescription());
    addScriptLines(body, title.getEffectScript());
    addExpansionFooter(body, title.getExpansionCode());
    Div card = new Div();
    card.addClassName("campaign-card");
    card.add(header, body);
    return card;
  }

  private static Div cardHeader(String headerClass, String typeLabel, String name) {
    Div header = new Div();
    header.addClassName("card-header");
    header.getClassNames().add(headerClass);
    Span label = new Span(typeLabel);
    label.addClassName("card-type");
    Div title = new Div(name != null ? name : "");
    title.addClassName("card-title");
    header.add(label, title);
    return header;
  }

  private static void addNarrative(Div body, String text) {
    if (text != null && !text.isBlank()) {
      Paragraph p = new Paragraph(text);
      p.addClassName("narrative");
      body.add(p);
    }
  }

  private static void addGuideBlock(Div body, SegmentRulePlayGuide guide) {
    if (guide == null) {
      return;
    }
    Div guideBlock = new Div();
    guideBlock.addClassName("guide-block");
    addGuideVariant(guideBlock, "SOLO", guide.solo() != null ? guide.solo().overview() : null);
    addGuideVariant(
        guideBlock,
        "MULTIPLAYER",
        guide.multiplayer() != null ? guide.multiplayer().overview() : null);
    if (guideBlock.getElement().getChildCount() > 0) {
      body.add(guideBlock);
    }
  }

  private static void addGuideVariant(Div guideBlock, String label, String overview) {
    if (overview == null || overview.isBlank()) {
      return;
    }
    Span guideLabel = new Span(label);
    guideLabel.addClassName("guide-label");
    Span guideText = new Span(overview);
    guideText.addClassName("guide-text");
    guideBlock.add(guideLabel, guideText);
  }

  private static void addScriptLines(Div body, String effectScript) {
    if (effectScript == null || effectScript.isBlank()) {
      return;
    }
    Div script = new Div();
    script.addClassName("effect-script");
    for (String line : effectScript.split(";")) {
      if (!line.isBlank()) {
        script.add(new Span(line.trim()));
      }
    }
    body.add(script);
  }

  private static void addExpansionFooter(Div body, String expansionCode) {
    Span exp = new Span(expansionCode != null ? expansionCode : "");
    exp.addClassName("expansion-tag");
    Div footer = new Div();
    footer.addClassName("card-footer");
    footer.add(exp);
    body.add(footer);
  }

  private Div buildChapterCard(CampaignChapterDTO chapter) {
    Div card = new Div();
    card.addClassName("campaign-card");

    Div header = new Div();
    header.addClassNames("card-header", "chapter-header");
    Span typeLabel = new Span("CAMPAIGN");
    typeLabel.addClassName("card-type");
    Div title = new Div(chapter.getTitle());
    title.addClassName("card-title");
    header.add(typeLabel, title);
    if (chapter.getDifficulty() != null) {
      Span diff = new Span(chapter.getDifficulty().name());
      diff.addClassName("difficulty-badge");
      header.add(diff);
    }

    Div body = new Div();
    body.addClassName("card-body");
    if (chapter.getIntroText() != null && !chapter.getIntroText().isBlank()) {
      Paragraph intro = new Paragraph(chapter.getIntroText());
      intro.addClassName("narrative");
      body.add(intro);
    }
    if (!chapter.getAllowedWrestlerNames().isEmpty()) {
      Div wr = new Div("Wrestlers: " + String.join(", ", chapter.getAllowedWrestlerNames()));
      wr.addClassName("info-row");
      body.add(wr);
    }
    if (!chapter.getRequiredExpansions().isEmpty()) {
      Span exp = new Span(String.join(" · ", chapter.getRequiredExpansions()));
      exp.addClassName("expansion-tag");
      body.add(exp);
    }
    Div count = new Div(chapter.getStaticEncounters().size() + " Encounter Cards");
    count.addClassName("card-count");
    body.add(count);

    card.add(header, body);
    return card;
  }

  private Div buildEncounterCard(StaticEncounterDTO enc, int index, int total) {
    Div card = new Div();
    card.addClassName("campaign-card");

    Div header = new Div();
    header.addClassName("card-header");
    Span num = new Span(index + " / " + total);
    num.addClassName("card-type");
    Div title = new Div(enc.getTitle());
    title.addClassName("card-title");
    header.add(num, title);
    if (enc.getRequiredWrestlerName() != null) {
      Span wrestler = new Span(enc.getRequiredWrestlerName());
      wrestler.addClassName("wrestler-tag");
      header.add(wrestler);
    }

    Div body = new Div();
    body.addClassName("card-body");
    if (enc.getNarrativeText() != null && !enc.getNarrativeText().isBlank()) {
      Paragraph narrative = new Paragraph(enc.getNarrativeText());
      narrative.addClassName("narrative");
      body.add(narrative);
    }
    if (enc.getChoices() != null && !enc.getChoices().isEmpty()) {
      Div choices = new Div();
      choices.addClassName("choices");
      enc.getChoices().forEach(c -> choices.add(buildChoice(c)));
      body.add(choices);
    }

    card.add(header, body);
    return card;
  }

  private Div buildChoice(StaticEncounterDTO.StaticChoiceDTO choice) {
    Div row = new Div();
    row.addClassName("choice");

    Span label = new Span("▶ " + choice.getLabel());
    label.addClassName("choice-label");
    row.add(label);

    if (choice.getText() != null && !choice.getText().isBlank()) {
      Span text = new Span(choice.getText());
      text.addClassName("choice-text");
      row.add(text);
    }

    StringBuilder meta = new StringBuilder();
    if (choice.getVpReward() != 0) {
      meta.append(choice.getVpReward() > 0 ? "+" : "").append(choice.getVpReward()).append(" VP  ");
    }
    if (choice.getAlignmentShift() != 0) {
      meta.append(choice.getAlignmentShift() > 0 ? "⬆" : "⬇").append(" Alignment  ");
    }
    if (choice.getNextPhase() != null) {
      meta.append("→ ").append(choice.getNextPhase().name());
      if (choice.getSegmentRules() != null && !choice.getSegmentRules().isEmpty()) {
        meta.append(" (").append(String.join(", ", choice.getSegmentRules())).append(")");
      }
    }
    if (!meta.isEmpty()) {
      Span metaSpan = new Span(meta.toString().trim());
      metaSpan.addClassName("choice-meta");
      row.add(metaSpan);
    }
    return row;
  }

  // ── CSS injected into the page head on attach ─────────────────────────────

  static String cardCss() {
    return """
    .campaign-card-grid {
      display: flex;
      flex-wrap: wrap;
      gap: 5mm;
      padding: 8px;
    }
    .campaign-card {
      width: 63.5mm;
      min-height: 88.9mm;
      border: 0.5mm solid #777;
      border-radius: 3mm;
      overflow: hidden;
      display: flex;
      flex-direction: column;
      background: var(--lumo-base-color, #fff);
      color: var(--lumo-body-text-color, #111);
      font-family: Georgia, 'Times New Roman', serif;
      break-inside: avoid;
      page-break-inside: avoid;
    }
    .card-header {
      background: #1a1a2e;
      color: #f0f0f0;
      padding: 2mm 3mm;
      flex-shrink: 0;
      display: flex;
      flex-direction: column;
      gap: 0.5mm;
    }
    .chapter-header { background: #4a235a; }
    .card-type {
      font-size: 5.5pt;
      opacity: 0.7;
      font-family: Arial, sans-serif;
      letter-spacing: 0.5pt;
      text-transform: uppercase;
    }
    .card-title {
      font-size: 9pt;
      font-weight: bold;
      line-height: 1.25;
    }
    .difficulty-badge, .wrestler-tag, .rule-badge {
      font-size: 6pt;
      font-family: Arial, sans-serif;
      background: rgba(255,255,255,0.2);
      padding: 0.3mm 2mm;
      border-radius: 2mm;
      display: inline-block;
    }
    .card-body {
      padding: 2mm 3mm;
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 1.5mm;
      font-size: 7.5pt;
    }
    .narrative {
      font-style: italic;
      line-height: 1.35;
      flex: 1;
      margin: 0;
    }
    .info-row { font-size: 7pt; }
    .card-count {
      font-size: 7pt;
      font-weight: bold;
      margin-top: auto;
      padding-top: 1mm;
    }
    .expansion-tag {
      display: inline-block;
      font-size: 6pt;
      font-weight: bold;
      font-family: Arial, sans-serif;
      background: #c0392b;
      color: #fff;
      padding: 0.5mm 2mm;
      border-radius: 2mm;
      text-transform: uppercase;
      letter-spacing: 0.5pt;
    }
    .choices {
      border-top: 0.3mm solid #ddd;
      padding-top: 1.5mm;
      display: flex;
      flex-direction: column;
      gap: 1.5mm;
    }
    .choice { display: flex; flex-direction: column; gap: 0.4mm; }
    .choice-label {
      font-size: 7pt;
      font-weight: bold;
      font-family: Arial, sans-serif;
      color: #1a1a2e;
    }
    .choice-text { font-size: 6.5pt; line-height: 1.25; }
    .choice-meta {
      font-size: 6pt;
      font-style: italic;
      font-family: Arial, sans-serif;
      opacity: 0.7;
    }

    /* ── Custom content cards (expansionCode == CUSTOM) ── */
    .segment-type-header { background: #0b4f6c; }
    .segment-rule-header { background: #7b241c; }
    .npc-header { background: #14532d; }
    .title-header { background: #7d6608; }
    .rule-badges {
      display: flex;
      flex-wrap: wrap;
      gap: 1mm;
      padding-top: 0.5mm;
    }
    .npc-meta {
      font-size: 6pt;
      font-family: Arial, sans-serif;
      opacity: 0.85;
      text-transform: uppercase;
      letter-spacing: 0.5pt;
    }
    .npc-image {
      width: 100%;
      max-height: 28mm;
      object-fit: cover;
      border-radius: 1.5mm;
      margin-bottom: 1mm;
    }
    .guide-block {
      border-top: 0.3mm solid #ddd;
      padding-top: 1mm;
      display: flex;
      flex-direction: column;
      gap: 0.8mm;
    }
    .guide-label {
      font-size: 5.5pt;
      font-weight: bold;
      font-family: Arial, sans-serif;
      text-transform: uppercase;
      letter-spacing: 0.5pt;
      opacity: 0.7;
    }
    .guide-text {
      font-size: 6.5pt;
      font-style: italic;
      line-height: 1.3;
    }
    .effect-script {
      font-family: 'Courier New', monospace;
      font-size: 6pt;
      line-height: 1.3;
      background: rgba(0,0,0,0.06);
      border-radius: 1mm;
      padding: 1mm;
      display: flex;
      flex-direction: column;
    }
    .card-footer {
      margin-top: auto;
      padding-top: 1mm;
      display: flex;
      flex-wrap: wrap;
      gap: 1mm;
      align-items: center;
    }
    .empty-state {
      font-size: 10pt;
      opacity: 0.7;
      padding: 5mm;
    }

    #campaign-card-print-title {
      display: none;
    }

    @media print {
      body * { visibility: hidden !important; }
      #campaign-card-print-area,
      #campaign-card-print-area * { visibility: visible !important; }
      #campaign-card-print-area {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
      }
      #campaign-card-print-title {
        display: block;
        flex: 0 0 100%;
        font-family: Georgia, serif;
        font-size: 14pt;
        font-weight: bold;
        margin-bottom: 5mm;
      }
      .campaign-card { border-color: #aaa; }
      .choice-label { color: #000; }
      @page { size: A4; margin: 10mm; }
    }
    """;
  }
}
