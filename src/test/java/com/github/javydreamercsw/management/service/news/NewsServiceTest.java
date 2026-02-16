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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.news.NewsCategory;
import com.github.javydreamercsw.management.domain.news.NewsItem;
import com.github.javydreamercsw.management.domain.news.NewsRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NewsServiceTest {

  private NewsRepository newsRepository;
  private NewsService newsService;

  @BeforeEach
  void setUp() {
    newsRepository = mock(NewsRepository.class);
    newsService = new NewsService(newsRepository);
  }

  @Test
  void testCreateNewsItem() {
    String headline = "Shocking Upset!";
    String content = "Local underdog defeats world champion in a non-title match.";
    NewsCategory category = NewsCategory.BREAKING;

    when(newsRepository.save(any(NewsItem.class)))
        .thenAnswer(
            invocation -> {
              NewsItem item = invocation.getArgument(0);
              return item;
            });

    NewsItem result = newsService.createNewsItem(headline, content, category, false, 5);

    assertNotNull(result);
    assertEquals(headline, result.getHeadline());
    assertEquals(content, result.getContent());
    assertEquals(category, result.getCategory());
    assertEquals(5, result.getImportance());

    verify(newsRepository).save(any(NewsItem.class));
  }

  @Test
  void testGetLatestNews() {
    NewsItem item =
        NewsItem.builder()
            .headline("Test Headline")
            .content("Test Content")
            .category(NewsCategory.ANALYSIS)
            .build();

    when(newsRepository.findTop10ByOrderByPublishDateDesc()).thenReturn(List.of(item));

    List<NewsItem> result = newsService.getLatestNews();

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("Test Headline", result.get(0).getHeadline());

    verify(newsRepository).findTop10ByOrderByPublishDateDesc();
  }
}
