/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.service.sync.entity.notion;

import com.github.javydreamercsw.base.ai.notion.NotionApiExecutor;
import com.github.javydreamercsw.base.ai.notion.NotionPropertyBuilder;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;
import notion.api.v1.model.pages.PageProperty;
import org.springframework.stereotype.Service;

@Service
public class SegmentNotionSyncService extends BaseNotionSyncService<Segment> {

  public SegmentNotionSyncService(
      SegmentRepository repository,
      SyncServiceDependencies syncServiceDependencies,
      NotionApiExecutor notionApiExecutor) {
    super(repository, syncServiceDependencies, notionApiExecutor);
  }

  @Override
  protected Map<String, PageProperty> getProperties(@NonNull Segment entity) {
    Map<String, PageProperty> properties = new HashMap<>();
    properties.put("Name", NotionPropertyBuilder.createTitleProperty(entity.getName()));

    if (entity.getShow() != null && entity.getShow().getExternalId() != null) {
      properties.put(
          "Shows", NotionPropertyBuilder.createRelationProperty(entity.getShow().getExternalId()));
    }

    if (entity.getSegmentType() != null && entity.getSegmentType().getExternalId() != null) {
      properties.put(
          "Segment Type",
          NotionPropertyBuilder.createRelationProperty(entity.getSegmentType().getExternalId()));
    }

    if (entity.getSegmentDate() != null) {
      properties.put(
          "Date", NotionPropertyBuilder.createDateProperty(entity.getSegmentDate().toString()));
    }

    if (entity.getSegmentRules() != null && !entity.getSegmentRules().isEmpty()) {
      properties.put(
          "Rules",
          NotionPropertyBuilder.createRelationProperty(
              entity.getSegmentRules().stream()
                  .map(
                      com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule
                          ::getExternalId)
                  .filter(java.util.Objects::nonNull)
                  .toList()));
    }

    if (entity.getNarration() != null) {
      properties.put(
          "Narration", NotionPropertyBuilder.createRichTextProperty(entity.getNarration()));
    }

    if (entity.getSummary() != null) {
      properties.put("Summary", NotionPropertyBuilder.createRichTextProperty(entity.getSummary()));
    }

    properties.put("Status", NotionPropertyBuilder.createSelectProperty(entity.getStatus().name()));

    properties.put(
        "Adjudication Status",
        NotionPropertyBuilder.createSelectProperty(entity.getAdjudicationStatus().name()));

    properties.put(
        "Is Title Segment",
        NotionPropertyBuilder.createCheckboxProperty(entity.getIsTitleSegment()));

    properties.put(
        "Is Main Event", NotionPropertyBuilder.createCheckboxProperty(entity.isMainEvent()));

    if (entity.getWinners() != null && !entity.getWinners().isEmpty()) {
      properties.put(
          "Winners",
          NotionPropertyBuilder.createRelationProperty(
              entity.getWinners().stream()
                  .map(com.github.javydreamercsw.management.domain.wrestler.Wrestler::getExternalId)
                  .filter(java.util.Objects::nonNull)
                  .toList()));
    }

    if (entity.getTitles() != null && !entity.getTitles().isEmpty()) {
      properties.put(
          "Title(s)",
          NotionPropertyBuilder.createRelationProperty(
              entity.getTitles().stream()
                  .map(com.github.javydreamercsw.management.domain.title.Title::getExternalId)
                  .filter(java.util.Objects::nonNull)
                  .toList()));
    }

    if (entity.getReferee() != null && entity.getReferee().getExternalId() != null) {
      properties.put(
          "Referee(s)",
          NotionPropertyBuilder.createRelationProperty(entity.getReferee().getExternalId()));
    }

    properties.put(
        "Referee Awareness Level",
        NotionPropertyBuilder.createNumberProperty((double) entity.getRefereeAwarenessLevel()));

    if (entity.getWrestlers() != null && !entity.getWrestlers().isEmpty()) {
      properties.put(
          "Participants",
          NotionPropertyBuilder.createRelationProperty(
              entity.getWrestlers().stream()
                  .map(com.github.javydreamercsw.management.domain.wrestler.Wrestler::getExternalId)
                  .filter(java.util.Objects::nonNull)
                  .toList()));
    }

    properties.put(
        "Sequence", NotionPropertyBuilder.createNumberProperty((double) entity.getSegmentOrder()));

    return properties;
  }

  @Override
  protected String getDatabaseName() {
    return "Segments";
  }

  @Override
  protected String getEntityName() {
    return "Segment";
  }
}
