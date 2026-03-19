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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.campaign.CampaignStoryline;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import com.github.javydreamercsw.management.dto.campaign.ChapterPointDTO;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorylineExportService {

  private final ObjectMapper objectMapper;

  public String exportStorylineAsChapter(@NonNull CampaignStoryline storyline) {
    CampaignChapterDTO chapterDTO = toChapterDTO(storyline);
    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(chapterDTO);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      log.error("Error exporting storyline as chapter", e);
      return "{}";
    }
  }

  public CampaignChapterDTO toChapterDTO(@NonNull CampaignStoryline storyline) {
    // Determine entry and exit points for the chapter based on the storyline's milestones
    List<ChapterPointDTO> entryPoints =
        List.of(
            ChapterPointDTO.builder()
                .name("Storyline: " + storyline.getTitle())
                .criteria(List.of()) // Criteria would need to be inferred or user-defined
                .build());

    List<ChapterPointDTO> exitPoints =
        List.of(
            ChapterPointDTO.builder()
                .name("Storyline Completed: " + storyline.getTitle())
                .criteria(List.of()) // Criteria would need to be inferred from storyline completion
                .build());

    // AI System Prompt should reflect the overarching goal of the storyline
    String aiSystemPrompt =
        String.format(
            "You are the Campaign Director for the storyline '%s'. Your goal is to guide the player"
                + " through the narrative described by this storyline.",
            storyline.getTitle());

    // For simplicity, we'll extract the title and description, and use a generic prompt.
    // Milestones don't directly map to rules/exclusions in a ChapterDTO.
    // A more advanced export might try to infer these.
    return CampaignChapterDTO.builder()
        .id(formatId(storyline.getTitle()))
        .title(storyline.getTitle())
        .shortDescription(storyline.getDescription())
        .introText(storyline.getDescription())
        .aiSystemPrompt(aiSystemPrompt)
        .difficulty(
            com.github.javydreamercsw.management.domain.campaign.Difficulty
                .MEDIUM) // Default to MEDIUM
        .tagTeam(false) // Needs to be inferred from storyline content
        .tournament(false) // Needs to be inferred from storyline content
        .entryPoints(entryPoints)
        .exitPoints(exitPoints)
        .rules(
            CampaignChapterDTO.ChapterRules.builder()
                .victoryPointsWin(2) // Default values
                .victoryPointsLoss(-1)
                .build())
        .exclusions(CampaignChapterDTO.ChapterExclusions.builder().build())
        .build();
  }

  private String formatId(@NonNull String title) {
    return title
        .toLowerCase()
        .replaceAll("[^a-z0-9 -]", "")
        .replaceAll(" +", "_")
        .replaceAll("_{2,}", "_")
        .trim();
  }
}
