package com.github.javydreamercsw.management.domain;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.card.CardService;
import com.github.javydreamercsw.management.service.card.CardSetService;
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
  public ApplicationRunner syncSetsFromFile(CardSetService cardSetService) {
    return args -> {
      ClassPathResource resource = new ClassPathResource("sets.json");
      if (resource.exists()) {
        ObjectMapper mapper = new ObjectMapper();
        try (var is = resource.getInputStream()) {
          var cardsFromFile = mapper.readValue(is, new TypeReference<List<CardSet>>() {});
          // Map existing cards by name
          Map<String, CardSet> existing =
              cardSetService.findAll().stream().collect(Collectors.toMap(CardSet::getName, c -> c));
          for (CardSet c : cardsFromFile) {
            CardSet existingSet = existing.get(c.getName());
            if (existingSet != null) {
              // Update fields
              cardSetService.save(existingSet);
            } else {
              cardSetService.save(c);
            }
          }
        }
      }
    };
  }

  public static class CardDTO {
    private String name;
    private String type;
    private int damage;
    private boolean finisher;
    private boolean signature;
    private int stamina;
    private int momentum;
    private int target;
    private int number;
    private String set;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public int getDamage() {
      return damage;
    }

    public void setNumber(int number) {
      this.number = number;
    }

    public int getNumber() {
      return number;
    }

    public void setDamage(int damage) {
      this.damage = damage;
    }

    public boolean isFinisher() {
      return finisher;
    }

    public void setFinisher(boolean finisher) {
      this.finisher = finisher;
    }

    public boolean isSignature() {
      return signature;
    }

    public void setSignature(boolean signature) {
      this.signature = signature;
    }

    public int getStamina() {
      return stamina;
    }

    public void setStamina(int stamina) {
      this.stamina = stamina;
    }

    public int getMomentum() {
      return momentum;
    }

    public void setMomentum(int momentum) {
      this.momentum = momentum;
    }

    public int getTarget() {
      return target;
    }

    public void setTarget(int target) {
      this.target = target;
    }

    public String getSet() {
      return set;
    }

    public void setSet(String set) {
      this.set = set;
    }
  }

  @Bean
  public ApplicationRunner syncCardsFromFile(
      CardService cardService, CardSetService cardSetService) {
    return args -> {
      ClassPathResource resource = new ClassPathResource("cards.json");
      if (resource.exists()) {
        ObjectMapper mapper = new ObjectMapper();
        try (var is = resource.getInputStream()) {
          var cardsFromFile = mapper.readValue(is, new TypeReference<List<CardDTO>>() {});
          Map<String, Card> existing =
              cardService.findAll().stream().collect(Collectors.toMap(Card::getName, c -> c));
          Map<String, CardSet> sets =
              cardSetService.findAll().stream().collect(Collectors.toMap(CardSet::getName, s -> s));
          for (CardDTO dto : cardsFromFile) {
            CardSet set = sets.get(dto.getSet());
            Card card = existing.getOrDefault(dto.getName(), new Card());
            card.setName(dto.getName());
            card.setDamage(dto.getDamage());
            card.setFinisher(dto.isFinisher());
            card.setSignature(dto.isSignature());
            card.setStamina(dto.getStamina());
            card.setMomentum(dto.getMomentum());
            card.setTarget(dto.getTarget());
            card.setNumber(dto.getNumber());
            card.setSet(set);
            card.setType(dto.getType());
            cardService.save(card);
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
