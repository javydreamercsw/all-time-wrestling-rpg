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

import com.github.javydreamercsw.management.domain.news.NewsCategory;
import com.github.javydreamercsw.management.domain.news.NewsItem;
import com.github.javydreamercsw.management.domain.news.NewsRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class NewsService {
  private final NewsRepository newsRepository;

  @Transactional(readOnly = true)
  public List<NewsItem> getLatestNews() {
    return newsRepository.findTop10ByOrderByPublishDateDesc();
  }

  @Transactional(readOnly = true)
  public List<NewsItem> getAllNews() {
    return newsRepository.findAllByOrderByPublishDateDesc();
  }

  @Transactional
  public NewsItem createNewsItem(
      String headline, String content, NewsCategory category, boolean isRumor, int importance) {
    log.info("Creating news item: {}", headline);
    NewsItem item =
        NewsItem.builder()
            .headline(headline)
            .content(content)
            .category(category)
            .isRumor(isRumor)
            .importance(importance)
            .publishDate(Instant.now())
            .build();
    return newsRepository.save(item);
  }

  @Transactional
  public void deleteNewsItem(Long id) {
    log.info("Deleting news item: {}", id);
    newsRepository.deleteById(id);
  }
}
