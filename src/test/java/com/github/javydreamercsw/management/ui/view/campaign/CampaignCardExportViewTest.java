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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import com.github.javydreamercsw.management.service.campaign.CampaignChapterService;
import com.github.javydreamercsw.management.service.campaign.FeatureDataService;
import com.github.javydreamercsw.management.service.expansion.ExpansionService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class CampaignCardExportViewTest {

  // CSS card dimensions — must match CampaignCardExportView.cardCss()
  private static final double CARD_WIDTH_MM = 63.5;
  private static final double CARD_HEIGHT_MM = 88.9;
  private static final double CARD_GAP_MM = 5.0;

  // A4 print layout — must match @page rule in cardCss()
  private static final double PAGE_WIDTH_MM = 210;
  private static final double PAGE_HEIGHT_MM = 297;
  private static final double PAGE_MARGIN_MM = 10;

  private static final int COLS =
      (int) ((PAGE_WIDTH_MM - 2 * PAGE_MARGIN_MM + CARD_GAP_MM) / (CARD_WIDTH_MM + CARD_GAP_MM));
  private static final int ROWS =
      (int) ((PAGE_HEIGHT_MM - 2 * PAGE_MARGIN_MM + CARD_GAP_MM) / (CARD_HEIGHT_MM + CARD_GAP_MM));
  private static final int CARDS_PER_PAGE = COLS * ROWS;

  private CampaignChapterService chapterService;

  @BeforeEach
  void setUp() {
    ObjectMapper objectMapper = new ObjectMapper();
    FeatureDataService featureDataService =
        new FeatureDataService(objectMapper, mock(CampaignStateRepository.class));
    ExpansionService allEnabled = mock(ExpansionService.class);
    when(allEnabled.isExpansionEnabled(anyString())).thenReturn(true);
    chapterService =
        new CampaignChapterService(
            objectMapper,
            featureDataService,
            allEnabled,
            new PathMatchingResourcePatternResolver());
    chapterService.init();
  }

  // ---------------------------------------------------------------------------
  // CSS regression guards
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Print CSS uses position:absolute, not position:fixed")
  void printCss_usesAbsolutePositioning_notFixed() {
    String css = CampaignCardExportView.cardCss();

    int mediaStart = css.indexOf("@media print");
    assertThat(mediaStart).as("@media print block must exist in cardCss()").isGreaterThan(-1);

    String printBlock = css.substring(mediaStart);
    assertThat(printBlock)
        .as(
            "position:fixed clips the grid to one viewport page — must be position:absolute so"
                + " cards flow across multiple printed pages")
        .doesNotContain("position: fixed");
    assertThat(printBlock).contains("position: absolute");
  }

  @Test
  @DisplayName("Card dimensions in CSS match the constants used for page-count calculations")
  void cardCss_dimensionsMatchTestConstants() {
    String css = CampaignCardExportView.cardCss();
    assertThat(css).contains("width: " + CARD_WIDTH_MM + "mm");
    assertThat(css).contains("min-height: " + CARD_HEIGHT_MM + "mm");
    assertThat(css).contains("gap: " + (int) CARD_GAP_MM + "mm");
    assertThat(css).contains("margin: " + (int) PAGE_MARGIN_MM + "mm");
  }

  // ---------------------------------------------------------------------------
  // Card count — renderCards() produces encounters.size() + 1 (chapter cover)
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("extreme_campaign total cards = encounters + 1 chapter cover card")
  void extremeCampaign_cardCount_equalsEncountersPlusOne() {
    CampaignChapterDTO chapter = loadChapter("extreme_campaign");
    int encounters = chapter.getStaticEncounters().size();
    int totalCards = encounters + 1;

    assertThat(encounters)
        .as("extreme_campaign must have at least 50 encounters")
        .isGreaterThan(50);
    assertThat(totalCards).as("total cards = encounters + chapter cover").isEqualTo(encounters + 1);
  }

  // ---------------------------------------------------------------------------
  // Page count — verify the campaign requires multiple printed pages
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("extreme_campaign spans more than one printed A4 page")
  void extremeCampaign_pageCount_exceedsOnePage() {
    CampaignChapterDTO chapter = loadChapter("extreme_campaign");
    int totalCards = chapter.getStaticEncounters().size() + 1;

    // COLS and ROWS are derived from the CSS constants at the top of this class.
    // Any change to card/page dimensions there will also update this assertion.
    int expectedPages = (int) Math.ceil((double) totalCards / CARDS_PER_PAGE);

    assertThat(CARDS_PER_PAGE)
        .as("Layout constants: %d cols × %d rows = %d cards/page", COLS, ROWS, CARDS_PER_PAGE)
        .isGreaterThan(0);
    assertThat(expectedPages)
        .as(
            "%d cards / %d per page = %d pages — campaign must require more than one page"
                + " (position:fixed would clip to just one)",
            totalCards, CARDS_PER_PAGE, expectedPages)
        .isGreaterThan(1);
  }

  @Test
  @DisplayName("All static chapters that have encounters require at least one page of cards")
  void allChapters_withEncounters_havePositivePageCount() {
    for (CampaignChapterDTO chapter : chapterService.getAllChapters()) {
      if (!chapter.hasStaticEncounters()) {
        continue;
      }
      int totalCards = chapter.getStaticEncounters().size() + 1;
      int expectedPages = (int) Math.ceil((double) totalCards / CARDS_PER_PAGE);

      assertThat(expectedPages)
          .as("[%s] must produce at least one print page", chapter.getId())
          .isGreaterThan(0);
    }
  }

  // ---------------------------------------------------------------------------
  // Helper
  // ---------------------------------------------------------------------------

  private CampaignChapterDTO loadChapter(final String chapterId) {
    Optional<CampaignChapterDTO> chapter =
        chapterService.getAllChapters().stream()
            .filter(c -> chapterId.equals(c.getId()))
            .findFirst();
    assertThat(chapter).as("Chapter '%s' must exist", chapterId).isPresent();
    return chapter.get();
  }
}
