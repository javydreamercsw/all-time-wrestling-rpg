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

import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import com.github.javydreamercsw.management.dto.campaign.StaticEncounterDTO;
import com.github.javydreamercsw.management.service.campaign.CampaignChapterService;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Admin-only view that renders campaign encounter cards directly in the browser at poker-card size
 * (63.5×88.9mm). Use the Print button or Ctrl+P / Cmd+P — the Vaadin shell is suppressed
 * automatically via @media print.
 */
@Route(value = "campaign-card-export", layout = MainLayout.class)
@PageTitle("Campaign Card Export")
@RolesAllowed(ADMIN_ROLE)
@Slf4j
public class CampaignCardExportView extends VerticalLayout {

  private static final String STYLE_ID = "campaign-card-export-styles";

  private final CampaignChapterService chapterService;
  private final ComboBox<CampaignChapterDTO> chapterSelect = new ComboBox<>("Campaign Chapter");
  private final Div cardGrid = new Div();
  private final Button printBtn =
      new Button("Print Cards", VaadinIcon.PRINT.create(), e -> print());

  public CampaignCardExportView(final CampaignChapterService chapterService) {
    this.chapterService = chapterService;
    setPadding(true);
    setSpacing(true);
    buildUi();
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
    add(new H2("Campaign Card Export"));
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

    printBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    printBtn.setEnabled(false);

    HorizontalLayout toolbar = new HorizontalLayout(chapterSelect, printBtn);
    toolbar.setAlignItems(Alignment.END);

    cardGrid.setId("campaign-card-print-area");
    cardGrid.addClassName("campaign-card-grid");

    add(toolbar, cardGrid);

    // Default to extreme_campaign, fallback to first available
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
  }

  // ── Card rendering ────────────────────────────────────────────────────────

  private void renderCards(CampaignChapterDTO chapter) {
    cardGrid.removeAll();
    printBtn.setEnabled(chapter != null);
    if (chapter == null) {
      return;
    }
    cardGrid.add(buildChapterCard(chapter));
    List<StaticEncounterDTO> encounters = chapter.getStaticEncounters();
    for (int i = 0; i < encounters.size(); i++) {
      cardGrid.add(buildEncounterCard(encounters.get(i), i + 1, encounters.size()));
    }
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

  private static String cardCss() {
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
    .difficulty-badge, .wrestler-tag {
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
      .campaign-card { border-color: #aaa; }
      .choice-label { color: #000; }
      @page { size: A4; margin: 10mm; }
    }
    """;
  }
}
