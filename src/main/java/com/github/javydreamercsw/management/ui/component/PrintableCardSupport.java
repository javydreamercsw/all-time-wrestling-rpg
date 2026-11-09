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

import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRulePlayGuide;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;

/**
 * Shared machinery for printable poker-size (63.5×88.9mm) content cards. Extracted from {@code
 * CampaignCardExportView} so other views (e.g. Weekly Challenges) can render print-only card DOM
 * with the same anatomy and stylesheet idiom.
 *
 * <p>A host view injects the shared stylesheet once via {@link #injectPrintStyles} (call from
 * {@code onAttach}), gives its print region a unique element id, and marks on-screen-only
 * containers with {@code no-print} so the {@code @media print} block hides them. The print block
 * makes everything on the page invisible except the element with id {@code
 * campaign-card-print-area} (and its children) — a host that already renders its own print area
 * with a different id should reuse {@link #printAreaCss} inside its own stylesheet instead of
 * relying on the shared id.
 */
public final class PrintableCardSupport {

  private PrintableCardSupport() {}

  /** Injects the shared card stylesheet into the document head exactly once. */
  public static void injectPrintStyles() {
    UI.getCurrent()
        .getPage()
        .executeJs(
            "if (!document.getElementById($0)) {"
                + "  const s = document.createElement('style');"
                + "  s.id = $0;"
                + "  s.textContent = $1;"
                + "  document.head.appendChild(s);"
                + "}",
            styleElementId(),
            cardCss());
  }

  /** The element id the shared stylesheet registers (one per page). */
  public static String styleElementId() {
    return "campaign-card-export-styles";
  }

  /** Triggers the browser print dialog (the @media print rules take over from there). */
  public static void print() {
    UI.getCurrent().getPage().executeJs("window.print()");
  }

  /** Builds the standard card header: a type label above the card name. */
  public static Div cardHeader(String headerClass, String typeLabel, String name) {
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

  /** Adds a narrative paragraph (italic lead text) if the text is present. */
  public static void addNarrative(Div body, String text) {
    if (text != null && !text.isBlank()) {
      Paragraph p = new Paragraph(text);
      p.addClassName("narrative");
      body.add(p);
    }
  }

  /** Adds the segment-rule solo/multiplayer guide block, if the guide carries overviews. */
  public static void addGuideBlock(Div body, SegmentRulePlayGuide guide) {
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

  /** Adds a semicolon-separated effect script as stacked lines, if present. */
  public static void addScriptLines(Div body, String effectScript) {
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

  /** Adds the expansion footer tag. */
  public static void addExpansionFooter(Div body, String expansionCode) {
    Span exp = new Span(expansionCode != null ? expansionCode : "");
    exp.addClassName("expansion-tag");
    Div footer = new Div();
    footer.addClassName("card-footer");
    footer.add(exp);
    body.add(footer);
  }

  /**
   * The shared card stylesheet: poker-size cards, per-category header colors, and the @media print
   * block that hides everything except the {@code campaign-card-print-area} element.
   */
  public static String cardCss() {
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

    .npc-image {
      width: 100%;
      max-height: 28mm;
      object-fit: cover;
      border-radius: 1.5mm;
      margin-bottom: 1mm;
    }
    .empty-state {
      font-size: 10pt;
      opacity: 0.7;
      padding: 5mm;
    }

    /* ── Custom content cards (expansionCode == CUSTOM) ── */
    .segment-type-header { background: #0b4f6c; }
    .segment-rule-header { background: #7b241c; }
    .npc-header { background: #14532d; }
    .title-header { background: #7d6608; }
    .challenge-header { background: #1e4620; }
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
    }
    .guide-block {
      border-top: 0.3mm solid #ddd;
      padding-top: 1.5mm;
      display: flex;
      flex-direction: column;
      gap: 1mm;
    }
    .guide-label {
      font-size: 6pt;
      font-weight: bold;
      font-family: Arial, sans-serif;
      letter-spacing: 0.5pt;
    }
    .guide-text {
      font-size: 6.5pt;
      line-height: 1.3;
    }
    .effect-script {
      display: flex;
      flex-direction: column;
      gap: 0.8mm;
      font-size: 6.5pt;
      font-family: Arial, sans-serif;
    }
    .card-footer {
      margin-top: auto;
      padding-top: 1mm;
      display: flex;
      justify-content: flex-end;
    }
    .section-label {
      font-size: 6pt;
      font-weight: bold;
      font-family: Arial, sans-serif;
      letter-spacing: 0.5pt;
      text-transform: uppercase;
      opacity: 0.75;
    }
    .section-text {
      font-size: 6.5pt;
      line-height: 1.3;
      margin: 0;
    }

    #campaign-card-print-title {
      display: none;
    }

    /* ── Full-page challenge sheets (one challenge per printed page) ── */
    .challenge-print-page {
      font-family: Georgia, 'Times New Roman', serif;
      color: var(--lumo-body-text-color, #111);
      background: var(--lumo-base-color, #fff);
      border: 0.5mm solid #999;
      border-radius: 2mm;
      padding: 8mm;
      margin-bottom: 6mm;
      display: flex;
      flex-direction: column;
      gap: 4mm;
      min-height: 250mm;
    }
    .challenge-page-header {
      background: #1e4620;
      color: #f5f5f5;
      border-radius: 2mm;
      padding: 5mm 6mm;
      display: flex;
      flex-direction: column;
      gap: 1.5mm;
    }
    .challenge-page-kicker {
      font-size: 8pt;
      font-family: Arial, sans-serif;
      letter-spacing: 1.5pt;
      text-transform: uppercase;
      opacity: 0.8;
    }
    .challenge-page-title {
      font-size: 20pt;
      font-weight: bold;
      line-height: 1.15;
    }
    .challenge-page-meta {
      font-size: 8pt;
      font-family: Arial, sans-serif;
      background: rgba(255,255,255,0.18);
      padding: 1mm 2.5mm;
      border-radius: 1.5mm;
      display: inline-block;
    }
    .challenge-page-body {
      display: flex;
      flex-direction: column;
      gap: 4mm;
      flex: 1;
    }
    .challenge-page-image {
      width: 100%;
      max-height: 90mm;
      object-fit: contain;
      border-radius: 2mm;
    }
    .challenge-page-flavor {
      font-style: italic;
      font-size: 10.5pt;
      line-height: 1.45;
    }
    .challenge-page-section {
      display: flex;
      flex-direction: column;
      gap: 1.5mm;
    }
    .challenge-page-section-label {
      font-size: 8pt;
      font-weight: bold;
      font-family: Arial, sans-serif;
      letter-spacing: 1pt;
      text-transform: uppercase;
      color: #1e4620;
      border-bottom: 0.3mm solid #ccc;
      padding-bottom: 0.8mm;
    }
    .challenge-page-section-text {
      font-size: 10pt;
      line-height: 1.4;
      font-style: normal !important;
    }
    .challenge-page-list-item {
      display: flex;
      flex-direction: column;
      gap: 0.8mm;
    }
    /* On screen the sheets are preview-sized; on paper they fill pages. */
    @media print {
      .challenge-print-page {
        border: none;
        padding: 0;
        margin: 0;
        min-height: auto;
        width: 100%;
        page-break-after: always;
      }
      .challenge-print-page:last-child {
        page-break-after: auto;
      }
    }

    /* ── Print suppression: only the card area (and its title) hits the paper ── */
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
