package com.github.javydreamercsw.management.domain;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.card.CardService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class DataInitializer {

  @Bean
  public ApplicationRunner syncCardsFromFile(CardService cardService) {
    return args -> {
      ClassPathResource resource = new ClassPathResource("cards.json");
      if (resource.exists()) {
        ObjectMapper mapper = new ObjectMapper();
        try (var is = resource.getInputStream()) {
          var cardsFromFile = mapper.readValue(is, new TypeReference<List<Card>>() {});
          // Map existing cards by name
          Map<String, Card> existing =
              cardService.findAll().stream().collect(Collectors.toMap(Card::getName, c -> c));
          for (Card c : cardsFromFile) {
            Card existingCard = existing.get(c.getName());
            if (existingCard != null) {
              // Update fields
              existingCard.setDamage(c.getDamage());
              existingCard.setFinisher(c.getFinisher());
              existingCard.setMomentum(c.getMomentum());
              existingCard.setSignature(c.getSignature());
              existingCard.setStamina(c.getStamina());
              existingCard.setTarget(c.getTarget());
              cardService.save(existingCard);
            } else {
              cardService.save(c);
            }
          }
        }
      }
    };
  }

  @Bean
  public ApplicationRunner syncWrestlersFromFile(WrestlerService wrestlerService) {
    return args -> {
      ClassPathResource resource = new ClassPathResource("wrestlers.json");
      if (resource.exists()) {
        ObjectMapper mapper = new ObjectMapper();
        try (var is = resource.getInputStream()) {
          var wrestlersFromFile = mapper.readValue(is, new TypeReference<List<Wrestler>>() {});
          // Map existing wrestlers by name
          Map<String, Wrestler> existing =
              wrestlerService.findAll().stream()
                  .collect(Collectors.toMap(Wrestler::getName, w -> w));
          for (Wrestler w : wrestlersFromFile) {
            Wrestler existingWrestler = existing.get(w.getName());
            if (existingWrestler != null) {
              // Update fields
              existingWrestler.setDeckSize(w.getDeckSize());
              existingWrestler.setStartingHealth(w.getStartingHealth());
              existingWrestler.setLowHealth(w.getLowHealth());
              existingWrestler.setStartingStamina(w.getStartingStamina());
              existingWrestler.setLowStamina(w.getLowStamina());
              wrestlerService.save(existingWrestler);
            } else {
              wrestlerService.save(w);
            }
          }
        }
      }
    };
  }
}
