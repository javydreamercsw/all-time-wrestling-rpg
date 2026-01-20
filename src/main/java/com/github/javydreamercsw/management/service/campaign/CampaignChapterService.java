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
package com.github.javydreamercsw.management.service.campaign;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CampaignChapterService {

  private final ObjectMapper objectMapper;
  private List<CampaignChapterDTO> chapters = Collections.emptyList();

  @Autowired
  public CampaignChapterService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @PostConstruct
  public void init() {
    loadChapters();
  }

  public void loadChapters() {
    try (InputStream is = getClass().getResourceAsStream("/campaign_chapters.json")) {
      if (is == null) {
        log.error("campaign_chapters.json not found in resources.");
        return;
      }
      chapters = objectMapper.readValue(is, new TypeReference<List<CampaignChapterDTO>>() {});
      log.info("Loaded {} campaign chapters.", chapters.size());
    } catch (IOException e) {
      log.error("Error loading campaign chapters from JSON", e);
    }
  }

  public List<CampaignChapterDTO> getAllChapters() {
    return Collections.unmodifiableList(chapters);
  }

  public Optional<CampaignChapterDTO> getChapter(int chapterNumber) {
    return chapters.stream().filter(c -> c.getChapterNumber() == chapterNumber).findFirst();
  }
}
