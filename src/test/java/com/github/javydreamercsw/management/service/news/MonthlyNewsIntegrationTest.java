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
package com.github.javydreamercsw.management.service.news;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.DataInitializer;
import com.github.javydreamercsw.management.domain.news.NewsCategory;
import com.github.javydreamercsw.management.domain.news.NewsItem;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

class MonthlyNewsIntegrationTest extends AbstractIntegrationTest {

  @Autowired private EventAggregationService aggregationService;
  @Autowired private NewsGenerationService newsGenerationService;
  @Autowired private NewsService newsService;
  @Autowired private DataInitializer dataInitializer;

  @BeforeEach
  void setup() {
    dataInitializer.init();
  }

  @Test
  @Transactional
  void testMonthlyAggregationAndSynthesis() {
    // 1. Setup Data
    Wrestler w1 = createTestWrestler("News Maker A");
    w1 = wrestlerRepository.save(w1);
    Wrestler w2 = createTestWrestler("News Maker B");
    w2 = wrestlerRepository.save(w2);

    ShowType weekly = showTypeRepository.findAll().getFirst();

    Show show = new Show();
    show.setName("Monthly PLE");
    show.setDescription("Test Description");
    show.setType(weekly);
    show = showRepository.save(show);

    SegmentType matchType = segmentTypeRepository.findAll().getFirst();

    // Create a segment from 10 days ago
    Segment s = new Segment();
    s.setShow(show);
    s.setSegmentType(matchType);
    s.setSegmentDate(Instant.now().minus(10, ChronoUnit.DAYS));
    s.addParticipant(w1);
    s.addParticipant(w2);
    s.setWinners(List.of(w1));
    s.setMainEvent(true);
    s = segmentRepository.save(s);

    // 2. Test Aggregation
    EventAggregationService.MonthlySummary summary = aggregationService.getMonthlySummary();
    assertThat(summary.getSegments()).isNotEmpty();
    assertThat(summary.getSegments()).contains(s);

    String formatted = aggregationService.formatMonthlySummary(summary);
    assertThat(formatted).contains("News Maker A won");

    // 3. Test Synthesis (using Mock AI)
    newsGenerationService.generateMonthlySynthesis();

    List<NewsItem> news = newsService.getAllNews();
    assertThat(news)
        .anyMatch(
            item ->
                item.getCategory() == NewsCategory.ANALYSIS
                    && item.getImportance() == 5
                    && !item.getIsRumor());

    assertThat(newsService.getLatestMonthlyAnalysis()).isPresent();
  }
}
