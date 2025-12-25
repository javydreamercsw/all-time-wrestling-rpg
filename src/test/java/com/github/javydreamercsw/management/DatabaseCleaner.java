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
package com.github.javydreamercsw.management;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.card.CardRepository;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import com.github.javydreamercsw.management.domain.deck.DeckCardRepository;
import com.github.javydreamercsw.management.domain.deck.DeckRepository;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseCleaner {

  @Autowired private DeckCardRepository deckCardRepository;
  @Autowired private DeckRepository deckRepository;
  @Autowired private ShowRepository showRepository;
  @Autowired private TitleRepository titleRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private CardRepository cardRepository;
  @Autowired private CardSetRepository cardSetRepository;
  @Autowired private ShowTemplateRepository showTemplateRepository;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private SegmentRuleRepository segmentRuleRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;
  @Autowired private SeasonRepository seasonRepository;

  @Transactional
  public void clearRepositories() {
    deckCardRepository.deleteAllInBatch();
    deckRepository.deleteAll();
    showRepository.deleteAll();
    titleRepository.deleteAll();
    wrestlerRepository.deleteAll();
    cardRepository.deleteAll();
    cardSetRepository.deleteAll();
    showTemplateRepository.deleteAll();
    showTypeRepository.deleteAll();
    segmentRuleRepository.deleteAll();
    segmentTypeRepository.deleteAll();
    seasonRepository.deleteAll();
  }
}
