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
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import jakarta.annotation.security.RolesAllowed;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Admin-only view that generates a printable poker-card-sized HTML sheet for any campaign chapter
 * that has static encounter cards. Download the HTML file and open it in a browser to print.
 */
@Route(value = "campaign-card-export", layout = MainLayout.class)
@PageTitle("Campaign Card Export")
@RolesAllowed(ADMIN_ROLE)
@Slf4j
public class CampaignCardExportView extends VerticalLayout {

  private final CampaignChapterService chapterService;
  private final ComboBox<CampaignChapterDTO> chapterSelect = new ComboBox<>("Campaign Chapter");
  private final VerticalLayout previewPanel = new VerticalLayout();

  public CampaignCardExportView(final CampaignChapterService chapterService) {
    this.chapterService = chapterService;
    setPadding(true);
    setSpacing(true);
    buildUi();
  }

  private void buildUi() {
    add(new H2("Campaign Card Export"));
    add(
        new Paragraph(
            "Select a campaign chapter to download a print-ready HTML card sheet. "
                + "Open the downloaded file in any browser and use Ctrl+P / Cmd+P to print."));

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
    chapterSelect.addValueChangeListener(e -> refreshPreview(e.getValue()));

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

    previewPanel.setPadding(false);
    previewPanel.setSpacing(false);

    Anchor downloadAnchor =
        new Anchor(
            DownloadHandler.fromInputStream(
                event -> {
                  CampaignChapterDTO chapter = chapterSelect.getValue();
                  if (chapter == null) {
                    return DownloadResponse.error(400);
                  }
                  String html = generatePrintHtml(chapter);
                  byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
                  String filename =
                      chapter.getTitle().toLowerCase().replaceAll("[^a-z0-9]", "-") + "-cards.html";
                  return new DownloadResponse(
                      new ByteArrayInputStream(bytes),
                      filename,
                      "text/html; charset=utf-8",
                      bytes.length);
                }),
            "");

    Button downloadBtn = new Button("Download Print Sheet", VaadinIcon.PRINT.create());
    downloadBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    downloadAnchor.add(downloadBtn);

    add(chapterSelect, previewPanel, downloadAnchor);
  }

  private void refreshPreview(CampaignChapterDTO chapter) {
    previewPanel.removeAll();
    if (chapter == null) {
      return;
    }

    Div card = new Div();
    card.getStyle()
        .set("background", "var(--lumo-base-color)")
        .set("border", "1px solid var(--lumo-contrast-10pct)")
        .set("border-radius", "var(--lumo-border-radius-m)")
        .set("padding", "var(--lumo-space-m)")
        .set("max-width", "480px");

    card.add(new H3(chapter.getTitle()));

    if (chapter.getDifficulty() != null) {
      Paragraph diff = new Paragraph("Difficulty: " + chapter.getDifficulty());
      diff.getStyle().set("font-size", "var(--lumo-font-size-s)");
      card.add(diff);
    }

    Paragraph count =
        new Paragraph(
            "Encounter cards: "
                + chapter.getStaticEncounters().size()
                + "  (+ 1 chapter summary card)");
    count.getStyle().set("font-size", "var(--lumo-font-size-s)");
    card.add(count);

    if (!chapter.getRequiredExpansions().isEmpty()) {
      Span badge = new Span("Requires: " + String.join(", ", chapter.getRequiredExpansions()));
      badge
          .getStyle()
          .set("background", "var(--lumo-error-color-10pct)")
          .set("color", "var(--lumo-error-color)")
          .set("padding", "2px 8px")
          .set("border-radius", "var(--lumo-border-radius-s)")
          .set("font-size", "var(--lumo-font-size-xs)");
      card.add(badge);
    }

    if (!chapter.getAllowedWrestlerNames().isEmpty()) {
      Paragraph wrestlers =
          new Paragraph(
              "Allowed wrestlers: " + String.join(", ", chapter.getAllowedWrestlerNames()));
      wrestlers.getStyle().set("font-size", "var(--lumo-font-size-s)");
      card.add(wrestlers);
    }

    previewPanel.add(card);
  }

  // ── HTML generation ───────────────────────────────────────────────────────

  private String generatePrintHtml(CampaignChapterDTO chapter) {
    StringBuilder sb = new StringBuilder();
    sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
    sb.append("<meta charset=\"UTF-8\">\n");
    sb.append("<title>").append(esc(chapter.getTitle())).append(" — Print Cards</title>\n");
    sb.append("<style>\n").append(printCss()).append("\n</style>\n");
    sb.append("</head>\n<body>\n");

    sb.append("<div class=\"toolbar\">\n");
    sb.append("  <div class=\"toolbar-text\">\n");
    sb.append("    <strong>").append(esc(chapter.getTitle())).append(" — Print Cards</strong>\n");
    sb.append(
        "    <span>Open Ctrl+P / Cmd+P &nbsp;·&nbsp; Paper: A4 &nbsp;·&nbsp;"
            + " Margins: 10 mm &nbsp;·&nbsp; Scale: 100%</span>\n");
    sb.append("  </div>\n");
    sb.append("  <button onclick=\"window.print()\">🖨&nbsp;Print Now</button>\n");
    sb.append("</div>\n");

    sb.append("<div class=\"card-grid\">\n");
    appendChapterCard(sb, chapter);
    List<StaticEncounterDTO> encounters = chapter.getStaticEncounters();
    for (int i = 0; i < encounters.size(); i++) {
      appendEncounterCard(sb, encounters.get(i), i + 1, encounters.size());
    }
    sb.append("</div>\n</body>\n</html>\n");
    return sb.toString();
  }

  private void appendChapterCard(StringBuilder sb, CampaignChapterDTO chapter) {
    sb.append("<div class=\"card chapter-card\">\n");
    sb.append("  <div class=\"card-header\">\n");
    sb.append("    <span class=\"card-type\">CAMPAIGN</span>\n");
    sb.append("    <div class=\"card-title\">").append(esc(chapter.getTitle())).append("</div>\n");
    if (chapter.getDifficulty() != null) {
      sb.append("    <span class=\"difficulty-badge\">")
          .append(chapter.getDifficulty())
          .append("</span>\n");
    }
    sb.append("  </div>\n");
    sb.append("  <div class=\"card-body\">\n");
    if (chapter.getIntroText() != null && !chapter.getIntroText().isBlank()) {
      sb.append("    <p class=\"intro-text\">")
          .append(esc(chapter.getIntroText()))
          .append("</p>\n");
    }
    if (!chapter.getAllowedWrestlerNames().isEmpty()) {
      sb.append("    <div class=\"info-row\"><strong>Wrestlers:</strong> ")
          .append(esc(String.join(", ", chapter.getAllowedWrestlerNames())))
          .append("</div>\n");
    }
    if (!chapter.getRequiredExpansions().isEmpty()) {
      sb.append("    <div class=\"expansion-tag\">")
          .append(esc(String.join(" · ", chapter.getRequiredExpansions())))
          .append("</div>\n");
    }
    sb.append("    <div class=\"card-count\">")
        .append(chapter.getStaticEncounters().size())
        .append(" Encounter Cards</div>\n");
    sb.append("  </div>\n");
    sb.append("</div>\n");
  }

  private void appendEncounterCard(StringBuilder sb, StaticEncounterDTO enc, int index, int total) {
    sb.append("<div class=\"card encounter-card\">\n");
    sb.append("  <div class=\"card-header\">\n");
    sb.append("    <span class=\"card-num\">")
        .append(index)
        .append(" / ")
        .append(total)
        .append("</span>\n");
    sb.append("    <div class=\"card-title\">").append(esc(enc.getTitle())).append("</div>\n");
    if (enc.getRequiredWrestlerName() != null) {
      sb.append("    <span class=\"wrestler-tag\">")
          .append(esc(enc.getRequiredWrestlerName()))
          .append("</span>\n");
    }
    sb.append("  </div>\n");
    sb.append("  <div class=\"card-body\">\n");
    if (enc.getNarrativeText() != null) {
      sb.append("    <p class=\"narrative\">").append(esc(enc.getNarrativeText())).append("</p>\n");
    }
    if (enc.getChoices() != null && !enc.getChoices().isEmpty()) {
      sb.append("    <div class=\"choices\">\n");
      for (StaticEncounterDTO.StaticChoiceDTO choice : enc.getChoices()) {
        appendChoiceHtml(sb, choice);
      }
      sb.append("    </div>\n");
    }
    sb.append("  </div>\n");
    sb.append("</div>\n");
  }

  private void appendChoiceHtml(StringBuilder sb, StaticEncounterDTO.StaticChoiceDTO choice) {
    sb.append("      <div class=\"choice\">\n");
    sb.append("        <span class=\"choice-label\">▶ ")
        .append(esc(choice.getLabel()))
        .append("</span>\n");
    if (choice.getText() != null && !choice.getText().isBlank()) {
      sb.append("        <span class=\"choice-text\">")
          .append(esc(choice.getText()))
          .append("</span>\n");
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
      sb.append("        <span class=\"choice-meta\">")
          .append(esc(meta.toString().trim()))
          .append("</span>\n");
    }
    sb.append("      </div>\n");
  }

  private String printCss() {
    return """
    * { box-sizing: border-box; margin: 0; padding: 0; }

    body {
      font-family: Georgia, 'Times New Roman', serif;
      background: #e8e8e8;
      padding: 16px;
      color: #111;
    }

    /* Screen toolbar */
    .toolbar {
      background: #1a1a2e;
      color: #f0f0f0;
      padding: 10px 16px;
      margin-bottom: 20px;
      border-radius: 6px;
      display: flex;
      align-items: center;
      gap: 16px;
    }
    .toolbar-text { flex: 1; display: flex; flex-direction: column; gap: 4px; font-size: 10pt; }
    .toolbar-text strong { font-size: 12pt; }
    .toolbar-text span { opacity: 0.75; font-size: 9pt; }
    .toolbar button {
      background: #27ae60; color: white; border: none;
      padding: 8px 18px; font-size: 11pt; cursor: pointer;
      border-radius: 4px; white-space: nowrap;
    }
    .toolbar button:hover { background: #219a52; }

    /* Card grid */
    .card-grid {
      display: flex;
      flex-wrap: wrap;
      gap: 5mm;
      justify-content: flex-start;
    }

    /* Base card — standard poker size 63.5 × 88.9 mm */
    .card {
      width: 63.5mm;
      min-height: 88.9mm;
      border: 0.5mm solid #555;
      border-radius: 3mm;
      overflow: hidden;
      display: flex;
      flex-direction: column;
      background: #fff;
      break-inside: avoid;
      page-break-inside: avoid;
    }

    .card-header {
      background: #1a1a2e;
      color: #f0f0f0;
      padding: 2mm 3mm;
      flex-shrink: 0;
    }
    .chapter-card .card-header { background: #4a235a; }

    .card-type, .card-num {
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
      margin: 1mm 0 0.5mm;
    }
    .difficulty-badge, .wrestler-tag {
      display: inline-block;
      font-size: 6pt;
      font-family: Arial, sans-serif;
      background: rgba(255,255,255,0.2);
      padding: 0.3mm 2mm;
      border-radius: 2mm;
    }

    .card-body {
      padding: 2mm 3mm;
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 1.5mm;
      font-size: 7.5pt;
    }

    .intro-text, .narrative {
      font-style: italic;
      line-height: 1.35;
      color: #1a1a1a;
      flex: 1;
    }

    .info-row { color: #333; font-size: 7pt; }
    .card-count { font-size: 7pt; font-weight: bold; color: #555; margin-top: auto; padding-top: 1mm; }

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

    /* Choices */
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
    .choice-text {
      font-size: 6.5pt;
      color: #333;
      line-height: 1.25;
    }
    .choice-meta {
      font-size: 6pt;
      color: #666;
      font-family: Arial, sans-serif;
      font-style: italic;
    }

    @media print {
      .toolbar { display: none !important; }
      body { background: white; padding: 0; }
      .card-grid { gap: 4mm; }
      .card { border-color: #aaa; }
      @page { size: A4; margin: 10mm; }
    }
    """;
  }

  private String esc(String s) {
    if (s == null) {
      return "";
    }
    return s.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }
}
