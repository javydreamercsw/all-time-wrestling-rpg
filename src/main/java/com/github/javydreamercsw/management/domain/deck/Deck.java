package com.github.javydreamercsw.management.domain.deck;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "deck")
@Getter
@Setter
public class Deck extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "deck_id")
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "wrestler_id", nullable = false)
  private Wrestler wrestler;

  @OneToMany(
      mappedBy = "deck",
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  private Set<DeckCard> cards = new HashSet<>();

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  @Override
  public @Nullable Long getId() {
    return id;
  }

  public void addCard(@NonNull Card card, @NonNull CardSet set, int amount) {
    DeckCard newDeckCard = new DeckCard();
    newDeckCard.setCard(card);
    newDeckCard.setSet(set);
    newDeckCard.setAmount(amount);
    newDeckCard.setDeck(this);

    // Remove the old one if it exists, then add the new one.
    // This ensures the amount is updated.
    getCards().remove(newDeckCard);
    getCards().add(newDeckCard);
  }
}
