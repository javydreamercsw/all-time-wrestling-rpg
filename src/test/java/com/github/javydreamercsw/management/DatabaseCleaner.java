package com.github.javydreamercsw.management;

import com.github.javydreamercsw.management.domain.card.CardRepository;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import com.github.javydreamercsw.management.domain.deck.DeckCardRepository;
import com.github.javydreamercsw.management.domain.deck.DeckRepository;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
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
  }
}
