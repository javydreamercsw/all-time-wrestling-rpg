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
package com.github.javydreamercsw.management.domain.show.segment.rule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

@Converter
@Slf4j
public class SegmentRulePlayGuideConverter
    implements AttributeConverter<SegmentRulePlayGuide, String> {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(final SegmentRulePlayGuide attribute) {
    if (attribute == null) {
      return null;
    }
    try {
      return MAPPER.writeValueAsString(attribute);
    } catch (JsonProcessingException e) {
      log.error("Error serializing SegmentRulePlayGuide to JSON", e);
      return null;
    }
  }

  @Override
  public SegmentRulePlayGuide convertToEntityAttribute(final String dbData) {
    if (dbData == null || dbData.isBlank() || "null".equals(dbData)) {
      return null;
    }
    try {
      return MAPPER.readValue(dbData, SegmentRulePlayGuide.class);
    } catch (JsonProcessingException e) {
      log.error("Error deserializing SegmentRulePlayGuide from JSON", e);
      return null;
    }
  }
}
