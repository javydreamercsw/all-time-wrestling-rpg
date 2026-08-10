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
package com.github.javydreamercsw.management.sync;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.service.card.CardSetService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(70)
public class CardSetSync implements DataSyncContributor {

  private final boolean skipIfNotEmpty;
  private final CardSetService cardSetService;
  private final ObjectMapper objectMapper;

  @Autowired
  public CardSetSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final boolean skipIfNotEmpty,
      final CardSetService cardSetService,
      final ObjectMapper objectMapper) {
    this.skipIfNotEmpty = skipIfNotEmpty;
    this.cardSetService = cardSetService;
    this.objectMapper = objectMapper;
  }

  @Override
  public void sync() {
    if (skipIfNotEmpty && cardSetService.count() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("sets.json");
    if (!resource.exists()) {
      return;
    }
    log.debug("Loading card sets from file: {}", resource.getPath());
    try (var is = resource.getInputStream()) {
      List<CardSet> setsFromFile = objectMapper.readValue(is, new TypeReference<>() {});
      List<CardSet> toSave = new ArrayList<>();
      for (CardSet c : setsFromFile) {
        Optional<CardSet> existingOpt = cardSetService.findBySetCode(c.getCode());
        if (existingOpt.isPresent()) {
          CardSet existing = existingOpt.get();
          existing.setName(c.getName());
          toSave.add(existing);
        } else {
          toSave.add(c);
        }
      }
      cardSetService.saveAll(toSave);
      log.debug("Card sets loading completed - {} sets processed", setsFromFile.size());
    } catch (IOException e) {
      log.error("Error loading card sets from file", e);
    }
  }
}
